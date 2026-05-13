import React, { useState } from 'react';
import SandboxTester from '../components/SandboxTester';

const ENDPOINTS = [
  { method:'POST', path:'/api/auth/register', auth:false, tag:'Auth', title:'Create Account', desc:'Register a new NylePay account. Required before merchant onboarding.', body:{ fullName:'string', email:'string', password:'string (min 8)', mpesaNumber:'string — 2547XXXXXXXX', countryCode:'string — e.g. "KE"' }, response:{ success:true, data:{ token:'JWT', userId:'number', email:'string', fullName:'string', accountNumber:'NPY-XXXXXXXX' } } },
  { method:'POST', path:'/api/auth/login', auth:false, tag:'Auth', title:'Authenticate', desc:'Authenticate with email and password. Returns a JWT access token valid for 24 hours. Include this token as a Bearer token on all protected requests.', body:{ email:'string', password:'string' }, response:{ success:true, data:{ token:'JWT', userId:'number', email:'string' } } },
  { method:'POST', path:'/api/merchant/register', auth:'jwt', tag:'Merchant', title:'Register Merchant', desc:'Onboard your business and receive API credentials. The secretKey is returned once — store it immediately in your server environment variables.', body:{ businessName:'string', businessEmail:'string', settlementPhone:'string — 2547XXXXXXXX', webhookUrl:'string (optional, HTTPS)' }, response:{ success:true, data:{ merchantId:'number', publicKey:'npy_pub_live_...', secretKey:'npy_sec_live_...', webhookSecret:'whsec_...', status:'PENDING' } } },
  { method:'GET',  path:'/api/merchant/profile', auth:'jwt', tag:'Merchant', title:'Merchant Profile', desc:'Retrieve your merchant profile including business name, key status, settlement configuration, and account standing.', body:null, response:{ success:true, data:{ merchantId:'number', businessName:'string', status:'ACTIVE | PENDING | SUSPENDED', balance:'number (KES)', publicKey:'npy_pub_live_...' } } },
  { method:'POST', path:'/api/merchant/payment-link', auth:'jwt', tag:'Payments', title:'Create Payment Link', desc:'Generate a hosted, time-limited checkout URL. Redirect your customer to checkoutUrl to complete payment via M-Pesa, Card, or Crypto.', body:{ amount:'number (KES)', currency:'string — "KES"', description:'string', redirectUrl:'string (optional)', expiryMinutes:'number — default 60' }, response:{ success:true, data:{ reference:'string', checkoutUrl:'https://pay.nylepay.com/c/...', amount:'number', expiresAt:'ISO 8601 timestamp' } } },
  { method:'POST', path:'/api/v1/merchant/charges', auth:'secret', tag:'Payments', title:'Initiate STK Push', desc:'Directly trigger an M-Pesa STK Push prompt on a customer\'s handset. Authenticate with your Secret Key (not JWT). The customer enters their M-Pesa PIN to confirm payment.', body:{ method:'"MPESA"', phone:'string — 2547XXXXXXXX', amount:'number (KES)', reference:'string — your internal order ID' }, response:{ success:true, data:{ chargeId:'string', status:'PENDING', method:'MPESA', amount:'number', phone:'string' } } },
  { method:'GET',  path:'/api/v1/merchant/balance', auth:'secret', tag:'Payments', title:'Settlement Balance', desc:'Query your real-time settlement balance. Authenticate with your Secret Key. Balance reflects completed transactions pending withdrawal.', body:null, response:{ success:true, data:{ available:'number (KES)', pending:'number (KES)', currency:'KES', lastUpdated:'ISO 8601' } } },
  { method:'POST', path:'/api/v1/merchant/transfers', auth:'secret', tag:'Payments', title:'Initiate Payout', desc:'Transfer your available balance to an external M-Pesa or bank account. Authenticate with your Secret Key.', body:{ amount:'number (KES)', method:'"MPESA" | "BANK"', phone:'string (M-Pesa) — optional', accountNumber:'string (bank) — optional', reference:'string' }, response:{ success:true, data:{ transferId:'string', status:'QUEUED', amount:'number', estimatedArrival:'ISO 8601' } } },
];

