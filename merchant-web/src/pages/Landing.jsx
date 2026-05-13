import React, { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import MarketingNav from '../components/MarketingNav';

const featurePreview = [
  ['Unified checkout', 'Accept M-Pesa, cards, wallet payments, and crypto through one hosted checkout or API.'],
  ['Real-time settlement', 'Track completed payments instantly and move funds to M-Pesa or bank rails.'],
  ['Signed webhooks', 'Every payment event is delivered with HMAC signatures for safe order fulfillment.'],
  ['Merchant operations', 'Manage balances, payment links, API keys, refunds, and settlements from one dashboard.'],
];

export default function Landing() {
  const [mode, setMode] = useState('signin');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [fullName, setFullName] = useState('');
  const [mpesa, setMpesa] = useState('');
  const [error, setError] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const { login, signup } = useAuth();
  const navigate = useNavigate();

  const submit = async (event) => {
    event.preventDefault();
    setError('');
    setSubmitting(true);
    try {
      if (mode === 'signup') {
        await signup(email, password, fullName, mpesa, 'KE');
      } else {
        await login(email, password);
      }
      navigate('/dashboard');
    } catch (err) {
      setError(err.message || 'Authentication failed. Please check your credentials.');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="marketing-page">
      <MarketingNav />

      <main>
        <section className="merchant-hero">
          <div className="merchant-hero-copy">
            <div className="eyebrow">Merchant payment infrastructure for Kenya and Africa</div>
            <h1>NylePay Merchant</h1>
            <p className="hero-lede">
              Accept local and international payments, reconcile settlement, and automate commerce workflows with one REST API built around M-Pesa, cards, wallets, and merchant checkout.
            </p>
            <div className="hero-actions">
              <Link className="btn-primary" to="/register-business">Open merchant account</Link>
              <Link className="btn-outline" to="/docs">View API docs</Link>
            </div>
            <div className="merchant-metrics" aria-label="Platform highlights">
              <div><strong>1.5%</strong><span>standard merchant fee</span></div>
              <div><strong>T+0</strong><span>settlement tracking</span></div>
              <div><strong>HMAC</strong><span>signed webhooks</span></div>
            </div>
          </div>

          <aside className="auth-panel" aria-label="Merchant sign in">
            <div className="auth-panel-header">
              <h2>{mode === 'signin' ? 'Sign in to dashboard' : 'Create merchant access'}</h2>
              <p>{mode === 'signin' ? 'Manage payments, keys, and settlements.' : 'Create your access account before business registration.'}</p>
            </div>

            <div className="segmented-control">
              <button type="button" className={mode === 'signin' ? 'active' : ''} onClick={() => { setMode('signin'); setError(''); }}>Sign in</button>
              <button type="button" className={mode === 'signup' ? 'active' : ''} onClick={() => { setMode('signup'); setError(''); }}>Sign up</button>
            </div>

            <form onSubmit={submit}>
              {mode === 'signup' && (
                <div className="form-group">
                  <label className="form-label" htmlFor="fullName">Full name</label>
                  <input id="fullName" className="form-input" type="text" required value={fullName} onChange={(event) => setFullName(event.target.value)} placeholder="Jane Wanjiru" autoComplete="name" />
                </div>
              )}

              <div className="form-group">
                <label className="form-label" htmlFor="email">Business email</label>
                <input id="email" className="form-input" type="email" required value={email} onChange={(event) => setEmail(event.target.value)} placeholder="payments@company.com" autoComplete="email" />
              </div>

              <div className="form-group">
                <label className="form-label" htmlFor="password">Password</label>
                <input id="password" className="form-input" type="password" required minLength={8} value={password} onChange={(event) => setPassword(event.target.value)} placeholder="At least 8 characters" autoComplete={mode === 'signup' ? 'new-password' : 'current-password'} />
              </div>

              {mode === 'signup' && (
                <div className="form-group">
                  <label className="form-label" htmlFor="mpesa">M-Pesa number</label>
                  <input id="mpesa" className="form-input" type="text" required value={mpesa} onChange={(event) => setMpesa(event.target.value)} placeholder="2547XXXXXXXX" />
                  <p className="form-hint">Used for account verification and settlement setup.</p>
                </div>
              )}

              {error && <div className="alert alert-error compact-alert">{error}</div>}

              <button type="submit" className="btn-primary auth-submit" disabled={submitting}>
                {submitting ? 'Please wait...' : mode === 'signup' ? 'Create account' : 'Sign in'}
              </button>
            </form>
          </aside>
        </section>

        <section className="section-band">
          <div className="section-heading">
            <span className="eyebrow">Built for merchant operations</span>
            <h2>Everything a growing merchant needs to accept and manage payments</h2>
          </div>
          <div className="feature-grid">
            {featurePreview.map(([title, body]) => (
              <article className="feature-card" key={title}>
                <h3>{title}</h3>
                <p>{body}</p>
              </article>
            ))}
          </div>
        </section>
      </main>
    </div>
  );
}
