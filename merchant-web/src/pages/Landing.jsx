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
  const [password, setPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [error, setError] = useState('');
  const [notice, setNotice] = useState('');
  const [sentTo, setSentTo] = useState('');
  const [code, setCode] = useState('');
  const [verifying, setVerifying] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const lastAutoVerified = useRef('');
  const lastPasswordCheck = useRef('');
  const { requestBusinessAccess, confirmBusinessAccess, requestPasswordReset } = useAuth();

  const firstName = fullName.trim().split(/\s+/)[0] || '';

  const passwordError = () => {
    if (password.length < 8) return 'Password must be at least 8 characters.';
    if (!/[A-Z]/.test(password) || !/[a-z]/.test(password) || !/\d/.test(password) || !/[^A-Za-z0-9]/.test(password)) {
      return 'Use uppercase, lowercase, number, and special symbol.';
    }
    if (mode === 'signup' && firstName && password.toLowerCase().includes(firstName.toLowerCase())) {
      return 'Password must not contain your first name.';
    }
    if (mode === 'signup' && password !== confirmPassword) return 'Passwords do not match.';
    return '';
  };

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

  useEffect(() => {
    if (mode !== 'signin' || sentTo || submitting) return;
    if (!email.includes('@') || password.length < 8) return;
    const fingerprint = `${email.trim().toLowerCase()}::${password}`;
    if (lastPasswordCheck.current === fingerprint) return;
    const timer = setTimeout(async () => {
      lastPasswordCheck.current = fingerprint;
      setError('');
      setNotice('');
      setSubmitting(true);
      try {
        await requestBusinessAccess({ email, password, mode: 'signin' });
        setSentTo(email);
        setCode('');
        lastAutoVerified.current = '';
      } catch {
        setError('Email or password is wrong. Try again or change password.');
      } finally {
        setSubmitting(false);
      }
    }, 750);
    return () => clearTimeout(timer);
  }, [email, password, mode, sentTo, submitting]);

  const submit = async (event) => {
    event.preventDefault();
    setError('');
    setNotice('');
    const localPasswordError = passwordError();
    if (localPasswordError) {
      setError(localPasswordError);
      return;
    }
    setSubmitting(true);
    try {
      await requestBusinessAccess({ fullName: mode === 'signup' ? fullName : '', email, password, mode });
      setSentTo(email);
      setCode('');
      lastAutoVerified.current = '';
    } catch (err) {
      setError(err.message || 'Unable to send verification code.');
    } finally {
      setSubmitting(false);
    }
  };

  const forgotPassword = async () => {
    setError('');
    setNotice('');
    if (!email || !email.includes('@')) {
      setError('Enter your email first, then request password reset.');
      return;
    }
    setSubmitting(true);
    try {
      await requestPasswordReset(email);
      setNotice('Password reset instructions have been sent to your email.');
    } catch (err) {
      setError(err.message || 'Unable to send password reset email.');
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
                <h2>{mode === 'signin' ? 'Login with password + code' : 'Create sandbox access'}</h2>
              </div>
              <span className="terminal-state">Sandbox ready</span>
            </div>

            <div className="route-access-strip" aria-hidden="true">
              <span>Identity</span>
              <strong>{mode === 'signin' ? 'Password check' : 'New operator'}</strong>
              <i />
              <span>Workspace</span>
              <strong>NylePay Business</strong>
            </div>

            <div className="access-switch" role="tablist" aria-label="Access mode">
              <button type="button" className={mode === 'signin' ? 'active' : ''} onClick={() => { setMode('signin'); setError(''); setNotice(''); setSentTo(''); }}>Existing operator</button>
              <button type="button" className={mode === 'signup' ? 'active' : ''} onClick={() => { setMode('signup'); setError(''); setNotice(''); setSentTo(''); }}>New access</button>
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
                <label className="form-label" htmlFor="email">Email</label>
                <input id="email" className="form-input" type="email" required value={email} onChange={(event) => { setEmail(event.target.value); setError(''); setNotice(''); }} placeholder="developer@example.com" autoComplete="email" />
              </div>

              <div className="form-group">
                <label className="form-label" htmlFor="password">Password</label>
                <input id="password" className="form-input" type="password" required value={password} onChange={(event) => { setPassword(event.target.value); setError(''); setNotice(''); }} placeholder="Minimum 8 characters" autoComplete={mode === 'signin' ? 'current-password' : 'new-password'} />
                {mode === 'signup' && <p className="form-hint">Use at least 8 characters with uppercase, lowercase, number, and symbol. Do not use your first name.</p>}
              </div>

              {mode === 'signup' && (
                <div className="form-group">
                  <label className="form-label" htmlFor="confirmPassword">Confirm password</label>
                  <input id="confirmPassword" className="form-input" type="password" required value={confirmPassword} onChange={(event) => { setConfirmPassword(event.target.value); setError(''); }} placeholder="Re-enter password" autoComplete="new-password" />
                </div>
              )}

              {error && <div className="alert alert-error compact-alert">{error}</div>}
              {notice && <div className="alert alert-success compact-alert">{notice}</div>}

              <button type="submit" className="terminal-submit" disabled={submitting}>
                <span>{submitting ? 'Checking...' : mode === 'signup' ? 'Create access and send code' : 'Get login code'}</span>
                <small>{mode === 'signin' ? 'Auto-sends when password is correct' : 'Password is hidden and encrypted'}</small>
              </button>
              {mode === 'signin' && (
                <button type="button" className="btn-ghost" style={{ width: '100%', justifyContent: 'center', marginTop: '0.65rem' }} onClick={forgotPassword} disabled={submitting}>
                  Change password
                </button>
              )}
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