const WEBHOOK_EVENTS = [
  { event:'payment.completed',  trigger:'Customer payment confirmed and settled to balance.' },
  { event:'payment.failed',     trigger:'Payment attempt failed — insufficient funds, timeout, or cancellation.' },
  { event:'payment.pending',    trigger:'Payment initiated, awaiting customer action (e.g. M-Pesa PIN entry).' },
  { event:'payment.refunded',   trigger:'Payment successfully reversed and refunded to the customer.' },
  { event:'payout.completed',   trigger:'Settlement payout delivered to your M-Pesa or bank account.' },
  { event:'payout.failed',      trigger:'Settlement payout could not be delivered. Funds returned to balance.' },
  { event:'key.regenerated',    trigger:'API keys were rotated. Previous keys are now invalid.' },
];

const WEBHOOK_CODE = {
  'Node.js': `// Express — verify webhook signature
const crypto = require('crypto');

app.post('/nylepay-webhook', express.raw({ type: '*/*' }), (req, res) => {
  const sig      = req.headers['x-nylepay-signature'];
  const expected = crypto
    .createHmac('sha256', process.env.NYLEPAY_WEBHOOK_SECRET)
    .update(req.body)           // raw Buffer — do NOT parse JSON first
    .digest('hex');

  if (!crypto.timingSafeEqual(Buffer.from(expected), Buffer.from(sig))) {
    return res.status(401).json({ error: 'Invalid signature' });
  }

  const event = JSON.parse(req.body);
  switch (event.type) {
    case 'payment.completed':
      await fulfillOrder(event.data.reference);
      break;
    case 'payment.failed':
      await cancelOrder(event.data.reference);
      break;
  }
  res.status(200).json({ received: true });
});`,
  'Python': `# Flask — verify webhook signature
import hmac, hashlib, os, json
from flask import request, abort, jsonify

@app.route('/nylepay-webhook', methods=['POST'])
def webhook():
    sig    = request.headers.get('X-NylePay-Signature', '')
    secret = os.environ['NYLEPAY_WEBHOOK_SECRET'].encode()
    digest = hmac.new(secret, request.data, hashlib.sha256).hexdigest()

    if not hmac.compare_digest(digest, sig):
        abort(401)

    event = request.get_json(force=True)
    if event['type'] == 'payment.completed':
        fulfill_order(event['data']['reference'])

    return jsonify(received=True), 200`,
};

const AUTH_CODE = {
  'Node.js': `// All JWT-protected endpoints
const res = await fetch('https://api.nylepay.com/api/merchant/profile', {
  headers: { 'Authorization': \`Bearer \${jwtToken}\` },
});

// Secret Key endpoints (charges, balance, transfers)
const charge = await fetch('https://api.nylepay.com/api/v1/merchant/charges', {
  method: 'POST',
  headers: {
    'Authorization': \`Bearer \${process.env.NYLEPAY_SECRET_KEY}\`,
    'Content-Type':  'application/json',
  },
  body: JSON.stringify({ method:'MPESA', phone:'254712345678', amount:1500, reference:'ORD-001' }),
});`,
  'Python': `# JWT-protected endpoints
import requests, os

headers = {'Authorization': f'Bearer {jwt_token}'}
profile = requests.get('https://api.nylepay.com/api/merchant/profile', headers=headers)

# Secret Key endpoints
secret_headers = {
    'Authorization': f'Bearer {os.environ["NYLEPAY_SECRET_KEY"]}',
    'Content-Type':  'application/json',
}
charge = requests.post(
    'https://api.nylepay.com/api/v1/merchant/charges',
    headers=secret_headers,
    json={'method':'MPESA','phone':'254712345678','amount':1500,'reference':'ORD-001'}
)`,
};

const tags = [...new Set(ENDPOINTS.map(e => e.tag))];

