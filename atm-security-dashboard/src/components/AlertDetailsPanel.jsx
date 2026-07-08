import { useState } from 'react';
import PropTypes from 'prop-types';
import { X, MapPin, Clock, Radio, ChevronRight, CheckCircle, User, Clock as ClockIcon } from 'lucide-react';
import StatusBadge from './StatusBadge';
import AlertResolveModal from './AlertResolveModal';

export default function AlertDetailsPanel({ alert, isOpen, onClose, onResolved, username }) {
  const [showResolveModal, setShowResolveModal] = useState(false);

  if (!isOpen || !alert) return null;

  const renderZoneBadges = (zoneNumbers) => {
    if (!zoneNumbers || zoneNumbers === '00' || zoneNumbers === '0') {
      return <span className="text-slate-500 text-sm">No Zone</span>;
    }

    const zones = zoneNumbers.split(',').map(z => z.trim()).filter(z => z !== '');
    
    if (zones.length === 0) {
      return <span className="text-slate-500 text-sm">No Zone</span>;
    }

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
  };

  const formatDuration = (seconds) => {
    if (!seconds) return 'N/A';
    const mins = Math.floor(seconds / 60);
    const hours = Math.floor(mins / 60);
    const days = Math.floor(hours / 24);
    
    if (days > 0) return `${days}d ${hours % 24}h ${mins % 60}m`;
    if (hours > 0) return `${hours}h ${mins % 60}m`;
    return `${mins}m`;
  };

  const isPending = alert.status === 'PENDING';

  return (
    <>
      {/* Mobile: Full screen overlay */}
      <div className="lg:hidden fixed inset-0 z-50 bg-black/70 backdrop-blur-sm" onClick={onClose}>
        <div 
          className="absolute bottom-0 left-0 right-0 bg-slate-900 rounded-t-2xl max-h-[80vh] overflow-y-auto p-6 border-t border-slate-700"
          onClick={(e) => e.stopPropagation()}
        >
          <div className="flex justify-between items-center mb-4">
            <h3 className="text-lg font-bold text-white">Alert Details</h3>
            <button onClick={onClose} className="p-2 hover:bg-slate-800 rounded-lg">
              <X className="w-5 h-5 text-slate-400" />
            </button>
          </div>
          <PanelContent 
            alert={alert} 
            renderZoneBadges={renderZoneBadges} 
            formatDuration={formatDuration}
            isPending={isPending}
            onResolveClick={() => setShowResolveModal(true)}
          />
        </div>
      </div>

      {/* Desktop: Right side panel */}
      <div className={`hidden lg:block fixed top-0 right-0 h-full w-[420px] bg-slate-900 border-l border-slate-800 shadow-2xl transform transition-transform duration-300 z-40 ${
        isOpen ? 'translate-x-0' : 'translate-x-full'
      }`}>
        <div className="p-6 h-full overflow-y-auto">
          <div className="flex justify-between items-center mb-6">
            <h3 className="text-lg font-bold text-white flex items-center gap-2">
              <ChevronRight className="w-5 h-5 text-red-500" />
              Alert Details
            </h3>
            <button onClick={onClose} className="p-2 hover:bg-slate-800 rounded-lg">
              <X className="w-5 h-5 text-slate-400" />
            </button>
          </div>
          <PanelContent 
            alert={alert} 
            renderZoneBadges={renderZoneBadges} 
            formatDuration={formatDuration}
            isPending={isPending}
            onResolveClick={() => setShowResolveModal(true)}
          />
        </div>
      </div>

      {/* Resolve Modal */}
      <AlertResolveModal
        alert={alert}
        isOpen={showResolveModal}
        onClose={() => setShowResolveModal(false)}
        onResolved={onResolved}
        username={username}
      />
    </>
  );
}

