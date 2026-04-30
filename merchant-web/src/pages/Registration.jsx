import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';

export default function Registration() {
  const navigate = useNavigate();
  const { updateMerchantInfo } = useAuth();
  const [formData, setFormData] = useState({
    businessName: '',
    businessEmail: '',
    webhookUrl: '',
    settlementPhone: ''
  });
  const [keys, setKeys] = useState(null);
  const [submitting, setSubmitting] = useState(false);

  const handleSubmit = async (e) => {
    e.preventDefault();
    setSubmitting(true);
    await new Promise(r => setTimeout(r, 1000));

    const generated = {
      publicKey: 'npy_pub_' + crypto.randomUUID().replace(/-/g, '').slice(0, 24),
      secretKey: 'npy_sec_' + crypto.randomUUID().replace(/-/g, '').slice(0, 24),
      webhookSecret: 'whsec_' + crypto.randomUUID().replace(/-/g, '').slice(0, 20),
    };
    setKeys(generated);
    updateMerchantInfo({
      merchantId: Math.floor(Math.random() * 9000 + 1000),
      businessName: formData.businessName,
      apiKeys: generated,
    });
    setSubmitting(false);
  };

  if (keys) {
    return (
      <div className="reg-container">
        <div className="card" style={{ maxWidth: '580px', width: '100%' }}>
          <div style={{ textAlign: 'center', marginBottom: '1.5rem' }}>
            <h2 style={{ color: 'var(--brand-green)' }}>You're All Set!</h2>
            <p style={{ color: 'var(--text-secondary)', marginTop: '.5rem' }}>
              Copy and save these keys now. The <strong>Secret Key</strong> will never be shown again.
            </p>
          </div>

          <hr className="divider" />

          <div className="form-group">
            <label className="form-label">Public Key (safe for frontend)</label>
            <div className="code-block">{keys.publicKey}</div>
          </div>

          <div className="form-group">
            <label className="form-label">Secret Key (keep server-side only)</label>
            <div className="code-block" style={{ color: 'var(--danger)' }}>{keys.secretKey}</div>
          </div>

          <div className="form-group">
            <label className="form-label">Webhook Signing Secret</label>
            <div className="code-block">{keys.webhookSecret}</div>
          </div>

          <button
            className="btn-primary"
            style={{ width: '100%', marginTop: '1rem' }}
            onClick={() => navigate('/dashboard')}
          >
            Go to Dashboard →
          </button>
        </div>

        <style>{`
          .reg-container {
            min-height: 100vh;
            display: flex;
            align-items: center;
            justify-content: center;
            padding: 2rem;
          }
        `}</style>
      </div>
    );
  }

  return (
    <div className="reg-container">
      <div className="card" style={{ maxWidth: '480px', width: '100%' }}>
        <div style={{ textAlign: 'center', marginBottom: '1.5rem' }}>
          <h2>Register Your Business</h2>
          <p style={{ color: 'var(--text-secondary)', marginTop: '.25rem', fontSize: '.9375rem' }}>
            Set up your merchant profile and get API keys.
          </p>
        </div>

        <form onSubmit={handleSubmit}>
          <div className="form-group">
            <label className="form-label" htmlFor="biz-name">Business Name</label>
            <input
              id="biz-name"
              className="form-input"
              type="text"
              required
              value={formData.businessName}
              onChange={e => setFormData({...formData, businessName: e.target.value})}
              placeholder="e.g. Acme Store Ltd"
            />
          </div>

          <div className="form-group">
            <label className="form-label" htmlFor="biz-email">Business Email</label>
            <input
              id="biz-email"
              className="form-input"
              type="email"
              required
              value={formData.businessEmail}
              onChange={e => setFormData({...formData, businessEmail: e.target.value})}
              placeholder="payments@acme.com"
            />
          </div>

          <div className="form-group">
            <label className="form-label" htmlFor="settle-phone">M-Pesa Settlement Number</label>
            <input
              id="settle-phone"
              className="form-input"
              type="text"
              required
              value={formData.settlementPhone}
              onChange={e => setFormData({...formData, settlementPhone: e.target.value})}
              placeholder="2547XXXXXXXX"
            />
            <p className="form-hint">Where NylePay will send your real-time payouts.</p>
          </div>

          <div className="form-group">
            <label className="form-label" htmlFor="webhook-url">Webhook URL <span style={{color: 'var(--text-muted)'}}>(optional)</span></label>
            <input
              id="webhook-url"
              className="form-input"
              type="url"
              value={formData.webhookUrl}
              onChange={e => setFormData({...formData, webhookUrl: e.target.value})}
              placeholder="https://yoursite.com/api/webhook"
            />
          </div>

          <button
            type="submit"
            className="btn-primary"
            disabled={submitting}
            style={{ width: '100%', marginTop: '.5rem' }}
          >
            {submitting ? 'Generating Keys…' : 'Register & Get API Keys'}
          </button>
        </form>
      </div>

      <style>{`
        .reg-container {
          min-height: 100vh;
          display: flex;
          align-items: center;
          justify-content: center;
          padding: 2rem;
        }
      `}</style>
    </div>
  );
}
