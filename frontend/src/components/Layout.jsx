import React from 'react';
import { useAuth } from '../context/AuthContext';
import DemoSwitcher from './DemoSwitcher';
import { 
  Building2, 
  ShoppingBag, 
  Factory, 
  LayoutDashboard, 
  FileText, 
  TrendingUp, 
  Layers,
  LogOut,
  Wifi,
  WifiOff
} from 'lucide-react';

export default function Layout({ activeTab, setActiveTab, isOffline, queuedOrdersCount, onSyncOffline, children }) {
  const { currentUser } = useAuth();

  const getRoleNavItems = () => {
    if (!currentUser) return [];

    switch (currentUser.role) {
      case 'SALESMAN':
        return [
          { id: 'sales-new', label: 'New Order', icon: ShoppingBag },
          { id: 'sales-orders', label: 'My Orders', icon: FileText },
        ];
      case 'PRODUCTION':
        return [
          { id: 'prod-run', label: 'BOM Production', icon: Factory },
          { id: 'prod-purchase', label: 'Raw Purchases', icon: Layers },
          { id: 'prod-history', label: 'Run History', icon: FileText },
        ];
      case 'ADMIN':
        return [
          { id: 'admin-orders', label: 'Orders & Dispatch', icon: LayoutDashboard },
          { id: 'admin-invoices', label: 'Invoices & PDF', icon: FileText },
          { id: 'admin-payments', label: 'Payments', icon: TrendingUp },
          { id: 'admin-stock', label: 'Live Inventory', icon: Layers },
        ];
      case 'AUDITOR':
        return [
          { id: 'auditor-reports', label: 'Audit Registers & GSTR', icon: FileText },
          { id: 'auditor-ledger', label: 'Full Stock Ledger', icon: Layers },
        ];
      case 'OWNER':
        return [
          { id: 'owner-dash', label: 'Executive KPIs', icon: LayoutDashboard },
          { id: 'owner-stock', label: 'Raw Stock & Inventory', icon: Layers },
        ];
      default:
        return [];
    }
  };

  const navItems = getRoleNavItems();

  return (
    <div className="min-h-screen flex flex-col bg-[#F8FAF6]">
      {/* Top Header */}
      <header className="bg-white border-b border-[#E3ECD7] sticky top-0 z-30 px-4 py-2.5 flex items-center justify-between shadow-sm">
        <div className="flex items-center gap-3">
          <div className="w-9 h-9 rounded-xl bg-[#728156] flex items-center justify-center text-white font-bold text-lg shadow-sm">
            F
          </div>
          <div>
            <h1 className="font-extrabold text-[#1F2617] text-base leading-tight tracking-tight flex items-center gap-2">
              FactoryFlow ERP
              <span className="text-[10px] font-semibold bg-[#CFE1B9] text-[#353D23] px-2 py-0.5 rounded-full">
                Phase 5
              </span>
            </h1>
            <p className="text-[11px] text-[#5F6D44] font-medium hidden sm:block">
              Snack Manufacturing • Order to Audit
            </p>
          </div>
        </div>

        {/* Center / Right controls */}
        <div className="flex items-center gap-3">
          {/* Offline indicator if relevant */}
          {typeof isOffline !== 'undefined' && (
            <div className="flex items-center gap-2">
              {isOffline ? (
                <span className="inline-flex items-center gap-1.5 px-2.5 py-1 bg-red-100 text-red-800 rounded-full text-xs font-semibold animate-pulse">
                  <WifiOff size={13} /> Offline Mode
                </span>
              ) : (
                <span className="hidden md:inline-flex items-center gap-1.5 px-2.5 py-1 bg-green-100 text-green-800 rounded-full text-xs font-semibold">
                  <Wifi size={13} /> Online
                </span>
              )}

              {queuedOrdersCount > 0 && (
                <button
                  onClick={onSyncOffline}
                  className="bg-[#C63D3D] text-white px-2.5 py-1 rounded-full text-xs font-bold hover:bg-red-700 transition"
                  title="Click to sync offline queued orders"
                >
                  Sync ({queuedOrdersCount})
                </button>
              )}
            </div>
          )}

          {/* Quick Demo Switcher */}
          <DemoSwitcher />
        </div>
      </header>

      {/* Role Navigation Bar */}
      <nav className="bg-[#CFE1B9]/40 border-b border-[#CFE1B9] px-4 py-1.5 flex items-center gap-1 overflow-x-auto text-xs">
        <span className="font-bold text-[#4B5633] uppercase tracking-wider text-[10px] mr-2 flex items-center gap-1">
          <span className="w-2 h-2 rounded-full bg-[#728156]"></span>
          {currentUser?.roleLabel || currentUser?.role}
        </span>
        {navItems.map((item) => {
          const Icon = item.icon;
          const isActive = activeTab === item.id;
          return (
            <button
              key={item.id}
              onClick={() => setActiveTab(item.id)}
              className={`flex items-center gap-1.5 px-3 py-1.5 rounded-lg font-semibold transition whitespace-nowrap ${
                isActive
                  ? 'bg-[#728156] text-white shadow-sm'
                  : 'text-[#353D23] hover:bg-[#CFE1B9]/80'
              }`}
            >
              <Icon size={14} />
              <span>{item.label}</span>
            </button>
          );
        })}
      </nav>

      {/* Main Content Area */}
      <main className="flex-1 p-4 md:p-6 max-w-7xl w-full mx-auto">
        {children}
      </main>

      {/* Footer */}
      <footer className="border-t border-[#E3ECD7] py-3 text-center text-xs text-[#5F6D44] bg-white">
        FactoryFlow ERP • Stitch Design System • Sage & Olive (#728156 / #CFE1B9) • Local Time +05:30
      </footer>
    </div>
  );
}
