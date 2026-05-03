const API_ROOT = '/api';
const AUTH_API_ROOT = import.meta.env.VITE_AUTH_API_ROOT || 'http://localhost:8081/api';
const GAME_API_ROOT = import.meta.env.VITE_GAME_API_ROOT || 'http://localhost:8082/api';
const STORAGE_API_ROOT = import.meta.env.VITE_STORAGE_API_ROOT || 'http://localhost:8085/api';
const PURCHASE_API_ROOT = import.meta.env.VITE_PURCHASE_API_ROOT || 'http://localhost:8083/api';
const WISHLIST_API_ROOT = import.meta.env.VITE_WISHLIST_API_ROOT || 'http://localhost:8084/api';
const TOKEN_KEY = 'gameSpotlightToken';
const STORAGE_ORIGIN = STORAGE_API_ROOT.replace(/\/api\/?$/, '');

async function parseResponse(response) {
  const text = await response.text();
  if (!text) {
    return null;
  }

  try {
    return JSON.parse(text);
  } catch {
    return text;
  }
}

function extractMessage(payload, fallback) {
  if (typeof payload === 'string') {
    return payload;
  }
  if (payload && typeof payload === 'object') {
    if (typeof payload.message === 'string') {
      return payload.message;
    }
    if (typeof payload.error === 'string') {
      return payload.error;
    }
    return JSON.stringify(payload);
  }
  return fallback;
}

function normalizeStorageUrl(value) {
  if (typeof value !== 'string' || !value.trim()) {
    return value;
  }

  if (/^https?:\/\//i.test(value)) {
    return value;
  }

  if (value.startsWith('/api/storage/')) {
    return `${STORAGE_ORIGIN}${value}`;
  }

  return value;
}

function normalizeGame(game) {
  if (!game || typeof game !== 'object') {
    return game;
  }

  return {
    ...game,
    imageUrl: normalizeStorageUrl(game.imageUrl),
    gameFileUrl: normalizeStorageUrl(game.gameFileUrl),
    galleryImageUrls: Array.isArray(game.galleryImageUrls)
      ? game.galleryImageUrls.map(normalizeStorageUrl)
      : game.galleryImageUrls
  };
}

function normalizeGames(payload) {
  if (Array.isArray(payload)) {
    return payload.map(normalizeGame);
  }
  return normalizeGame(payload);
}

export async function request(path, options = {}) {
  const { body, headers = {}, ...rest } = options;
  const finalHeaders = { ...headers };
  let finalBody = body;

  const token = localStorage.getItem(TOKEN_KEY);
  if (token && !finalHeaders.Authorization) {
    finalHeaders.Authorization = `Bearer ${token}`;
  }

  if (body !== undefined && body !== null && !(body instanceof FormData)) {
    finalHeaders['Content-Type'] = finalHeaders['Content-Type'] || 'application/json';
    finalBody = JSON.stringify(body);
  }

  const response = await fetch(`${API_ROOT}${path}`, {
    credentials: 'include',
    ...rest,
    headers: finalHeaders,
    body: finalBody
  });

  const payload = await parseResponse(response);
  if (!response.ok) {
    const error = new Error(extractMessage(payload, response.statusText));
    error.status = response.status;
    error.payload = payload;
    throw error;
  }

  return payload;
}

export async function downloadFile(url, fileName) {
  try {
    // Backend always returns proxy URLs (starting with /api) for downloads
    // Client can fetch directly; no need to parse fileId from Supabase URLs
    const response = await fetch(url, { credentials: 'include' });
    if (!response.ok) {
      throw new Error(`Download failed (${response.status})`);
    }

    const blob = await response.blob();
    const objectUrl = URL.createObjectURL(blob);

    try {
      const link = document.createElement('a');
      link.href = objectUrl;
      link.download = fileName || '';
      link.rel = 'noopener';
      document.body.appendChild(link);
      link.click();
      link.remove();
    } finally {
      setTimeout(() => URL.revokeObjectURL(objectUrl), 1000);
    }
  } catch {
    window.open(url, '_blank', 'noopener,noreferrer');
  }
}