// ===== PanelContent Component =====
function PanelContent({ alert, renderZoneBadges, formatDuration, isPending, onResolveClick }) {
  return (
    <div className="space-y-4">
      <div className="flex flex-wrap items-center gap-3">
        <StatusBadge status={alert.status} />
        <span className="text-sm font-mono text-white bg-slate-800 px-3 py-1 rounded-lg">
          {alert.alarmSystem?.systemCode || 'UNKNOWN'}
        </span>
      </div>

      <div className="flex items-center gap-2 text-slate-300">
        <MapPin className="w-4 h-4 text-slate-500 flex-shrink-0" />
        <span className="text-sm">{alert.alarmSystem?.location || 'Unknown Location'}</span>
      </div>

      <div className="flex items-start gap-2">
        <Radio className="w-4 h-4 text-slate-500 flex-shrink-0 mt-1" />
        <div className="flex flex-wrap gap-2">
          {renderZoneBadges(alert.zoneNumbers)}
        </div>
      </div>

      <div className="bg-slate-950 border border-slate-800 rounded-xl p-4">
        <p className="text-xs text-slate-400 mb-2 uppercase tracking-wider">📨 Full Message</p>
        <p className="text-white font-mono text-sm break-words leading-relaxed">
          {alert.rawMessage || alert.alertType || 'No message'}
        </p>
      </div>

      <div className="flex items-center gap-2 text-slate-400 text-sm border-t border-slate-800 pt-4">
        <Clock className="w-4 h-4 flex-shrink-0" />
        <span>Received: {new Date(alert.receivedAt).toLocaleString()}</span>
      </div>

      {/* Resolved Info - Only show if resolved */}
      {alert.status === 'RESOLVED' && (
        <div className="bg-emerald-500/5 border border-emerald-500/20 rounded-xl p-4 space-y-2">
          <div className="flex items-center gap-2 text-emerald-400">
            <CheckCircle className="w-4 h-4" />
            <span className="text-sm font-bold">Resolved</span>
          </div>
          <div className="flex items-center gap-2 text-slate-400 text-xs">
            <User className="w-3.5 h-3.5" />
            <span>By: <span className="text-white">{alert.resolvedBy || 'Unknown'}</span></span>
          </div>
          <div className="flex items-center gap-2 text-slate-400 text-xs">
            <ClockIcon className="w-3.5 h-3.5" />
            <span>At: <span className="text-white">{alert.resolvedAt ? new Date(alert.resolvedAt).toLocaleString() : 'N/A'}</span></span>
          </div>
          <div className="flex items-center gap-2 text-slate-400 text-xs">
            <Clock className="w-3.5 h-3.5" />
            <span>Pending duration: <span className="text-yellow-400 font-bold">{formatDuration(alert.pendingDurationSeconds)}</span></span>
          </div>
          {alert.resolutionDescription && (
            <div className="mt-2 bg-slate-950 border border-slate-800 rounded-lg p-3 text-xs text-slate-300">
              <span className="text-slate-500">Description: </span>
              {alert.resolutionDescription}
            </div>
          )}
          {alert.resolvedFromIp && (
            <div className="text-[10px] text-slate-500 font-mono">
              IP: {alert.resolvedFromIp}
            </div>
          )}
        </div>
      )}

      {/* Resolve Button - Only show for PENDING */}
      {isPending && (
        <button
          onClick={onResolveClick}
          className="w-full mt-2 py-2.5 bg-gradient-to-r from-emerald-600 to-emerald-700 hover:from-emerald-500 hover:to-emerald-600 text-white font-bold rounded-xl text-sm font-mono transition-all uppercase tracking-wide flex items-center justify-center gap-2"
        >
          <CheckCircle className="w-4 h-4" />
          Resolve This Alert
        </button>
      )}
    </div>
  );
}

// ===== PropTypes =====
AlertDetailsPanel.propTypes = {
  alert: PropTypes.object,
  isOpen: PropTypes.bool.isRequired,
  onClose: PropTypes.func.isRequired,
  onResolved: PropTypes.func.isRequired,
  username: PropTypes.string.isRequired,
};

PanelContent.propTypes = {
  alert: PropTypes.object.isRequired,
  renderZoneBadges: PropTypes.func.isRequired,
  formatDuration: PropTypes.func.isRequired,
  isPending: PropTypes.bool.isRequired,
  onResolveClick: PropTypes.func.isRequired,
};