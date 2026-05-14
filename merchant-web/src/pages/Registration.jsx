import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../context/useAuth';

const API_BASE = import.meta.env.VITE_API_BASE_URL || 'https://nyle-pay.onrender.com';

export default function Registration() {
  const navigate = useNavigate();
  const { user, updateBusinessInfo } = useAuth();
  const [step, setStep] = useState(1);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState('');
  const [keys, setKeys] = useState(null);
  const [form, setForm] = useState({
    businessName: '',
    businessEmail: '',
    settlementMethod: 'MPESA',
    settlementPhone: '',
    bankCode: '',
    bankAccount: '',
    webhookUrl: '',
  });

  const set = (key, value) => setForm((current) => ({ ...current, [key]: value }));

  const submit = async (event) => {
    event.preventDefault();
    setError('');
    setSubmitting(true);
    try {
      const res = await fetch(`${API_BASE}/api/business/register`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json', Authorization: `Bearer ${user?.token}` },
        body: JSON.stringify(form),
      });
      const json = await res.json();
      if (!res.ok || !json.success) throw new Error(json.message || 'Business registration failed');

      const { publicKey, secretKey, webhookSecret, businessId } = json.data;
      const generated = { publicKey, secretKey, webhookSecret };
      setKeys(generated);
      updateBusinessInfo({
        businessId,
        businessName: form.businessName,
        settlementPolicy: {
          primary: form.settlementMethod,
          phone: form.settlementPhone,
          bankCode: form.bankCode,
          bankAccount: form.bankAccount,
        },
        apiKeys: {
          publicKey,
          hasSecretKey: !!secretKey,
          hasWebhookSecret: !!webhookSecret,
        },
      });
      setStep(2);
    } catch (err) {
      setError(err.message || `Cannot reach ${API_BASE}. Start the API server or set VITE_API_BASE_URL.`);
    } finally {
      setSubmitting(false);
    }
  };

  if (step === 2) return <SuccessScreen keys={keys} biz={form.businessName} onDone={() => navigate('/dashboard')} />;

  return (
    <div style={P.page}>
      <div style={P.card}>
        <div style={P.header}>
          <a href="/dashboard" style={P.back}>
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"><path d="M19 12H5M12 19l-7-7 7-7"/></svg>
            Back to dashboard
          </a>
          <div style={P.badge}>Business setup</div>
        </div>

        <h1 style={P.title}>Register your business</h1>
        <p style={P.sub}>Create your NylePay Business profile, first settlement destination, and API credentials.</p>

        <form onSubmit={submit}>
          <div style={P.twoCols}>
            <div className="form-group">
              <label className="form-label">Business Name *</label>
              <input className="form-input" required value={form.businessName} onChange={(e) => set('businessName', e.target.value)} placeholder="Acme Store Ltd" />
            </div>
            <div className="form-group">
              <label className="form-label">Business Email *</label>
              <input className="form-input" type="email" required value={form.businessEmail} onChange={(e) => set('businessEmail', e.target.value)} placeholder="payments@acme.co.ke" />
            </div>
          </div>

          <div style={P.twoCols}>
            <div className="form-group">
              <label className="form-label">Primary Settlement Rail</label>
              <select className="form-input" value={form.settlementMethod} onChange={(e) => set('settlementMethod', e.target.value)}>
                <option value="MPESA">M-Pesa</option>
                <option value="AIRTEL_MONEY">Airtel Money</option>
                <option value="PESALINK">PesaLink</option>
                <option value="BANK">Bank Account</option>
                <option value="NYLEPAY_WALLET">NylePay Wallet</option>
              </select>
            </div>
            <div className="form-group">
              <label className="form-label">Settlement Mobile Money Number</label>
              <input className="form-input" value={form.settlementPhone} onChange={(e) => set('settlementPhone', e.target.value)} placeholder="2547XXXXXXXX" />
            </div>
          </div>

          <div style={P.twoCols}>
            <div className="form-group">
              <label className="form-label">Bank Code</label>
              <input className="form-input" value={form.bankCode} onChange={(e) => set('bankCode', e.target.value)} placeholder="Optional fallback" />
            </div>
            <div className="form-group">
              <label className="form-label">Bank Account</label>
              <input className="form-input" value={form.bankAccount} onChange={(e) => set('bankAccount', e.target.value)} placeholder="Optional fallback" />
            </div>
          </div>

          <div className="form-group">
            <label className="form-label">Webhook URL <span style={P.optional}>(optional - add later)</span></label>
            <input className="form-input" type="url" value={form.webhookUrl} onChange={(e) => set('webhookUrl', e.target.value)} placeholder="https://yourapp.com/api/nylepay/webhook" />
            <p className="form-hint">NylePay posts signed route, payment, and settlement events to this HTTPS endpoint.</p>
          </div>

          <div className="alert alert-info" style={{ marginBottom: '1.25rem' }}>
            <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" style={{ flexShrink: 0 }}><path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"/></svg>
            Secret credentials are shown once after registration. Store them in your backend environment and never in frontend code.
          </div>

          {error && <div className="alert alert-error" style={{ marginBottom: '1rem' }}>{error}</div>}

          <button type="submit" className="btn-primary" disabled={submitting} style={P.submit}>
            {submitting ? <><span className="spinner" /> Generating keys...</> : 'Register business and generate API keys'}
          </button>
        </form>
      </div>
    </div>
  );
}

