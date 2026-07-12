import { useState } from 'react';
import PropTypes from 'prop-types';
import { X, MapPin, Clock, Radio, CheckCircle, User, Clock as ClockIcon, Timer, AlertTriangle, Bell, Phone } from 'lucide-react';
import StatusBadge from './StatusBadge';
import AlertResolveModal from './AlertResolveModal';

export default function AlertDetailsPanel({ alert, isOpen, onClose, onResolved, username }) {
  const [showResolveModal, setShowResolveModal] = useState(false);

  if (!isOpen || !alert) return null;

  // ===== NEW: Render zone names from zoneNames field =====
  const renderZoneNames = () => {
    // If zoneNames is available, use it
    if (alert.zoneNames) {
      const zones = alert.zoneNames.split(',').map(z => z.trim());
      return (
        <div className="flex flex-wrap gap-2">
          {zones.map((zone, index) => (
            <span 
              key={index}
              className="px-3 py-1 bg-amber-500/20 text-amber-400 border border-amber-500/30 rounded-lg text-sm font-bold"
            >
              {zone}
            </span>
          ))}
        </div>
      );
    }
    
    // Fallback: Use zone numbers
    if (alert.zoneNumbers && alert.zoneNumbers !== '00') {
      const zones = alert.zoneNumbers.split(',').map(z => z.trim());
      return (
        <div className="flex flex-wrap gap-2">
          {zones.map((zone, index) => (
            <span 
              key={index}
              className="px-3 py-1 bg-amber-500/20 text-amber-400 border border-amber-500/30 rounded-lg text-sm font-bold"
            >
              Zone {String(zone).padStart(2, '0')}
            </span>
          ))}
        </div>
      );
    }
    
    return <span className="text-slate-400 text-sm">No Zone</span>;
  };

  const formatDuration = (seconds) => {
    if (!seconds) return 'N/A';
    
    const days = Math.floor(seconds / 86400);
    const hours = Math.floor((seconds % 86400) / 3600);
    const mins = Math.floor((seconds % 3600) / 60);
    const secs = Math.floor(seconds % 60);
    
    const parts = [];
    if (days > 0) parts.push(`${days}d`);
    if (hours > 0) parts.push(`${hours}h`);
    if (mins > 0) parts.push(`${mins}m`);
    if (secs > 0) parts.push(`${secs}s`);
    
    return parts.length > 0 ? parts.join(' ') : '0s';
  };

  const getAlertIcon = (alertType) => {
    if (!alertType) return <Bell className="w-5 h-5 text-slate-400" />;
    const lower = alertType.toLowerCase();
    if (lower.includes('call')) return <Phone className="w-5 h-5 text-blue-400" />;
    if (lower.includes('zone')) return <AlertTriangle className="w-5 h-5 text-amber-400" />;
    if (lower.includes('alarm')) return <Bell className="w-5 h-5 text-red-400" />;
    return <Bell className="w-5 h-5 text-slate-400" />;
  };

  const isPending = alert.status === 'PENDING';

  const getLivePendingDuration = () => {
    if (!alert.receivedAt || !isPending) return null;
    const now = new Date();
    const received = new Date(alert.receivedAt);
    const diffMs = now - received;
    const diffSecs = Math.floor(diffMs / 1000);
    return formatDuration(diffSecs);
  };

  const handleClose = () => {
    if (onClose) onClose();
  };

  return (
    <>
      <div 
        className="fixed inset-0 z-[100] flex items-center justify-center p-4 bg-black/80 backdrop-blur-md animate-in fade-in duration-200"
        onClick={handleClose}
      >
        <div 
          className="bg-slate-900 border border-slate-700 rounded-2xl max-w-2xl w-full max-h-[90vh] overflow-y-auto shadow-2xl shadow-red-500/10"
          onClick={(e) => e.stopPropagation()}
        >
          {/* Header */}
          <div className="flex justify-between items-center p-5 border-b border-slate-800 bg-slate-950/40 sticky top-0 z-10 rounded-t-2xl">
            <div className="flex items-center gap-3">
              <div className="bg-red-500/10 p-2 rounded-lg border border-red-500/20">
                {getAlertIcon(alert.alertType)}
              </div>
              <div>
                <h2 className="text-lg font-bold text-white">Alert Details</h2>
                <p className="text-xs text-slate-400 font-mono">ID: #{alert.id}</p>
              </div>
            </div>
            <button 
              onClick={handleClose}
              className="p-2 hover:bg-slate-800 rounded-lg transition-colors"
              aria-label="Close panel"
            >
              <X className="w-5 h-5 text-slate-400 hover:text-white" />
            </button>
          </div>

          {/* Body */}
          <div className="p-5 space-y-4">
            
            {/* Status & System */}
            <div className="flex flex-wrap items-center gap-3">
              <StatusBadge status={alert.status} />
              <span className="text-sm font-mono font-bold text-white bg-slate-800 px-3 py-1 rounded-lg">
                {alert.alarmSystem?.systemCode || 'UNKNOWN'}
              </span>
            </div>

            {/* Location */}
            <div className="flex items-center gap-2 text-slate-300 bg-slate-800/30 rounded-xl px-4 py-2.5">
              <MapPin className="w-4 h-4 text-slate-500 flex-shrink-0" />
              <div>
                <div className="text-sm">{alert.alarmSystem?.location || 'Unknown Location'}</div>
                {alert.alarmSystem?.description && (
                  <div className="text-[12px] text-slate-500">{alert.alarmSystem.description}</div>
                )}
              </div>
            </div>

            {/* ===== ZONES - NOW SHOWING NAMES ===== */}
            <div className="bg-slate-800/30 rounded-xl px-4 py-2.5">
              <div className="flex items-start gap-2">
                <Radio className="w-4 h-4 text-slate-500 flex-shrink-0 mt-0.5" />
                <div className="flex flex-col gap-1 w-full">
                  <span className="text-xs text-slate-400 font-mono">Affected Zones</span>
                  <div className="flex flex-wrap gap-2">
                    {renderZoneNames()}
                  </div>
                </div>
              </div>
            </div>

            {/* Full Message */}
            <div className="bg-slate-950/70 border border-slate-800 rounded-xl p-4">
              <p className="text-xs text-slate-400 mb-2 uppercase tracking-wider font-mono">📨 Full Message</p>
              <p className="text-white font-mono text-sm break-words leading-relaxed">
                {alert.rawMessage || alert.alertType || 'No message'}
              </p>
            </div>

            {/* Received Time */}
            <div className="flex items-center gap-2 text-slate-400 text-sm bg-slate-800/30 rounded-xl px-4 py-2.5">
              <Clock className="w-4 h-4 flex-shrink-0" />
              <span>Received: <span className="text-white">{new Date(alert.receivedAt).toLocaleString()}</span></span>
            </div>

            {/* Pending Duration */}
            {isPending && (
              <div className="flex items-center gap-2 text-yellow-400 text-sm bg-yellow-500/10 border border-yellow-500/20 rounded-xl px-4 py-2.5">
                <Timer className="w-4 h-4 flex-shrink-0" />
                <span>Pending for: <span className="font-bold font-mono text-yellow-300">{getLivePendingDuration()}</span></span>
              </div>
            )}

            {/* Resolved Info */}
            {alert.status === 'RESOLVED' && (
              <div className="bg-emerald-500/10 border border-emerald-500/20 rounded-xl p-4 space-y-2.5">
                <div className="flex items-center gap-2 text-emerald-400">
                  <CheckCircle className="w-4 h-4" />
                  <span className="text-sm font-bold">Resolved</span>
                </div>
                
                <div className="grid grid-cols-1 sm:grid-cols-2 gap-2">
                  <div className="flex items-center gap-2 text-slate-400 text-xs">
                    <User className="w-3.5 h-3.5" />
                    <span>By: <span className="text-white font-medium">{alert.resolvedBy || 'Unknown'}</span></span>
                  </div>
                  <div className="flex items-center gap-2 text-slate-400 text-xs">
                    <ClockIcon className="w-3.5 h-3.5" />
                    <span>At: <span className="text-white font-medium">{alert.resolvedAt ? new Date(alert.resolvedAt).toLocaleString() : 'N/A'}</span></span>
                  </div>
                </div>

                <div className="flex items-center gap-2 text-slate-400 text-xs">
                  <Timer className="w-3.5 h-3.5 text-yellow-500" />
                  <span>Pending duration: <span className="text-yellow-400 font-bold">{formatDuration(alert.pendingDurationSeconds)}</span></span>
                </div>

                {alert.resolutionDescription && (
                  <div className="mt-2 bg-slate-950 border border-slate-800 rounded-lg p-3 text-xs text-slate-300">
                    <span className="text-slate-500 font-mono text-[10px]">📝 Description:</span>
                    <p className="mt-0.5">{alert.resolutionDescription}</p>
                  </div>
                )}

                {alert.resolvedFromIp && (
                  <div className="text-[10px] text-slate-500 font-mono flex items-center gap-1">
                    <span>🌐 IP: {alert.resolvedFromIp}</span>
                  </div>
                )}
              </div>
            )}

            {/* Actions */}
            <div className="flex flex-col sm:flex-row gap-3 pt-2 border-t border-slate-800">
              {isPending && (
                <button
                  onClick={() => setShowResolveModal(true)}
                  className="flex-1 py-2.5 bg-gradient-to-r from-emerald-600 to-emerald-700 hover:from-emerald-500 hover:to-emerald-600 text-white font-bold rounded-xl text-sm font-mono transition-all uppercase tracking-wide flex items-center justify-center gap-2"
                >
                  <CheckCircle className="w-4 h-4" />
                  Resolve This Alert
                </button>
              )}
              
              <button
                onClick={handleClose}
                className={`${isPending ? 'flex-1' : 'w-full'} py-2.5 border border-slate-700 text-slate-400 hover:text-white hover:border-slate-600 rounded-xl text-sm font-mono transition-all`}
              >
                Close
              </button>
            </div>
          </div>
        </div>
      </div>

      <AlertResolveModal
        alert={alert}
        isOpen={showResolveModal}
        onClose={() => setShowResolveModal(false)}
        onResolved={(resolvedAlert) => {
          setShowResolveModal(false);
          if (onResolved) onResolved(resolvedAlert);
        }}
        username={username}
      />
    </>
  );
}

AlertDetailsPanel.propTypes = {
  alert: PropTypes.object,
  isOpen: PropTypes.bool.isRequired,
  onClose: PropTypes.func.isRequired,
  onResolved: PropTypes.func,
  username: PropTypes.string,
};