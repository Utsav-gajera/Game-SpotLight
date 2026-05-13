import React, { useEffect, useMemo, useRef, useState } from 'react';
import { NavLink, Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { api } from '../lib/api';
import { prefetchRoute } from '../lib/routePrefetch';
import ProfileMenu from './ProfileMenu';

const baseNavItems = [
  { to: '/', label: 'Discover' },
  { to: '/catalog', label: 'Catalog' }
];

function NavButton({ to, children }) {
  const prefetchNavTarget = () => prefetchRoute(to);

  return (
    <NavLink
      to={to}
      onMouseEnter={prefetchNavTarget}
      onFocus={prefetchNavTarget}
      onTouchStart={prefetchNavTarget}
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
  const [suggestions, setSuggestions] = useState([]);
  const [didYouMean, setDidYouMean] = useState('');
  const [showSuggestions, setShowSuggestions] = useState(false);
  const [suggestionsLoading, setSuggestionsLoading] = useState(false);
  const searchBoxRef = useRef(null);

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


  useEffect(() => {
    const term = search.trim();
    if (!term) {
      setSuggestions([]);
      setDidYouMean('');
      setShowSuggestions(false);
      return;
    }

    let cancelled = false;
    const timeout = setTimeout(async () => {
      try {
        setSuggestionsLoading(true);
        const result = await api.games.suggestions(term, 8);
        if (!cancelled) {
          const nextSuggestions = Array.isArray(result?.suggestions) ? result.suggestions : [];
          setSuggestions(nextSuggestions);
          setDidYouMean(typeof result?.didYouMean === 'string' ? result.didYouMean : '');
          setShowSuggestions(true);
        }
      } catch {
        if (!cancelled) {
          setSuggestions([]);
          setDidYouMean('');
        }
      } finally {
        if (!cancelled) {
          setSuggestionsLoading(false);
        }
      }
    }, 250);

    return () => {
      cancelled = true;
      clearTimeout(timeout);
    };
  }, [search]);

  useEffect(() => {
    const handleClickOutside = (event) => {
      if (searchBoxRef.current && !searchBoxRef.current.contains(event.target)) {
        setShowSuggestions(false);
      }
    };

    document.addEventListener('mousedown', handleClickOutside);
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, []);

  const submitSearch = (event) => {
    event.preventDefault();
    // Clear timeout and search immediately on enter/submit
    if (search.trim()) {
      navigate(`/catalog?aiQuery=${encodeURIComponent(search.trim())}`);
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
              <div className="text-xs uppercase tracking-[0.22em] text-slate-400">Browse, save, buy, build</div>
            </div>
          </Link>

          <form onSubmit={submitSearch} className="hidden flex-1 lg:flex" ref={searchBoxRef}>
            <div className="relative w-full">
              <div className="flex w-full items-center gap-3 rounded-full border border-white/10 bg-white/5 px-4 py-2 shadow-glow transition focus-within:border-accent/40 focus-within:bg-white/10">
              <svg viewBox="0 0 24 24" className="h-4 w-4 text-slate-400" fill="none" stroke="currentColor" strokeWidth="2">
                <circle cx="11" cy="11" r="7" />
                <path d="m20 20-3.5-3.5" />
              </svg>
              <input
                value={search}
                onChange={(event) => setSearch(event.target.value)}
                onFocus={() => search.trim() && setShowSuggestions(true)}
                placeholder="Search titles, studios, genres, or aliases"
                className="w-full bg-transparent text-sm outline-none placeholder:text-slate-500"
              />
              </div>

              {showSuggestions && (search.trim() || suggestionsLoading) ? (
                <div className="absolute left-0 right-0 top-full z-50 mt-2 overflow-hidden rounded-2xl border border-white/10 bg-ink/95 shadow-2xl backdrop-blur-xl">
                  <div className="border-b border-white/10 px-4 py-2 text-[11px] uppercase tracking-[0.24em] text-slate-400">
                    {suggestionsLoading ? 'Finding matches...' : 'Suggestions'}
                  </div>
                  {didYouMean ? (
                    <button
                      type="button"
                      onClick={() => {
                        setSearch(didYouMean);
                        setShowSuggestions(false);
                        navigate(`/catalog?aiQuery=${encodeURIComponent(didYouMean)}`);
                      }}
                      className="flex w-full items-center justify-between border-b border-white/10 bg-accent/10 px-4 py-3 text-left text-sm text-white transition hover:bg-accent/20"
                    >
                      <span>
                        Did you mean <span className="font-semibold">{didYouMean}</span>?
                      </span>
                      <span className="text-xs uppercase tracking-[0.2em] text-accent2">Use</span>
                    </button>
                  ) : null}
                  {suggestions.length > 0 ? (
                    <div className="max-h-72 overflow-auto p-2">
                      {suggestions.map((item) => (
                        <button
                          key={item}
                          type="button"
                          onClick={() => {
                            setSearch(item);
                            setShowSuggestions(false);
                            navigate(`/catalog?aiQuery=${encodeURIComponent(item)}`);
                          }}
                          className="flex w-full items-center justify-between rounded-xl px-3 py-2 text-left text-sm text-slate-200 transition hover:bg-white/8 hover:text-white"
                        >
                          <span>{item}</span>
                          <span className="text-xs uppercase tracking-[0.2em] text-slate-500">Search</span>
                        </button>
                      ))}
                    </div>
                  ) : !suggestionsLoading ? (
                    <div className="px-4 py-4 text-sm text-slate-400">No suggestions yet. Try a title, studio, genre, or alias.</div>
                  ) : null}
                </div>
              ) : null}
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
              <ProfileMenu />
            ) : (
              <Link
                to="/auth"
                onMouseEnter={() => prefetchRoute('/auth')}
                onFocus={() => prefetchRoute('/auth')}
                onTouchStart={() => prefetchRoute('/auth')}
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
          <NavButton to="/catalog">Search</NavButton>
          <NavButton to="/workspace/wishlist">Wishlist</NavButton>
          <NavButton to="/workspace/purchases">Purchases</NavButton>
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