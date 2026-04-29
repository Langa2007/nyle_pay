import React from 'react';
import { Link } from 'react-router-dom';

const Hero = () => {
  return (
    <section style={styles.hero} className="animate-fade-in">
      <div style={styles.content}>
        <div style={styles.badge} className="glass-panel text-gradient">
          ✨ The Future of Payments in Africa
        </div>
        <h1 style={styles.title}>
          Money Without <br/>
          <span className="text-gradient">Borders</span>
        </h1>
        <p style={styles.subtitle}>
          NylePay is your ultimate financial hub. Pay merchants, send money to friends, and manage your crypto—all from a single, beautifully designed app.
        </p>
        <div style={styles.ctaGroup}>
          <Link to="/register" className="btn-primary">Open an Account</Link>
          <a href="http://localhost:5174" className="btn-secondary">I'm a Business</a>
        </div>
        
        <div style={styles.statsRow}>
          <div style={styles.statBox}>
            <h3 style={styles.statValue}>1.5%</h3>
            <p style={styles.statLabel}>Low Fees</p>
          </div>
          <div style={styles.statBox}>
            <h3 style={styles.statValue}>T+0</h3>
            <p style={styles.statLabel}>Instant Settlement</p>
          </div>
          <div style={styles.statBox}>
            <h3 style={styles.statValue}>AES-256</h3>
            <p style={styles.statLabel}>Bank Grade Security</p>
          </div>
        </div>
      </div>
      
      <div style={styles.graphics}>
        <div className="glass-panel" style={styles.mockup}>
          <div style={styles.mockupHeader}>
            <div style={styles.dot} />
            <div style={styles.dot} />
            <div style={styles.dot} />
          </div>
          <div style={styles.mockupBody}>
            <h4 style={{ color: 'var(--text-secondary)', marginBottom: '0.5rem' }}>Total Balance</h4>
            <h2 style={{ fontSize: '3rem', fontFamily: 'var(--font-heading)', color: 'var(--text-primary)' }}>KES 45,200</h2>
            <div style={{ marginTop: '2rem', display: 'flex', gap: '1rem' }}>
               <div style={{...styles.actionBtn, background: 'rgba(16, 185, 129, 0.1)', color: 'var(--brand-green)'}}>↓ Receive</div>
               <div style={{...styles.actionBtn, background: 'rgba(59, 130, 246, 0.1)', color: 'var(--brand-blue)'}}>↑ Send</div>
            </div>
          </div>
        </div>
        {/* Glow effect behind the mockup */}
        <div style={styles.glowBlob} />
      </div>
    </section>
  );
};

const styles = {
  hero: {
    minHeight: '100vh',
    display: 'flex',
    alignItems: 'center',
    padding: '8rem 5% 4rem',
    gap: '4rem',
    position: 'relative',
    overflow: 'hidden',
  },
  content: {
    flex: 1,
    maxWidth: '600px',
    zIndex: 10,
  },
  badge: {
    display: 'inline-block',
    padding: '0.5rem 1rem',
    borderRadius: '100px',
    fontSize: '0.875rem',
    fontWeight: 600,
    marginBottom: '1.5rem',
  },
  title: {
    fontSize: '5rem',
    marginBottom: '1.5rem',
    letterSpacing: '-0.02em',
  },
  subtitle: {
    fontSize: '1.25rem',
    color: 'var(--text-secondary)',
    marginBottom: '2.5rem',
    lineHeight: 1.6,
  },
  ctaGroup: {
    display: 'flex',
    gap: '1rem',
    marginBottom: '4rem',
  },
  statsRow: {
    display: 'flex',
    gap: '3rem',
    borderTop: '1px solid var(--border-color)',
    paddingTop: '2rem',
  },
  statValue: {
    fontSize: '2rem',
    color: 'var(--text-primary)',
    marginBottom: '0.25rem',
  },
  statLabel: {
    color: 'var(--text-muted)',
    fontSize: '0.875rem',
    textTransform: 'uppercase',
    letterSpacing: '0.05em',
    fontWeight: 600,
  },
  graphics: {
    flex: 1,
    position: 'relative',
    display: 'flex',
    justifyContent: 'center',
    alignItems: 'center',
    zIndex: 10,
  },
  mockup: {
    width: '100%',
    maxWidth: '400px',
    height: '500px',
    borderRadius: '24px',
    overflow: 'hidden',
    position: 'relative',
    zIndex: 2,
    border: '1px solid rgba(255, 255, 255, 0.15)',
  },
  mockupHeader: {
    height: '40px',
    borderBottom: '1px solid var(--glass-border)',
    display: 'flex',
    alignItems: 'center',
    padding: '0 1rem',
    gap: '0.5rem',
    background: 'rgba(0,0,0,0.2)',
  },
  dot: {
    width: '10px',
    height: '10px',
    borderRadius: '50%',
    background: 'var(--border-color)',
  },
  mockupBody: {
    padding: '2rem',
  },
  actionBtn: {
    flex: 1,
    padding: '1rem',
    borderRadius: '12px',
    textAlign: 'center',
    fontWeight: 600,
  },
  glowBlob: {
    position: 'absolute',
    width: '400px',
    height: '400px',
    background: 'var(--brand-gradient)',
    filter: 'blur(100px)',
    opacity: 0.2,
    borderRadius: '50%',
    zIndex: 1,
  }
};

export default Hero;
