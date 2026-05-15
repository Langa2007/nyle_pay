import React, { useMemo, useState } from 'react';
import MarketingNav from '../components/MarketingNav';
import SandboxTester from '../components/SandboxTester';

const BASE_URL = 'https://nyle-pay.onrender.com';

const endpoints = [
  {
    group: 'Authentication',
    method: 'POST',
    path: '/api/auth/business-access/request',
    auth: 'Public',
    title: 'Request business access',
    description: 'Sends a 6-digit Resend email verification code for NylePay Business access.',
    body: { fullName: 'Jane Wanjiru', email: 'payments@example.com' },
  },
  {
    group: 'Authentication',
    method: 'POST',
    path: '/api/auth/business-access/confirm',
    auth: 'Public',
    title: 'Confirm business email',
    description: 'Verifies the 6-digit email code and returns a JWT for the Business dashboard.',
    body: { email: 'payments@example.com', code: '123456' },
  },
  {
    group: 'Business setup',
    method: 'POST',
    path: '/api/business/register',
    auth: 'JWT Bearer',
    title: 'Register a business',
    description: 'Creates a business profile, initial settlement policy, and API credentials. Secret credentials must stay server-side.',
    body: { businessName: 'Acme Store', businessEmail: 'payments@acme.co.ke', settlementMethod: 'MPESA', settlementPhone: '254712345678', fallbackSettlementMethod: 'AIRTEL_MONEY', webhookUrl: 'https://example.com/nylepay/webhook' },
  },
  {
    group: 'Routing',
    method: 'GET',
    path: '/api/routes/capabilities?country=KE',
    auth: 'Secret Key',
    title: 'List route capabilities',
    description: 'Returns supported source and destination rails for a country, starting with Kenya.',
  },
  {
    group: 'Routing',
    method: 'POST',
    path: '/api/routes/quote',
    auth: 'Secret Key',
    title: 'Quote a route',
    description: 'Prices a route before money moves, including rail, fee, estimated speed, FX, and fallback information.',
    body: {
      sourceRail: 'AIRTEL_MONEY',
      destinationRail: 'PESALINK',
      sourceAsset: 'KSH',
      destinationAsset: 'KSH',
      amount: 1500,
      destination: { phone: '254733123456', bankCode: '01', accountNumber: '1234567890', accountName: 'Acme Store' },
      idempotencyKey: 'ORDER-1001',
    },
  },
  {
    group: 'Routing',
    method: 'POST',
    path: '/api/routes/execute',
    auth: 'Secret Key',
    title: 'Execute a route',
    description: 'Starts a quoted route. NylePay tracks each leg until the route is completed, failed, or awaiting customer/provider action.',
    body: {
      sourceRail: 'NYLEPAY_WALLET',
      destinationRail: 'AIRTEL_MONEY',
      sourceAsset: 'KSH',
      amount: 750,
      idempotencyKey: 'ORDER-1001',
      destination: { phone: '254733123456' },
    },
  },
  {
    group: 'Routing',
    method: 'GET',
    path: '/api/routes/{routeId}',
    auth: 'Secret Key',
    title: 'Get route status',
    description: 'Returns route status, source, destination, fees, provider references, and leg-level state.',
  },
  {
    group: 'Checkout',
    method: 'POST',
    path: '/api/business/checkout-links',
    auth: 'JWT Bearer',
    title: 'Create business checkout link',
    description: 'Creates a hosted checkout URL that resolves into a NylePay route after the customer selects a rail.',
    body: { amount: 1500, currency: 'KES', description: 'Order #1001', destinationRail: 'AIRTEL_MONEY', redirectUrl: 'https://example.com/thank-you', expiryMinutes: 60 },
  },
  {
    group: 'Settlements',
    method: 'POST',
    path: '/api/business/settlement-policy',
    auth: 'Secret Key',
    title: 'Update settlement policy',
    description: 'Sets primary destination, fallback destination, settlement mode, and route preference.',
    body: { mode: 'REALTIME', primaryRail: 'MPESA', primaryPhone: '254712345678', fallbackRail: 'AIRTEL_MONEY', bankSwitchRail: 'PESALINK', preference: 'FASTEST' },
  },
];

