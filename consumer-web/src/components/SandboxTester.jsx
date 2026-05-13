import React, { useState } from 'react';

const API_BASE = 'http://localhost:8080';

const SANDBOX_TESTS = [
  {
    id: 'ping',
    label: 'Health Check',
    method: 'GET',
    endpoint: '/api/sandbox/health',
    body: null,
    description: 'Verify the NylePay backend is reachable and sandbox mode is active.',
  },
  {
    id: 'register',
    label: 'Register User',
    method: 'POST',
    endpoint: '/api/auth/register',
    body: {
      fullName: 'Test User',
      email: 'test@sandbox.nylepay.com',
      password: 'Sandbox@1234',
      mpesaNumber: '254712345678',
      countryCode: 'KE',
    },
    description: 'Create a new test consumer account. Use any email — sandbox does not send real emails.',
  },
  {
    id: 'login',
    label: 'Login',
    method: 'POST',
    endpoint: '/api/auth/login',
    body: { email: 'test@sandbox.nylepay.com', password: 'Sandbox@1234' },
    description: 'Authenticate and receive a JWT token for subsequent requests.',
  },
  {
    id: 'merchant-reg',
    label: 'Register Merchant',
    method: 'POST',
    endpoint: '/api/merchant/register',
    body: {
      businessName: 'Test Shop Ltd',
      businessEmail: 'shop@sandbox.nylepay.com',
      webhookUrl: 'https://webhook.site/your-unique-id',
    },
    description: 'Register a merchant account and receive test API keys (publicKey + secretKey).',
    requiresAuth: true,
  },
  {
    id: 'payment-link',
    label: 'Create Payment Link',
    method: 'POST',
    endpoint: '/api/merchant/payment-link',
    body: { amount: 150.00, currency: 'KES', description: 'Test Order #001', expiryMinutes: 60 },
    description: 'Generate a sandbox checkout link. No real money moves.',
    requiresAuth: true,
  },
];

