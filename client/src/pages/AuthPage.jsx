import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';

const roleOptions = [
  { label: 'Normal user', value: 'NORMAL_USER' },
  { label: 'Developer', value: 'DEVELOPER' }
];

function getLandingPath(role) {
  if (role === 'DEVELOPER' || role === 'ADMIN') {
    return '/workspace/developer';
  }

  return '/workspace/wishlist';
}

export default function AuthPage() {
  const navigate = useNavigate();
  const { login, register, user } = useAuth();
  const [mode, setMode] = useState('login');
  const [message, setMessage] = useState('');
  const [busy, setBusy] = useState(false);
  const [loginForm, setLoginForm] = useState({ username: '', password: '' });
  const [registerForm, setRegisterForm] = useState({ username: '', email: '', password: '', role: 'NORMAL_USER' });

  const updateForm = (setter) => (event) => {
    const { name, value } = event.target;
    setter((current) => ({ ...current, [name]: value }));
  };

  const handleLogin = async (event) => {
    event.preventDefault();
    setBusy(true);
    setMessage('');
    try {
      const response = await login(loginForm);
      setMessage('Login successful. Redirecting to your library...');
      const role = response?.user?.role || response?.user?.roles?.[0];
      navigate(getLandingPath(role));
    } catch (error) {
      setMessage(error.message || 'Login failed.');
    } finally {
      setBusy(false);
    }
  };

  const handleRegister = async (event) => {
    event.preventDefault();
    setBusy(true);
    setMessage('');
    try {
      const username = registerForm.username.trim();
      const email = registerForm.email.trim();
      const password = registerForm.password;

      if (!username) {
        throw new Error('Username is required.');
      }
      if (password.length < 8) {
        throw new Error('Password must be at least 8 characters long.');
      }
      if (email && !email.includes('@')) {
        throw new Error('Enter a valid email address or leave it blank.');
      }

      const response = await register(registerForm);
      setMessage(typeof response === 'string' ? response : 'Registration successful. You can now log in.');
      setLoginForm({ username: username, password: '' });
      setRegisterForm({ username: '', email: '', password: '', role: 'NORMAL_USER' });
      setMode('login');
    } catch (error) {
      setMessage(error.message || 'Registration failed.');
    } finally {
      setBusy(false);
    }
  };

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
              Authentication
            </div>
            <h1 className="mt-4 font-display text-4xl font-bold text-white sm:text-5xl">Sign in to manage your storefront experience.</h1>
            <p className="mt-4 max-w-md text-sm leading-6 text-slate-300">
              The backend uses session-based auth, so the frontend keeps your login state while you move across the catalog, library, developer tools, and admin workspace.
            </p>
          </div>

          <div className="grid gap-4 sm:grid-cols-3">
            <div className="metric-card reveal-delay-1">
              <div className="text-xs uppercase tracking-[0.24em] text-slate-400">Session</div>
              <div className="mt-2 text-lg font-semibold text-white">Persistent</div>
              <p className="mt-1 text-sm text-slate-400">You stay signed in while you move through the app.</p>
            </div>
            <div className="metric-card reveal-delay-2">
              <div className="text-xs uppercase tracking-[0.24em] text-slate-400">Routes</div>
              <div className="mt-2 text-lg font-semibold text-white">Role aware</div>
              <p className="mt-1 text-sm text-slate-400">Users land on pages that match their role.</p>
            </div>
            <div className="metric-card reveal-delay-3">
              <div className="text-xs uppercase tracking-[0.24em] text-slate-400">Flow</div>
              <div className="mt-2 text-lg font-semibold text-white">Faster</div>
              <p className="mt-1 text-sm text-slate-400">Less friction, more storefront focus.</p>
            </div>
          </div>

          <div className="space-y-4 text-sm text-slate-300">
            <div className="rounded-3xl border border-white/10 bg-black/20 p-4">Step 1: sign in or create an account.</div>
            <div className="rounded-3xl border border-white/10 bg-black/20 p-4">Step 2: the app routes you into the correct area for your role.</div>
            <div className="rounded-3xl border border-white/10 bg-black/20 p-4">Step 3: continue to catalog, wishlist, purchases, or workspace without extra setup.</div>
          </div>
          {user ? <div className="text-sm text-accent">Currently signed in as {user.username}.</div> : null}
        </div>
      </section>

      <section className="section-shell page-surface p-6 sm:p-8">
        <div className="flex gap-2 rounded-full border border-white/10 bg-white/5 p-1">
          <button type="button" onClick={() => setMode('login')} className={`flex-1 rounded-full px-4 py-2 text-sm font-semibold transition ${mode === 'login' ? 'bg-accent text-ink' : 'text-slate-300'}`}>
            Sign in
          </button>
          <button type="button" onClick={() => setMode('register')} className={`flex-1 rounded-full px-4 py-2 text-sm font-semibold transition ${mode === 'register' ? 'bg-accent text-ink' : 'text-slate-300'}`}>
            Create account
          </button>
        </div>

        {message ? <div className="mt-4 rounded-2xl border border-white/10 bg-white/5 p-4 text-sm text-slate-200 reveal-up">{message}</div> : null}

        {mode === 'login' ? (
          <form onSubmit={handleLogin} className="mt-6 space-y-4">
            <div>
              <label className="label-text">Username</label>
              <input name="username" value={loginForm.username} onChange={updateForm(setLoginForm)} className="input-field" placeholder="Your username" />
            </div>
            <div>
              <label className="label-text">Password</label>
              <input type="password" name="password" value={loginForm.password} onChange={updateForm(setLoginForm)} className="input-field" placeholder="Your password" />
            </div>
            <button type="submit" disabled={busy} className="primary-button w-full">
              {busy ? 'Signing in...' : 'Sign in'}
            </button>
          </form>
        ) : (
          <form onSubmit={handleRegister} className="mt-6 space-y-4">
            <div>
              <label className="label-text">Username</label>
              <input name="username" value={registerForm.username} onChange={updateForm(setRegisterForm)} className="input-field" placeholder="Choose a username" />
            </div>
            <div>
              <label className="label-text">Email</label>
              <input type="email" name="email" value={registerForm.email} onChange={updateForm(setRegisterForm)} className="input-field" placeholder="you@example.com" />
            </div>
            <div>
              <label className="label-text">Password</label>
              <input type="password" name="password" value={registerForm.password} onChange={updateForm(setRegisterForm)} className="input-field" placeholder="Create a password" />
            </div>
            <div>
              <label className="label-text">Role</label>
              <select name="role" value={registerForm.role} onChange={updateForm(setRegisterForm)} className="input-field select-field">
                {roleOptions.map((option) => (
                  <option key={option.value} value={option.value}>{option.label}</option>
                ))}
              </select>
            </div>
            <button type="submit" disabled={busy} className="primary-button w-full">
              {busy ? 'Creating account...' : 'Create account'}
            </button>
          </form>
        )}
      </section>
    </div>
  );
}