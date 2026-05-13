import React from 'react';
import { Link } from 'react-router-dom';

const Hero = () => {
  return (
    <section style={styles.hero}>
      {/* Background glows */}
      <div style={styles.glowTop} />
      <div style={styles.glowBottom} />

      <div style={styles.inner}>
        {/* Left content */}
        <div style={styles.content}>
          <div className="animate-fade-in-up" style={{ marginBottom: '1.5rem' }}>
            <span className="pill pill-blue">
              <span style={{ width: '6px', height: '6px', borderRadius: '50%', background: 'var(--brand-blue-light)', display: 'inline-block' }} />
              Your Unified Financial Identity
            </span>
          </div>

          <h1 className="animate-fade-in-up delay-100" style={styles.title}>
            Money Without
            <br />
            <span className="text-gradient">Borders.</span>
          </h1>

          <p className="animate-fade-in-up delay-200" style={styles.subtitle}>
            Unlock your unique NPY account. Fund via M-Pesa, bank transfer, or crypto.
            Pay any NylePay merchant instantly — <strong style={{ color: 'var(--text-primary)' }}>zero hidden fees.</strong>
          </p>

          <div className="animate-fade-in-up delay-300" style={styles.ctaGroup}>
            <Link to="/register" className="btn-primary">
              <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5">
                <path d="M20 21v-2a4 4 0 00-4-4H8a4 4 0 00-4 4v2"/>
                <circle cx="12" cy="7" r="4"/>
              </svg>
              Create NPY Account
            </Link>
            <a href="http://localhost:5174" className="btn-secondary" target="_blank" rel="noreferrer">
              Merchant Portal
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                <path d="M7 17L17 7M17 7H7M17 7v10"/>
              </svg>
            </a>
          </div>

          <div className="animate-fade-in-up delay-400" style={styles.statsRow}>
            <div style={styles.statBox}>
              <span style={styles.statValue} className="text-gradient">1.5%</span>
              <span style={styles.statLabel}>Low Fees</span>
            </div>
            <div style={styles.statDivider} />
            <div style={styles.statBox}>
              <span style={styles.statValue} className="text-gradient">T+0</span>
              <span style={styles.statLabel}>Instant Settlement</span>
            </div>
            <div style={styles.statDivider} />
            <div style={styles.statBox}>
              <span style={styles.statValue} className="text-gradient">AES-256</span>
              <span style={styles.statLabel}>Bank Grade Security</span>
            </div>
          </div>
        </div>

        {/* Right — wallet mockup */}
        <div className="animate-fade-in-right delay-200" style={styles.graphics}>
          <div style={styles.glowOrb} />
          <div className="glass-panel float-anim" style={styles.walletCard}>
            {/* Card top bar */}
            <div style={styles.cardTopBar}>
              <div style={styles.cardDots}>
                {['#ff5f57','#febc2e','#28c840'].map((c,i) => (
                  <div key={i} style={{ width: 10, height: 10, borderRadius: '50%', background: c }} />
                ))}
              </div>
              <span style={{ fontSize: '0.7rem', color: 'var(--text-muted)', fontFamily: 'var(--font-mono)' }}>nylepay.com</span>
            </div>

            {/* Card body */}
            <div style={styles.cardBody}>
              <div style={{ marginBottom: '1.5rem' }}>
                <div style={styles.cardLabel}>Account Number</div>
                <div style={styles.cardMono}>NPY • AZJ7 • 890X</div>
              </div>

              <div style={styles.balanceSection}>
                <div style={styles.cardLabel}>Available Balance</div>
                <div style={styles.balanceAmount}>KES 45,200</div>
                <div style={styles.balanceSub}>≈ $348.50 USD</div>
              </div>

              <div style={styles.actionRow}>
                <div style={{ ...styles.actionBtn, background: 'rgba(16,185,129,0.12)', border: '1px solid rgba(16,185,129,0.2)', color: 'var(--brand-green-light)' }}>
                  <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5">
                    <path d="M12 5v14M5 12l7-7 7 7"/>
                  </svg>
                  Fund
                </div>
                <div style={{ ...styles.actionBtn, background: 'rgba(59,130,246,0.12)', border: '1px solid rgba(59,130,246,0.2)', color: 'var(--brand-blue-light)' }}>
                  <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5">
                    <path d="M5 12h14M12 5l7 7-7 7"/>
                  </svg>
                  Send
                </div>
                <div style={{ ...styles.actionBtn, background: 'rgba(139,92,246,0.12)', border: '1px solid rgba(139,92,246,0.2)', color: 'var(--brand-purple)' }}>
                  <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5">
                    <rect x="2" y="5" width="20" height="14" rx="2"/><path d="M2 10h20"/>
                  </svg>
                  Pay
                </div>
              </div>

              {/* Mini transactions */}
              <div style={styles.txList}>
                {[
                  { icon: '🛒', label: 'Jumia Payment', amount: '-KES 2,300', color: '#f87171' },
                  { icon: '📲', label: 'M-Pesa Top-up', amount: '+KES 5,000', color: '#34d399' },
                  { icon: '🏪', label: 'Merchant Refund', amount: '+KES 450', color: '#34d399' },
                ].map((tx, i) => (
                  <div key={i} style={styles.txRow}>
                    <span style={styles.txIcon}>{tx.icon}</span>
                    <span style={styles.txLabel}>{tx.label}</span>
                    <span style={{ ...styles.txAmount, color: tx.color }}>{tx.amount}</span>
                  </div>
                ))}
              </div>
            </div>
          </div>

          {/* Floating mini badge */}
          <div style={styles.floatingBadge} className="animate-fade-in-up delay-500">
            <div style={{ width: 8, height: 8, borderRadius: '50%', background: '#10b981', animation: 'pulse-glow 2s infinite' }} />
            <span style={{ fontSize: '0.75rem', fontWeight: 600 }}>Live settlement active</span>
          </div>
        </div>
      </div>
    </section>
  );
};

