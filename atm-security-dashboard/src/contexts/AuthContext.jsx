import React, { createContext, useState, useContext, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import toast from 'react-hot-toast';

const AuthContext = createContext();

export const useAuth = () => useContext(AuthContext);

export const AuthProvider = ({ children }) => {
  const [user, setUser] = useState(null);
  const [loading, setLoading] = useState(true);
  // useNavigate එක මෙතනදී call නොකරන්න - Router context එක තියෙනවද කියලා check කරන්න

  useEffect(() => {
    const token = localStorage.getItem('authToken');
    const userData = localStorage.getItem('user');
    if (token && userData) {
      setUser(JSON.parse(userData));
    }
    setLoading(false);
  }, []);

  // Login function - useNavigate එක පිටතින් call කරන්න
  const login = async (username, password, navigate) => {
    try {
      // TODO: Connect to real backend
      // For now, use mock data
      const mockUser = {
        id: 1,
        username,
        fullName: 'Admin User',
        role: 'ADMIN',
        bank: { id: 1, name: 'National Bank' }
      };
      
      localStorage.setItem('authToken', 'mock-jwt-token');
      localStorage.setItem('user', JSON.stringify(mockUser));
      setUser(mockUser);
      toast.success('Login successful!');
      if (navigate) {
        navigate('/dashboard');
      }
      return true;
    } catch (error) {
      toast.error('Login failed. Please try again.');
      return false;
    }
  };

  const logout = (navigate) => {
    localStorage.removeItem('authToken');
    localStorage.removeItem('user');
    setUser(null);
    toast.success('Logged out successfully');
    if (navigate) {
      navigate('/login');
    }
  };

  const value = {
    user,
    loading,
    login,
    logout,
    isAuthenticated: !!user,
  };

  return (
    <AuthContext.Provider value={value}>
      {children}
    </AuthContext.Provider>
  );
};