import React, { createContext, useContext, useState, useEffect } from 'react';

const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const [user, setUser] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    // Check localStorage for existing session on mount
    const stored = localStorage.getItem('npy_merchant_session');
    if (stored) {
      try {
        setUser(JSON.parse(stored));
      } catch { /* corrupt data, ignore */ }
    }
    setLoading(false);
  }, []);

  const login = (email, password) => {
    return new Promise((resolve, reject) => {
      setTimeout(() => {
        if (!email || !password) {
          reject(new Error('Email and password are required'));
          return;
        }
        const session = {
          email,
          token: 'jwt_mock_' + Date.now(),
          merchantId: null,
          businessName: null,
          apiKeys: null,
        };
        localStorage.setItem('npy_merchant_session', JSON.stringify(session));
        setUser(session);
        resolve(session);
      }, 800);
    });
  };

  const signup = (email, password, fullName) => {
    return new Promise((resolve, reject) => {
      setTimeout(() => {
        if (!email || !password || !fullName) {
          reject(new Error('All fields are required'));
          return;
        }
        const session = {
          email,
          fullName,
          token: 'jwt_mock_' + Date.now(),
          merchantId: null,
          businessName: null,
          apiKeys: null,
        };
        localStorage.setItem('npy_merchant_session', JSON.stringify(session));
        setUser(session);
        resolve(session);
      }, 800);
    });
  };

  const updateMerchantInfo = (info) => {
    const updated = { ...user, ...info };
    localStorage.setItem('npy_merchant_session', JSON.stringify(updated));
    setUser(updated);
  };

  const logout = () => {
    localStorage.removeItem('npy_merchant_session');
    setUser(null);
  };

  return (
    <AuthContext.Provider value={{ user, loading, login, signup, logout, updateMerchantInfo }}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth must be used inside AuthProvider');
  return ctx;
}
