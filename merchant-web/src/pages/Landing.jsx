import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import ThemeToggle from '../components/ThemeToggle';

const NAV_LINKS = ['Features', 'Pricing', 'Docs', 'Status'];

export default function Landing() {
  const [mode, setMode]             = useState('signin');
  const [email, setEmail]           = useState('');
  const [password, setPassword]     = useState('');
  const [fullName, setFullName]     = useState('');
  const [mpesa, setMpesa]           = useState('');
  const [error, setError]           = useState('');
  const [submitting, setSubmitting] = useState(false);
  const { login, signup }           = useAuth();
  const navigate                    = useNavigate();

  const submit = async (e) => {
    e.preventDefault();
    setError('');
    setSubmitting(true);
    try {
      if (mode === 'signup') await signup(email, password, fullName, mpesa, 'KE');
      else await login(email, password);
      navigate('/dashboard');
    } catch (err) {
      setError(err.message || 'Authentication failed. Please check your credentials.');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div style={S.page}>
      {/* ── Top navigation ── */}
      <nav style={S.nav}>
        <div style={S.navInner}>
          <div style={S.navLeft}>
            <a href="/" style={S.logo}>
              <div style={S.logoMark}>
                <svg width="18" height="18" viewBox="0 0 24 24" fill="none">
                  <path d="M12 2L2 7l10 5 10-5-10-5zM2 17l10 5 10-5M2 12l10 5 10-5" stroke="#fff" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"/>
                </svg>
              </div>
              <span style={S.logoName}>NylePay</span>
              <span style={S.logoSub}>Business</span>
            </a>
            <div style={S.navLinks}>
              {NAV_LINKS.map(l => (
                <a key={l} href="#" style={S.navLink}>{l}</a>
              ))}
            </div>
          </div>
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem' }}>
            <ThemeToggle />
          </div>
        </div>
      </nav>

      {/* ── Page content ── */}
      <main style={S.main}>
        {/* Left — value proposition */}
        <section style={S.left} className="animate-fade-up">
          {/* Trust badge */}
          <div style={S.trustBadge}>
            <span style={S.trustDot} />
            CBK-Regulated · PCI DSS SAQ-A · AML Screened
          </div>

          <h1 style={S.headline}>
            The payments platform<br />
            <span style={S.headlineBlue}>built for Africa</span>
          </h1>

          <p style={S.body}>
            Accept M-Pesa, Cards, and Crypto with a single API integration.
            Real-time settlement, HMAC-signed webhooks, and enterprise-grade
            security — so you can focus on building your product.
          </p>

          {/* Key metrics */}
          <div style={S.statsRow}>
            {[
              { val: '1.5%', label: 'Flat rate, all methods' },
              { val: 'T+0',  label: 'Real-time settlement' },
              { val: '99.9%', label: 'API uptime SLA' },
              { val: '<200ms', label: 'P99 response time' },
            ].map(s => (
              <div key={s.val} style={S.stat}>
                <div style={S.statVal}>{s.val}</div>
                <div style={S.statLabel}>{s.label}</div>
              </div>
            ))}
          </div>

          {/* Feature list */}
          <div style={S.features}>
            {[
              { icon: '⚡', title: 'Instant payouts', desc: 'M-Pesa and bank settlements land in seconds, not days.' },
              { icon: '🔑', title: 'API-first design', desc: 'RESTful JSON API with sandbox environment and test keys.' },
              { icon: '🔒', title: 'Enterprise security', desc: 'AES-256 encryption, HMAC webhooks, anomaly detection.' },
              { icon: '📊', title: 'Real-time analytics', desc: 'Live transaction feed, revenue charts, and dispute management.' },
            ].map(f => (
              <div key={f.title} style={S.feature}>
                <span style={S.featureIcon}>{f.icon}</span>
                <div>
                  <div style={S.featureTitle}>{f.title}</div>
                  <div style={S.featureDesc}>{f.desc}</div>
                </div>
              </div>
            ))}
          </div>
        </section>

        {/* Right — auth card */}
        <section style={S.right} className="animate-fade-up delay-200">
          <div style={S.card}>
            <div style={S.cardTop}>
              <h2 style={S.cardTitle}>{mode === 'signin' ? 'Sign in to your account' : 'Create your merchant account'}</h2>
              <p style={S.cardSub}>{mode === 'signin' ? 'Access your dashboard and API keys.' : 'Get API keys in under 5 minutes.'}</p>
            </div>

            {/* Mode switcher */}
            <div style={S.modeSwitcher}>
              {[['signin', 'Sign In'], ['signup', 'Sign Up']].map(([m, l]) => (
                <button
                  key={m}
                  style={{ ...S.modeBtn, ...(mode === m ? S.modeBtnActive : {}) }}
                  onClick={() => { setMode(m); setError(''); }}
                >
                  {l}
                </button>
              ))}
            </div>

            <form onSubmit={submit} style={{ display: 'flex', flexDirection: 'column', gap: '0.125rem' }}>
              {mode === 'signup' && (
                <div className="form-group">
                  <label className="form-label" htmlFor="fullName">Full Name</label>
                  <input id="fullName" className="form-input" type="text" required
                    value={fullName} onChange={e => setFullName(e.target.value)}
                    placeholder="Jane Wanjiru" autoComplete="name"
                  />
                </div>
              )}

              <div className="form-group">
                <label className="form-label" htmlFor="email">{mode === 'signup' ? 'Email Address' : 'Business Email'}</label>
                <input id="email" className="form-input" type="email" required
                  value={email} onChange={e => setEmail(e.target.value)}
                  placeholder="you@company.com" autoComplete="email"
                />
              </div>

              <div className="form-group">
                <label className="form-label" htmlFor="pass">Password</label>
                <input id="pass" className="form-input" type="password" required minLength={8}
                  value={password} onChange={e => setPassword(e.target.value)}
                  placeholder={mode === 'signup' ? 'At least 8 characters' : 'Your password'}
                  autoComplete={mode === 'signup' ? 'new-password' : 'current-password'}
                />
              </div>

              {mode === 'signup' && (
                <div className="form-group">
                  <label className="form-label" htmlFor="mpesa">M-Pesa Number</label>
                  <input id="mpesa" className="form-input" type="text" required
                    value={mpesa} onChange={e => setMpesa(e.target.value)}
                    placeholder="2547XXXXXXXX"
                  />
                  <p className="form-hint">Used for account verification and settlement payouts.</p>
                </div>
              )}

              {error && (
                <div className="alert alert-error" style={{ marginBottom: '0.75rem' }}>
                  <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" style={{ flexShrink: 0, marginTop: '1px' }}>
                    <circle cx="12" cy="12" r="10"/><path d="M12 8v4M12 16h.01"/>
                  </svg>
                  {error}
                </div>
              )}

              <button type="submit" className="btn-primary" disabled={submitting}
                style={{ width: '100%', padding: '0.75rem', fontSize: '0.9375rem', borderRadius: '8px', marginTop: '0.25rem' }}>
                {submitting
                  ? <><span className="spinner" /> Please wait…</>
                  : mode === 'signup' ? 'Create account' : 'Sign in to dashboard'
                }
              </button>
            </form>

            <p style={S.switchLine}>
              {mode === 'signin'
                ? <>{`Don't have an account? `}<button style={S.switchBtn} onClick={() => { setMode('signup'); setError(''); }}>Create one free</button></>
                : <>Already have an account? <button style={S.switchBtn} onClick={() => { setMode('signin'); setError(''); }}>Sign in</button></>
              }
            </p>

            {/* Social proof */}
            <div style={S.proofRow}>
              <div style={S.proofLock}>
                <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                  <rect x="3" y="11" width="18" height="11" rx="2"/><path d="M7 11V7a5 5 0 0110 0v4"/>
                </svg>
                256-bit TLS encryption
              </div>
              <div style={S.proofLock}>
                <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                  <path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"/>
                </svg>
                PCI DSS compliant
              </div>
            </div>
          </div>
        </section>
      </main>
    </div>
  );
}

/* ── Styles ── */
const S = {
  page: { minHeight: '100vh', background: '#f8fafc', display: 'flex', flexDirection: 'column' },
  nav: { background: '#fff', borderBottom: '1px solid #e2e8f0', position: 'sticky', top: 0, zIndex: 100 },
  navInner: { maxWidth: 1200, margin: '0 auto', padding: '0 2rem', display: 'flex', alignItems: 'center', justifyContent: 'space-between', height: '60px' },
  navLeft: { display: 'flex', alignItems: 'center', gap: '2.5rem' },
  logo: { display: 'flex', alignItems: 'center', gap: '0.6rem', textDecoration: 'none' },
  logoMark: { width: 32, height: 32, borderRadius: 8, background: 'linear-gradient(135deg,#1d4ed8,#2563eb)', display: 'flex', alignItems: 'center', justifyContent: 'center', boxShadow: '0 2px 8px rgba(37,99,235,.35)' },
  logoName: { fontWeight: 800, fontSize: '1.125rem', color: '#0f172a', letterSpacing: '-0.02em' },
  logoSub: { fontSize: '0.72rem', fontWeight: 700, color: '#2563eb', background: '#eff6ff', padding: '0.1rem 0.45rem', borderRadius: '4px', letterSpacing: '0.03em', textTransform: 'uppercase' },
  navLinks: { display: 'flex', gap: '0.125rem' },
  navLink: { color: '#475569', fontWeight: 500, fontSize: '0.875rem', padding: '0.375rem 0.625rem', borderRadius: '6px', transition: 'all 0.15s', textDecoration: 'none' },
  main: { flex: 1, maxWidth: 1200, margin: '0 auto', padding: '5rem 2rem 4rem', display: 'grid', gridTemplateColumns: '1fr 420px', gap: '5rem', alignItems: 'start', width: '100%' },
  left: { paddingTop: '0.5rem' },
  trustBadge: { display: 'inline-flex', alignItems: 'center', gap: '0.5rem', fontSize: '0.75rem', fontWeight: 600, color: '#059669', background: '#d1fae5', border: '1px solid #a7f3d0', padding: '0.3rem 0.75rem', borderRadius: '100px', marginBottom: '1.75rem', letterSpacing: '0.02em' },
  trustDot: { width: 6, height: 6, borderRadius: '50%', background: '#059669', animation: 'pulse-ring 2s ease infinite' },
  headline: { fontSize: 'clamp(2.25rem, 4vw, 3.25rem)', fontWeight: 800, lineHeight: 1.12, letterSpacing: '-0.03em', color: '#0f172a', marginBottom: '1.25rem' },
  headlineBlue: { color: '#2563eb' },
  body: { fontSize: '1.0625rem', color: '#475569', lineHeight: 1.75, maxWidth: 520, marginBottom: '2.5rem' },
  statsRow: { display: 'grid', gridTemplateColumns: 'repeat(4, 1fr)', gap: '0', marginBottom: '2.5rem', background: '#fff', border: '1px solid #e2e8f0', borderRadius: '12px', overflow: 'hidden' },
  stat: { padding: '1rem 1.25rem', borderRight: '1px solid #e2e8f0', textAlign: 'center' },
  statVal: { fontSize: '1.375rem', fontWeight: 800, color: '#2563eb', letterSpacing: '-0.02em', marginBottom: '0.2rem' },
  statLabel: { fontSize: '0.72rem', color: '#64748b', fontWeight: 500, lineHeight: 1.3 },
  features: { display: 'flex', flexDirection: 'column', gap: '1.125rem' },
  feature: { display: 'flex', alignItems: 'flex-start', gap: '0.875rem' },
  featureIcon: { fontSize: '1.25rem', lineHeight: 1, marginTop: '1px', flexShrink: 0 },
  featureTitle: { fontWeight: 600, fontSize: '0.9375rem', color: '#1e293b', marginBottom: '0.2rem' },
  featureDesc: { fontSize: '0.84rem', color: '#64748b', lineHeight: 1.55 },
  right: { position: 'sticky', top: '80px' },
  card: { background: '#fff', border: '1px solid #e2e8f0', borderRadius: '16px', padding: '2rem', boxShadow: '0 4px 24px rgba(0,0,0,.07)' },
  cardTop: { marginBottom: '1.5rem' },
  cardTitle: { fontSize: '1.1875rem', fontWeight: 700, color: '#0f172a', marginBottom: '0.25rem', letterSpacing: '-0.01em' },
  cardSub: { fontSize: '0.85rem', color: '#64748b' },
  modeSwitcher: { display: 'flex', background: '#f1f5f9', padding: '3px', borderRadius: '8px', marginBottom: '1.5rem', gap: '2px' },
  modeBtn: { flex: 1, padding: '0.5rem', border: 'none', background: 'transparent', color: '#64748b', fontWeight: 600, fontSize: '0.875rem', borderRadius: '6px', cursor: 'pointer', transition: 'all 0.15s', fontFamily: 'inherit' },
  modeBtnActive: { background: '#fff', color: '#1d4ed8', boxShadow: '0 1px 3px rgba(0,0,0,.08)' },
  switchLine: { textAlign: 'center', marginTop: '1.25rem', fontSize: '0.8125rem', color: '#64748b' },
  switchBtn: { background: 'none', border: 'none', color: '#2563eb', fontWeight: 600, cursor: 'pointer', fontSize: '0.8125rem', fontFamily: 'inherit', padding: 0 },
  proofRow: { display: 'flex', justifyContent: 'center', gap: '1.5rem', marginTop: '1.25rem', paddingTop: '1rem', borderTop: '1px solid #f1f5f9' },
  proofLock: { display: 'flex', alignItems: 'center', gap: '0.35rem', fontSize: '0.72rem', color: '#94a3b8', fontWeight: 500 },
};
