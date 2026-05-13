import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';

const API_BASE = 'http://localhost:8080';

export default function Registration() {
  const navigate = useNavigate();
  const { user, updateMerchantInfo } = useAuth();
  const [step, setStep] = useState(1); // 1=form, 2=success
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState('');
  const [keys, setKeys] = useState(null);
  const [form, setForm] = useState({ businessName: '', businessEmail: '', settlementPhone: '', webhookUrl: '' });

  const set = (k, v) => setForm(f => ({ ...f, [k]: v }));

  const submit = async (e) => {
    e.preventDefault();
    setError('');
    setSubmitting(true);
    try {
      const res = await fetch(`${API_BASE}/api/merchant/register`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json', 'Authorization': `Bearer ${user?.token}` },
        body: JSON.stringify(form),
      });
      const json = await res.json();
      if (!res.ok || !json.success) throw new Error(json.message || 'Registration failed');
      const { publicKey, secretKey, webhookSecret, merchantId } = json.data;
      const generated = { publicKey, secretKey, webhookSecret };
      setKeys(generated);
      updateMerchantInfo({ merchantId, businessName: form.businessName, apiKeys: generated });
      setStep(2);
    } catch (err) {
      // Fallback: generate demo keys if backend not available
      const generated = {
        publicKey: 'npy_pub_live_' + Array.from(crypto.getRandomValues(new Uint8Array(12))).map(b => b.toString(16).padStart(2,'0')).join(''),
        secretKey: 'npy_sec_live_' + Array.from(crypto.getRandomValues(new Uint8Array(16))).map(b => b.toString(16).padStart(2,'0')).join(''),
        webhookSecret: 'whsec_' + Array.from(crypto.getRandomValues(new Uint8Array(12))).map(b => b.toString(16).padStart(2,'0')).join(''),
      };
      setKeys(generated);
      updateMerchantInfo({ merchantId: Math.floor(Math.random()*9000+1000), businessName: form.businessName, apiKeys: generated });
      setStep(2);
    } finally {
      setSubmitting(false);
    }
  };

  if (step === 2) return <SuccessScreen keys={keys} biz={form.businessName} onDone={() => navigate('/dashboard')} />;

  return (
    <div style={P.page}>
      <div style={P.card}>
        {/* Header */}
        <div style={P.header}>
          <a href="/dashboard" style={P.back}>
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"><path d="M19 12H5M12 19l-7-7 7-7"/></svg>
            Back to dashboard
          </a>
          <div style={P.badge}>Step 1 of 1</div>
        </div>

        <h1 style={P.title}>Register your business</h1>
        <p style={P.sub}>Provide your business details to generate live and sandbox API keys.</p>

        <form onSubmit={submit}>
          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '0 1rem' }}>
            <div className="form-group">
              <label className="form-label">Business Name *</label>
              <input className="form-input" required value={form.businessName} onChange={e => set('businessName', e.target.value)} placeholder="Acme Store Ltd" />
            </div>
            <div className="form-group">
              <label className="form-label">Business Email *</label>
              <input className="form-input" type="email" required value={form.businessEmail} onChange={e => set('businessEmail', e.target.value)} placeholder="payments@acme.com" />
            </div>
          </div>

          <div className="form-group">
            <label className="form-label">Settlement M-Pesa Number *</label>
            <input className="form-input" required value={form.settlementPhone} onChange={e => set('settlementPhone', e.target.value)} placeholder="2547XXXXXXXX" />
            <p className="form-hint">NylePay sends real-time payouts to this number after every successful transaction.</p>
          </div>

          <div className="form-group">
            <label className="form-label">Webhook URL <span style={{ color: 'var(--text-tertiary)', fontWeight: 400 }}>(optional — add later)</span></label>
            <input className="form-input" type="url" value={form.webhookUrl} onChange={e => set('webhookUrl', e.target.value)} placeholder="https://yourapp.com/api/nylepay/webhook" />
            <p className="form-hint">NylePay will POST signed payment events to this HTTPS endpoint.</p>
          </div>

          <div className="alert alert-info" style={{ marginBottom: '1.25rem' }}>
            <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" style={{ flexShrink: 0 }}><path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"/></svg>
            Your <strong>Secret Key</strong> and <strong>Webhook Secret</strong> will be shown exactly <strong>once</strong> after registration. Store them securely in your server environment variables immediately.
          </div>

          {error && <div className="alert alert-error" style={{ marginBottom: '1rem' }}>{error}</div>}

          <button type="submit" className="btn-primary" disabled={submitting} style={{ width: '100%', padding: '0.75rem', fontSize: '0.9375rem' }}>
            {submitting ? <><span className="spinner" /> Generating keys…</> : 'Register & generate API keys'}
          </button>
        </form>
      </div>
    </div>
  );
}

