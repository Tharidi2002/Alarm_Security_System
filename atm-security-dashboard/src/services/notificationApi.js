// src/services/notificationApi.js

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api';

// ============================================================
// GET NOTIFICATIONS
// ============================================================

export const getNotifications = async (username, unreadOnly = false, page = 0, size = 20) => {
    try {
        const params = new URLSearchParams();
        params.append('username', username);
        if (unreadOnly) params.append('unreadOnly', 'true');
        params.append('page', page);
        params.append('size', size);

        const response = await fetch(`${API_BASE_URL}/notifications?${params}`);
        if (!response.ok) {
            const error = await response.text();
            throw new Error(error || 'Failed to fetch notifications');
        }
        return await response.json();
    } catch (error) {
        console.error('Error fetching notifications:', error);
        throw error;
    }
};

// ============================================================
// GET UNREAD COUNT
// ============================================================

export const getUnreadCount = async (username) => {
    try {
        const response = await fetch(`${API_BASE_URL}/notifications/unread/count?username=${encodeURIComponent(username)}`);
        if (!response.ok) {
            throw new Error('Failed to fetch unread count');
        }
        return await response.json();
    } catch (error) {
        console.error('Error fetching unread count:', error);
        return { unreadCount: 0, criticalCount: 0 };
    }
};

// ============================================================
// MARK AS READ
// ============================================================

export const markAsRead = async (notificationId, username) => {
    try {
        const response = await fetch(
            `${API_BASE_URL}/notifications/${notificationId}/read?username=${encodeURIComponent(username)}`,
            { method: 'PUT' }
        );
        if (!response.ok) {
            const error = await response.text();
            throw new Error(error || 'Failed to mark as read');
        }
        return await response.json();
    } catch (error) {
        console.error('Error marking as read:', error);
        throw error;
    }
};

// ============================================================
// MARK ALL AS READ
// ============================================================

export const markAllAsRead = async (username) => {
    try {
        const response = await fetch(
            `${API_BASE_URL}/notifications/read-all?username=${encodeURIComponent(username)}`,
            { method: 'PUT' }
        );
        if (!response.ok) {
            const error = await response.text();
            throw new Error(error || 'Failed to mark all as read');
        }
        return await response.json();
    } catch (error) {
        console.error('Error marking all as read:', error);
        throw error;
    }
};

// ============================================================
// MARK MULTIPLE AS READ
// ============================================================

export const markMultipleAsRead = async (ids, username) => {
    try {
        const response = await fetch(
            `${API_BASE_URL}/notifications/read-multiple?username=${encodeURIComponent(username)}`,
            {
                method: 'PUT',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ ids })
            }
        );
        if (!response.ok) {
            const error = await response.text();
            throw new Error(error || 'Failed to mark as read');
        }
        return await response.json();
    } catch (error) {
        console.error('Error marking multiple as read:', error);
        throw error;
    }
};

// ============================================================
// DELETE NOTIFICATION
// ============================================================

export const deleteNotification = async (notificationId, username) => {
    try {
        const response = await fetch(
            `${API_BASE_URL}/notifications/${notificationId}?username=${encodeURIComponent(username)}`,
            { method: 'DELETE' }
        );
        if (!response.ok) {
            const error = await response.text();
            throw new Error(error || 'Failed to delete notification');
        }
        return await response.text();
    } catch (error) {
        console.error('Error deleting notification:', error);
        throw error;
    }
};

// ============================================================
// GET SINGLE NOTIFICATION
// ============================================================

export const getNotification = async (notificationId, username) => {
    try {
        const response = await fetch(
            `${API_BASE_URL}/notifications/${notificationId}?username=${encodeURIComponent(username)}`
        );
        if (!response.ok) {
            throw new Error('Failed to fetch notification');
        }
        return await response.json();
    } catch (error) {
        console.error('Error fetching notification:', error);
        throw error;
    }
};