import { useState, useEffect, useCallback, useRef } from 'react';
import PropTypes from 'prop-types';
import { 
  X, FileText, Download, Printer,
  RefreshCw, AlertCircle, CheckCircle,
  FileSpreadsheet, Clock, Zap, Calendar,
  ChevronLeft, ChevronRight, Search,
  Filter, ChevronDown
} from 'lucide-react';

import './ReportGenerator.css';

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api';

export default function ReportGenerator({ isOpen, onClose, user }) {
  const [reportType, setReportType] = useState('');
  const [selectedType, setSelectedType] = useState(null);
  const [showPreview, setShowPreview] = useState(false);
  const [previewData, setPreviewData] = useState(null);
  const [previewLoading, setPreviewLoading] = useState(false);
  const [dateRange, setDateRange] = useState('this_month');
  const [fromDate, setFromDate] = useState('');
  const [toDate, setToDate] = useState('');
  const [selectedSystem, setSelectedSystem] = useState('ALL');
  const [selectedStatus, setSelectedStatus] = useState('ALL');
  const [systems, setSystems] = useState([]);
  const [systemsLoading, setSystemsLoading] = useState(false);
  const [loading, setLoading] = useState(false);
  const [downloading, setDownloading] = useState(false);
  const [summaryData, setSummaryData] = useState(null);
  const [detailedData, setDetailedData] = useState([]);
  const [healthData, setHealthData] = useState(null);
  const [performanceData, setPerformanceData] = useState(null);
  const [alertLogsData, setAlertLogsData] = useState(null);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');
  
  // ===== ALERT LOGS PAGINATION =====
  const [currentPage, setCurrentPage] = useState(0);
  const [pageSize, setPageSize] = useState(50);
  const [totalPages, setTotalPages] = useState(0);
  const [totalRecords, setTotalRecords] = useState(0);
  const [searchQuery, setSearchQuery] = useState('');

  const fromInputRef = useRef(null);
  const toInputRef = useRef(null);

  // ===== GET LOCAL DATE STRING =====
  const getLocalDateStr = (date) => {
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');
    return `${year}-${month}-${day}`;
  };

  // ===== UPDATE DATES FOR RANGE =====
  const updateDatesForRange = (range) => {
    const now = new Date();
    let from = new Date();
    let to = new Date();

    switch(range) {
      case 'today':
        from = new Date(now);
        to = new Date(now);
        break;
      case 'this_week': {
        const jsDay = now.getDay();
        const isoDay = jsDay === 0 ? 7 : jsDay;
        from = new Date(now);
        from.setDate(now.getDate() - (isoDay - 1));
        from.setHours(0, 0, 0, 0);
        to = new Date(from);
        to.setDate(from.getDate() + 6);
        to.setHours(23, 59, 59, 999);
        break;
      }
      case 'this_month':
        from = new Date(now.getFullYear(), now.getMonth(), 1);
        to = new Date(now.getFullYear(), now.getMonth() + 1, 0);
        break;
      case 'last_month':
        from = new Date(now.getFullYear(), now.getMonth() - 1, 1);
        to = new Date(now.getFullYear(), now.getMonth(), 0);
        break;
      case 'custom':
        setFromDate('');
        setToDate('');
        return;
      default:
        break;
    }

    setFromDate(getLocalDateStr(from));
    setToDate(getLocalDateStr(to));
  };

  // ===== LOAD SYSTEMS =====
  const loadSystems = useCallback(async () => {
    setSystemsLoading(true);
    try {
      const params = new URLSearchParams();
      if (user.role === 'USER') {
        params.append('username', user.username);
      }
      const url = `${API_BASE_URL}/reports/systems${params.toString() ? ('?' + params.toString()) : ''}`;
      const response = await fetch(url);
      if (response.ok) {
        const data = await response.json();
        setSystems(Array.isArray(data) ? data : []);
      } else {
        setSystems([]);
      }
    } catch (err) {
      console.error('Failed to load systems:', err);
      setSystems([]);
    } finally {
      setSystemsLoading(false);
    }
  }, [user.role, user.username]);

  // ===== FETCH PREVIEW DATA =====
  const fetchPreviewData = useCallback(async (typeId) => {
    if (!typeId || !fromDate || !toDate) return null;

    const params = new URLSearchParams();
    params.append('reportType', typeId);
    params.append('from', fromDate);
    params.append('to', toDate);
    if (user.role === 'USER') {
      params.append('username', user.username);
    }
    if (selectedSystem !== 'ALL') {
      params.append('systemCode', selectedSystem);
    }
    if (selectedStatus !== 'ALL' && typeId === 'alert-logs') {
      params.append('status', selectedStatus);
    }

    const response = await fetch(`${API_BASE_URL}/reports/preview?${params}`);
    if (!response.ok) {
      const errorText = await response.text();
      throw new Error(errorText || 'Failed to fetch preview');
    }
    return await response.json();
  }, [fromDate, toDate, user.role, user.username, selectedSystem, selectedStatus]);

  // ===== REPORT TYPE SELECT HANDLER =====
  const handleTypeSelect = async (typeId) => {
    setSelectedType(typeId);
    setReportType(typeId);
    setPreviewLoading(true);
    setError('');

    try {
      const preview = await fetchPreviewData(typeId);
      setPreviewData(preview);
      setShowPreview(true);
    } catch (err) {
      console.error('Failed to load preview:', err);
      setPreviewData({ reportType: typeId });
      setShowPreview(true);
    } finally {
      setPreviewLoading(false);
    }
  };

  // ===== REAL-TIME PREVIEW UPDATE ON FILTER CHANGE =====
  useEffect(() => {
    if (!isOpen || !selectedType || !showPreview || !fromDate || !toDate) return;

    let isMounted = true;
    setPreviewLoading(true);

    const timer = setTimeout(async () => {
      try {
        const preview = await fetchPreviewData(selectedType);
        if (isMounted && preview) {
          setPreviewData(preview);
        }
      } catch (err) {
        console.error('Failed to update preview in real-time:', err);
      } finally {
        if (isMounted) setPreviewLoading(false);
      }
    }, 250);

    return () => {
      isMounted = false;
      clearTimeout(timer);
    };
  }, [isOpen, selectedType, showPreview, fromDate, toDate, selectedSystem, selectedStatus, fetchPreviewData]);

  // ===== CANCEL PREVIEW HANDLER =====
  const handleCancelPreview = () => {
    setShowPreview(false);
    setPreviewData(null);
    setSelectedType(null);
    setReportType('');
    setSummaryData(null);
    setDetailedData([]);
    setHealthData(null);
    setPerformanceData(null);
    setAlertLogsData(null);
  };

  // ===== GENERATE REPORT =====
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
      params.append('reportType', reportType || 'summary');
      params.append('from', fromDate);
      params.append('to', toDate);
      if (user.role === 'USER') {
        params.append('username', user.username);
      }
      if (selectedSystem !== 'ALL') {
        params.append('systemCode', selectedSystem);
      }
      if (selectedStatus !== 'ALL' && reportType === 'alert-logs') {
        params.append('status', selectedStatus);
      }
      params.append('saveToDb', 'true');
      
      // Add pagination for alert logs
      if (reportType === 'alert-logs') {
        params.append('page', currentPage);
        params.append('size', pageSize);
      }

      let endpoint = `${API_BASE_URL}/reports/generate`;

      const response = await fetch(`${endpoint}?${params}`, {
        method: 'POST'
      });
      
      if (!response.ok) {
        const errorText = await response.text();
        throw new Error(errorText || 'Failed to generate report');
      }
      
      const data = await response.json();

      switch(reportType) {
        case 'summary':
          setSummaryData(data);
          setDetailedData([]);
          setHealthData(null);
          setPerformanceData(null);
          setAlertLogsData(null);
          break;
        case 'detailed':
          setDetailedData(data.alerts || []);
          setSummaryData(null);
          setHealthData(null);
          setPerformanceData(null);
          setAlertLogsData(null);
          break;
        case 'health':
          setHealthData(data);
          setSummaryData(null);
          setDetailedData([]);
          setPerformanceData(null);
          setAlertLogsData(null);
          break;
        case 'performance':
          setPerformanceData(data);
          setSummaryData(null);
          setDetailedData([]);
          setHealthData(null);
          setAlertLogsData(null);
          break;
        case 'alert-logs':
          setAlertLogsData(data);
          setSummaryData(null);
          setDetailedData([]);
          setHealthData(null);
          setPerformanceData(null);
          if (data.totalPages !== undefined) {
            setTotalPages(data.totalPages);
          }
          if (data.totalRecords !== undefined) {
            setTotalRecords(data.totalRecords);
          }
          break;
        default:
          setSummaryData(data);
      }

      const saveMsg = data.savedReportId ? ' ✅ Saved to database!' : '';
      setSuccess(`✅ Report generated successfully!${saveMsg}`);
      setShowPreview(false);
      setTimeout(() => setSuccess(''), 3000);
    } catch (err) {
      setError(err.message || 'Failed to generate report');
    } finally {
      setLoading(false);
    }
  }, [fromDate, toDate, user.role, user.username, selectedSystem, selectedStatus, reportType, currentPage, pageSize]);

  // ============================================================
  // 🔥 AUTO-GENERATE REMOVED - User must click Generate button
  // ============================================================
  // useEffect(() => {
  //   if (isOpen && fromDate && toDate) {
  //     generateReport();
  //   }
  // }, [reportType, isOpen, fromDate, toDate, selectedSystem, selectedStatus, currentPage, generateReport]);

  // ============================================================
  // LOAD ON OPEN - Only set default dates, no auto-generate
  // ============================================================
  useEffect(() => {
    if (isOpen) {
      setDefaultDates();
      loadSystems();
      setCurrentPage(0);
      setSelectedStatus('ALL');
    }
  }, [isOpen, loadSystems]);

  const setDefaultDates = () => {
    updateDatesForRange('this_month');
  };

  const handleDateRangeChange = (range) => {
    setDateRange(range);
    updateDatesForRange(range);
    setCurrentPage(0);
  };
  
  useEffect(() => {
    if (dateRange === 'custom') {
      setTimeout(() => fromInputRef.current?.focus(), 50);
    }
  }, [dateRange]);

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
      if (selectedStatus !== 'ALL' && reportType === 'alert-logs') {
        params.append('status', selectedStatus);
      }

      let endpoint = '';
      let filename = '';

      if (reportType === 'alert-logs') {
        if (type === 'pdf') {
          endpoint = `${API_BASE_URL}/reports/alert-logs/export/pdf`;
          filename = `Alert_Logs_${new Date().toISOString().split('T')[0]}.pdf`;
        } else if (type === 'excel') {
          endpoint = `${API_BASE_URL}/reports/alert-logs/export/excel`;
          filename = `Alert_Logs_${new Date().toISOString().split('T')[0]}.xlsx`;
        }
      } else {
        if (type === 'pdf') {
          endpoint = `${API_BASE_URL}/reports/export/pdf`;
          filename = `Alarm_Report_${reportType}_${new Date().toISOString().split('T')[0]}.pdf`;
          params.append('reportType', reportType);
        } else if (type === 'excel') {
          endpoint = `${API_BASE_URL}/reports/export/excel`;
          filename = `Alarm_Report_${new Date().toISOString().split('T')[0]}.xlsx`;
        }
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

  // ===== STATUS OPTIONS =====
  const statusOptions = [
    { value: 'ALL', label: 'All Statuses' },
    { value: 'PENDING', label: '🟡 Pending' },
    { value: 'RESOLVED', label: '✅ Resolved' },
    { value: 'REJECTED', label: '🚫 Rejected' },
    { value: 'SIREN_STOP', label: '🔕 Siren Stop' },
    { value: 'CALL', label: '📞 Call' },
    { value: 'ARMED', label: '🔐 Armed' }
  ];

  // ===== FORMAT DURATION =====
  const formatDuration = (seconds) => {
    if (!seconds || seconds === 0) return '-';
    const mins = Math.floor(seconds / 60);
    const secs = Math.floor(seconds % 60);
    if (mins > 60) {
      const hours = Math.floor(mins / 60);
      const remainingMins = mins % 60;
      return `${hours}h ${remainingMins}m ${secs}s`;
    }
    return `${mins}m ${secs}s`;
  };

  // ===== FORMAT DATE - Custom function without date-fns =====
  const formatDate = (dateStr) => {
    if (!dateStr) return '-';
    try {
      const date = new Date(dateStr);
      // Check if date is valid
      if (isNaN(date.getTime())) return dateStr;
      
      const year = date.getFullYear();
      const month = String(date.getMonth() + 1).padStart(2, '0');
      const day = String(date.getDate()).padStart(2, '0');
      const hours = String(date.getHours()).padStart(2, '0');
      const minutes = String(date.getMinutes()).padStart(2, '0');
      const seconds = String(date.getSeconds()).padStart(2, '0');
      
      return `${year}-${month}-${day} ${hours}:${minutes}:${seconds}`;
    } catch {
      return dateStr;
    }
  };

  // ===== RENDER ALERT LOGS =====
  const renderAlertLogs = () => {
    if (!alertLogsData) return null;
    
    const logs = alertLogsData.alertLogs || [];
    
    if (logs.length === 0) {
      return <div className="text-center text-slate-400 py-8">No alert logs found</div>;
    }

    return (
      <div className="space-y-4">
        {/* Status Summary */}
        <div className="grid grid-cols-3 sm:grid-cols-6 gap-2">
          {Object.entries(alertLogsData.statusCounts || {}).map(([status, count]) => (
            <div key={status} className="bg-slate-950/50 border border-slate-800 rounded-xl p-2 text-center">
              <div className="text-lg font-bold text-white">{count}</div>
              <div className="text-[10px] text-slate-400 font-mono">{status}</div>
            </div>
          ))}
        </div>

        {/* Table */}
        <div className="overflow-x-auto">
          <table className="w-full text-xs">
            <thead className="bg-slate-800/50 text-slate-400 uppercase font-mono sticky top-0 z-10">
              <tr>
                <th className="px-2 py-2 text-left">ID</th>
                <th className="px-2 py-2 text-left">System</th>
                <th className="px-2 py-2 text-left">Location</th>
                <th className="px-2 py-2 text-left">Zones</th>
                <th className="px-2 py-2 text-left">Zone Names</th>
                <th className="px-2 py-2 text-left">Type</th>
                <th className="px-2 py-2 text-left">Status</th>
                <th className="px-2 py-2 text-left">Received</th>
                <th className="px-2 py-2 text-left">Pending</th>
                <th className="px-2 py-2 text-left">Resolved By</th>
                <th className="px-2 py-2 text-left">Resolved At</th>
                <th className="px-2 py-2 text-left">Resolution</th>
                <th className="px-2 py-2 text-left">IP</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-800/50">
              {logs.map((alert) => (
                <tr key={alert.id} className="hover:bg-slate-900/40 transition-colors">
                  <td className="px-2 py-2 text-slate-400 font-mono">#{alert.id}</td>
                  <td className="px-2 py-2 text-white font-mono">
                    {alert.system?.systemCode || 'N/A'}
                  </td>
                  <td className="px-2 py-2 text-slate-300">
                    {alert.system?.location || 'N/A'}
                  </td>
                  <td className="px-2 py-2 text-slate-300">
                    {alert.zoneNumbers || '00'}
                  </td>
                  <td className="px-2 py-2 text-slate-300 max-w-xs truncate">
                    {alert.zoneNames || 'No Zone'}
                  </td>
                  <td className="px-2 py-2 text-slate-300 max-w-xs truncate">
                    {alert.alertType || 'N/A'}
                  </td>
                  <td className="px-2 py-2">
                    <StatusBadge status={alert.status} />
                  </td>
                  <td className="px-2 py-2 text-slate-400 text-[10px]">
                    {formatDate(alert.receivedAt)}
                  </td>
                  <td className="px-2 py-2 text-yellow-400 font-mono text-center">
                    {alert.pendingDurationSeconds ? formatDuration(alert.pendingDurationSeconds) : '-'}
                  </td>
                  <td className="px-2 py-2 text-slate-300">
                    {alert.resolvedBy || '-'}
                  </td>
                  <td className="px-2 py-2 text-slate-400 text-[10px]">
                    {formatDate(alert.resolvedAt)}
                  </td>
                  <td className="px-2 py-2 text-slate-300 max-w-xs truncate" title={alert.resolutionDescription}>
                    {alert.resolutionDescription || '-'}
                  </td>
                  <td className="px-2 py-2 text-slate-400 text-[10px]">
                    {alert.resolvedFromIp || '-'}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>

        {/* Pagination */}
        {totalPages > 0 && (
          <div className="flex items-center justify-between pt-4 border-t border-slate-800">
            <div className="text-xs text-slate-400 font-mono">
              Showing {logs.length} of {totalRecords} records
            </div>
            <div className="flex items-center gap-2">
              <button
                onClick={() => setCurrentPage(Math.max(0, currentPage - 1))}
                disabled={currentPage === 0}
                className="p-2 rounded-lg bg-slate-800 hover:bg-slate-700 disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
              >
                <ChevronLeft className="w-4 h-4" />
              </button>
              <span className="text-xs text-slate-400 font-mono">
                Page {currentPage + 1} of {totalPages}
              </span>
              <button
                onClick={() => setCurrentPage(Math.min(totalPages - 1, currentPage + 1))}
                disabled={currentPage >= totalPages - 1}
                className="p-2 rounded-lg bg-slate-800 hover:bg-slate-700 disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
              >
                <ChevronRight className="w-4 h-4" />
              </button>
              <select
                value={pageSize}
                onChange={(e) => {
                  setPageSize(Number(e.target.value));
                  setCurrentPage(0);
                }}
                className="bg-slate-800 border border-slate-700 rounded-lg px-2 py-1 text-xs text-white focus:outline-none focus:border-blue-500/50"
              >
                <option value={25}>25</option>
                <option value={50}>50</option>
                <option value={100}>100</option>
                <option value={200}>200</option>
              </select>
            </div>
          </div>
        )}
      </div>
    );
  };

  // ===== RENDER CONTENT =====
  const renderContent = () => {
    if (loading && !(reportType === 'alert-logs' && alertLogsData)) {
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
      case 'alert-logs':
        return renderAlertLogs();
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
                <td className="px-3 py-2 text-slate-400 text-xs">{formatDate(alert.receivedAt)}</td>
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
    if (status === 'REJECTED') {
      return (
        <span className="inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-[10px] font-semibold bg-slate-500/10 text-slate-400 border border-slate-500/20">
          🚫 REJECTED
        </span>
      );
    }
    if (status === 'SIREN_STOP') {
      return (
        <span className="inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-[10px] font-semibold bg-orange-500/10 text-orange-400 border border-orange-500/20">
          🔕 SIREN_STOP
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

  const getTypeLabel = (typeId) => {
    switch (typeId) {
      case 'summary': return 'Summary';
      case 'detailed': return 'Detailed';
      case 'health': return 'Health';
      case 'performance': return 'Performance';
      case 'alert-logs': return 'Alert Logs';
      default: return 'Report';
    }
  };

  const renderPreviewContent = () => {
    if (previewLoading) {
      return (
        <div className="preview-loading">
          <div className="spinner"></div>
          <span>Loading preview metrics...</span>
        </div>
      );
    }

    if (!previewData) {
      return <div className="text-slate-400 text-xs">No preview data available. Click Generate Report to load full results.</div>;
    }

    const total = previewData.totalAlerts ?? previewData.totalRecords ?? 0;
    const pending = previewData.pending ?? 0;
    const resolved = previewData.resolved ?? 0;

    return (
      <div className="space-y-3">
        <div className="stat-row">
          <span className="label">Total Records:</span>
          <span className="value">{total}</span>
        </div>
        <div className="stat-row">
          <span className="label">Pending Alerts:</span>
          <span className="value pending">{pending}</span>
        </div>
        <div className="stat-row">
          <span className="label">Resolved Alerts:</span>
          <span className="value resolved">{resolved}</span>
        </div>

        {previewData.bySystem && Object.keys(previewData.bySystem).length > 0 && (
          <div className="mt-3">
            <div className="text-xs font-bold text-slate-400 mb-2">Systems Overview:</div>
            {Object.entries(previewData.bySystem).slice(0, 4).map(([sys, count]) => {
              const pct = total > 0 ? Math.min(100, Math.round((count / total) * 100)) : 0;
              return (
                <div key={sys} className="system-bar text-xs">
                  <span className="w-24 truncate text-slate-300">{sys}</span>
                  <div className="bar-track">
                    <div className="bar-fill" style={{ width: `${pct}%` }}></div>
                  </div>
                  <span className="w-12 text-right text-slate-400 font-mono">{count}</span>
                </div>
              );
            })}
          </div>
        )}
      </div>
    );
  };

  const renderPreview = () => {
    if (!showPreview) return null;

    return (
      <div className="preview-section mb-6">
        <h3>📊 {getTypeLabel(selectedType)} Preview</h3>
        <div className="preview-content">
          {renderPreviewContent()}
        </div>
        <div className="preview-actions">
          <button onClick={generateReport} disabled={loading} className="btn-generate">
            {loading ? 'Generating...' : '✅ Generate Report'}
          </button>
          <button onClick={handleCancelPreview} className="btn-cancel">
            ❌ Cancel
          </button>
        </div>
      </div>
    );
  };

  if (!isOpen) return null;

  return (
    <div className="fixed inset-0 z-[160] flex items-center justify-center p-4 bg-black/80 backdrop-blur-sm">
      <div className="bg-slate-900 border border-slate-700 rounded-2xl max-w-7xl w-full max-h-[90vh] overflow-hidden shadow-2xl shadow-blue-500/10">
        
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
            <div className="grid grid-cols-2 sm:grid-cols-5 gap-3">
              {[
                { id: 'summary', label: '📊 Summary', desc: 'Overview statistics' },
                { id: 'detailed', label: '📋 Detailed', desc: 'All alerts list' },
                { id: 'health', label: '💚 Health', desc: 'System status' },
                { id: 'performance', label: '👤 Performance', desc: 'User activity' },
                { id: 'alert-logs', label: '📋 Alert Logs', desc: 'Complete alert details' }
              ].map((type) => (
                <button
                  key={type.id}
                  onClick={() => handleTypeSelect(type.id)}
                  className={`type-card ${selectedType === type.id ? 'selected' : ''}`}
                >
                  <div className="type-icon">{type.label.split(' ')[0]}</div>
                  <div className="type-label">{type.label.split(' ').slice(1).join(' ')}</div>
                  <div className="type-desc">{type.desc}</div>
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
                    ref={fromInputRef}
                    value={fromDate}
                    onChange={(e) => {
                      setFromDate(e.target.value);
                      setDateRange('custom');
                      setCurrentPage(0);
                    }}
                    className={`bg-slate-950 border border-slate-800 rounded-lg px-3 py-1.5 text-sm text-white focus:outline-none focus:border-blue-500/50 ${
                      dateRange !== 'custom' ? 'opacity-50 cursor-not-allowed' : ''
                    }`}
                    disabled={dateRange !== 'custom'}
                  />
                  <button
                    type="button"
                    onClick={() => {
                      if (dateRange !== 'custom') handleDateRangeChange('custom');
                      setTimeout(() => fromInputRef.current?.focus(), 50);
                    }}
                    className="p-1 rounded-md text-slate-400 hover:text-white"
                  >
                    <Calendar className="w-5 h-5" />
                  </button>
                </div>
              </div>
              <span className="text-slate-600 text-sm">→</span>
              <div>
                <label className="text-[10px] text-slate-500 font-mono block">To</label>
                <div className="flex items-center gap-2">
                  <input
                    type="date"
                    ref={toInputRef}
                    value={toDate}
                    onChange={(e) => {
                      setToDate(e.target.value);
                      setDateRange('custom');
                      setCurrentPage(0);
                    }}
                    className={`bg-slate-950 border border-slate-800 rounded-lg px-3 py-1.5 text-sm text-white focus:outline-none focus:border-blue-500/50 ${
                      dateRange !== 'custom' ? 'opacity-50 cursor-not-allowed' : ''
                    }`}
                    disabled={dateRange !== 'custom'}
                  />
                  <button
                    type="button"
                    onClick={() => {
                      if (dateRange !== 'custom') handleDateRangeChange('custom');
                      setTimeout(() => toInputRef.current?.focus(), 50);
                    }}
                    className="p-1 rounded-md text-slate-400 hover:text-white"
                  >
                    <Calendar className="w-5 h-5" />
                  </button>
                </div>
              </div>
            </div>
          </div>

          {/* SYSTEM FILTER */}
          <div className="mb-4">
            <label className="text-xs font-bold tracking-wide uppercase text-slate-400 font-mono block mb-2">🔍 System</label>
            <div className="flex flex-wrap items-center gap-2">
              <select
                value={selectedSystem}
                onChange={(e) => {
                  setSelectedSystem(e.target.value);
                  setCurrentPage(0);
                }}
                className="w-full sm:w-64 bg-slate-950 border border-slate-800 rounded-xl px-4 py-2.5 text-sm text-white focus:outline-none focus:border-blue-500/50"
              >
                <option value="ALL">📊 All Systems</option>
                {systems.map((sys) => (
                  <option key={sys.id} value={sys.systemCode}>{sys.systemCode}</option>
                ))}
              </select>
              <button
                type="button"
                onClick={loadSystems}
                className="p-2 rounded-md text-slate-400 hover:text-white"
                title="Reload systems"
              >
                {systemsLoading ? (
                  <span className="w-4 h-4 border-2 border-white/30 border-t-white rounded-full animate-spin block" />
                ) : (
                  <RefreshCw className="w-5 h-5" />
                )}
              </button>
            </div>
          </div>

          {/* STATUS FILTER - Only for Alert Logs */}
          {reportType === 'alert-logs' && (
            <div className="mb-4">
              <label className="text-xs font-bold tracking-wide uppercase text-slate-400 font-mono block mb-2">📌 Status</label>
              <select
                value={selectedStatus}
                onChange={(e) => {
                  setSelectedStatus(e.target.value);
                  setCurrentPage(0);
                }}
                className="w-full sm:w-64 bg-slate-950 border border-slate-800 rounded-xl px-4 py-2.5 text-sm text-white focus:outline-none focus:border-blue-500/50"
              >
                {statusOptions.map((option) => (
                  <option key={option.value} value={option.value}>
                    {option.label}
                  </option>
                ))}
              </select>
            </div>
          )}

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
            {reportType === 'alert-logs' ? (
              <>
                <button
                  onClick={() => downloadReport('pdf')}
                  disabled={downloading || !alertLogsData}
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
                  disabled={downloading || !alertLogsData}
                  className="flex items-center gap-2 px-5 py-2.5 bg-emerald-600 hover:bg-emerald-500 text-white font-bold rounded-xl text-sm transition-all disabled:opacity-50"
                >
                  {downloading ? (
                    <span className="w-4 h-4 border-2 border-white/30 border-t-white rounded-full animate-spin" />
                  ) : (
                    <FileSpreadsheet className="w-4 h-4" />
                  )}
                  Download Excel
                </button>
              </>
            ) : (
              <>
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
              </>
            )}
            <button
              onClick={printReport}
              disabled={!summaryData && !detailedData && !healthData && !performanceData && !alertLogsData}
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

          {/* PREVIEW SECTION */}
          {showPreview && renderPreview()}

          {/* REPORT CONTENT */}
          {!showPreview && renderContent()}
        </div>
      </div>
    </div>
  );
}

ReportGenerator.propTypes = {
  isOpen: PropTypes.bool.isRequired,
  onClose: PropTypes.func.isRequired,
  user: PropTypes.shape({
    username: PropTypes.string.isRequired,
    role: PropTypes.string.isRequired,
  }).isRequired,
};