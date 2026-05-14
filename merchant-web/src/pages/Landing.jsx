import React, { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/useAuth';
import MarketingNav from '../components/MarketingNav';

const featurePreview = [
  ['Route any collection', 'Accept M-Pesa, Airtel Money, cards, wallets, bank transfers, and crypto through one business API.'],
  ['Settle in real time', 'Send value to the M-Pesa number, Airtel Money number, PesaLink account, bank account, or wallet your business chooses.'],
  ['Control fallback rails', 'Define what happens when a provider is slow, down, rejected, or too expensive.'],
  ['Reconcile every leg', 'See source, destination, fees, FX, provider references, and webhook status in one place.'],
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
            <div className="eyebrow">Business money routing for Kenya first, Africa next</div>
            <h1>NylePay Business</h1>
            <p className="hero-lede">
              Let customers pay from the rail they trust, then route the money to the account your business wants in real time. M-Pesa, Airtel Money, PesaLink, bank, wallet, card, and crypto rails become one programmable business layer.
            </p>
            <div className="hero-actions">
              <Link className="btn-primary" to="/register-business">Open business account</Link>
              <Link className="btn-outline" to="/docs">View routing API</Link>
            </div>
            <div className="merchant-metrics" aria-label="Platform highlights">
              <div><strong>One API</strong><span>collections and payouts</span></div>
              <div><strong>Real time</strong><span>settlement routing</span></div>
              <div><strong>HMAC</strong><span>signed route webhooks</span></div>
            </div>
          </div>

          <aside className="auth-panel" aria-label="Business sign in">
            <div className="auth-panel-header">
              <h2>{mode === 'signin' ? 'Sign in to Business' : 'Create business access'}</h2>
              <p>{mode === 'signin' ? 'Manage routes, keys, settlements, and webhooks.' : 'Create your access account before business onboarding.'}</p>
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
                  <label className="form-label" htmlFor="mpesa">Mobile money number</label>
                  <input id="mpesa" className="form-input" type="text" required value={mpesa} onChange={(event) => setMpesa(event.target.value)} placeholder="2547XXXXXXXX" />
                  <p className="form-hint">Used for account verification and first settlement destination.</p>
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
            <span className="eyebrow">Built for business routing</span>
            <h2>Collect from anywhere. Route to the account you choose.</h2>
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