const webhookEvents = [
  ['route.created', 'A route has been accepted and is ready for provider processing.'],
  ['route.processing', 'One or more route legs are in progress.'],
  ['route.completed', 'The final destination has received the routed value.'],
  ['route.failed', 'The route failed, timed out, or was rejected.'],
  ['settlement.completed', 'A business settlement was delivered to the configured destination.'],
  ['webhook.retry', 'A webhook delivery failed and has been scheduled for retry.'],
];

const routePlaybooks = [
  {
    rail: 'M-Pesa',
    summary: 'Collect from a customer M-Pesa number, then route the confirmed value into a NylePay wallet, merchant balance, bank, PesaLink, Airtel Money, or another allowed destination.',
    note: 'M-Pesa source routes start with STK Push. NylePay continues the next leg only after Safaricom confirms payment.',
    quote: {
      sourceRail: 'MPESA',
      destinationRail: 'PESALINK',
      sourceAsset: 'KSH',
      destinationAsset: 'KSH',
      amount: 1500,
      destination: { phone: '254712345678', bankCode: '01', accountNumber: '1234567890', accountName: 'Acme Store' },
      idempotencyKey: 'ORDER-MPESA-1001',
    },
    execute: {
      sourceRail: 'MPESA',
      destinationRail: 'PESALINK',
      sourceAsset: 'KSH',
      destinationAsset: 'KSH',
      amount: 1500,
      destination: { phone: '254712345678', bankCode: '01', accountNumber: '1234567890', accountName: 'Acme Store' },
      idempotencyKey: 'ORDER-MPESA-1001',
    },
  },
  {
    rail: 'Airtel Money',
    summary: 'Collect from Airtel Money or settle wallet funds to an Airtel Money number as a first-class Kenyan mobile-money rail.',
    note: 'Airtel Money routes wait for provider confirmation before settlement or onward routing.',
    quote: {
      sourceRail: 'AIRTEL_MONEY',
      destinationRail: 'NYLEPAY_WALLET',
      sourceAsset: 'KSH',
      destinationAsset: 'KSH',
      amount: 1200,
      destination: { phone: '254733123456' },
      idempotencyKey: 'ORDER-AIRTEL-1001',
    },
    execute: {
      sourceRail: 'NYLEPAY_WALLET',
      destinationRail: 'AIRTEL_MONEY',
      sourceAsset: 'KSH',
      amount: 750,
      destination: { phone: '254733123456' },
      idempotencyKey: 'PAYOUT-AIRTEL-1001',
    },
  },
  {
    rail: 'PesaLink',
    summary: 'Route funds to a Kenyan bank account through the bank-switch rail where available.',
    note: 'Use PesaLink when the destination is a bank account and near real-time bank settlement is preferred.',
    quote: {
      sourceRail: 'NYLEPAY_WALLET',
      destinationRail: 'PESALINK',
      sourceAsset: 'KSH',
      amount: 5000,
      destination: { bankCode: '01', accountNumber: '1234567890', accountName: 'Acme Store' },
      idempotencyKey: 'PESALINK-SETTLE-1001',
    },
    execute: {
      sourceRail: 'NYLEPAY_WALLET',
      destinationRail: 'PESALINK',
      sourceAsset: 'KSH',
      amount: 5000,
      destination: { bankCode: '01', accountNumber: '1234567890', accountName: 'Acme Store' },
      idempotencyKey: 'PESALINK-SETTLE-1001',
    },
  },
  {
    rail: 'Bank',
    summary: 'Settle wallet, card, mobile-money, or converted value to a Kenyan bank account.',
    note: 'Bank payout timing depends on the bank/provider. PesaLink may be a faster bank-switch route where supported.',
    quote: {
      sourceRail: 'CARD',
      destinationRail: 'BANK',
      sourceAsset: 'KSH',
      amount: 3500,
      destination: { bankCode: '68', accountNumber: '9876543210', accountName: 'Acme Store', country: 'KE' },
      idempotencyKey: 'CARD-BANK-1001',
    },
    execute: {
      sourceRail: 'NYLEPAY_WALLET',
      destinationRail: 'BANK',
      sourceAsset: 'KSH',
      amount: 3500,
      destination: { bankCode: '68', accountNumber: '9876543210', accountName: 'Acme Store', country: 'KE' },
      idempotencyKey: 'WALLET-BANK-1001',
    },
  },
  {
    rail: 'Crypto and stablecoins',
    summary: 'Receive supported on-chain assets or exchange balances, then route value to wallet, M-Pesa, Airtel Money, PesaLink, bank, or merchant settlement after confirmation and conversion.',
    note: 'On-chain routes return a NylePay custody deposit address. The route continues only after blockchain/provider confirmation.',
    quote: {
      sourceRail: 'ONCHAIN',
      sourceAsset: 'USDT',
      destinationRail: 'MPESA',
      destinationAsset: 'KSH',
      amount: 25,
      destination: { phone: '254712345678', chain: 'POLYGON' },
      idempotencyKey: 'USDT-MPESA-1001',
    },
    execute: {
      sourceRail: 'ONCHAIN',
      sourceAsset: 'USDT',
      destinationRail: 'MPESA',
      destinationAsset: 'KSH',
      amount: 25,
      destination: { phone: '254712345678', chain: 'POLYGON' },
      idempotencyKey: 'USDT-MPESA-1001',
    },
  },
  {
    rail: 'Cards, Visa, and Mastercard',
    summary: 'Collect card payments through tokenized card providers, then route settled value to the merchant wallet, bank, PesaLink, M-Pesa, or Airtel Money.',
    note: 'NylePay should never store raw card numbers. Use provider-hosted or tokenized capture, then route after provider settlement confirmation.',
    quote: {
      sourceRail: 'CARD',
      destinationRail: 'MERCHANT',
      sourceAsset: 'KSH',
      amount: 4200,
      destination: { merchantReference: 'MERCH-001', cardNetwork: 'VISA_OR_MASTERCARD' },
      idempotencyKey: 'CARD-MERCHANT-1001',
    },
    execute: {
      sourceRail: 'CARD',
      destinationRail: 'MERCHANT',
      sourceAsset: 'KSH',
      amount: 4200,
      destination: { merchantReference: 'MERCH-001', cardNetwork: 'VISA_OR_MASTERCARD' },
      idempotencyKey: 'CARD-MERCHANT-1001',
    },
  },
  {
    rail: 'PayPal',
    summary: 'Create a PayPal checkout intake route into NylePay, then route captured value to wallet, merchant balance, M-Pesa, Airtel Money, PesaLink, or bank after confirmation.',
    note: 'PayPal Orders API handles checkout/capture. PayPal Payouts can send to PayPal recipients where the Business account is approved for Payouts.',
    quote: {
      sourceRail: 'PAYPAL',
      sourceAsset: 'USD',
      destinationRail: 'NYLEPAY_WALLET',
      destinationAsset: 'KSH',
      amount: 20,
      destination: { returnUrl: 'https://example.com/paypal/return', cancelUrl: 'https://example.com/paypal/cancel' },
      idempotencyKey: 'PAYPAL-WALLET-1001',
    },
    execute: {
      sourceRail: 'PAYPAL',
      sourceAsset: 'USD',
      destinationRail: 'NYLEPAY_WALLET',
      destinationAsset: 'KSH',
      amount: 20,
      destination: { returnUrl: 'https://example.com/paypal/return', cancelUrl: 'https://example.com/paypal/cancel' },
      idempotencyKey: 'PAYPAL-WALLET-1001',
    },
  },
];