async function requestWithRoot(root, path, options = {}) {
  const { body, headers = {}, ...rest } = options;
  const finalHeaders = { ...headers };
  let finalBody = body;

  const token = localStorage.getItem(TOKEN_KEY);
  if (token && !finalHeaders.Authorization) {
    finalHeaders.Authorization = `Bearer ${token}`;
  }

  if (body !== undefined && body !== null && !(body instanceof FormData)) {
    finalHeaders['Content-Type'] = finalHeaders['Content-Type'] || 'application/json';
    finalBody = JSON.stringify(body);
  }

  const response = await fetch(`${root}${path}`, {
    credentials: 'include',
    ...rest,
    headers: finalHeaders,
    body: finalBody
  });

  const payload = await parseResponse(response);
  if (!response.ok) {
    const error = new Error(extractMessage(payload, response.statusText));
    error.status = response.status;
    error.payload = payload;
    throw error;
  }

  return payload;
}

export const api = {
  auth: {
    register: (payload) => requestWithRoot(AUTH_API_ROOT, '/auth/register', { method: 'POST', body: payload }),
    login: (payload) => requestWithRoot(AUTH_API_ROOT, '/auth/login', { method: 'POST', body: payload }),
    logout: () => requestWithRoot(AUTH_API_ROOT, '/auth/logout', { method: 'POST' }),
    session: () => requestWithRoot(AUTH_API_ROOT, '/auth/session')
  },
  games: {
    all: async () => normalizeGames(await requestWithRoot(GAME_API_ROOT, '/games')),
    byId: async (gameId) => normalizeGame(await requestWithRoot(GAME_API_ROOT, `/games/${gameId}`)),
    addReview: async (gameId, payload) => normalizeGame(await requestWithRoot(GAME_API_ROOT, `/games/${gameId}/reviews`, { method: 'POST', body: payload })),
    deleteReview: async (gameId) => normalizeGame(await requestWithRoot(GAME_API_ROOT, `/games/${gameId}/reviews`, { method: 'DELETE' })),
    search: async (title) => normalizeGames(await requestWithRoot(GAME_API_ROOT, `/games/search?title=${encodeURIComponent(title)}`)),
    genre: async (genre) => normalizeGames(await requestWithRoot(GAME_API_ROOT, `/games/genre/${encodeURIComponent(genre)}`)),
    price: (min, max) => {
      const params = new URLSearchParams();
      if (min !== undefined && min !== null && min !== '') params.set('min', min);
      if (max !== undefined && max !== null && max !== '') params.set('max', max);
      return requestWithRoot(GAME_API_ROOT, `/games/price?${params.toString()}`).then(normalizeGames);
    },
    filter: ({ title, genre, minPrice, maxPrice }) => {
      const params = new URLSearchParams();
      if (title) params.set('title', title);
      if (genre) params.set('genre', genre);
      if (minPrice !== undefined && minPrice !== null && minPrice !== '') params.set('minPrice', minPrice);
      if (maxPrice !== undefined && maxPrice !== null && maxPrice !== '') params.set('maxPrice', maxPrice);
      return requestWithRoot(GAME_API_ROOT, `/games/filter?${params.toString()}`).then(normalizeGames);
    }
  },
  user: {
    profile: () => requestWithRoot(AUTH_API_ROOT, '/auth/session'),
    updateProfile: (username) => requestWithRoot(AUTH_API_ROOT, '/user/profile', { method: 'PUT', body: new URLSearchParams({ username }).toString(), headers: { 'Content-Type': 'application/x-www-form-urlencoded' } }),
    changePassword: (payload) => requestWithRoot(AUTH_API_ROOT, '/user/password', { method: 'POST', body: payload }),
    purchases: () => requestWithRoot(PURCHASE_API_ROOT, '/purchases/user/me'),
    purchaseGame: (gameId) => requestWithRoot(PURCHASE_API_ROOT, '/purchases', { method: 'POST', body: { gameId } }),
    getGameDownloadUrl: async (gameId) => {
      const payload = await requestWithRoot(GAME_API_ROOT, `/games/${gameId}/download-url`);
      if (payload && typeof payload === 'object' && payload.url) {
        return { ...payload, url: normalizeStorageUrl(payload.url) };
      }
      return payload;
    },
    wishlistList: () => request('/user/wishlist'),
    wishlistCreate: (name) => request(`/user/wishlist/create?name=${encodeURIComponent(name)}`, { method: 'POST' }),
    wishlistById: (wishlistId) => request(`/user/wishlist/${wishlistId}`),
    wishlistDelete: (wishlistId) => request(`/user/wishlist/${wishlistId}`, { method: 'DELETE' }),
    wishlistAdd: (wishlistId, gameId) => request(`/user/wishlist/${wishlistId}/add/${gameId}`, { method: 'POST' }),
    wishlistRemove: (wishlistId, gameId) => request(`/user/wishlist/${wishlistId}/remove/${gameId}`, { method: 'DELETE' }),
    wishlistUpdate: (wishlistId, oldGameId, newGameId) => request(`/user/wishlist/${wishlistId}/update/${oldGameId}/${newGameId}`, { method: 'PUT' })
  },
  developer: {
    profile: () => requestWithRoot(AUTH_API_ROOT, '/auth/session'),
    games: async () => normalizeGames(await requestWithRoot(GAME_API_ROOT, '/games')),
    createGame: (payload) => requestWithRoot(GAME_API_ROOT, '/games', { method: 'POST', body: payload }),
    updateGame: (gameId, payload) => requestWithRoot(GAME_API_ROOT, `/games/${gameId}`, { method: 'PUT', body: payload }),
    deleteGame: (gameId) => requestWithRoot(GAME_API_ROOT, `/games/${gameId}`, { method: 'DELETE' })
  },
  storage: {
    uploadFile: (file, uploadedBy) => {
      const formData = new FormData();
      formData.append('file', file);
      if (uploadedBy) {
        formData.append('uploadedBy', uploadedBy);
      }
      return requestWithRoot(STORAGE_API_ROOT, '/storage/upload', { method: 'POST', body: formData });
    }
  },
  admin: {
    profile: () => requestWithRoot(AUTH_API_ROOT, '/auth/session'),
    users: () => requestWithRoot(AUTH_API_ROOT, '/users'),
    games: async () => normalizeGames(await requestWithRoot(GAME_API_ROOT, '/games')),
    purchases: () => requestWithRoot(PURCHASE_API_ROOT, '/purchases'),
    createUser: (payload) => requestWithRoot(AUTH_API_ROOT, '/users', { method: 'POST', body: payload }),
    updateUser: (userId, payload) => requestWithRoot(AUTH_API_ROOT, `/users/${userId}`, { method: 'PUT', body: payload }),
    deleteUser: (userId) => requestWithRoot(AUTH_API_ROOT, `/users/${userId}`, { method: 'DELETE' }),
    createGame: (payload) => requestWithRoot(GAME_API_ROOT, '/games', { method: 'POST', body: payload }),
    updateGame: (gameId, payload) => requestWithRoot(GAME_API_ROOT, `/games/${gameId}`, { method: 'PUT', body: payload }),
    deleteGame: (gameId) => requestWithRoot(GAME_API_ROOT, `/games/${gameId}`, { method: 'DELETE' })
  }
};

export function storeAuthToken(token) {
  if (token) {
    localStorage.setItem(TOKEN_KEY, token);
  } else {
    localStorage.removeItem(TOKEN_KEY);
  }
}
