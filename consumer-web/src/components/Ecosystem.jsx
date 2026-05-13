import React from 'react';

const STEPS = [
  {
    num: '01',
    title: 'Create Your NylePay Account',
    desc: 'Sign up as a consumer with your email and M-Pesa number. Complete a fast automated KYC check to unlock your unique 11-digit NPY account.',
    icon: (
      <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
        <path d="M20 21v-2a4 4 0 00-4-4H8a4 4 0 00-4 4v2"/><circle cx="12" cy="7" r="4"/>
      </svg>
    ),
    tag: 'Identity',
    tagClass: 'pill-blue',
  },
  {
    num: '02',
    title: 'Fund Your Wallet',
    desc: 'Top up instantly via M-Pesa (Paybill/USSD), direct bank transfer from any Kenyan bank, or supported crypto networks (ETH, MATIC, USDC, USDT).',
    icon: (
      <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
        <rect x="2" y="5" width="20" height="14" rx="2"/><path d="M2 10h20"/>
      </svg>
    ),
    tag: 'Funding',
    tagClass: 'pill-green',
  },
  {
    num: '03',
    title: 'Pay Verified Merchants',
    desc: 'Pay any NylePay-verified business by entering their Merchant ID or scanning their QR code. Real-time settlement, T+0.',
    icon: (
      <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
        <path d="M9 12l2 2 4-4"/><path d="M21 12c0 4.97-4.03 9-9 9S3 16.97 3 12 7.03 3 12 3s9 4.03 9 9z"/>
      </svg>
    ),
    tag: 'Payments',
    tagClass: 'pill-purple',
  },
];

const Ecosystem = () => {
  return (
    <section id="ecosystem" style={styles.section}>
      <div className="gradient-divider" />
      <div style={styles.inner}>
        <div style={styles.header} className="animate-fade-in-up">
          <div className="section-eyebrow">How It Works</div>
          <h2 className="section-title">
            The <span className="text-gradient">NylePay</span> Ecosystem
          </h2>
          <p className="section-subtitle">
            A unified identity layer for Africa's payment ecosystem — one account, every rail.
          </p>
        </div>

        <div style={styles.grid}>
          {STEPS.map((s, i) => (
            <div key={i} className="card-elevated animate-fade-in-up" style={{ ...styles.card, animationDelay: `${i * 0.1}s` }}>
              <div style={styles.cardHeader}>
                <div style={styles.iconBox}>{s.icon}</div>
                <span className={`pill ${s.tagClass}`}>{s.tag}</span>
              </div>
              <div style={styles.stepNum}>{s.num}</div>
              <h3 style={styles.cardTitle}>{s.title}</h3>
              <p style={styles.cardDesc}>{s.desc}</p>
            </div>
          ))}
        </div>

        {/* Rails bar */}
        <div style={styles.railsBar} className="animate-fade-in-up">
          <div style={styles.railsLabel}>Supported payment rails</div>
          <div style={styles.rails}>
            {['M-Pesa', 'Bank Transfer', 'Ethereum', 'Polygon', 'USDC', 'USDT', 'Visa/MC'].map((r, i) => (
              <span key={i} style={styles.rail}>{r}</span>
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
    textAlign: 'center',
    marginBottom: '4rem',
  },
  grid: {
    display: 'grid',
    gridTemplateColumns: 'repeat(auto-fit, minmax(300px, 1fr))',
    gap: '1.5rem',
    marginBottom: '3rem',
  },
  card: {
    padding: '2.25rem',
    transition: 'transform 0.3s ease, box-shadow 0.3s ease',
    cursor: 'default',
    position: 'relative',
    overflow: 'hidden',
  },
  cardHeader: {
    display: 'flex',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: '1.25rem',
  },
  iconBox: {
    width: '48px',
    height: '48px',
    background: 'rgba(59,130,246,0.1)',
    border: '1px solid rgba(59,130,246,0.2)',
    borderRadius: '12px',
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
    color: 'var(--brand-blue-light)',
  },
  stepNum: {
    fontSize: '4rem',
    fontFamily: 'var(--font-heading)',
    fontWeight: 900,
    color: 'rgba(59,130,246,0.07)',
    lineHeight: 1,
    marginBottom: '-1rem',
    letterSpacing: '-0.02em',
  },
  cardTitle: {
    fontSize: '1.2rem',
    fontWeight: 700,
    marginBottom: '0.75rem',
    color: 'var(--text-primary)',
  },
  cardDesc: {
    color: 'var(--text-secondary)',
    fontSize: '0.95rem',
    lineHeight: 1.65,
  },
  railsBar: {
    background: 'rgba(20,31,51,0.6)',
    border: '1px solid var(--border-color)',
    borderRadius: 'var(--radius-lg)',
    padding: '1.25rem 2rem',
    display: 'flex',
    alignItems: 'center',
    gap: '2rem',
    flexWrap: 'wrap',
  },
  railsLabel: {
    fontSize: '0.75rem',
    fontWeight: 700,
    letterSpacing: '0.08em',
    textTransform: 'uppercase',
    color: 'var(--text-muted)',
    whiteSpace: 'nowrap',
  },
  rails: {
    display: 'flex',
    gap: '0.6rem',
    flexWrap: 'wrap',
  },
  rail: {
    padding: '0.3rem 0.85rem',
    background: 'rgba(59,130,246,0.08)',
    border: '1px solid rgba(59,130,246,0.15)',
    borderRadius: 'var(--radius-full)',
    fontSize: '0.8rem',
    color: 'var(--text-secondary)',
    fontWeight: 500,
  },
};

export default Ecosystem;
