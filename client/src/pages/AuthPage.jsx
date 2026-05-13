import React from 'react';
import { useAuth } from '../context/AuthContext';
import { Auth } from '@supabase/auth-ui-react';
import { ThemeSupa } from '@supabase/auth-ui-shared';
import { supabase } from '../lib/supabase';

export default function AuthPage() {
  const { user } = useAuth();

  return (
    <div className="grid gap-6 xl:grid-cols-[1.05fr_0.95fr]">
      <section className="hero-panel page-surface relative overflow-hidden p-8 sm:p-10">
        <div className="absolute inset-0 bg-[radial-gradient(circle_at_top_left,rgba(61,214,198,0.12),transparent_30%),radial-gradient(circle_at_bottom_right,rgba(108,140,255,0.12),transparent_28%)]" />
        <div className="absolute -right-8 top-10 h-40 w-40 rounded-full bg-accent/10 blur-3xl float-slow" />
        <div className="absolute bottom-0 left-10 h-56 w-56 rounded-full bg-accent2/10 blur-3xl float-slower" />
        <div className="relative grid gap-6">
          <div className="max-w-xl reveal-up">
            <div className="hero-badge">
              <span className="h-2 w-2 rounded-full bg-accent" />
              Supabase OAuth
            </div>
            <h1 className="mt-4 font-display text-4xl font-bold text-white sm:text-5xl">Sign in with Google or GitHub to manage your storefront experience.</h1>
            <p className="mt-4 max-w-md text-sm leading-6 text-slate-300">
              Supabase handles the OAuth handshake, then the app exchanges that session for the existing backend JWT so your catalog, library, developer tools, and admin workspace keep working.
            </p>
          </div>

          <div className="grid gap-4 sm:grid-cols-3">
            <div className="metric-card reveal-delay-1">
              <div className="text-xs uppercase tracking-[0.24em] text-slate-400">Session</div>
              <div className="mt-2 text-lg font-semibold text-white">OAuth-backed</div>
              <p className="mt-1 text-sm text-slate-400">Google and GitHub logins land in the same backend identity flow.</p>
            </div>
            <div className="metric-card reveal-delay-2">
              <div className="text-xs uppercase tracking-[0.24em] text-slate-400">Routes</div>
              <div className="mt-2 text-lg font-semibold text-white">Role aware</div>
              <p className="mt-1 text-sm text-slate-400">Users land on pages that match their role.</p>
            </div>
            <div className="metric-card reveal-delay-3">
              <div className="text-xs uppercase tracking-[0.24em] text-slate-400">Flow</div>
              <div className="mt-2 text-lg font-semibold text-white">Less friction</div>
              <p className="mt-1 text-sm text-slate-400">No local registration form, just an identity provider.</p>
            </div>
          </div>

          <div className="space-y-4 text-sm text-slate-300">
            <div className="rounded-3xl border border-white/10 bg-black/20 p-4">Step 1: choose Google or GitHub.</div>
            <div className="rounded-3xl border border-white/10 bg-black/20 p-4">Step 2: Supabase returns the OAuth session and the app mints its existing backend token.</div>
            <div className="rounded-3xl border border-white/10 bg-black/20 p-4">Step 3: continue to catalog, wishlist, purchases, or workspace without a separate password.</div>
          </div>
          {user ? <div className="text-sm text-accent">Currently signed in as {user.username}.</div> : null}
        </div>
      </section>

      <section className="section-shell page-surface p-6 sm:p-8">
        <div className="mt-6">
          <div className="glass-panel p-4">
            <Auth
              supabaseClient={supabase}
              providers={[ 'google', 'github' ]}
              socialLayout="vertical"
              magicLink={true}
              appearance={{ theme: ThemeSupa }}
            />
          </div>
        </div>

        <div className="mt-6 rounded-3xl border border-white/10 bg-black/20 p-4 text-sm leading-6 text-slate-300">
          Supabase handles the sign-in UI and session flow. Log out first if you want to switch accounts, then choose a different provider account on the next sign-in.
        </div>
      </section>
    </div>
  );
}