import { useState, useEffect, useCallback } from 'react';
import PropTypes from 'prop-types';
import { 
  X, FileText, Download, Printer,
  RefreshCw, AlertCircle, CheckCircle,
  FileSpreadsheet, Clock, Zap
} from 'lucide-react';

const API_BASE_URL = 'http://localhost:8080/api';

export default function ReportGenerator({ isOpen, onClose, user }) {
  const [reportType, setReportType] = useState('summary');
  const [dateRange, setDateRange] = useState('this_month');
  const [fromDate, setFromDate] = useState('');
  const [toDate, setToDate] = useState('');
  const [selectedSystem, setSelectedSystem] = useState('ALL');
  const [systems, setSystems] = useState([]);
  const [loading, setLoading] = useState(false);
  const [downloading, setDownloading] = useState(false);
  const [summaryData, setSummaryData] = useState(null);
  const [detailedData, setDetailedData] = useState([]);
  const [healthData, setHealthData] = useState(null);
  const [performanceData, setPerformanceData] = useState(null);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');

  // ===== GET TODAY'S DATE IN LOCAL TIMEZONE =====
  const getLocalDateStr = (date) => {
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');
    return `${year}-${month}-${day}`;
  };

  // ===== LOAD SYSTEMS =====
  const loadSystems = useCallback(async () => {
    try {
      const params = new URLSearchParams();
      if (user.role === 'USER') {
        params.append('username', user.username);
      }
      const response = await fetch(`${API_BASE_URL}/reports/systems?${params}`);
      if (response.ok) {
        const data = await response.json();
        setSystems(data);
      }
    } catch (err) {
      console.error('Failed to load systems:', err);
    }
  }, [user.role, user.username]);

  // ===== SET DEFAULT DATES =====
  const setDefaultDates = () => {
    const now = new Date();
    const from = new Date();
    from.setDate(now.getDate() - 30);
    setFromDate(getLocalDateStr(from));
    setToDate(getLocalDateStr(now));
  };

  // ===== UPDATE DATES FOR RANGE =====
  const updateDatesForRange = (range) => {
    const now = new Date();
    const from = new Date();

    switch(range) {
      case 'today':
        setFromDate(getLocalDateStr(now));
        setToDate(getLocalDateStr(now));
        break;
      case 'this_week':
        from.setDate(now.getDate() - 7);
        setFromDate(getLocalDateStr(from));
        setToDate(getLocalDateStr(now));
        break;
      case 'this_month':
        from.setDate(now.getDate() - 30);
        setFromDate(getLocalDateStr(from));
        setToDate(getLocalDateStr(now));
        break;
      case 'last_month':
        from.setDate(now.getDate() - 60);
        setFromDate(getLocalDateStr(from));
        setToDate(getLocalDateStr(now));
        break;
      case 'custom':
        break;
      default:
        break;
    }
  };

  // ===== HANDLE DATE RANGE CHANGE =====
  const handleDateRangeChange = (range) => {
    setDateRange(range);
    updateDatesForRange(range);
  };

  // ===== GENERATE REPORT BASED ON TYPE =====
  const generateReport = useCallback(async () => {
    if (!fromDate || !toDate) {
      setError('Please select valid dates');
      return;
    }

    setLoading(true);
    setError('');
    setSuccess('');

    try {
      const params = new URLSearchParams();
      params.append('from', fromDate);
      params.append('to', toDate);
      if (user.role === 'USER') {
        params.append('username', user.username);
      }
      if (selectedSystem !== 'ALL') {
        params.append('systemCode', selectedSystem);
      }

      let endpoint = '';
      let data = null;

      switch(reportType) {
        case 'summary':
          endpoint = `${API_BASE_URL}/reports/summary`;
          break;
        case 'detailed':
          endpoint = `${API_BASE_URL}/reports/detailed`;
          break;
        case 'health':
          endpoint = `${API_BASE_URL}/reports/health?${params}`;
          break;
        case 'performance':
          endpoint = `${API_BASE_URL}/reports/performance?${params}`;
          break;
        default:
          endpoint = `${API_BASE_URL}/reports/summary`;
      }

      const response = await fetch(`${endpoint}?${params}`);
      if (!response.ok) {
        const errorText = await response.text();
        throw new Error(errorText || 'Failed to generate report');
      }
      
      data = await response.json();

      // Set data based on report type
      switch(reportType) {
        case 'summary':
          setSummaryData(data);
          setDetailedData([]);
          setHealthData(null);
          setPerformanceData(null);
          break;
        case 'detailed':
          setDetailedData(data);
          setSummaryData(null);
          setHealthData(null);
          setPerformanceData(null);
          break;
        case 'health':
          setHealthData(data);
          setSummaryData(null);
          setDetailedData([]);
          setPerformanceData(null);
          break;
        case 'performance':
          setPerformanceData(data);
          setSummaryData(null);
          setDetailedData([]);
          setHealthData(null);
          break;
        default:
          setSummaryData(data);
      }

      setSuccess('✅ Report generated successfully!');
      setTimeout(() => setSuccess(''), 3000);
    } catch (err) {
      setError(err.message || 'Failed to generate report');
    } finally {
      setLoading(false);
    }
  }, [fromDate, toDate, user.role, user.username, selectedSystem, reportType]);

  // ===== AUTO-GENERATE ON REPORT TYPE CHANGE =====
  useEffect(() => {
    if (isOpen && fromDate && toDate) {
      generateReport();
    }
  }, [reportType, isOpen, fromDate, toDate, selectedSystem, generateReport]);

  // ===== LOAD ON OPEN =====
  useEffect(() => {
    if (isOpen) {
      setDefaultDates();
      loadSystems();
    }
  }, [isOpen, loadSystems]);

  // ===== FORMAT DATE =====
  const formatDateDisplay = (dateStr) => {
    if (!dateStr) return '';
    try {
      const parts = dateStr.split('-');
      const d = new Date(parts[0], parts[1] - 1, parts[2]);
      return d.toLocaleDateString('en-US', { 
        day: '2-digit', 
        month: 'short', 
        year: 'numeric' 
      });
    } catch {
      return dateStr;
    }
  };

  // ===== DOWNLOAD REPORT =====
  const downloadReport = async (type) => {
    setDownloading(true);
    setError('');
    setSuccess('');

    try {
      const params = new URLSearchParams();
      params.append('from', fromDate);
      params.append('to', toDate);
      if (user.role === 'USER') {
        params.append('username', user.username);
      }
      if (selectedSystem !== 'ALL') {
        params.append('systemCode', selectedSystem);
      }
      params.append('reportType', reportType);

      let endpoint = '';
      let filename = '';

      if (type === 'pdf') {
        endpoint = `${API_BASE_URL}/reports/export/pdf`;
        filename = `Alarm_Report_${reportType}_${new Date().toISOString().split('T')[0]}.pdf`;
      } else if (type === 'excel') {
        endpoint = `${API_BASE_URL}/reports/export/excel`;
        filename = `Alarm_Report_${new Date().toISOString().split('T')[0]}.xlsx`;
      }

      const response = await fetch(`${endpoint}?${params}`);
      
      if (!response.ok) {
        const errorText = await response.text();
        throw new Error(errorText || 'Failed to download report');
      }

      const blob = await response.blob();
      const url = window.URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href = url;
      link.download = filename;
      document.body.appendChild(link);
      link.click();
      document.body.removeChild(link);
      window.URL.revokeObjectURL(url);

      setSuccess(`✅ ${type.toUpperCase()} downloaded successfully!`);
      setTimeout(() => setSuccess(''), 3000);
    } catch (err) {
      setError(err.message || 'Failed to download report');
    } finally {
      setDownloading(false);
    }
  };

  const printReport = () => {
    window.print();
  };

  if (!isOpen) return null;

  // ===== RENDER CONTENT BASED ON REPORT TYPE =====
  const renderContent = () => {
    if (loading) {
      return (
        <div className="text-center py-8">
          <div className="w-8 h-8 border-2 border-blue-500/30 border-t-blue-500 rounded-full animate-spin mx-auto mb-2" />
          <p className="text-slate-400 text-sm font-mono">Loading report data...</p>
        </div>
      );
    }

    switch(reportType) {
      case 'summary':
        return renderSummary();
      case 'detailed':
        return renderDetailed();
      case 'health':
        return renderHealth();
      case 'performance':
        return renderPerformance();
      default:
        return renderSummary();
    }
  };

  // ===== RENDER SUMMARY =====
  const renderSummary = () => {
    if (!summaryData) return null;
    return (
      <div className="space-y-4 print:block">
        <div className="grid grid-cols-2 sm:grid-cols-4 gap-3">
          <StatCard label="Total Alerts" value={summaryData.totalAlerts || 0} color="blue" icon={<AlertCircle className="w-5 h-5" />} />
          <StatCard label="Pending" value={summaryData.pending || 0} color="red" icon={<Clock className="w-5 h-5" />} />
          <StatCard label="Resolved" value={summaryData.resolved || 0} color="green" icon={<CheckCircle className="w-5 h-5" />} />
          <StatCard label="Avg Resolution" value={formatDuration(summaryData.avgResolutionSeconds)} color="yellow" icon={<Zap className="w-5 h-5" />} />
        </div>

        {/* By System */}
        {summaryData.bySystem && Object.keys(summaryData.bySystem).length > 0 && (
          <div className="bg-slate-950/50 border border-slate-800 rounded-xl p-4">
            <h3 className="text-sm font-bold text-white mb-3">📊 Alerts by System</h3>
            <div className="space-y-2">
              {Object.entries(summaryData.bySystem).map(([system, count]) => {
                const total = summaryData.totalAlerts || 1;
                const pct = (count / total) * 100;
                return (
                  <div key={system} className="flex items-center gap-2">
                    <span className="text-sm text-slate-300 font-mono w-32 truncate">{system}</span>
                    <div className="flex-1 bg-slate-800 rounded-full h-2 overflow-hidden">
                      <div className="h-full bg-blue-500 rounded-full transition-all" style={{ width: `${Math.min(pct, 100)}%`, minWidth: '4px' }} />
                    </div>
                    <span className="text-sm text-slate-400 font-mono w-12 text-right">{count}</span>
                  </div>
                );
              })}
            </div>
          </div>
        )}

        {/* By Zone */}
        {summaryData.byZone && Object.keys(summaryData.byZone).length > 0 && (
          <div className="bg-slate-950/50 border border-slate-800 rounded-xl p-4">
            <h3 className="text-sm font-bold text-white mb-3">📍 Alerts by Zone</h3>
            <div className="grid grid-cols-2 sm:grid-cols-4 gap-2">
              {Object.entries(summaryData.byZone)
                .sort((a, b) => b[1] - a[1])
                .slice(0, 8)
                .map(([zone, count]) => (
                  <div key={zone} className="flex justify-between items-center bg-slate-800/50 rounded-lg px-3 py-2">
                    <span className="text-xs text-slate-300 truncate">{zone}</span>
                    <span className="text-xs font-bold text-amber-400">{count}</span>
                  </div>
                ))}
            </div>
          </div>
        )}

        {/* Resolved By */}
        {summaryData.resolvedBy && Object.keys(summaryData.resolvedBy).length > 0 && (
          <div className="bg-slate-950/50 border border-slate-800 rounded-xl p-4">
            <h3 className="text-sm font-bold text-white mb-3">👤 Resolved By</h3>
            <div className="flex flex-wrap gap-2">
              {Object.entries(summaryData.resolvedBy).map(([user, count]) => (
                <div key={user} className="flex items-center gap-2 bg-slate-800/50 rounded-lg px-3 py-2">
                  <span className="text-xs text-slate-300">{user}</span>
                  <span className="text-xs font-bold text-emerald-400">{count}</span>
                </div>
              ))}
            </div>
          </div>
        )}

        {/* Daily Trend */}
        {summaryData.dailyTrend && Object.keys(summaryData.dailyTrend).length > 0 && (
          <div className="bg-slate-950/50 border border-slate-800 rounded-xl p-4">
            <h3 className="text-sm font-bold text-white mb-3">📈 Daily Trend</h3>
            <div className="flex items-end gap-1 h-32 overflow-x-auto">
              {Object.entries(summaryData.dailyTrend)
                .sort((a, b) => a[0].localeCompare(b[0]))
                .slice(-14)
                .map(([date, count]) => {
                  const values = Object.values(summaryData.dailyTrend);
                  const max = Math.max(...values);
                  const height = max > 0 ? (count / max) * 100 : 0;
                  return (
                    <div key={date} className="flex-1 flex flex-col items-center gap-1 min-w-[30px]">
                      <div className="w-full bg-blue-500/50 rounded-t transition-all" style={{ height: `${Math.max(height, 4)}%` }} />
                      <span className="text-[8px] text-slate-500 font-mono">{date.substring(5)}</span>
                      <span className="text-[8px] text-slate-400 font-mono">{count}</span>
                    </div>
                  );
                })}
            </div>
          </div>
        )}
      </div>
    );
  };

  // ===== RENDER DETAILED =====
  const renderDetailed = () => {
    if (!detailedData || detailedData.length === 0) {
      return <div className="text-center text-slate-400 py-8">No detailed data available</div>;
    }
    return (
      <div className="overflow-x-auto">
        <table className="w-full text-sm">
          <thead className="bg-slate-800/50 text-slate-400 text-xs uppercase font-mono">
            <tr>
              <th className="px-3 py-2 text-left">ID</th>
              <th className="px-3 py-2 text-left">System</th>
              <th className="px-3 py-2 text-left">Zones</th>
              <th className="px-3 py-2 text-left">Status</th>
              <th className="px-3 py-2 text-left">Received</th>
              <th className="px-3 py-2 text-left">Resolved By</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-slate-800/50">
            {detailedData.slice(0, 50).map((alert) => (
              <tr key={alert.id} className="hover:bg-slate-900/40">
                <td className="px-3 py-2 text-slate-400 font-mono text-xs">#{alert.id}</td>
                <td className="px-3 py-2 text-white font-mono text-xs">{alert.alarmSystem?.systemCode || 'UNKNOWN'}</td>
                <td className="px-3 py-2 text-slate-300 text-xs">{alert.zoneNumbers || '00'}</td>
                <td className="px-3 py-2"><StatusBadge status={alert.status} /></td>
                <td className="px-3 py-2 text-slate-400 text-xs">{new Date(alert.receivedAt).toLocaleString()}</td>
                <td className="px-3 py-2 text-slate-300 text-xs">{alert.resolvedBy || '-'}</td>
              </tr>
            ))}
          </tbody>
        </table>
        {detailedData.length > 50 && (
          <div className="text-center text-slate-500 text-xs py-2">Showing 50 of {detailedData.length} records</div>
        )}
      </div>
    );
  };

  // ===== RENDER HEALTH =====
  const renderHealth = () => {
    if (!healthData) return null;
    const systems = healthData.systems || [];
    return (
      <div className="space-y-4">
        <div className="grid grid-cols-2 sm:grid-cols-4 gap-3">
          <StatCard label="Total Systems" value={healthData.totalSystems || 0} color="blue" icon={<AlertCircle className="w-5 h-5" />} />
          <StatCard label="Active Systems" value={healthData.activeSystems || 0} color="green" icon={<CheckCircle className="w-5 h-5" />} />
          <StatCard label="Total Zones" value={healthData.totalZones || 0} color="yellow" icon={<Zap className="w-5 h-5" />} />
          <StatCard label="Active Zones" value={healthData.activeZones || 0} color="green" icon={<CheckCircle className="w-5 h-5" />} />
        </div>

        {systems.map((system) => (
          <div key={system.systemCode} className="bg-slate-950/50 border border-slate-800 rounded-xl p-4">
            <div className="flex items-center justify-between mb-2">
              <div>
                <span className="font-mono font-bold text-white">{system.systemCode}</span>
                <span className={`ml-2 text-xs px-2 py-0.5 rounded-full border ${
                  system.status === 'ACTIVE' 
                    ? 'bg-emerald-500/10 text-emerald-400 border-emerald-500/20' 
                    : 'bg-red-500/10 text-red-400 border-red-500/20'
                }`}>{system.status}</span>
              </div>
              <span className="text-xs text-slate-400">{system.location}</span>
            </div>
            <div className="flex gap-4 text-xs text-slate-400">
              <span>Zones: <span className="text-white">{system.totalZones}</span></span>
              <span>Active: <span className="text-emerald-400">{system.activeZones}</span></span>
              <span>Inactive: <span className="text-red-400">{system.inactiveZones}</span></span>
            </div>
          </div>
        ))}
      </div>
    );
  };

  // ===== RENDER PERFORMANCE =====
  const renderPerformance = () => {
    if (!performanceData) return null;
    const resolvedBy = performanceData.resolvedBy || {};
    const avgTime = performanceData.averageTime || {};
    
    if (Object.keys(resolvedBy).length === 0) {
      return <div className="text-center text-slate-400 py-8">No performance data available</div>;
    }

    return (
      <div className="space-y-4">
        <div className="grid grid-cols-2 sm:grid-cols-3 gap-3">
          <StatCard label="Total Resolved" value={performanceData.totalResolved || 0} color="green" icon={<CheckCircle className="w-5 h-5" />} />
          <StatCard label="Total Pending" value={performanceData.totalPending || 0} color="red" icon={<Clock className="w-5 h-5" />} />
        </div>

        <div className="bg-slate-950/50 border border-slate-800 rounded-xl p-4">
          <h3 className="text-sm font-bold text-white mb-3">👤 User Resolution Performance</h3>
          <div className="space-y-2">
            {Object.entries(resolvedBy).map(([user, count]) => {
              const avg = avgTime[user] || 0;
              const avgStr = formatDuration(Math.round(avg));
              return (
                <div key={user} className="flex items-center justify-between bg-slate-800/30 rounded-lg px-3 py-2">
                  <span className="text-sm text-slate-300 font-mono">{user}</span>
                  <div className="flex items-center gap-4">
                    <span className="text-xs text-slate-400">Resolved: <span className="text-emerald-400 font-bold">{count}</span></span>
                    <span className="text-xs text-slate-400">Avg Time: <span className="text-yellow-400 font-bold">{avgStr}</span></span>
                  </div>
                </div>
              );
            })}
          </div>
        </div>
      </div>
    );
  };

  return (
    <div className="fixed inset-0 z-[160] flex items-center justify-center p-4 bg-black/80 backdrop-blur-sm">
      <div className="bg-slate-900 border border-slate-700 rounded-2xl max-w-5xl w-full max-h-[90vh] overflow-hidden shadow-2xl shadow-blue-500/10">
        
        {/* HEADER */}
        <div className="flex justify-between items-center p-5 border-b border-slate-800 bg-slate-950/40 sticky top-0 z-10">
          <div className="flex items-center gap-3">
            <FileText className="w-6 h-6 text-blue-400" />
            <h2 className="text-xl font-bold text-white">📊 Report Generator</h2>
          </div>
          <button onClick={onClose} className="p-2 hover:bg-slate-800 rounded-lg transition-colors">
            <X className="w-5 h-5 text-slate-400 hover:text-white" />
          </button>
        </div>

        {/* CONTENT */}
        <div className="p-5 overflow-y-auto max-h-[calc(90vh-80px)]">
          
          {/* SUCCESS/ERROR */}
          {success && (
            <div className="bg-emerald-500/10 border border-emerald-500/30 rounded-xl p-3 flex items-start gap-2.5 text-sm text-emerald-400 mb-4">
              <CheckCircle className="w-4 h-4 mt-0.5 flex-shrink-0" />
              <span>{success}</span>
            </div>
          )}
          {error && (
            <div className="bg-red-500/10 border border-red-500/30 rounded-xl p-3 flex items-start gap-2.5 text-sm text-red-400 mb-4">
              <AlertCircle className="w-4 h-4 mt-0.5 flex-shrink-0" />
              <span>{error}</span>
            </div>
          )}

          {/* REPORT TYPE CARDS */}
          <div className="mb-6">
            <label className="text-xs font-bold tracking-wide uppercase text-slate-400 font-mono block mb-2">Report Type</label>
            <div className="grid grid-cols-2 sm:grid-cols-4 gap-3">
              {[
                { id: 'summary', label: '📊 Summary', desc: 'Overview statistics' },
                { id: 'detailed', label: '📋 Detailed', desc: 'All alerts list' },
                { id: 'health', label: '💚 Health', desc: 'System status' },
                { id: 'performance', label: '👤 Performance', desc: 'User activity' }
              ].map((type) => (
                <button
                  key={type.id}
                  onClick={() => setReportType(type.id)}
                  className={`p-3 rounded-xl border text-left transition-all ${
                    reportType === type.id
                      ? 'bg-blue-500/10 border-blue-500/50 text-white shadow-lg shadow-blue-500/10'
                      : 'bg-slate-950 border-slate-800 text-slate-400 hover:border-slate-600'
                  }`}
                >
                  <div className="text-sm font-bold">{type.label}</div>
                  <div className="text-[10px] text-slate-500">{type.desc}</div>
                </button>
              ))}
            </div>
          </div>

          {/* DATE RANGE */}
          <div className="mb-6">
            <label className="text-xs font-bold tracking-wide uppercase text-slate-400 font-mono block mb-2">📅 Date Range</label>
            <div className="flex flex-wrap gap-2 mb-3">
              {[
                { id: 'today', label: 'Today' },
                { id: 'this_week', label: 'This Week' },
                { id: 'this_month', label: 'This Month' },
                { id: 'last_month', label: 'Last Month' },
                { id: 'custom', label: 'Custom' }
              ].map((range) => (
                <button
                  key={range.id}
                  onClick={() => handleDateRangeChange(range.id)}
                  className={`px-3 py-1.5 rounded-lg text-xs font-mono transition-all ${
                    dateRange === range.id
                      ? 'bg-blue-500/20 text-blue-400 border border-blue-500/50'
                      : 'bg-slate-800 text-slate-400 border border-slate-700 hover:border-slate-500'
                  }`}
                >
                  {range.label}
                </button>
              ))}
            </div>

            <div className="flex flex-wrap items-center gap-4">
              <div>
                <label className="text-[10px] text-slate-500 font-mono block">From</label>
                <div className="flex items-center gap-2">
                  <input
                    type="date"
                    value={fromDate}
                    onChange={(e) => {
                      setFromDate(e.target.value);
                      setDateRange('custom');
                    }}
                    className={`bg-slate-950 border border-slate-800 rounded-lg px-3 py-1.5 text-sm text-white focus:outline-none focus:border-blue-500/50 ${
                      dateRange !== 'custom' ? 'opacity-50 cursor-not-allowed' : ''
                    }`}
                    disabled={dateRange !== 'custom'}
                  />
                  <span className="text-slate-500 text-sm font-mono hidden sm:inline">{formatDateDisplay(fromDate)}</span>
                </div>
              </div>
              <span className="text-slate-600 text-sm">→</span>
              <div>
                <label className="text-[10px] text-slate-500 font-mono block">To</label>
                <div className="flex items-center gap-2">
                  <input
                    type="date"
                    value={toDate}
                    onChange={(e) => {
                      setToDate(e.target.value);
                      setDateRange('custom');
                    }}
                    className={`bg-slate-950 border border-slate-800 rounded-lg px-3 py-1.5 text-sm text-white focus:outline-none focus:border-blue-500/50 ${
                      dateRange !== 'custom' ? 'opacity-50 cursor-not-allowed' : ''
                    }`}
                    disabled={dateRange !== 'custom'}
                  />
                  <span className="text-slate-500 text-sm font-mono hidden sm:inline">{formatDateDisplay(toDate)}</span>
                </div>
              </div>
            </div>
          </div>

          {/* SYSTEM FILTER */}
          <div className="mb-6">
            <label className="text-xs font-bold tracking-wide uppercase text-slate-400 font-mono block mb-2">🔍 System</label>
            <select
              value={selectedSystem}
              onChange={(e) => setSelectedSystem(e.target.value)}
              className="w-full sm:w-64 bg-slate-950 border border-slate-800 rounded-xl px-4 py-2.5 text-sm text-white focus:outline-none focus:border-blue-500/50"
            >
              <option value="ALL">📊 All Systems</option>
              {systems.map((sys) => (
                <option key={sys.id} value={sys.systemCode}>{sys.systemCode}</option>
              ))}
            </select>
          </div>

          {/* ACTIONS */}
          <div className="flex flex-wrap gap-3 mb-6">
            <button
              onClick={generateReport}
              disabled={loading}
              className="flex items-center gap-2 px-5 py-2.5 bg-blue-600 hover:bg-blue-500 text-white font-bold rounded-xl text-sm transition-all disabled:opacity-50"
            >
              {loading ? (
                <span className="w-4 h-4 border-2 border-white/30 border-t-white rounded-full animate-spin" />
              ) : (
                <RefreshCw className="w-4 h-4" />
              )}
              Generate
            </button>
            <button
              onClick={() => downloadReport('pdf')}
              disabled={downloading || !summaryData}
              className="flex items-center gap-2 px-5 py-2.5 bg-red-600 hover:bg-red-500 text-white font-bold rounded-xl text-sm transition-all disabled:opacity-50"
            >
              {downloading ? (
                <span className="w-4 h-4 border-2 border-white/30 border-t-white rounded-full animate-spin" />
              ) : (
                <Download className="w-4 h-4" />
              )}
              Download PDF
            </button>
            <button
              onClick={() => downloadReport('excel')}
              disabled={downloading || !summaryData}
              className="flex items-center gap-2 px-5 py-2.5 bg-emerald-600 hover:bg-emerald-500 text-white font-bold rounded-xl text-sm transition-all disabled:opacity-50"
            >
              {downloading ? (
                <span className="w-4 h-4 border-2 border-white/30 border-t-white rounded-full animate-spin" />
              ) : (
                <FileSpreadsheet className="w-4 h-4" />
              )}
              Download Excel
            </button>
            <button
              onClick={printReport}
              disabled={!summaryData}
              className="flex items-center gap-2 px-5 py-2.5 bg-slate-700 hover:bg-slate-600 text-white font-bold rounded-xl text-sm transition-all disabled:opacity-50"
            >
              <Printer className="w-4 h-4" />
              Print
            </button>
            <button
              onClick={onClose}
              className="flex items-center gap-2 px-5 py-2.5 border border-slate-700 text-slate-400 hover:text-white rounded-xl text-sm transition-all"
            >
              <X className="w-4 h-4" />
              Close
            </button>
          </div>

          {/* REPORT CONTENT */}
          {renderContent()}
        </div>
      </div>
    </div>
  );
}

