import PropTypes from 'prop-types';
import { X, MapPin, Clock, Radio, AlertTriangle } from 'lucide-react';
import StatusBadge from './StatusBadge';

export default function AlertModal({ alert, isOpen, onClose }) {
  if (!isOpen || !alert) return null;

  const renderZoneBadges = (zoneNumbers) => {
    if (!zoneNumbers || zoneNumbers === '00' || zoneNumbers === '0') {
      return <span className="text-slate-500">No Zone</span>;
    }

    const zones = zoneNumbers.split(',').map(z => z.trim()).filter(z => z !== '');
    
    if (zones.length === 0) {
      return <span className="text-slate-500">No Zone</span>;
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

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/70 backdrop-blur-sm animate-in fade-in duration-200">
      <div className="bg-slate-900 border border-slate-700 rounded-2xl max-w-2xl w-full max-h-[90vh] overflow-y-auto shadow-2xl">
        {/* Header */}
        <div className="flex justify-between items-center p-6 border-b border-slate-800">
          <div className="flex items-center gap-3">
            <AlertTriangle className="w-6 h-6 text-red-500" />
            <h2 className="text-xl font-bold text-white">Alert Details</h2>
          </div>
          <button 
            onClick={onClose}
            className="p-2 hover:bg-slate-800 rounded-lg transition-colors"
          >
            <X className="w-5 h-5 text-slate-400" />
          </button>
        </div>

        {/* Content */}
        <div className="p-6 space-y-4">
          <div className="flex flex-wrap items-center gap-4">
            <StatusBadge status={alert.status} />
            <span className="text-sm font-mono text-white bg-slate-800 px-3 py-1 rounded-lg">
              {alert.alarmSystem?.systemCode || 'UNKNOWN'}
            </span>
          </div>

          <div className="flex items-center gap-2 text-slate-300">
            <MapPin className="w-4 h-4 text-slate-500" />
            <div>
              <div>{alert.alarmSystem?.location || 'Unknown Location'}</div>
              {alert.alarmSystem?.description && (
                <div className="text-[12px] text-slate-500">{alert.alarmSystem.description}</div>
              )}
            </div>
          </div>

          <div className="flex items-start gap-2">
            <Radio className="w-4 h-4 text-slate-500 flex-shrink-0 mt-1" />
            <div className="flex flex-wrap gap-2">
              {renderZoneBadges(alert.zoneNumbers)}
            </div>
          </div>

          <div className="bg-slate-950 border border-slate-800 rounded-xl p-4">
            <p className="text-sm text-slate-400 mb-2">📨 Full Message</p>
            <p className="text-white font-mono text-sm break-words">
              {alert.rawMessage || alert.alertType || 'No message'}
            </p>
          </div>

          <div className="flex items-center gap-2 text-slate-400 text-sm">
            <Clock className="w-4 h-4" />
            <span>{new Date(alert.receivedAt).toLocaleString()}</span>
          </div>
        </div>
      </div>
    </div>
  );
}

AlertModal.propTypes = {
  alert: PropTypes.object,
  isOpen: PropTypes.bool.isRequired,
  onClose: PropTypes.func.isRequired,
};