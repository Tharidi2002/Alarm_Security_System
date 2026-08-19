// src/components/NotificationDropdown.jsx

import { useState, useEffect } from 'react';
import PropTypes from 'prop-types';
import { 
    X, Bell, CheckCheck, Eye, 
    AlertCircle, ChevronRight, Loader2, Ban
} from 'lucide-react';
import { 
    getNotifications, 
    markAsRead, 
    markAllAsRead,
    deleteNotification 
} from '../services/notificationApi';

export default function NotificationDropdown({ 
    username, 
    onClose, 
    onUnreadChange,
    onNotificationClick,
    isCompanyInactive = false
}) {
    const [notifications, setNotifications] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState('');
    const [markingAll, setMarkingAll] = useState(false);
    const [page, setPage] = useState(0);
    const [hasMore, setHasMore] = useState(true);

    // ============================================================
    // LOAD NOTIFICATIONS
    // ============================================================

    const loadNotifications = async (reset = false) => {
        if (isCompanyInactive || !username) {
            setLoading(false);
            setNotifications([]);
            setHasMore(false);
            return;
        }
        
        setLoading(true);
        setError('');
        
        try {
            const currentPage = reset ? 0 : page;
            const data = await getNotifications(username, false, currentPage, 10);
            
            if (reset) {
                setNotifications(data.notifications || []);
            } else {
                setNotifications(prev => [...prev, ...(data.notifications || [])]);
            }
            
            setHasMore(data.hasMore || false);
            if (reset) setPage(0);
            
        } catch (err) {
            setError(err.message || 'Failed to load notifications');
        } finally {
            setLoading(false);
        }
    };

    // ============================================================
    // AUTO-REFRESH EVERY 10 SECONDS (for dropdown)
    // ============================================================

    useEffect(() => {
        if (!isCompanyInactive) {
            loadNotifications(true);
            
            const interval = setInterval(() => {
                if (!isCompanyInactive) {
                    loadNotifications(true);
                }
            }, 10000);
            
            return () => clearInterval(interval);
        } else {
            setLoading(false);
            setNotifications([]);
        }
    }, [username, isCompanyInactive]);

    // ============================================================
    // MARK AS READ
    // ============================================================

    const handleMarkAsRead = async (id) => {
        if (isCompanyInactive) return;
        
        try {
            await markAsRead(id, username);
            setNotifications(prev => 
                prev.map(n => n.id === id ? { ...n, isRead: true } : n)
            );
            if (onUnreadChange) onUnreadChange();
        } catch (err) {
            console.error('Failed to mark as read:', err);
        }
    };

    // ============================================================
    // MARK ALL AS READ
    // ============================================================

    const handleMarkAllAsRead = async () => {
        if (isCompanyInactive) return;
        
        setMarkingAll(true);
        try {
            await markAllAsRead(username);
            setNotifications(prev => 
                prev.map(n => ({ ...n, isRead: true }))
            );
            if (onUnreadChange) onUnreadChange();
        } catch (err) {
            console.error('Failed to mark all as read:', err);
        } finally {
            setMarkingAll(false);
        }
    };

    // ============================================================
    // DELETE NOTIFICATION
    // ============================================================

    const handleDelete = async (id) => {
        if (isCompanyInactive) return;
        
        try {
            await deleteNotification(id, username);
            setNotifications(prev => prev.filter(n => n.id !== id));
            if (onUnreadChange) onUnreadChange();
        } catch (err) {
            console.error('Failed to delete notification:', err);
        }
    };

    // ============================================================
    // LOAD MORE
    // ============================================================

    const handleLoadMore = () => {
        if (isCompanyInactive) return;
        const nextPage = page + 1;
        setPage(nextPage);
        loadNotifications(false);
    };

    // ============================================================
    // FORMAT TIME
    // ============================================================

    const formatTime = (dateStr) => {
        if (!dateStr) return '';
        const date = new Date(dateStr);
        const now = new Date();
        const diffMs = now - date;
        const diffMins = Math.floor(diffMs / 60000);
        const diffHours = Math.floor(diffMins / 60);
        const diffDays = Math.floor(diffHours / 24);

        if (diffMins < 1) return 'Just now';
        if (diffMins < 60) return `${diffMins}m ago`;
        if (diffHours < 24) return `${diffHours}h ago`;
        return `${diffDays}d ago`;
    };

    // ============================================================
    // GET ICON FOR NOTIFICATION TYPE
    // ============================================================

    const getTypeIcon = (type) => {
        const icons = {
            'NEW_ALERT': '🔴',
            'ALERT_RESOLVED': '✅',
            'SIREN_STOP': '🔕',
            'SYSTEM_DISARM': '🔓',
            'SYSTEM_ARMED': '🔐',
            'SYSTEM_STATUS_CHANGE': '⚠️',
            'SYSTEM_DELETED': '🗑️',
            'SYSTEM_RESTORED': '🔄',
            'USER_CREATED': '👤',
            'USER_DELETED': '❌',
            'HEARTBEAT_LOST': '📡',
            'HEARTBEAT_RESTORED': '📶',
            'ZONE_UPDATED': '✏️',
            'COMPANY_DEACTIVATED': '🚫',
            'COMPANY_REACTIVATED': '✅'
        };
        return icons[type] || '📌';
    };

    // ============================================================
    // GET SEVERITY COLOR
    // ============================================================

    const getSeverityColor = (severity) => {
        const colors = {
            'INFO': 'bg-blue-500/10 text-blue-400 border-blue-500/20',
            'WARNING': 'bg-yellow-500/10 text-yellow-400 border-yellow-500/20',
            'CRITICAL': 'bg-red-500/10 text-red-400 border-red-500/20'
        };
        return colors[severity] || 'bg-slate-500/10 text-slate-400 border-slate-500/20';
    };

    // ============================================================
    // RENDER
    // ============================================================

    const unreadCount = notifications.filter(n => !n.isRead).length;

    if (isCompanyInactive) {
        return (
            <div className="bg-slate-900 border border-slate-700 rounded-2xl shadow-2xl shadow-black/50 overflow-hidden">
                <div className="flex justify-between items-center p-4 border-b border-slate-800 bg-slate-950/50">
                    <div className="flex items-center gap-2">
                        <Bell className="w-4 h-4 text-slate-500" />
                        <span className="text-sm font-bold text-slate-400">Notifications</span>
                    </div>
                    <button onClick={onClose} className="p-1.5 hover:bg-slate-800 rounded-lg transition-colors text-slate-400 hover:text-white">
                        <X className="w-4 h-4" />
                    </button>
                </div>
                <div className="p-8 text-center">
                    <Ban className="w-12 h-12 text-slate-600 mx-auto mb-3" />
                    <p className="text-slate-400 font-mono text-sm">Notifications Disabled</p>
                    <p className="text-xs text-slate-500 mt-1">Company is inactive</p>
                </div>
            </div>
        );
    }

    return (
        <div className="bg-slate-900 border border-slate-700 rounded-2xl shadow-2xl shadow-black/50 overflow-hidden max-h-[500px] flex flex-col">
            
            {/* Header */}
            <div className="flex justify-between items-center p-4 border-b border-slate-800 bg-slate-950/50">
                <div className="flex items-center gap-2">
                    <Bell className="w-4 h-4 text-blue-400" />
                    <span className="text-sm font-bold text-white">Notifications</span>
                    {unreadCount > 0 && (
                        <span className="text-[10px] px-2 py-0.5 bg-yellow-500/20 text-yellow-400 rounded-full font-mono">
                            {unreadCount} new
                        </span>
                    )}
                </div>
                <div className="flex items-center gap-1">
                    {unreadCount > 0 && (
                        <button
                            onClick={handleMarkAllAsRead}
                            disabled={markingAll}
                            className="p-1.5 hover:bg-slate-800 rounded-lg transition-colors text-slate-400 hover:text-white"
                            title="Mark all as read"
                        >
                            {markingAll ? (
                                <Loader2 className="w-4 h-4 animate-spin" />
                            ) : (
                                <CheckCheck className="w-4 h-4" />
                            )}
                        </button>
                    )}
                    <button
                        onClick={onClose}
                        className="p-1.5 hover:bg-slate-800 rounded-lg transition-colors text-slate-400 hover:text-white"
                    >
                        <X className="w-4 h-4" />
                    </button>
                </div>
            </div>

            {/* Auto-refresh indicator */}
            <div className="px-4 py-1 text-[8px] text-slate-500 font-mono flex items-center gap-2 border-b border-slate-800">
                <span>🔄 Auto-refresh every 10s</span>
                <span className="w-1 h-1 bg-emerald-500 rounded-full animate-pulse"></span>
                <span>{notifications.length} notifications</span>
            </div>

            {/* Body */}
            <div className="flex-1 overflow-y-auto p-2 space-y-1.5 max-h-[350px]">
                {loading && notifications.length === 0 ? (
                    <div className="text-center py-8 text-slate-500">
                        <Loader2 className="w-8 h-8 mx-auto mb-2 animate-spin text-slate-600" />
                        <p className="text-sm font-mono">Loading...</p>
                    </div>
                ) : error ? (
                    <div className="text-center py-8 text-red-400">
                        <AlertCircle className="w-8 h-8 mx-auto mb-2" />
                        <p className="text-sm font-mono">{error}</p>
                        <button 
                            onClick={() => loadNotifications(true)}
                            className="mt-2 text-xs text-blue-400 hover:text-blue-300"
                        >
                            Retry
                        </button>
                    </div>
                ) : notifications.length === 0 ? (
                    <div className="text-center py-8 text-slate-500">
                        <Bell className="w-8 h-8 mx-auto mb-2 text-slate-600" />
                        <p className="text-sm font-mono">No notifications</p>
                    </div>
                ) : (
                    notifications.map((notification) => {
                        const isRead = notification.isRead || false;
                        return (
                            <div
                                key={notification.id}
                                className={`p-3 rounded-xl border transition-all cursor-pointer ${
                                    isRead
                                        ? 'bg-slate-950/50 border-slate-800 opacity-60'
                                        : 'bg-slate-950 border-slate-700 hover:border-slate-600'
                                }`}
                                onClick={() => {
                                    if (!isRead && !isCompanyInactive) {
                                        handleMarkAsRead(notification.id);
                                    }
                                    if (onNotificationClick) {
                                        onNotificationClick(notification);
                                    }
                                }}
                            >
                                <div className="flex items-start gap-2.5">
                                    <div className="text-xl flex-shrink-0">
                                        {getTypeIcon(notification.type)}
                                    </div>
                                    <div className="flex-1 min-w-0">
                                        <div className="flex items-center gap-1.5 flex-wrap">
                                            <h4 className={`text-xs font-bold truncate ${
                                                isRead ? 'text-slate-400' : 'text-white'
                                            }`}>
                                                {notification.title}
                                            </h4>
                                            <span className={`text-[8px] font-mono px-1.5 py-0.5 rounded-full border ${getSeverityColor(notification.severity)}`}>
                                                {notification.severity}
                                            </span>
                                            {!isRead && (
                                                <span className="w-1.5 h-1.5 bg-red-500 rounded-full animate-pulse flex-shrink-0" />
                                            )}
                                        </div>
                                        <p className={`text-[11px] mt-0.5 truncate ${
                                            isRead ? 'text-slate-500' : 'text-slate-300'
                                        }`}>
                                            {notification.message}
                                        </p>
                                        <div className="flex items-center gap-2 mt-1 text-[9px] text-slate-500 font-mono">
                                            <span>{formatTime(notification.createdAt)}</span>
                                            {notification.actionBy && (
                                                <span>• By: {notification.actionBy}</span>
                                            )}
                                            {notification.system?.systemCode && (
                                                <span>• {notification.system.systemCode}</span>
                                            )}
                                        </div>
                                    </div>
                                    <button
                                        onClick={(e) => {
                                            e.stopPropagation();
                                            if (!isCompanyInactive) {
                                                handleDelete(notification.id);
                                            }
                                        }}
                                        className={`p-1 hover:bg-slate-800 rounded-lg transition-colors flex-shrink-0 ${
                                            isCompanyInactive ? 'opacity-40 cursor-not-allowed' : 'text-slate-500 hover:text-red-400'
                                        }`}
                                        title="Delete"
                                        disabled={isCompanyInactive}
                                    >
                                        <X className="w-3 h-3" />
                                    </button>
                                </div>
                            </div>
                        );
                    })
                )}
            </div>

            {/* Footer */}
            <div className="p-2 border-t border-slate-800 bg-slate-950/30">
                <button
                    onClick={() => {
                        if (onNotificationClick) {
                            onNotificationClick();
                        }
                        window.location.href = '/notifications';
                    }}
                    disabled={isCompanyInactive}
                    className={`w-full py-2 text-center text-xs font-mono rounded-lg transition-colors flex items-center justify-center gap-1 ${
                        isCompanyInactive 
                            ? 'text-slate-500 cursor-not-allowed opacity-50' 
                            : 'text-blue-400 hover:text-blue-300 hover:bg-blue-500/10'
                    }`}
                >
                    View All
                    <ChevronRight className="w-3 h-3" />
                </button>
            </div>
        </div>
    );
}

NotificationDropdown.propTypes = {
    username: PropTypes.string.isRequired,
    onClose: PropTypes.func.isRequired,
    onUnreadChange: PropTypes.func,
    onNotificationClick: PropTypes.func,
    isCompanyInactive: PropTypes.bool,
};