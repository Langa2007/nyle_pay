import React, { useState } from 'react';

const ENDPOINTS = [
  { id:'health', label:'Health Check', method:'GET', path:'/api/sandbox/health', auth:false, body:null, desc:'Verify the API is reachable and sandbox mode is active.' },
  { id:'login', label:'Authenticate', method:'POST', path:'/api/auth/login', auth:false, body:{ email:'merchant@example.com', password:'Password123' }, desc:'Obtain a JWT token. The token auto-fills below for subsequent authenticated requests.' },
  { id:'reg', label:'Register Merchant', method:'POST', path:'/api/merchant/register', auth:true, body:{ businessName:'Test Business', businessEmail:'biz@test.com', webhookUrl:'https://example.com/webhook' }, desc:'Register your business profile and receive API credentials.' },
  { id:'link', label:'Create Payment Link', method:'POST', path:'/api/merchant/payment-link', auth:true, body:{ amount:1500, currency:'KES', description:'Order #TEST-001', expiryMinutes:60 }, desc:'Generate a hosted checkout URL to share with your customer.' },
  { id:'profile', label:'Merchant Profile', method:'GET', path:'/api/merchant/profile', auth:true, body:null, desc:'Retrieve your merchant profile, key status, and settlement configuration.' },
  { id:'charges', label:'STK Push (M-Pesa)', method:'POST', path:'/api/v1/merchant/charges', auth:'secret', body:{ method:'MPESA', phone:'254712345678', amount:500, reference:'TEST-001' }, desc:'Initiate an M-Pesa STK Push to a customer phone number. Use your Secret Key as the Bearer token.' },
  { id:'balance', label:'Settlement Balance', method:'GET', path:'/api/v1/merchant/balance', auth:'secret', body:null, desc:'Query your available settlement balance. Use your Secret Key as the Bearer token.' },
];

