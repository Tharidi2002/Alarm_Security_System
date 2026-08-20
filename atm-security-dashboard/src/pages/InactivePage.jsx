import PropTypes from 'prop-types';
import { Lock, LogOut, User, Calendar, AlertCircle, FileText } from 'lucide-react';

export default function InactivePage({ user, onLogout }) {
    const formatDate = (dateStr) => {
        if (!dateStr) return 'N/A';
        try {
            const date = new Date(dateStr);
            return date.toLocaleString('en-US', {
                year: 'numeric',
                month: 'short',
                day: '2-digit',
                hour: '2-digit',
                minute: '2-digit'
            });
        } catch {
            return dateStr;
        }
    };

    return (
        <div className="min-h-screen bg-slate-950 text-slate-100 flex items-center justify-center p-4 font-sans">
            <div className="max-w-md w-full bg-slate-900/80 backdrop-blur-xl border border-red-500/30 rounded-2xl shadow-2xl shadow-red-500/10 p-8 animate-in fade-in duration-300">
                
                {/* Header */}
                <div className="text-center mb-6">
                    <div className="bg-red-500/10 p-4 rounded-full border border-red-500/20 mx-auto w-fit mb-4">
                        <Lock className="w-12 h-12 text-red-500 animate-pulse" />
                    </div>
                    <h1 className="text-2xl font-bold text-white uppercase tracking-wider">
                        🚫 Account Inactive
                    </h1>
                    <p className="text-sm text-slate-400 mt-1 font-mono">
                        Access has been restricted
                    </p>
                </div>

                {/* Message */}
                <div className="bg-slate-950/80 border border-slate-800 rounded-xl p-4 mb-6">
                    <p className="text-sm text-slate-300 text-center">
                        Your account has been deactivated. Please contact the system administrator to regain access.
                    </p>
                </div>

                {/* Details Card */}
                <div className="bg-slate-950 border border-slate-800 rounded-xl p-4 space-y-3 mb-6">
                    <div className="flex items-center justify-between text-sm border-b border-slate-800 pb-2">
                        <span className="text-slate-500 flex items-center gap-2">
                            <User className="w-3.5 h-3.5" />
                            Username
                        </span>
                        <span className="text-white font-bold">{user?.username || 'Unknown'}</span>
                    </div>
                    
                    {user?.inactivatedBy && (
                        <div className="flex items-center justify-between text-sm border-b border-slate-800 pb-2">
                            <span className="text-slate-500 flex items-center gap-2">
                                <AlertCircle className="w-3.5 h-3.5 text-red-400" />
                                Deactivated By
                            </span>
                            <span className="text-white font-medium">{user.inactivatedBy}</span>
                        </div>
                    )}

                    {user?.inactivatedAt && (
                        <div className="flex items-center justify-between text-sm border-b border-slate-800 pb-2">
                            <span className="text-slate-500 flex items-center gap-2">
                                <Calendar className="w-3.5 h-3.5" />
                                Deactivated At
                            </span>
                            <span className="text-white font-medium">{formatDate(user.inactivatedAt)}</span>
                        </div>
                    )}

                    {user?.inactivationReason && (
                        <div className="flex flex-col text-sm border-b border-slate-800 pb-2">
                            <span className="text-slate-500 flex items-center gap-2 mb-1">
                                <AlertCircle className="w-3.5 h-3.5 text-yellow-400" />
                                Reason
                            </span>
                            <span className="text-yellow-400 font-medium">{user.inactivationReason}</span>
                        </div>
                    )}

                    {user?.inactivationDescription && (
                        <div className="flex flex-col text-sm">
                            <span className="text-slate-500 flex items-center gap-2 mb-1">
                                <FileText className="w-3.5 h-3.5" />
                                Description
                            </span>
                            <span className="text-slate-300">{user.inactivationDescription}</span>
                        </div>
                    )}
                </div>

                {/* Logout Button */}
                <button
                    onClick={onLogout}
                    className="w-full py-3 bg-gradient-to-r from-red-600 to-red-700 hover:from-red-500 hover:to-red-600 text-white font-bold rounded-xl text-sm transition-all uppercase tracking-wide flex items-center justify-center gap-2 shadow-lg shadow-red-500/10"
                >
                    <LogOut className="w-4 h-4" />
                    Logout
                </button>

                {/* Footer */}
                <p className="text-[10px] text-slate-500 font-mono text-center mt-4">
                    🔒 Access restricted until account is reactivated by administrator
                </p>
            </div>
        </div>
    );
}

InactivePage.propTypes = {
    user: PropTypes.shape({
        username: PropTypes.string,
        inactivatedBy: PropTypes.string,
        inactivatedAt: PropTypes.string,
        inactivationReason: PropTypes.string,
        inactivationDescription: PropTypes.string,
    }),
    onLogout: PropTypes.func.isRequired,
};