const SandboxTester = () => {
  const [activeTest, setActiveTest] = useState(SANDBOX_TESTS[0]);
  const [token, setToken] = useState('');
  const [response, setResponse] = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  const runTest = async () => {
    setLoading(true);
    setError('');
    setResponse(null);

    const headers = { 'Content-Type': 'application/json' };
    if (activeTest.requiresAuth && token) {
      headers['Authorization'] = `Bearer ${token}`;
    }

    try {
      const res = await fetch(`${API_BASE}${activeTest.endpoint}`, {
        method: activeTest.method,
        headers,
        body: activeTest.body ? JSON.stringify(activeTest.body) : undefined,
      });

      const data = await res.json();

      // Auto-extract token on login
      if (activeTest.id === 'login' && data?.data?.token) {
        setToken(data.data.token);
      }

      setResponse({ status: res.status, ok: res.ok, data });
    } catch (err) {
      setError(`Cannot connect to ${API_BASE}. Make sure the Spring Boot backend is running on port 8080.\n\nRun: ./mvnw spring-boot:run -Dspring-boot.run.profiles=local`);
    } finally {
      setLoading(false);
    }
  };

  return (
    <section id="sandbox" style={styles.section}>
      <div className="gradient-divider" />
      <div style={styles.inner}>
        <div style={styles.header} className="animate-fade-in-up">
          <div className="section-eyebrow" style={{ color: 'var(--brand-amber)' }}>Sandbox</div>
          <h2 className="section-title">
            Test Before You <span className="text-gradient-purple">Go Live</span>
          </h2>
          <p className="section-subtitle">
            Sandbox mode mirrors production exactly — no real money, no KYC required. Run integration tests directly from this page.
          </p>
        </div>

        {/* Sandbox badge */}
        <div style={styles.sandboxBanner}>
          <div style={styles.sandboxIcon}>
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="var(--brand-amber)" strokeWidth="2">
              <path d="M10.29 3.86L1.82 18a2 2 0 001.71 3h16.94a2 2 0 001.71-3L13.71 3.86a2 2 0 00-3.42 0z"/>
              <line x1="12" y1="9" x2="12" y2="13"/><line x1="12" y1="17" x2="12.01" y2="17"/>
            </svg>
          </div>
          <div>
            <strong style={{ color: 'var(--brand-amber)', display: 'block', marginBottom: '0.25rem' }}>Sandbox Environment</strong>
            <span style={{ fontSize: '0.875rem', color: 'var(--text-secondary)' }}>
              All transactions are simulated. No real funds are moved. Backend must be running locally on <code style={{ fontFamily: 'var(--font-mono)', color: 'var(--text-primary)' }}>localhost:8080</code> with the <code style={{ fontFamily: 'var(--font-mono)', color: 'var(--text-primary)' }}>local</code> profile.
            </span>
          </div>
        </div>

        <div style={styles.testerLayout}>
          {/* Test list */}
          <div style={styles.testList}>
            <div style={styles.listHeader}>API Endpoints</div>
            {SANDBOX_TESTS.map((t) => (
              <div
                key={t.id}
                style={{
                  ...styles.testItem,
                  ...(activeTest.id === t.id ? styles.testItemActive : {}),
                }}
                onClick={() => { setActiveTest(t); setResponse(null); setError(''); }}
              >
                <span style={{
                  ...styles.methodBadge,
                  background: t.method === 'GET' ? 'rgba(16,185,129,0.15)' : 'rgba(59,130,246,0.15)',
                  color: t.method === 'GET' ? 'var(--brand-green-light)' : 'var(--brand-blue-light)',
                }}>
                  {t.method}
                </span>
                <span style={styles.testLabel}>{t.label}</span>
                {t.requiresAuth && (
                  <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="var(--text-muted)" strokeWidth="2" title="Requires auth">
                    <rect x="3" y="11" width="18" height="11" rx="2"/><path d="M7 11V7a5 5 0 0110 0v4"/>
                  </svg>
                )}
              </div>
            ))}

            {/* JWT Token input */}
            <div style={styles.tokenSection}>
              <div style={styles.tokenLabel}>
                <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="var(--brand-amber)" strokeWidth="2">
                  <rect x="3" y="11" width="18" height="11" rx="2"/><path d="M7 11V7a5 5 0 0110 0v4"/>
                </svg>
                JWT Token (auto-filled on login)
              </div>
              <textarea
                value={token}
                onChange={e => setToken(e.target.value)}
                placeholder="Token auto-fills after running Login test..."
                style={styles.tokenInput}
                rows={3}
              />
            </div>
          </div>

          {/* Request / response panel */}
          <div style={styles.requestPanel}>
            <div style={styles.requestHeader}>
              <div>
                <span style={{
                  ...styles.methodBadge,
                  fontSize: '0.9rem',
                  padding: '0.3rem 0.8rem',
                  background: activeTest.method === 'GET' ? 'rgba(16,185,129,0.15)' : 'rgba(59,130,246,0.15)',
                  color: activeTest.method === 'GET' ? 'var(--brand-green-light)' : 'var(--brand-blue-light)',
                }}>
                  {activeTest.method}
                </span>
                <span style={{ fontFamily: 'var(--font-mono)', fontSize: '0.9rem', marginLeft: '0.75rem', color: 'var(--text-secondary)' }}>
                  {API_BASE}{activeTest.endpoint}
                </span>
              </div>
              <button
                className="btn-primary"
                onClick={runTest}
                disabled={loading}
                style={{ padding: '0.5rem 1.25rem', fontSize: '0.875rem' }}
              >
                {loading ? (
                  <>
                    <span style={{ display: 'inline-block', width: '14px', height: '14px', border: '2px solid rgba(255,255,255,0.3)', borderTopColor: '#fff', borderRadius: '50%', animation: 'spin-slow 0.8s linear infinite' }} />
                    Running...
                  </>
                ) : (
                  <>
                    <svg width="14" height="14" viewBox="0 0 24 24" fill="currentColor">
                      <polygon points="5,3 19,12 5,21"/>
                    </svg>
                    Run Test
                  </>
                )}
              </button>
            </div>

            <p style={styles.testDesc}>{activeTest.description}</p>

            {activeTest.body && (
              <div style={{ marginBottom: '1.25rem' }}>
                <div style={styles.panelLabel}>Request Body</div>
                <div className="code-block">
                  <pre style={{ margin: 0 }}><code>{JSON.stringify(activeTest.body, null, 2)}</code></pre>
                </div>
              </div>
            )}

            {/* Response */}
            {(response || error) && (
              <div>
                <div style={styles.panelLabel}>
                  Response
                  {response && (
                    <span style={{
                      marginLeft: '0.75rem',
                      padding: '0.15rem 0.6rem',
                      borderRadius: 'var(--radius-full)',
                      fontSize: '0.7rem',
                      fontWeight: 700,
                      background: response.ok ? 'rgba(16,185,129,0.15)' : 'rgba(239,68,68,0.15)',
                      color: response.ok ? 'var(--brand-green-light)' : '#f87171',
                    }}>
                      {response.status} {response.ok ? 'OK' : 'ERROR'}
                    </span>
                  )}
                </div>
                {error ? (
                  <div style={styles.errorBox}>
                    <pre style={{ margin: 0, whiteSpace: 'pre-wrap', fontSize: '0.85rem' }}>{error}</pre>
                  </div>
                ) : (
                  <div className="code-block">
                    <pre style={{ margin: 0 }}><code>{JSON.stringify(response.data, null, 2)}</code></pre>
                  </div>
                )}
              </div>
            )}
          </div>
        </div>

        {/* Local setup guide */}
        <div style={styles.setupBox}>
          <h3 style={{ marginBottom: '1.25rem', fontSize: '1.1rem' }}>
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="var(--brand-green)" strokeWidth="2" style={{ verticalAlign: 'middle', marginRight: '0.5rem' }}>
              <rect x="2" y="3" width="20" height="14" rx="2"/><path d="M8 21h8M12 17v4"/>
            </svg>
            Run Backend Locally
          </h3>
          <div style={styles.setupSteps}>
            {[
              { label: '1. Use local profile (local postgres)', code: './mvnw spring-boot:run -Dspring-boot.run.profiles=local' },
              { label: '2. Or set env vars and run directly', code: 'SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/nylepay ./mvnw spring-boot:run' },
              { label: '3. Confirm backend is live', code: 'curl http://localhost:8080/api/sandbox/health' },
            ].map((s, i) => (
              <div key={i} style={styles.setupStep}>
                <div style={styles.setupLabel}>{s.label}</div>
                <div className="code-block" style={{ padding: '0.75rem 1rem', fontSize: '0.82rem' }}>
                  <code style={{ fontFamily: 'var(--font-mono)' }}>$ {s.code}</code>
                </div>
              </div>
            ))}
          </div>
        </div>
      </div>
    </section>
  );
};

