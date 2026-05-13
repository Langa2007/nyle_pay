import React, { useState } from 'react';

const STEPS = [
  {
    num: 1,
    title: 'Create a NylePay Consumer Account',
    desc: 'A merchant must first have an active NylePay consumer account with completed KYC. Your personal account is the root identity.',
    detail: 'Sign up at nylepay.com → complete ID verification → your NPY account is created.',
  },
  {
    num: 2,
    title: 'Log In to the Merchant Portal',
    desc: 'Navigate to the Merchant Portal using your existing NylePay credentials. One login — two portals.',
    detail: 'Visit merchant.nylepay.com → sign in → you\'ll be guided to register your business.',
  },
  {
    num: 3,
    title: 'Register Your Business',
    desc: 'Submit your Business Name, Business Email, and Webhook URL (where NylePay will POST payment events).',
    detail: 'POST /api/merchant/register with businessName, businessEmail, webhookUrl.',
  },
  {
    num: 4,
    title: 'Receive Your API Keys',
    desc: 'Upon successful registration you receive a Public Key and a Secret Key. The Secret Key is shown ONCE — store it securely in your .env file.',
    detail: 'publicKey → embed in your frontend.\nsecretKey → keep in backend .env ONLY.',
  },
  {
    num: 5,
    title: 'Configure Webhook & Verification',
    desc: 'Every payment event is HMAC-SHA256 signed with your webhook secret. Validate the X-NylePay-Signature header on every incoming request.',
    detail: 'Check header: X-NylePay-Signature = hmac_sha256(webhookSecret, payload)',
  },
  {
    num: 6,
    title: 'Go Live',
    desc: 'Switch from Sandbox to Production in the Merchant Portal. Your merchant status changes from PENDING → ACTIVE after KYC approval.',
    detail: 'Admin reviews your KYC → ACTIVE status → real money flows enabled.',
  },
];

