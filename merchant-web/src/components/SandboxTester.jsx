import React, { useState } from 'react';

const ENDPOINTS = [
  { id: 'health', label: 'Health Check', method: 'GET', path: '/api/sandbox/health', auth: false, body: null, desc: 'Verify the API is reachable and sandbox mode is active.' },
  { id: 'login', label: 'Authenticate', method: 'POST', path: '/api/auth/login', auth: false, body: { email: 'business@example.com', password: 'Password123' }, desc: 'Obtain a JWT token for dashboard and onboarding requests.' },
  { id: 'reg', label: 'Register Business', method: 'POST', path: '/api/business/register', auth: 'jwt', body: { businessName: 'Test Business', businessEmail: 'biz@test.com', settlementMethod: 'MPESA', settlementPhone: '254712345678', webhookUrl: 'https://example.com/webhook' }, desc: 'Create a business profile and receive API credentials.' },
  { id: 'caps', label: 'Capabilities', method: 'GET', path: '/api/routes/capabilities?country=KE', auth: 'secret', body: null, desc: 'Discover supported Kenyan source and destination rails.' },
  { id: 'quote-mpesa-bank', label: 'Quote M-Pesa to Bank', method: 'POST', path: '/api/routes/quote', auth: 'secret', body: { sourceRail: 'MPESA', destinationRail: 'BANK', sourceAsset: 'KSH', destinationAsset: 'KSH', amount: 1500, destination: { phone: '254712345678', bankCode: '01', accountNumber: '1234567890' }, idempotencyKey: 'TEST-ROUTE-001' }, desc: 'Quote a customer M-Pesa payment settling to a business bank account.' },
  { id: 'quote-airtel-pesalink', label: 'Quote Airtel to PesaLink', method: 'POST', path: '/api/routes/quote', auth: 'secret', body: { sourceRail: 'AIRTEL_MONEY', destinationRail: 'PESALINK', sourceAsset: 'KSH', destinationAsset: 'KSH', amount: 2500, destination: { phone: '254733123456', bankCode: '01', accountNumber: '1234567890', accountName: 'Test Business' }, idempotencyKey: 'TEST-ROUTE-PSL-001' }, desc: 'Quote Airtel Money collection settling through the PesaLink bank switch.' },
  { id: 'quote-wallet-airtel', label: 'Quote Wallet to Airtel', method: 'POST', path: '/api/routes/quote', auth: 'secret', body: { sourceRail: 'NYLEPAY_WALLET', destinationRail: 'AIRTEL_MONEY', sourceAsset: 'KSH', amount: 750, destination: { phone: '254733123456' }, idempotencyKey: 'TEST-ROUTE-AIR-001' }, desc: 'Quote a NylePay wallet payout to Airtel Money.' },
  { id: 'quote-crypto-mpesa', label: 'Quote USDT to M-Pesa', method: 'POST', path: '/api/routes/quote', auth: 'secret', body: { sourceRail: 'ONCHAIN', sourceAsset: 'USDT', destinationRail: 'MPESA', destinationAsset: 'KSH', amount: 25, destination: { phone: '254712345678', chain: 'POLYGON' }, idempotencyKey: 'TEST-ROUTE-002' }, desc: 'Quote a crypto intake route that settles to M-Pesa after conversion.' },
  { id: 'execute', label: 'Execute Wallet to Airtel', method: 'POST', path: '/api/routes/execute', auth: 'secret', body: { sourceRail: 'NYLEPAY_WALLET', destinationRail: 'AIRTEL_MONEY', sourceAsset: 'KSH', amount: 750, destination: { phone: '254733123456' }, idempotencyKey: 'TEST-ROUTE-AIR-EXEC-001' }, desc: 'Execute a wallet-funded payout to Airtel Money in sandbox mode.' },
  { id: 'status', label: 'Route Status', method: 'GET', path: '/api/routes/rt_test_123', auth: 'secret', body: null, desc: 'Inspect source, destination, fees, provider references, and route legs.' },
];

