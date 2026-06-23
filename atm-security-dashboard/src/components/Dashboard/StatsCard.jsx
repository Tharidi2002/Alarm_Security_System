import React from 'react';

const colorMap = {
  blue: 'border-blue-500/20 bg-blue-500/10 text-blue-400',
  red: 'border-red-500/20 bg-red-500/10 text-red-400',
  green: 'border-emerald-500/20 bg-emerald-500/10 text-emerald-400',
  yellow: 'border-yellow-500/20 bg-yellow-500/10 text-yellow-400',
  purple: 'border-purple-500/20 bg-purple-500/10 text-purple-400',
};

const StatsCard = ({ icon, label, value, color = 'blue', subtitle }) => {
  return (
    <div className={`bg-slate-950 border rounded-xl p-3 sm:p-5 shadow-lg shadow-black/20 ${colorMap[color]}`}>
      <div className="flex items-center justify-between">
        <div className="min-w-0">
          <p className="text-[10px] sm:text-xs font-medium text-slate-400 uppercase tracking-wider truncate">{label}</p>
          <h3 className="text-xl sm:text-2xl lg:text-3xl font-bold text-white mt-0.5 sm:mt-1">{value}</h3>
          {subtitle && (
            <p className="text-[10px] sm:text-xs text-slate-500 mt-0.5 truncate">{subtitle}</p>
          )}
        </div>
        <div className={`p-2 sm:p-3 rounded-xl border flex-shrink-0 ${colorMap[color]}`}>
          {icon}
        </div>
      </div>
    </div>
  );
};

export default StatsCard;