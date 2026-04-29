import React from 'react';
import Hero from '../components/Hero';

const Home = () => {
  return (
    <div>
      <Hero />
      
      {/* How it Works Section */}
      <section id="how-it-works" style={styles.section}>
        <h2 style={styles.sectionTitle}>How NylePay Works</h2>
        <div style={styles.grid}>
          <div className="glass-panel" style={styles.card}>
            <div style={styles.iconBox}>1</div>
            <h3>Create Account</h3>
            <p style={{color: 'var(--text-secondary)', marginTop: '0.5rem'}}>Sign up in seconds and complete your fast KYC verification to get your unique NPY account number.</p>
          </div>
          <div className="glass-panel" style={styles.card}>
            <div style={styles.iconBox}>2</div>
            <h3>Fund Your Wallet</h3>
            <p style={{color: 'var(--text-secondary)', marginTop: '0.5rem'}}>Instantly top up via M-Pesa, Bank Transfer, or supported Crypto networks.</p>
          </div>
          <div className="glass-panel" style={styles.card}>
            <div style={styles.iconBox}>3</div>
            <h3>Pay & Transfer</h3>
            <p style={{color: 'var(--text-secondary)', marginTop: '0.5rem'}}>Pay merchants at checkout or send money to friends instantly with zero hidden fees.</p>
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
  iconBox: {
    width: '60px',
    height: '60px',
    borderRadius: '50%',
    background: 'var(--brand-gradient)',
    color: 'white',
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
    fontSize: '1.5rem',
    fontWeight: 'bold',
    margin: '0 auto 1.5rem',
    fontFamily: 'var(--font-heading)',
  }
};

export default Home;
