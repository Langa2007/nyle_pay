import React, { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import ThemeToggle from '../components/ThemeToggle';

export default function Landing() {
  const [mode, setMode] = useState('signin');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [fullName, setFullName] = useState('');
  const [error, setError] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const { login, signup } = useAuth();
  const navigate = useNavigate();

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    setSubmitting(true);
    try {
      if (mode === 'signup') {
        await signup(email, password, fullName);
      } else {
        await login(email, password);
      }
      navigate('/dashboard');
    } catch (err) {
      setError(err.message || 'Something went wrong');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="landing-page">
      <div className="landing-blob landing-blob-1" />
      <div className="landing-blob landing-blob-2" />

      <nav className="landing-nav">
        <div className="landing-nav-logo">
          <span className="text-gradient" style={{ fontWeight: 800, fontSize: '1.375rem' }}>NylePay</span>
          <span style={{ color: 'var(--text-muted)', fontWeight: 500, marginLeft: '.5rem', fontSize: '.875rem' }}>for Business</span>
        </div>
        <div className="landing-nav-links" style={{ display: 'flex', alignItems: 'center', gap: '2rem' }}>
          <a href="#features">Features</a>
          <a href="#pricing">Pricing</a>
          <a href="#docs">Docs</a>
          <ThemeToggle />
        </div>
      </nav>

      {/* ── Main Split Layout ── */}
      <main className="landing-main">
        {/* Left: Value Proposition */}
        <section className="landing-left animate-fade-up">
          <div className="landing-badge">
            Real-Time Settlement — No More T+1
          </div>
          <h1 className="landing-title">
            Accept Payments.<br />
            <span className="text-gradient">Get Paid Instantly.</span>
          </h1>
          <p className="landing-subtitle">
            NylePay is the payment gateway built for African businesses. Get your API keys in minutes, integrate M-Pesa, Cards, and Crypto — and receive settlement the moment your customer pays.
          </p>

          {/* Feature Pills */}
          <div className="landing-features">
            <div className="feature-pill">
              <span className="feature-icon"></span>
              <div>
                <strong>M-Pesa, Cards & Crypto</strong>
                <p>One integration, every payment method.</p>
              </div>
            </div>
            <div className="feature-pill">
              <span className="feature-icon"></span>
              <div>
                <strong>Instant Payouts</strong>
                <p>Real-time settlement to your M-Pesa or Bank.</p>
              </div>
            </div>
            <div className="feature-pill">
              <span className="feature-icon"></span>
              <div>
                <strong>Bank-Grade Security</strong>
                <p>AES-256 encryption. HMAC webhook signatures.</p>
              </div>
            </div>
            <div className="feature-pill">
              <span className="feature-icon"></span>
              <div>
                <strong>1.5% Flat Fee</strong>
                <p>No hidden charges. No setup costs.</p>
              </div>
            </div>
          </div>
        </section>

        <section className="landing-right animate-fade-up delay-2">
          <div className="auth-card glass-panel">
            <div className="auth-tabs">
              <button
                className={`auth-tab ${mode === 'signin' ? 'auth-tab-active' : ''}`}
                onClick={() => { setMode('signin'); setError(''); }}
              >
                Sign In
              </button>
              <button
                className={`auth-tab ${mode === 'signup' ? 'auth-tab-active' : ''}`}
                onClick={() => { setMode('signup'); setError(''); }}
              >
                Sign Up
              </button>
            </div>

            <form onSubmit={handleSubmit} className="auth-form">
              {mode === 'signup' && (
                <div className="form-group">
                  <label className="form-label" htmlFor="fullName">Full Name</label>
                  <input
                    id="fullName"
                    className="form-input"
                    type="text"
                    required
                    value={fullName}
                    onChange={(e) => setFullName(e.target.value)}
                    placeholder="your name"
                    autoComplete="name"
                  />
                </div>
              )}

              <div className="form-group">
                <label className="form-label" htmlFor="email">Business Email</label>
                <input
                  id="email"
                  className="form-input"
                  type="email"
                  required
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                  placeholder="you@company.com"
                  autoComplete="email"
                />
              </div>

              <div className="form-group">
                <label className="form-label" htmlFor="password">Password</label>
                <input
                  id="password"
                  className="form-input"
                  type="password"
                  required
                  minLength={8}
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                  placeholder="Minimum 8 characters"
                  autoComplete={mode === 'signup' ? 'new-password' : 'current-password'}
                />
              </div>

              {error && <p className="form-error" style={{ marginBottom: '1rem' }}>{error}</p>}

              <button
                type="submit"
                className="btn-primary"
                disabled={submitting}
                style={{ width: '100%', padding: '.875rem' }}
              >
                {submitting
                  ? 'Please wait…'
                  : mode === 'signup'
                    ? 'Create Account'
                    : 'Sign In'
                }
              </button>
            </form>

            <p className="auth-footer">
              {mode === 'signin'
                ? <>Don't have an account? <button className="auth-switch-link" onClick={() => setMode('signup')}>Sign Up</button></>
                : <>Already have an account? <button className="auth-switch-link" onClick={() => setMode('signin')}>Sign In</button></>
              }
            </p>
          </div>
        </section>
      </main>

      <style>{`
        .landing-page {
          min-height: 100vh;
          position: relative;
          overflow: hidden;
        }

        /* Glowing blobs */
        .landing-blob {
          position: absolute;
          border-radius: 50%;
          filter: blur(120px);
          opacity: .15;
          pointer-events: none;
          z-index: 0;
        }
        .landing-blob-1 {
          width: 600px; height: 600px;
          background: var(--brand-blue);
          top: -200px; right: -100px;
        }
        .landing-blob-2 {
          width: 500px; height: 500px;
          background: var(--brand-green);
          bottom: -150px; left: -100px;
        }

        /* Nav */
        .landing-nav {
          display: flex;
          justify-content: space-between;
          align-items: center;
          padding: 1.25rem 5%;
          position: relative;
          z-index: 10;
        }
        .landing-nav-logo { display: flex; align-items: baseline; }
        .landing-nav-links { display: flex; gap: 2rem; }
        .landing-nav-links a {
          color: var(--text-secondary);
          font-weight: 500;
          font-size: .9375rem;
          transition: color .2s;
        }
        .landing-nav-links a:hover { color: var(--text-primary); }

        /* Main */
        .landing-main {
          display: flex;
          align-items: center;
          gap: 5rem;
          padding: 4rem 5% 6rem;
          min-height: calc(100vh - 80px);
          position: relative;
          z-index: 10;
        }

        /* Left */
        .landing-left { flex: 1.2; max-width: 620px; }
        .landing-badge {
          display: inline-block;
          padding: .375rem 1rem;
          border-radius: var(--radius-full);
          background: rgba(16,185,129,.1);
          color: var(--brand-green);
          font-size: .8125rem;
          font-weight: 600;
          margin-bottom: 1.5rem;
          border: 1px solid rgba(16,185,129,.15);
        }
        .landing-title {
          font-size: 3.5rem;
          letter-spacing: -.02em;
          margin-bottom: 1.25rem;
          line-height: 1.1;
        }
        .landing-subtitle {
          font-size: 1.125rem;
          color: var(--text-secondary);
          margin-bottom: 2.5rem;
          max-width: 520px;
          line-height: 1.7;
        }

        /* Feature Pills */
        .landing-features {
          display: grid;
          grid-template-columns: 1fr 1fr;
          gap: 1rem;
        }
        .feature-pill {
          display: flex;
          align-items: flex-start;
          gap: .75rem;
          padding: 1rem;
          border-radius: var(--radius-md);
          background: rgba(255,255,255,.03);
          border: 1px solid var(--border-color);
          transition: background .2s;
        }
        .feature-pill:hover { background: rgba(255,255,255,.06); }
        .feature-pill .feature-icon { font-size: 1.5rem; line-height: 1; }
        .feature-pill strong {
          display: block;
          font-size: .875rem;
          margin-bottom: .125rem;
        }
        .feature-pill p {
          color: var(--text-muted);
          font-size: .75rem;
          line-height: 1.4;
          margin: 0;
        }

        /* Right / Auth Card */
        .landing-right { flex: 1; display: flex; justify-content: center; }
        .auth-card {
          width: 100%;
          max-width: 420px;
          padding: 2.5rem;
        }

        /* Auth Tabs */
        .auth-tabs {
          display: flex;
          border-radius: var(--radius-sm);
          background: var(--bg-primary);
          padding: .25rem;
          margin-bottom: 2rem;
        }
        .auth-tab {
          flex: 1;
          padding: .625rem;
          border: none;
          background: transparent;
          color: var(--text-secondary);
          font-family: var(--font-heading);
          font-weight: 600;
          font-size: .9375rem;
          cursor: pointer;
          border-radius: var(--radius-xs);
          transition: all .2s;
        }
        .auth-tab-active {
          background: var(--bg-tertiary);
          color: var(--text-primary);
          box-shadow: var(--shadow-sm);
        }

        .auth-form { display: flex; flex-direction: column; }

        .auth-footer {
          text-align: center;
          margin-top: 1.5rem;
          color: var(--text-muted);
          font-size: .8125rem;
        }
        .auth-switch-link {
          background: none;
          border: none;
          color: var(--brand-blue);
          font-weight: 600;
          cursor: pointer;
          font-size: .8125rem;
          font-family: inherit;
        }
        .auth-switch-link:hover { text-decoration: underline; }

        /* Responsive */
        @media (max-width: 900px) {
          .landing-main { flex-direction: column; gap: 3rem; padding-top: 2rem; }
          .landing-left { max-width: 100%; }
          .landing-title { font-size: 2.5rem; }
          .landing-features { grid-template-columns: 1fr; }
          .landing-nav-links { display: none; }
        }
      `}</style>
    </div>
  );
}
