import React from 'react';
import { Link } from 'react-router-dom';

function formatPrice(price) {
  if (price === null || price === undefined || price === '') {
    return 'Free';
  }
  const numeric = Number(price);
  if (Number.isNaN(numeric)) {
    return price;
  }
  return `$${numeric.toFixed(2)}`;
}

export default function GameCard({ game, compact = false, actions }) {
  const image = game.imageUrl || `https://images.unsplash.com/photo-1511512578047-dfb367046420?auto=format&fit=crop&w=1200&q=80`;
  const subtitle = game.description || 'Premium game listing with rich metadata, screenshots, and quick actions.';
  const metadata = [game.releaseDate, game.version].filter(Boolean).join(' • ') || 'New release';

  return (
    <article className="group overflow-hidden rounded-[1.9rem] border border-white/10 bg-panel/85 shadow-glow backdrop-blur transition duration-500 hover:-translate-y-1 hover:border-accent/30 hover:shadow-[0_30px_90px_rgba(0,0,0,0.5)]">
      <Link to={`/games/${game.id}`} className="block focus:outline-none">
        <div className="relative aspect-[16/10] overflow-hidden">
          <img src={image} alt={game.title} className="h-full w-full object-cover transition duration-700 group-hover:scale-110" />
          <div className="absolute inset-0 bg-[radial-gradient(circle_at_top,rgba(255,255,255,0.16),transparent_20%),linear-gradient(180deg,rgba(4,17,31,0.05),rgba(4,17,31,0.92))] transition duration-500 group-hover:opacity-95" />
          <div className="absolute inset-x-0 top-0 h-1 bg-gradient-to-r from-accent via-accent2 to-warm opacity-0 transition duration-500 group-hover:opacity-100" />
          <div className="absolute left-4 top-4 flex flex-wrap gap-2">
            {game.genre ? <span className="hero-badge bg-black/40 text-white">{game.genre}</span> : null}
            {game.platform ? <span className="hero-badge">{game.platform}</span> : null}
          </div>
          <div className="absolute right-4 top-4 rounded-2xl border border-white/10 bg-black/35 px-3 py-2 text-right backdrop-blur-xl">
            <div className="text-[10px] uppercase tracking-[0.28em] text-slate-300">Price</div>
            <div className="text-sm font-semibold text-white">{formatPrice(game.price)}</div>
          </div>
          <div className="absolute bottom-0 left-0 right-0 border-t border-white/10 bg-gradient-to-t from-ink via-ink/80 to-transparent p-4 sm:p-5">
            <div className="flex items-end justify-between gap-4">
              <div className="min-w-0">
                <h3 className={`font-display ${compact ? 'text-lg' : 'text-xl'} font-bold leading-tight text-white`}>
                  {game.title}
                </h3>
                <p className="mt-2 line-clamp-2 text-sm leading-6 text-slate-300">
                  {subtitle}
                </p>
              </div>
              <div className="hidden shrink-0 rounded-2xl border border-white/10 bg-white/8 px-3 py-2 text-right backdrop-blur sm:block">
                <div className="text-[10px] uppercase tracking-[0.3em] text-slate-300">Details</div>
                <div className="text-xs font-semibold uppercase tracking-[0.2em] text-white">View game</div>
              </div>
            </div>
          </div>
        </div>
      </Link>
      <div className="flex flex-wrap items-center justify-between gap-3 px-4 py-4 sm:px-5">
        <div className="flex flex-wrap items-center gap-2 text-xs uppercase tracking-[0.24em] text-slate-400">
          <span>{metadata}</span>
        </div>
        {actions ? <div className="flex items-center gap-2">{actions}</div> : null}
      </div>
    </article>
  );
}