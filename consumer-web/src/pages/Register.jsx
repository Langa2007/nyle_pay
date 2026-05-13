import React, { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';

const API = 'http://localhost:8080';

export default function Register() {
  const [step, setStep]           = useState(1); // step 1 = details, step 2 = done
  const [fullName, setFullName]   = useState('');
  const [email, setEmail]         = useState('');
  const [password, setPassword]   = useState('');
  const [mpesa, setMpesa]         = useState('');
  const [error, setError]         = useState('');
  const [loading, setLoading]     = useState(false);
  const [userData, setUserData]   = useState(null);
  const navigate                  = useNavigate();

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    setLoading(true);
    try {
      const res  = await fetch(`${API}/api/auth/register`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          fullName, email, password,
          mpesaNumber: mpesa,
          countryCode: 'KE',
        }),
      });
      const json = await res.json();
      if (!res.ok || !json.success) throw new Error(json.message || 'Registration failed');

      // Auto-login to get token
      const loginRes  = await fetch(`${API}/api/auth/login`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ email, password }),
      });
      const loginJson = await loginRes.json();
      if (loginRes.ok && loginJson.success) {
        localStorage.setItem('npy_consumer_session', JSON.stringify(loginJson.data));
        setUserData(loginJson.data);
      }
      setStep(2);
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  };

  if (step === 2) {
    return (
      <div style={S.page}>
        <div style={S.blob1} /><div style={S.blob2} />
        <div style={S.card}>
          <div style={S.successIcon}>
            <svg width="32" height="32" viewBox="0 0 24 24" fill="none" stroke="#10b981" strokeWidth="2.5">
              <path d="M20 6L9 17l-5-5"/>
            </svg>
          </div>
          <h1 style={{ ...S.heading, textAlign: 'center', marginBottom: '0.5rem' }}>Account Created!</h1>
          <p style={{ ...S.sub, textAlign: 'center', marginBottom: '1.5rem' }}>
            Welcome to NylePay, <strong style={{ color: '#f1f5f9' }}>{fullName}</strong>.
            Your NPY account is ready.
          </p>

          {userData?.accountNumber && (
            <div style={S.acctBox}>
              <div style={S.acctLabel}>Your NPY Account Number</div>
              <div style={S.acctNumber}>{userData.accountNumber}</div>
              <div style={S.acctHint}>Use this to receive payments from other NylePay users</div>
            </div>
          )}

          <button style={S.btn} onClick={() => navigate('/login')}>
            Continue to Sign In
          </button>
        </div>
      </div>
    );
  }

  return (
    <div style={S.page}>
      <div style={S.blob1} /><div style={S.blob2} />

      <div style={S.card}>
        <Link to="/" style={S.logoRow}>
          <svg width="26" height="26" viewBox="0 0 24 24" fill="none">
            <defs>
              <linearGradient id="lg2" x1="0" y1="0" x2="24" y2="24" gradientUnits="userSpaceOnUse">
                <stop offset="0%" stopColor="#3b82f6"/>
                <stop offset="100%" stopColor="#10b981"/>
              </linearGradient>
            </defs>
            <path d="M12 2L2 7l10 5 10-5-10-5zM2 17l10 5 10-5M2 12l10 5 10-5" stroke="url(#lg2)" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"/>
          </svg>
          <span style={S.logoText}>NylePay</span>
        </Link>

        <h1 style={S.heading}>Create your account</h1>
        <p style={S.sub}>Your personal NPY wallet — fund, send, and pay instantly</p>

        <form onSubmit={handleSubmit} style={S.form}>
          <div style={S.row}>
            <div style={S.group}>
              <label style={S.label} htmlFor="fullName">Full Name</label>
              <input
                id="fullName" type="text" required
                style={S.input} value={fullName}
                onChange={e => setFullName(e.target.value)}
                placeholder="Jane Doe"
                autoComplete="name"
              />
            </div>
          </div>

          <div style={S.group}>
            <label style={S.label} htmlFor="email">Email</label>
            <input
              id="email" type="email" required
              style={S.input} value={email}
              onChange={e => setEmail(e.target.value)}
              placeholder="jane@example.com"
              autoComplete="email"
            />
          </div>

          <div style={S.group}>
            <label style={S.label} htmlFor="mpesa">M-Pesa Number</label>
            <input
              id="mpesa" type="text" required
              style={S.input} value={mpesa}
              onChange={e => setMpesa(e.target.value)}
              placeholder="2547XXXXXXXX"
            />
            <span style={S.hint}>Used for funding your wallet and receiving payouts</span>
          </div>

          <div style={S.group}>
            <label style={S.label} htmlFor="password">Password</label>
            <input
              id="password" type="password" required minLength={8}
              style={S.input} value={password}
              onChange={e => setPassword(e.target.value)}
              placeholder="Minimum 8 characters"
              autoComplete="new-password"
            />
          </div>

          {error && <div style={S.error}>{error}</div>}

          <button type="submit" disabled={loading} style={S.btn}>
            {loading ? 'Creating account…' : 'Create NPY Account'}
          </button>
        </form>

        <p style={S.foot}>
          Already have an account?{' '}
          <Link to="/login" style={S.footLink}>Sign in</Link>
        </p>

        <div style={S.divider}/>
        <p style={{ ...S.foot, marginTop: 0 }}>
          Are you a business?{' '}
          <a href="http://localhost:5174" style={S.footLink}>Merchant Portal →</a>
        </p>
      </div>
    </div>
  );
}

