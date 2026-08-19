// src/components/NotificationBell.jsx

import { useState, useEffect, useRef } from 'react';
import PropTypes from 'prop-types';
import { Bell, BellDot, BellRing } from 'lucide-react';
import NotificationDropdown from './NotificationDropdown';
import { getUnreadCount } from '../services/notificationApi';

export default function NotificationBell({ 
    username, 
    onNotificationClick,
    isCompanyInactive = false  // ← NEW PROP
}) {
    const [unreadCount, setUnreadCount] = useState(0);
    const [criticalCount, setCriticalCount] = useState(0);
    const [isOpen, setIsOpen] = useState(false);
    const [loading, setLoading] = useState(false);
    const dropdownRef = useRef(null);

    // ============================================================
    // FETCH UNREAD COUNT - SKIP IF COMPANY INACTIVE
    // ============================================================

    const fetchUnreadCount = async () => {
        // ===== SKIP if company is inactive =====
        if (isCompanyInactive || !username) {
            setUnreadCount(0);
            setCriticalCount(0);
            return;
        }
        
        setLoading(true);
        try {
            const data = await getUnreadCount(username);
            setUnreadCount(data.unreadCount || 0);
            setCriticalCount(data.criticalCount || 0);
        } catch (error) {
            console.error('Failed to fetch unread count:', error);
        } finally {
            setLoading(false);
        }
    };

    // ============================================================
    // AUTO-REFRESH EVERY 30 SECONDS - SKIP IF INACTIVE
    // ============================================================

    useEffect(() => {
        fetchUnreadCount();
        const interval = setInterval(() => {
            if (!isCompanyInactive) {
                fetchUnreadCount();
            }
        }, 30000);
        return () => clearInterval(interval);
    }, [username, isCompanyInactive]);

    // ============================================================
    // CLOSE DROPDOWN ON CLICK OUTSIDE
    // ============================================================

    useEffect(() => {
        const handleClickOutside = (event) => {
            if (dropdownRef.current && !dropdownRef.current.contains(event.target)) {
                setIsOpen(false);
            }
        };
        document.addEventListener('mousedown', handleClickOutside);
        return () => document.removeEventListener('mousedown', handleClickOutside);
    }, []);

    // ============================================================
    // TOGGLE DROPDOWN
    // ============================================================

    const toggleDropdown = () => {
        // ===== Don't open if company is inactive =====
        if (isCompanyInactive) {
            return;
        }
        
        if (!isOpen) {
            fetchUnreadCount();
        }
        setIsOpen(!isOpen);
    };

    // ============================================================
    // HANDLE NOTIFICATION CLICK
    // ============================================================

    const handleNotificationClick = () => {
        setIsOpen(false);
        if (onNotificationClick) {
            onNotificationClick();
        }
    };

    // ============================================================
    // RENDER
    // ============================================================

    // ===== If company inactive, show disabled bell =====
    if (isCompanyInactive) {
        return (
            <div className="relative" ref={dropdownRef}>
                <button
                    onClick={toggleDropdown}
                    className="relative p-2 rounded-lg opacity-40 cursor-not-allowed"
                    aria-label="Notifications disabled"
                    title="Company is inactive. Notifications disabled."
                    disabled
                >
                    <Bell className="w-5 h-5 text-slate-500" />
                    <span className="absolute -top-0.5 -right-0.5 text-[8px] font-bold rounded-full min-w-[16px] h-[16px] flex items-center justify-center px-1 bg-slate-600 text-slate-300">
                        !
                    </span>
                </button>
            </div>
        );
    }

    const getBellIcon = () => {
        if (criticalCount > 0) {
            return <BellRing className="w-5 h-5 text-red-500 animate-pulse" />;
        }
        if (unreadCount > 0) {
            return <BellDot className="w-5 h-5 text-yellow-400" />;
        }
        return <Bell className="w-5 h-5 text-slate-400" />;
    };

    return (
        <div className="relative" ref={dropdownRef}>
            <button
                onClick={toggleDropdown}
                className="relative p-2 rounded-lg hover:bg-slate-800 transition-colors"
                aria-label="Notifications"
                title={`${unreadCount} unread notifications`}
            >
                {getBellIcon()}
                
                {unreadCount > 0 && (
                    <span className={`absolute -top-0.5 -right-0.5 text-[9px] font-bold rounded-full min-w-[18px] h-[18px] flex items-center justify-center px-1 ${
                        criticalCount > 0 
                            ? 'bg-red-500 text-white animate-pulse' 
                            : 'bg-yellow-500 text-black'
                    }`}>
                        {unreadCount > 99 ? '99+' : unreadCount}
                    </span>
                )}
                
                {loading && (
                    <span className="absolute -bottom-0.5 -right-0.5 w-2 h-2 bg-slate-500 rounded-full animate-pulse" />
                )}
            </button>

            {isOpen && !isCompanyInactive && (
                <div className="absolute right-0 mt-2 w-[400px] max-w-[calc(100vw-20px)] z-50">
                    <NotificationDropdown
                        username={username}
                        onClose={() => setIsOpen(false)}
                        onUnreadChange={fetchUnreadCount}
                        onNotificationClick={handleNotificationClick}
                        isCompanyInactive={isCompanyInactive}  // ← PASS TO DROPDOWN
                    />
                </div>
            )}
        </div>
    );
}

NotificationBell.propTypes = {
    username: PropTypes.string.isRequired,
    onNotificationClick: PropTypes.func,
    isCompanyInactive: PropTypes.bool,  // ← NEW PROP
};