export default function ApiDocs() {
  const [tab, setTab]         = useState('reference');
  const [active, setActive]   = useState(ENDPOINTS[0]);
  const [langW, setLangW]     = useState('Node.js');
  const [langA, setLangA]     = useState('Node.js');
  const [copied, setCopied]   = useState('');
  const [filterTag, setFilterTag] = useState('All');

  const copy = (text, id) => {
    navigator.clipboard.writeText(text);
    setCopied(id);
    setTimeout(() => setCopied(''), 2000);
  };

  const mc = (m) => m === 'GET' ? 'badge-method-get' : 'badge-method-post';
  const authLabel = (a) => a === 'secret' ? '🔑 Secret Key' : a === 'jwt' ? '🔐 JWT Bearer' : '🔓 Public';
  const authClass = (a) => a === 'secret' ? 'badge-amber' : a === 'jwt' ? 'badge-blue' : 'badge-green';

  const filtered = filterTag === 'All' ? ENDPOINTS : ENDPOINTS.filter(e => e.tag === filterTag);

  return (
    <div>
      {/* Tab bar */}
      <div className="tab-bar" style={{ marginBottom: '1.5rem' }}>
        {[['reference','Endpoint Reference'],['webhooks','Webhook Events'],['sandbox','Sandbox Tester']].map(([id, label]) => (
          <button key={id} className={`tab-item ${tab===id?'active':''}`} onClick={() => setTab(id)}>{label}</button>
        ))}
      </div>

      {/* ─── ENDPOINT REFERENCE ─── */}
      {tab === 'reference' && (
        <div style={{ display:'grid', gridTemplateColumns:'260px 1fr', gap:'1.5rem', alignItems:'start' }}>
          {/* Sidebar */}
          <div style={{ position:'sticky', top:'1rem', background:'var(--bg-surface)', border:'1px solid var(--border)', borderRadius:'var(--radius-md)', overflow:'hidden' }}>
            {/* Tag filter */}
            <div style={{ padding:'0.625rem', borderBottom:'1px solid var(--border)', display:'flex', gap:'0.25rem', flexWrap:'wrap' }}>
              {['All', ...tags].map(t => (
                <button key={t} onClick={() => setFilterTag(t)}
                  style={{ padding:'0.2rem 0.6rem', borderRadius:'100px', border:'none', fontSize:'0.72rem', fontWeight:600, cursor:'pointer', background: filterTag===t ? 'var(--brand)' : 'var(--gray-100)', color: filterTag===t ? '#fff' : 'var(--text-secondary)', fontFamily:'inherit' }}>
                  {t}
                </button>
              ))}
            </div>
            {filtered.map(ep => (
              <div key={ep.path+ep.method} onClick={() => setActive(ep)}
                style={{ display:'flex', alignItems:'center', gap:'0.5rem', padding:'0.6rem 0.875rem', cursor:'pointer', borderBottom:'1px solid var(--border)', background: active===ep ? 'var(--blue-50)' : 'transparent', borderLeft: active===ep ? '3px solid var(--brand)' : '3px solid transparent', transition:'all 0.1s' }}>
                <span className={`badge ${mc(ep.method)}`} style={{ fontSize:'0.58rem' }}>{ep.method}</span>
                <span style={{ fontSize:'0.8rem', color:'var(--text-secondary)', fontWeight:500 }}>{ep.title}</span>
              </div>
            ))}
          </div>

          {/* Detail */}
          <div>
            <div style={{ display:'flex', alignItems:'center', gap:'0.75rem', marginBottom:'1rem', flexWrap:'wrap' }}>
              <span className={`badge ${mc(active.method)}`}>{active.method}</span>
              <code style={{ fontFamily:'var(--font-mono)', fontSize:'0.9rem', color:'var(--text-primary)', fontWeight:600 }}>{active.path}</code>
              <span className={`badge ${authClass(active.auth)}`} style={{ marginLeft:'auto' }}>{authLabel(active.auth)}</span>
            </div>
            <h2 style={{ fontSize:'1.375rem', fontWeight:800, letterSpacing:'-0.02em', marginBottom:'0.375rem' }}>{active.title}</h2>
            <p style={{ color:'var(--text-secondary)', lineHeight:1.7, marginBottom:'1.5rem', fontSize:'0.9375rem' }}>{active.desc}</p>

            {active.body && (
              <div className="card" style={{ marginBottom:'1.25rem' }}>
                <h4 style={{ fontSize:'0.78rem', fontWeight:700, textTransform:'uppercase', letterSpacing:'0.07em', color:'var(--text-tertiary)', marginBottom:'0.875rem' }}>Request Body</h4>
                <table className="data-table">
                  <thead><tr><th>Field</th><th>Type / Description</th></tr></thead>
                  <tbody>
                    {Object.entries(active.body).map(([k,v]) => (
                      <tr key={k}>
                        <td><code style={{ fontFamily:'var(--font-mono)', fontSize:'0.82rem', color:'var(--blue-700)', background:'var(--blue-50)', padding:'0.1rem 0.35rem', borderRadius:'3px' }}>{k}</code></td>
                        <td style={{ color:'var(--text-secondary)', fontSize:'0.84rem' }}>{v}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}

            <div className="card" style={{ marginBottom:'1.25rem' }}>
              <h4 style={{ fontSize:'0.78rem', fontWeight:700, textTransform:'uppercase', letterSpacing:'0.07em', color:'var(--text-tertiary)', marginBottom:'0.875rem' }}>Response Schema</h4>
              <div className="code-block" style={{ position:'relative' }}>
                <button className="copy-btn" onClick={() => copy(JSON.stringify(active.response,null,2),'resp')}>{copied==='resp'?'✓ Copied':'Copy'}</button>
                <pre><code>{JSON.stringify(active.response, null, 2)}</code></pre>
              </div>
            </div>

            {/* Auth note on first two */}
            {(active.tag === 'Auth' || active === ENDPOINTS[0]) && (
              <div className="card">
                <h4 style={{ fontSize:'0.78rem', fontWeight:700, textTransform:'uppercase', letterSpacing:'0.07em', color:'var(--text-tertiary)', marginBottom:'0.875rem' }}>
                  Authentication — Code Example
                </h4>
                <div style={{ display:'flex', gap:'0.25rem', marginBottom:'0.5rem' }}>
                  {Object.keys(AUTH_CODE).map(l => (
                    <button key={l} onClick={() => setLangA(l)} style={{ padding:'0.2rem 0.65rem', border:'none', borderRadius:'4px', fontSize:'0.75rem', fontWeight:600, cursor:'pointer', fontFamily:'inherit', background: langA===l ? 'var(--brand)' : 'var(--gray-100)', color: langA===l ? '#fff' : 'var(--text-secondary)' }}>{l}</button>
                  ))}
                </div>
                <div className="code-block" style={{ position:'relative' }}>
                  <button className="copy-btn" onClick={() => copy(AUTH_CODE[langA],'auth')}>{copied==='auth'?'✓ Copied':'Copy'}</button>
                  <pre><code>{AUTH_CODE[langA]}</code></pre>
                </div>
              </div>
            )}
          </div>
        </div>
      )}

      {/* ─── WEBHOOK EVENTS ─── */}
      {tab === 'webhooks' && (
        <div>
          <div className="alert alert-info" style={{ marginBottom:'1.5rem' }}>
            <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" style={{ flexShrink:0, marginTop:'1px' }}><circle cx="12" cy="12" r="10"/><path d="M12 16v-4M12 8h.01"/></svg>
            <div>Every POST to your webhook URL includes an <code style={{ fontFamily:'var(--font-mono)', fontSize:'0.82rem' }}>X-NylePay-Signature</code> header containing <code style={{ fontFamily:'var(--font-mono)', fontSize:'0.82rem' }}>HMAC-SHA256(webhookSecret, rawBody)</code>. Always verify this header before fulfilling orders.</div>
          </div>

          <div className="card" style={{ marginBottom:'1.5rem' }}>
            <h3 style={{ fontSize:'0.9375rem', fontWeight:700, marginBottom:'1rem' }}>Event Types</h3>
            <table className="data-table">
              <thead><tr><th>Event</th><th>Description</th></tr></thead>
              <tbody>
                {WEBHOOK_EVENTS.map(e => (
                  <tr key={e.event}>
                    <td><code style={{ fontFamily:'var(--font-mono)', fontSize:'0.82rem', color:'var(--blue-700)', background:'var(--blue-50)', padding:'0.1rem 0.4rem', borderRadius:'3px', whiteSpace:'nowrap' }}>{e.event}</code></td>
                    <td style={{ color:'var(--text-secondary)', fontSize:'0.875rem' }}>{e.trigger}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>

          <div className="card">
            <div style={{ display:'flex', justifyContent:'space-between', alignItems:'center', marginBottom:'0.875rem' }}>
              <h3 style={{ fontSize:'0.9375rem', fontWeight:700 }}>Signature Verification</h3>
              <div style={{ display:'flex', gap:'0.25rem' }}>
                {Object.keys(WEBHOOK_CODE).map(l => (
                  <button key={l} onClick={() => setLangW(l)} style={{ padding:'0.2rem 0.65rem', border:'none', borderRadius:'4px', fontSize:'0.75rem', fontWeight:600, cursor:'pointer', fontFamily:'inherit', background: langW===l ? 'var(--brand)' : 'var(--gray-100)', color: langW===l ? '#fff' : 'var(--text-secondary)' }}>{l}</button>
                ))}
              </div>
            </div>
            <div className="code-block" style={{ position:'relative' }}>
              <button className="copy-btn" onClick={() => copy(WEBHOOK_CODE[langW],'hook')}>{copied==='hook'?'✓ Copied':'Copy'}</button>
              <pre><code>{WEBHOOK_CODE[langW]}</code></pre>
            </div>
          </div>
        </div>
      )}

      {/* ─── SANDBOX TESTER ─── */}
      {tab === 'sandbox' && <SandboxTester />}
    </div>
  );
}
