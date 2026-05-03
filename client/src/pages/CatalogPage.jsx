import React, { useEffect, useMemo, useState } from 'react';
import { useSearchParams } from 'react-router-dom';
import GameCard from '../components/GameCard';
import { api } from '../lib/api';
import { gamingGenres } from '../lib/genres';
import PrettySelect from '../components/PrettySelect';

const initialFilters = {
  title: '',
  genre: '',
  minPrice: '',
  maxPrice: ''
};

export default function CatalogPage() {
  const [searchParams, setSearchParams] = useSearchParams();
  const [filters, setFilters] = useState({
    ...initialFilters,
    title: searchParams.get('title') || '',
    genre: searchParams.get('genre') || '',
    minPrice: searchParams.get('minPrice') || '',
    maxPrice: searchParams.get('maxPrice') || ''
  });
  const [games, setGames] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  const loadGames = async (nextFilters) => {
    setLoading(true);
    setError('');
    try {
      const activeFilters = nextFilters || filters;
      const hasAnyFilter = Object.values(activeFilters).some(Boolean);
      const data = hasAnyFilter ? await api.games.filter(activeFilters) : await api.games.all();
      setGames(Array.isArray(data) ? data : []);
    } catch (err) {
      setError(err.message || 'Unable to load catalog.');
    } finally {
      setLoading(false);
    }
  };

  // Load games on initial mount
  useEffect(() => {
    loadGames(filters);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  // Watch URL search params and update filters/load games when they change
  useEffect(() => {
    const title = searchParams.get('title') || '';
    const genre = searchParams.get('genre') || '';
    const minPrice = searchParams.get('minPrice') || '';
    const maxPrice = searchParams.get('maxPrice') || '';

    const updatedFilters = { title, genre, minPrice, maxPrice };
    setFilters(updatedFilters);
    loadGames(updatedFilters);
  }, [searchParams]);

  // Use the canonical genres list for filter dropdowns and quick-genre buttons
  const genres = gamingGenres;

  const handleChange = (event) => {
    const { name, value } = event.target;
    setFilters((current) => ({ ...current, [name]: value }));
  };

  const handleSubmit = async (event) => {
    event.preventDefault();
    const next = { ...filters };
    setSearchParams(next, { replace: true });
    await loadGames(next);
  };

  const applyQuickGenre = async (genre) => {
    const next = { ...filters, genre };
    setFilters(next);
    setSearchParams(next, { replace: true });
    await loadGames(next);
  };

  const resetFilters = async () => {
    setFilters(initialFilters);
    setSearchParams({}, { replace: true });
    await loadGames(initialFilters);
  };

  return (
    <div className="grid gap-6 xl:grid-cols-[320px_1fr]">
      <aside className="space-y-6 xl:sticky xl:top-28 xl:h-fit">
        <section className="hero-panel page-surface">
          <div className="absolute inset-0 bg-[radial-gradient(circle_at_top_right,rgba(61,214,198,0.1),transparent_28%),radial-gradient(circle_at_bottom_left,rgba(108,140,255,0.08),transparent_24%)]" />
          <div className="relative">
            <div className="hero-badge">
              <span className="h-2 w-2 rounded-full bg-accent" />
              Catalog
            </div>
            <h1 className="mt-3 font-display text-3xl font-bold text-white">Browse the full storefront</h1>
            <p className="mt-2 text-sm leading-6 text-slate-300">
              Search by title, genre, or price. This layout keeps filters pinned and makes browsing feel closer to a premium storefront.
            </p>
            <button type="button" onClick={resetFilters} className="secondary-button mt-5 w-full">
              Reset filters
            </button>
          </div>
        </section>

        <section className="section-shell page-surface">
          <div className="text-xs uppercase tracking-[0.24em] text-slate-400">Filter deck</div>
          <form onSubmit={handleSubmit} className="mt-4 space-y-4">
            <div>
              <label className="label-text">Title</label>
              <input name="title" value={filters.title} onChange={handleChange} className="input-field" placeholder="Search games" />
            </div>
            <div>
              <label className="label-text">Genre</label>
              <PrettySelect
                options={[{ value: '', label: 'Any genre', description: 'Show all available releases' }, ...genres.map((genre) => ({ value: genre, label: genre }))]}
                value={filters.genre}
                onChange={(v) => setFilters((c) => ({ ...c, genre: v }))}
                placeholder="Any genre"
              />
            </div>
            <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-1">
              <div>
                <label className="label-text">Min price</label>
                <input name="minPrice" value={filters.minPrice} onChange={handleChange} className="input-field" placeholder="0" type="number" min="0" step="0.01" />
              </div>
              <div>
                <label className="label-text">Max price</label>
                <input name="maxPrice" value={filters.maxPrice} onChange={handleChange} className="input-field" placeholder="100" type="number" min="0" step="0.01" />
              </div>
            </div>
            <button type="submit" className="primary-button w-full">
              Apply filters
            </button>
          </form>

          <div className="metric-card mt-5">
            <div className="text-xs uppercase tracking-[0.24em] text-slate-400">Live summary</div>
            <div className="mt-2 text-2xl font-bold text-white">{loading ? '...' : games.length}</div>
            <div className="mt-1 text-sm text-slate-400">Games matching the current query.</div>
          </div>
        </section>

        <section className="section-shell page-surface">
          <div className="text-xs uppercase tracking-[0.24em] text-slate-400">Quick genres</div>
          <div className="mt-4 flex flex-wrap gap-2">
            {genres.slice(0, 10).map((genre) => (
              <button
                key={genre}
                type="button"
                onClick={() => applyQuickGenre(genre)}
                className="chip-button normal-case tracking-normal"
              >
                {genre}
              </button>
            ))}
          </div>
        </section>
      </aside>

      <section className="space-y-4">
        <div className="toolbar-shell justify-between">
          <div>
            <div className="toolbar-note">Results</div>
            <div className="mt-1 text-sm text-slate-300">The storefront updates in place as you refine the filter deck.</div>
          </div>
          <div className="status-pill">
            {loading ? 'Loading results...' : `${games.length} title${games.length === 1 ? '' : 's'} found`}
          </div>
        </div>

        {error ? <div className="surface-card text-warm">{error}</div> : null}

        {loading ? (
          <div className="surface-card text-slate-300">Loading catalog...</div>
        ) : games.length ? (
          <section className="grid gap-5 md:grid-cols-2 xl:grid-cols-2 2xl:grid-cols-3">
            {games.map((game) => (
              <div key={game.id} className="reveal-up">
                <GameCard game={game} compact />
              </div>
            ))}
          </section>
        ) : (
          <div className="empty-state">No results matched your filters.</div>
        )}
      </section>
    </div>
  );
}