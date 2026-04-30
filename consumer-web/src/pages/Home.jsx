import React from 'react';
import Hero from '../components/Hero';

const Home = () => {
  return (
    <div>
      <Hero />
      
      {/* How it Works Section */}
      <section id="ecosystem" style={styles.section}>
        <h2 style={styles.sectionTitle}>The NylePay Ecosystem</h2>
        <div style={styles.grid}>
          <div className="glass-panel" style={styles.card}>
            <div style={styles.stepIndicator}>01</div>
            <h3>Identity Verification</h3>
            <p style={{color: 'var(--text-secondary)', marginTop: '0.75rem', lineHeight: 1.6}}>Complete a fast, automated KYC process to unlock your secure NylePay wallet and unique 11-digit NPY account number.</p>
          </div>
          <div className="glass-panel" style={styles.card}>
            <div style={styles.stepIndicator}>02</div>
            <h3>Unified Funding</h3>
            <p style={{color: 'var(--text-secondary)', marginTop: '0.75rem', lineHeight: 1.6}}>Instantly top up your balance from multiple sources, including M-Pesa, standard Bank Transfers, or supported blockchain networks.</p>
          </div>
          <div className="glass-panel" style={styles.card}>
            <div style={styles.stepIndicator}>03</div>
            <h3>Instant Settlement</h3>
            <p style={{color: 'var(--text-secondary)', marginTop: '0.75rem', lineHeight: 1.6}}>Execute real-time transfers to peers or pay verified merchants seamlessly without any hidden transaction fees.</p>
          </div>
        </div>
      </section>
    </div>
  );
};

const styles = {
  section: {
    padding: '6rem 5%',
    position: 'relative',
    zIndex: 10,
  },
  sectionTitle: {
    fontSize: '3rem',
    textAlign: 'center',
    marginBottom: '4rem',
  },
  grid: {
    display: 'grid',
    gridTemplateColumns: 'repeat(auto-fit, minmax(300px, 1fr))',
    gap: '2rem',
  },
  card: {
    padding: '2.5rem',
    textAlign: 'center',
    transition: 'transform 0.3s ease',
    cursor: 'pointer',
  },
  stepIndicator: {
    color: 'var(--brand-blue)',
    fontSize: '0.875rem',
    fontWeight: 700,
    letterSpacing: '0.1em',
    marginBottom: '1rem',
    textTransform: 'uppercase',
  }
};

export default Home;
