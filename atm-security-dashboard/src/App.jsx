import { useState } from 'react';
import Dashboard from './pages/Dashboard';
import Login from './pages/Login';
import Register from './pages/Register';
import './index.css';

function App() {
  const [user, setUser] = useState(() => {
    const savedUser = localStorage.getItem('user');
    return savedUser ? JSON.parse(savedUser) : null;
  });

  const [showRegister, setShowRegister] = useState(false);
  const [registerUnlocked, setRegisterUnlocked] = useState(false);

  const handleLoginSuccess = (userData) => {
    localStorage.setItem('user', JSON.stringify(userData));
    setUser(userData);
  };

  const handleLogout = () => {
    localStorage.removeItem('user');
    setUser(null);
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

  // If user is logged in, show Dashboard
  if (user) {
    return <Dashboard user={user} onLogout={handleLogout} />;
  }

  // Show Register or Login page
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

  return (
    <Login 
      onLoginSuccess={handleLoginSuccess}
      onShowRegister={handleShowRegister}
      onUnlockRegister={handleRegisterUnlocked}
    />
  );
}

export default App;