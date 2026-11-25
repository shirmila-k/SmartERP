import React, { useState, useEffect } from 'react';
import { api } from '../../services/api';
import { 
  TrendingUp, 
  Package, 
  AlertTriangle, 
  DollarSign, 
  Layers, 
  ArrowUpRight,
  RefreshCw,
  Percent,
  CheckCircle2
} from 'lucide-react';

export default function OwnerWorkspace() {
  const [dashboard, setDashboard] = useState(null);
  const [balances, setBalances] = useState([]);
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    loadData();
  }, []);

  const loadData = async () => {
    try {
      setLoading(true);
      const [dash, balList] = await Promise.all([
        api.getOwnerDashboard(),
        api.getInventoryBalances(),
      ]);
      setDashboard(dash);
      setBalances(balList);
    } catch (e) {
      console.error(e);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="space-y-6">
      {/* Header Bar */}
      <div className="flex items-center justify-between">
        <div>
          <h2 className="text-xl font-black text-[#1F2617] tracking-tight">Factory Owner Executive Overview</h2>
          <p className="text-xs text-[#5F6D44]">High-level operational throughput, profit margins, and active factory alerts</p>
        </div>
        <button
          onClick={loadData}
          className="p-2 rounded-xl border border-[#CFE1B9] hover:bg-[#E7F5DC] text-[#4B5633] transition"
          title="Refresh KPIs"
        >
          <RefreshCw size={15} className={loading ? 'animate-spin' : ''} />
        </button>
      </div>

      {dashboard && (
        <>
          {/* Top 4 KPI Cards Matching Stitch Layout */}
          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
            {/* Card 1: Today's Orders */}
            <div className="stitch-card p-4 space-y-2 bg-white">
              <div className="flex justify-between items-center text-[#5F6D44]">
                <span className="text-xs font-bold uppercase tracking-wider">Today's Orders</span>
                <span className="p-2 rounded-xl bg-[#E7F5DC] text-[#728156]">
                  <TrendingUp size={16} />
                </span>
              </div>
              <div className="space-y-0.5">
                <span className="text-2xl font-black text-[#1F2617]">₹{dashboard.todaysOrdersValue}</span>
                <span className="text-xs text-[#5F6D44] block font-medium">
                  {dashboard.todaysOrdersCount} new order{dashboard.todaysOrdersCount === 1 ? '' : 's'} booked today
                </span>
              </div>
            </div>

            {/* Card 2: Finished Goods Stock */}
            <div className="stitch-card p-4 space-y-2 bg-white">
              <div className="flex justify-between items-center text-[#5F6D44]">
                <span className="text-xs font-bold uppercase tracking-wider">Finished Goods Stock</span>
                <span className="p-2 rounded-xl bg-[#E7F5DC] text-[#728156]">
                  <Package size={16} />
                </span>
              </div>
              <div className="space-y-0.5">
                {dashboard.finishedGoodsStock.map((fg) => (
                  <div key={fg.itemId} className="flex justify-between items-baseline">
                    <span className="text-2xl font-black text-[#1F2617]">{fg.currentBalance} pkts</span>
                    <span
                      className={`stitch-pill ${
                        fg.belowReorderLevel ? 'bg-red-100 text-[#C63D3D]' : 'bg-green-100 text-green-800'
                      }`}
                    >
                      {fg.belowReorderLevel ? 'Low' : 'Healthy'}
                    </span>
                  </div>
                ))}
                <span className="text-[11px] text-[#5F6D44] block">Murukku 200 g ready for dispatch</span>
              </div>
            </div>

            {/* Card 3: Active Variance Alerts */}
            <div className="stitch-card p-4 space-y-2 bg-white">
              <div className="flex justify-between items-center text-[#5F6D44]">
                <span className="text-xs font-bold uppercase tracking-wider">Active Variance Alerts</span>
                <span className="p-2 rounded-xl bg-red-50 text-[#C63D3D]">
                  <AlertTriangle size={16} />
                </span>
              </div>
              <div className="space-y-0.5">
                <span className="text-2xl font-black text-[#C63D3D]">
                  {dashboard.activeVarianceAlerts.length}
                </span>
                <span className="text-xs text-[#5F6D44] block font-medium">
                  Batch variances exceeding ±3%
                </span>
              </div>
            </div>

            {/* Card 4: Customer Outstanding Dues */}
            <div className="stitch-card p-4 space-y-2 bg-white">
              <div className="flex justify-between items-center text-[#5F6D44]">
                <span className="text-xs font-bold uppercase tracking-wider">Customer Outstanding</span>
                <span className="p-2 rounded-xl bg-[#E7F5DC] text-[#728156]">
                  <DollarSign size={16} />
                </span>
              </div>
              <div className="space-y-0.5">
                <span className="text-2xl font-black text-[#1F2617]">₹{dashboard.totalOutstandingDues}</span>
                <span className="text-xs text-[#5F6D44] block font-medium">
                  Total unpaid invoice receivables
                </span>
              </div>
            </div>
          </div>

          {/* Middle Row: Latest Cost & Margin Card + Variance Details */}
          <div className="grid grid-cols-1 md:grid-cols-12 gap-6">
            {/* Cost Per Packet & Margin */}
            <div className="md:col-span-5 stitch-card p-5 space-y-4 bg-gradient-to-br from-white to-[#E7F5DC]/40">
              <div className="flex justify-between items-center border-b border-[#E3ECD7] pb-2">
                <h3 className="font-bold text-sm text-[#1F2617] uppercase tracking-wider">
                  Latest Batch Cost vs Selling Margin
                </h3>
                <span className="p-1.5 bg-[#CFE1B9] text-[#353D23] rounded-lg">
                  <Percent size={14} />
                </span>
              </div>

              {dashboard.latestCostPerPacket ? (
                <div className="space-y-4">
                  <div className="p-4 bg-white rounded-xl border border-[#CFE1B9] space-y-2">
                    <span className="text-xs text-[#5F6D44] block font-medium">
                      Product: <strong className="text-[#1F2617]">{dashboard.latestCostPerPacket.productName}</strong>
                    </span>
                    <div className="flex justify-between items-baseline">
                      <div>
                        <span className="text-[11px] text-[#5F6D44] block uppercase">FIFO Cost / Packet</span>
                        <span className="text-3xl font-black text-[#1F2617]">
                          ₹{dashboard.latestCostPerPacket.calculatedUnitCost}
                        </span>
                      </div>
                      <div className="text-right">
                        <span className="text-[11px] text-[#5F6D44] block uppercase">Selling Price</span>
                        <span className="text-xl font-bold text-[#728156]">
                          ₹{dashboard.latestCostPerPacket.sellingPrice}
                        </span>
                      </div>
                    </div>

                    <div className="pt-2 border-t border-[#E3ECD7] flex justify-between items-center">
                      <span className="text-xs font-bold text-[#1F2617]">Gross Profit Margin:</span>
                      <span className="stitch-pill bg-green-100 text-green-800 text-sm font-extrabold">
                        {dashboard.latestCostPerPacket.grossMarginPct}%
                      </span>
                    </div>
                  </div>

                  <p className="text-[11px] text-[#5F6D44]">
                    Formula: (₹50.00 − ₹{dashboard.latestCostPerPacket.calculatedUnitCost}) ÷ ₹50.00 = <strong>{dashboard.latestCostPerPacket.grossMarginPct}%</strong>
                  </p>
                </div>
              ) : (
                <p className="text-xs text-[#5F6D44]">No production runs logged yet.</p>
              )}
            </div>

            {/* Active Variance Alerts List */}
            <div className="md:col-span-7 stitch-card p-5 space-y-3">
              <h3 className="font-bold text-sm text-[#1F2617] uppercase tracking-wider border-b border-[#E3ECD7] pb-2">
                Production Variance Alerts (Action Required)
              </h3>

              {dashboard.activeVarianceAlerts.length === 0 ? (
                <div className="p-6 text-center text-xs text-green-700 bg-green-50 rounded-xl border border-green-200 flex items-center justify-center gap-2">
                  <CheckCircle2 size={16} /> All batch runs are operating within the standard ±3% tolerance limit!
                </div>
              ) : (
                <div className="space-y-2.5">
                  {dashboard.activeVarianceAlerts.map((va) => (
                    <div
                      key={va.runId}
                      className="p-3 bg-red-50/60 border border-red-200 rounded-xl flex items-start justify-between text-xs"
                    >
                      <div className="space-y-1">
                        <div className="flex items-center gap-2">
                          <span className="font-bold text-[#1F2617]">{va.runCode}</span>
                          <span className="text-[10px] text-[#5F6D44]">({va.runDate})</span>
                        </div>
                        <p className="text-[#1F2617] font-medium">{va.productName}</p>
                        {va.flaggedConsumptions?.map((c, idx) => (
                          <span key={idx} className="inline-block bg-white border border-red-200 text-red-800 px-2 py-0.5 rounded text-[10px] font-semibold mr-1.5">
                            {c}
                          </span>
                        ))}
                      </div>

                      <span className="stitch-pill bg-red-100 text-[#C63D3D] font-black text-xs">
                        Yield Var: {va.yieldVariancePct}%
                      </span>
                    </div>
                  ))}
                </div>
              )}
            </div>
          </div>

          {/* Bottom Row: Live Raw Material Stock Level Bars */}
          <div className="stitch-card p-5 space-y-4">
            <div className="flex justify-between items-center border-b border-[#E3ECD7] pb-2">
              <h3 className="font-bold text-sm text-[#1F2617] uppercase tracking-wider flex items-center gap-2">
                <Layers size={16} className="text-[#728156]" /> Live Factory Inventory Stock Levels
              </h3>
              <span className="text-xs text-[#5F6D44]">Dynamic stock_ledger derivation</span>
            </div>

            <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 gap-4">
              {balances.map((b) => {
                const maxLevel = Math.max(parseFloat(b.reorderLevel) * 3, parseFloat(b.currentBalance) * 1.2, 100);
                const pct = Math.min(100, Math.max(5, (parseFloat(b.currentBalance) / maxLevel) * 100));

                return (
                  <div key={b.itemId} className="p-3 bg-[#F8FAF6] border border-[#CFE1B9] rounded-xl space-y-2 text-xs">
                    <div className="flex justify-between items-center">
                      <span className="font-bold text-[#1F2617]">{b.name}</span>
                      <span
                        className={`stitch-pill text-[10px] ${
                          b.belowReorderLevel ? 'bg-red-100 text-[#C63D3D]' : 'bg-green-100 text-green-800'
                        }`}
                      >
                        {b.currentBalance} {b.unit}
                      </span>
                    </div>

                    {/* Visual Progress Bar */}
                    <div className="w-full bg-[#CFE1B9]/60 rounded-full h-2.5 overflow-hidden">
                      <div
                        className={`h-2.5 rounded-full transition-all duration-500 ${
                          b.belowReorderLevel ? 'bg-[#C63D3D]' : 'bg-[#728156]'
                        }`}
                        style={{ width: `${pct}%` }}
                      ></div>
                    </div>

                    <div className="flex justify-between text-[10px] text-[#5F6D44]">
                      <span>SKU: {b.skuCode}</span>
                      <span>Reorder Level: {b.reorderLevel} {b.unit}</span>
                    </div>
                  </div>
                );
              })}
            </div>
          </div>
        </>
      )}
    </div>
  );
}