const S = {
  page: {
    minHeight: '100vh',
    display: 'flex', alignItems: 'center', justifyContent: 'center',
    padding: '2rem 1rem', position: 'relative', overflow: 'hidden',
  },
  blob1: {
    position: 'fixed', top: '-200px', right: '-100px',
    width: '500px', height: '500px', borderRadius: '50%',
    background: '#3b82f6', filter: 'blur(130px)', opacity: 0.1, pointerEvents: 'none',
  },
  blob2: {
    position: 'fixed', bottom: '-150px', left: '-100px',
    width: '450px', height: '450px', borderRadius: '50%',
    background: '#10b981', filter: 'blur(130px)', opacity: 0.1, pointerEvents: 'none',
  },
  card: {
    width: '100%', maxWidth: '440px',
    background: 'rgba(13,21,38,0.85)',
    border: '1px solid rgba(255,255,255,0.07)',
    borderRadius: '20px', padding: '2.5rem',
    backdropFilter: 'blur(20px)', WebkitBackdropFilter: 'blur(20px)',
    boxShadow: '0 8px 40px rgba(0,0,0,0.5)',
    position: 'relative', zIndex: 1,
  },
  logoRow: {
    display: 'flex', alignItems: 'center', gap: '0.5rem',
    marginBottom: '1.75rem', textDecoration: 'none',
  },
  logoText: {
    fontFamily: "'Outfit', sans-serif", fontWeight: 800, fontSize: '1.4rem',
    background: 'linear-gradient(135deg,#3b82f6,#10b981)',
    WebkitBackgroundClip: 'text', WebkitTextFillColor: 'transparent', backgroundClip: 'text',
  },
  heading: {
    fontFamily: "'Outfit', sans-serif", fontSize: '1.55rem', fontWeight: 700,
    color: '#f1f5f9', marginBottom: '0.25rem',
  },
  sub: { color: '#8b9dc3', fontSize: '0.875rem', marginBottom: '1.75rem' },
  form: { display: 'flex', flexDirection: 'column' },
  row: { display: 'flex', gap: '0.75rem' },
  group: { marginBottom: '1rem' },
  label: { display: 'block', color: '#8b9dc3', fontSize: '0.8rem', fontWeight: 500, marginBottom: '0.35rem' },
  input: {
    width: '100%', padding: '0.7rem 1rem',
    background: '#060b18', border: '1px solid rgba(255,255,255,0.08)',
    borderRadius: '8px', color: '#f1f5f9',
    fontFamily: "'Inter', sans-serif", fontSize: '0.9rem',
    outline: 'none', boxSizing: 'border-box', transition: 'border-color 0.2s',
  },
  hint: { display: 'block', color: '#556a8a', fontSize: '0.73rem', marginTop: '0.25rem' },
  error: {
    padding: '0.7rem 1rem',
    background: 'rgba(239,68,68,0.08)', border: '1px solid rgba(239,68,68,0.25)',
    borderRadius: '8px', color: '#f87171',
    fontSize: '0.8rem', marginBottom: '1rem', lineHeight: 1.5,
  },
  btn: {
    width: '100%', padding: '0.875rem', marginTop: '0.25rem',
    background: 'linear-gradient(135deg,#3b82f6,#10b981)',
    border: 'none', borderRadius: '8px', color: '#fff',
    fontFamily: "'Outfit', sans-serif", fontWeight: 700, fontSize: '0.9375rem',
    cursor: 'pointer', transition: 'opacity 0.2s',
  },
  foot: { textAlign: 'center', color: '#556a8a', fontSize: '0.8125rem', marginTop: '1.5rem' },
  footLink: { color: '#3b82f6', fontWeight: 600 },
  divider: { height: '1px', background: 'rgba(255,255,255,0.06)', margin: '1.25rem 0' },
  successIcon: {
    width: '64px', height: '64px', borderRadius: '50%',
    background: 'rgba(16,185,129,0.12)', border: '1px solid rgba(16,185,129,0.25)',
    display: 'flex', alignItems: 'center', justifyContent: 'center',
    margin: '0 auto 1.5rem',
  },
  acctBox: {
    background: 'rgba(59,130,246,0.07)', border: '1px solid rgba(59,130,246,0.2)',
    borderRadius: '10px', padding: '1.25rem', marginBottom: '1.5rem', textAlign: 'center',
  },
  acctLabel: { fontSize: '0.72rem', fontWeight: 700, letterSpacing: '0.08em', textTransform: 'uppercase', color: '#8b9dc3', marginBottom: '0.5rem' },
  acctNumber: { fontFamily: "'JetBrains Mono', monospace", fontSize: '1.1rem', fontWeight: 700, color: '#3b82f6', letterSpacing: '0.05em', marginBottom: '0.4rem' },
  acctHint: { fontSize: '0.75rem', color: '#556a8a' },
};
