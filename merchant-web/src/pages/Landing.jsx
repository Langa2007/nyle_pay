import React, { useEffect, useRef, useState } from 'react';
import { Link } from 'react-router-dom';
import { useAuth } from '../context/useAuth';
import MarketingNav from '../components/MarketingNav';

const featurePreview = [
  ['Route any collection', 'Accept M-Pesa, Airtel Money, cards, wallets, bank transfers, and crypto through one business API.'],
  ['Settle in real time', 'Send value to the M-Pesa number, Airtel Money number, PesaLink account, bank account, or wallet your business chooses.'],
  ['Control fallback rails', 'Define what happens when a provider is slow, down, rejected, or too expensive.'],
  ['Reconcile every leg', 'See source, destination, fees, FX, provider references, and webhook status in one place.'],
];

export default function Landing() {
  const currentYear = new Date().getFullYear();
  const [mode, setMode] = useState('signin');
  const [email, setEmail] = useState('');
  const [fullName, setFullName] = useState('');
  const [error, setError] = useState('');
  const [sentTo, setSentTo] = useState('');
  const [code, setCode] = useState('');
  const [verifying, setVerifying] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const lastAutoVerified = useRef('');
  const { requestBusinessAccess, confirmBusinessAccess } = useAuth();

  const verifyCode = async (nextCode = code) => {
    if (!sentTo || nextCode.length !== 6 || verifying) return;
    setError('');
    setVerifying(true);
    try {
      await confirmBusinessAccess({ email: sentTo, code: nextCode });
      window.location.href = '/dashboard';
    } catch (err) {
      setError(err.message || 'Invalid verification code.');
    } finally {
      setVerifying(false);
    }
  };

  useEffect(() => {
    if (sentTo && code.length === 6 && lastAutoVerified.current !== code) {
      lastAutoVerified.current = code;
      verifyCode(code);
    }
  }, [code, sentTo]);

  const submit = async (event) => {
    event.preventDefault();
    setError('');
    setSubmitting(true);
    try {
      await requestBusinessAccess({ fullName: mode === 'signup' ? fullName : '', email });
      setSentTo(email);
      setCode('');
      lastAutoVerified.current = '';
    } catch (err) {
      setError(err.message || 'Unable to send confirmation email.');
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

          <aside className="access-terminal" aria-label="Business access console">
            <div className="terminal-head">
              <div>
                <span className="terminal-kicker">Email access</span>
                <h2>{mode === 'signin' ? 'Send access email' : 'Create business access'}</h2>
              </div>
              <span className="terminal-state">Sandbox ready</span>
            </div>

            <div className="route-access-strip" aria-hidden="true">
              <span>Identity</span>
              <strong>{mode === 'signin' ? 'Known operator' : 'New operator'}</strong>
              <i />
              <span>Workspace</span>
              <strong>NylePay Business</strong>
            </div>

            <div className="access-switch" role="tablist" aria-label="Access mode">
              <button type="button" className={mode === 'signin' ? 'active' : ''} onClick={() => { setMode('signin'); setError(''); setSentTo(''); }}>Existing operator</button>
              <button type="button" className={mode === 'signup' ? 'active' : ''} onClick={() => { setMode('signup'); setError(''); setSentTo(''); }}>New access</button>
            </div>

            {sentTo ? (
              <div className="terminal-form access-sent">
                <h3>Enter the 6-digit code</h3>
                <p>A verification code has been sent to {sentTo}.</p>
                <input
                  className="code-input"
                  inputMode="numeric"
                  maxLength={6}
                  value={code}
                  onChange={(event) => {
                    setCode(event.target.value.replace(/\D/g, '').slice(0, 6));
                    setError('');
                  }}
                  placeholder="000000"
                  autoFocus
                />
                {error && <div className="alert alert-error compact-alert">{error}</div>}
                <button type="button" className="terminal-submit" disabled={verifying || code.length !== 6} onClick={() => verifyCode()}>
                  <span>{verifying ? 'Verifying...' : 'Verify code'}</span>
                  <small>Auto-checks after 6 digits</small>
                </button>
              </div>
            ) : <form className="terminal-form" onSubmit={submit}>
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

              {error && <div className="alert alert-error compact-alert">{error}</div>}

              <button type="submit" className="terminal-submit" disabled={submitting}>
                <span>{submitting ? 'Sending email...' : mode === 'signup' ? 'Send confirmation email' : 'Send access email'}</span>
                <small>Confirm email to continue</small>
              </button>
            </form>}

            <div className="terminal-policy">
              <span>JWT secured</span>
              <span>HMAC webhooks</span>
              <span>Rail policy enforced</span>
            </div>
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

      <footer className="business-footer">
        <div>
          <strong>NylePay Business</strong>
          <span>Payment routing infrastructure for African commerce.</span>
        </div>
        <nav aria-label="Footer navigation">
          <Link to="/">Home</Link>
          <Link to="/requirements">Requirements</Link>
          <Link to="/docs">API Docs</Link>
        </nav>
        <span>Copyright © {currentYear} NylePay Business. All rights reserved.</span>
      </footer>
    </div>
  );
}
