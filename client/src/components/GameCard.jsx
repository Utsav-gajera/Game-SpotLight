import React from 'react';
import { Link } from 'react-router-dom';
import { formatPrice } from '../lib/formatters';
import { prefetchRoute } from '../lib/routePrefetch';

export default function GameCard({ game, compact = false, actions }) {
  const image = game.imageUrl || `https://images.unsplash.com/photo-1511512578047-dfb367046420?auto=format&fit=crop&w=1200&q=80`;
  const subtitle = game.description || 'Open for details, similar games, and library actions.';
  const metadata = [game.releaseDate, game.version].filter(Boolean).join(' • ') || 'New release';
  const gamePath = `/games/${game.id}`;
  const prefetchGameRoute = () => prefetchRoute(gamePath);
  const downloads = game.totalDownloads ? (game.totalDownloads > 1000000 ? `${(game.totalDownloads / 1000000).toFixed(1)}M` : game.totalDownloads > 1000 ? `${(game.totalDownloads / 1000).toFixed(1)}K` : String(game.totalDownloads)) : '0';

  return (
    <article className="game-card group relative overflow-hidden rounded-[20px]">
      {/* Background glow effect */}
      <div className="absolute -inset-0.5 rounded-[20px] bg-gradient-to-br from-accent/40 via-accent2/20 to-warm/20 opacity-0 blur-xl transition-all duration-500 group-hover:opacity-100 -z-10" />
      
      {/* Card container with border and glass effect */}
      <div className="relative h-full rounded-[20px] border border-white/15 bg-gradient-to-br from-white/8 via-white/3 to-transparent backdrop-blur-sm overflow-hidden shadow-[0_8px_32px_rgba(0,0,0,0.3)] transition-all duration-500 group-hover:shadow-[0_32px_64px_rgba(0,0,0,0.6)] group-hover:border-accent/40">
        
        <Link
          to={gamePath}
          onMouseEnter={prefetchGameRoute}
          onFocus={prefetchGameRoute}
          onTouchStart={prefetchGameRoute}
          className="block focus:outline-none"
        >
          <div className="relative aspect-[16/9] overflow-hidden">
            <img src={image} alt={game.title} className="h-full w-full object-cover transition-transform duration-700 group-hover:scale-125" />
            
            {/* Multi-layer gradient overlays */}
            <div className="absolute inset-0 bg-gradient-to-t from-ink/95 via-ink/40 to-transparent" />
            <div className="absolute inset-0 bg-gradient-to-r from-black/20 via-transparent to-transparent opacity-0 transition-opacity duration-500 group-hover:opacity-100" />
            <div className="absolute inset-0 bg-[radial-gradient(ellipse_200%_100%_at_50%_0%,rgba(168,85,247,0.15),transparent_60%)] opacity-0 transition-opacity duration-500 group-hover:opacity-100" />
            
            {/* Animated top stripe */}
            <div className="absolute inset-x-0 top-0 h-0.5 bg-gradient-to-r from-transparent via-accent to-transparent opacity-0 transition-all duration-500 group-hover:opacity-100 group-hover:h-1" />
            
            {/* Top-left badges container */}
            <div className="absolute left-0 top-0 p-3 sm:p-4 flex flex-col gap-2">
              {/* Genre badge */}
            {!compact && game.genre ? (
              <div className="inline-flex w-fit items-center gap-1.5 rounded-full bg-gradient-to-r from-accent/80 to-accent2/70 border border-accent/60 px-3.5 py-1.5 text-[11px] font-semibold uppercase tracking-[0.3em] text-white shadow-lg backdrop-blur-md transition-all duration-300 group-hover:from-accent group-hover:to-accent2 group-hover:border-accent group-hover:shadow-[0_8px_24px_rgba(0,200,200,0.4)]">
                <span className="h-1.5 w-1.5 rounded-full bg-white animate-pulse" />
                {game.genre}
              </div>
            ) : null}
            
            {/* Platform badge */}
            {!compact && game.platform ? (
              <div className="inline-flex w-fit rounded-full bg-black/60 border border-white/20 px-3 py-1 text-[10px] font-medium uppercase tracking-[0.25em] text-slate-200 backdrop-blur-md transition-all duration-300 group-hover:bg-black/70 group-hover:border-white/40">
                {game.platform}
              </div>
            ) : null}
          </div>
          
          {/* Top-right price badge */}
          {!compact ? (
            <div className="absolute right-0 top-0 p-3 sm:p-4">
              <div className="rounded-2xl bg-gradient-to-br from-accent/90 to-accent2/80 border border-accent/50 px-4 sm:px-5 py-3 sm:py-3.5 backdrop-blur-xl shadow-lg transition-all duration-300 group-hover:from-accent group-hover:to-accent2 group-hover:shadow-[0_12px_32px_rgba(0,200,200,0.4)]">
                <div className="text-[9px] uppercase tracking-[0.35em] font-bold text-cyan-100/90">Price</div>
                <div className="mt-1 text-lg sm:text-xl font-black text-white">{formatPrice(game.price)}</div>
              </div>
            </div>
          ) : null}
            
            {/* Bottom content area */}
            <div className="absolute bottom-0 left-0 right-0 bg-gradient-to-t from-ink/98 via-ink/80 to-transparent p-4 sm:p-5 transition-all duration-500 group-hover:via-ink/90">
              <div className="flex flex-col gap-2">
                <h3 className={`game-title font-display font-black leading-tight text-white transition-all duration-300 ${
                  compact ? 'text-base' : 'text-xl sm:text-2xl'
                } group-hover:text-accent drop-shadow-lg`}>
                  {game.title}
                </h3>
                {!compact ? (
                  <p className="line-clamp-2 text-xs sm:text-sm leading-4 sm:leading-5 text-slate-300 font-medium transition-colors duration-300 group-hover:text-slate-100">
                    {subtitle}
                  </p>
                ) : null}
              </div>
            </div>
            
            {/* Download badge - bottom right */}
            {!compact && game.totalDownloads ? (
              <div className="absolute right-0 bottom-0 p-3 sm:p-4">
                <div className="inline-flex items-center gap-1.5 rounded-full bg-black/70 border border-white/20 px-3.5 py-1.5 text-[10px] font-semibold uppercase tracking-[0.24em] text-slate-200 backdrop-blur-md transition-all duration-300 group-hover:bg-black/80 group-hover:border-white/40">
                  <svg className="h-3.5 w-3.5 text-accent" fill="currentColor" viewBox="0 0 20 20">
                    <path fillRule="evenodd" d="M4 4a2 2 0 00-2 2v4a1 1 0 001 1h12a1 1 0 001-1V6a2 2 0 00-2-2H4zm12 12H4a2 2 0 01-2-2v-4a1 1 0 00-1-1H0a1 1 0 001 1v4a4 4 0 004 4h12a1 1 0 001-1h1a1 1 0 00-1-1z" clipRule="evenodd" />
                  </svg>
                  {downloads}
                </div>
              </div>
            ) : null}
          </div>
        </Link>
        
        {/* Footer section */}
        {!compact ? (
          <div className="flex flex-col gap-2 border-t border-white/10 bg-gradient-to-r from-white/5 to-transparent px-4 py-3 sm:px-5 sm:py-4 transition-all duration-300 group-hover:from-white/10">
            {/* Metadata row */}
            <div className="flex flex-wrap items-center justify-between gap-3">
              <div className="flex flex-wrap items-center gap-2">
                <span className="text-xs uppercase tracking-[0.26em] font-semibold text-slate-400 transition-colors duration-300 group-hover:text-slate-300">
                  {metadata}
                </span>
              </div>
              
              {/* Age rating badge */}
              {game.ageRating ? (
                <div className="inline-flex items-center justify-center rounded-full bg-gradient-to-br from-purple-500/20 to-pink-500/20 border border-purple-400/40 px-2.5 py-1 text-xs font-bold uppercase tracking-[0.24em] text-purple-300 transition-all duration-300 group-hover:from-purple-500/40 group-hover:to-pink-500/30 group-hover:border-purple-400/60">
                  {game.ageRating}
                </div>
              ) : null}
            </div>
            
            {/* Actions row */}
            {actions ? (
              <div className="mt-1 flex items-center gap-2">
                {actions}
              </div>
            ) : null}
          </div>
        ) : null}
      </div>
    </article>
  );
}