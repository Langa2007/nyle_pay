import React, { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';

const Navbar = () => {
  const [scrolled, setScrolled] = useState(false);
  const [menuOpen, setMenuOpen] = useState(false);

  useEffect(() => {
    const handleScroll = () => setScrolled(window.scrollY > 20);
    window.addEventListener('scroll', handleScroll, { passive: true });
    return () => window.removeEventListener('scroll', handleScroll);
  }, []);

  return (
    <nav style={{
      ...styles.navbar,
      background: scrolled ? 'rgba(6, 9, 16, 0.92)' : 'rgba(6, 9, 16, 0.6)',
      borderBottom: scrolled ? '1px solid rgba(255,255,255,0.07)' : '1px solid transparent',
      boxShadow: scrolled ? '0 4px 30px rgba(0,0,0,0.4)' : 'none',
    }}>
      <div style={styles.logoWrap}>
        <div style={styles.logoIcon}>
          <svg width="22" height="22" viewBox="0 0 24 24" fill="none">
            <defs>
              <linearGradient id="npy-logo" x1="0" y1="0" x2="24" y2="24" gradientUnits="userSpaceOnUse">
                <stop offset="0%" stopColor="#3b82f6"/>
                <stop offset="100%" stopColor="#10b981"/>
              </linearGradient>
            </defs>
            <path d="M12 2L2 7l10 5 10-5-10-5zM2 17l10 5 10-5M2 12l10 5 10-5" stroke="url(#npy-logo)" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"/>
          </svg>
        </div>
        <span className="text-gradient" style={{ fontWeight: 800, fontSize: '1.4rem', fontFamily: 'var(--font-heading)' }}>
          NylePay
        </span>
      </div>

      <div style={{ ...styles.links, display: menuOpen ? 'flex' : undefined }}>
        <a href="#ecosystem" style={styles.link} onClick={() => setMenuOpen(false)}>Ecosystem</a>
        <a href="#for-merchants" style={styles.link} onClick={() => setMenuOpen(false)}>For Merchants</a>
        <a href="#sandbox" style={styles.link} onClick={() => setMenuOpen(false)}>Sandbox</a>
        <a href="#security" style={styles.link} onClick={() => setMenuOpen(false)}>Security</a>
      </div>

      <div style={styles.actions}>
        <Link to="/login" style={styles.signInLink}>Sign In</Link>
        <Link to="/register" className="btn-primary" style={{ padding: '0.5rem 1.25rem', fontSize: '0.9rem' }}>
          Get Started
        </Link>
        <button
          style={styles.hamburger}
          onClick={() => setMenuOpen(o => !o)}
          aria-label="Toggle menu"
        >
          <span style={{ ...styles.bar, transform: menuOpen ? 'rotate(45deg) translate(5px, 5px)' : 'none' }} />
          <span style={{ ...styles.bar, opacity: menuOpen ? 0 : 1 }} />
          <span style={{ ...styles.bar, transform: menuOpen ? 'rotate(-45deg) translate(5px, -5px)' : 'none' }} />
        </button>
      </div>
    </nav>
  );
};

const styles = {
  navbar: {
    display: 'flex',
    justifyContent: 'space-between',
    alignItems: 'center',
    padding: '1rem 5%',
    position: 'fixed',
    top: 0, left: 0, right: 0,
    zIndex: 1000,
    backdropFilter: 'blur(20px)',
    WebkitBackdropFilter: 'blur(20px)',
    transition: 'all 0.3s ease',
  },
  logoWrap: {
    display: 'flex',
    alignItems: 'center',
    gap: '0.5rem',
    textDecoration: 'none',
  },
  logoIcon: {
    width: '34px',
    height: '34px',
    background: 'rgba(59,130,246,0.15)',
    border: '1px solid rgba(59,130,246,0.3)',
    borderRadius: '8px',
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
  },
  links: {
    display: 'flex',
    gap: '2.25rem',
    alignItems: 'center',
    '@media(max-width:768px)': { display: 'none' },
  },
  link: {
    color: 'var(--text-secondary)',
    fontWeight: 500,
    fontSize: '0.9rem',
    transition: 'color 0.25s',
    cursor: 'pointer',
  },
  actions: {
    display: 'flex',
    alignItems: 'center',
    gap: '1rem',
  },
  signInLink: {
    color: 'var(--text-secondary)',
    fontWeight: 500,
    fontSize: '0.9rem',
    transition: 'color 0.25s',
  },
  hamburger: {
    display: 'none',
    flexDirection: 'column',
    gap: '5px',
    background: 'none',
    border: 'none',
    cursor: 'pointer',
    padding: '4px',
  },
  bar: {
    display: 'block',
    width: '22px',
    height: '2px',
    background: 'var(--text-primary)',
    borderRadius: '2px',
    transition: 'all 0.3s ease',
  },
};

export default Navbar;
