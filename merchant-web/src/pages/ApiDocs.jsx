import React, { useMemo, useState } from 'react';
import MarketingNav from '../components/MarketingNav';
import SandboxTester from '../components/SandboxTester';

const BASE_URL = 'https://api.nylepay.com';

const endpoints = [
  {
    group: 'Authentication',
    method: 'POST',
    path: '/api/auth/register',
    auth: 'Public',
    title: 'Create a NylePay user',
    description: 'Creates the user identity used to access NylePay Business.',
    body: { fullName: 'Jane Wanjiru', email: 'payments@example.com', password: 'strong-password', mpesaNumber: '254712345678', countryCode: 'KE' },
  },
  {
    group: 'Authentication',
    method: 'POST',
    path: '/api/auth/login',
    auth: 'Public',
    title: 'Sign in',
    description: 'Returns a JWT used for dashboard access and business onboarding.',
    body: { email: 'payments@example.com', password: 'strong-password' },
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

const languages = ['cURL', 'JavaScript', 'Python', 'Java'];
const groups = ['All', ...new Set(endpoints.map((endpoint) => endpoint.group))];

export default function ApiDocs({ embedded = false }) {
  const [tab, setTab] = useState('reference');
  const [language, setLanguage] = useState('cURL');
  const [group, setGroup] = useState('All');
  const [activePath, setActivePath] = useState(endpoints[0].path);
  const [copied, setCopied] = useState(false);

  const filtered = useMemo(() => (
    group === 'All' ? endpoints : endpoints.filter((endpoint) => endpoint.group === group)
  ), [group]);
  const active = endpoints.find((endpoint) => endpoint.path === activePath) || filtered[0] || endpoints[0];
  const code = sample(active, language);

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
          ['reference', 'Endpoint reference'],
          ['webhooks', 'Webhooks'],
          ['sandbox', 'Sandbox tester'],
        ].map(([id, label]) => (
          <button key={id} className={`tab-item ${tab === id ? 'active' : ''}`} onClick={() => setTab(id)}>{label}</button>
        ))}
      </div>

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
