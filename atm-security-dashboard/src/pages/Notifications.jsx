// src/pages/Notifications.jsx

import { useState, useEffect, useCallback } from 'react';
import PropTypes from 'prop-types';
import { 
    Bell, BellRing, CheckCheck, Eye, EyeOff,
    Filter, X, Loader2, AlertCircle, ChevronLeft,
    ArrowLeft, Trash2
} from 'lucide-react';
import { 
    getNotifications, 
    markAsRead, 
    markAllAsRead,
    markMultipleAsRead,
    deleteNotification,
    getUnreadCount
} from '../services/notificationApi';

export default function Notifications({ user, onBack }) {
    const [notifications, setNotifications] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState('');
    const [filter, setFilter] = useState('all'); // all, unread, critical
    const [page, setPage] = useState(0);
    const [hasMore, setHasMore] = useState(true);
    const [unreadCount, setUnreadCount] = useState(0);
    const [criticalCount, setCriticalCount] = useState(0);
    const [selectedIds, setSelectedIds] = useState([]);
    const [selectMode, setSelectMode] = useState(false);
    const [markingAll, setMarkingAll] = useState(false);

    const username = user?.username;

    // ============================================================
    // LOAD NOTIFICATIONS
    // ============================================================

    const loadNotifications = useCallback(async (reset = false) => {
        if (!username) return;
        
        setLoading(true);
        setError('');
        
        try {
            const currentPage = reset ? 0 : page;
            const unreadOnly = filter === 'unread';
            const data = await getNotifications(username, unreadOnly, currentPage, 25);
            
            if (reset) {
                setNotifications(data.notifications || []);
                setPage(0);
            } else {
                setNotifications(prev => [...prev, ...(data.notifications || [])]);
            }
            
            setHasMore(data.hasMore || false);
            setUnreadCount(data.unreadCount || 0);
            setCriticalCount(data.criticalCount || 0);
            
        } catch (err) {
            setError(err.message || 'Failed to load notifications');
        } finally {
            setLoading(false);
        }
    }, [username, filter, page]);

    // ============================================================
    // INITIAL LOAD & REFRESH
    // ============================================================

    useEffect(() => {
        loadNotifications(true);
    }, [username, filter]);

    // ============================================================
    // MARK AS READ
    // ============================================================

    const handleMarkAsRead = async (id) => {
        try {
            await markAsRead(id, username);
            setNotifications(prev => 
                prev.map(n => n.id === id ? { ...n, isRead: true } : n)
            );
            updateCounts();
        } catch (err) {
            console.error('Failed to mark as read:', err);
        }
    };

    // ============================================================
    // MARK MULTIPLE AS READ
    // ============================================================

    const handleMarkSelectedAsRead = async () => {
        if (selectedIds.length === 0) return;
        
        try {
            await markMultipleAsRead(selectedIds, username);
            setNotifications(prev => 
                prev.map(n => 
                    selectedIds.includes(n.id) ? { ...n, isRead: true } : n
                )
            );
            setSelectedIds([]);
            setSelectMode(false);
            updateCounts();
        } catch (err) {
            console.error('Failed to mark selected as read:', err);
        }
    };

    // ============================================================
    // MARK ALL AS READ
    // ============================================================

    const handleMarkAllAsRead = async () => {
        setMarkingAll(true);
        try {
            await markAllAsRead(username);
            setNotifications(prev => 
                prev.map(n => ({ ...n, isRead: true }))
            );
            updateCounts();
        } catch (err) {
            console.error('Failed to mark all as read:', err);
        } finally {
            setMarkingAll(false);
        }
    };

    // ============================================================
    // DELETE
    // ============================================================

    const handleDelete = async (id) => {
        if (!window.confirm('Delete this notification?')) return;
        
        try {
            await deleteNotification(id, username);
            setNotifications(prev => prev.filter(n => n.id !== id));
            updateCounts();
        } catch (err) {
            console.error('Failed to delete:', err);
        }
    };

    // ============================================================
    // DELETE SELECTED
    // ============================================================

    const handleDeleteSelected = async () => {
        if (selectedIds.length === 0) return;
        if (!window.confirm(`Delete ${selectedIds.length} notifications?`)) return;
        
        try {
            for (const id of selectedIds) {
                await deleteNotification(id, username);
            }
            setNotifications(prev => prev.filter(n => !selectedIds.includes(n.id)));
            setSelectedIds([]);
            setSelectMode(false);
            updateCounts();
        } catch (err) {
            console.error('Failed to delete selected:', err);
        }
    };

    // ============================================================
    // TOGGLE SELECTION
    // ============================================================

    const toggleSelect = (id) => {
        setSelectedIds(prev => 
            prev.includes(id) 
                ? prev.filter(i => i !== id) 
                : [...prev, id]
        );
    };

    const toggleSelectAll = () => {
        if (selectedIds.length === notifications.length) {
            setSelectedIds([]);
        } else {
            setSelectedIds(notifications.map(n => n.id));
        }
    };

    // ============================================================
    // UPDATE COUNTS
    // ============================================================

    const updateCounts = async () => {
        try {
            const data = await getUnreadCount(username);
            setUnreadCount(data.unreadCount || 0);
            setCriticalCount(data.criticalCount || 0);
        } catch (err) {
            console.error('Failed to update counts:', err);
        }
    };

    // ============================================================
    // LOAD MORE
    // ============================================================

    const handleLoadMore = () => {
        setPage(prev => prev + 1);
        // loadNotifications will be triggered by useEffect
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
        const diffMonths = Math.floor(diffDays / 30);
        const diffYears = Math.floor(diffDays / 365);

        if (diffMins < 1) return 'Just now';
        if (diffMins < 60) return `${diffMins}m ago`;
        if (diffHours < 24) return `${diffHours}h ago`;
        if (diffDays < 30) return `${diffDays}d ago`;
        if (diffMonths < 12) return `${diffMonths}mo ago`;
        return `${diffYears}y ago`;
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
            'ZONE_UPDATED': '✏️'
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

    const unreadNotifs = notifications.filter(n => !n.isRead);

    return (
        <div className="min-h-screen bg-slate-950 text-slate-100">
            {/* Header */}
            <div className="sticky top-0 z-20 bg-slate-900/95 backdrop-blur border-b border-slate-800 px-4 py-3">
                <div className="max-w-4xl mx-auto flex items-center justify-between">
                    <div className="flex items-center gap-3">
                        <button
                            onClick={onBack}
                            className="p-2 hover:bg-slate-800 rounded-lg transition-colors"
                            title="Back to Dashboard"
                        >
                            <ArrowLeft className="w-5 h-5 text-slate-400" />
                        </button>
                        <div className="flex items-center gap-2">
                            <Bell className="w-5 h-5 text-blue-400" />
                            <h1 className="text-lg font-bold text-white">Notifications</h1>
                            {unreadCount > 0 && (
                                <span className="text-xs px-2 py-0.5 bg-yellow-500/20 text-yellow-400 rounded-full font-mono">
                                    {unreadCount} unread
                                </span>
                            )}
                            {criticalCount > 0 && (
                                <span className="text-xs px-2 py-0.5 bg-red-500/20 text-red-400 rounded-full font-mono animate-pulse">
                                    {criticalCount} critical
                                </span>
                            )}
                        </div>
                    </div>
                    <div className="flex items-center gap-2">
                        {selectMode ? (
                            <>
                                <button
                                    onClick={handleMarkSelectedAsRead}
                                    disabled={selectedIds.length === 0}
                                    className="px-3 py-1.5 bg-blue-500/20 hover:bg-blue-500/30 text-blue-400 rounded-lg text-xs font-mono transition-all disabled:opacity-50"
                                >
                                    Mark Read ({selectedIds.length})
                                </button>
                                <button
                                    onClick={handleDeleteSelected}
                                    disabled={selectedIds.length === 0}
                                    className="px-3 py-1.5 bg-red-500/20 hover:bg-red-500/30 text-red-400 rounded-lg text-xs font-mono transition-all disabled:opacity-50"
                                >
                                    Delete
                                </button>
                                <button
                                    onClick={() => {
                                        setSelectMode(false);
                                        setSelectedIds([]);
                                    }}
                                    className="px-3 py-1.5 bg-slate-800 hover:bg-slate-700 text-slate-400 rounded-lg text-xs font-mono transition-all"
                                >
                                    Cancel
                                </button>
                            </>
                        ) : (
                            <>
                                <button
                                    onClick={() => setSelectMode(true)}
                                    className="p-2 hover:bg-slate-800 rounded-lg transition-colors text-slate-400 hover:text-white"
                                    title="Select multiple"
                                >
                                    <CheckCheck className="w-4 h-4" />
                                </button>
                                {unreadCount > 0 && (
                                    <button
                                        onClick={handleMarkAllAsRead}
                                        disabled={markingAll}
                                        className="px-3 py-1.5 bg-yellow-500/20 hover:bg-yellow-500/30 text-yellow-400 rounded-lg text-xs font-mono transition-all disabled:opacity-50 flex items-center gap-1"
                                    >
                                        {markingAll ? (
                                            <Loader2 className="w-3 h-3 animate-spin" />
                                        ) : (
                                            'Mark All Read'
                                        )}
                                    </button>
                                )}
                            </>
                        )}
                    </div>
                </div>
            </div>

            {/* Filters */}
            <div className="max-w-4xl mx-auto px-4 pt-4 pb-2">
                <div className="flex gap-2 flex-wrap">
                    {[
                        { id: 'all', label: 'All', icon: <Bell className="w-3 h-3" /> },
                        { id: 'unread', label: 'Unread', icon: <BellRing className="w-3 h-3" /> },
                        { id: 'critical', label: 'Critical', icon: <AlertCircle className="w-3 h-3" /> }
                    ].map((tab) => (
                        <button
                            key={tab.id}
                            onClick={() => {
                                setFilter(tab.id);
                                setPage(0);
                            }}
                            className={`flex items-center gap-1.5 px-3 py-1.5 rounded-lg text-xs font-mono transition-all ${
                                filter === tab.id
                                    ? 'bg-red-500/20 text-red-400 border border-red-500/30'
                                    : 'bg-slate-800 text-slate-400 hover:bg-slate-700'
                            }`}
                        >
                            {tab.icon}
                            {tab.label}
                        </button>
                    ))}
                </div>
            </div>

            {/* Notifications List */}
            <div className="max-w-4xl mx-auto px-4 pb-8">
                {selectMode && notifications.length > 0 && (
                    <div className="flex items-center gap-2 mb-3 text-xs text-slate-400">
                        <button
                            onClick={toggleSelectAll}
                            className="px-2 py-1 bg-slate-800 rounded-lg hover:bg-slate-700 transition-colors"
                        >
                            {selectedIds.length === notifications.length ? 'Deselect All' : 'Select All'}
                        </button>
                        <span>{selectedIds.length} selected</span>
                    </div>
                )}

                {loading && notifications.length === 0 ? (
                    <div className="text-center py-12 text-slate-500">
                        <Loader2 className="w-12 h-12 mx-auto mb-3 animate-spin text-slate-600" />
                        <p className="font-mono">Loading notifications...</p>
                    </div>
                ) : error ? (
                    <div className="text-center py-12 text-red-400">
                        <AlertCircle className="w-12 h-12 mx-auto mb-3" />
                        <p className="font-mono">{error}</p>
                        <button 
                            onClick={() => loadNotifications(true)}
                            className="mt-3 px-4 py-2 bg-slate-800 hover:bg-slate-700 rounded-lg text-sm font-mono transition-colors"
                        >
                            Retry
                        </button>
                    </div>
                ) : notifications.length === 0 ? (
                    <div className="text-center py-12 text-slate-500">
                        <Bell className="w-16 h-16 mx-auto mb-3 text-slate-600" />
                        <p className="text-lg font-mono">No notifications</p>
                        <p className="text-sm text-slate-600 mt-1">All clear! 🎉</p>
                    </div>
                ) : (
                    <>
                        <div className="space-y-2">
                            {notifications.map((notification) => (
                                <div
                                    key={notification.id}
                                    className={`p-4 rounded-xl border transition-all ${
                                        notification.isRead
                                            ? 'bg-slate-950/50 border-slate-800 opacity-60'
                                            : 'bg-slate-900 border-slate-700 hover:border-slate-600'
                                    }`}
                                >
                                    <div className="flex items-start gap-3">
                                        {selectMode && (
                                            <input
                                                type="checkbox"
                                                checked={selectedIds.includes(notification.id)}
                                                onChange={() => toggleSelect(notification.id)}
                                                className="mt-1.5 w-4 h-4 rounded border-slate-700 bg-slate-800 text-red-500 focus:ring-red-500 focus:ring-offset-0"
                                            />
                                        )}
                                        <div className="text-2xl flex-shrink-0">
                                            {getTypeIcon(notification.type)}
                                        </div>
                                        <div className="flex-1 min-w-0">
                                            <div className="flex items-center gap-2 flex-wrap">
                                                <h3 className={`text-sm font-bold ${
                                                    notification.isRead ? 'text-slate-400' : 'text-white'
                                                }`}>
                                                    {notification.title}
                                                </h3>
                                                <span className={`text-[10px] font-mono px-2 py-0.5 rounded-full border ${getSeverityColor(notification.severity)}`}>
                                                    {notification.severity}
                                                </span>
                                                {!notification.isRead && (
                                                    <span className="w-2 h-2 bg-red-500 rounded-full animate-pulse flex-shrink-0" />
                                                )}
                                            </div>
                                            <p className={`text-sm mt-1 ${
                                                notification.isRead ? 'text-slate-500' : 'text-slate-300'
                                            }`}>
                                                {notification.message}
                                            </p>
                                            <div className="flex items-center gap-3 mt-2 text-xs text-slate-500 font-mono flex-wrap">
                                                <span>{formatTime(notification.createdAt)}</span>
                                                {notification.actionBy && (
                                                    <span>• By: {notification.actionBy}</span>
                                                )}
                                                {notification.system?.systemCode && (
                                                    <span>• System: {notification.system.systemCode}</span>
                                                )}
                                                {notification.alertId && (
                                                    <span>• Alert #{notification.alertId}</span>
                                                )}
                                            </div>
                                        </div>
                                        <div className="flex items-center gap-1 flex-shrink-0">
                                            {!notification.isRead && (
                                                <button
                                                    onClick={() => handleMarkAsRead(notification.id)}
                                                    className="p-1.5 hover:bg-slate-800 rounded-lg transition-colors text-slate-400 hover:text-blue-400"
                                                    title="Mark as read"
                                                >
                                                    <Eye className="w-4 h-4" />
                                                </button>
                                            )}
                                            <button
                                                onClick={() => handleDelete(notification.id)}
                                                className="p-1.5 hover:bg-slate-800 rounded-lg transition-colors text-slate-500 hover:text-red-400"
                                                title="Delete"
                                            >
                                                <Trash2 className="w-4 h-4" />
                                            </button>
                                        </div>
                                    </div>
                                </div>
                            ))}
                        </div>

                        {/* Load More */}
                        {hasMore && (
                            <button
                                onClick={handleLoadMore}
                                disabled={loading}
                                className="w-full mt-4 py-3 bg-slate-800 hover:bg-slate-700 rounded-xl text-sm font-mono transition-all disabled:opacity-50"
                            >
                                {loading ? (
                                    <Loader2 className="w-4 h-4 animate-spin inline mr-2" />
                                ) : (
                                    'Load More'
                                )}
                            </button>
                        )}
                    </>
                )}
            </div>
        </div>
    );
}

Notifications.propTypes = {
    user: PropTypes.shape({
        username: PropTypes.string.isRequired,
        role: PropTypes.string,
    }).isRequired,
    onBack: PropTypes.func.isRequired,
};