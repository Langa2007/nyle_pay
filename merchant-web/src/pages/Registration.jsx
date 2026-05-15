import React from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/useAuth';

export default function Registration() {
  const navigate = useNavigate();
  const { user } = useAuth();

  return (
    <div style={P.page}>
      <div style={P.card}>
        <div style={P.badge}>Sandbox workspace</div>
        <h1 style={P.title}>No business profile is needed for sandbox</h1>
        <p style={P.sub}>
          Your verified operator account can test NylePay routes, generate sandbox API keys,
          and inspect API docs before Go Live. Registered business details are collected only
          when you request production activation.
        </p>

        <div style={P.identity}>
          <span>Signed in as</span>
          <strong>{user?.email || 'verified operator'}</strong>
        </div>

        <div style={P.actions}>
          <button className="btn-primary" onClick={() => navigate('/dashboard')}>Open dashboard</button>
          <Link className="btn-outline" to="/docs">View API docs</Link>
        </div>
      </div>
    </div>
  );
}

const P = {
  page: { minHeight: '100vh', background: 'var(--bg-page)', display: 'flex', alignItems: 'center', justifyContent: 'center', padding: '2rem 1rem' },
  card: { width: '100%', maxWidth: 620, background: 'var(--bg-surface)', border: '1px solid var(--border)', borderRadius: '8px', padding: '2.25rem', boxShadow: 'var(--shadow-lg)' },
  badge: { display: 'inline-flex', marginBottom: '1.25rem', fontSize: '0.72rem', fontWeight: 800, color: 'var(--brand)', background: 'var(--brand-xlight)', padding: '0.25rem 0.7rem', borderRadius: '999px', textTransform: 'uppercase', letterSpacing: '0.06em' },
  title: { fontSize: '1.8rem', fontWeight: 800, letterSpacing: 0, marginBottom: '0.75rem' },
  sub: { color: 'var(--text-secondary)', lineHeight: 1.75, marginBottom: '1.5rem' },
  identity: { padding: '0.9rem 1rem', border: '1px solid var(--border)', borderRadius: 8, background: 'var(--bg-page)', display: 'grid', gap: '0.2rem', marginBottom: '1.5rem' },
  actions: { display: 'flex', gap: '0.75rem', flexWrap: 'wrap' },
};
