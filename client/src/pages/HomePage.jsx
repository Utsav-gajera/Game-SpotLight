import React, { useEffect, useMemo, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import GameCard from '../components/GameCard';
import { api } from '../lib/api';

function formatPrice(price) {
  const value = Number(price);
  if (!Number.isFinite(value) || value === 0) {
    return 'Free';
  }
  return `$${value.toFixed(2)}`;
}

function HeroStat({ label, value }) {
  return (
    <div className="metric-card">
      <div className="text-xs uppercase tracking-[0.28em] text-slate-400">{label}</div>
      <div className="mt-2 font-display text-2xl font-bold text-white">{value}</div>
    </div>
  );
}

export default function HomePage() {
  const navigate = useNavigate();
  const [games, setGames] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [search, setSearch] = useState('');

  useEffect(() => {
    let active = true;
    setLoading(true);
    api.games
      .all()
      .then((data) => {
        if (!active) return;
        setGames(Array.isArray(data) ? data : []);
      })
      .catch((err) => {
        if (!active) return;
        setError(err.message || 'Failed to load games.');
      })
      .finally(() => {
        if (active) setLoading(false);
      });

    return () => {
      active = false;
    };
  }, []);

  const genres = useMemo(() => {
    const unique = new Set(games.map((game) => game.genre).filter(Boolean));
    return Array.from(unique).slice(0, 8);
  }, [games]);

  const featured = games.slice(0, 6);
  const newest = [...games]
    .sort((left, right) => String(right.releaseDate || '').localeCompare(String(left.releaseDate || '')))
    .slice(0, 3);

  const submitSearch = (event) => {
    event.preventDefault();
    navigate(`/catalog?title=${encodeURIComponent(search.trim())}`);
  };

  return (
    <div className="space-y-8">
      <section className="grid gap-6 lg:grid-cols-[1.25fr_0.75fr]">
        <div className="hero-panel page-surface">
          <div className="absolute inset-0 bg-[radial-gradient(circle_at_top_right,rgba(61,214,198,0.15),transparent_30%),radial-gradient(circle_at_bottom_left,rgba(108,140,255,0.15),transparent_25%)]" />
          <div className="absolute -right-8 top-10 h-40 w-40 rounded-full bg-accent/10 blur-3xl float-slow" />
          <div className="absolute bottom-0 left-10 h-56 w-56 rounded-full bg-accent2/10 blur-3xl float-slower" />
          <div className="relative max-w-2xl reveal-up">
            <div className="hero-badge">
              <span className="h-2 w-2 rounded-full bg-accent" />
              Play Store inspired game marketplace
            </div>
            <h1 className="mt-5 max-w-xl font-display text-4xl font-bold leading-tight text-white sm:text-6xl">
              Discover, wishlist, and launch games in one cinematic space.
            </h1>
            <p className="mt-5 max-w-xl text-base leading-7 text-slate-300 sm:text-lg">
              GameSpotlight blends storefront polish with the control panels developers and admins need.
              Browse rich listings, manage your library, and ship updates with a UI that feels fast and premium.
            </p>
            <div className="mt-7 flex flex-wrap gap-3 reveal-delay-1">
              <Link to="/catalog" className="primary-button">
                Explore catalog
              </Link>
              <Link to="/wishlist" className="secondary-button">
                View wishlist
              </Link>
            </div>
            <form onSubmit={submitSearch} className="mt-7 max-w-2xl reveal-delay-2">
              <label className="label-text">Search games</label>
              <div className="mt-2 flex flex-col gap-3 sm:flex-row">
                <input
                  value={search}
                  onChange={(event) => setSearch(event.target.value)}
                  className="input-field flex-1"
                  placeholder="Search by title, studio, or keyword"
                />
                <button type="submit" className="secondary-button sm:px-6">
                  Search
                </button>
              </div>
              <div className="mt-3 flex flex-wrap gap-2 text-xs uppercase tracking-[0.22em] text-slate-400">
                <Link to="/catalog?genre=Action" className="chip-button">
                  Action
                </Link>
                <Link to="/catalog?genre=Adventure" className="chip-button">
                  Adventure
                </Link>
                <Link to="/catalog?genre=Racing" className="chip-button">
                  Racing
                </Link>
              </div>
            </form>
            <div className="mt-8 flex flex-wrap gap-2 reveal-delay-3">
              {genres.map((genre) => (
                <Link key={genre} to={`/catalog?genre=${encodeURIComponent(genre)}`} className="chip-button normal-case tracking-normal">
                  {genre}
                </Link>
              ))}
            </div>
          </div>
        </div>

        <div className="grid gap-4">
          <HeroStat label="Games listed" value={String(games.length).padStart(2, '0')} />
          <HeroStat label="Featured collections" value={String(featured.length).padStart(2, '0')} />
          <div className="surface-card reveal-up">
            <div className="text-xs uppercase tracking-[0.28em] text-slate-400">Newest drop</div>
            {newest[0] ? (
              <div className="mt-4 space-y-3">
                <div className="font-display text-2xl font-bold text-white">{newest[0].title}</div>
                <div className="text-sm text-slate-300">{newest[0].description || 'Fresh release ready for spotlight coverage.'}</div>
                <div className="flex flex-wrap gap-2 text-xs uppercase tracking-[0.24em] text-slate-400">
                  <span>{newest[0].genre}</span>
                  <span>•</span>
                  <span>{newest[0].platform}</span>
                  <span>•</span>
                  <span>{formatPrice(newest[0].price)}</span>
                </div>
              </div>
            ) : (
              <div className="mt-4 text-sm text-slate-400">No games available yet.</div>
            )}
          </div>
        </div>
      </section>

      <section className="space-y-4 reveal-up">
        <div className="flex items-end justify-between gap-4">
          <div>
            <h2 className="section-heading">Featured on the shelf</h2>
            <p className="mt-1 text-sm text-slate-400">Hand-picked listings with an App Store-style presentation.</p>
          </div>
          <Link to="/catalog" className="text-sm font-semibold text-accent hover:text-white">
            View all
          </Link>
        </div>
        {loading ? (
          <div className="surface-card text-slate-300">Loading games...</div>
        ) : error ? (
          <div className="surface-card text-warm">{error}</div>
        ) : featured.length ? (
          <div className="grid gap-5 md:grid-cols-2 xl:grid-cols-3">
            {featured.map((game) => (
              <div key={game.id} className="reveal-up">
                <GameCard game={game} />
              </div>
            ))}
          </div>
        ) : (
          <div className="empty-state">No games found.</div>
        )}
      </section>
    </div>
  );
}