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
    description: 'Creates the account used to access the merchant dashboard and start onboarding.',
    body: { fullName: 'Jane Wanjiru', email: 'payments@example.com', password: 'strong-password', mpesaNumber: '254712345678', countryCode: 'KE' },
  },
  {
    group: 'Authentication',
    method: 'POST',
    path: '/api/auth/login',
    auth: 'Public',
    title: 'Sign in',
    description: 'Returns a JWT used for dashboard and merchant onboarding endpoints.',
    body: { email: 'payments@example.com', password: 'strong-password' },
  },
  {
    group: 'Merchant setup',
    method: 'POST',
    path: '/api/merchant/register',
    auth: 'JWT Bearer',
    title: 'Register a merchant',
    description: 'Creates a merchant profile and returns API credentials. Store secret keys server-side only.',
    body: { businessName: 'Acme Store', businessEmail: 'payments@acme.co.ke', settlementPhone: '254712345678', webhookUrl: 'https://example.com/nylepay/webhook' },
  },
  {
    group: 'Merchant setup',
    method: 'GET',
    path: '/api/merchant/profile',
    auth: 'JWT Bearer',
    title: 'Get merchant profile',
    description: 'Returns merchant status, settlement configuration, and public API key details.',
  },
  {
    group: 'Payments',
    method: 'POST',
    path: '/api/merchant/payment-link',
    auth: 'JWT Bearer',
    title: 'Create a payment link',
    description: 'Creates a hosted checkout URL for an order. Customers can pay using available NylePay rails.',
    body: { amount: 1500, currency: 'KES', description: 'Order #1001', redirectUrl: 'https://example.com/thank-you', expiryMinutes: 60 },
  },
  {
    group: 'Payments',
    method: 'POST',
    path: '/api/v1/merchant/charges',
    auth: 'Secret Key',
    title: 'Create a direct charge',
    description: 'Initiates a server-side merchant charge, such as an M-Pesa STK Push.',
    body: { method: 'MPESA', phone: '254712345678', amount: 1500, reference: 'ORDER-1001' },
  },
  {
    group: 'Payments',
    method: 'GET',
    path: '/api/v1/merchant/balance',
    auth: 'Secret Key',
    title: 'Get settlement balance',
    description: 'Returns available and pending merchant settlement balances.',
  },
  {
    group: 'Payouts',
    method: 'POST',
    path: '/api/v1/merchant/transfers',
    auth: 'Secret Key',
    title: 'Create a payout',
    description: 'Moves available merchant funds to an M-Pesa or bank destination.',
    body: { amount: 2500, method: 'MPESA', phone: '254712345678', reference: 'PAYOUT-1001' },
  },
];

const webhookEvents = [
  ['payment.pending', 'A payment has been initiated and is awaiting customer action.'],
  ['payment.completed', 'A payment has been confirmed and credited to merchant settlement balance.'],
  ['payment.failed', 'A payment failed, timed out, or was cancelled.'],
  ['payment.refunded', 'A payment has been refunded or reversed.'],
  ['payout.completed', 'A settlement payout has been delivered.'],
  ['payout.failed', 'A settlement payout failed and funds remain available.'],
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

  if (language === 'PHP') {
    return `<?php
$headers = ['Content-Type: application/json'];${header ? `
$headers[] = 'Authorization: Bearer ' . getenv('${endpoint.auth === 'Secret Key' ? 'NYLEPAY_SECRET_KEY' : 'NYLEPAY_JWT'}');` : ''}

$ch = curl_init('${url}');
curl_setopt_array($ch, [
    CURLOPT_CUSTOMREQUEST => '${endpoint.method}',
    CURLOPT_HTTPHEADER => $headers,
    CURLOPT_RETURNTRANSFER => true,${body ? `
    CURLOPT_POSTFIELDS => json_encode(${JSON.stringify(endpoint.body, null, 4)}),` : ''}
]);

$result = json_decode(curl_exec($ch), true);`;
  }

  return `HttpClient client = HttpClient.newHttpClient();
HttpRequest.Builder builder = HttpRequest.newBuilder()
    .uri(URI.create("${url}"))
    .header("Content-Type", "application/json")${header ? `
    .header("Authorization", "Bearer " + System.getenv("${endpoint.auth === 'Secret Key' ? 'NYLEPAY_SECRET_KEY' : 'NYLEPAY_JWT'}"))` : ''};

HttpRequest request = builder
    .method("${endpoint.method}", ${body ? `HttpRequest.BodyPublishers.ofString("""
${body}
""")` : 'HttpRequest.BodyPublishers.noBody()'})
    .build();

HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());`;
}

const languages = ['cURL', 'JavaScript', 'Python', 'PHP', 'Java'];
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
          <h1>Integrate NylePay from any language</h1>
          <p>
            NylePay is a JSON over HTTP API. Use cURL, JavaScript, Python, PHP, Java, Go, Ruby, .NET, or any stack that can send HTTPS requests.
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
              <button
                key={`${endpoint.method}-${endpoint.path}`}
                className={`endpoint-row ${active.path === endpoint.path ? 'active' : ''}`}
                onClick={() => setActivePath(endpoint.path)}
              >
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
            Always verify the <code>X-NylePay-Signature</code> header against the raw request body before fulfilling an order.
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