const styles = {
  hero: {
    minHeight: '100vh',
    display: 'flex',
    alignItems: 'center',
    padding: '8rem 5% 5rem',
    position: 'relative',
    overflow: 'hidden',
  },
  glowTop: {
    position: 'absolute',
    top: '-10%',
    left: '10%',
    width: '600px',
    height: '600px',
    background: 'radial-gradient(circle, rgba(59,130,246,0.12) 0%, transparent 70%)',
    borderRadius: '50%',
    pointerEvents: 'none',
  },
  glowBottom: {
    position: 'absolute',
    bottom: '-10%',
    right: '5%',
    width: '500px',
    height: '500px',
    background: 'radial-gradient(circle, rgba(16,185,129,0.1) 0%, transparent 70%)',
    borderRadius: '50%',
    pointerEvents: 'none',
  },
  inner: {
    maxWidth: '1280px',
    margin: '0 auto',
    width: '100%',
    display: 'flex',
    alignItems: 'center',
    gap: '5rem',
    position: 'relative',
    zIndex: 10,
  },
  content: {
    flex: '1 1 50%',
    maxWidth: '580px',
  },
  title: {
    fontSize: 'clamp(3rem, 5.5vw, 5.5rem)',
    fontWeight: 900,
    letterSpacing: '-0.03em',
    marginBottom: '1.5rem',
    lineHeight: 1.05,
  },
  subtitle: {
    fontSize: '1.15rem',
    color: 'var(--text-secondary)',
    marginBottom: '2.5rem',
    lineHeight: 1.7,
  },
  ctaGroup: {
    display: 'flex',
    gap: '1rem',
    flexWrap: 'wrap',
    marginBottom: '4rem',
  },
  statsRow: {
    display: 'flex',
    alignItems: 'center',
    gap: '2.5rem',
    paddingTop: '2rem',
    borderTop: '1px solid var(--border-color)',
    flexWrap: 'wrap',
  },
  statDivider: {
    width: '1px',
    height: '40px',
    background: 'var(--border-color)',
  },
  statBox: {
    display: 'flex',
    flexDirection: 'column',
    gap: '0.2rem',
  },
  statValue: {
    fontSize: '1.75rem',
    fontFamily: 'var(--font-heading)',
    fontWeight: 800,
    lineHeight: 1,
  },
  statLabel: {
    color: 'var(--text-muted)',
    fontSize: '0.75rem',
    textTransform: 'uppercase',
    letterSpacing: '0.07em',
    fontWeight: 600,
  },
  graphics: {
    flex: '1 1 45%',
    position: 'relative',
    display: 'flex',
    justifyContent: 'center',
    alignItems: 'center',
  },
  glowOrb: {
    position: 'absolute',
    width: '380px',
    height: '380px',
    background: 'radial-gradient(circle, rgba(59,130,246,0.18) 0%, transparent 70%)',
    borderRadius: '50%',
    zIndex: 1,
  },
  walletCard: {
    width: '100%',
    maxWidth: '380px',
    borderRadius: '20px',
    overflow: 'hidden',
    position: 'relative',
    zIndex: 2,
    border: '1px solid rgba(255,255,255,0.1)',
  },
  cardTopBar: {
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'space-between',
    padding: '0.75rem 1rem',
    background: 'rgba(0,0,0,0.3)',
    borderBottom: '1px solid var(--glass-border)',
  },
  cardDots: {
    display: 'flex',
    gap: '6px',
  },
  cardBody: {
    padding: '1.75rem',
  },
  cardLabel: {
    fontSize: '0.72rem',
    color: 'var(--text-muted)',
    textTransform: 'uppercase',
    letterSpacing: '0.08em',
    fontWeight: 600,
    marginBottom: '0.35rem',
  },
  cardMono: {
    fontFamily: 'var(--font-mono)',
    fontSize: '0.95rem',
    color: 'var(--text-secondary)',
    letterSpacing: '0.05em',
  },
  balanceSection: {
    background: 'rgba(59,130,246,0.06)',
    border: '1px solid rgba(59,130,246,0.12)',
    borderRadius: '12px',
    padding: '1.25rem',
    marginBottom: '1.25rem',
  },
  balanceAmount: {
    fontSize: '2.25rem',
    fontFamily: 'var(--font-heading)',
    fontWeight: 800,
    color: 'var(--text-primary)',
    lineHeight: 1.2,
    marginBottom: '0.25rem',
  },
  balanceSub: {
    fontSize: '0.8rem',
    color: 'var(--text-muted)',
  },
  actionRow: {
    display: 'flex',
    gap: '0.75rem',
    marginBottom: '1.5rem',
  },
  actionBtn: {
    flex: 1,
    padding: '0.65rem 0.5rem',
    borderRadius: '10px',
    textAlign: 'center',
    fontWeight: 600,
    fontSize: '0.8rem',
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
    gap: '0.3rem',
    cursor: 'pointer',
    transition: 'all 0.2s',
  },
  txList: {
    display: 'flex',
    flexDirection: 'column',
    gap: '0.75rem',
    borderTop: '1px solid var(--glass-border)',
    paddingTop: '1rem',
  },
  txRow: {
    display: 'flex',
    alignItems: 'center',
    gap: '0.75rem',
  },
  txIcon: {
    fontSize: '1.1rem',
    width: '30px',
    textAlign: 'center',
  },
  txLabel: {
    flex: 1,
    fontSize: '0.82rem',
    color: 'var(--text-secondary)',
  },
  txAmount: {
    fontSize: '0.82rem',
    fontWeight: 600,
    fontFamily: 'var(--font-mono)',
  },
  floatingBadge: {
    position: 'absolute',
    bottom: '-1rem',
    left: '5%',
    display: 'flex',
    alignItems: 'center',
    gap: '0.6rem',
    background: 'rgba(16,185,129,0.1)',
    border: '1px solid rgba(16,185,129,0.3)',
    borderRadius: 'var(--radius-full)',
    padding: '0.5rem 1rem',
    zIndex: 5,
    backdropFilter: 'blur(12px)',
    color: 'var(--brand-green-light)',
  },
};

export default Hero;
