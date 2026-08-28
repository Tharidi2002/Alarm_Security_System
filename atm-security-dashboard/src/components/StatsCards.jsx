// import React from 'react';
import PropTypes from 'prop-types';
import { Radio, AlertTriangle, CheckCircle2 } from 'lucide-react';

export default function StatsCards({ stats }) {
  const cards = [
    {
      title: 'Total Incidents',
      value: stats.total || 0,
      icon: Radio,
      color: 'blue'
    },
    {
      title: 'Active Threats',
      value: stats.pending || 0,
      icon: AlertTriangle,
      color: 'red'
    },
    {
      title: 'Resolved Cases',
      value: stats.resolved || 0,
      icon: CheckCircle2,
      color: 'emerald'
    }
  ];

  return (
    <div className="grid grid-cols-1 sm:grid-cols-3 gap-3 sm:gap-4 md:gap-5">
      {cards.map((card, index) => (
        <div 
          key={index}
          className="bg-slate-950/80 border border-slate-800/80 rounded-xl sm:rounded-2xl p-4 sm:p-5 flex items-center justify-between shadow-xl shadow-black/40 hover:border-slate-700/80 transition-all duration-300 backdrop-blur-sm group"
        >
          <div className="min-w-0">
            <p className="text-[10px] sm:text-xs font-semibold text-slate-400 uppercase tracking-widest font-mono">
              {card.title}
            </p>
            <h3 className={`text-2xl sm:text-3xl md:text-4xl font-extrabold mt-1 sm:mt-1.5 tracking-tight font-mono transition-colors ${
              card.color === 'red' ? 'text-red-500 group-hover:text-red-400' : 
              card.color === 'emerald' ? 'text-emerald-400 group-hover:text-emerald-300' : 
              'text-white group-hover:text-blue-400'
            }`}>
              {card.value}
            </h3>
          </div>
          <div className={`p-3 sm:p-4 rounded-xl border flex-shrink-0 transition-all duration-300 ${
            card.color === 'red' ? 'bg-red-500/10 border-red-500/30 text-red-400 group-hover:bg-red-500/20 group-hover:border-red-500/50' :
            card.color === 'emerald' ? 'bg-emerald-500/10 border-emerald-500/30 text-emerald-400 group-hover:bg-emerald-500/20 group-hover:border-emerald-500/50' :
            'bg-blue-500/10 border-blue-500/30 text-blue-400 group-hover:bg-blue-500/20 group-hover:border-blue-500/50'
          }`}>
            <card.icon className="w-5 h-5 sm:w-6 sm:h-6" />
          </div>
        </div>
      ))}
    </div>
  );
}

StatsCards.propTypes = {
  stats: PropTypes.shape({
    total: PropTypes.number,
    pending: PropTypes.number,
    resolved: PropTypes.number,
  }).isRequired,
};