function requestFor(endpoint) {
  return endpoint.body ? JSON.stringify(endpoint.body, null, 2) : null;
}

function authHeader(auth) {
  if (auth === 'Secret Key') return 'Authorization: Bearer $NYLEPAY_SECRET_KEY';
  if (auth === 'JWT Bearer') return 'Authorization: Bearer $NYLEPAY_JWT';
  return null;
}

function sample(endpoint, language) {
  const url = `${BASE_URL}${endpoint.path}`;
  const body = requestFor(endpoint);
  const header = authHeader(endpoint.auth);

  if (language === 'cURL') {
    const lines = [`curl -X ${endpoint.method} "${url}"`, '  -H "Content-Type: application/json"'];
    if (header) lines.push(`  -H "${header}"`);
    if (body) lines.push(`  -d '${body.replaceAll('\n', '\n  ')}'`);
    return lines.join(' \\\n');
  }

  if (language === 'JavaScript') {
    return `const response = await fetch('${url}', {
  method: '${endpoint.method}',
  headers: {
    'Content-Type': 'application/json',${header ? `\n    'Authorization': 'Bearer ' + process.env.${endpoint.auth === 'Secret Key' ? 'NYLEPAY_SECRET_KEY' : 'NYLEPAY_JWT'},` : ''}
  },${body ? `\n  body: JSON.stringify(${body}),` : ''}
});

const result = await response.json();`;
  }

  if (language === 'Python') {
    return `import os
import requests

headers = {'Content-Type': 'application/json'}${header ? `
headers['Authorization'] = 'Bearer ' + os.environ['${endpoint.auth === 'Secret Key' ? 'NYLEPAY_SECRET_KEY' : 'NYLEPAY_JWT'}']` : ''}

response = requests.${endpoint.method.toLowerCase()}(
    '${url}',
    headers=headers${body ? `,
    json=${body.replaceAll('true', 'True').replaceAll('false', 'False').replaceAll('null', 'None')}` : ''}
)
result = response.json()`;
  }

  return `HttpRequest request = HttpRequest.newBuilder()
    .uri(URI.create("${url}"))
    .header("Content-Type", "application/json")${header ? `
    .header("Authorization", "Bearer " + System.getenv("${endpoint.auth === 'Secret Key' ? 'NYLEPAY_SECRET_KEY' : 'NYLEPAY_JWT'}"))` : ''}
    .method("${endpoint.method}", ${body ? `HttpRequest.BodyPublishers.ofString("""
${body}
""")` : 'HttpRequest.BodyPublishers.noBody()'})
    .build();`;
}

