import React, { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../context/useAuth';
import ThemeToggle from '../components/ThemeToggle';
import ApiDocs from './ApiDocs';

const NAV = [
  { id: 'overview', label: 'Overview', icon: 'grid' },
  { id: 'routes', label: 'Routes', icon: 'route' },
  { id: 'settlements', label: 'Settlements', icon: 'wallet' },
  { id: 'api', label: 'API Keys', icon: 'key' },
  { id: 'live', label: 'Go Live', icon: 'shield' },
  { id: 'docs', label: 'API Docs', icon: 'docs' },
  { id: 'settings', label: 'Settings', icon: 'settings' },
];

const API_BASE = import.meta.env.VITE_API_BASE_URL || 'https://nyle-pay.onrender.com';

const sampleRoutes = [
  ['RT-KE-1001', 'M-Pesa', 'Business M-Pesa', 'KES 0.00', 'Ready', 'Realtime'],
  ['RT-KE-1002', 'Airtel Money', 'PesaLink bank account', 'KES 0.00', 'Ready', 'Bank switch'],
  ['RT-KE-1003', 'USDT', 'Airtel Money', 'KES 0.00', 'Sandbox', 'FX + settlement'],
];

function Icon({ name }) {
  const props = { width: 16, height: 16, viewBox: '0 0 24 24', fill: 'none', stroke: 'currentColor', strokeWidth: 2 };
  const paths = {
    grid: <><rect x="3" y="3" width="7" height="7" /><rect x="14" y="3" width="7" height="7" /><rect x="3" y="14" width="7" height="7" /><rect x="14" y="14" width="7" height="7" /></>,
    route: <><circle cx="6" cy="6" r="3" /><circle cx="18" cy="18" r="3" /><path d="M9 6h4a5 5 0 010 10h-2" /></>,
    wallet: <><path d="M20 7H5a2 2 0 00-2 2v9a2 2 0 002 2h15a2 2 0 002-2V9a2 2 0 00-2-2z" /><path d="M16 12h4v4h-4a2 2 0 010-4z" /><path d="M18 7V5a2 2 0 00-2-2H6a2 2 0 00-2 2v2" /></>,
    key: <><path d="M21 2l-2 2m-7.61 7.61a5.5 5.5 0 11-7.778 7.778 5.5 5.5 0 017.777-7.777zm0 0L15.5 7.5m0 0l3 3L22 7l-3-3m-3.5 3.5L19 4" /></>,
    docs: <><path d="M14 2H6a2 2 0 00-2 2v16a2 2 0 002 2h12a2 2 0 002-2V8z" /><polyline points="14 2 14 8 20 8" /><line x1="16" y1="13" x2="8" y2="13" /><line x1="16" y1="17" x2="8" y2="17" /></>,
    shield: <><path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z" /><path d="M9 12l2 2 4-5" /></>,
    settings: <><circle cx="12" cy="12" r="3" /><path d="M19.4 15a1.65 1.65 0 00.33 1.82l.06.06a2 2 0 010 2.83 2 2 0 01-2.83 0l-.06-.06a1.65 1.65 0 00-1.82-.33 1.65 1.65 0 00-1 1.51V21a2 2 0 01-4 0v-.09A1.65 1.65 0 009 19.4a1.65 1.65 0 00-1.82.33l-.06.06a2 2 0 01-2.83-2.83l.06-.06A1.65 1.65 0 004.68 15a1.65 1.65 0 00-1.51-1H3a2 2 0 010-4h.09A1.65 1.65 0 004.6 9a1.65 1.65 0 00-.33-1.82l-.06-.06a2 2 0 012.83-2.83l.06.06A1.65 1.65 0 009 4.68a1.65 1.65 0 001-1.51V3a2 2 0 014 0v.09a1.65 1.65 0 001 1.51 1.65 1.65 0 001.82-.33l.06-.06a2 2 0 012.83 2.83l-.06.06A1.65 1.65 0 0019.4 9a1.65 1.65 0 001.51 1H21a2 2 0 010 4h-.09A1.65 1.65 0 0019.4 15z" /></>,
  };
  return <svg {...props}>{paths[name]}</svg>;
}

export default function Dashboard() {
  const { user, logout } = useAuth();
  const navigate = useNavigate();
  const [tab, setTab] = useState('overview');
  const currentYear = new Date().getFullYear();
  const workspaceName = 'Sandbox Workspace';
  const initial = (user?.fullName || user?.email || 'N').charAt(0).toUpperCase();

  return (
    <div className="dash-layout">
      <aside className="dash-sidebar">
        <div style={S.sidebarTop}>
          <div style={S.sidebarLogo}>
            <div style={S.logoMark}><Icon name="route" /></div>
            <div>
              <div style={S.sidebarBrand}>NylePay</div>
              <div style={S.sidebarSub}>Developer Dashboard</div>
            </div>
          </div>
        </div>

        <nav style={S.sidebarNav}>
          <div style={S.navSection}>OPERATIONS</div>
          {NAV.map((item) => (
            <button key={item.id} className={`nav-item ${tab === item.id ? 'active' : ''}`} onClick={() => setTab(item.id)}>
              <Icon name={item.icon} />
              {item.label}
            </button>
          ))}
        </nav>

        <div style={S.sandboxBanner}>
          <div style={S.sandboxDot} />
          <div>
            <div style={S.sandboxTitle}>Sandbox Ready</div>
            <div style={S.sandboxSub}>Real API, simulated money</div>
          </div>
        </div>

        <div style={S.sidebarFooter}>
          <div style={S.userRow}>
            <div style={S.avatar}>{initial}</div>
            <div style={{ flex: 1, minWidth: 0 }}>
              <div style={S.userName}>{workspaceName}</div>
              <div style={S.userEmail}>{user?.email || '-'}</div>
            </div>
            <ThemeToggle />
          </div>
          <button className="btn-ghost" style={S.signOut} onClick={() => { logout(); navigate('/'); }}>
            Sign out
          </button>
          <div style={S.copyright}>Copyright (c) {currentYear} NylePay Business.</div>
        </div>
      </aside>

      <div className="dash-main">
        {tab === 'overview' && <OverviewTab workspaceName={workspaceName} user={user} setTab={setTab} />}
        {tab === 'routes' && <RoutesTab />}
        {tab === 'settlements' && <SettlementsTab />}
        {tab === 'api' && <ApiKeysTab user={user} setTab={setTab} />}
        {tab === 'live' && <GoLiveTab />}
        {tab === 'docs' && <div style={{ padding: '2rem' }}><ApiDocs embedded /></div>}
        {tab === 'settings' && <SettingsTab user={user} />}
      </div>
    </div>
  );
}

function OverviewTab({ workspaceName, user, setTab }) {
  const hasKeys = !!user?.apiKeys;
  const metrics = [
    { label: 'Sandbox Routes', value: '0', sub: 'Ready for API tests', color: '#0f172a' },
    { label: 'Test Balance', value: 'KES 250K', sub: 'Simulated settlement float', color: '#059669' },
    { label: 'Live Status', value: 'Off', sub: 'Use Go Live for activation', color: '#0f172a' },
    { label: 'Sandbox Limit', value: 'KES 100K', sub: 'Per simulated movement', color: '#0f172a' },
  ];

  return (
    <>
      <div className="dash-header">
        <div>
          <h1 style={S.pageTitle}>Overview</h1>
          <p style={S.pageSub}>{workspaceName} for testing routes before production activation</p>
        </div>
        <button className="btn-primary" onClick={() => setTab('routes')}>
          <Icon name="route" />
          Test Route
        </button>
      </div>

      <div className="dash-content">
        {!hasKeys && (
          <div style={S.onboarding}>
            <div style={S.onboardingIcon}><Icon name="key" /></div>
            <div style={{ flex: 1 }}>
              <div style={S.onboardingTitle}>Sandbox workspace ready</div>
              <div style={S.onboardingText}>Use real sandbox API keys against the live backend. Sandbox calls are capped and never move real money.</div>
            </div>
            <button className="btn-primary" style={S.smallPrimary} onClick={() => setTab('api')}>View sandbox keys</button>
            <button className="btn-outline" style={S.smallPrimary} onClick={() => setTab('live')}>Go Live / Higher Limits</button>
          </div>
        )}

        <div style={S.metricsGrid}>
          {metrics.map((metric) => (
            <div className="metric-card" key={metric.label}>
              <div className="metric-label">{metric.label}</div>
              <div className="metric-value" style={{ color: metric.color }}>{metric.value}</div>
              <div className="metric-delta">{metric.sub}</div>
            </div>
          ))}
        </div>

        <div style={S.operationGrid}>
          <div className="card">
            <h3 style={S.cardTitle}>Routing promise</h3>
            <p style={S.cardCopy}>NylePay Business should let a company collect from any supported rail and settle to the account it chooses in real time, subject to confirmation and risk checks.</p>
            <div style={S.routePreview}>
              <span>Customer rail</span><strong>M-Pesa / Airtel Money / Card / Crypto / Wallet</strong>
              <span>Destination</span><strong>M-Pesa / Airtel Money / PesaLink / Bank / Wallet</strong>
            </div>
          </div>

          <div className="card">
            <h3 style={S.cardTitle}>Next actions</h3>
            {[
              ['Add fallback settlement account', 'Protect payouts when one rail is unavailable.'],
              ['Test route quote API', 'Compare provider fee, speed, and destination.'],
              ['Configure webhook retry URL', 'Keep order fulfillment tied to signed events.'],
            ].map(([title, body]) => (
              <div key={title} style={S.actionRow}>
                <div><strong>{title}</strong><p>{body}</p></div>
              </div>
            ))}
          </div>
        </div>
      </div>
    </>
  );
}

function RoutesTab() {
  return (
    <>
      <div className="dash-header">
        <div>
          <h1 style={S.pageTitle}>Routes</h1>
          <p style={S.pageSub}>Collections, conversions, payouts, and settlement legs</p>
        </div>
        <div style={S.headerActions}>
          <button className="btn-outline" style={S.compactButton}>Export CSV</button>
          <button className="btn-primary" style={S.compactButton}>New route</button>
        </div>
      </div>
      <div className="dash-content">
        <div className="card" style={{ marginBottom: '1.5rem' }}>
          <h3 style={S.cardTitle}>Route builder</h3>
          <div style={S.builderGrid}>
            <select className="form-input"><option>From M-Pesa</option><option>From Airtel Money</option><option>From card</option><option>From USDT</option><option>From NylePay wallet</option></select>
            <select className="form-input"><option>To business M-Pesa</option><option>To Airtel Money</option><option>To PesaLink bank account</option><option>To bank account</option><option>To NylePay wallet</option><option>To Paybill</option></select>
            <input className="form-input" placeholder="Amount, e.g. 1500" />
            <button className="btn-primary" style={{ justifyContent: 'center' }}>Quote route</button>
          </div>
          <p style={S.formNote}>This UI should call /api/routes/quote, show fees and speed, then execute through /api/routes/execute.</p>
        </div>

        <div className="card">
          <table className="data-table">
            <thead>
              <tr>{['Route ID', 'Source', 'Destination', 'Amount', 'Status', 'Settlement'].map((h) => <th key={h}>{h}</th>)}</tr>
            </thead>
            <tbody>
              {sampleRoutes.map((row) => (
                <tr key={row[0]}>{row.map((cell) => <td key={cell}>{cell}</td>)}</tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>
    </>
  );
}

function SettlementsTab() {
  return (
    <>
      <div className="dash-header">
        <div>
          <h1 style={S.pageTitle}>Settlements</h1>
          <p style={S.pageSub}>Choose where NylePay sends business funds after route confirmation</p>
        </div>
      </div>
      <div className="dash-content">
        <div style={S.operationGrid}>
          {[
            ['Primary destination', 'M-Pesa', '2547XXXXXXXX', 'Realtime'],
            ['Fallback destination', 'Airtel Money', '2547XXXXXXXX', 'Realtime'],
            ['Bank-switch destination', 'PesaLink', 'Bank ****7890', 'Near real-time where supported'],
            ['Reserve wallet', 'NylePay Wallet', 'Active after onboarding', 'Instant internal ledger'],
          ].map(([title, rail, value, speed]) => (
            <div className="card" key={title}>
              <h3 style={S.cardTitle}>{title}</h3>
              <p style={S.cardCopy}>{rail}</p>
              <strong style={S.destinationValue}>{value}</strong>
              <div className="badge badge-green" style={{ marginTop: '1rem' }}>{speed}</div>
            </div>
          ))}
        </div>

        <div className="card" style={{ marginTop: '1.5rem' }}>
          <h3 style={S.cardTitle}>Settlement policy</h3>
          <div style={S.builderGrid}>
            <select className="form-input"><option>Settle every successful route</option><option>Settle when balance reaches threshold</option><option>Settle on schedule</option></select>
            <select className="form-input"><option>Prefer lowest fee</option><option>Prefer fastest route</option><option>Prefer M-Pesa first</option><option>Prefer Airtel Money first</option><option>Prefer PesaLink for bank settlement</option></select>
            <input className="form-input" placeholder="Minimum threshold, e.g. 1000" />
            <button className="btn-primary" style={{ justifyContent: 'center' }}>Save policy</button>
          </div>
        </div>
      </div>
    </>
  );
}

function ApiKeysTab({ user, setTab }) {
  const [show, setShow] = useState({});
  const [copied, setCopied] = useState('');
  const [sandboxKeys, setSandboxKeys] = useState(null);
  const [loadingKeys, setLoadingKeys] = useState(true);
  const [keyError, setKeyError] = useState('');
  const keys = user?.apiKeys;

  useEffect(() => {
    let cancelled = false;
    const loadSandboxKeys = async () => {
      if (!user?.token) return;
      setLoadingKeys(true);
      setKeyError('');
      try {
        const res = await fetch(`${API_BASE}/api/business/sandbox-keys`, {
          headers: { Authorization: `Bearer ${user.token}` },
        });
        const json = await res.json();
        if (!res.ok || !json.success) throw new Error(json.message || 'Could not load sandbox keys');
        if (!cancelled) setSandboxKeys(json.data);
      } catch (err) {
        if (!cancelled) setKeyError(err.message || 'Could not load sandbox keys');
      } finally {
        if (!cancelled) setLoadingKeys(false);
      }
    };
    loadSandboxKeys();
    return () => { cancelled = true; };
  }, [user?.token]);

  const copy = (value, key) => {
    if (!value) return;
    navigator.clipboard.writeText(value);
    setCopied(key);
    setTimeout(() => setCopied(''), 2000);
  };

  const KeyRow = ({ label, hint, value, id, secret }) => (
    <div style={{ marginBottom: '1.25rem' }}>
      <div style={S.keyHeader}>
        <label style={S.keyLabel}>{label}</label>
        {secret && <span style={S.serverOnly}>Server-side only</span>}
      </div>
      <div className="key-field">
        <div className="key-value">{secret ? 'Shown once during business registration' : show[id] ? value : '*'.repeat(32)}</div>
        {!secret && <button className="key-copy" onClick={() => setShow((current) => ({ ...current, [id]: !current[id] }))}>{show[id] ? 'Hide' : 'Reveal'}</button>}
        {!secret && <button className="key-copy" onClick={() => copy(value, id)}>{copied === id ? 'Copied' : 'Copy'}</button>}
      </div>
      {hint && <p style={S.hint}>{hint}</p>}
    </div>
  );

  return (
    <>
      <div className="dash-header">
        <div>
          <h1 style={S.pageTitle}>API Keys & Webhooks</h1>
          <p style={S.pageSub}>Real credentials for sandbox testing and production routing</p>
        </div>
      </div>
      <div className="dash-content">
        <div className="card" style={{ marginBottom: '1.5rem' }}>
          <h2 style={S.cardTitle}>Sandbox Keys</h2>
          {loadingKeys && <p style={S.cardCopy}>Loading sandbox credentials from the NylePay API...</p>}
          {keyError && <div className="alert alert-error" style={{ marginBottom: '1rem' }}>{keyError}</div>}
          {sandboxKeys && (
            <>
              <KeyRow label="Sandbox Public Key" hint="Use in checkout initialization and sandbox route simulations." value={sandboxKeys.publicKey} id="sandboxPub" />
              <KeyRow label="Sandbox Secret Key" hint="Use in server-to-server test calls. Sandbox calls are real API calls but simulated money movement." value={sandboxKeys.secretKey} id="sandboxSec" />
              <KeyRow label="Sandbox Webhook Secret" hint="Use to verify signed sandbox webhooks." value={sandboxKeys.webhookSecret} id="sandboxHook" />
              <div className="alert alert-info">
                Sandbox requests authenticate against the same API as production. Amounts above KES 100,000 are blocked in sandbox.
              </div>
            </>
          )}
        </div>

        <div className="card" style={{ marginBottom: '1.5rem' }}>
          <h2 style={S.cardTitle}>Production Keys</h2>
          {keys ? (
            <>
              <KeyRow label="Public Key" hint="Safe for client-side checkout initialization." value={keys.publicKey} id="pub" />
              <KeyRow label="Secret Key" hint="Keep in backend environment variables as NYLEPAY_SECRET_KEY." secret />
              <KeyRow label="Webhook Signing Secret" hint="Use to verify X-NylePay-Signature on route events." secret />
            </>
          ) : (
            <div style={S.emptyState}>
              <Icon name="key" />
              <p>Production keys are issued after Go Live approval.</p>
              <button className="btn-primary" style={{ marginTop: '1rem' }} onClick={() => setTab('live')}>Request production activation</button>
            </div>
          )}
        </div>

        <div className="card">
          <h3 style={{ ...S.cardTitle, color: 'var(--danger)' }}>Key rotation</h3>
          <p style={S.cardCopy}>Regenerating keys must be a backend action that invalidates old credentials and creates an audit event. This button is intentionally not local-only.</p>
          <button className="btn-danger">Request key rotation</button>
        </div>
      </div>
    </>
  );
}

function GoLiveTab() {
  const sections = [
    ['Business identity', 'Business registration or incorporation certificate, current company search or CR12, business KRA PIN, permits or sector licences, business address, and business activity description.'],
    ['Owners and operators', 'Director, owner, partner, and signatory IDs, KRA PINs, contact details, address proof, passport photos where required, and authority to operate.'],
    ['Beneficial ownership', 'Beneficial owner details, ownership percentages, voting/control structure, BO filing where applicable, share register or ownership chart.'],
    ['Settlement setup', 'Bank account evidence, M-PESA Till or Paybill details, Airtel Money merchant details where enabled, PesaLink bank destination, webhook URL, and reconciliation email.'],
    ['Risk and compliance', 'Expected volumes, average ticket size, source of funds, countries served, regulated-sector evidence, AML screening consent, and acceptable-use declaration.'],
  ];

  return (
    <>
      <div className="dash-header">
        <div>
          <h1 style={S.pageTitle}>Go Live</h1>
          <p style={S.pageSub}>Submit production activation information for real-money routing, higher limits, settlement rails, and production API keys.</p>
        </div>
      </div>
      <div className="dash-content">
        <div className="card" style={{ marginBottom: '1.5rem' }}>
          <h2 style={S.cardTitle}>Live activation request</h2>
          <div style={S.builderGrid}>
            <input className="form-input" placeholder="Registered business name" />
            <input className="form-input" placeholder="Business KRA PIN" />
            <input className="form-input" placeholder="Expected monthly volume, e.g. KES 500,000" />
            <select className="form-input"><option>Primary rail: M-Pesa</option><option>Airtel Money</option><option>PesaLink</option><option>Bank settlement</option></select>
            <input className="form-input" placeholder="Compliance contact email" />
            <button className="btn-primary" style={{ justifyContent: 'center' }}>Save live request</button>
          </div>
        </div>

        <div style={S.operationGrid}>
          {sections.map(([title, body]) => (
            <div className="card" key={title}>
              <h3 style={S.cardTitle}>{title}</h3>
              <p style={S.cardCopy}>{body}</p>
              <input className="form-input" type="file" />
            </div>
          ))}
        </div>
      </div>
    </>
  );
}

function SettingsTab({ user }) {
  return (
    <>
      <div className="dash-header">
        <h1 style={S.pageTitle}>Settings</h1>
      </div>
      <div className="dash-content">
        <div className="card" style={{ marginBottom: '1.5rem' }}>
          <h3 style={S.cardTitle}>Workspace Profile</h3>
          <div style={S.builderGrid}>
            <input className="form-input" defaultValue={user?.fullName || ''} placeholder="Operator name" />
            <input className="form-input" type="email" defaultValue={user?.email || ''} placeholder="developer@example.com" />
            <select className="form-input"><option>Kenya sandbox</option><option>Uganda sandbox - planned</option><option>Tanzania sandbox - planned</option></select>
            <button className="btn-primary" style={{ justifyContent: 'center' }}>Save changes</button>
          </div>
        </div>

        <div className="card" style={{ marginBottom: '1.5rem' }}>
          <h3 style={S.cardTitle}>Webhook Configuration</h3>
          <p style={S.cardCopy}>NylePay posts signed route.created, route.processing, route.completed, route.failed, settlement.completed, and webhook.retry events.</p>
          <div style={S.builderGrid}>
            <input className="form-input" type="url" placeholder="https://yourapp.com/api/nylepay/webhook" />
            <select className="form-input"><option>Send all route events</option><option>Only final states</option><option>Failures and settlement events</option></select>
            <button className="btn-primary" style={{ justifyContent: 'center' }}>Save webhook</button>
          </div>
        </div>
      </div>
    </>
  );
}

const S = {
  sidebarTop: { padding: '1.25rem 1rem', borderBottom: '1px solid var(--border)' },
  sidebarLogo: { display: 'flex', alignItems: 'center', gap: '0.75rem' },
  logoMark: { width: 32, height: 32, borderRadius: 8, background: 'linear-gradient(135deg,#1d4ed8,#3b82f6)', color: '#fff', display: 'flex', alignItems: 'center', justifyContent: 'center', flexShrink: 0 },
  sidebarBrand: { fontWeight: 800, fontSize: '1rem', letterSpacing: 0, color: 'var(--text-primary)' },
  sidebarSub: { fontSize: '0.68rem', color: 'var(--text-tertiary)', fontWeight: 500 },
  sidebarNav: { flex: 1, padding: '0.875rem 0.75rem', display: 'flex', flexDirection: 'column', gap: '0.125rem', overflowY: 'auto' },
  navSection: { fontSize: '0.65rem', fontWeight: 700, letterSpacing: '0.1em', textTransform: 'uppercase', color: 'var(--text-tertiary)', padding: '0.5rem 0.5rem 0.375rem', marginTop: '0.25rem' },
  sandboxBanner: { margin: '0 0.75rem 0.75rem', padding: '0.625rem 0.875rem', background: 'var(--warning-light)', border: '1px solid #fde68a', borderRadius: 'var(--radius-sm)', display: 'flex', alignItems: 'center', gap: '0.5rem' },
  sandboxDot: { width: 8, height: 8, borderRadius: '50%', background: '#d97706', flexShrink: 0 },
  sandboxTitle: { fontSize: '0.75rem', fontWeight: 700, color: '#92400e' },
  sandboxSub: { fontSize: '0.68rem', color: '#b45309' },
  sidebarFooter: { padding: '0.75rem', borderTop: '1px solid var(--border)' },
  copyright: { marginTop: '0.75rem', color: 'var(--text-muted)', fontSize: '0.72rem', lineHeight: 1.5 },
  userRow: { display: 'flex', alignItems: 'center', gap: '0.5rem' },
  avatar: { width: 32, height: 32, borderRadius: 8, background: 'var(--brand)', color: '#fff', display: 'flex', alignItems: 'center', justifyContent: 'center', fontWeight: 700, fontSize: '0.875rem', flexShrink: 0 },
  userName: { fontSize: '0.8125rem', fontWeight: 600, color: 'var(--text-primary)', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' },
  userEmail: { fontSize: '0.7rem', color: 'var(--text-tertiary)', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' },
  signOut: { width: '100%', marginTop: '0.5rem', justifyContent: 'center', color: 'var(--gray-500)', fontSize: '0.8125rem' },
  pageTitle: { fontSize: '1.25rem', fontWeight: 700, letterSpacing: 0 },
  pageSub: { fontSize: '0.8125rem', color: 'var(--text-secondary)', marginTop: '0.1rem' },
  metricsGrid: { display: 'grid', gridTemplateColumns: 'repeat(4, 1fr)', gap: '1rem' },
  operationGrid: { display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '1.5rem', marginTop: '1.5rem' },
  onboarding: { display: 'flex', alignItems: 'center', gap: '1rem', background: 'var(--blue-50)', border: '1px solid var(--blue-100)', borderRadius: 'var(--radius-md)', padding: '1rem 1.25rem', marginBottom: '1.5rem' },
  onboardingIcon: { width: 44, height: 44, borderRadius: 8, background: '#dbeafe', color: '#2563eb', display: 'flex', alignItems: 'center', justifyContent: 'center', flexShrink: 0 },
  onboardingTitle: { fontWeight: 700, color: '#1e40af', marginBottom: '0.25rem' },
  onboardingText: { fontSize: '0.84rem', color: '#3b82f6' },
  smallPrimary: { fontSize: '0.84rem', padding: '0.5rem 1rem', whiteSpace: 'nowrap' },
  cardTitle: { fontSize: '0.9375rem', fontWeight: 700, letterSpacing: 0, marginBottom: '0.75rem' },
  cardCopy: { fontSize: '0.86rem', color: 'var(--text-secondary)', lineHeight: 1.65, marginBottom: '1rem' },
  routePreview: { display: 'grid', gridTemplateColumns: '120px 1fr', gap: '0.5rem 1rem', fontSize: '0.84rem', color: 'var(--text-secondary)' },
  actionRow: { padding: '0.75rem 0', borderTop: '1px solid var(--border)' },
  headerActions: { display: 'flex', gap: '0.5rem' },
  compactButton: { fontSize: '0.8125rem', padding: '0.5rem 0.875rem' },
  builderGrid: { display: 'grid', gridTemplateColumns: 'repeat(4, minmax(0, 1fr))', gap: '0.75rem', alignItems: 'start' },
  formNote: { color: 'var(--text-tertiary)', fontSize: '0.78rem', marginTop: '0.75rem' },
  destinationValue: { display: 'block', color: 'var(--text-primary)', fontSize: '1rem' },
  keyHeader: { display: 'flex', justifyContent: 'space-between', alignItems: 'baseline', marginBottom: '0.375rem' },
  keyLabel: { fontSize: '0.8125rem', fontWeight: 600, color: 'var(--text-secondary)' },
  serverOnly: { fontSize: '0.72rem', color: 'var(--danger)', fontWeight: 600, background: 'var(--danger-light)', padding: '0.1rem 0.4rem', borderRadius: '4px' },
  hint: { fontSize: '0.73rem', color: 'var(--text-tertiary)', marginTop: '0.3rem' },
  emptyState: { textAlign: 'center', padding: '3rem 1rem', display: 'flex', flexDirection: 'column', alignItems: 'center', color: 'var(--text-tertiary)' },
};
