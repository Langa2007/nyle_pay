import React from 'react';
import { Link } from 'react-router-dom';

const Navbar = () => {
  return (
    <nav style={styles.navbar}>
      <div style={styles.logo}>
        <span className="text-gradient" style={{ fontWeight: 800, fontSize: '1.5rem', fontFamily: 'var(--font-heading)' }}>
          NylePay
        </span>
      </div>
      <div style={styles.links}>
        <a href="#ecosystem" style={styles.link}>Ecosystem</a>
        <a href="#identity" style={styles.link}>Identity</a>
        <a href="#security" style={styles.link}>Security</a>
      </div>
      <div style={styles.actions}>
        <Link to="/login" className="btn-secondary" style={{ padding: '0.5rem 1.5rem', fontSize: '1rem', border: 'none' }}>Sign In</Link>
        <Link to="/register" className="btn-primary" style={{ padding: '0.5rem 1.5rem', fontSize: '1rem', marginLeft: '1rem' }}>Create Account</Link>
      </div>
    </nav>
  );
};

const styles = {
  navbar: {
    display: 'flex',
    justifyContent: 'space-between',
    alignItems: 'center',
    padding: '1.25rem 5%',
    position: 'fixed',
    top: 0,
    left: 0,
    right: 0,
    zIndex: 100,
    background: 'rgba(10, 14, 23, 0.85)',
    backdropFilter: 'blur(12px)',
    WebkitBackdropFilter: 'blur(12px)',
    borderBottom: 'none',
  },
  links: {
    display: 'flex',
    gap: '2rem',
  },
  link: {
    color: 'var(--text-primary)',
    fontWeight: 500,
    fontSize: '1rem',
  },
  actions: {
    display: 'flex',
    alignItems: 'center',
  }
};

export default Navbar;
