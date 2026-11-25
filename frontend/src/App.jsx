import React, { useState, useEffect } from 'react';
import { useAuth, SEEDED_USERS } from './context/AuthContext';
import Layout from './components/Layout';
import SalesmanWorkspace from './components/workspaces/SalesmanWorkspace';
import ProductionWorkspace from './components/workspaces/ProductionWorkspace';
import AdminWorkspace from './components/workspaces/AdminWorkspace';
import AuditorWorkspace from './components/workspaces/AuditorWorkspace';
import OwnerWorkspace from './components/workspaces/OwnerWorkspace';
import { api } from './services/api';
import { Shield, ArrowRight, Lock, UserCheck } from 'lucide-react';

export default function App() {
  const { currentUser, loginAs, logout } = useAuth();

  const [activeTab, setActiveTab] = useState('');
  const [isOffline, setIsOffline] = useState(false);
  const [queuedOrders, setQueuedOrders] = useState(() => {
    const saved = localStorage.getItem('offline_orders');
    return saved ? JSON.parse(saved) : [];
  });

  // Default activeTab when role switches
  useEffect(() => {
    if (currentUser) {
      switch (currentUser.role) {
        case 'SALESMAN':
          setActiveTab('sales-new');
          break;
        case 'PRODUCTION':
          setActiveTab('prod-run');
          break;
        case 'ADMIN':
          setActiveTab('admin-orders');
          break;
        case 'AUDITOR':
          setActiveTab('auditor-reports');
          break;
        case 'OWNER':
          setActiveTab('owner-dash');
          break;
        default:
          setActiveTab('');
      }
    }
  }, [currentUser]);

  // Listen to network online/offline events
  useEffect(() => {
    const handleOnline = () => setIsOffline(false);
    const handleOffline = () => setIsOffline(true);

    window.addEventListener('online', handleOnline);
    window.addEventListener('offline', handleOffline);

    return () => {
      window.removeEventListener('online', handleOnline);
      window.removeEventListener('offline', handleOffline);
    };
  }, []);

  const handleSyncOffline = async () => {
    if (queuedOrders.length === 0) return;
    let synced = 0;
    const remaining = [];

    for (const order of queuedOrders) {
      try {
        await api.createOrder({
          customerId: order.customerId,
          salesmanId: order.salesmanId,
          lines: order.lines,
        });
        synced++;
      } catch (err) {
        console.error('Failed to sync offline order:', err);
        remaining.push(order);
      }
    }

    setQueuedOrders(remaining);
    localStorage.setItem('offline_orders', JSON.stringify(remaining));
    alert(`Offline Sync Complete! Successfully synced ${synced} order(s).`);
  };

  // If no user is logged in, show Login Screen
  if (!currentUser) {
    return (
      <div className="min-h-screen bg-[#F8FAF6] flex items-center justify-center p-4">
        <div className="max-w-md w-full bg-white border border-[#CFE1B9] rounded-2xl p-8 shadow-lg space-y-6">
          <div className="text-center space-y-2">
            <div className="w-14 h-14 bg-[#728156] rounded-2xl mx-auto flex items-center justify-center text-white text-2xl font-black shadow-md">
              F
            </div>
            <h1 className="text-2xl font-black text-[#1F2617] tracking-tight">FactoryFlow ERP</h1>
            <p className="text-xs text-[#5F6D44] font-medium">
              Snack Factory Manufacturing • Order-to-Audit Suite
            </p>
          </div>

          <div className="space-y-3">
            <span className="text-xs font-bold text-[#4B5633] uppercase tracking-wider block">
              1-Click Demo Login as Seeded User:
            </span>
            <div className="space-y-2">
              {SEEDED_USERS.map((u) => (
                <button
                  key={u.id}
                  onClick={() => loginAs(u.id)}
                  className="w-full p-3 rounded-xl border border-[#CFE1B9] hover:border-[#728156] hover:bg-[#E7F5DC] flex items-center justify-between transition text-left text-xs font-semibold text-[#1F2617] group"
                >
                  <div className="flex items-center gap-2.5">
                    <UserCheck size={16} className="text-[#728156]" />
                    <div>
                      <span className="font-bold block">{u.fullName}</span>
                      <span className="text-[11px] text-[#5F6D44]">{u.username} • {u.roleLabel}</span>
                    </div>
                  </div>
                  <span className="stitch-pill bg-[#CFE1B9] text-[#353D23] group-hover:bg-[#728156] group-hover:text-white transition">
                    {u.role}
                  </span>
                </button>
              ))}
            </div>
          </div>

          <div className="text-center text-[11px] text-[#5F6D44] border-t border-[#E3ECD7] pt-4">
            Default credentials for all accounts: <code className="bg-[#E7F5DC] px-1.5 py-0.5 rounded font-mono text-[#1F2617]">pass123</code>
          </div>
        </div>
      </div>
    );
  }

  return (
    <Layout
      activeTab={activeTab}
      setActiveTab={setActiveTab}
      isOffline={isOffline}
      queuedOrdersCount={queuedOrders.length}
      onSyncOffline={handleSyncOffline}
    >
      {/* Role-Based Dynamic Workspaces */}
      {currentUser.role === 'SALESMAN' && (
        <SalesmanWorkspace
          activeTab={activeTab}
          isOffline={isOffline}
          setIsOffline={setIsOffline}
          queuedOrders={queuedOrders}
          setQueuedOrders={setQueuedOrders}
          onSyncOffline={handleSyncOffline}
        />
      )}

      {currentUser.role === 'PRODUCTION' && (
        <ProductionWorkspace activeTab={activeTab} />
      )}

      {currentUser.role === 'ADMIN' && (
        <AdminWorkspace activeTab={activeTab} />
      )}

      {currentUser.role === 'AUDITOR' && (
        <AuditorWorkspace activeTab={activeTab} />
      )}

      {currentUser.role === 'OWNER' && (
        <OwnerWorkspace activeTab={activeTab} />
      )}
    </Layout>
  );
}
