// src/App.jsx

import { useState, useEffect } from 'react';
import Dashboard from './pages/Dashboard';
import Login from './pages/Login';
import Register from './pages/Register';
import Notifications from './pages/Notifications';
import './index.css';
import { checkServerHealth } from './services/api';

function App() {
  const [user, setUser] = useState(() => {
    const savedUser = localStorage.getItem('user');
    return savedUser ? JSON.parse(savedUser) : null;
  });

  const [showRegister, setShowRegister] = useState(false);
  const [registerUnlocked, setRegisterUnlocked] = useState(false);
  const [serverOnline, setServerOnline] = useState(true);
  const [showNotifications, setShowNotifications] = useState(false);

  // ===== CHECK SERVER ON APP LOAD =====
  useEffect(() => {
    const checkServer = async () => {
      const isOnline = await checkServerHealth();
      setServerOnline(isOnline);
      if (!isOnline && user) {
        console.warn('⚠️ Server is offline. Some features may not work.');
      }
    };
    checkServer();
    
    // Check every 30 seconds
    const interval = setInterval(checkServer, 30000);
    return () => clearInterval(interval);
  }, [user]);

  const handleLoginSuccess = (userData) => {
    localStorage.setItem('user', JSON.stringify(userData));
    setUser(userData);
    setShowNotifications(false);
  };

  const handleLogout = () => {
    localStorage.removeItem('user');
    setUser(null);
    setShowNotifications(false);
  };

  const handleShowRegister = () => {
    setShowRegister(true);
  };

  const handleRegisterUnlocked = () => {
    setRegisterUnlocked(true);
    setShowRegister(true);
  };

  const handleRegisterClose = () => {
    setShowRegister(false);
    setRegisterUnlocked(false);
  };

  const handleNotificationClick = () => {
    setShowNotifications(true);
  };

  const handleBackFromNotifications = () => {
    setShowNotifications(false);
  };

  // ===== IF SHOW NOTIFICATIONS =====
  if (user && showNotifications) {
    return (
      <Notifications 
        user={user} 
        onBack={handleBackFromNotifications} 
      />
    );
  }

  // ===== IF LOGGED IN =====
  if (user) {
    return (
      <Dashboard 
        user={user} 
        onLogout={handleLogout} 
        onNotificationClick={handleNotificationClick}
      />
    );
  }

  // ===== IF REGISTER =====
  if (showRegister) {
    return (
      <Register 
        onBackToLogin={handleRegisterClose}
        isUnlocked={registerUnlocked}
        onRegisterSuccess={() => {
          setShowRegister(false);
          setRegisterUnlocked(false);
        }}
      />
    );
  }

  // ===== LOGIN =====
  return (
    <Login 
      onLoginSuccess={handleLoginSuccess}
      onShowRegister={handleShowRegister}
      onUnlockRegister={handleRegisterUnlocked}
    />
  );
}

export default App;