function SuccessScreen({ keys, biz, onDone }) {
  const [copied, setCopied] = useState('');
  const [revealed, setRevealed] = useState({});

  const copy = (value, id) => {
    navigator.clipboard.writeText(value);
    setCopied(id);
    setTimeout(() => setCopied(''), 2000);
  };

  const KeyBlock = ({ label, value, id, danger }) => (
    <div style={{ marginBottom: '1rem' }}>
      <div style={P.keyHeader}>
        <span style={P.keyLabel}>{label}</span>
        {danger && <span style={P.serverOnly}>Server-side only</span>}
      </div>
      <div className="key-field">
        <div className="key-value">{revealed[id] ? value : '*'.repeat(36)}</div>
        <button className="key-copy" onClick={() => setRevealed((current) => ({ ...current, [id]: !current[id] }))}>{revealed[id] ? 'Hide' : 'Reveal'}</button>
        <button className="key-copy" onClick={() => copy(value, id)}>{copied === id ? 'Copied' : 'Copy'}</button>
      </div>
    </div>
  );

  return (
    <div style={P.page}>
      <div style={{ ...P.card, maxWidth: 560 }}>
        <div style={P.successIcon}>
          <svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="#059669" strokeWidth="2.5"><path d="M20 6L9 17l-5-5"/></svg>
        </div>
        <h1 style={{ ...P.title, textAlign: 'center' }}>{biz} is ready to route</h1>
        <p style={{ ...P.sub, textAlign: 'center', marginBottom: '1.75rem' }}>
          Copy these credentials now. Secret credentials are not stored in the browser and should live only on your server.
        </p>

        <KeyBlock label="Public Key" value={keys.publicKey} id="pub" />
        <KeyBlock label="Secret Key" value={keys.secretKey} id="sec" danger />
        <KeyBlock label="Webhook Signing Secret" value={keys.webhookSecret} id="hook" danger />

        <div className="alert alert-warning" style={{ marginTop: '1rem', marginBottom: '1.5rem' }}>
          <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" style={{ flexShrink: 0 }}><path d="M10.29 3.86L1.82 18a2 2 0 001.71 3h16.94a2 2 0 001.71-3L13.71 3.86a2 2 0 00-3.42 0z"/><line x1="12" y1="9" x2="12" y2="13"/><line x1="12" y1="17" x2="12.01" y2="17"/></svg>
          Use environment variables such as NYLEPAY_SECRET_KEY and NYLEPAY_WEBHOOK_SECRET. Do not commit them.
        </div>

        <button className="btn-primary" style={P.submit} onClick={onDone}>
          Go to dashboard
        </button>
      </div>
    </div>
  );
}

const P = {
  page: { minHeight: '100vh', background: 'var(--bg-page)', display: 'flex', alignItems: 'center', justifyContent: 'center', padding: '2rem 1rem' },
  card: { width: '100%', maxWidth: 720, background: 'var(--bg-surface)', border: '1px solid var(--border)', borderRadius: '8px', padding: '2.25rem', boxShadow: 'var(--shadow-lg)' },
  header: { display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1.75rem' },
  back: { display: 'flex', alignItems: 'center', gap: '0.4rem', fontSize: '0.84rem', color: 'var(--text-secondary)', fontWeight: 500, textDecoration: 'none' },
  badge: { fontSize: '0.72rem', fontWeight: 700, color: 'var(--brand)', background: 'var(--brand-xlight)', padding: '0.2rem 0.65rem', borderRadius: '100px' },
  title: { fontSize: '1.5rem', fontWeight: 800, letterSpacing: 0, marginBottom: '0.375rem' },
  sub: { fontSize: '0.9rem', color: 'var(--text-secondary)', marginBottom: '1.75rem', lineHeight: 1.65 },
  twoCols: { display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '0 1rem' },
  optional: { color: 'var(--text-tertiary)', fontWeight: 400 },
  submit: { width: '100%', padding: '0.75rem', fontSize: '0.9375rem', justifyContent: 'center' },
  successIcon: { width: 64, height: 64, borderRadius: '50%', background: '#d1fae5', border: '1px solid #a7f3d0', display: 'flex', alignItems: 'center', justifyContent: 'center', margin: '0 auto 1.5rem' },
  keyHeader: { display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '0.375rem' },
  keyLabel: { fontSize: '0.8rem', fontWeight: 600, color: 'var(--text-secondary)' },
  serverOnly: { fontSize: '0.7rem', color: 'var(--danger)', fontWeight: 700, background: 'var(--danger-light)', padding: '0.1rem 0.45rem', borderRadius: '4px' },
};
