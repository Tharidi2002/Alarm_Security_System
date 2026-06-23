import React from 'react';
import { useNavigate } from 'react-router-dom';
import { Shield, LogOut, User, Bell } from 'lucide-react';
import { useAuth } from '../../contexts/AuthContext';

const Navbar = () => {
  const { user, logout } = useAuth();
  const navigate = useNavigate();

  const handleLogout = () => {
    logout(navigate);
  };

  return (
    <nav className="bg-slate-950/90 border-b border-slate-800 px-3 sm:px-6 py-2 sm:py-3 flex items-center justify-between sticky top-0 z-50 backdrop-blur">
      <div className="flex items-center gap-2 sm:gap-3 min-w-0">
        <div className="bg-red-500/10 p-1.5 sm:p-2 rounded-lg border border-red-500/20 flex-shrink-0">
          <Shield className="w-4 h-4 sm:w-5 sm:h-5 text-red-500 animate-pulse" />
        </div>
        <div className="min-w-0">
          <h1 className="text-sm sm:text-base lg:text-lg font-bold tracking-wider text-white truncate">ATM SECURITY</h1>
          <p className="hidden sm:flex text-[10px] sm:text-xs text-slate-400 font-mono items-center gap-1">
            <span className="w-1.5 h-1.5 bg-emerald-500 rounded-full animate-ping"></span>
            LIVE MONITORING
          </p>
        </div>
      </div>
      
      <div className="flex items-center gap-2 sm:gap-4 flex-shrink-0">
        <button className="relative p-1.5 sm:p-2 hover:bg-slate-800 rounded-lg transition-colors">
          <Bell className="w-4 h-4 sm:w-5 sm:h-5 text-slate-400" />
          <span className="absolute top-0.5 right-0.5 w-1.5 h-1.5 sm:w-2 sm:h-2 bg-red-500 rounded-full animate-pulse" />
        </button>
        <div className="hidden sm:flex items-center gap-3">
          <div className="text-right">
            <p className="text-sm font-medium text-white truncate max-w-[100px]">{user?.fullName || 'User'}</p>
            <p className="text-xs text-slate-400">{user?.role || 'User'}</p>
          </div>
          <button
            onClick={handleLogout}
            className="p-2 hover:bg-slate-800 rounded-lg transition-colors"
          >
            <LogOut className="w-4 h-4 sm:w-5 sm:h-5 text-slate-400" />
          </button>
        </div>
        {/* Mobile user indicator */}
        <div className="sm:hidden flex items-center gap-2">
          <div className="w-8 h-8 rounded-full bg-slate-800 flex items-center justify-center border border-slate-700">
            <User className="w-4 h-4 text-slate-400" />
          </div>
        </div>
      </div>
    </nav>
  );
};

export default Navbar;