import React, { useState } from 'react';

const STEPS = [
  {
    num: 1,
    title: 'Confirm Business Email',
    desc: 'Start with name and email. NylePay sends a 6-digit code through Resend, then opens the Business dashboard after code verification.',
    detail: 'POST /api/auth/business-access/request\nRequired: fullName, email\nPOST /api/auth/business-access/confirm\nRequired: email, code',
  },
  {
    num: 2,
    title: 'Register Your Business',
    desc: 'Submit business details, first settlement destination, and webhook URL. NylePay returns API credentials for business routing.',
    detail: 'POST /api/business/register\nRequired: businessName, businessEmail, settlementMethod\nOptional: settlementPhone, bank fallback, webhookUrl',
  },
  {
    num: 3,
    title: 'Quote the Route',
    desc: 'Ask NylePay for the best path before money moves. The quote shows rail, destination, fee, speed, FX, and fallback information.',
    detail: 'POST /api/routes/quote\nExample: MPESA -> BANK, AIRTEL_MONEY -> PESALINK, USDT -> MPESA, CARD -> NYLEPAY_WALLET',
  },
  {
    num: 4,
    title: 'Execute and Track',
    desc: 'Execute the quote with an idempotency key, then track route legs until the final destination receives value.',
    detail: 'POST /api/routes/execute\nGET /api/routes/{routeId}\nStatuses: created, processing, awaiting_action, completed, failed',
  },
  {
    num: 5,
    title: 'Verify Webhooks',
    desc: 'Every route and settlement event is signed. Verify signatures before fulfilling orders or marking a route settled.',
    detail: 'HMAC_SHA256(webhookSecret, rawRequestBody)\nCompare with X-NylePay-Signature using timing-safe comparison',
  },
  {
    num: 6,
    title: 'Test in Sandbox, Then Go Live',
    desc: 'Sandbox keys simulate quote, execution, settlement, failure, and webhook flows without real money movement.',
    detail: 'Sandbox: npy_test_ credentials\nProduction: real money, live rails, KYC and risk review required',
  },
];

const CODE = {
  'Node.js': `const quote = await fetch('https://nyle-pay.onrender.com/api/routes/quote', {
  method: 'POST',
  headers: {
    Authorization: \`Bearer \${process.env.NYLEPAY_SECRET_KEY}\`,
    'Content-Type': 'application/json',
  },
  body: JSON.stringify({
    sourceRail: 'AIRTEL_MONEY',
    destinationRail: 'PESALINK',
    sourceAsset: 'KSH',
    destinationAsset: 'KSH',
    amount: 1500,
    destination: { phone: '254733123456', bankCode: '01', accountNumber: '1234567890', accountName: 'Acme Store' },
    idempotencyKey: 'ORDER-1234',
  }),
});

await quote.json();

await fetch('https://nyle-pay.onrender.com/api/routes/execute', {
  method: 'POST',
  headers: {
    Authorization: \`Bearer \${process.env.NYLEPAY_SECRET_KEY}\`,
    'Content-Type': 'application/json',
  },
  body: JSON.stringify({
    sourceRail: 'NYLEPAY_WALLET',
    destinationRail: 'AIRTEL_MONEY',
    sourceAsset: 'KSH',
    amount: 750,
    destination: { phone: '254733123456' },
    idempotencyKey: 'ORDER-1234-PAYOUT',
  }),
});`,
  Python: `import os
import requests

headers = {
    'Authorization': 'Bearer ' + os.environ['NYLEPAY_SECRET_KEY'],
    'Content-Type': 'application/json',
}

quote = requests.post('https://nyle-pay.onrender.com/api/routes/quote', headers=headers, json={
    'sourceRail': 'AIRTEL_MONEY',
    'destinationRail': 'PESALINK',
    'sourceAsset': 'KSH',
    'destinationAsset': 'KSH',
    'amount': 1500,
    'destination': {'phone': '254733123456', 'bankCode': '01', 'accountNumber': '1234567890', 'accountName': 'Acme Store'},
    'idempotencyKey': 'ORDER-1234',
}).json()['data']

requests.post('https://nyle-pay.onrender.com/api/routes/execute', headers=headers, json={
    'sourceRail': 'NYLEPAY_WALLET',
    'destinationRail': 'AIRTEL_MONEY',
    'sourceAsset': 'KSH',
    'amount': 750,
    'destination': {'phone': '254733123456'},
    'idempotencyKey': 'ORDER-1234-PAYOUT',
})`,
};

