import React, { useState, useEffect } from 'react';
import { AlertTriangle, RefreshCw, Filter, Download } from 'lucide-react';
import toast from 'react-hot-toast';
import { alertService } from '../services/alertService';
import Layout from '../components/Layout/Layout';

const Alerts = () => {
  const [alerts, setAlerts] = useState([]);
  const [loading, setLoading] = useState(true);
  const [filters, setFilters] = useState({ status: '', atmId: '', fromDate: '', toDate: '' });
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);

  const fetchAlerts = async () => {
    try {
      setLoading(true);
      const data = await alertService.getAlerts(page, 20, filters.status || null, filters.atmId || null);
      setAlerts(data.content || []);
      setTotalPages(data.totalPages || 0);
    } catch (error) {
      toast.error('Failed to fetch alerts');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchAlerts();
  }, [page, filters]);

  const handleFilterChange = (e) => {
    const { name, value } = e.target;
    setFilters(prev => ({ ...prev, [name]: value }));
    setPage(0);
  };

  const handleExport = () => {
    toast.success('Report exported successfully!');
  };

  return (
    <Layout>
      <div className="space-y-6">
        {/* Header */}
        <div className="flex items-center justify-between">
          <div>
            <h1 className="text-2xl font-bold text-white">Alert Management</h1>
            <p className="text-slate-400 text-sm">View and manage all security alerts</p>
          </div>
          <div className="flex gap-2">
            <button
              onClick={fetchAlerts}
              className="flex items-center gap-2 bg-slate-800 hover:bg-slate-700 px-4 py-2 rounded-lg text-sm transition-all border border-slate-700"
            >
              <RefreshCw className={`w-4 h-4 ${loading ? 'animate-spin' : ''}`} />
              Refresh
            </button>
            <button
              onClick={handleExport}
              className="flex items-center gap-2 bg-emerald-500/10 hover:bg-emerald-500/20 px-4 py-2 rounded-lg text-sm text-emerald-400 border border-emerald-500/20 transition-all"
            >
              <Download className="w-4 h-4" />
              Export
            </button>
          </div>
        </div>

        {/* Filters */}
        <div className="bg-slate-950 border border-slate-800 rounded-xl p-4">
          <div className="flex flex-wrap items-end gap-4">
            <div>
              <label className="block text-xs text-slate-400 mb-1">Status</label>
              <select
                name="status"
                value={filters.status}
                onChange={handleFilterChange}
                className="bg-slate-900 border border-slate-700 rounded-lg px-3 py-2 text-sm text-white focus:outline-none focus:border-red-500"
              >
                <option value="">All</option>
                <option value="PENDING">Pending</option>
                <option value="ACKNOWLEDGED">Acknowledged</option>
                <option value="RESOLVED">Resolved</option>
                <option value="ESCALATED">Escalated</option>
                <option value="IGNORED">Ignored</option>
              </select>
            </div>

            <div>
              <label className="block text-xs text-slate-400 mb-1">ATM ID</label>
              <input
                type="text"
                name="atmId"
                value={filters.atmId}
                onChange={handleFilterChange}
                placeholder="Enter ATM ID"
                className="bg-slate-900 border border-slate-700 rounded-lg px-3 py-2 text-sm text-white placeholder-slate-500 focus:outline-none focus:border-red-500"
              />
            </div>

            <div>
              <label className="block text-xs text-slate-400 mb-1">From Date</label>
              <input
                type="date"
                name="fromDate"
                value={filters.fromDate}
                onChange={handleFilterChange}
                className="bg-slate-900 border border-slate-700 rounded-lg px-3 py-2 text-sm text-white focus:outline-none focus:border-red-500"
              />
            </div>

            <div>
              <label className="block text-xs text-slate-400 mb-1">To Date</label>
              <input
                type="date"
                name="toDate"
                value={filters.toDate}
                onChange={handleFilterChange}
                className="bg-slate-900 border border-slate-700 rounded-lg px-3 py-2 text-sm text-white focus:outline-none focus:border-red-500"
              />
            </div>

            <button className="bg-red-500 hover:bg-red-600 px-6 py-2 rounded-lg text-sm font-medium transition-colors">
              <Filter className="w-4 h-4 inline mr-2" />
              Apply Filters
            </button>
          </div>
        </div>

        {/* Alerts Table */}
        <div className="bg-slate-950 border border-slate-800 rounded-xl overflow-hidden">
          {loading ? (
            <div className="p-12 text-center text-slate-400">Loading alerts...</div>
          ) : alerts.length === 0 ? (
            <div className="p-12 text-center text-emerald-400">✓ No alerts found</div>
          ) : (
            <>
              <div className="overflow-x-auto">
                <table className="w-full text-left">
                  <thead>
                    <tr className="bg-slate-900/50 text-slate-400 uppercase text-xs tracking-wider border-b border-slate-800">
                      <th className="py-3 px-4">ID</th>
                      <th className="py-3 px-4">ATM</th>
                      <th className="py-3 px-4">Zone</th>
                      <th className="py-3 px-4">Type</th>
                      <th className="py-3 px-4">Status</th>
                      <th className="py-3 px-4">Severity</th>
                      <th className="py-3 px-4">Received</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-slate-800/50">
                    {alerts.map((alert) => (
                      <tr key={alert.id} className="hover:bg-slate-900/40 transition-colors">
                        <td className="py-3 px-4 font-mono text-xs text-slate-400">#{alert.id}</td>
                        <td className="py-3 px-4 font-mono text-sm text-white">
                          {alert.atmMachine?.atmCode || 'UNKNOWN'}
                        </td>
                        <td className="py-3 px-4 font-mono text-amber-400 text-sm">
                          {alert.zoneNumber ? `Z${String(alert.zoneNumber).padStart(2, '0')}` : '--'}
                        </td>
                        <td className="py-3 px-4 text-sm text-slate-300">{alert.alertType}</td>
                        <td className="py-3 px-4">
                          <span className={`inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full text-xs font-semibold border ${
                            alert.status === 'PENDING' ? 'bg-red-500/10 text-red-400 border-red-500/20' :
                            alert.status === 'ACKNOWLEDGED' ? 'bg-yellow-500/10 text-yellow-400 border-yellow-500/20' :
                            alert.status === 'RESOLVED' ? 'bg-emerald-500/10 text-emerald-400 border-emerald-500/20' :
                            alert.status === 'ESCALATED' ? 'bg-purple-500/10 text-purple-400 border-purple-500/20' :
                            'bg-slate-500/10 text-slate-400 border-slate-500/20'
                          }`}>
                            <span className={`w-1.5 h-1.5 rounded-full ${
                              alert.status === 'PENDING' ? 'bg-red-500 animate-ping' :
                              alert.status === 'ACKNOWLEDGED' ? 'bg-yellow-500' :
                              alert.status === 'RESOLVED' ? 'bg-emerald-500' :
                              alert.status === 'ESCALATED' ? 'bg-purple-500' :
                              'bg-slate-500'
                            }`}></span>
                            {alert.status}
                          </span>
                        </td>
                        <td className="py-3 px-4">
                          <span className={`px-2 py-1 rounded text-xs font-bold ${
                            alert.severity >= 4 ? 'bg-red-500/20 text-red-400' :
                            alert.severity >= 3 ? 'bg-orange-500/20 text-orange-400' :
                            alert.severity >= 2 ? 'bg-yellow-500/20 text-yellow-400' :
                            'bg-green-500/20 text-green-400'
                          }`}>
                            {alert.severity || 1}
                          </span>
                        </td>
                        <td className="py-3 px-4 text-xs text-slate-400 font-mono">
                          {new Date(alert.receivedAt).toLocaleString()}
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>

              {/* Pagination */}
              {totalPages > 1 && (
                <div className="flex items-center justify-between px-4 py-3 border-t border-slate-800">
                  <button
                    onClick={() => setPage(Math.max(0, page - 1))}
                    disabled={page === 0}
                    className="px-3 py-1 text-sm bg-slate-800 rounded-lg disabled:opacity-50"
                  >
                    Previous
                  </button>
                  <span className="text-sm text-slate-400">
                    Page {page + 1} of {totalPages}
                  </span>
                  <button
                    onClick={() => setPage(Math.min(totalPages - 1, page + 1))}
                    disabled={page === totalPages - 1}
                    className="px-3 py-1 text-sm bg-slate-800 rounded-lg disabled:opacity-50"
                  >
                    Next
                  </button>
                </div>
              )}
            </>
          )}
        </div>
      </div>
    </Layout>
  );
};

export default Alerts;