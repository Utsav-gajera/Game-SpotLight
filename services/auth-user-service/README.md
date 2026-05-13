# auth-user-service

Standalone Spring Boot service for authentication and user profile data.

## What it does
- Registers users in MongoDB.
- Logs in users and returns a JWT.
- Exposes protected `/api/auth/me` and `/api/users/profile` endpoints.

## Run locally

```powershell
cd services/auth-user-service
$env:MONGO_URI="mongodb+srv://utsav:9228224337@cluster0.yslwcbv.mongodb.net/auth-user-db?retryWrites=true&w=majority"
$env:JWT_SECRET="[REDACTED-JWT]"
mvn spring-boot:run
```

## Build

```powershell
cd services/auth-user-service
mvn -DskipTests package
```

## Docker

```powershell
cd services/auth-user-service
docker build -t auth-user-service:dev .
docker run -p 8087:8087 -e PORT=8087 -e MONGO_URI="mongodb+srv://utsav:9228224337@cluster0.yslwcbv.mongodb.net/auth-user-db?retryWrites=true&w=majority" -e JWT_SECRET="[REDACTED-JWT]" auth-user-service:dev
```