export default function SandboxTester() {
  const [base, setBase] = useState(import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080');
  const [jwt, setJwt] = useState('');
  const [secretKey, setSecretKey] = useState('');
  const [active, setActive] = useState(ENDPOINTS[0]);
  const [body, setBody] = useState('');
  const [loading, setLoading] = useState(false);
  const [result, setResult] = useState(null);
  const [err, setErr] = useState('');

  const pick = (endpoint) => {
    setActive(endpoint);
    setResult(null);
    setErr('');
    setBody(endpoint.body ? JSON.stringify(endpoint.body, null, 2) : '');
  };

  const run = async () => {
    setLoading(true);
    setResult(null);
    setErr('');
    const headers = { 'Content-Type': 'application/json' };
    if (active.auth === 'jwt' && jwt) headers.Authorization = `Bearer ${jwt}`;
    if (active.auth === 'secret' && secretKey) headers.Authorization = `Bearer ${secretKey}`;
    try {
      const res = await fetch(`${base}${active.path}`, { method: active.method, headers, body: active.method !== 'GET' && body ? body : undefined });
      const data = await res.json();
      if (active.id === 'login' && data?.data?.token) setJwt(data.data.token);
      if (active.id === 'reg' && data?.data?.secretKey) setSecretKey(data.data.secretKey);
      setResult({ status: res.status, ok: res.ok, data });
    } catch {
      setErr(`Cannot reach ${base}. Ensure your NylePay API server is running and accepting connections.`);
    } finally {
      setLoading(false);
    }
  };

  const methodColor = (method) => method === 'GET' ? { bg: '#d1fae5', color: '#065f46' } : { bg: '#dbeafe', color: '#1e40af' };

  return (
    <div>
      <div style={S.urlBar}>
        <label style={S.urlLabel}>API Base URL</label>
        <div style={S.urlControls}>
          <input style={S.urlInput} value={base} onChange={(event) => setBase(event.target.value.replace(/\/$/, ''))} placeholder="https://api.yourdomain.com" />
          {['http://localhost:8080', 'https://api.nylepay.com'].map((url) => (
            <button key={url} onClick={() => setBase(url)} style={{ ...S.urlChip, ...(base === url ? S.urlChipActive : {}) }}>{url.replace(/https?:\/\//, '')}</button>
          ))}
        </div>
      </div>

      <div style={S.layout}>
        <div style={S.sidebar}>
          <div style={S.sidebarHead}>Sandbox routes</div>
          {ENDPOINTS.map((endpoint) => {
            const colors = methodColor(endpoint.method);
            return (
              <div key={endpoint.id} onClick={() => pick(endpoint)} style={{ ...S.epItem, ...(active.id === endpoint.id ? S.epItemActive : {}) }}>
                <span style={{ ...S.methodPill, background: colors.bg, color: colors.color }}>{endpoint.method}</span>
                <span style={S.epLabel}>{endpoint.label}</span>
              </div>
            );
          })}

          <div style={S.tokenBox}>
            <div style={S.tokenLabel}>JWT Token</div>
            <textarea rows={3} style={S.tokenInput} value={jwt} onChange={(event) => setJwt(event.target.value)} placeholder="Auto-fills on Authenticate" />
            <div style={{ ...S.tokenLabel, marginTop: '0.75rem' }}>Secret Key</div>
            <textarea rows={3} style={S.tokenInput} value={secretKey} onChange={(event) => setSecretKey(event.target.value)} placeholder="Paste sandbox secret key or register business" />
          </div>
        </div>

        <div style={S.panel}>
          <div style={S.reqBar}>
            <span style={{ ...S.methodPill, ...methodColor(active.method), fontSize: '0.85rem', padding: '0.3rem 0.75rem' }}>{active.method}</span>
            <code style={S.reqPath}>{base}<strong style={{ color: 'var(--text-primary)' }}>{active.path}</strong></code>
            {active.auth && <span className="badge badge-amber" style={{ fontSize: '0.68rem' }}>{active.auth === 'secret' ? 'Secret Key' : 'JWT Required'}</span>}
            <button className="btn-primary" onClick={run} disabled={loading} style={S.runButton}>
              {loading ? <><span className="spinner" />Running...</> : 'Run Request'}
            </button>
          </div>

          <p style={S.desc}>{active.desc}</p>

          {active.method !== 'GET' && (
            <div style={{ marginBottom: '1rem' }}>
              <div style={S.panelLabel}>Request Body</div>
              <textarea rows={8} style={S.bodyInput} value={body} onChange={(event) => setBody(event.target.value)} />
            </div>
          )}

          {(result || err) && (
            <div>
              <div style={{ ...S.panelLabel, display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                Response
                {result && <span style={{ padding: '0.1rem 0.5rem', borderRadius: '100px', fontSize: '0.68rem', fontWeight: 700, background: result.ok ? '#d1fae5' : '#fee2e2', color: result.ok ? '#065f46' : '#991b1b' }}>{result.status} {result.ok ? 'OK' : 'ERROR'}</span>}
              </div>
              {err ? (
                <div className="alert alert-error">{err}</div>
              ) : (
                <div className="code-block" style={{ maxHeight: 320, overflow: 'auto' }}>
                  <pre><code>{JSON.stringify(result.data, null, 2)}</code></pre>
                </div>
              )}
            </div>
          )}
        </div>
      </div>
    </div>
  );
}

const S = {
  urlBar: { background: 'var(--bg-surface)', border: '1px solid var(--border)', borderRadius: 'var(--radius-md)', padding: '1rem 1.25rem', marginBottom: '1.25rem' },
  urlLabel: { display: 'block', fontSize: '0.72rem', fontWeight: 700, textTransform: 'uppercase', letterSpacing: '0.07em', color: 'var(--text-tertiary)', marginBottom: '0.5rem' },
  urlControls: { display: 'flex', gap: '0.5rem', alignItems: 'center', flexWrap: 'wrap' },
  urlInput: { flex: 1, minWidth: 200, padding: '0.5rem 0.75rem', border: '1.5px solid var(--border)', borderRadius: 'var(--radius-sm)', fontFamily: 'var(--font-mono)', fontSize: '0.84rem', color: 'var(--text-primary)', background: 'var(--bg-page)', outline: 'none' },
  urlChip: { padding: '0.375rem 0.75rem', border: '1px solid var(--border)', borderRadius: 'var(--radius-sm)', background: 'var(--bg-page)', color: 'var(--text-secondary)', fontSize: '0.75rem', fontFamily: 'var(--font-mono)', cursor: 'pointer', transition: 'all 0.15s', whiteSpace: 'nowrap' },
  urlChipActive: { background: 'var(--blue-50)', borderColor: 'var(--blue-100)', color: 'var(--blue-700)' },
  layout: { display: 'grid', gridTemplateColumns: '260px 1fr', gap: '1rem' },
  sidebar: { background: 'var(--bg-surface)', border: '1px solid var(--border)', borderRadius: 'var(--radius-md)', overflow: 'hidden', display: 'flex', flexDirection: 'column' },
  sidebarHead: { fontSize: '0.65rem', fontWeight: 700, letterSpacing: '0.1em', textTransform: 'uppercase', color: 'var(--text-tertiary)', padding: '0.75rem 1rem', borderBottom: '1px solid var(--border)', background: 'var(--gray-50)' },
  epItem: { display: 'flex', alignItems: 'center', gap: '0.4rem', padding: '0.6rem 0.875rem', cursor: 'pointer', borderBottom: '1px solid var(--border)', transition: 'background 0.1s', borderLeft: '3px solid transparent' },
  epItemActive: { background: 'var(--blue-50)', borderLeftColor: 'var(--brand)' },
  methodPill: { fontSize: '0.62rem', fontWeight: 700, fontFamily: 'var(--font-mono)', padding: '0.15rem 0.4rem', borderRadius: '3px', flexShrink: 0 },
  epLabel: { flex: 1, fontSize: '0.8rem', color: 'var(--text-secondary)', fontWeight: 500 },
  tokenBox: { padding: '0.875rem', borderTop: '1px solid var(--border)', marginTop: 'auto' },
  tokenLabel: { fontSize: '0.68rem', fontWeight: 700, color: 'var(--text-tertiary)', textTransform: 'uppercase', letterSpacing: '0.07em', marginBottom: '0.375rem' },
  tokenInput: { width: '100%', padding: '0.5rem 0.625rem', border: '1.5px solid var(--border)', borderRadius: 'var(--radius-sm)', fontFamily: 'var(--font-mono)', fontSize: '0.7rem', color: 'var(--text-secondary)', background: 'var(--bg-page)', resize: 'vertical', outline: 'none', lineHeight: 1.5 },
  panel: { background: 'var(--bg-surface)', border: '1px solid var(--border)', borderRadius: 'var(--radius-md)', padding: '1.25rem' },
  reqBar: { display: 'flex', alignItems: 'center', gap: '0.625rem', marginBottom: '0.875rem', flexWrap: 'wrap' },
  reqPath: { fontFamily: 'var(--font-mono)', fontSize: '0.8rem', color: 'var(--text-secondary)', flex: 1, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' },
  runButton: { marginLeft: 'auto', padding: '0.5rem 1.25rem', fontSize: '0.875rem' },
  desc: { fontSize: '0.84rem', color: 'var(--text-secondary)', marginBottom: '1rem', lineHeight: 1.6 },
  panelLabel: { fontSize: '0.68rem', fontWeight: 700, textTransform: 'uppercase', letterSpacing: '0.08em', color: 'var(--text-tertiary)', marginBottom: '0.375rem' },
  bodyInput: { width: '100%', padding: '0.625rem 0.75rem', border: '1.5px solid var(--border)', borderRadius: 'var(--radius-sm)', fontFamily: 'var(--font-mono)', fontSize: '0.8rem', color: 'var(--text-primary)', background: 'var(--bg-page)', resize: 'vertical', outline: 'none', lineHeight: 1.7 },
};
