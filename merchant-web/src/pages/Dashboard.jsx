import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import ThemeToggle from '../components/ThemeToggle';
import ApiDocs from './ApiDocs';

const NAV = [
  { id: 'overview', label: 'Overview', icon: <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"><rect x="3" y="3" width="7" height="7"/><rect x="14" y="3" width="7" height="7"/><rect x="3" y="14" width="7" height="7"/><rect x="14" y="14" width="7" height="7"/></svg> },
  { id: 'payments', label: 'Payments', icon: <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"><rect x="2" y="5" width="20" height="14" rx="2"/><path d="M2 10h20"/></svg> },
  { id: 'api',      label: 'API Keys',  icon: <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"><path d="M21 2l-2 2m-7.61 7.61a5.5 5.5 0 11-7.778 7.778 5.5 5.5 0 017.777-7.777zm0 0L15.5 7.5m0 0l3 3L22 7l-3-3m-3.5 3.5L19 4"/></svg> },
  { id: 'docs',     label: 'API Docs',  icon: <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"><path d="M14 2H6a2 2 0 00-2 2v16a2 2 0 002 2h12a2 2 0 002-2V8z"/><polyline points="14 2 14 8 20 8"/><line x1="16" y1="13" x2="8" y2="13"/><line x1="16" y1="17" x2="8" y2="17"/><polyline points="10 9 9 9 8 9"/></svg> },
  { id: 'settings', label: 'Settings',  icon: <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"><circle cx="12" cy="12" r="3"/><path d="M19.4 15a1.65 1.65 0 00.33 1.82l.06.06a2 2 0 010 2.83 2 2 0 01-2.83 0l-.06-.06a1.65 1.65 0 00-1.82-.33 1.65 1.65 0 00-1 1.51V21a2 2 0 01-4 0v-.09A1.65 1.65 0 009 19.4a1.65 1.65 0 00-1.82.33l-.06.06a2 2 0 01-2.83-2.83l.06-.06A1.65 1.65 0 004.68 15a1.65 1.65 0 00-1.51-1H3a2 2 0 010-4h.09A1.65 1.65 0 004.6 9a1.65 1.65 0 00-.33-1.82l-.06-.06a2 2 0 012.83-2.83l.06.06A1.65 1.65 0 009 4.68a1.65 1.65 0 001-1.51V3a2 2 0 014 0v.09a1.65 1.65 0 001 1.51 1.65 1.65 0 001.82-.33l.06-.06a2 2 0 012.83 2.83l-.06.06A1.65 1.65 0 0019.4 9a1.65 1.65 0 001.51 1H21a2 2 0 010 4h-.09a1.65 1.65 0 00-1.51 1z"/></svg> },
];

export default function Dashboard() {
  const { user, logout } = useAuth();
  const navigate = useNavigate();
  const [tab, setTab] = useState('overview');

  const biz = user?.businessName || 'Your Business';
  const initial = biz.charAt(0).toUpperCase();

  return (
    <div className="dash-layout">
      {/* ── Sidebar ── */}
      <aside className="dash-sidebar">
        <div style={S.sidebarTop}>
          <div style={S.sidebarLogo}>
            <div style={S.logoMark}>
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none">
                <path d="M12 2L2 7l10 5 10-5-10-5zM2 17l10 5 10-5M2 12l10 5 10-5" stroke="#fff" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"/>
              </svg>
            </div>
            <div>
              <div style={S.sidebarBrand}>NylePay</div>
              <div style={S.sidebarSub}>Merchant Dashboard</div>
            </div>
          </div>
        </div>

        <nav style={S.sidebarNav}>
          <div style={S.navSection}>MAIN MENU</div>
          {NAV.map(item => (
            <button key={item.id} className={`nav-item ${tab === item.id ? 'active' : ''}`} onClick={() => setTab(item.id)}>
              {item.icon}
              {item.label}
            </button>
          ))}
        </nav>

        {/* Sandbox mode indicator */}
        <div style={S.sandboxBanner}>
          <div style={S.sandboxDot} />
          <div>
            <div style={S.sandboxTitle}>Sandbox Mode</div>
            <div style={S.sandboxSub}>No real transactions</div>
          </div>
        </div>

        <div style={S.sidebarFooter}>
          <div style={S.userRow}>
            <div style={S.avatar}>{initial}</div>
            <div style={{ flex: 1, minWidth: 0 }}>
              <div style={S.userName}>{biz}</div>
              <div style={S.userEmail}>{user?.email || '—'}</div>
            </div>
            <ThemeToggle />
          </div>
          <button className="btn-ghost" style={{ width: '100%', marginTop: '0.5rem', justifyContent: 'center', color: 'var(--gray-500)', fontSize: '0.8125rem' }}
            onClick={() => { logout(); navigate('/'); }}>
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"><path d="M9 21H5a2 2 0 01-2-2V5a2 2 0 012-2h4"/><polyline points="16 17 21 12 16 7"/><line x1="21" y1="12" x2="9" y2="12"/></svg>
            Sign out
          </button>
        </div>
      </aside>

      {/* ── Main ── */}
      <div className="dash-main">
        {tab === 'overview' && <OverviewTab biz={biz} user={user} setTab={setTab} />}
        {tab === 'payments' && <PaymentsTab />}
        {tab === 'api'      && <ApiKeysTab user={user} />}
        {tab === 'docs'     && <div style={{ padding: '2rem' }}><ApiDocs /></div>}
        {tab === 'settings' && <SettingsTab user={user} />}
      </div>
    </div>
  );
}

/* ── Overview ── */
function OverviewTab({ biz, user, setTab }) {
  const hasKeys = !!user?.apiKeys;
  return (
    <>
      <div className="dash-header">
        <div>
          <h1 style={{ fontSize: '1.25rem', fontWeight: 700, letterSpacing: '-0.01em' }}>Overview</h1>
          <p style={{ fontSize: '0.8125rem', color: 'var(--text-secondary)', marginTop: '0.1rem' }}>Welcome back, {biz}</p>
        </div>
        <button className="btn-primary" onClick={() => setTab('api')}>
          <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5"><line x1="12" y1="5" x2="12" y2="19"/><line x1="5" y1="12" x2="19" y2="12"/></svg>
          Create Payment
        </button>
      </div>

      <div className="dash-content">
        {/* Onboarding callout if no keys */}
        {!hasKeys && (
          <div style={S.onboarding}>
            <div style={S.onboardingIcon}>
              <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="#2563eb" strokeWidth="2"><path d="M21 2l-2 2m-7.61 7.61a5.5 5.5 0 11-7.778 7.778 5.5 5.5 0 017.777-7.777zm0 0L15.5 7.5m0 0l3 3L22 7l-3-3m-3.5 3.5L19 4"/></svg>
            </div>
            <div style={{ flex: 1 }}>
              <div style={{ fontWeight: 700, color: '#1e40af', marginBottom: '0.25rem' }}>Complete your merchant setup</div>
              <div style={{ fontSize: '0.84rem', color: '#3b82f6' }}>Register your business to receive your live and sandbox API keys and start accepting payments.</div>
            </div>
            <a href="/register-business" className="btn-primary" style={{ fontSize: '0.84rem', padding: '0.5rem 1rem', whiteSpace: 'nowrap' }}>Register business →</a>
          </div>
        )}

        {/* Metrics */}
        <div style={S.metricsGrid}>
          {[
            { label: 'Available Balance', value: 'KES 0.00', sub: 'Available for withdrawal', color: '#059669' },
            { label: "Today's Volume", value: 'KES 0.00', sub: '0 successful transactions', color: '#0f172a' },
            { label: 'Pending Settlement', value: 'KES 0.00', sub: 'Settles in real-time', color: '#0f172a' },
            { label: 'Success Rate', value: '—', sub: 'No transactions yet', color: '#0f172a' },
          ].map(m => (
            <div className="metric-card" key={m.label}>
              <div className="metric-label">{m.label}</div>
              <div className="metric-value" style={{ color: m.color }}>{m.value}</div>
              <div className="metric-delta">{m.sub}</div>
            </div>
          ))}
        </div>

        {/* Recent activity */}
        <div className="card" style={{ marginTop: '1.5rem' }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1rem' }}>
            <h3 style={{ fontSize: '0.9375rem', fontWeight: 700 }}>Recent Transactions</h3>
            <button className="btn-ghost" style={{ fontSize: '0.8125rem' }}>View all →</button>
          </div>
          <div style={S.emptyState}>
            <svg width="40" height="40" viewBox="0 0 24 24" fill="none" stroke="#cbd5e1" strokeWidth="1.5"><rect x="2" y="5" width="20" height="14" rx="2"/><path d="M2 10h20"/></svg>
            <p style={{ color: 'var(--text-tertiary)', fontSize: '0.875rem', marginTop: '0.75rem' }}>No transactions yet</p>
            <p style={{ color: 'var(--text-tertiary)', fontSize: '0.8125rem' }}>Create a payment link or integrate the API to get started.</p>
          </div>
        </div>
      </div>
    </>
  );
}

/* ── Payments ── */
function PaymentsTab() {
  return (
    <>
      <div className="dash-header">
        <div>
          <h1 style={{ fontSize: '1.25rem', fontWeight: 700 }}>Payments</h1>
          <p style={{ fontSize: '0.8125rem', color: 'var(--text-secondary)', marginTop: '0.1rem' }}>All incoming transactions and settlement status</p>
        </div>
        <div style={{ display: 'flex', gap: '0.5rem' }}>
          <button className="btn-outline" style={{ fontSize: '0.8125rem', padding: '0.5rem 0.875rem' }}>
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"><path d="M21 15v4a2 2 0 01-2 2H5a2 2 0 01-2-2v-4"/><polyline points="7 10 12 15 17 10"/><line x1="12" y1="15" x2="12" y2="3"/></svg>
            Export CSV
          </button>
          <button className="btn-outline" style={{ fontSize: '0.8125rem', padding: '0.5rem 0.875rem' }}>Filter</button>
        </div>
      </div>
      <div className="dash-content">
        <div className="card">
          <div style={{ overflowX: 'auto' }}>
            <table className="data-table">
              <thead>
                <tr>
                  {['Reference', 'Method', 'Amount', 'Customer', 'Status', 'Date'].map(h => <th key={h}>{h}</th>)}
                </tr>
              </thead>
              <tbody>
                <tr>
                  <td colSpan={6} style={{ textAlign: 'center', padding: '3rem', color: 'var(--text-tertiary)' }}>
                    No payment records yet. Payments will appear here once your first transaction is completed.
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>
      </div>
    </>
  );
}

/* ── API Keys ── */
function ApiKeysTab({ user }) {
  const [show, setShow] = useState({});
  const [copied, setCopied] = useState('');
  const keys = user?.apiKeys;

  const copy = (val, k) => {
    navigator.clipboard.writeText(val);
    setCopied(k);
    setTimeout(() => setCopied(''), 2000);
  };

  const KeyRow = ({ label, hint, value, id, secret }) => (
    <div style={{ marginBottom: '1.25rem' }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'baseline', marginBottom: '0.375rem' }}>
        <label style={{ fontSize: '0.8125rem', fontWeight: 600, color: 'var(--text-secondary)' }}>{label}</label>
        {secret && <span style={{ fontSize: '0.72rem', color: 'var(--danger)', fontWeight: 600, background: 'var(--danger-light)', padding: '0.1rem 0.4rem', borderRadius: '4px' }}>Keep server-side only</span>}
      </div>
      <div className="key-field">
        <div className="key-value">{show[id] ? value : '•'.repeat(32)}</div>
        <button className="key-copy" onClick={() => setShow(s => ({ ...s, [id]: !s[id] }))}>{show[id] ? 'Hide' : 'Reveal'}</button>
        <button className="key-copy" onClick={() => copy(value, id)}>{copied === id ? '✓ Copied' : 'Copy'}</button>
      </div>
      {hint && <p style={{ fontSize: '0.73rem', color: 'var(--text-tertiary)', marginTop: '0.3rem' }}>{hint}</p>}
    </div>
  );

  return (
    <>
      <div className="dash-header">
        <div>
          <h1 style={{ fontSize: '1.25rem', fontWeight: 700 }}>API Keys & Webhooks</h1>
          <p style={{ fontSize: '0.8125rem', color: 'var(--text-secondary)', marginTop: '0.1rem' }}>Credentials for integrating NylePay into your product</p>
        </div>
      </div>
      <div className="dash-content">
        {/* Live keys */}
        <div className="card" style={{ marginBottom: '1.5rem' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem', marginBottom: '1.5rem' }}>
            <h2 style={{ fontSize: '1rem', fontWeight: 700, flex: 1 }}>Live Keys</h2>
            <span className="badge badge-green">
              <span style={{ width: 6, height: 6, borderRadius: '50%', background: '#059669', display: 'inline-block' }} />
              Active
            </span>
          </div>
          {keys ? (
            <>
              <KeyRow label="Public Key" hint="Safe to include in frontend or mobile code." value={keys.publicKey} id="pub" />
              <KeyRow label="Secret Key" value={keys.secretKey} id="sec" secret />
              <KeyRow label="Webhook Signing Secret" hint="Use this to verify the X-NylePay-Signature header on webhook events." value={keys.webhookSecret} id="hook" />
            </>
          ) : (
            <div style={S.emptyState}>
              <svg width="36" height="36" viewBox="0 0 24 24" fill="none" stroke="#cbd5e1" strokeWidth="1.5"><path d="M21 2l-2 2m-7.61 7.61a5.5 5.5 0 11-7.778 7.778 5.5 5.5 0 017.777-7.777zm0 0L15.5 7.5m0 0l3 3L22 7l-3-3"/></svg>
              <p style={{ color: 'var(--text-tertiary)', fontSize: '0.875rem', marginTop: '0.75rem' }}>Complete business registration to receive your API keys.</p>
              <a href="/register-business" className="btn-primary" style={{ marginTop: '1rem', fontSize: '0.875rem' }}>Register business →</a>
            </div>
          )}
        </div>

        {/* Sandbox keys */}
        <div className="card" style={{ marginBottom: '1.5rem' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem', marginBottom: '1.5rem' }}>
            <h2 style={{ fontSize: '1rem', fontWeight: 700, flex: 1 }}>Sandbox Keys</h2>
            <span className="badge badge-amber">Test Mode</span>
          </div>
          <div className="alert alert-info" style={{ marginBottom: '1.25rem' }}>
            <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" style={{ flexShrink: 0, marginTop: '1px' }}><circle cx="12" cy="12" r="10"/><path d="M12 16v-4M12 8h.01"/></svg>
            Sandbox keys trigger simulated payment flows with no real money movement. Use prefix <code style={{ fontFamily: 'var(--font-mono)', fontSize: '0.8rem' }}>npy_test_</code> to identify test credentials.
          </div>
          {keys ? (
            <>
              <KeyRow label="Sandbox Public Key" value={'npy_test_pub_' + keys.publicKey.slice(-16)} id="spub" hint="For frontend test integrations." />
              <KeyRow label="Sandbox Secret Key" value={'npy_test_sec_' + keys.secretKey.slice(-16)} id="ssec" secret />
            </>
          ) : (
            <p style={{ color: 'var(--text-tertiary)', fontSize: '0.875rem' }}>Sandbox keys are generated alongside your live keys after business registration.</p>
          )}
        </div>

        {/* Danger zone */}
        <div className="card" style={{ borderColor: 'var(--danger-light)' }}>
          <h3 style={{ fontSize: '0.9375rem', fontWeight: 700, color: 'var(--danger)', marginBottom: '0.5rem' }}>Danger Zone</h3>
          <p style={{ fontSize: '0.84rem', color: 'var(--text-secondary)', marginBottom: '1rem', lineHeight: 1.6 }}>
            Regenerating your API keys will immediately invalidate all existing credentials. Any live integrations using the old keys will stop working until updated.
          </p>
          <button className="btn-danger">Regenerate all keys</button>
        </div>
      </div>
    </>
  );
}

/* ── Settings ── */
function SettingsTab({ user }) {
  return (
    <>
      <div className="dash-header">
        <h1 style={{ fontSize: '1.25rem', fontWeight: 700 }}>Settings</h1>
      </div>
      <div className="dash-content">
        <div className="card" style={{ marginBottom: '1.5rem' }}>
          <h3 style={{ fontSize: '0.9375rem', fontWeight: 700, marginBottom: '1.25rem' }}>Business Profile</h3>
          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '0.75rem' }}>
            <div className="form-group">
              <label className="form-label">Business Name</label>
              <input className="form-input" defaultValue={user?.businessName || ''} placeholder="Acme Ltd" />
            </div>
            <div className="form-group">
              <label className="form-label">Business Email</label>
              <input className="form-input" type="email" defaultValue={user?.email || ''} placeholder="payments@acme.com" />
            </div>
          </div>
          <button className="btn-primary" style={{ marginTop: '0.5rem' }}>Save changes</button>
        </div>

        <div className="card" style={{ marginBottom: '1.5rem' }}>
          <h3 style={{ fontSize: '0.9375rem', fontWeight: 700, marginBottom: '0.5rem' }}>Settlement Account</h3>
          <p style={{ fontSize: '0.84rem', color: 'var(--text-secondary)', marginBottom: '1.25rem' }}>Where NylePay sends your real-time payouts after a successful transaction.</p>
          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '0.75rem' }}>
            <div className="form-group">
              <label className="form-label">Settlement Method</label>
              <select className="form-input"><option>M-Pesa</option><option>Bank Account</option></select>
            </div>
            <div className="form-group">
              <label className="form-label">M-Pesa Number</label>
              <input className="form-input" placeholder="2547XXXXXXXX" />
            </div>
          </div>
          <button className="btn-primary" style={{ marginTop: '0.5rem' }}>Update settlement</button>
        </div>

        <div className="card" style={{ marginBottom: '1.5rem' }}>
          <h3 style={{ fontSize: '0.9375rem', fontWeight: 700, marginBottom: '0.5rem' }}>Webhook Configuration</h3>
          <p style={{ fontSize: '0.84rem', color: 'var(--text-secondary)', marginBottom: '1.25rem' }}>NylePay posts signed events to your endpoint for every payment state change.</p>
          <div className="form-group">
            <label className="form-label">Webhook URL</label>
            <input className="form-input" type="url" placeholder="https://yourapp.com/api/nylepay/webhook" />
          </div>
          <div className="alert alert-info" style={{ marginBottom: '1rem' }}>
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" style={{ flexShrink: 0 }}><circle cx="12" cy="12" r="10"/><path d="M12 16v-4M12 8h.01"/></svg>
            Every POST includes an <code style={{ fontFamily: 'var(--font-mono)', fontSize: '0.78rem' }}>X-NylePay-Signature</code> header. Always verify it using your Webhook Signing Secret before acting on the event.
          </div>
          <button className="btn-primary">Save webhook URL</button>
        </div>
      </div>
    </>
  );
}

const S = {
  sidebarTop: { padding: '1.25rem 1rem', borderBottom: '1px solid var(--border)' },
  sidebarLogo: { display: 'flex', alignItems: 'center', gap: '0.75rem' },
  logoMark: { width: 32, height: 32, borderRadius: 8, background: 'linear-gradient(135deg,#1d4ed8,#3b82f6)', display: 'flex', alignItems: 'center', justifyContent: 'center', flexShrink: 0 },
  sidebarBrand: { fontWeight: 800, fontSize: '1rem', letterSpacing: '-0.01em', color: 'var(--text-primary)' },
  sidebarSub: { fontSize: '0.68rem', color: 'var(--text-tertiary)', fontWeight: 500 },
  sidebarNav: { flex: 1, padding: '0.875rem 0.75rem', display: 'flex', flexDirection: 'column', gap: '0.125rem', overflowY: 'auto' },
  navSection: { fontSize: '0.65rem', fontWeight: 700, letterSpacing: '0.1em', textTransform: 'uppercase', color: 'var(--text-tertiary)', padding: '0.5rem 0.5rem 0.375rem', marginTop: '0.25rem' },
  sandboxBanner: { margin: '0 0.75rem 0.75rem', padding: '0.625rem 0.875rem', background: 'var(--warning-light)', border: '1px solid #fde68a', borderRadius: 'var(--radius-sm)', display: 'flex', alignItems: 'center', gap: '0.5rem' },
  sandboxDot: { width: 8, height: 8, borderRadius: '50%', background: '#d97706', flexShrink: 0 },
  sandboxTitle: { fontSize: '0.75rem', fontWeight: 700, color: '#92400e' },
  sandboxSub: { fontSize: '0.68rem', color: '#b45309' },
  sidebarFooter: { padding: '0.75rem', borderTop: '1px solid var(--border)' },
  userRow: { display: 'flex', alignItems: 'center', gap: '0.5rem' },
  avatar: { width: 32, height: 32, borderRadius: 8, background: 'var(--brand)', color: '#fff', display: 'flex', alignItems: 'center', justifyContent: 'center', fontWeight: 700, fontSize: '0.875rem', flexShrink: 0 },
  userName: { fontSize: '0.8125rem', fontWeight: 600, color: 'var(--text-primary)', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' },
  userEmail: { fontSize: '0.7rem', color: 'var(--text-tertiary)', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' },
  metricsGrid: { display: 'grid', gridTemplateColumns: 'repeat(4, 1fr)', gap: '1rem' },
  emptyState: { textAlign: 'center', padding: '3rem 1rem', display: 'flex', flexDirection: 'column', alignItems: 'center' },
  onboarding: { display: 'flex', alignItems: 'center', gap: '1rem', background: 'var(--blue-50)', border: '1px solid var(--blue-100)', borderRadius: 'var(--radius-md)', padding: '1rem 1.25rem', marginBottom: '1.5rem' },
  onboardingIcon: { width: 44, height: 44, borderRadius: '10px', background: '#dbeafe', display: 'flex', alignItems: 'center', justifyContent: 'center', flexShrink: 0 },
};
