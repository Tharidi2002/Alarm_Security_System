import React, { useState } from 'react';
import { User, Shield, Bell, Database, Settings as SettingsIcon } from 'lucide-react';
import toast from 'react-hot-toast';
import Layout from '../components/Layout/Layout';

const SettingsPage = () => {
  const [activeTab, setActiveTab] = useState('profile');

  const tabs = [
    { id: 'profile', label: 'Profile', icon: User },
    { id: 'security', label: 'Security', icon: Shield },
    { id: 'notifications', label: 'Notifications', icon: Bell },
    { id: 'system', label: 'System', icon: Database },
  ];

  const handleSave = () => {
    toast.success('Settings saved successfully!');
  };

  return (
    <Layout>
      <div className="space-y-6">
        {/* Header */}
        <div className="flex items-center justify-between">
          <div>
            <h1 className="text-2xl font-bold text-white">Settings</h1>
            <p className="text-slate-400 text-sm">Manage your account and system preferences</p>
          </div>
          <div className="bg-slate-800/50 p-2 rounded-lg border border-slate-700">
            <SettingsIcon className="w-5 h-5 text-slate-400" />
          </div>
        </div>

        {/* Tabs */}
        <div className="bg-slate-950 border border-slate-800 rounded-xl overflow-hidden">
          <div className="flex border-b border-slate-800 overflow-x-auto">
            {tabs.map((tab) => (
              <button
                key={tab.id}
                onClick={() => setActiveTab(tab.id)}
                className={`flex items-center gap-2 px-4 md:px-6 py-3 text-xs md:text-sm font-medium transition-colors whitespace-nowrap ${
                  activeTab === tab.id
                    ? 'text-red-400 border-b-2 border-red-400 bg-red-500/5'
                    : 'text-slate-400 hover:text-white hover:bg-slate-800/50'
                }`}
              >
                <tab.icon className="w-4 h-4" />
                {tab.label}
              </button>
            ))}
          </div>

          <div className="p-6">
            {activeTab === 'profile' && (
              <div className="space-y-4">
                <h3 className="text-lg font-semibold text-white">Profile Settings</h3>
                <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                  <div>
                    <label className="block text-sm text-slate-400 mb-1">Full Name</label>
                    <input
                      type="text"
                      defaultValue="Admin User"
                      className="w-full bg-slate-900 border border-slate-700 rounded-lg px-3 py-2 text-white focus:outline-none focus:border-red-500"
                    />
                  </div>
                  <div>
                    <label className="block text-sm text-slate-400 mb-1">Email</label>
                    <input
                      type="email"
                      defaultValue="admin@system.com"
                      className="w-full bg-slate-900 border border-slate-700 rounded-lg px-3 py-2 text-white focus:outline-none focus:border-red-500"
                    />
                  </div>
                  <div>
                    <label className="block text-sm text-slate-400 mb-1">Role</label>
                    <input
                      type="text"
                      defaultValue="Administrator"
                      disabled
                      className="w-full bg-slate-900/50 border border-slate-700 rounded-lg px-3 py-2 text-slate-400 cursor-not-allowed"
                    />
                  </div>
                  <div>
                    <label className="block text-sm text-slate-400 mb-1">Bank</label>
                    <input
                      type="text"
                      defaultValue="National Bank"
                      disabled
                      className="w-full bg-slate-900/50 border border-slate-700 rounded-lg px-3 py-2 text-slate-400 cursor-not-allowed"
                    />
                  </div>
                </div>
                <button
                  onClick={handleSave}
                  className="bg-red-500 hover:bg-red-600 px-6 py-2 rounded-lg font-medium transition-colors"
                >
                  Save Changes
                </button>
              </div>
            )}

            {activeTab === 'security' && (
              <div className="space-y-4">
                <h3 className="text-lg font-semibold text-white">Security Settings</h3>
                <div className="space-y-3">
                  <div>
                    <label className="block text-sm text-slate-400 mb-1">Current Password</label>
                    <input
                      type="password"
                      placeholder="Enter current password"
                      className="w-full bg-slate-900 border border-slate-700 rounded-lg px-3 py-2 text-white focus:outline-none focus:border-red-500"
                    />
                  </div>
                  <div>
                    <label className="block text-sm text-slate-400 mb-1">New Password</label>
                    <input
                      type="password"
                      placeholder="Enter new password"
                      className="w-full bg-slate-900 border border-slate-700 rounded-lg px-3 py-2 text-white focus:outline-none focus:border-red-500"
                    />
                  </div>
                  <div>
                    <label className="block text-sm text-slate-400 mb-1">Confirm Password</label>
                    <input
                      type="password"
                      placeholder="Confirm new password"
                      className="w-full bg-slate-900 border border-slate-700 rounded-lg px-3 py-2 text-white focus:outline-none focus:border-red-500"
                    />
                  </div>
                </div>
                <button
                  onClick={handleSave}
                  className="bg-red-500 hover:bg-red-600 px-6 py-2 rounded-lg font-medium transition-colors"
                >
                  Update Password
                </button>
              </div>
            )}

            {activeTab === 'notifications' && (
              <div className="space-y-4">
                <h3 className="text-lg font-semibold text-white">Notification Preferences</h3>
                <div className="space-y-3">
                  <label className="flex items-center gap-3 text-slate-300 cursor-pointer">
                    <input type="checkbox" defaultChecked className="w-4 h-4 accent-red-500" />
                    Email notifications for critical alerts
                  </label>
                  <label className="flex items-center gap-3 text-slate-300 cursor-pointer">
                    <input type="checkbox" defaultChecked className="w-4 h-4 accent-red-500" />
                    SMS notifications for high severity alerts
                  </label>
                  <label className="flex items-center gap-3 text-slate-300 cursor-pointer">
                    <input type="checkbox" className="w-4 h-4 accent-red-500" />
                    Daily summary reports
                  </label>
                  <label className="flex items-center gap-3 text-slate-300 cursor-pointer">
                    <input type="checkbox" className="w-4 h-4 accent-red-500" />
                    Real-time push notifications
                  </label>
                </div>
                <button
                  onClick={handleSave}
                  className="bg-red-500 hover:bg-red-600 px-6 py-2 rounded-lg font-medium transition-colors"
                >
                  Save Preferences
                </button>
              </div>
            )}

            {activeTab === 'system' && (
              <div className="space-y-4">
                <h3 className="text-lg font-semibold text-white">System Settings</h3>
                <div className="space-y-3">
                  <div>
                    <label className="block text-sm text-slate-400 mb-1">Alert Retention Period</label>
                    <select className="w-full bg-slate-900 border border-slate-700 rounded-lg px-3 py-2 text-white focus:outline-none focus:border-red-500">
                      <option>30 days</option>
                      <option>60 days</option>
                      <option selected>90 days</option>
                      <option>180 days</option>
                      <option>365 days</option>
                    </select>
                  </div>
                  <div>
                    <label className="block text-sm text-slate-400 mb-1">Auto-Refresh Interval</label>
                    <select className="w-full bg-slate-900 border border-slate-700 rounded-lg px-3 py-2 text-white focus:outline-none focus:border-red-500">
                      <option>5 seconds</option>
                      <option selected>10 seconds</option>
                      <option>30 seconds</option>
                      <option>1 minute</option>
                      <option>5 minutes</option>
                    </select>
                  </div>
                  <div>
                    <label className="block text-sm text-slate-400 mb-1">Default Alert View</label>
                    <select className="w-full bg-slate-900 border border-slate-700 rounded-lg px-3 py-2 text-white focus:outline-none focus:border-red-500">
                      <option selected>All Alerts</option>
                      <option>Pending Only</option>
                      <option>High Severity</option>
                      <option>Today's Alerts</option>
                    </select>
                  </div>
                </div>
                <button
                  onClick={handleSave}
                  className="bg-red-500 hover:bg-red-600 px-6 py-2 rounded-lg font-medium transition-colors"
                >
                  Save System Settings
                </button>
              </div>
            )}
          </div>
        </div>
      </div>
    </Layout>
  );
};

export default SettingsPage;