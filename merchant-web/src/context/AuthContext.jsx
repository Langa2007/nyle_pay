import React, { useState, useEffect } from 'react';
import { AuthContext } from './authContextValue';

const API = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080';

export function AuthProvider({ children }) {
  const [user, setUser]     = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const stored = localStorage.getItem('npy_business_session');
    if (stored) {
      try { setUser(JSON.parse(stored)); } catch { /* corrupt */ }
    }
    setLoading(false);
  }, []);

  /**
   * Sign in with email + password against the real NylePay backend.
   * On success stores the JWT and user profile in localStorage.
   */
  const login = async (email, password) => {
    const res = await fetch(`${API}/api/auth/login`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ email, password }),
    });
    const json = await res.json();
    if (!res.ok || !json.success) {
      throw new Error(json.message || 'Invalid credentials');
    }
    const { token, userId, email: userEmail, fullName, accountNumber } = json.data;
    const session = {
      token,
      userId,
      email: userEmail,
      fullName,
      accountNumber,
      businessId: null,
      businessName: null,
      apiKeys: null,
    };
    localStorage.setItem('npy_business_session', JSON.stringify(session));
    setUser(session);
    return session;
  };

  /**
   * Create a new NylePay user account required before business onboarding.
   * mpesaNumber and countryCode are required by the backend.
   */
  const signup = async (email, password, fullName, mpesaNumber = '', countryCode = 'KE') => {
    const res = await fetch(`${API}/api/auth/register`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ email, password, fullName, mpesaNumber, countryCode }),
    });
    const json = await res.json();
    if (!res.ok || !json.success) {
      throw new Error(json.message || 'Registration failed');
    }
    // Auto-login after successful registration
    return login(email, password);
  };

  const updateBusinessInfo = (info) => {
    const updated = { ...user, ...info };
    localStorage.setItem('npy_business_session', JSON.stringify(updated));
    setUser(updated);
  };

  const logout = () => {
    localStorage.removeItem('npy_business_session');
    setUser(null);
  };

  return (
    <AuthContext.Provider value={{ user, loading, login, signup, logout, updateBusinessInfo }}>
      {children}
    </AuthContext.Provider>
  );
}
