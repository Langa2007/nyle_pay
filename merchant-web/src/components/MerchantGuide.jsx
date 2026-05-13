import React, { useState } from 'react';

const STEPS = [
  {
    num: 1,
    title: 'Create a NylePay Account',
    desc: 'Sign up with your email, password, and M-Pesa number. This creates your NylePay identity — used for both personal access and merchant management.',
    detail: 'POST /api/auth/register\nRequired: email, password (min 8), fullName, mpesaNumber, countryCode',
  },
  {
    num: 2,
    title: 'Register Your Business',
    desc: 'After signing in, submit your business name, business email, and M-Pesa settlement number. Optionally provide a webhook URL for real-time payment events.',
    detail: 'POST /api/merchant/register\nRequired: businessName, businessEmail, settlementPhone\nOptional: webhookUrl (HTTPS)',
  },
  {
    num: 3,
    title: 'Secure Your API Keys',
    desc: 'You receive a Public Key, Secret Key, and Webhook Signing Secret. The Secret Key and Webhook Secret are shown exactly once — store them immediately in your server environment variables.',
    detail: 'PUBLIC KEY    — safe in frontend / mobile client\nSECRET KEY    — server .env ONLY, never in source control\nWEBHOOK SECRET — verify X-NylePay-Signature on all events',
  },
  {
    num: 4,
    title: 'Integrate the REST API',
    desc: 'Use your Secret Key from your backend to create payment links, initiate M-Pesa STK pushes, query balances, and trigger payouts. Any language, any HTTP client.',
    detail: 'Base URL: https://api.nylepay.com\nContent-Type: application/json\nAuth: Authorization: Bearer {token}',
  },
  {
    num: 5,
    title: 'Verify Webhook Events',
    desc: 'Every payment event posted to your webhook URL is signed with HMAC-SHA256 using your Webhook Secret. Always verify the signature before fulfilling an order.',
    detail: 'HMAC_SHA256(webhookSecret, rawRequestBody)\n=== X-NylePay-Signature header value\n\nUse timing-safe comparison (timingSafeEqual)',
  },
  {
    num: 6,
    title: 'Test in Sandbox, Then Go Live',
    desc: 'Sandbox keys simulate the full payment flow with no real money. Test all edge cases (success, failure, timeouts) before requesting production activation.',
    detail: 'Sandbox: test key prefix npy_test_, simulated flows\nProduction: real money, KYC required, reviewed within 1–2 business days',
  },
];