// ===== STAT CARD =====
function StatCard({ label, value, color, icon }) {
  const colors = {
    blue: 'bg-blue-500/10 border-blue-500/20 text-blue-400',
    red: 'bg-red-500/10 border-red-500/20 text-red-400',
    green: 'bg-emerald-500/10 border-emerald-500/20 text-emerald-400',
    yellow: 'bg-yellow-500/10 border-yellow-500/20 text-yellow-400'
  };
  return (
    <div className={`p-3 rounded-xl border ${colors[color]}`}>
      <div className="flex items-center gap-2">
        {icon}
        <div>
          <p className="text-xs text-slate-400">{label}</p>
          <p className="text-xl font-bold">{value}</p>
        </div>
      </div>
    </div>
  );
}

StatCard.propTypes = {
  label: PropTypes.string.isRequired,
  value: PropTypes.oneOfType([PropTypes.string, PropTypes.number]).isRequired,
  color: PropTypes.string.isRequired,
  icon: PropTypes.node.isRequired,
};

// ===== STATUS BADGE =====
function StatusBadge({ status }) {
  if (status === 'PENDING') {
    return (
      <span className="inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-[10px] font-semibold bg-red-500/10 text-red-400 border border-red-500/20">
        <span className="w-1.5 h-1.5 rounded-full bg-red-500 animate-ping" />
        PENDING
      </span>
    );
  }
  if (status === 'RESOLVED') {
    return (
      <span className="inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-[10px] font-semibold bg-emerald-500/10 text-emerald-400 border border-emerald-500/20">
        <span className="w-1.5 h-1.5 rounded-full bg-emerald-500" />
        RESOLVED
      </span>
    );
  }
  if (status === 'CALL') {
    return (
      <span className="inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-[10px] font-semibold bg-blue-500/10 text-blue-400 border border-blue-500/20">
        📞 CALL
      </span>
    );
  }
  if (status === 'ARMED') {
    return (
      <span className="inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-[10px] font-semibold bg-yellow-500/10 text-yellow-400 border border-yellow-500/20">
        ARMED
      </span>
    );
  }
  return (
    <span className="inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-[10px] font-semibold bg-slate-500/10 text-slate-400 border border-slate-500/20">
      {status || 'UNKNOWN'}
    </span>
  );
}

StatusBadge.propTypes = {
  status: PropTypes.string,
};

function formatDuration(seconds) {
  if (!seconds || seconds === 0) return 'N/A';
  const mins = Math.floor(seconds / 60);
  const secs = Math.floor(seconds % 60);
  if (mins > 60) {
    const hours = Math.floor(mins / 60);
    const remainingMins = mins % 60;
    return `${hours}h ${remainingMins}m`;
  }
  return `${mins}m ${secs}s`;
}

ReportGenerator.propTypes = {
  isOpen: PropTypes.bool.isRequired,
  onClose: PropTypes.func.isRequired,
  user: PropTypes.shape({
    username: PropTypes.string.isRequired,
    role: PropTypes.string.isRequired,
  }).isRequired,
};