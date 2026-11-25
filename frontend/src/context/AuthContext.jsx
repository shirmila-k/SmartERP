import React, { createContext, useContext, useState, useEffect } from 'react';

// Seeded users from benchmark
export const SEEDED_USERS = [
  { id: 1, username: 'salesman_a', fullName: 'Salesman A', role: 'SALESMAN', roleLabel: 'Field Salesman' },
  { id: 2, username: 'salesman_b', fullName: 'Salesman B', role: 'SALESMAN', roleLabel: 'Field Salesman' },
  { id: 3, username: 'admin', fullName: 'Factory Admin', role: 'ADMIN', roleLabel: 'Factory Admin Desk' },
  { id: 4, username: 'prod_sup', fullName: 'Production Supervisor', role: 'PRODUCTION', roleLabel: 'Production Floor' },
  { id: 5, username: 'auditor', fullName: 'External Auditor', role: 'AUDITOR', roleLabel: 'Accountant / Auditor' },
  { id: 6, username: 'owner', fullName: 'Factory Owner', role: 'OWNER', roleLabel: 'Owner Executive' },
];

const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const [currentUser, setCurrentUser] = useState(() => {
    const saved = localStorage.getItem('erp_user');
    if (saved) {
      try {
        return JSON.parse(saved);
      } catch (e) {}
    }
    return SEEDED_USERS[2]; // Default to Factory Admin for demo
  });

  const loginAs = (userIdOrUsername) => {
    const found = SEEDED_USERS.find(
      (u) => u.id === Number(userIdOrUsername) || u.username === userIdOrUsername
    );
    if (found) {
      setCurrentUser(found);
      localStorage.setItem('erp_user', JSON.stringify(found));
    }
  };

  const logout = () => {
    setCurrentUser(null);
    localStorage.removeItem('erp_user');
  };

  return (
    <AuthContext.Provider value={{ currentUser, loginAs, logout, allUsers: SEEDED_USERS }}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
}
