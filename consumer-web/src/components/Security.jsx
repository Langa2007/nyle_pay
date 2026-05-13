import React from 'react';

const FEATURES = [
  {
    icon: (
      <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8">
        <rect x="3" y="11" width="18" height="11" rx="2"/><path d="M7 11V7a5 5 0 0110 0v4"/>
      </svg>
    ),
    color: '#3b82f6',
    title: 'AES-256-GCM Encryption',
    desc: 'All sensitive data — secret keys, bank details, crypto private keys — are encrypted at rest using AES-256-GCM with unique IVs per record.',
  },
  {
    icon: (
      <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8">
        <path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"/>
      </svg>
    ),
    color: '#10b981',
    title: 'JWT + 2FA Authentication',
    desc: 'Short-lived JWT access tokens, optional TOTP/OTP second factor, and account lockout after 5 failed login attempts.',
  },
  {
    icon: (
      <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8">
        <polyline points="22 12 18 12 15 21 9 3 6 12 2 12"/>
      </svg>
    ),
    color: '#8b5cf6',
    title: 'ACID-Compliant Ledger',
    desc: 'Every balance movement executes inside a serializable database transaction. Idempotency keys prevent double charges.',
  },
  {
    icon: (
      <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8">
        <circle cx="12" cy="12" r="10"/><line x1="2" y1="12" x2="22" y2="12"/><path d="M12 2a15.3 15.3 0 010 20M12 2a15.3 15.3 0 000 20"/>
      </svg>
    ),
    color: '#f59e0b',
    title: 'PCI DSS SAQ-A Compliant',
    desc: 'Card processing is handled by Paystack/Stripe. NylePay never touches raw card numbers — fully compliant by design.',
  },
  {
    icon: (
      <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8">
        <path d="M17 21v-2a4 4 0 00-4-4H5a4 4 0 00-4 4v2"/><circle cx="9" cy="7" r="4"/><path d="M23 21v-2a4 4 0 00-3-3.87M16 3.13a4 4 0 010 7.75"/>
      </svg>
    ),
    color: '#ec4899',
    title: 'KYC / AML Screening',
    desc: 'Identity verified via Smile Identity (Kenya). AML sanctions screening via ComplyAdvantage — CBK-mandated limits enforced.',
  },
  {
    icon: (
      <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8">
        <path d="M14 2H6a2 2 0 00-2 2v16a2 2 0 002 2h12a2 2 0 002-2V8z"/><polyline points="14 2 14 8 20 8"/><line x1="16" y1="13" x2="8" y2="13"/><line x1="16" y1="17" x2="8" y2="17"/>
      </svg>
    ),
    color: '#06b6d4',
    title: 'Immutable Audit Log',
    desc: 'Every API call, login, fund movement, and webhook delivery is logged with IP, user agent, and outcome for forensic analysis.',
  },
];

const Security = () => {
  return (
    <section id="security" style={styles.section}>
      <div className="gradient-divider" />
      <div style={styles.inner}>
        <div style={styles.header} className="animate-fade-in-up">
          <div className="section-eyebrow">Trust & Security</div>
          <h2 className="section-title">
            Built for <span className="text-gradient">Bank-Grade</span> Safety
          </h2>
          <p className="section-subtitle">
            Security is not a feature — it's the foundation. Every layer of NylePay is designed to protect consumer and merchant funds.
          </p>
        </div>

        <div style={styles.grid}>
          {FEATURES.map((f, i) => (
            <div
              key={i}
              className="card-elevated animate-fade-in-up"
              style={{ ...styles.card, animationDelay: `${i * 0.08}s` }}
            >
              <div style={{ ...styles.icon, background: `${f.color}18`, border: `1px solid ${f.color}30`, color: f.color }}>
                {f.icon}
              </div>
              <h3 style={styles.cardTitle}>{f.title}</h3>
              <p style={styles.cardDesc}>{f.desc}</p>
            </div>
          ))}
        </div>

        {/* Compliance strip */}
        <div style={styles.complianceRow} className="animate-fade-in-up">
          {['CBK Regulated', 'PCI DSS SAQ-A', 'AML Screened', 'ISO 27001 Aligned', 'GDPR Ready'].map((c, i) => (
            <div key={i} style={styles.compTag}>
              <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="var(--brand-green)" strokeWidth="3">
                <path d="M20 6L9 17l-5-5"/>
              </svg>
              {c}
            </div>
          ))}
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
    background: 'linear-gradient(180deg, transparent 0%, rgba(16,185,129,0.03) 50%, transparent 100%)',
  },
  inner: {
    maxWidth: '1280px',
    margin: '0 auto',
  },
  header: {
    marginBottom: '4rem',
  },
  grid: {
    display: 'grid',
    gridTemplateColumns: 'repeat(auto-fit, minmax(280px, 1fr))',
    gap: '1.5rem',
    marginBottom: '3rem',
  },
  card: {
    padding: '1.75rem',
    transition: 'transform 0.3s ease',
  },
  icon: {
    width: '50px',
    height: '50px',
    borderRadius: '12px',
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
    marginBottom: '1.25rem',
  },
  cardTitle: {
    fontSize: '1rem',
    fontWeight: 700,
    marginBottom: '0.65rem',
    color: 'var(--text-primary)',
  },
  cardDesc: {
    color: 'var(--text-secondary)',
    fontSize: '0.875rem',
    lineHeight: 1.65,
  },
  complianceRow: {
    display: 'flex',
    gap: '1rem',
    flexWrap: 'wrap',
    justifyContent: 'center',
    paddingTop: '1rem',
  },
  compTag: {
    display: 'flex',
    alignItems: 'center',
    gap: '0.5rem',
    padding: '0.5rem 1rem',
    background: 'rgba(16,185,129,0.07)',
    border: '1px solid rgba(16,185,129,0.15)',
    borderRadius: 'var(--radius-full)',
    fontSize: '0.8rem',
    color: 'var(--text-secondary)',
    fontWeight: 500,
  },
};

export default Security;
