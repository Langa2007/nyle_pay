import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';

const Registration = () => {
  const navigate = useNavigate();
  const [formData, setFormData] = useState({
    businessName: '',
    businessEmail: '',
    webhookUrl: '',
    settlementPhone: ''
  });

  const [keys, setKeys] = useState(null);

  const handleSubmit = (e) => {
    e.preventDefault();
    // In a real app, this would be a fetch() call to POST /api/merchant/register
    // and POST /api/merchant/settlement-account
    
    // Mock response for UI demonstration
    setKeys({
      publicKey: 'npy_pub_8a92fbc147...',
      secretKey: 'npy_sec_38cd910ea2...',
      webhookSecret: 'whsec_908123afdb...'
    });
  };

  if (keys) {
    return (
      <div style={styles.container}>
        <div className="card" style={{ maxWidth: '600px', width: '100%' }}>
          <h2 style={{ color: 'var(--brand-success)', marginBottom: '1rem' }}>Registration Successful!</h2>
          <p style={{ color: 'var(--text-secondary)', marginBottom: '2rem' }}>
            Please save these API keys now. The Secret Key will never be shown again.
          </p>
          
          <div className="form-group">
            <label>Public Key (Publishable)</label>
            <div className="code-block">{keys.publicKey}</div>
          </div>
          
          <div className="form-group">
            <label>Secret Key (Keep Server-Side)</label>
            <div className="code-block" style={{ color: 'var(--brand-danger)' }}>{keys.secretKey}</div>
          </div>
          
          <div className="form-group">
            <label>Webhook Secret</label>
            <div className="code-block">{keys.webhookSecret}</div>
          </div>
          
          <button 
            className="btn-primary" 
            style={{ width: '100%', marginTop: '1rem' }}
            onClick={() => navigate('/dashboard')}
          >
            Go to Dashboard
          </button>
        </div>
      </div>
    );
  }

  return (
    <div style={styles.container}>
      <div className="card" style={{ maxWidth: '500px', width: '100%' }}>
        <h2 style={{ marginBottom: '0.5rem', textAlign: 'center' }}>NylePay for Business</h2>
        <p style={{ color: 'var(--text-secondary)', marginBottom: '2rem', textAlign: 'center' }}>
          Create your merchant account to get API keys.
        </p>
        
        <form onSubmit={handleSubmit}>
          <div className="form-group">
            <label>Business Name</label>
            <input 
              type="text" 
              required
              value={formData.businessName}
              onChange={e => setFormData({...formData, businessName: e.target.value})}
              placeholder="e.g. Acme Store Ltd" 
            />
          </div>
          
          <div className="form-group">
            <label>Business Email</label>
            <input 
              type="email" 
              required
              value={formData.businessEmail}
              onChange={e => setFormData({...formData, businessEmail: e.target.value})}
              placeholder="payments@acme.com" 
            />
          </div>
          
          <div className="form-group">
            <label>M-Pesa Settlement Number</label>
            <input 
              type="text" 
              required
              value={formData.settlementPhone}
              onChange={e => setFormData({...formData, settlementPhone: e.target.value})}
              placeholder="2547XXXXXXXX" 
            />
            <small style={{ color: 'var(--text-secondary)', display: 'block', marginTop: '0.25rem' }}>
              Where we will send your real-time payouts.
            </small>
          </div>
          
          <div className="form-group">
            <label>Webhook URL (Optional)</label>
            <input 
              type="url" 
              value={formData.webhookUrl}
              onChange={e => setFormData({...formData, webhookUrl: e.target.value})}
              placeholder="https://acme.com/api/webhook" 
            />
          </div>
          
          <button type="submit" className="btn-primary" style={{ width: '100%', marginTop: '1rem' }}>
            Register & Get API Keys
          </button>
        </form>
      </div>
    </div>
  );
};

const styles = {
  container: {
    minHeight: '100vh',
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
    padding: '2rem',
  }
};

export default Registration;