const styles = {
  section: {
    padding: '7rem 5%',
    position: 'relative',
    zIndex: 10,
  },
  inner: {
    maxWidth: '1280px',
    margin: '0 auto',
  },
  header: {
    marginBottom: '2.5rem',
  },
  sandboxBanner: {
    display: 'flex',
    gap: '1rem',
    alignItems: 'flex-start',
    background: 'var(--sandbox-amber)',
    border: '1px solid var(--sandbox-border)',
    borderRadius: 'var(--radius-lg)',
    padding: '1.25rem 1.5rem',
    marginBottom: '3rem',
  },
  sandboxIcon: {
    flexShrink: 0,
    marginTop: '2px',
  },
  testerLayout: {
    display: 'grid',
    gridTemplateColumns: '280px 1fr',
    gap: '1.5rem',
    marginBottom: '3rem',
  },
  testList: {
    background: 'var(--bg-secondary)',
    border: '1px solid var(--border-color)',
    borderRadius: 'var(--radius-lg)',
    overflow: 'hidden',
    display: 'flex',
    flexDirection: 'column',
  },
  listHeader: {
    fontSize: '0.7rem',
    fontWeight: 700,
    letterSpacing: '0.1em',
    textTransform: 'uppercase',
    color: 'var(--text-muted)',
    padding: '0.875rem 1rem',
    borderBottom: '1px solid var(--border-color)',
  },
  testItem: {
    display: 'flex',
    alignItems: 'center',
    gap: '0.6rem',
    padding: '0.75rem 1rem',
    cursor: 'pointer',
    transition: 'background 0.2s',
    borderBottom: '1px solid var(--border-color)',
  },
  testItemActive: {
    background: 'rgba(59,130,246,0.1)',
    borderLeft: '3px solid var(--brand-blue)',
  },
  methodBadge: {
    fontSize: '0.65rem',
    fontWeight: 700,
    fontFamily: 'var(--font-mono)',
    padding: '0.2rem 0.5rem',
    borderRadius: '4px',
    flexShrink: 0,
  },
  testLabel: {
    flex: 1,
    fontSize: '0.85rem',
    color: 'var(--text-secondary)',
    fontWeight: 500,
  },
  tokenSection: {
    padding: '1rem',
    marginTop: 'auto',
    borderTop: '1px solid var(--border-color)',
  },
  tokenLabel: {
    fontSize: '0.7rem',
    fontWeight: 600,
    color: 'var(--text-muted)',
    textTransform: 'uppercase',
    letterSpacing: '0.06em',
    marginBottom: '0.5rem',
    display: 'flex',
    alignItems: 'center',
    gap: '0.4rem',
  },
  tokenInput: {
    width: '100%',
    background: 'rgba(0,0,0,0.3)',
    border: '1px solid var(--border-color)',
    borderRadius: 'var(--radius-sm)',
    color: 'var(--text-secondary)',
    fontSize: '0.72rem',
    fontFamily: 'var(--font-mono)',
    padding: '0.5rem 0.6rem',
    resize: 'vertical',
    outline: 'none',
  },
  requestPanel: {
    background: 'var(--bg-secondary)',
    border: '1px solid var(--border-color)',
    borderRadius: 'var(--radius-lg)',
    padding: '1.5rem',
  },
  requestHeader: {
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'space-between',
    marginBottom: '1rem',
    flexWrap: 'wrap',
    gap: '1rem',
  },
  testDesc: {
    color: 'var(--text-secondary)',
    fontSize: '0.875rem',
    marginBottom: '1.25rem',
    lineHeight: 1.6,
  },
  panelLabel: {
    fontSize: '0.72rem',
    fontWeight: 700,
    color: 'var(--text-muted)',
    textTransform: 'uppercase',
    letterSpacing: '0.08em',
    marginBottom: '0.5rem',
    display: 'flex',
    alignItems: 'center',
  },
  errorBox: {
    background: 'rgba(239,68,68,0.07)',
    border: '1px solid rgba(239,68,68,0.25)',
    borderRadius: 'var(--radius-md)',
    padding: '1.25rem',
    color: '#fca5a5',
  },
  setupBox: {
    background: 'rgba(16,185,129,0.04)',
    border: '1px solid rgba(16,185,129,0.15)',
    borderRadius: 'var(--radius-xl)',
    padding: '2rem',
  },
  setupSteps: {
    display: 'flex',
    flexDirection: 'column',
    gap: '1rem',
  },
  setupStep: {
    display: 'flex',
    flexDirection: 'column',
    gap: '0.4rem',
  },
  setupLabel: {
    fontSize: '0.85rem',
    color: 'var(--text-secondary)',
    fontWeight: 500,
  },
};

export default SandboxTester;
