import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';

export default function Dashboard() {
  const { user, logout } = useAuth();
  const navigate = useNavigate();
  const [activeTab, setActiveTab] = useState('overview');

  const handleLogout = () => {
    logout();
    navigate('/');
  };

  const businessName = user?.businessName || 'My Business';
  const merchantId = user?.merchantId || '—';
  const initial = businessName.charAt(0).toUpperCase();

  return (
    <div className="dashboard-layout">
      {/* ── Sidebar ── */}
      <aside className="sidebar">
        <div style={{ marginBottom: '2rem' }}>
          <h2 style={{ color: 'var(--brand-blue)', display: 'flex', alignItems: 'center', gap: '.5rem', fontSize: '1.25rem' }}>
            <span>⚡</span> NylePay Business
          </h2>
        </div>

        <nav style={{ display: 'flex', flexDirection: 'column', gap: '.25rem', flex: 1 }}>
          {[
            { id: 'overview', label: '📊  Overview' },
            { id: 'payments', label: '💳  Payments' },
            { id: 'api',      label: '🔑  API Keys' },
            { id: 'settings', label: '⚙️  Settings' },
          ].map((item) => (
            <button
              key={item.id}
              onClick={() => setActiveTab(item.id)}
              style={{
                background: activeTab === item.id ? 'rgba(59,130,246,.1)' : 'transparent',
                color: activeTab === item.id ? 'var(--brand-blue)' : 'var(--text-secondary)',
                fontWeight: activeTab === item.id ? 600 : 500,
                border: 'none',
                padding: '.625rem 1rem',
                textAlign: 'left',
                borderRadius: 'var(--radius-sm)',
                fontFamily: 'var(--font-heading)',
                fontSize: '.9375rem',
                cursor: 'pointer',
                transition: 'all .15s',
              }}
            >
              {item.label}
            </button>
          ))}
        </nav>

        {/* User Footer */}
        <div style={{ borderTop: '1px solid var(--border-color)', paddingTop: '1.25rem', marginTop: 'auto' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '.75rem', marginBottom: '1rem' }}>
            <div style={{
              width: 36, height: 36, borderRadius: 8,
              background: 'var(--brand-blue)', color: '#fff',
              display: 'flex', alignItems: 'center', justifyContent: 'center',
              fontWeight: 700, fontFamily: 'var(--font-heading)', fontSize: '.875rem'
            }}>
              {initial}
            </div>
            <div>
              <p style={{ fontWeight: 600, fontSize: '.8125rem', lineHeight: 1.3 }}>{businessName}</p>
              <p style={{ color: 'var(--text-muted)', fontSize: '.6875rem' }}>ID: {merchantId}</p>
            </div>
          </div>
          <button onClick={handleLogout} className="btn-outline" style={{ width: '100%', fontSize: '.8125rem', padding: '.5rem' }}>
            Sign Out
          </button>
        </div>
      </aside>

      {/* ── Main Content ── */}
      <main className="main-content">
        {activeTab === 'overview' && <OverviewTab businessName={businessName} />}
        {activeTab === 'payments' && <PaymentsTab />}
        {activeTab === 'api' && <ApiTab />}
        {activeTab === 'settings' && <SettingsTab />}
      </main>
    </div>
  );
}

/* ─────────────── Overview Tab ─────────────── */
function OverviewTab({ businessName }) {
  return (
    <>
      <header style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '2.5rem', flexWrap: 'wrap', gap: '1rem' }}>
        <div>
          <h1 style={{ fontSize: '1.75rem' }}>Welcome back</h1>
          <p style={{ color: 'var(--text-secondary)', marginTop: '.25rem' }}>Here's what's happening with {businessName}.</p>
        </div>
        <button className="btn-primary">+ Create Payment Link</button>
      </header>

      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(220px, 1fr))', gap: '1.5rem', marginBottom: '2.5rem' }}>
        <div className="card metric-card">
          <span className="metric-label">Available Balance</span>
          <span className="metric-value" style={{ color: 'var(--brand-green)' }}>KES 0.00</span>
          <span style={{ fontSize: '.75rem', color: 'var(--text-muted)' }}>Real-time settlements enabled</span>
        </div>
        <div className="card metric-card">
          <span className="metric-label">Today's Volume</span>
          <span className="metric-value">KES 0.00</span>
          <span style={{ fontSize: '.75rem', color: 'var(--text-muted)' }}>0 transactions</span>
        </div>
        <div className="card metric-card">
          <span className="metric-label">Settlement</span>
          <span className="metric-value" style={{ fontSize: '1.375rem', marginTop: '.5rem' }}>Not configured</span>
          <span className="badge badge-warning" style={{ marginTop: '.5rem', alignSelf: 'flex-start' }}>Setup Required</span>
        </div>
      </div>

      <div className="card">
        <h3 style={{ marginBottom: '1rem', fontSize: '1.125rem' }}>Recent Payments</h3>
        <div style={{ textAlign: 'center', padding: '3rem 1rem', color: 'var(--text-muted)' }}>
          <p style={{ fontSize: '2rem', marginBottom: '.5rem' }}>📭</p>
          <p>No payments yet. Create a payment link or integrate the API to get started.</p>
        </div>
      </div>
    </>
  );
}

