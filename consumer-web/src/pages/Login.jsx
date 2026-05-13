import React, { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';

const API = 'http://localhost:8080';

export default function Login() {
  const [email, setEmail]       = useState('');
  const [password, setPassword] = useState('');
  const [error, setError]       = useState('');
  const [loading, setLoading]   = useState(false);
  const navigate                = useNavigate();

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    setLoading(true);
    try {
      const res  = await fetch(`${API}/api/auth/login`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ email, password }),
      });
      const json = await res.json();
      if (!res.ok || !json.success) throw new Error(json.message || 'Invalid credentials');
      localStorage.setItem('npy_consumer_session', JSON.stringify(json.data));
      navigate('/dashboard');          // change target when consumer dashboard exists
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div style={S.page}>
      <div style={S.blob1} />
      <div style={S.blob2} />

      <div style={S.card}>
        {/* Logo */}
        <Link to="/" style={S.logoRow}>
          <svg width="26" height="26" viewBox="0 0 24 24" fill="none">
            <defs>
              <linearGradient id="lg" x1="0" y1="0" x2="24" y2="24" gradientUnits="userSpaceOnUse">
                <stop offset="0%" stopColor="#3b82f6"/>
                <stop offset="100%" stopColor="#10b981"/>
              </linearGradient>
            </defs>
            <path d="M12 2L2 7l10 5 10-5-10-5zM2 17l10 5 10-5M2 12l10 5 10-5" stroke="url(#lg)" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"/>
          </svg>
          <span style={S.logoText}>NylePay</span>
        </Link>

        <h1 style={S.heading}>Welcome back</h1>
        <p style={S.sub}>Sign in to your NPY wallet</p>

        <form onSubmit={handleSubmit} style={S.form}>
          <div style={S.group}>
            <label style={S.label} htmlFor="email">Email</label>
            <input
              id="email" type="email" required
              style={S.input}
              value={email}
              onChange={e => setEmail(e.target.value)}
              placeholder="you@example.com"
              autoComplete="email"
            />
          </div>

          <div style={S.group}>
            <label style={S.label} htmlFor="password">Password</label>
            <input
              id="password" type="password" required
              style={S.input}
              value={password}
              onChange={e => setPassword(e.target.value)}
              placeholder="Your password"
              autoComplete="current-password"
            />
          </div>

          {error && <div style={S.error}>{error}</div>}

          <button type="submit" disabled={loading} style={S.btn}>
            {loading ? 'Signing in…' : 'Sign In'}
          </button>
        </form>

        <p style={S.foot}>
          Don't have an account?{' '}
          <Link to="/register" style={S.footLink}>Create one</Link>
        </p>

        <div style={S.divider}/>
        <p style={{ ...S.foot, marginTop: 0 }}>
          Are you a business?{' '}
          <a href="http://localhost:5174" style={S.footLink}>Go to Merchant Portal</a>
        </p>
      </div>
    </div>
  );
}

const S = {
  page: {
    minHeight: '100vh',
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
    padding: '2rem 1rem',
    position: 'relative',
    overflow: 'hidden',
  },
  blob1: {
    position: 'fixed', top: '-200px', right: '-100px',
    width: '500px', height: '500px', borderRadius: '50%',
    background: '#3b82f6', filter: 'blur(130px)', opacity: 0.1,
    pointerEvents: 'none',
  },
  blob2: {
    position: 'fixed', bottom: '-150px', left: '-100px',
    width: '450px', height: '450px', borderRadius: '50%',
    background: '#10b981', filter: 'blur(130px)', opacity: 0.1,
    pointerEvents: 'none',
  },
  card: {
    width: '100%', maxWidth: '420px',
    background: 'rgba(13,21,38,0.85)',
    border: '1px solid rgba(255,255,255,0.07)',
    borderRadius: '20px',
    padding: '2.5rem',
    backdropFilter: 'blur(20px)',
    WebkitBackdropFilter: 'blur(20px)',
    boxShadow: '0 8px 40px rgba(0,0,0,0.5)',
    position: 'relative',
    zIndex: 1,
  },
  logoRow: {
    display: 'flex', alignItems: 'center', gap: '0.5rem',
    marginBottom: '2rem', textDecoration: 'none',
  },
  logoText: {
    fontFamily: "'Outfit', sans-serif",
    fontWeight: 800, fontSize: '1.4rem',
    background: 'linear-gradient(135deg,#3b82f6,#10b981)',
    WebkitBackgroundClip: 'text', WebkitTextFillColor: 'transparent',
    backgroundClip: 'text',
  },
  heading: {
    fontFamily: "'Outfit', sans-serif",
    fontSize: '1.6rem', fontWeight: 700,
    color: '#f1f5f9', marginBottom: '0.25rem',
  },
  sub: { color: '#8b9dc3', fontSize: '0.9rem', marginBottom: '2rem' },
  form: { display: 'flex', flexDirection: 'column', gap: 0 },
  group: { marginBottom: '1.1rem' },
  label: { display: 'block', color: '#8b9dc3', fontSize: '0.8rem', fontWeight: 500, marginBottom: '0.35rem' },
  input: {
    width: '100%', padding: '0.75rem 1rem',
    background: '#060b18', border: '1px solid rgba(255,255,255,0.08)',
    borderRadius: '8px', color: '#f1f5f9',
    fontFamily: "'Inter', sans-serif", fontSize: '0.9375rem',
    outline: 'none', boxSizing: 'border-box',
    transition: 'border-color 0.2s',
  },
  error: {
    padding: '0.75rem 1rem',
    background: 'rgba(239,68,68,0.08)',
    border: '1px solid rgba(239,68,68,0.25)',
    borderRadius: '8px', color: '#f87171',
    fontSize: '0.8125rem', marginBottom: '1rem', lineHeight: 1.5,
  },
  btn: {
    width: '100%', padding: '0.875rem',
    background: 'linear-gradient(135deg,#3b82f6,#10b981)',
    border: 'none', borderRadius: '8px',
    color: '#fff', fontFamily: "'Outfit', sans-serif",
    fontWeight: 700, fontSize: '0.9375rem',
    cursor: 'pointer', marginTop: '0.25rem',
    transition: 'opacity 0.2s',
  },
  foot: { textAlign: 'center', color: '#556a8a', fontSize: '0.8125rem', marginTop: '1.5rem' },
  footLink: { color: '#3b82f6', fontWeight: 600 },
  divider: { height: '1px', background: 'rgba(255,255,255,0.06)', margin: '1.25rem 0' },
};