export default function SandboxTester() {
  const [base, setBase]       = useState('http://localhost:8080');
  const [token, setToken]     = useState('');
  const [active, setActive]   = useState(ENDPOINTS[0]);
  const [body, setBody]       = useState('');
  const [loading, setLoading] = useState(false);
  const [result, setResult]   = useState(null);
  const [err, setErr]         = useState('');

  const pick = (ep) => { setActive(ep); setResult(null); setErr(''); setBody(ep.body ? JSON.stringify(ep.body, null, 2) : ''); };

  const run = async () => {
    setLoading(true); setResult(null); setErr('');
    const headers = { 'Content-Type': 'application/json' };
    if (active.auth && token) headers['Authorization'] = `Bearer ${token}`;
    try {
      const res  = await fetch(`${base}${active.path}`, { method: active.method, headers, body: active.method !== 'GET' && body ? body : undefined });
      const data = await res.json();
      if (active.id === 'login' && data?.data?.token) setToken(data.data.token);
      setResult({ status: res.status, ok: res.ok, data });
    } catch {
      setErr(`Cannot reach ${base}. Ensure your NylePay API server is running and accepting connections.`);
    } finally { setLoading(false); }
  };

  const methodColor = (m) => m === 'GET' ? { bg:'#d1fae5', color:'#065f46' } : { bg:'#dbeafe', color:'#1e40af' };

  return (
    <div>
      {/* Base URL bar */}
      <div style={S.urlBar}>
        <label style={S.urlLabel}>API Base URL</label>
        <div style={{ display:'flex', gap:'0.5rem', alignItems:'center', flexWrap:'wrap' }}>
          <input style={S.urlInput} value={base} onChange={e => setBase(e.target.value.replace(/\/$/,''))} placeholder="https://api.yourdomain.com" />
          {['http://localhost:8080','https://api.nylepay.com'].map(u => (
            <button key={u} onClick={() => setBase(u)} style={{ ...S.urlChip, ...(base===u ? S.urlChipActive : {}) }}>{u.replace(/https?:\/\//,'')}</button>
          ))}
        </div>
      </div>

      <div style={S.layout}>
        {/* Sidebar */}
        <div style={S.sidebar}>
          <div style={S.sidebarHead}>Endpoints</div>
          {ENDPOINTS.map(ep => {
            const mc = methodColor(ep.method);
            return (
              <div key={ep.id} onClick={() => pick(ep)} style={{ ...S.epItem, ...(active.id===ep.id ? S.epItemActive : {}) }}>
                <span style={{ ...S.methodPill, background:mc.bg, color:mc.color }}>{ep.method}</span>
                <span style={S.epLabel}>{ep.label}</span>
                {ep.auth && <svg width="11" height="11" viewBox="0 0 24 24" fill="none" stroke="var(--text-tertiary)" strokeWidth="2"><rect x="3" y="11" width="18" height="11" rx="2"/><path d="M7 11V7a5 5 0 0110 0v4"/></svg>}
              </div>
            );
          })}

          <div style={S.tokenBox}>
            <div style={S.tokenLabel}>JWT Token <span style={{fontWeight:400,color:'var(--text-tertiary)'}}>— auto-fills on Authenticate</span></div>
            <textarea rows={3} style={S.tokenInput} value={token} onChange={e => setToken(e.target.value)} placeholder="Paste token or use Authenticate endpoint above…" />
          </div>
        </div>

        {/* Request panel */}
        <div style={S.panel}>
          <div style={S.reqBar}>
            <span style={{ ...S.methodPill, ...methodColor(active.method), fontSize:'0.85rem', padding:'0.3rem 0.75rem' }}>{active.method}</span>
            <code style={S.reqPath}>{base}<strong style={{color:'var(--text-primary)'}}>{active.path}</strong></code>
            {active.auth && <span className="badge badge-amber" style={{fontSize:'0.68rem'}}>{active.auth==='secret' ? 'Secret Key' : 'JWT Required'}</span>}
            <button className="btn-primary" onClick={run} disabled={loading} style={{marginLeft:'auto',padding:'0.5rem 1.25rem',fontSize:'0.875rem'}}>
              {loading ? <><span className="spinner"/>Running…</> : '▶ Run Request'}
            </button>
          </div>

          <p style={{fontSize:'0.84rem',color:'var(--text-secondary)',marginBottom:'1rem',lineHeight:1.6}}>{active.desc}</p>

          {active.method !== 'GET' && (
            <div style={{marginBottom:'1rem'}}>
              <div style={S.panelLabel}>Request Body</div>
              <textarea rows={6} style={S.bodyInput} value={body} onChange={e => setBody(e.target.value)} />
            </div>
          )}

          {(result || err) && (
            <div>
              <div style={{...S.panelLabel, display:'flex', alignItems:'center', gap:'0.5rem'}}>
                Response
                {result && <span style={{padding:'0.1rem 0.5rem', borderRadius:'100px', fontSize:'0.68rem', fontWeight:700, background:result.ok?'#d1fae5':'#fee2e2', color:result.ok?'#065f46':'#991b1b'}}>{result.status} {result.ok?'OK':'ERROR'}</span>}
              </div>
              {err ? (
                <div className="alert alert-error"><svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" style={{flexShrink:0}}><circle cx="12" cy="12" r="10"/><path d="M12 8v4M12 16h.01"/></svg>{err}</div>
              ) : (
                <div className="code-block" style={{maxHeight:320,overflow:'auto'}}>
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
  urlBar: { background:'var(--bg-surface)', border:'1px solid var(--border)', borderRadius:'var(--radius-md)', padding:'1rem 1.25rem', marginBottom:'1.25rem' },
  urlLabel: { display:'block', fontSize:'0.72rem', fontWeight:700, textTransform:'uppercase', letterSpacing:'0.07em', color:'var(--text-tertiary)', marginBottom:'0.5rem' },
  urlInput: { flex:1, minWidth:200, padding:'0.5rem 0.75rem', border:'1.5px solid var(--border)', borderRadius:'var(--radius-sm)', fontFamily:'var(--font-mono)', fontSize:'0.84rem', color:'var(--text-primary)', background:'var(--bg-page)', outline:'none' },
  urlChip: { padding:'0.375rem 0.75rem', border:'1px solid var(--border)', borderRadius:'var(--radius-sm)', background:'var(--bg-page)', color:'var(--text-secondary)', fontSize:'0.75rem', fontFamily:'var(--font-mono)', cursor:'pointer', transition:'all 0.15s', whiteSpace:'nowrap' },
  urlChipActive: { background:'var(--blue-50)', borderColor:'var(--blue-100)', color:'var(--blue-700)' },
  layout: { display:'grid', gridTemplateColumns:'240px 1fr', gap:'1rem' },
  sidebar: { background:'var(--bg-surface)', border:'1px solid var(--border)', borderRadius:'var(--radius-md)', overflow:'hidden', display:'flex', flexDirection:'column' },
  sidebarHead: { fontSize:'0.65rem', fontWeight:700, letterSpacing:'0.1em', textTransform:'uppercase', color:'var(--text-tertiary)', padding:'0.75rem 1rem', borderBottom:'1px solid var(--border)', background:'var(--gray-50)' },
  epItem: { display:'flex', alignItems:'center', gap:'0.4rem', padding:'0.6rem 0.875rem', cursor:'pointer', borderBottom:'1px solid var(--border)', transition:'background 0.1s', borderLeft:'3px solid transparent' },
  epItemActive: { background:'var(--blue-50)', borderLeftColor:'var(--brand)' },
  methodPill: { fontSize:'0.62rem', fontWeight:700, fontFamily:'var(--font-mono)', padding:'0.15rem 0.4rem', borderRadius:'3px', flexShrink:0 },
  epLabel: { flex:1, fontSize:'0.8rem', color:'var(--text-secondary)', fontWeight:500 },
  tokenBox: { padding:'0.875rem', borderTop:'1px solid var(--border)', marginTop:'auto' },
  tokenLabel: { fontSize:'0.68rem', fontWeight:700, color:'var(--text-tertiary)', textTransform:'uppercase', letterSpacing:'0.07em', marginBottom:'0.375rem' },
  tokenInput: { width:'100%', padding:'0.5rem 0.625rem', border:'1.5px solid var(--border)', borderRadius:'var(--radius-sm)', fontFamily:'var(--font-mono)', fontSize:'0.7rem', color:'var(--text-secondary)', background:'var(--bg-page)', resize:'vertical', outline:'none', lineHeight:1.5 },
  panel: { background:'var(--bg-surface)', border:'1px solid var(--border)', borderRadius:'var(--radius-md)', padding:'1.25rem' },
  reqBar: { display:'flex', alignItems:'center', gap:'0.625rem', marginBottom:'0.875rem', flexWrap:'wrap' },
  reqPath: { fontFamily:'var(--font-mono)', fontSize:'0.8rem', color:'var(--text-secondary)', flex:1, overflow:'hidden', textOverflow:'ellipsis', whiteSpace:'nowrap' },
  panelLabel: { fontSize:'0.68rem', fontWeight:700, textTransform:'uppercase', letterSpacing:'0.08em', color:'var(--text-tertiary)', marginBottom:'0.375rem' },
  bodyInput: { width:'100%', padding:'0.625rem 0.75rem', border:'1.5px solid var(--border)', borderRadius:'var(--radius-sm)', fontFamily:'var(--font-mono)', fontSize:'0.8rem', color:'var(--text-primary)', background:'var(--bg-page)', resize:'vertical', outline:'none', lineHeight:1.7 },
};
