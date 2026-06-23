import React, { useState } from 'react';
import { Filter, X } from 'lucide-react';

const AlertFilters = ({ onFilterChange }) => {
  const [status, setStatus] = useState('');
  const [atmId, setAtmId] = useState('');

  const handleApply = () => {
    onFilterChange({ status, atmId: atmId || '' });
  };

  const handleClear = () => {
    setStatus('');
    setAtmId('');
    onFilterChange({ status: '', atmId: '' });
  };

  return (
    <div className="bg-slate-950 border border-slate-800 rounded-xl p-3 sm:p-4">
      <div className="flex flex-col sm:flex-row flex-wrap items-stretch sm:items-end gap-3 sm:gap-4">
        <div className="flex-1 min-w-[120px]">
          <label className="block text-[10px] sm:text-xs text-slate-400 mb-1">Status</label>
          <select
            value={status}
            onChange={(e) => setStatus(e.target.value)}
            className="w-full bg-slate-900 border border-slate-700 rounded-lg px-2 sm:px-3 py-1.5 sm:py-2 text-xs sm:text-sm text-white focus:outline-none focus:border-red-500"
          >
            <option value="">All</option>
            <option value="PENDING">Pending</option>
            <option value="ACKNOWLEDGED">Acknowledged</option>
            <option value="RESOLVED">Resolved</option>
            <option value="ESCALATED">Escalated</option>
            <option value="IGNORED">Ignored</option>
          </select>
        </div>

        <div className="flex-1 min-w-[120px]">
          <label className="block text-[10px] sm:text-xs text-slate-400 mb-1">ATM ID</label>
          <input
            type="text"
            value={atmId}
            onChange={(e) => setAtmId(e.target.value)}
            placeholder="Enter ATM ID"
            className="w-full bg-slate-900 border border-slate-700 rounded-lg px-2 sm:px-3 py-1.5 sm:py-2 text-xs sm:text-sm text-white placeholder-slate-500 focus:outline-none focus:border-red-500"
          />
        </div>

        <div className="flex gap-2 flex-shrink-0">
          <button
            onClick={handleApply}
            className="flex-1 sm:flex-none flex items-center justify-center gap-1 sm:gap-2 bg-red-500 hover:bg-red-600 px-3 sm:px-4 py-1.5 sm:py-2 rounded-lg text-xs sm:text-sm font-medium transition-colors"
          >
            <Filter className="w-3 h-3 sm:w-4 sm:h-4" />
            <span className="hidden xs:inline">Apply</span>
          </button>
          <button
            onClick={handleClear}
            className="flex-1 sm:flex-none flex items-center justify-center gap-1 sm:gap-2 bg-slate-800 hover:bg-slate-700 px-3 sm:px-4 py-1.5 sm:py-2 rounded-lg text-xs sm:text-sm font-medium transition-colors"
          >
            <X className="w-3 h-3 sm:w-4 sm:h-4" />
            <span className="hidden xs:inline">Clear</span>
          </button>
        </div>
      </div>
    </div>
  );
};

export default AlertFilters;