const CODE = {
  'Node.js': {
    payLink: `const res = await fetch('https://api.nylepay.com/api/merchant/payment-link', {
  method: 'POST',
  headers: {
    'Authorization': \`Bearer \${process.env.NYLEPAY_SECRET_KEY}\`,
    'Content-Type':  'application/json',
  },
  body: JSON.stringify({
    amount:        1500,
    currency:      'KES',
    description:   'Order #1234',
    redirectUrl:   'https://yourapp.com/thank-you',
    expiryMinutes: 60,
  }),
});
const { data } = await res.json();
// Redirect customer to data.checkoutUrl`,
    webhook: `const crypto = require('crypto');

app.post('/nylepay-webhook', express.raw({ type: '*/*' }), (req, res) => {
  const sig      = req.headers['x-nylepay-signature'];
  const expected = crypto
    .createHmac('sha256', process.env.NYLEPAY_WEBHOOK_SECRET)
    .update(req.body)
    .digest('hex');

  if (!crypto.timingSafeEqual(Buffer.from(expected), Buffer.from(sig)))
    return res.status(401).send('Forbidden');

  const { type, data } = JSON.parse(req.body);
  if (type === 'payment.completed') await fulfillOrder(data.reference);

  res.status(200).send('OK');
});`,
  },
  'Python': {
    payLink: `import requests, os

res = requests.post(
    'https://api.nylepay.com/api/merchant/payment-link',
    headers={
        'Authorization': f'Bearer {os.environ["NYLEPAY_SECRET_KEY"]}',
        'Content-Type':  'application/json',
    },
    json={
        'amount':        1500,
        'currency':      'KES',
        'description':   'Order #1234',
        'redirectUrl':   'https://yourapp.com/thank-you',
        'expiryMinutes': 60,
    }
)
data = res.json()['data']
# Redirect customer to data['checkoutUrl']`,
    webhook: `import hmac, hashlib, os
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
  },
};

export default function MerchantGuide() {
  const [active, setActive]     = useState(0);
  const [lang, setLang]         = useState('Node.js');
  const [codeTab, setCodeTab]   = useState('payLink');
  const [copied, setCopied]     = useState('');

  const copy = (text) => {
    navigator.clipboard.writeText(text);
    setCopied('1');
    setTimeout(() => setCopied(''), 2000);
  };

  const codeSource = CODE[lang]?.[codeTab] || '';

  return (
    <div>
      <div style={{ display:'grid', gridTemplateColumns:'1fr 1fr', gap:'1.5rem', marginBottom:'2rem' }}>
        {/* Steps list */}
        <div style={{ display:'flex', flexDirection:'column', gap:'0.375rem' }}>
          {STEPS.map((s, i) => (
            <div key={i} onClick={() => setActive(i)}
              style={{ display:'flex', gap:'0.875rem', padding:'0.875rem', borderRadius:'var(--radius-md)', cursor:'pointer', border:'1px solid', borderColor: active===i ? 'var(--blue-200,#bfdbfe)' : 'transparent', background: active===i ? 'var(--blue-50)' : 'transparent', transition:'all 0.15s', alignItems:'flex-start' }}>
              <div style={{ width:28, height:28, borderRadius:'50%', background: active===i ? 'var(--brand)' : 'var(--gray-200)', color: active===i ? '#fff' : 'var(--text-tertiary)', display:'flex', alignItems:'center', justifyContent:'center', fontWeight:700, fontSize:'0.78rem', flexShrink:0, transition:'all 0.15s' }}>{s.num}</div>
              <div style={{ flex:1 }}>
                <div style={{ fontWeight:600, fontSize:'0.875rem', color:'var(--text-primary)', marginBottom: active===i?'0.25rem':0 }}>{s.title}</div>
                {active===i && <div style={{ fontSize:'0.8125rem', color:'var(--text-secondary)', lineHeight:1.6 }}>{s.desc}</div>}
              </div>
              {active===i && <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="var(--brand)" strokeWidth="2.5"><path d="M9 18l6-6-6-6"/></svg>}
            </div>
          ))}
        </div>

        {/* Detail card */}
        <div style={{ position:'sticky', top:'1rem' }}>
          <div className="card">
            <div style={{ fontSize:'0.68rem', fontWeight:700, textTransform:'uppercase', letterSpacing:'0.1em', color:'var(--brand)', marginBottom:'0.5rem' }}>Step {STEPS[active].num}</div>
            <h3 style={{ fontSize:'1.0625rem', fontWeight:700, marginBottom:'0.75rem' }}>{STEPS[active].title}</h3>
            <p style={{ fontSize:'0.875rem', color:'var(--text-secondary)', lineHeight:1.7, marginBottom:'1rem' }}>{STEPS[active].desc}</p>
            <div style={{ background:'var(--gray-900)', border:'1px solid rgba(255,255,255,0.07)', borderRadius:'var(--radius-sm)', padding:'0.875rem 1rem' }}>
              <pre style={{ fontFamily:'var(--font-mono)', fontSize:'0.78rem', color:'#93c5fd', margin:0, whiteSpace:'pre-wrap', lineHeight:1.8 }}>{STEPS[active].detail}</pre>
            </div>
          </div>
        </div>
      </div>

      {/* Code samples */}
      <div className="card">
        <div style={{ display:'flex', justifyContent:'space-between', alignItems:'center', marginBottom:'1rem', flexWrap:'wrap', gap:'0.5rem' }}>
          <h3 style={{ fontSize:'0.9375rem', fontWeight:700 }}>Integration Examples</h3>
          <div style={{ display:'flex', gap:'0.25rem' }}>
            {['Node.js','Python'].map(l => (
              <button key={l} onClick={() => setLang(l)} style={{ padding:'0.25rem 0.7rem', border:'none', borderRadius:'4px', fontSize:'0.78rem', fontWeight:600, cursor:'pointer', fontFamily:'inherit', background: lang===l ? 'var(--brand)' : 'var(--gray-100)', color: lang===l ? '#fff' : 'var(--text-secondary)' }}>{l}</button>
            ))}
          </div>
        </div>

        <div style={{ display:'flex', gap:'0.25rem', marginBottom:'0.75rem' }}>
          {[['payLink','Create Payment Link'],['webhook','Verify Webhook']].map(([id,label]) => (
            <button key={id} onClick={() => setCodeTab(id)} style={{ padding:'0.3rem 0.875rem', border:'1px solid', borderColor: codeTab===id ? 'var(--border-brand)' : 'var(--border)', borderRadius:'var(--radius-sm)', fontSize:'0.8rem', fontWeight:600, cursor:'pointer', fontFamily:'inherit', background: codeTab===id ? 'var(--blue-50)' : 'var(--bg-surface)', color: codeTab===id ? 'var(--blue-700)' : 'var(--text-secondary)' }}>{label}</button>
          ))}
        </div>

        <div className="code-block" style={{ position:'relative' }}>
          <button className="copy-btn" onClick={() => copy(codeSource)}>{copied ? '✓ Copied' : 'Copy'}</button>
          <pre><code>{codeSource}</code></pre>
        </div>
      </div>
    </div>
  );
}
