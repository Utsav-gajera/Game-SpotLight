import React, { createContext, useContext, useEffect, useMemo, useState } from 'react';
import { api, storeAuthToken } from '../lib/api';

const AuthContext = createContext(null);

function normalizeUser(payload, fallbackRole) {
  if (!payload) {
    return null;
  }

  const normalizeRole = (role) => {
    if (!role) {
      return role;
    }
    const upper = String(role).toUpperCase();
    return upper === 'USER' ? 'NORMAL_USER' : upper;
  };

  const primaryRole = Array.isArray(payload.roles) && payload.roles.length > 0
    ? normalizeRole(payload.roles[0])
    : undefined;

  return {
    id: payload.id,
    username: payload.username,
    email: payload.email,
    displayName: payload.displayName,
    roles: Array.isArray(payload.roles) ? payload.roles.map(normalizeRole) : payload.roles,
    role: normalizeRole(payload.role || primaryRole || fallbackRole)
  };
}

async function loadSessionUser() {
  try {
    const profile = await api.auth.session();
    return normalizeUser(profile);
  } catch (error) {
    return null;
  }
}

export function AuthProvider({ children }) {
  const [user, setUser] = useState(null);
  const [loading, setLoading] = useState(true);
  const [bootstrapError, setBootstrapError] = useState('');

  const refreshSession = async () => {
    setLoading(true);
    setBootstrapError('');

    try {
      const profile = await loadSessionUser();
      if (profile) {
        setUser(profile);
        return profile;
      }

      setUser(null);
      return null;
    } catch (error) {
      setUser(null);
      setBootstrapError('');
      return null;
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    refreshSession().catch(() => {});
  }, []);

  const login = async (payload) => {
    const message = await api.auth.login(payload);
    if (message?.token) {
      storeAuthToken(message.token);
    }

    const immediateProfile = normalizeUser(message?.user);
    if (immediateProfile) {
      setUser(immediateProfile);
      setLoading(false);
      return { ...message, user: immediateProfile };
    }

    const refreshedProfile = await refreshSession();
    return { ...message, user: refreshedProfile };
  };

  const register = async (payload) => {
    const message = await api.auth.register(payload);
    return message;
  };

  const logout = async () => {
    try {
      await api.auth.logout();
    } finally {
      storeAuthToken(null);
      setUser(null);
    }
  };

  const value = useMemo(
    () => ({
      user,
      loading,
      bootstrapError,
      refreshSession,
      login,
      register,
      logout,
      setUser,
      isAdmin: user?.role === 'ADMIN',
      isDeveloper: user?.role === 'DEVELOPER',
      isNormalUser: user?.role === 'NORMAL_USER'
    }),
    [user, loading, bootstrapError]
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used inside AuthProvider');
  }
  return context;
}