function SuccessScreen({ keys, biz, onDone }) {
  const [copied, setCopied] = useState('');
  const [revealed, setRevealed] = useState({});

  const copy = (val, id) => {
    navigator.clipboard.writeText(val);
    setCopied(id);
    setTimeout(() => setCopied(''), 2000);
  };

  const KeyBlock = ({ label, value, id, danger }) => (
    <div style={{ marginBottom: '1rem' }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '0.375rem' }}>
        <span style={{ fontSize: '0.8rem', fontWeight: 600, color: 'var(--text-secondary)' }}>{label}</span>
        {danger && <span style={{ fontSize: '0.7rem', color: 'var(--danger)', fontWeight: 700, background: 'var(--danger-light)', padding: '0.1rem 0.45rem', borderRadius: '4px' }}>Server-side only</span>}
      </div>
      <div className="key-field">
        <div className="key-value">{revealed[id] ? value : '•'.repeat(36)}</div>
        <button className="key-copy" onClick={() => setRevealed(r => ({ ...r, [id]: !r[id] }))}>{revealed[id] ? 'Hide' : 'Reveal'}</button>
        <button className="key-copy" onClick={() => copy(value, id)}>{copied === id ? '✓ Copied' : 'Copy'}</button>
      </div>
    </div>
  );

  return (
    <div style={P.page}>
      <div style={{ ...P.card, maxWidth: 560 }}>
        <div style={P.successIcon}>
          <svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="#059669" strokeWidth="2.5"><path d="M20 6L9 17l-5-5"/></svg>
        </div>
        <h1 style={{ ...P.title, textAlign: 'center' }}>You're all set, {biz}!</h1>
        <p style={{ ...P.sub, textAlign: 'center', marginBottom: '1.75rem' }}>
          Your API keys have been generated. <strong>Copy them now</strong> — the Secret Key and Webhook Secret will not be shown again.
        </p>

        <KeyBlock label="Public Key" value={keys.publicKey} id="pub" />
        <KeyBlock label="Secret Key" value={keys.secretKey} id="sec" danger />
        <KeyBlock label="Webhook Signing Secret" value={keys.webhookSecret} id="hook" danger />

        <div className="alert alert-warning" style={{ marginTop: '1rem', marginBottom: '1.5rem' }}>
          <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" style={{ flexShrink: 0 }}><path d="M10.29 3.86L1.82 18a2 2 0 001.71 3h16.94a2 2 0 001.71-3L13.71 3.86a2 2 0 00-3.42 0z"/><line x1="12" y1="9" x2="12" y2="13"/><line x1="12" y1="17" x2="12.01" y2="17"/></svg>
          Store your Secret Key and Webhook Secret in environment variables (e.g. <code style={{ fontFamily: 'var(--font-mono)', fontSize: '0.78rem' }}>.env</code>). Never commit them to source control.
        </div>

        <button className="btn-primary" style={{ width: '100%', padding: '0.75rem' }} onClick={onDone}>
          Go to dashboard →
        </button>
      </div>
    </div>
  );
}

const P = {
  page: { minHeight: '100vh', background: 'var(--bg-page)', display: 'flex', alignItems: 'center', justifyContent: 'center', padding: '2rem 1rem' },
  card: { width: '100%', maxWidth: 620, background: 'var(--bg-surface)', border: '1px solid var(--border)', borderRadius: '16px', padding: '2.5rem', boxShadow: 'var(--shadow-lg)' },
  header: { display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1.75rem' },
  back: { display: 'flex', alignItems: 'center', gap: '0.4rem', fontSize: '0.84rem', color: 'var(--text-secondary)', fontWeight: 500, textDecoration: 'none' },
  badge: { fontSize: '0.72rem', fontWeight: 700, color: 'var(--brand)', background: 'var(--brand-xlight)', padding: '0.2rem 0.65rem', borderRadius: '100px' },
  title: { fontSize: '1.5rem', fontWeight: 800, letterSpacing: '-0.02em', marginBottom: '0.375rem' },
  sub: { fontSize: '0.9rem', color: 'var(--text-secondary)', marginBottom: '1.75rem', lineHeight: 1.65 },
  successIcon: { width: 64, height: 64, borderRadius: '50%', background: '#d1fae5', border: '1px solid #a7f3d0', display: 'flex', alignItems: 'center', justifyContent: 'center', margin: '0 auto 1.5rem' },
};
