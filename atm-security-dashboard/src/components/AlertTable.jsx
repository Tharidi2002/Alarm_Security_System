import { useState, useEffect, useMemo } from 'react';
import PropTypes from 'prop-types';
import { MapPin, MessageSquare, Phone, Bell, AlertTriangle, CheckCircle, Timer, Filter, X, Search } from 'lucide-react';
import StatusBadge from './StatusBadge';
import AlertModal from './AlertModal';
import AlertDetailsPanel from './AlertDetailsPanel';
import AlertResolveModal from './AlertResolveModal';
import LoadingSkeleton from './LoadingSkeleton';

export default function AlertTable({ alerts, loading, tableContainerRef, username, onAlertResolved }) {
  const [selectedAlert, setSelectedAlert] = useState(null);
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [isPanelOpen, setIsPanelOpen] = useState(false);
  const [resolveAlertData, setResolveAlertData] = useState(null);
  const [isResolveModalOpen, setIsResolveModalOpen] = useState(false);
  const [currentTime, setCurrentTime] = useState(new Date());
  
  const [filterCategory, setFilterCategory] = useState('ALL');
  const [filterSystem, setFilterSystem] = useState('ALL');
  const [showFilterDropdown, setShowFilterDropdown] = useState(false);
  const [showSystemDropdown, setShowSystemDropdown] = useState(false);
  const [searchQuery, setSearchQuery] = useState('');

  useEffect(() => {
    const interval = setInterval(() => setCurrentTime(new Date()), 1000);
    return () => clearInterval(interval);
  }, []);

  const getAlertCategory = (alert) => {
    const alertType = alert.alertType || '';
    const status = alert.status || '';
    const lowerType = alertType.toLowerCase();
    
    if (status === 'SIREN_STOP' || lowerType.includes('siren_stop')) {
      return 'SIREN_STOP';
    }
    if (status === 'REJECTED') {
      return 'REJECTED';
    }
    if (lowerType.includes('call incoming') || lowerType.includes('call from') || 
        lowerType.includes('incoming call') || status === 'CALL') {
      return 'CALL';
    }
    if (lowerType.includes('armed') || status === 'ARMED') {
      return 'ARMED';
    }
    if (status === 'RESOLVED') {
      return 'RESOLVED';
    }
    if (lowerType.includes('zone') || lowerType.includes('alarm')) {
      return 'ZONE_ALARM';
    }
    if (status === 'SIREN_STOP') {
      return 'SIREN_STOP';
    }
    return 'OTHER';
  };

  const getCategoryColor = (category) => {
    switch(category) {
      case 'REJECTED': return 'text-slate-400 bg-slate-500/10 border-slate-500/20';
      case 'CALL': return 'text-blue-400 bg-blue-500/10 border-blue-500/20';
      case 'ARMED': return 'text-yellow-400 bg-yellow-500/10 border-yellow-500/20';
      case 'RESOLVED': return 'text-emerald-400 bg-emerald-500/10 border-emerald-500/20';
      case 'ZONE_ALARM': return 'text-red-400 bg-red-500/10 border-red-500/20';
      case 'SIREN_STOP': return 'text-orange-400 bg-orange-500/10 border-orange-500/20';
      default: return 'text-slate-400 bg-slate-500/10 border-slate-500/20';
    }
  };

  const getCategoryShort = (category) => {
    switch(category) {
      case 'REJECTED': return '🚫 Rejected';
      case 'CALL': return 'Call';
      case 'ARMED': return 'ARMED';
      case 'RESOLVED': return 'Done';
      case 'ZONE_ALARM': return 'Zone';
      case 'SIREN_STOP': return '🔕 Stop';
      default: return 'Other';
    }
  };

  const getUniqueSystems = useMemo(() => {
    const systems = new Set();
    alerts.forEach(alert => {
      if (alert.alarmSystem?.systemCode) {
        systems.add(alert.alarmSystem.systemCode);
      }
    });
    return ['ALL', ...Array.from(systems)];
  }, [alerts]);

  const getCategoryCounts = useMemo(() => {
    const counts = {
      ALL: alerts.length,
      CALL: 0,
      ARMED: 0,
      RESOLVED: 0,
      ZONE_ALARM: 0,
      SIREN_STOP: 0,
      OTHER: 0
    };
    alerts.forEach(alert => {
      const category = getAlertCategory(alert);
      if (counts[category] !== undefined) {
        counts[category] = (counts[category] || 0) + 1;
      }
    });
    return counts;
  }, [alerts]);

  const getSystemCounts = useMemo(() => {
    const counts = {};
    alerts.forEach(alert => {
      const system = alert.alarmSystem?.systemCode || 'UNKNOWN';
      counts[system] = (counts[system] || 0) + 1;
    });
    return counts;
  }, [alerts]);

  const getFilteredAlerts = () => {
    let filtered = alerts;
    if (filterCategory !== 'ALL') {
      filtered = filtered.filter(alert => getAlertCategory(alert) === filterCategory);
    }
    if (filterSystem !== 'ALL') {
      filtered = filtered.filter(alert => 
        (alert.alarmSystem?.systemCode || 'UNKNOWN') === filterSystem
      );
    }
    if (searchQuery.trim()) {
      const query = searchQuery.toLowerCase().trim();
      filtered = filtered.filter(alert => {
        const systemCode = (alert.alarmSystem?.systemCode || '').toLowerCase();
        const location = (alert.alarmSystem?.location || '').toLowerCase();
        const message = (alert.alertType || '').toLowerCase();
        const zones = (alert.zoneNumbers || '').toLowerCase();
        return systemCode.includes(query) || 
               location.includes(query) || 
               message.includes(query) || 
               zones.includes(query);
      });
    }
    return filtered;
  };

  const filteredAlerts = getFilteredAlerts();

  const getPendingDuration = (receivedAt) => {
    if (!receivedAt) return 'N/A';
    const now = currentTime;
    const received = new Date(receivedAt);
    const diffMs = now - received;
    if (diffMs < 0) return 'N/A';
    const diffSecs = Math.floor(diffMs / 1000);
    const diffMins = Math.floor(diffSecs / 60);
    const diffHours = Math.floor(diffMins / 60);
    const diffDays = Math.floor(diffHours / 24);
    let durationStr = '';
    if (diffDays > 0) durationStr += `${diffDays}d `;
    if (diffHours % 24 > 0) durationStr += `${diffHours % 24}h `;
    if (diffMins % 60 > 0) durationStr += `${diffMins % 60}m `;
    if (diffSecs % 60 > 0) durationStr += `${diffSecs % 60}s`;
    return durationStr.trim() || '0s';
  };

  const FilterDropdown = () => {
    const categories = [
      { value: 'ALL', label: '📊 All' },
      { value: 'ZONE_ALARM', label: '🔴 Zone' },
      { value: 'CALL', label: '📞 Call' },
      { value: 'ARMED', label: '🟡 ARMED' },
      { value: 'RESOLVED', label: '✅ Done' },
      { value: 'SIREN_STOP', label: '🔕 Siren Stop' },
      { value: 'REJECTED', label: '🚫 Rejected' },
      { value: 'OTHER', label: '📌 Other' }
    ];

    return (
      <div className="relative" style={{ zIndex: 9999 }}>
        <button
          onClick={() => {
            setShowFilterDropdown(!showFilterDropdown);
            setShowSystemDropdown(false);
          }}
          className="flex items-center gap-1 px-1.5 sm:px-2 py-1 sm:py-1.5 bg-slate-800 hover:bg-slate-700 border border-slate-700 rounded-lg text-[9px] sm:text-[10px] font-mono text-slate-300 transition-all whitespace-nowrap"
        >
          <Filter className="w-2.5 h-2.5 sm:w-3 sm:h-3" />
          <span className="truncate max-w-[35px] sm:max-w-[50px]">
            {filterCategory === 'ALL' ? 'All' : getCategoryShort(filterCategory)}
          </span>
          <span className="text-slate-500 text-[8px] sm:text-[9px]">({filterCategory === 'ALL' ? alerts.length : getCategoryCounts[filterCategory] || 0})</span>
        </button>
        {showFilterDropdown && (
          <div className="absolute top-full left-0 mt-1 w-36 sm:w-40 bg-slate-900 border border-slate-800 rounded-xl shadow-2xl overflow-hidden" style={{ zIndex: 9999, minWidth: '130px', maxHeight: '250px', overflowY: 'auto' }}>
            <div className="p-1 space-y-0.5">
              {categories.map((option) => (
                <button
                  key={option.value}
                  onClick={() => {
                    setFilterCategory(option.value);
                    setShowFilterDropdown(false);
                  }}
                  className={`w-full text-left px-2 sm:px-3 py-1 sm:py-1.5 rounded-lg text-[9px] sm:text-[10px] font-mono transition-all flex items-center justify-between ${
                    filterCategory === option.value
                      ? 'bg-red-500/10 text-white border border-red-500/30'
                      : 'text-slate-400 hover:bg-slate-800 hover:text-white'
                  }`}
                >
                  <span>{option.label}</span>
                  <span className="text-[8px] sm:text-[9px] text-slate-500 flex-shrink-0 ml-1 sm:ml-2">({getCategoryCounts[option.value] || 0})</span>
                </button>
              ))}
            </div>
          </div>
        )}
      </div>
    );
  };

  const SystemDropdown = () => {
    const systems = getUniqueSystems;
    return (
      <div className="relative" style={{ zIndex: 9998 }}>
        <button
          onClick={() => {
            setShowSystemDropdown(!showSystemDropdown);
            setShowFilterDropdown(false);
          }}
          className="flex items-center gap-1 px-1.5 sm:px-2 py-1 sm:py-1.5 bg-slate-800 hover:bg-slate-700 border border-slate-700 rounded-lg text-[9px] sm:text-[10px] font-mono text-slate-300 transition-all whitespace-nowrap"
        >
          <Search className="w-2.5 h-2.5 sm:w-3 sm:h-3" />
          <span className="truncate max-w-[35px] sm:max-w-[50px]">
            {filterSystem === 'ALL' ? 'Systems' : filterSystem}
          </span>
          <span className="text-slate-500 text-[8px] sm:text-[9px]">
            ({filterSystem === 'ALL' ? alerts.length : getSystemCounts[filterSystem] || 0})
          </span>
        </button>
        {showSystemDropdown && (
          <div className="absolute top-full left-0 mt-1 w-40 sm:w-48 bg-slate-900 border border-slate-800 rounded-xl shadow-2xl overflow-hidden" style={{ zIndex: 9998, minWidth: '150px', maxHeight: '250px', overflowY: 'auto' }}>
            <div className="p-1 space-y-0.5">
              {systems.map((system) => (
                <button
                  key={system}
                  onClick={() => {
                    setFilterSystem(system);
                    setShowSystemDropdown(false);
                  }}
                  className={`w-full text-left px-2 sm:px-3 py-1 sm:py-1.5 rounded-lg text-[9px] sm:text-[10px] font-mono transition-all flex items-center justify-between ${
                    filterSystem === system
                      ? 'bg-red-500/10 text-white border border-red-500/30'
                      : 'text-slate-400 hover:bg-slate-800 hover:text-white'
                  }`}
                >
                  <span className="truncate max-w-[80px] sm:max-w-[120px]">{system === 'ALL' ? '📊 All Systems' : system}</span>
                  <span className="text-[8px] sm:text-[9px] text-slate-500 flex-shrink-0 ml-1 sm:ml-2">({getSystemCounts[system] || 0})</span>
                </button>
              ))}
            </div>
          </div>
        )}
      </div>
    );
  };

  const getMessageIcon = (alertType) => {
    if (!alertType) return <MessageSquare className="w-3 h-3 sm:w-3.5 sm:h-3.5 text-slate-400 flex-shrink-0" />;
    const lower = alertType.toLowerCase();
    if (lower.includes('call')) return <Phone className="w-3 h-3 sm:w-3.5 sm:h-3.5 text-blue-400 flex-shrink-0" />;
    if (lower.includes('alarm')) return <Bell className="w-3 h-3 sm:w-3.5 sm:h-3.5 text-red-400 flex-shrink-0" />;
    if (lower.includes('zone')) return <AlertTriangle className="w-3 h-3 sm:w-3.5 sm:h-3.5 text-amber-400 flex-shrink-0" />;
    return <MessageSquare className="w-3 h-3 sm:w-3.5 sm:h-3.5 text-slate-400 flex-shrink-0" />;
  };

  const getMessagePreview = (alertType) => {
    if (!alertType) return '—';
    if (alertType.length > 20) return alertType.substring(0, 20) + '…';
    return alertType;
  };

  const handleRowClick = (alert) => {
    setSelectedAlert(alert);
    setIsPanelOpen(true);
  };

  const handleMessageClick = (e, alert) => {
    e.stopPropagation();
    setSelectedAlert(alert);
    setIsModalOpen(true);
  };

  const handleResolveClick = (e, alert) => {
    e.stopPropagation();
    setResolveAlertData(alert);
    setIsResolveModalOpen(true);
  };

  const handleResolved = (resolvedAlert) => {
    setSelectedAlert(resolvedAlert);
    if (onAlertResolved) {
      onAlertResolved();
    }
    setIsResolveModalOpen(false);
  };

  const renderZoneBadges = (zoneData) => {
    if (!zoneData || zoneData === '00' || zoneData === '0') {
      return <span className="text-slate-500 text-[8px] sm:text-[9px]">—</span>;
    }
    const zones = zoneData.split(',').map(z => z.trim()).filter(z => z !== '');
    if (zones.length === 0) return <span className="text-slate-500 text-[8px] sm:text-[9px]">—</span>;
    
    const isNames = /[a-zA-Z]/.test(zoneData);
    
    return (
      <div className="flex flex-wrap gap-0.5">
        {zones.map((zone, index) => (
          <span key={index} className="inline-flex items-center px-0.5 py-0.5 rounded text-[7px] sm:text-[8px] font-bold bg-amber-500/20 text-amber-400 border border-amber-500/30 whitespace-nowrap">
            {isNames ? zone : `Z${String(zone).padStart(2, '0')}`}
          </span>
        ))}
      </div>
    );
  };

  const renderSirenStatus = (alert) => {
    if (!alert.alarmSystem || alert.status !== 'PENDING') return null;
    const status = alert.alarmSystem.sirenStatus;
    if (status === 'ON') {
      return (
        <span className="inline-flex items-center gap-0.5 px-1 py-0.5 rounded-full text-[7px] sm:text-[8px] font-bold bg-red-500/20 text-red-400 border border-red-500/30 animate-pulse">
          🔔 ON
        </span>
      );
    }
    if (status === 'OFF') {
      return (
        <span className="inline-flex items-center gap-0.5 px-1 py-0.5 rounded-full text-[7px] sm:text-[8px] font-bold bg-slate-500/20 text-slate-400 border border-slate-500/30">
          🔕 OFF
        </span>
      );
    }
    return null;
  };

  if (loading) {
    return <LoadingSkeleton />;
  }

  if (!alerts || alerts.length === 0) {
    return (
      <div className="bg-slate-950 border border-slate-800 rounded-xl p-6 sm:p-8 md:p-12 text-center">
        <div className="text-emerald-400 font-mono text-base sm:text-lg">🎉 System Secure. No alerts.</div>
      </div>
    );
  }

  return (
    <>
      <div ref={tableContainerRef} className="bg-slate-950 border border-slate-800 rounded-xl overflow-hidden flex flex-col">
        
        {/* Filter Bar - Responsive */}
        <div className="sticky top-0 z-20 bg-slate-900/95 border-b border-slate-800 p-1.5 sm:p-2 flex flex-wrap items-center justify-between gap-1 backdrop-blur flex-shrink-0">
          <div className="flex items-center gap-0.5 sm:gap-1 flex-wrap">
            <span className="text-[8px] sm:text-[9px] text-slate-400 font-mono hidden xs:inline">Filter:</span>
            <FilterDropdown />
            <SystemDropdown />
            <div className="relative">
              <input
                type="text"
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
                placeholder="🔍"
                className="w-12 xs:w-16 sm:w-24 md:w-32 bg-slate-800 border border-slate-700 rounded-lg px-1.5 sm:px-2 py-1 sm:py-1.5 text-[9px] sm:text-[10px] font-mono text-white placeholder-slate-500 focus:outline-none focus:border-red-500/50 transition-all"
              />
              {searchQuery && (
                <button onClick={() => setSearchQuery('')} className="absolute right-0.5 sm:right-1 top-1/2 -translate-y-1/2 text-slate-500 hover:text-white">
                  <X className="w-2 h-2 sm:w-2.5 sm:h-2.5" />
                </button>
              )}
            </div>
          </div>
          <div className="flex items-center gap-1 text-[8px] sm:text-[9px] text-slate-500 font-mono flex-wrap">
            <span><span className="text-white font-bold">{filteredAlerts.length}</span>/{alerts.length}</span>
            {(filterCategory !== 'ALL' || filterSystem !== 'ALL' || searchQuery) && (
              <button
                onClick={() => { setFilterCategory('ALL'); setFilterSystem('ALL'); setSearchQuery(''); }}
                className="flex items-center gap-0.5 px-1 py-0.5 bg-slate-800 hover:bg-slate-700 rounded text-slate-400 hover:text-white transition-colors text-[7px] sm:text-[8px]"
              >
                <X className="w-1.5 h-1.5 sm:w-2 sm:h-2" /> Clear
              </button>
            )}
          </div>
        </div>

        {/* Table */}
        <div className="overflow-y-auto" style={{ maxHeight: 'calc(100vh - 320px)', minHeight: '150px' }}>
          
          {/* DESKTOP - Full Table */}
          <div className="hidden lg:block">
            <table className="w-full text-left min-w-[600px]">
              <thead className="sticky top-0 bg-slate-900/95 text-slate-400 uppercase text-[8px] sm:text-[9px] tracking-wider border-b border-slate-800 font-mono z-10 backdrop-blur">
                <tr>
                  <th className="py-1.5 px-1.5 sm:px-2">Status</th>
                  <th className="py-1.5 px-1.5 sm:px-2">System</th>
                  <th className="py-1.5 px-1.5 sm:px-2">Cat</th>
                  <th className="py-1.5 px-1.5 sm:px-2">Siren</th>
                  <th className="py-1.5 px-1.5 sm:px-2">Zones</th>
                  <th className="py-1.5 px-1.5 sm:px-2">Message</th>
                  <th className="py-1.5 px-1.5 sm:px-2">Time</th>
                  <th className="py-1.5 px-1.5 sm:px-2 text-center">Pending</th>
                  <th className="py-1.5 px-1.5 sm:px-2 text-center">Action</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-800/50">
                {filteredAlerts.map((alert) => {
                  const isPending = alert.status === 'PENDING';
                  const category = getAlertCategory(alert);
                  const zoneDisplay = alert.zoneNames || alert.zoneNumbers;
                  return (
                    <tr key={alert.id} onClick={() => handleRowClick(alert)} className="hover:bg-slate-900/40 transition-colors cursor-pointer">
                      <td className="py-1.5 px-1.5 sm:px-2"><StatusBadge status={alert.status} /></td>
                      <td className="py-1.5 px-1.5 sm:px-2 font-mono font-bold text-white text-[9px] sm:text-[10px] truncate max-w-[60px] sm:max-w-[80px]">
                        <div className="flex items-center gap-0.5 sm:gap-1">
                          {alert.alarmSystem?.systemCode || 'UNKNOWN'}
                          {renderSirenStatus(alert)}
                        </div>
                      </td>
                      <td className="py-1.5 px-1.5 sm:px-2">
                        <span className={`inline-flex items-center px-1 py-0.5 rounded-full text-[7px] sm:text-[8px] font-mono border ${getCategoryColor(category)}`}>
                          {getCategoryShort(category)}
                        </span>
                      </td>
                      <td className="py-1.5 px-1.5 sm:px-2 text-center">
                        {renderSirenStatus(alert) || (
                          <span className="text-slate-500 text-[8px] font-mono">—</span>
                        )}
                      </td>
                      <td className="py-1.5 px-1.5 sm:px-2">{renderZoneBadges(zoneDisplay)}</td>
                      <td className="py-1.5 px-1.5 sm:px-2 max-w-[100px] sm:max-w-[130px]">
                        <button onClick={(e) => handleMessageClick(e, alert)} className="flex items-center gap-0.5 sm:gap-1 text-slate-400 hover:text-white transition-colors group w-full min-w-0">
                          <span className="flex-shrink-0">{getMessageIcon(alert.alertType)}</span>
                          <span className="text-[8px] sm:text-[10px] font-mono group-hover:text-blue-400 transition-colors truncate block w-full">
                            {getMessagePreview(alert.alertType)}
                          </span>
                        </button>
                      </td>
                      <td className="py-1.5 px-1.5 sm:px-2 text-slate-400 text-[8px] sm:text-[9px] font-mono whitespace-nowrap">
                        {new Date(alert.receivedAt).toLocaleTimeString()}
                      </td>
                      <td className="py-1.5 px-1.5 sm:px-2 text-center">
                        {isPending ? (
                          <span className="inline-flex items-center gap-0.5 text-yellow-400 font-mono text-[8px] sm:text-[9px] font-bold whitespace-nowrap">
                            <Timer className="w-2 h-2 sm:w-2.5 sm:h-2.5" />
                            {getPendingDuration(alert.receivedAt)}
                          </span>
                        ) : (
                          <span className="text-[7px] sm:text-[8px] text-slate-500 font-mono">—</span>
                        )}
                      </td>
                      <td className="py-1.5 px-1.5 sm:px-2 text-center">
                        {isPending ? (
                          <button onClick={(e) => handleResolveClick(e, alert)} className="inline-flex items-center gap-0.5 px-1 py-0.5 sm:px-1.5 sm:py-0.5 bg-emerald-500/10 hover:bg-emerald-500/20 text-emerald-400 border border-emerald-500/30 rounded text-[7px] sm:text-[8px] font-mono transition-all whitespace-nowrap">
                            <CheckCircle className="w-2 h-2 sm:w-2.5 sm:h-2.5" /> ✓
                          </button>
                        ) : (
                          <span className="text-[7px] sm:text-[8px] text-slate-500 font-mono">—</span>
                        )}
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>

          {/* TABLET - Compact Table */}
          <div className="hidden sm:block lg:hidden">
            <table className="w-full text-left min-w-[450px]">
              <thead className="sticky top-0 bg-slate-900/95 text-slate-400 uppercase text-[7px] sm:text-[8px] tracking-wider border-b border-slate-800 font-mono z-10 backdrop-blur">
                <tr>
                  <th className="py-1 px-1">Status</th>
                  <th className="py-1 px-1">System</th>
                  <th className="py-1 px-1">Cat</th>
                  <th className="py-1 px-1">Siren</th>
                  <th className="py-1 px-1">Zones</th>
                  <th className="py-1 px-1">Message</th>
                  <th className="py-1 px-1">Time</th>
                  <th className="py-1 px-1 text-center">Action</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-800/50">
                {filteredAlerts.map((alert) => {
                  const isPending = alert.status === 'PENDING';
                  const category = getAlertCategory(alert);
                  const zoneDisplay = alert.zoneNames || alert.zoneNumbers;
                  return (
                    <tr key={alert.id} onClick={() => handleRowClick(alert)} className="hover:bg-slate-900/40 transition-colors cursor-pointer">
                      <td className="py-1 px-1"><StatusBadge status={alert.status} /></td>
                      <td className="py-1 px-1 font-mono font-bold text-white text-[8px] truncate max-w-[50px]">
                        {alert.alarmSystem?.systemCode || 'UNKNOWN'}
                      </td>
                      <td className="py-1 px-1">
                        <span className={`inline-flex items-center px-1 py-0.5 rounded-full text-[6px] sm:text-[7px] font-mono border ${getCategoryColor(category)}`}>
                          {getCategoryShort(category)}
                        </span>
                      </td>
                      <td className="py-1 px-1 text-center">
                        {alert.status === 'PENDING' ? (
                          alert.alarmSystem?.sirenStatus === 'ON' ? '🔔' : '🔕'
                        ) : (
                          <span className="text-slate-500 text-[6px] font-mono">—</span>
                        )}
                      </td>
                      <td className="py-1 px-1">{renderZoneBadges(zoneDisplay)}</td>
                      <td className="py-1 px-1 max-w-[80px]">
                        <button onClick={(e) => handleMessageClick(e, alert)} className="flex items-center gap-0.5 text-slate-400 hover:text-white transition-colors group w-full min-w-0">
                          <span className="flex-shrink-0">{getMessageIcon(alert.alertType)}</span>
                          <span className="text-[7px] sm:text-[8px] font-mono group-hover:text-blue-400 transition-colors truncate block w-full">
                            {getMessagePreview(alert.alertType)}
                          </span>
                        </button>
                      </td>
                      <td className="py-1 px-1 text-slate-400 text-[7px] sm:text-[8px] font-mono whitespace-nowrap">
                        {new Date(alert.receivedAt).toLocaleTimeString()}
                      </td>
                      <td className="py-1 px-1 text-center">
                        {isPending ? (
                          <button onClick={(e) => handleResolveClick(e, alert)} className="inline-flex items-center gap-0.5 px-1 py-0.5 bg-emerald-500/10 hover:bg-emerald-500/20 text-emerald-400 border border-emerald-500/30 rounded text-[6px] sm:text-[7px] font-mono transition-all whitespace-nowrap">
                            <CheckCircle className="w-1.5 h-1.5 sm:w-2 sm:h-2" /> ✓
                          </button>
                        ) : (
                          <span className="text-[6px] sm:text-[7px] text-slate-500 font-mono">—</span>
                        )}
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>

          {/* MOBILE - Cards */}
          <div className="sm:hidden divide-y divide-slate-800">
            {filteredAlerts.map((alert) => {
              const isPending = alert.status === 'PENDING';
              const category = getAlertCategory(alert);
              const zoneDisplay = alert.zoneNames || alert.zoneNumbers;
              return (
                <div key={alert.id} onClick={() => handleRowClick(alert)} className="p-2 hover:bg-slate-900/40 transition-colors cursor-pointer space-y-0.5">
                  <div className="flex items-center justify-between">
                    <div className="flex items-center gap-1 flex-wrap min-w-0">
                      <StatusBadge status={alert.status} />
                      <span className="font-mono font-bold text-white text-[8px] truncate max-w-[45px]">
                        {alert.alarmSystem?.systemCode || 'UNKNOWN'}
                      </span>
                      <span className={`inline-flex items-center px-1 py-0.5 rounded-full text-[6px] font-mono border ${getCategoryColor(category)}`}>
                        {getCategoryShort(category)}
                      </span>
                      {renderSirenStatus(alert)}
                    </div>
                    <div className="flex items-center gap-0.5 flex-shrink-0">
                      {isPending && (
                        <button onClick={(e) => handleResolveClick(e, alert)} className="p-0.5 bg-emerald-500/10 hover:bg-emerald-500/20 text-emerald-400 border border-emerald-500/30 rounded transition-all">
                          <CheckCircle className="w-2 h-2" />
                        </button>
                      )}
                      <button onClick={(e) => handleMessageClick(e, alert)} className="p-0.5 bg-slate-800 rounded hover:bg-slate-700 transition-colors flex-shrink-0">
                        {getMessageIcon(alert.alertType)}
                      </button>
                    </div>
                  </div>

                  <div className="flex flex-wrap items-center gap-0.5 text-[8px] text-slate-400 min-w-0">
                    <MapPin className="w-2 h-2 flex-shrink-0" />
                    <span className="truncate max-w-[60px]">{alert.alarmSystem?.location || 'Unknown'}</span>
                    <span className="text-slate-600 mx-0.5">•</span>
                    {renderZoneBadges(zoneDisplay)}
                  </div>

                  <div className="flex items-center justify-between gap-1 text-[8px]">
                    <button onClick={(e) => handleMessageClick(e, alert)} className="flex items-center gap-0.5 text-slate-400 hover:text-white transition-colors group min-w-0 flex-1">
                      <span className="flex-shrink-0">{getMessageIcon(alert.alertType)}</span>
                      <span className="truncate font-mono text-[7px] block w-full">
                        {getMessagePreview(alert.alertType)}
                      </span>
                    </button>
                    <span className="text-slate-500 text-[7px] font-mono flex-shrink-0">
                      {new Date(alert.receivedAt).toLocaleTimeString()}
                    </span>
                  </div>

                  {isPending && (
                    <div className="flex items-center gap-0.5 text-yellow-400 text-[7px] font-mono font-bold">
                      <Timer className="w-2 h-2" />
                      {getPendingDuration(alert.receivedAt)}
                    </div>
                  )}
                </div>
              );
            })}
          </div>

          {/* NO RESULTS */}
          {filteredAlerts.length === 0 && (
            <div className="p-4 sm:p-6 text-center">
              <div className="text-slate-500 font-mono text-xs sm:text-sm">No alerts match your filters</div>
              <button onClick={() => { setFilterCategory('ALL'); setFilterSystem('ALL'); setSearchQuery(''); }} className="mt-1 text-[8px] sm:text-[9px] text-red-400 hover:text-red-300 font-mono transition-colors">
                Clear all filters
              </button>
            </div>
          )}
        </div>
      </div>

      {/* MODALS */}
      <AlertModal alert={selectedAlert} isOpen={isModalOpen} onClose={() => setIsModalOpen(false)} />
      <AlertDetailsPanel alert={selectedAlert} isOpen={isPanelOpen} onClose={() => setIsPanelOpen(false)} onResolved={handleResolved} username={username} />
      <AlertResolveModal alert={resolveAlertData} isOpen={isResolveModalOpen} onClose={() => { setIsResolveModalOpen(false); setResolveAlertData(null); }} onResolved={handleResolved} username={username} />
    </>
  );
}

AlertTable.propTypes = {
  alerts: PropTypes.array.isRequired,
  loading: PropTypes.bool.isRequired,
  tableContainerRef: PropTypes.object,
  username: PropTypes.string.isRequired,
  onAlertResolved: PropTypes.func,
};