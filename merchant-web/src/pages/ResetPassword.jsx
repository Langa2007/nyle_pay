import React, { useState } from 'react';
import { Link, useNavigate, useSearchParams } from 'react-router-dom';
import { useAuth } from '../context/useAuth';

export default function ResetPassword() {
  const [params] = useSearchParams();
  const navigate = useNavigate();
  const { resetPassword } = useAuth();
  const [password, setPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [error, setError] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const token = params.get('token') || '';

  const validate = () => {
    if (!token) return 'Reset token is missing.';
    if (password.length < 8) return 'Password must be at least 8 characters.';
    if (!/[A-Z]/.test(password) || !/[a-z]/.test(password) || !/\d/.test(password) || !/[^A-Za-z0-9]/.test(password)) {
      return 'Use uppercase, lowercase, number, and special symbol.';
    }
    if (password !== confirmPassword) return 'Passwords do not match.';
    return '';
  };

  const submit = async (event) => {
    event.preventDefault();
    setError('');
    const validation = validate();
    if (validation) {
      setError(validation);
      return;
    }
    setSubmitting(true);
    try {
      await resetPassword({ token, password });
      navigate('/', { replace: true });
    } catch (err) {
      setError(err.message || 'Unable to reset password.');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div style={P.page}>
      <form style={P.card} onSubmit={submit}>
        <Link to="/" style={P.back}>Back to sign in</Link>
        <h1 style={P.title}>Change password</h1>
        <p style={P.sub}>Set a new password for NylePay Business access.</p>

        <div className="form-group">
          <label className="form-label" htmlFor="password">New password</label>
          <input id="password" className="form-input" type="password" required value={password} onChange={(event) => setPassword(event.target.value)} autoComplete="new-password" />
          <p className="form-hint">Use at least 8 characters with uppercase, lowercase, number, and symbol.</p>
        </div>

        <div className="form-group">
          <label className="form-label" htmlFor="confirmPassword">Confirm password</label>
          <input id="confirmPassword" className="form-input" type="password" required value={confirmPassword} onChange={(event) => setConfirmPassword(event.target.value)} autoComplete="new-password" />
        </div>

        {error && <div className="alert alert-error compact-alert">{error}</div>}

        <button type="submit" className="btn-primary" style={P.submit} disabled={submitting}>
          {submitting ? 'Saving...' : 'Save new password'}
        </button>
      </form>
    </div>
  );
}

const P = {
  page: { minHeight: '100vh', background: 'var(--bg-page)', display: 'flex', alignItems: 'center', justifyContent: 'center', padding: '2rem 1rem' },
  card: { width: '100%', maxWidth: 460, background: 'var(--bg-surface)', border: '1px solid var(--border)', borderRadius: '8px', padding: '2rem', boxShadow: 'var(--shadow-lg)' },
  back: { display: 'inline-flex', marginBottom: '1.25rem', color: 'var(--text-secondary)', fontWeight: 700, fontSize: '0.84rem' },
  title: { fontSize: '1.6rem', fontWeight: 800, letterSpacing: 0, marginBottom: '0.4rem' },
  sub: { color: 'var(--text-secondary)', marginBottom: '1.5rem' },
  submit: { width: '100%', justifyContent: 'center', padding: '0.75rem' },
};
