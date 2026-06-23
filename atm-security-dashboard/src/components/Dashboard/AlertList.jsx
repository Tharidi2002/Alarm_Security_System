import React, { useState } from 'react';
import { Clock, MapPin, AlertCircle, CheckCircle, XCircle, ChevronDown, ChevronUp } from 'lucide-react';
import { formatDistanceToNow } from 'date-fns';

const AlertList = ({ alerts, loading, page, totalPages, onPageChange, onAlertUpdate }) => {
  const [expandedRow, setExpandedRow] = useState(null);

  const getStatusBadge = (status) => {
    const config = {
      PENDING: { color: 'bg-red-500/10 text-red-400 border-red-500/20', icon: AlertCircle },
      ACKNOWLEDGED: { color: 'bg-yellow-500/10 text-yellow-400 border-yellow-500/20', icon: Clock },
      RESOLVED: { color: 'bg-emerald-500/10 text-emerald-400 border-emerald-500/20', icon: CheckCircle },
      IGNORED: { color: 'bg-slate-500/10 text-slate-400 border-slate-500/20', icon: XCircle },
      ESCALATED: { color: 'bg-purple-500/10 text-purple-400 border-purple-500/20', icon: AlertCircle },
    };
    return config[status] || config.PENDING;
  };

  if (loading) {
    return (
      <div className="bg-slate-950 border border-slate-800 rounded-xl p-8 sm:p-12 text-center">
        <div className="text-slate-400 font-mono text-sm sm:text-base">Loading security logs...</div>
      </div>
    );
  }

  if (alerts.length === 0) {
    return (
      <div className="bg-slate-950 border border-slate-800 rounded-xl p-8 sm:p-12 text-center">
        <div className="text-emerald-400 font-mono text-lg">✓ All Clear</div>
        <div className="text-slate-500 text-xs sm:text-sm mt-2">No security threats detected</div>
      </div>
    );
  }

  return (
    <div className="bg-slate-950 border border-slate-800 rounded-xl overflow-hidden">
      {/* Desktop Table - hidden on mobile */}
      <div className="hidden md:block overflow-x-auto">
        <table className="w-full text-left">
          <thead>
            <tr className="bg-slate-900/50 text-slate-400 uppercase text-xs tracking-wider border-b border-slate-800 font-mono">
              <th className="py-3 px-3">Status</th>
              <th className="py-3 px-3">ATM</th>
              <th className="py-3 px-3">Zone</th>
              <th className="py-3 px-3">Alert Type</th>
              <th className="py-3 px-3 hidden lg:table-cell">Location</th>
              <th className="py-3 px-3">Time</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-slate-800/50">
            {alerts.map((alert) => {
              const StatusIcon = getStatusBadge(alert.status).icon;
              return (
                <tr key={alert.id} className="hover:bg-slate-900/40 transition-colors">
                  <td className="py-3 px-3">
                    <span className={`inline-flex items-center gap-1.5 px-2 py-1 rounded-full text-xs font-semibold border ${getStatusBadge(alert.status).color}`}>
                      <StatusIcon className="w-3 h-3" />
                      {alert.status}
                    </span>
                  </td>
                  <td className="py-3 px-3 font-mono text-xs sm:text-sm text-white">
                    {alert.atmMachine?.atmCode || 'UNKNOWN'}
                  </td>
                  <td className="py-3 px-3 font-mono text-amber-400 text-sm">
                    {alert.zoneNumber ? `Z${String(alert.zoneNumber).padStart(2, '0')}` : '--'}
                  </td>
                  <td className="py-3 px-3">
                    <span className="text-xs sm:text-sm text-slate-300">{alert.alertType}</span>
                    {alert.zoneName && (
                      <span className="text-xs text-slate-500 block">{alert.zoneName}</span>
                    )}
                  </td>
                  <td className="py-3 px-3 hidden lg:table-cell">
                    <div className="flex items-center gap-1 text-xs text-slate-400">
                      <MapPin className="w-3 h-3" />
                      {alert.atmMachine?.location || 'Unknown'}
                    </div>
                  </td>
                  <td className="py-3 px-3 text-xs text-slate-400 font-mono">
                    {formatDistanceToNow(new Date(alert.receivedAt), { addSuffix: true })}
                  </td>
                </tr>
              );
            })}
          </tbody>
        </table>
      </div>

      {/* Mobile Cards - visible on mobile */}
      <div className="md:hidden divide-y divide-slate-800">
        {alerts.map((alert) => {
          const StatusIcon = getStatusBadge(alert.status).icon;
          const isExpanded = expandedRow === alert.id;
          
          return (
            <div key={alert.id} className="p-4 hover:bg-slate-900/40 transition-colors">
              <div 
                className="flex items-center justify-between cursor-pointer"
                onClick={() => setExpandedRow(isExpanded ? null : alert.id)}
              >
                <div className="flex items-center gap-3 min-w-0">
                  <span className={`inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-xs font-semibold border ${getStatusBadge(alert.status).color} flex-shrink-0`}>
                    <StatusIcon className="w-3 h-3" />
                    {alert.status}
                  </span>
                  <span className="font-mono text-sm text-white truncate">
                    {alert.atmMachine?.atmCode || 'UNKNOWN'}
                  </span>
                </div>
                <div className="flex items-center gap-2 flex-shrink-0">
                  <span className="font-mono text-amber-400 text-sm">
                    Z{String(alert.zoneNumber || 0).padStart(2, '0')}
                  </span>
                  {isExpanded ? <ChevronUp className="w-4 h-4 text-slate-400" /> : <ChevronDown className="w-4 h-4 text-slate-400" />}
                </div>
              </div>
              
              {/* Expanded details */}
              {isExpanded && (
                <div className="mt-3 space-y-2 text-sm border-t border-slate-800/50 pt-3">
                  <div className="flex justify-between">
                    <span className="text-slate-400">Alert Type:</span>
                    <span className="text-slate-300">{alert.alertType}</span>
                  </div>
                  {alert.zoneName && (
                    <div className="flex justify-between">
                      <span className="text-slate-400">Zone Name:</span>
                      <span className="text-slate-300">{alert.zoneName}</span>
                    </div>
                  )}
                  <div className="flex justify-between">
                    <span className="text-slate-400">Location:</span>
                    <span className="text-slate-300">{alert.atmMachine?.location || 'Unknown'}</span>
                  </div>
                  <div className="flex justify-between">
                    <span className="text-slate-400">Time:</span>
                    <span className="text-slate-300 text-xs font-mono">
                      {new Date(alert.receivedAt).toLocaleString()}
                    </span>
                  </div>
                  {alert.rawMessage && (
                    <div className="flex flex-col">
                      <span className="text-slate-400">Message:</span>
                      <span className="text-slate-300 text-xs font-mono break-all bg-slate-900/50 p-2 rounded-lg mt-1">
                        {alert.rawMessage}
                      </span>
                    </div>
                  )}
                </div>
              )}
            </div>
          );
        })}
      </div>

      {/* Pagination */}
      {totalPages > 1 && (
        <div className="flex flex-col sm:flex-row items-center justify-between gap-3 px-3 sm:px-4 py-3 border-t border-slate-800">
          <button
            onClick={() => onPageChange(Math.max(0, page - 1))}
            disabled={page === 0}
            className="w-full sm:w-auto px-3 py-1.5 text-sm bg-slate-800 rounded-lg disabled:opacity-50 hover:bg-slate-700 transition-colors"
          >
            Previous
          </button>
          <span className="text-sm text-slate-400">
            Page {page + 1} of {totalPages}
          </span>
          <button
            onClick={() => onPageChange(Math.min(totalPages - 1, page + 1))}
            disabled={page === totalPages - 1}
            className="w-full sm:w-auto px-3 py-1.5 text-sm bg-slate-800 rounded-lg disabled:opacity-50 hover:bg-slate-700 transition-colors"
          >
            Next
          </button>
        </div>
      )}
    </div>
  );
};

export default AlertList;