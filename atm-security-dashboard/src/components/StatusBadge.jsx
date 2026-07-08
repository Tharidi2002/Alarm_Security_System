import PropTypes from 'prop-types';

export default function StatusBadge({ status }) {
  if (status === 'PENDING') {
    return (
      <span className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full text-xs font-semibold tracking-wide bg-red-500/10 text-red-400 border border-red-500/20">
        <span className="w-1.5 h-1.5 rounded-full bg-red-500 animate-ping"></span>
        PENDING
      </span>
    );
  }
  
  if (status === 'ARMED') {
    return (
      <span className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full text-xs font-semibold tracking-wide bg-yellow-500/10 text-yellow-400 border border-yellow-500/20">
        <span className="w-1.5 h-1.5 rounded-full bg-yellow-500 animate-pulse"></span>
        ARMED
      </span>
    );
  }
  
  if (status === 'RESOLVED') {
    return (
      <span className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full text-xs font-semibold tracking-wide bg-emerald-500/10 text-emerald-400 border border-emerald-500/20">
        <span className="w-1.5 h-1.5 rounded-full bg-emerald-500"></span>
        RESOLVED
      </span>
    );
  }
  
  return (
    <span className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full text-xs font-semibold tracking-wide bg-slate-500/10 text-slate-400 border border-slate-500/20">
      {status || 'UNKNOWN'}
    </span>
  );
}

StatusBadge.propTypes = {
  status: PropTypes.string,
};