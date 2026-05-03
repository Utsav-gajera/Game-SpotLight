import React, { useEffect, useMemo, useState } from 'react';
import { NavLink, Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';

const baseNavItems = [
  { to: '/', label: 'Discover' },
  { to: '/catalog', label: 'Catalog' }
];

function NavButton({ to, children }) {
  return (
    <NavLink
      to={to}
      className={({ isActive }) =>
        [
          'rounded-full px-4 py-2 text-sm font-medium transition',
          isActive ? 'bg-white/15 text-white shadow-glow' : 'text-slate-300 hover:bg-white/10 hover:text-white'
        ].join(' ')
      }
    >
      {children}
    </NavLink>
  );
}

export default function Layout({ children }) {
  const navigate = useNavigate();
  const { user, logout, bootstrapError, isDeveloper, isAdmin } = useAuth();
  const [search, setSearch] = useState('');

  const roleLabel = useMemo(() => user?.role?.replace('_', ' ') || 'GUEST', [user]);
  const navItems = useMemo(() => {
    if (user?.role === 'NORMAL_USER') {
      return [
        ...baseNavItems,
        { to: '/workspace/wishlist', label: 'Wishlist' },
        { to: '/workspace/purchases', label: 'Purchases' }
      ];
    }
    if (isDeveloper || isAdmin) {
      return [
        ...baseNavItems,
        { to: '/workspace/developer', label: 'DEVELOPER' }
      ];
    }
    return baseNavItems;
  }, [user?.role, isDeveloper, isAdmin]);

  // Debounced instant search
  useEffect(() => {
    const timeout = setTimeout(() => {
      if (search.trim()) {
        navigate(`/catalog?title=${encodeURIComponent(search.trim())}`);
      }
    }, 400); // 400ms debounce delay
    return () => clearTimeout(timeout);
  }, [search, navigate]);

  const submitSearch = (event) => {
    event.preventDefault();
    // Clear timeout and search immediately on enter/submit
    if (search.trim()) {
      navigate(`/catalog?title=${encodeURIComponent(search.trim())}`);
    }
  };

  return (
    <div className="app-shell bg-ink">
      <a href="#main-content" className="skip-link">
        Skip to content
      </a>
      <div className="fixed inset-0 -z-10 bg-hero-grid" />
      <header className="sticky top-0 z-40 border-b border-white/10 bg-ink/80 backdrop-blur-2xl">
        <div className="mx-auto flex max-w-7xl items-center gap-4 px-4 py-4 sm:px-6 lg:px-8">
          <Link to="/" className="flex items-center gap-3">
            <div className="grid h-11 w-11 place-items-center rounded-2xl bg-gradient-to-br from-accent to-accent2 text-sm font-black text-ink shadow-glow">
              GS
            </div>
            <div>
              <div className="font-display text-lg font-bold tracking-wide">GameSpotlight</div>
              <div className="text-xs uppercase tracking-[0.22em] text-slate-400">Curated game marketplace</div>
            </div>
          </Link>

          <form onSubmit={submitSearch} className="hidden flex-1 lg:flex">
            <div className="flex w-full items-center gap-3 rounded-full border border-white/10 bg-white/5 px-4 py-2 shadow-glow transition focus-within:border-accent/40 focus-within:bg-white/10">
              <svg viewBox="0 0 24 24" className="h-4 w-4 text-slate-400" fill="none" stroke="currentColor" strokeWidth="2">
                <circle cx="11" cy="11" r="7" />
                <path d="m20 20-3.5-3.5" />
              </svg>
              <input
                value={search}
                onChange={(event) => setSearch(event.target.value)}
                placeholder="Search games, genres, studios, or keywords"
                className="w-full bg-transparent text-sm outline-none placeholder:text-slate-500"
              />
            </div>
          </form>

          <nav className="hidden items-center gap-2 lg:flex" aria-label="Primary navigation">
            {navItems.map((item) => (
              <NavButton key={item.to} to={item.to}>
                {item.label}
              </NavButton>
            ))}
          </nav>

          <div className="ml-auto flex items-center gap-3">
            <div className="hidden rounded-full border border-white/10 bg-white/5 px-3 py-1 text-xs uppercase tracking-[0.24em] text-slate-300 sm:block">
              {roleLabel}
            </div>
            {user ? (
              <button
                type="button"
                onClick={logout}
                className="rounded-full border border-white/10 bg-white/5 px-4 py-2 text-sm font-medium text-white transition hover:border-accent/40 hover:bg-accent/10"
              >
                Logout
              </button>
            ) : (
              <Link
                to="/auth"
                className="rounded-full bg-gradient-to-r from-accent to-accent2 px-4 py-2 text-sm font-semibold text-ink shadow-glow transition hover:brightness-110"
              >
                Login
              </Link>
            )}
          </div>
        </div>
        <div className="mx-auto flex max-w-7xl items-center gap-2 overflow-x-auto px-4 pb-4 sm:px-6 lg:hidden lg:px-8">
          {navItems.map((item) => (
            <NavButton key={item.to} to={item.to}>
              {item.label}
            </NavButton>
          ))}
        </div>
      </header>

      {bootstrapError ? (
        <div className="border-b border-warm/30 bg-warm/10 px-4 py-3 text-center text-sm text-warm">
          Session notice: {bootstrapError}
        </div>
      ) : null}

      <main id="main-content" className="page-surface mx-auto max-w-7xl px-4 py-6 sm:px-6 lg:px-8">
        {children}
      </main>
    </div>
  );
}