import { useEffect, useState } from 'react';
import PropTypes from 'prop-types';
import { Bell, X, AlertTriangle, MapPin, Clock, Radio, BellOff } from 'lucide-react';
import StatusBadge from './StatusBadge';

export default function NotificationToast({ alert, onClose }) {
  const [isVisible, setIsVisible] = useState(true);
  const [isClosing, setIsClosing] = useState(false);

  useEffect(() => {
    const timer = setTimeout(() => {
      handleClose();
    }, 8000);
    return () => clearInterval(timer);
  }, []);

  const handleClose = () => {
    setIsClosing(true);
    setTimeout(() => {
      setIsVisible(false);
      onClose();
    }, 300);
  };

  if (!isVisible || !alert) return null;

  // ===== RENDER ZONE NAMES OR NUMBERS =====
  const renderZoneBadges = (zoneData) => {
    if (!zoneData || zoneData === '00' || zoneData === '0') {
      return <span className="text-slate-400">No Zone</span>;
    }

    const zones = zoneData.split(',').map(z => z.trim()).filter(z => z !== '');
    
    if (zones.length === 0) {
      return <span className="text-slate-400">No Zone</span>;
    }

    // Check if it's zone names (contains letters) or zone numbers
    const isNames = /[a-zA-Z]/.test(zoneData);

    return (
      <div className="flex flex-wrap gap-3 justify-center">
        {zones.map((zone, index) => (
          <span 
            key={index}
            className="px-5 py-2 bg-amber-500/20 text-amber-400 border-2 border-amber-500/40 rounded-xl text-xl font-bold"
          >
            {isNames ? zone : `Zone ${String(zone).padStart(2, '0')}`}
          </span>
        ))}
      </div>
    );
  };

  // ===== USE zoneNames IF AVAILABLE, FALLBACK TO zoneNumbers =====
  const zoneDisplay = alert.zoneNames || alert.zoneNumbers;

  return (
    <div className={`fixed inset-0 z-[100] flex items-center justify-center bg-black/80 backdrop-blur-md animate-in fade-in duration-300 ${
      isClosing ? 'animate-out fade-out duration-300' : ''
    }`}>
      <div className={`bg-slate-900 border-2 border-red-500/30 rounded-3xl max-w-2xl w-full mx-4 p-8 shadow-2xl shadow-red-500/20 transform transition-all duration-500 ${
        isClosing ? 'scale-95 opacity-0' : 'scale-100 opacity-100'
      }`}>
        
        <button 
          onClick={handleClose}
          className="absolute top-4 right-4 p-2 hover:bg-slate-800 rounded-xl transition-colors"
        >
          <X className="w-6 h-6 text-slate-400 hover:text-white" />
        </button>

        <div className="text-center mb-6">
          <div className="flex justify-center mb-3">
            <div className="relative">
              <div className="absolute inset-0 bg-red-500/30 rounded-full blur-2xl animate-pulse"></div>
              <div className="relative bg-red-500/20 p-5 rounded-full border-4 border-red-500/50">
                <Bell className="w-16 h-16 text-red-500 animate-bounce" />
              </div>
            </div>
          </div>
          <h2 className="text-4xl font-bold text-white uppercase tracking-wider animate-pulse">
            🚨 NEW ALERT!
          </h2>
          <p className="text-red-400 text-sm font-mono mt-1">Immediate Attention Required</p>
        </div>

        <div className="bg-slate-950/80 border border-slate-800 rounded-2xl p-6 space-y-4">
          <div className="flex flex-wrap items-center justify-center gap-4">
            <StatusBadge status={alert.status} />
            <span className="text-2xl font-mono font-bold text-emerald-400">
              {alert.alarmSystem?.systemCode || 'UNKNOWN'}
            </span>
          </div>

          <div className="flex flex-col items-center gap-1 text-slate-300 text-lg">
            <div className="flex items-center gap-2">
              <MapPin className="w-5 h-5 text-slate-500 flex-shrink-0" />
              <span>{alert.alarmSystem?.location || 'Unknown Location'}</span>
            </div>
            {alert.alarmSystem?.description && (
              <div className="text-slate-500 text-sm font-mono tracking-wide max-w-xl text-center">
                {alert.alarmSystem.description}
              </div>
            )}
          </div>

          {/* ===== ZONES - NOW SHOWING NAMES ===== */}
          <div className="flex flex-col items-center gap-2">
            <div className="flex items-center gap-2 text-slate-400 text-sm">
              <Radio className="w-4 h-4 flex-shrink-0" />
              <span>Affected Zones</span>
            </div>
            {renderZoneBadges(zoneDisplay)}
          </div>

          {alert.status === 'SIREN_STOP' && (
            <div className="bg-orange-500/10 border border-orange-500/30 rounded-xl p-3 text-center">
              <div className="flex items-center justify-center gap-2">
                <BellOff className="w-5 h-5 text-orange-400" />
                <span className="text-orange-400 font-bold text-sm">🔕 Siren Stopped</span>
              </div>
              <p className="text-xs text-slate-400 mt-1">System action completed</p>
            </div>
          )}

          <div className="bg-slate-950 border border-slate-800 rounded-xl p-4 mt-2">
            <p className="text-xs text-slate-500 mb-1 font-mono">📨 Alert Message</p>
            <p className="text-white font-mono text-base break-words">
              {alert.alertType || 'No message'}
            </p>
          </div>

          <div className="flex items-center justify-center gap-2 text-slate-400 text-sm">
            <Clock className="w-4 h-4 flex-shrink-0" />
            <span>Received: {new Date(alert.receivedAt).toLocaleString()}</span>
          </div>
        </div>

        <div className="flex flex-col sm:flex-row gap-3 mt-6">
          <button
            onClick={handleClose}
            className="flex-1 py-3 bg-slate-800 hover:bg-slate-700 text-white rounded-xl text-sm font-mono transition-all border border-slate-700"
          >
            Dismiss
          </button>
          <button
            onClick={() => {
              const table = document.querySelector('.max-h-\\[600px\\]');
              if (table) {
                table.scrollIntoView({ behavior: 'smooth', block: 'start' });
              }
              handleClose();
            }}
            className="flex-1 py-3 bg-gradient-to-r from-red-600 to-red-700 hover:from-red-500 hover:to-red-600 text-white font-bold rounded-xl text-sm font-mono transition-all uppercase tracking-wide flex items-center justify-center gap-2"
          >
            <AlertTriangle className="w-4 h-4" />
            View Alert
          </button>
        </div>

        <div className="mt-4 flex justify-center">
          <div className="w-32 h-1 bg-slate-800 rounded-full overflow-hidden">
            <div 
              className="h-full bg-red-500 rounded-full animate-progress"
              style={{ animationDuration: '8s' }}
            ></div>
          </div>
        </div>
      </div>
    </div>
  );
}

NotificationToast.propTypes = {
  alert: PropTypes.object,
  onClose: PropTypes.func.isRequired,
};