const MerchantRegistration = () => {
  const [activeStep, setActiveStep] = useState(0);
  const [copied, setCopied] = useState('');

  const copy = (text, key) => {
    navigator.clipboard.writeText(text).then(() => {
      setCopied(key);
      setTimeout(() => setCopied(''), 2000);
    });
  };

  const sampleCode = `// 1. Install NylePay SDK (or call API directly)
// npm install @nylepay/checkout-js

// 2. Server-side: Create payment link
const response = await fetch('https://api.nylepay.com/api/merchant/payment-link', {
  method: 'POST',
  headers: {
    'Authorization': 'Bearer <YOUR_JWT_TOKEN>',
    'Content-Type': 'application/json',
  },
  body: JSON.stringify({
    amount: 1500.00,
    currency: 'KES',
    description: 'Order #1234',
    redirectUrl: 'https://mystore.com/thank-you',
    expiryMinutes: 60,
  }),
});

const { checkoutUrl, reference } = await response.json();
// Redirect customer to checkoutUrl`;

  const webhookCode = `// 3. Validate webhook signature
const crypto = require('crypto');

function verifyNylePaySignature(payload, signature, secret) {
  const expected = crypto
    .createHmac('sha256', secret)
    .update(payload)
    .digest('hex');
  return crypto.timingSafeEqual(
    Buffer.from(expected),
    Buffer.from(signature)
  );
}

app.post('/nylepay-webhook', express.raw({ type: '*/*' }), (req, res) => {
  const sig = req.headers['x-nylepay-signature'];
  if (!verifyNylePaySignature(req.body, sig, process.env.NYLEPAY_WEBHOOK_SECRET)) {
    return res.status(401).send('Invalid signature');
  }
  const event = JSON.parse(req.body);
  if (event.status === 'COMPLETED') {
    // Fulfill order
  }
  res.status(200).send('OK');
});`;

  return (
    <section id="for-merchants" style={styles.section}>
      <div className="gradient-divider" />
      <div style={styles.inner}>
        <div style={styles.header} className="animate-fade-in-up">
          <div className="section-eyebrow">For Merchants</div>
          <h2 className="section-title">
            Accept Payments in <span className="text-gradient">6 Steps</span>
          </h2>
          <p className="section-subtitle">
            From registration to your first live transaction — a clear, structured guide to integrating NylePay into your business.
          </p>
        </div>

        <div style={styles.layout}>
          {/* Steps column */}
          <div style={styles.stepsCol}>
            {STEPS.map((s, i) => (
              <div
                key={i}
                style={{
                  ...styles.step,
                  ...(activeStep === i ? styles.stepActive : {}),
                }}
                onClick={() => setActiveStep(i)}
              >
                <div style={{ ...styles.stepNum, ...(activeStep === i ? styles.stepNumActive : {}) }}>
                  {s.num}
                </div>
                <div style={styles.stepContent}>
                  <div style={styles.stepTitle}>{s.title}</div>
                  {activeStep === i && (
                    <div style={styles.stepDesc}>{s.desc}</div>
                  )}
                </div>
                {activeStep === i && (
                  <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="var(--brand-blue-light)" strokeWidth="2.5" style={{ flexShrink: 0 }}>
                    <path d="M9 18l6-6-6-6"/>
                  </svg>
                )}
              </div>
            ))}
          </div>

          {/* Detail panel */}
          <div style={styles.detailPanel}>
            <div className="glass-panel" style={styles.detailCard}>
              <div style={styles.detailNum}>Step {STEPS[activeStep].num}</div>
              <h3 style={styles.detailTitle}>{STEPS[activeStep].title}</h3>
              <p style={styles.detailDesc}>{STEPS[activeStep].desc}</p>
              <div style={styles.detailNote}>
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="var(--brand-blue-light)" strokeWidth="2">
                  <circle cx="12" cy="12" r="10"/><path d="M12 16v-4M12 8h.01"/>
                </svg>
                <span style={{ fontSize: '0.85rem', color: 'var(--text-secondary)', fontFamily: 'var(--font-mono)', whiteSpace: 'pre-line' }}>
                  {STEPS[activeStep].detail}
                </span>
              </div>
            </div>
          </div>
        </div>

        {/* Code samples */}
        <div style={styles.codeSection}>
          <h3 style={{ marginBottom: '1.5rem', fontSize: '1.3rem' }}>
            <span className="text-gradient">Integration</span> Quickstart
          </h3>
          <div style={styles.codeGrid}>
            <div>
              <div style={styles.codeLabel}>Create a Payment Link</div>
              <div className="code-block" style={{ position: 'relative' }}>
                <button className="copy-btn" onClick={() => copy(sampleCode, 'pay')}>
                  {copied === 'pay' ? 'Copied!' : 'Copy'}
                </button>
                <pre style={{ margin: 0 }}><code>{sampleCode}</code></pre>
              </div>
            </div>
            <div>
              <div style={styles.codeLabel}>Validate Webhook Events</div>
              <div className="code-block" style={{ position: 'relative' }}>
                <button className="copy-btn" onClick={() => copy(webhookCode, 'hook')}>
                  {copied === 'hook' ? 'Copied!' : 'Copy'}
                </button>
                <pre style={{ margin: 0 }}><code>{webhookCode}</code></pre>
              </div>
            </div>
          </div>
        </div>

        {/* CTA */}
        <div style={styles.ctaBox} className="animate-fade-in-up">
          <div>
            <h3 style={{ fontSize: '1.4rem', marginBottom: '0.5rem' }}>Ready to Accept Payments?</h3>
            <p style={{ color: 'var(--text-secondary)', fontSize: '0.95rem' }}>
              Register your business and get API keys in under 5 minutes.
            </p>
          </div>
          <a href="http://localhost:5174" target="_blank" rel="noreferrer" className="btn-primary">
            Open Merchant Portal
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5">
              <path d="M7 17L17 7M17 7H7M17 7v10"/>
            </svg>
          </a>
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
    background: 'linear-gradient(180deg, transparent 0%, rgba(59,130,246,0.03) 50%, transparent 100%)',
  },
  inner: {
    maxWidth: '1280px',
    margin: '0 auto',
  },
  header: {
    marginBottom: '4rem',
  },
  layout: {
    display: 'grid',
    gridTemplateColumns: '1fr 1fr',
    gap: '2rem',
    marginBottom: '4rem',
    alignItems: 'start',
  },
  stepsCol: {
    display: 'flex',
    flexDirection: 'column',
    gap: '0.5rem',
  },
  step: {
    display: 'flex',
    alignItems: 'flex-start',
    gap: '1rem',
    padding: '1rem 1.25rem',
    borderRadius: 'var(--radius-md)',
    cursor: 'pointer',
    transition: 'all 0.25s ease',
    border: '1px solid transparent',
  },
  stepActive: {
    background: 'rgba(59,130,246,0.07)',
    border: '1px solid rgba(59,130,246,0.2)',
  },
  stepNum: {
    width: '2.2rem',
    height: '2.2rem',
    borderRadius: '50%',
    background: 'var(--bg-elevated)',
    border: '1px solid var(--border-color)',
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
    fontFamily: 'var(--font-heading)',
    fontWeight: 700,
    fontSize: '0.85rem',
    color: 'var(--text-secondary)',
    flexShrink: 0,
    transition: 'all 0.25s',
  },
  stepNumActive: {
    background: 'var(--brand-gradient)',
    border: 'none',
    color: '#fff',
    boxShadow: 'var(--glow-blue)',
  },
  stepContent: {
    flex: 1,
  },
  stepTitle: {
    fontWeight: 600,
    fontSize: '0.95rem',
    color: 'var(--text-primary)',
    marginBottom: '0.25rem',
  },
  stepDesc: {
    fontSize: '0.85rem',
    color: 'var(--text-secondary)',
    lineHeight: 1.6,
    marginTop: '0.4rem',
  },
  detailPanel: {
    position: 'sticky',
    top: '7rem',
  },
  detailCard: {
    padding: '2rem',
  },
  detailNum: {
    fontSize: '0.7rem',
    fontWeight: 700,
    letterSpacing: '0.1em',
    textTransform: 'uppercase',
    color: 'var(--brand-blue-light)',
    marginBottom: '0.75rem',
  },
  detailTitle: {
    fontSize: '1.25rem',
    marginBottom: '0.75rem',
    color: 'var(--text-primary)',
  },
  detailDesc: {
    color: 'var(--text-secondary)',
    lineHeight: 1.7,
    marginBottom: '1.5rem',
  },
  detailNote: {
    display: 'flex',
    gap: '0.75rem',
    alignItems: 'flex-start',
    background: 'rgba(59,130,246,0.06)',
    border: '1px solid rgba(59,130,246,0.12)',
    borderRadius: 'var(--radius-md)',
    padding: '1rem',
  },
  codeSection: {
    marginBottom: '3rem',
  },
  codeLabel: {
    fontSize: '0.8rem',
    fontWeight: 600,
    color: 'var(--text-muted)',
    textTransform: 'uppercase',
    letterSpacing: '0.06em',
    marginBottom: '0.6rem',
  },
  codeGrid: {
    display: 'grid',
    gridTemplateColumns: '1fr 1fr',
    gap: '1.5rem',
  },
  ctaBox: {
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'space-between',
    background: 'linear-gradient(135deg, rgba(59,130,246,0.1) 0%, rgba(16,185,129,0.1) 100%)',
    border: '1px solid rgba(59,130,246,0.2)',
    borderRadius: 'var(--radius-xl)',
    padding: '2rem 2.5rem',
    flexWrap: 'wrap',
    gap: '1.5rem',
  },
};

export default MerchantRegistration;