export default function MerchantGuide() {
  const [active, setActive] = useState(0);
  const [lang, setLang] = useState('Node.js');
  const [copied, setCopied] = useState(false);

  const copy = (text) => {
    navigator.clipboard.writeText(text);
    setCopied(true);
    setTimeout(() => setCopied(false), 2000);
  };

  return (
    <div>
      <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '1.5rem', marginBottom: '2rem' }}>
        <div style={{ display: 'flex', flexDirection: 'column', gap: '0.375rem' }}>
          {STEPS.map((step, index) => (
            <div key={step.title} onClick={() => setActive(index)} style={{ display: 'flex', gap: '0.875rem', padding: '0.875rem', borderRadius: 'var(--radius-md)', cursor: 'pointer', border: '1px solid', borderColor: active === index ? 'var(--blue-200,#bfdbfe)' : 'transparent', background: active === index ? 'var(--blue-50)' : 'transparent', alignItems: 'flex-start' }}>
              <div style={{ width: 28, height: 28, borderRadius: '50%', background: active === index ? 'var(--brand)' : 'var(--gray-200)', color: active === index ? '#fff' : 'var(--text-tertiary)', display: 'flex', alignItems: 'center', justifyContent: 'center', fontWeight: 700, fontSize: '0.78rem', flexShrink: 0 }}>{step.num}</div>
              <div style={{ flex: 1 }}>
                <div style={{ fontWeight: 600, fontSize: '0.875rem', color: 'var(--text-primary)', marginBottom: active === index ? '0.25rem' : 0 }}>{step.title}</div>
                {active === index && <div style={{ fontSize: '0.8125rem', color: 'var(--text-secondary)', lineHeight: 1.6 }}>{step.desc}</div>}
              </div>
            </div>
          ))}
        </div>

        <div className="card" style={{ position: 'sticky', top: '1rem', alignSelf: 'start' }}>
          <div style={{ fontSize: '0.68rem', fontWeight: 700, textTransform: 'uppercase', letterSpacing: '0.1em', color: 'var(--brand)', marginBottom: '0.5rem' }}>Step {STEPS[active].num}</div>
          <h3 style={{ fontSize: '1.0625rem', fontWeight: 700, marginBottom: '0.75rem' }}>{STEPS[active].title}</h3>
          <p style={{ fontSize: '0.875rem', color: 'var(--text-secondary)', lineHeight: 1.7, marginBottom: '1rem' }}>{STEPS[active].desc}</p>
          <pre className="schema-preview">{STEPS[active].detail}</pre>
        </div>
      </div>

      <div className="card">
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1rem', flexWrap: 'wrap', gap: '0.5rem' }}>
          <h3 style={{ fontSize: '0.9375rem', fontWeight: 700 }}>Route Example</h3>
          <div style={{ display: 'flex', gap: '0.25rem' }}>
            {Object.keys(CODE).map((item) => (
              <button key={item} onClick={() => setLang(item)} style={{ padding: '0.25rem 0.7rem', border: 'none', borderRadius: '4px', fontSize: '0.78rem', fontWeight: 600, cursor: 'pointer', fontFamily: 'inherit', background: lang === item ? 'var(--brand)' : 'var(--gray-100)', color: lang === item ? '#fff' : 'var(--text-secondary)' }}>{item}</button>
            ))}
          </div>
        </div>
        <div className="code-block">
          <button className="copy-btn" onClick={() => copy(CODE[lang])}>{copied ? 'Copied' : 'Copy'}</button>
          <pre><code>{CODE[lang]}</code></pre>
        </div>
      </div>
    </div>
  );
}
