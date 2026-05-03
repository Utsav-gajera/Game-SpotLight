package com.gamestore.storage.service;

import com.gamestore.storage.dto.FileMetadataDTO;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class StorageService {

    private static final byte[] PLACEHOLDER_IMAGE = Base64.getDecoder().decode(
            "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAusB9WlH4P8AAAAASUVORK5CYII="
    );

    private final ObjectMapper objectMapper;
    private final OkHttpClient httpClient;
    
    @Value("${supabase.url:}")
    private String supabaseUrl;
    
    @Value("${supabase.key:}")
    private String supabaseKey;
    
    @Value("${supabase.bucket.game-files:game-files}")
    private String gameFilesBucket;
    
    @Value("${supabase.bucket.game-images:game-images}")
    private String gameImagesBucket;
    
    @Value("${storage.directory:/app/files}")
    private String localStorageDirectory;

    private Path storageIndexFile;
    private boolean isSupabaseEnabled = false;

    public StorageService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.httpClient = new OkHttpClient();
    }

    @PostConstruct
    public void initializeStorage() {
        try {
            Path path = Paths.get(localStorageDirectory);
            Files.createDirectories(path);
            storageIndexFile = path.resolve("storage-index.json");
            if (!Files.exists(storageIndexFile)) {
                Files.writeString(storageIndexFile, "[]");
            }

            // Check if Supabase is configured
            if (supabaseUrl != null && !supabaseUrl.isEmpty() && 
                supabaseKey != null && !supabaseKey.isEmpty()) {
                isSupabaseEnabled = true;
                System.out.println("✅ Supabase storage enabled: " + supabaseUrl);
            } else {
                // Fallback to local storage
                isSupabaseEnabled = false;
                System.out.println("⚠️ Supabase not configured, using local storage at: " + localStorageDirectory);
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to initialize storage", e);
        }
    }

    public FileMetadataDTO uploadFile(String fileName, String fileType, Long fileSize, String uploadedBy, MultipartFile file) throws IOException {
        String fileId = UUID.randomUUID().toString();
        String extension = getFileExtension(fileName);
        String storedFileName = fileId + (extension != null ? "." + extension : "");
        
        String fileUrl;
        if (isSupabaseEnabled) {
            fileUrl = uploadToSupabase(storedFileName, fileType, file);
        } else {
            fileUrl = uploadToLocalStorage(storedFileName, file);
        }

        String browserUrl = fileUrl;

        StoredFile storedFile = new StoredFile(
                fileId,
                fileName,
                fileType,
                fileSize,
                fileUrl,
                uploadedBy,
                LocalDateTime.now().toString(),
            browserUrl
        );

        saveStoredFile(storedFile);
        return convertToDTO(storedFile);
    }

    private String uploadToSupabase(String fileName, String fileType, MultipartFile file) throws IOException {
        // Determine bucket based on file type
        String bucket = fileType != null && fileType.startsWith("image/") ? gameImagesBucket : gameFilesBucket;
        
        String uploadUrl = supabaseUrl.replaceAll("/$", "") + "/storage/v1/object/" + bucket + "/" + fileName;
        
        System.out.println("📤 Uploading to Supabase bucket '" + bucket + "': " + fileName);
        
        try {
            RequestBody requestBody = RequestBody.create(file.getBytes());
            Request request = new Request.Builder()
                    .url(uploadUrl)
                    .post(requestBody)
                    .addHeader("Authorization", "Bearer " + supabaseKey)
                    .addHeader("Content-Type", fileType != null ? fileType : "application/octet-stream")
                    .build();
            
            try (Response response = httpClient.newCall(request).execute()) {
                if (response.isSuccessful()) {
                    String publicUrl = supabaseUrl.replaceAll("/$", "") + "/storage/v1/object/public/" + bucket + "/" + fileName;
                    System.out.println("   ✅ Uploaded! URL: " + publicUrl);
                    return publicUrl;
                }

                throw new IOException("Supabase upload failed: " + response.code() + " " + response.message());
            }
        } catch (Exception e) {
            System.err.println("❌ Supabase upload error: " + e.getMessage());
            e.printStackTrace();
            throw new IOException("Supabase upload failed", e);
        }
    }

    private String uploadToLocalStorage(String storedFileName, MultipartFile file) throws IOException {
        Path filePath = Paths.get(localStorageDirectory, storedFileName);
        file.transferTo(filePath.toFile());
        return "/api/storage/files/" + storedFileName;
    }

    public FileMetadataDTO getFileMetadata(String fileId) {
        return loadStoredFiles().stream()
            .filter(file -> fileId.equals(file.id))
            .findFirst()
            .map(file -> convertToDTO(file))
            .orElse(null);
    }

    public byte[] getFileContent(String fileId) throws IOException {
        StoredFile storedFile = loadStoredFiles().stream()
                .filter(file -> fileId.equals(file.id))
                .findFirst()
                .orElse(null);

        if (storedFile == null) {
            return PLACEHOLDER_IMAGE;
        }

        if (isSupabaseEnabled && storedFile.filePath.startsWith("http")) {
            return downloadFromSupabase(storedFile.filePath);
        } else {
            return getFileContentLocal(storedFile.filePath);
        }
    }

    private byte[] downloadFromSupabase(String supabaseUrl) {
        try {
            SupabaseObjectRef objectRef = resolveSupabaseObjectRef(supabaseUrl);
            if (objectRef == null) {
                return PLACEHOLDER_IMAGE;
            }

            String downloadUrl = this.supabaseUrl.replaceAll("/$", "") + "/storage/v1/object/" + objectRef.bucket + "/" + objectRef.objectPath;
            Request request = new Request.Builder()
                    .url(downloadUrl)
                    .get()
                    .addHeader("Authorization", "Bearer " + supabaseKey)
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful() || response.body() == null) {
                    return PLACEHOLDER_IMAGE;
                }
                return response.body().bytes();
            }
        } catch (Exception e) {
            System.err.println("⚠️ Failed to download from Supabase: " + e.getMessage());
            return PLACEHOLDER_IMAGE;
        }
    }

    private byte[] getFileContentLocal(String filePath) throws IOException {
        Path path = Paths.get(filePath);
        if (!Files.exists(path)) {
            return PLACEHOLDER_IMAGE;
        }
        return Files.readAllBytes(path);
    }

    public String getFileType(String fileId) {
        StoredFile storedFile = loadStoredFiles().stream()
                .filter(file -> fileId.equals(file.id))
                .findFirst()
                .orElse(null);
        return storedFile != null && storedFile.fileType != null ? storedFile.fileType : "image/png";
    }

    public String getFileName(String fileId) {
        StoredFile storedFile = loadStoredFiles().stream()
                .filter(file -> fileId.equals(file.id))
                .findFirst()
                .orElse(null);
        return storedFile != null && storedFile.fileName != null ? storedFile.fileName : fileId + ".png";
    }

    public String getSignedDownloadUrl(String fileId, int expiresInSeconds) throws IOException {
        StoredFile storedFile = loadStoredFiles().stream()
                .filter(file -> fileId.equals(file.id))
                .findFirst()
                .orElse(null);

        if (storedFile == null) {
            throw new IOException("File not found: " + fileId);
        }

        if (!isSupabaseEnabled) {
            throw new IOException("Signed URLs require Supabase");
        }

        String filePath = storedFile.filePath;
        if (!filePath.startsWith("http")) {
            throw new IOException("Legacy file path detected. Cannot generate signed URL for: " + filePath);
        }

        SupabaseObjectRef ref = resolveSupabaseObjectRef(filePath);
        if (ref == null) {
            throw new IOException("Could not parse Supabase URL: " + filePath);
        }

        // Diagnostic: log the resolved bucket and object path to help debug "File not found" issues.
        System.out.println("🔍 Signed URL request for fileId=" + fileId + " -> bucket='" + ref.bucket + "', path='" + ref.objectPath + "'");

        // Return backend proxy endpoint for downloads (supports private buckets via service-role key)
        // The backend will fetch from Supabase and stream to client
        String proxyUrl = "/api/storage/files/" + fileId + "/download";
        System.out.println("✅ Generated download proxy URL for " + fileId + " -> " + proxyUrl);
        return proxyUrl;
        
    }

    public void deleteFile(String fileId) {
        List<StoredFile> storedFiles = loadStoredFiles();
        StoredFile storedFile = storedFiles.stream()
                .filter(file -> fileId.equals(file.id))
                .findFirst()
                .orElse(null);

        if (storedFile != null) {
            if (isSupabaseEnabled) {
                deleteFromSupabase(storedFile.filePath);
            } else {
                deleteFromLocalStorage(storedFile.filePath);
            }
            storedFiles.removeIf(file -> fileId.equals(file.id));
            try {
                writeStoredFiles(storedFiles);
            } catch (Exception e) {
                System.err.println("⚠️ Failed to update storage index: " + e.getMessage());
            }
        }
    }

    private void deleteFromSupabase(String fileUrl) {
        try {
            SupabaseObjectRef objectRef = resolveSupabaseObjectRef(fileUrl);
            if (objectRef != null) {
                String deleteUrl = supabaseUrl.replaceAll("/$", "") + "/storage/v1/object/" + objectRef.bucket + "/" + objectRef.objectPath;
                
                Request request = new Request.Builder()
                        .url(deleteUrl)
                        .delete()
                        .addHeader("Authorization", "Bearer " + supabaseKey)
                        .build();
                
                try (Response response = httpClient.newCall(request).execute()) {
                    if (response.isSuccessful()) {
                        System.out.println("✅ Deleted from Supabase: " + objectRef.objectPath);
                    } else {
                        System.err.println("⚠️ Delete failed: " + response.code());
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("❌ Supabase delete error: " + e.getMessage());
        }
    }

    private void deleteFromLocalStorage(String filePath) {
        try {
            Path path = Paths.get(filePath);
            Files.deleteIfExists(path);
        } catch (IOException e) {
            System.err.println("⚠️ Failed to delete local file: " + e.getMessage());
        }
    }

    private void saveStoredFile(StoredFile storedFile) throws IOException {
        List<StoredFile> storedFiles = loadStoredFiles();
        storedFiles.removeIf(file -> storedFile.id.equals(file.id));
        storedFiles.add(storedFile);
        writeStoredFiles(storedFiles);
    }

    private List<StoredFile> loadStoredFiles() {
        try {
            if (storageIndexFile == null || !Files.exists(storageIndexFile)) {
                return new ArrayList<>();
            }
            String content = Files.readString(storageIndexFile);
            if (content.isBlank()) {
                return new ArrayList<>();
            }
            return objectMapper.readValue(content, new TypeReference<List<StoredFile>>() {});
        } catch (IOException e) {
            return new ArrayList<>();
        }
    }

    private void writeStoredFiles(List<StoredFile> storedFiles) throws IOException {
        if (storageIndexFile != null) {
            Files.writeString(storageIndexFile, objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(storedFiles));
        }
    }

    private FileMetadataDTO convertToDTO(StoredFile entity) {
        FileMetadataDTO dto = new FileMetadataDTO();
        dto.setId(entity.id);
        dto.setFileName(entity.fileName);
        dto.setFileType(entity.fileType);
        dto.setFileSize(entity.fileSize);
        dto.setUrl(resolveBrowserUrl(entity));
        dto.setUploadedBy(entity.uploadedBy);
        return dto;
    }

    private String resolveBrowserUrl(StoredFile entity) {
        if (entity == null) {
            return null;
        }
        if (entity.url != null && entity.url.startsWith("http")) {
            return entity.url;
        }
        if (entity.filePath != null && entity.filePath.startsWith("http")) {
            return entity.filePath;
        }
        return entity.url;
    }

    private String getFileExtension(String fileName) {
        if (fileName != null && fileName.contains(".")) {
            return fileName.substring(fileName.lastIndexOf(".") + 1);
        }
        return null;
    }

    private SupabaseObjectRef resolveSupabaseObjectRef(String fileUrl) {
        if (fileUrl == null || fileUrl.isBlank()) {
            return null;
        }

        String normalized = fileUrl.trim();
        int objectIndex = normalized.indexOf("/object/public/");
        if (objectIndex >= 0) {
            String remaining = normalized.substring(objectIndex + "/object/public/".length());
            int separatorIndex = remaining.indexOf('/');
            if (separatorIndex <= 0 || separatorIndex >= remaining.length() - 1) {
                return null;
            }
            return new SupabaseObjectRef(remaining.substring(0, separatorIndex), remaining.substring(separatorIndex + 1));
        }

        objectIndex = normalized.indexOf("/object/");
        if (objectIndex >= 0) {
            String remaining = normalized.substring(objectIndex + "/object/".length());
            int separatorIndex = remaining.indexOf('/');
            if (separatorIndex <= 0 || separatorIndex >= remaining.length() - 1) {
                return null;
            }
            return new SupabaseObjectRef(remaining.substring(0, separatorIndex), remaining.substring(separatorIndex + 1));
        }

        return null;
    }

    private static final class SupabaseObjectRef {
        private final String bucket;
        private final String objectPath;

        private SupabaseObjectRef(String bucket, String objectPath) {
            this.bucket = bucket;
            this.objectPath = objectPath;
        }
    }

    private static class StoredFile {
        public String id;
        public String fileName;
        public String fileType;
        public Long fileSize;
        public String filePath;
        public String uploadedBy;
        public String uploadedAt;
        public String url;

        public StoredFile() {
        }

        public StoredFile(String id, String fileName, String fileType, Long fileSize, String filePath, String uploadedBy, String uploadedAt, String url) {
            this.id = id;
            this.fileName = fileName;
            this.fileType = fileType;
            this.fileSize = fileSize;
            this.filePath = filePath;
            this.uploadedBy = uploadedBy;
            this.uploadedAt = uploadedAt;
            this.url = url;
        }
    }
}

