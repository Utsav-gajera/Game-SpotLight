import React from 'react';
import { Navigate, Route, Routes } from 'react-router-dom';
import Layout from './components/Layout';
import { useAuth } from './context/AuthContext';
import AuthPage from './pages/AuthPage';
import CatalogPage from './pages/CatalogPage';
import GameDetailPage from './pages/GameDetailPage';
import HomePage from './pages/HomePage';
import NotFoundPage from './pages/NotFoundPage';
import PurchasesPage from './pages/PurchasesPage';
import WishlistPage from './pages/WishlistPage';
import WorkspacePage from './pages/WorkspacePage';

function WorkspaceRedirect() {
  const { user, loading, isNormalUser } = useAuth();

  if (loading) {
    return <div className="glass-panel p-8 text-center text-slate-300">Restoring your session...</div>;
  }

  if (!user) {
    return <Navigate to="/auth" replace />;
  }

  if (isNormalUser) {
    return <Navigate to="/workspace/wishlist" replace />;
  }

  return <Navigate to="/catalog" replace />;
}

function ProtectedNormalUserArea({ children }) {
  const { user, loading, isNormalUser } = useAuth();

  if (loading) {
    return <div className="glass-panel p-8 text-center text-slate-300">Restoring your session...</div>;
  }

  if (!user) {
    return <Navigate to="/auth" replace />;
  }

  if (!isNormalUser) {
    return <Navigate to="/catalog" replace />;
  }

  return children;
}

function ProtectedDeveloperArea({ children }) {
  const { user, loading, isDeveloper, isAdmin } = useAuth();

  if (loading) {
    return <div className="glass-panel p-8 text-center text-slate-300">Restoring your session...</div>;
  }

  if (!user) {
    return <Navigate to="/auth" replace />;
  }

  if (!isDeveloper && !isAdmin) {
    return <Navigate to="/catalog" replace />;
  }

  return children;
}

export default function App() {
  return (
    <Layout>
      <Routes>
        <Route path="/" element={<HomePage />} />
        <Route path="/catalog" element={<CatalogPage />} />
        <Route path="/games/:gameId" element={<GameDetailPage />} />
        <Route path="/auth" element={<AuthPage />} />
        <Route path="/workspace" element={<WorkspaceRedirect />} />
        <Route path="/workspace/wishlist" element={<ProtectedNormalUserArea><WishlistPage /></ProtectedNormalUserArea>} />
        <Route path="/workspace/purchases" element={<ProtectedNormalUserArea><PurchasesPage /></ProtectedNormalUserArea>} />
        <Route path="/workspace/developer" element={<ProtectedDeveloperArea><WorkspacePage /></ProtectedDeveloperArea>} />
        <Route path="*" element={<NotFoundPage />} />
      </Routes>
    </Layout>
  );
}