function routeSample(body, action, language) {
  const endpoint = {
    method: 'POST',
    path: action === 'quote' ? '/api/routes/quote' : '/api/routes/execute',
    auth: 'Secret Key',
    body,
  };
  return sample(endpoint, language);
}

const languages = ['cURL', 'JavaScript', 'Python', 'Java'];
const groups = ['All', ...new Set(endpoints.map((endpoint) => endpoint.group))];

export default function ApiDocs({ embedded = false }) {
  const [tab, setTab] = useState('playbooks');
  const [language, setLanguage] = useState('cURL');
  const [group, setGroup] = useState('All');
  const [activePath, setActivePath] = useState(endpoints[0].path);
  const [activePlaybook, setActivePlaybook] = useState(routePlaybooks[0].rail);
  const [playbookAction, setPlaybookAction] = useState('quote');
  const [copied, setCopied] = useState(false);

  const filtered = useMemo(() => (
    group === 'All' ? endpoints : endpoints.filter((endpoint) => endpoint.group === group)
  ), [group]);
  const active = endpoints.find((endpoint) => endpoint.path === activePath) || filtered[0] || endpoints[0];
  const activeRoute = routePlaybooks.find((playbook) => playbook.rail === activePlaybook) || routePlaybooks[0];
  const code = tab === 'playbooks'
    ? routeSample(activeRoute[playbookAction], playbookAction, language)
    : sample(active, language);

  const copy = async () => {
    await navigator.clipboard.writeText(code);
    setCopied(true);
    setTimeout(() => setCopied(false), 1600);
  };

  const content = (
    <main className={embedded ? 'docs-shell embedded' : 'docs-shell'}>
      {!embedded && (
        <section className="marketing-subhero docs-hero">
          <div className="eyebrow">API documentation</div>
          <h1>Route business money from any supported rail</h1>
          <p>
            NylePay Business is a JSON over HTTP API for quoting, executing, settling, and reconciling routes across M-Pesa, Airtel Money, PesaLink, banks, wallets, cards, and crypto rails.
          </p>
        </section>
      )}

      <div className="tab-bar">
        {[
          ['playbooks', 'Routing playbooks'],
          ['reference', 'Endpoint reference'],
          ['webhooks', 'Webhooks'],
          ['sandbox', 'Sandbox tester'],
        ].map(([id, label]) => (
          <button key={id} className={`tab-item ${tab === id ? 'active' : ''}`} onClick={() => setTab(id)}>{label}</button>
        ))}
      </div>

      {tab === 'playbooks' && (
        <section className="docs-reference">
          <aside className="docs-sidebar">
            <div className="docs-filter">
              {['quote', 'execute'].map((item) => (
                <button key={item} className={playbookAction === item ? 'active' : ''} onClick={() => setPlaybookAction(item)}>{item}</button>
              ))}
            </div>
            {routePlaybooks.map((playbook) => (
              <button key={playbook.rail} className={`endpoint-row ${activeRoute.rail === playbook.rail ? 'active' : ''}`} onClick={() => setActivePlaybook(playbook.rail)}>
                <span className="badge badge-blue">RAIL</span>
                <span>{playbook.rail}</span>
              </button>
            ))}
          </aside>

          <article className="docs-detail">
            <div className="endpoint-heading">
              <span className="badge badge-method-post">POST</span>
              <code>{playbookAction === 'quote' ? '/api/routes/quote' : '/api/routes/execute'}</code>
              <span className="badge badge-gray">Secret Key</span>
            </div>
            <h2>{activeRoute.rail} routing</h2>
            <p>{activeRoute.summary}</p>
            <div className="alert alert-info" style={{ marginBottom: '1.25rem' }}>{activeRoute.note}</div>

            <div className="card docs-card">
              <h3>{playbookAction === 'quote' ? 'Quote body' : 'Execute body'}</h3>
              <pre className="schema-preview">{JSON.stringify(activeRoute[playbookAction], null, 2)}</pre>
            </div>

            <div className="card docs-card">
              <div className="code-header">
                <h3>{language} example</h3>
                <div className="language-tabs">
                  {languages.map((item) => (
                    <button key={item} className={language === item ? 'active' : ''} onClick={() => setLanguage(item)}>{item}</button>
                  ))}
                </div>
              </div>
              <div className="code-block">
                <button className="copy-btn" onClick={copy}>{copied ? 'Copied' : 'Copy'}</button>
                <pre><code>{code}</code></pre>
              </div>
            </div>
          </article>
        </section>
      )}

      {tab === 'reference' && (
        <section className="docs-reference">
          <aside className="docs-sidebar">
            <div className="docs-filter">
              {groups.map((item) => (
                <button key={item} className={group === item ? 'active' : ''} onClick={() => setGroup(item)}>{item}</button>
              ))}
            </div>
            {filtered.map((endpoint) => (
              <button key={`${endpoint.method}-${endpoint.path}`} className={`endpoint-row ${active.path === endpoint.path ? 'active' : ''}`} onClick={() => setActivePath(endpoint.path)}>
                <span className={`badge ${endpoint.method === 'GET' ? 'badge-method-get' : 'badge-method-post'}`}>{endpoint.method}</span>
                <span>{endpoint.title}</span>
              </button>
            ))}
          </aside>

          <article className="docs-detail">
            <div className="endpoint-heading">
              <span className={`badge ${active.method === 'GET' ? 'badge-method-get' : 'badge-method-post'}`}>{active.method}</span>
              <code>{active.path}</code>
              <span className="badge badge-gray">{active.auth}</span>
            </div>
            <h2>{active.title}</h2>
            <p>{active.description}</p>

            {active.body && (
              <div className="card docs-card">
                <h3>Request body</h3>
                <pre className="schema-preview">{requestFor(active)}</pre>
              </div>
            )}

            <div className="card docs-card">
              <div className="code-header">
                <h3>{language} example</h3>
                <div className="language-tabs">
                  {languages.map((item) => (
                    <button key={item} className={language === item ? 'active' : ''} onClick={() => setLanguage(item)}>{item}</button>
                  ))}
                </div>
              </div>
              <div className="code-block">
                <button className="copy-btn" onClick={copy}>{copied ? 'Copied' : 'Copy'}</button>
                <pre><code>{code}</code></pre>
              </div>
            </div>
          </article>
        </section>
      )}

      {tab === 'webhooks' && (
        <section className="docs-webhooks">
          <div className="alert alert-info">
            Always verify the <code>X-NylePay-Signature</code> header against the raw request body before fulfilling an order or marking a route as settled.
          </div>
          <div className="card docs-card">
            <h3>Event types</h3>
            <table className="data-table">
              <thead><tr><th>Event</th><th>When it is sent</th></tr></thead>
              <tbody>
                {webhookEvents.map(([event, description]) => (
                  <tr key={event}><td><code>{event}</code></td><td>{description}</td></tr>
                ))}
              </tbody>
            </table>
          </div>
          <div className="card docs-card">
            <h3>Verification formula</h3>
            <div className="code-block">
              <pre><code>{`expected = HMAC_SHA256(webhookSecret, rawRequestBody)
secure_compare(expected, request.headers['X-NylePay-Signature'])`}</code></pre>
            </div>
          </div>
        </section>
      )}

      {tab === 'sandbox' && <SandboxTester />}
    </main>
  );

  if (embedded) return content;

  return (
    <div className="marketing-page">
      <MarketingNav />
      {content}
    </div>
  );
}