/* ─────────────── Payments Tab ─────────────── */
function PaymentsTab() {
  return (
    <>
      <h1 style={{ fontSize: '1.75rem', marginBottom: '.5rem' }}>Payments</h1>
      <p style={{ color: 'var(--text-secondary)', marginBottom: '2rem' }}>View all incoming payments and their settlement status.</p>
      <div className="card">
        <div style={{ textAlign: 'center', padding: '3rem 1rem', color: 'var(--text-muted)' }}>
          <p style={{ fontSize: '2rem', marginBottom: '.5rem' }}>📭</p>
          <p>No payment history yet. Payments will appear here once customers start paying.</p>
        </div>
      </div>
    </>
  );
}

/* ─────────────── API Keys Tab ─────────────── */
function ApiTab() {
  return (
    <>
      <h1 style={{ fontSize: '1.75rem', marginBottom: '.5rem' }}>API Keys & Webhooks</h1>
      <p style={{ color: 'var(--text-secondary)', marginBottom: '2rem' }}>
        Use these keys to integrate NylePay into your backend. Authenticate with <code style={{ background: 'var(--bg-tertiary)', padding: '.125rem .375rem', borderRadius: 4, fontFamily: 'var(--font-mono)', fontSize: '.8125rem' }}>Authorization: Bearer npy_sec_...</code>
      </p>

      <div className="card" style={{ marginBottom: '1.5rem' }}>
        <h3 style={{ marginBottom: '1rem' }}>Quick Start</h3>
        <div className="code-block" style={{ fontSize: '.8125rem', lineHeight: 1.8 }}>
{`# Initiate M-Pesa STK Push
curl -X POST https://api.nylepay.com/api/v1/merchant/charges \\
  -H "Authorization: Bearer npy_sec_YOUR_KEY" \\
  -H "Content-Type: application/json" \\
  -d '{
    "method": "MPESA",
    "phone": "254712345678",
    "amount": 1500,
    "reference": "ORDER-001"
  }'`}
        </div>
      </div>

      <div className="card">
        <h3 style={{ marginBottom: '1rem' }}>Available Endpoints</h3>
        <table style={{ width: '100%', borderCollapse: 'collapse' }}>
          <thead>
            <tr style={{ borderBottom: '1px solid var(--border-color)' }}>
              <th style={{ padding: '.75rem 0', textAlign: 'left', color: 'var(--text-secondary)', fontSize: '.8125rem', fontWeight: 500 }}>Method</th>
              <th style={{ padding: '.75rem 0', textAlign: 'left', color: 'var(--text-secondary)', fontSize: '.8125rem', fontWeight: 500 }}>Endpoint</th>
              <th style={{ padding: '.75rem 0', textAlign: 'left', color: 'var(--text-secondary)', fontSize: '.8125rem', fontWeight: 500 }}>Description</th>
            </tr>
          </thead>
          <tbody>
            {[
              ['POST', '/api/v1/merchant/charges',   'Initiate M-Pesa STK Push'],
              ['POST', '/api/v1/merchant/transfers',  'Payout to external account'],
              ['GET',  '/api/v1/merchant/balance',    'Check settlement balance'],
            ].map(([method, path, desc], i) => (
              <tr key={i} style={{ borderBottom: '1px solid rgba(255,255,255,.04)' }}>
                <td style={{ padding: '.75rem 0' }}>
                  <span className={`badge ${method === 'POST' ? 'badge-success' : 'badge-warning'}`}>{method}</span>
                </td>
                <td style={{ padding: '.75rem 0', fontFamily: 'var(--font-mono)', fontSize: '.8125rem' }}>{path}</td>
                <td style={{ padding: '.75rem 0', color: 'var(--text-secondary)', fontSize: '.875rem' }}>{desc}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </>
  );
}

/* ─────────────── Settings Tab ─────────────── */
function SettingsTab() {
  return (
    <>
      <h1 style={{ fontSize: '1.75rem', marginBottom: '.5rem' }}>Settings</h1>
      <p style={{ color: 'var(--text-secondary)', marginBottom: '2rem' }}>Manage your merchant account, settlement details, and preferences.</p>

      <div className="card" style={{ marginBottom: '1.5rem' }}>
        <h3 style={{ marginBottom: '1rem' }}>Settlement Account</h3>
        <form onSubmit={(e) => e.preventDefault()}>
          <div className="form-group">
            <label className="form-label">Settlement Type</label>
            <select className="form-input" defaultValue="MPESA">
              <option value="MPESA">M-Pesa</option>
              <option value="BANK">Bank Account</option>
            </select>
          </div>
          <div className="form-group">
            <label className="form-label">M-Pesa Number</label>
            <input className="form-input" type="text" placeholder="2547XXXXXXXX" />
          </div>
          <button className="btn-primary">Save Settlement Account</button>
        </form>
      </div>

      <div className="card">
        <h3 style={{ marginBottom: '1rem', color: 'var(--danger)' }}>Danger Zone</h3>
        <p style={{ color: 'var(--text-secondary)', marginBottom: '1rem', fontSize: '.875rem' }}>
          Regenerating your API keys will immediately invalidate all existing keys. Your integrations will stop working until you update them.
        </p>
        <button className="btn-outline" style={{ borderColor: 'var(--danger)', color: 'var(--danger)' }}>
          Regenerate API Keys
        </button>
      </div>
    </>
  );
}
