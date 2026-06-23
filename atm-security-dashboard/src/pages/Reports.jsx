import React, { useState } from 'react';
import { FileText, Download, Calendar, BarChart3, PieChart, TrendingUp } from 'lucide-react';
import toast from 'react-hot-toast';
import Layout from '../components/Layout/Layout';

const Reports = () => {
  const [reportType, setReportType] = useState('daily');
  const [dateRange, setDateRange] = useState({ from: '', to: '' });

  const handleGenerate = () => {
    toast.success(`Generating ${reportType} report...`);
  };

  const handleExport = (format) => {
    toast.success(`Exporting as ${format}...`);
  };

  return (
    <Layout>
      <div className="space-y-6">
        {/* Header */}
        <div className="flex items-center justify-between">
          <div>
            <h1 className="text-2xl font-bold text-white">Reports & Analytics</h1>
            <p className="text-slate-400 text-sm">Generate and export security reports</p>
          </div>
        </div>

        {/* Report Generation */}
        <div className="bg-slate-950 border border-slate-800 rounded-xl p-6">
          <h2 className="text-lg font-semibold text-white mb-4">Generate Report</h2>
          <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
            <div>
              <label className="block text-sm text-slate-400 mb-1">Report Type</label>
              <select
                value={reportType}
                onChange={(e) => setReportType(e.target.value)}
                className="w-full bg-slate-900 border border-slate-700 rounded-lg px-3 py-2 text-white focus:outline-none focus:border-red-500"
              >
                <option value="daily">Daily Report</option>
                <option value="weekly">Weekly Report</option>
                <option value="monthly">Monthly Report</option>
                <option value="custom">Custom Range</option>
              </select>
            </div>

            <div>
              <label className="block text-sm text-slate-400 mb-1">From Date</label>
              <input
                type="date"
                value={dateRange.from}
                onChange={(e) => setDateRange({ ...dateRange, from: e.target.value })}
                className="w-full bg-slate-900 border border-slate-700 rounded-lg px-3 py-2 text-white focus:outline-none focus:border-red-500"
              />
            </div>

            <div>
              <label className="block text-sm text-slate-400 mb-1">To Date</label>
              <input
                type="date"
                value={dateRange.to}
                onChange={(e) => setDateRange({ ...dateRange, to: e.target.value })}
                className="w-full bg-slate-900 border border-slate-700 rounded-lg px-3 py-2 text-white focus:outline-none focus:border-red-500"
              />
            </div>
          </div>

          <button
            onClick={handleGenerate}
            className="mt-4 bg-red-500 hover:bg-red-600 px-6 py-2 rounded-lg font-medium transition-colors"
          >
            <FileText className="w-4 h-4 inline mr-2" />
            Generate Report
          </button>
        </div>

        {/* Quick Stats */}
        <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
          <div className="bg-slate-950 border border-slate-800 rounded-xl p-4">
            <div className="flex items-center gap-3">
              <div className="bg-blue-500/10 p-2 rounded-lg">
                <BarChart3 className="w-5 h-5 text-blue-400" />
              </div>
              <div>
                <p className="text-xs text-slate-400">Total Alerts</p>
                <p className="text-xl font-bold text-white">1,284</p>
              </div>
            </div>
          </div>

          <div className="bg-slate-950 border border-slate-800 rounded-xl p-4">
            <div className="flex items-center gap-3">
              <div className="bg-red-500/10 p-2 rounded-lg">
                <TrendingUp className="w-5 h-5 text-red-400" />
              </div>
              <div>
                <p className="text-xs text-slate-400">Active Threats</p>
                <p className="text-xl font-bold text-red-400">23</p>
              </div>
            </div>
          </div>

          <div className="bg-slate-950 border border-slate-800 rounded-xl p-4">
            <div className="flex items-center gap-3">
              <div className="bg-emerald-500/10 p-2 rounded-lg">
                <PieChart className="w-5 h-5 text-emerald-400" />
              </div>
              <div>
                <p className="text-xs text-slate-400">Resolved</p>
                <p className="text-xl font-bold text-emerald-400">1,261</p>
              </div>
            </div>
          </div>

          <div className="bg-slate-950 border border-slate-800 rounded-xl p-4">
            <div className="flex items-center gap-3">
              <div className="bg-purple-500/10 p-2 rounded-lg">
                <Calendar className="w-5 h-5 text-purple-400" />
              </div>
              <div>
                <p className="text-xs text-slate-400">Avg Response</p>
                <p className="text-xl font-bold text-white">4.2m</p>
              </div>
            </div>
          </div>
        </div>

        {/* Export Options */}
        <div className="bg-slate-950 border border-slate-800 rounded-xl p-6">
          <h2 className="text-lg font-semibold text-white mb-4">Export Reports</h2>
          <div className="flex flex-wrap gap-3">
            <button
              onClick={() => handleExport('PDF')}
              className="flex items-center gap-2 bg-slate-800 hover:bg-slate-700 px-4 py-2 rounded-lg text-sm transition-colors"
            >
              <Download className="w-4 h-4" /> Export as PDF
            </button>
            <button
              onClick={() => handleExport('Excel')}
              className="flex items-center gap-2 bg-slate-800 hover:bg-slate-700 px-4 py-2 rounded-lg text-sm transition-colors"
            >
              <Download className="w-4 h-4" /> Export as Excel
            </button>
            <button
              onClick={() => handleExport('CSV')}
              className="flex items-center gap-2 bg-slate-800 hover:bg-slate-700 px-4 py-2 rounded-lg text-sm transition-colors"
            >
              <Download className="w-4 h-4" /> Export as CSV
            </button>
          </div>
        </div>
      </div>
    </Layout>
  );
};

export default Reports;