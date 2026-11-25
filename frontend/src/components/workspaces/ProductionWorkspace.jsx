import React, { useState, useEffect } from 'react';
import { useAuth } from '../../context/AuthContext';
import { api } from '../../services/api';
import { Factory, ShoppingCart, AlertCircle, CheckCircle2, AlertTriangle, Layers, Percent } from 'lucide-react';

export default function ProductionWorkspace({ activeTab }) {
  const { currentUser } = useAuth();

  const [finishedItems, setFinishedItems] = useState([]);
  const [rawMaterials, setRawMaterials] = useState([]);
  const [selectedFgId, setSelectedFgId] = useState('');
  const [bom, setBom] = useState(null);

  // Production Run form
  const [batchesPlanned, setBatchesPlanned] = useState('1.00');
  const [rawInputs, setRawInputs] = useState({}); // itemId -> actualQty
  const [actualGoodPackets, setActualGoodPackets] = useState(188);
  const [damagedPouches, setDamagedPouches] = useState(6);
  const [cartonsUsed, setCartonsUsed] = useState(9);
  const [labourCost, setLabourCost] = useState('1200.00');
  const [overheadCost, setOverheadCost] = useState('600.00');

  // Purchase form
  const [supplierName, setSupplierName] = useState('Sri Krishna Traders');
  const [invoiceNo, setInvoiceNo] = useState('INV-2026-001');
  const [purchaseItemId, setPurchaseItemId] = useState('');
  const [purchaseQty, setPurchaseQty] = useState('60.000');
  const [purchaseRate, setPurchaseRate] = useState('45.00');
  const [purchaseDate, setPurchaseDate] = useState(new Date().toISOString().split('T')[0]);

  // History & Feedback
  const [runs, setRuns] = useState([]);
  const [recentCompletedRun, setRecentCompletedRun] = useState(null);
  const [loading, setLoading] = useState(false);
  const [msg, setMsg] = useState({ type: '', text: '' });

  useEffect(() => {
    loadData();
  }, []);

  const loadData = async () => {
    try {
      const [allItms, runList] = await Promise.all([api.getItems(), api.getProductionRuns()]);
      const fg = allItms.filter((i) => i.itemType === 'FINISHED_GOOD');
      const raw = allItms.filter((i) => i.itemType !== 'FINISHED_GOOD');
      setFinishedItems(fg);
      setRawMaterials(raw);
      setRuns(runList);

      if (fg.length > 0 && !selectedFgId) {
        setSelectedFgId(fg[0].id);
        loadBom(fg[0].id);
      }
      if (raw.length > 0 && !purchaseItemId) {
        setPurchaseItemId(raw[0].id);
      }
    } catch (e) {
      console.error(e);
    }
  };

  const loadBom = async (fgId) => {
    try {
      const b = await api.getBom(fgId);
      setBom(b);
      // Pre-fill default inputs for benchmark (40 flour, 10.5 oil, 1.2 spice)
      const initial = {};
      b.lines.forEach((l) => {
        if (!l.packaging) {
          if (l.componentSkuCode === 'RM-FLOUR') initial[l.componentItemId] = '40.000';
          else if (l.componentSkuCode === 'RM-OIL') initial[l.componentItemId] = '10.500';
          else if (l.componentSkuCode === 'RM-SPICE') initial[l.componentItemId] = '1.200';
          else initial[l.componentItemId] = (l.standardQty * parseFloat(batchesPlanned)).toFixed(3);
        }
      });
      setRawInputs(initial);
    } catch (e) {
      console.error('Failed to load BOM:', e);
    }
  };

  // Live variance calculations
  const expectedOutput = bom ? bom.expectedYieldPackets * parseFloat(batchesPlanned || 1) : 200;
  const yieldVariancePct = expectedOutput > 0
    ? (((actualGoodPackets - expectedOutput) / expectedOutput) * 100).toFixed(2)
    : '0.00';
  const isYieldFlagged = Math.abs(parseFloat(yieldVariancePct)) > 3.0;

  const handleProductionRunSubmit = async (e) => {
    e.preventDefault();
    setMsg({ type: '', text: '' });

    const rawConsumptions = Object.entries(rawInputs).map(([k, v]) => ({
      itemId: Number(k),
      actualQty: parseFloat(v),
    }));

    const payload = {
      runCode: 'RUN-' + Date.now().toString().slice(-6),
      finishedItemId: Number(selectedFgId),
      batchesPlanned: parseFloat(batchesPlanned),
      rawConsumptions,
      actualGoodPackets: Number(actualGoodPackets),
      damagedPouches: Number(damagedPouches),
      cartonsUsed: Number(cartonsUsed),
      labourCost: parseFloat(labourCost),
      overheadCost: parseFloat(overheadCost),
      runDate: new Date().toISOString().split('T')[0],
      createdBy: currentUser.id,
    };

    try {
      setLoading(true);
      const res = await api.createProductionRun(payload);
      setRecentCompletedRun(res);
      setMsg({
        type: 'success',
        text: `Production Run ${res.runCode} successfully completed! Cost: ₹${res.calculatedUnitCost}/packet. Total: ₹${res.totalBatchCost}`,
      });
      loadData();
    } catch (err) {
      setMsg({ type: 'error', text: err.message || 'Production Run Failed' });
    } finally {
      setLoading(false);
    }
  };

  const handlePurchaseSubmit = async (e) => {
    e.preventDefault();
    setMsg({ type: '', text: '' });

    const payload = {
      supplierName,
      invoiceNo,
      purchaseDate,
      itemId: Number(purchaseItemId),
      qty: parseFloat(purchaseQty),
      unitRate: parseFloat(purchaseRate),
      createdBy: currentUser.id,
    };

    try {
      setLoading(true);
      const res = await api.createPurchase(payload);
      setMsg({
        type: 'success',
        text: `Purchased ${res.qty} ${res.unit} of ${res.itemName} @ ₹${res.unitRate}. Total: ₹${res.totalAmount}`,
      });
      loadData();
    } catch (err) {
      setMsg({ type: 'error', text: err.message || 'Purchase Failed' });
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="space-y-6 max-w-5xl mx-auto">
      {msg.text && (
        <div
          className={`p-3.5 rounded-xl text-sm flex items-center gap-2 ${
            msg.type === 'success'
              ? 'bg-green-50 border border-green-200 text-green-800'
              : 'bg-red-50 border border-red-200 text-red-800'
          }`}
        >
          {msg.type === 'success' ? <CheckCircle2 size={18} /> : <AlertTriangle size={18} />}
          <span>{msg.text}</span>
        </div>
      )}

      {/* Production Run Tab */}
      {activeTab === 'prod-run' && (
        <div className="grid grid-cols-1 md:grid-cols-12 gap-6">
          <div className="md:col-span-8 stitch-card p-5 space-y-4">
            <h2 className="text-base font-bold text-[#1F2617] border-b border-[#E3ECD7] pb-2 flex items-center justify-between">
              <span className="flex items-center gap-2">
                <Factory size={18} className="text-[#728156]" /> BOM Batch Production Run
              </span>
              <span className="text-xs bg-[#CFE1B9] text-[#353D23] px-2 py-0.5 rounded font-bold">
                Operator: {currentUser?.fullName}
              </span>
            </h2>

            <form onSubmit={handleProductionRunSubmit} className="space-y-4">
              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label className="block text-xs font-bold text-[#4B5633] uppercase mb-1">
                    Finished Product
                  </label>
                  <select
                    value={selectedFgId}
                    onChange={(e) => {
                      setSelectedFgId(e.target.value);
                      loadBom(e.target.value);
                    }}
                    className="w-full bg-[#F8FAF6] border border-[#CFE1B9] rounded-xl px-3 py-2 text-sm font-semibold"
                  >
                    {finishedItems.map((fg) => (
                      <option key={fg.id} value={fg.id}>
                        {fg.name} ({fg.skuCode})
                      </option>
                    ))}
                  </select>
                </div>

                <div>
                  <label className="block text-xs font-bold text-[#4B5633] uppercase mb-1">
                    Batches Planned
                  </label>
                  <input
                    type="number"
                    step="0.1"
                    min="0.1"
                    value={batchesPlanned}
                    onChange={(e) => setBatchesPlanned(e.target.value)}
                    className="w-full bg-[#F8FAF6] border border-[#CFE1B9] rounded-xl px-3 py-2 text-sm font-semibold"
                  />
                </div>
              </div>

              {/* BOM Raw Material Consumption Inputs with Realtime Variance */}
              <div className="space-y-2 pt-2 border-t border-[#E3ECD7]">
                <span className="text-xs font-bold text-[#1F2617] block uppercase tracking-wider">
                  Raw Material Consumptions & Variances
                </span>
                <div className="space-y-2">
                  {bom?.lines
                    .filter((l) => !l.packaging)
                    .map((line) => {
                      const planned = (line.standardQty * parseFloat(batchesPlanned || 1)).toFixed(3);
                      const actual = rawInputs[line.componentItemId] || '';
                      const actualVal = parseFloat(actual) || 0;
                      const plannedVal = parseFloat(planned) || 1;
                      const varPct = (((actualVal - plannedVal) / plannedVal) * 100).toFixed(2);
                      const isFlagged = Math.abs(parseFloat(varPct)) > 3.0;

                      return (
                        <div
                          key={line.id}
                          className="flex items-center justify-between p-2.5 bg-[#F8FAF6] rounded-xl border border-[#CFE1B9] text-xs gap-3"
                        >
                          <div className="flex-1">
                            <span className="font-bold text-[#1F2617] block">{line.componentName}</span>
                            <span className="text-[11px] text-[#5F6D44]">
                              Standard: {planned} {line.unit}
                            </span>
                          </div>

                          <div className="flex items-center gap-2">
                            <span className="text-[11px] text-[#5F6D44]">Actual:</span>
                            <input
                              type="number"
                              step="0.001"
                              value={actual}
                              onChange={(e) =>
                                setRawInputs({ ...rawInputs, [line.componentItemId]: e.target.value })
                              }
                              className="w-24 bg-white border border-[#B6C99B] rounded-lg px-2 py-1 text-right font-bold text-[#1F2617]"
                            />
                            <span className="text-xs text-[#5F6D44] w-10">{line.unit}</span>
                          </div>

                          <div className="w-24 text-right">
                            <span
                              className={`stitch-pill ${
                                isFlagged ? 'bg-red-100 text-[#C63D3D]' : 'bg-green-100 text-green-800'
                              }`}
                            >
                              {varPct > 0 ? `+${varPct}%` : `${varPct}%`}
                            </span>
                          </div>
                        </div>
                      );
                    })}
                </div>
              </div>

              {/* Outputs & Packaging */}
              <div className="pt-2 border-t border-[#E3ECD7] grid grid-cols-3 gap-3">
                <div>
                  <label className="block text-xs font-bold text-[#4B5633] mb-1">
                    Good Packets Produced
                  </label>
                  <input
                    type="number"
                    value={actualGoodPackets}
                    onChange={(e) => setActualGoodPackets(parseInt(e.target.value) || 0)}
                    className="w-full bg-[#F8FAF6] border border-[#CFE1B9] rounded-xl p-2 text-sm font-bold"
                  />
                  <span className="text-[10px] text-[#5F6D44]">Expected: {expectedOutput}</span>
                </div>

                <div>
                  <label className="block text-xs font-bold text-[#4B5633] mb-1">
                    Damaged Pouches (Wastage)
                  </label>
                  <input
                    type="number"
                    value={damagedPouches}
                    onChange={(e) => setDamagedPouches(parseInt(e.target.value) || 0)}
                    className="w-full bg-[#F8FAF6] border border-[#CFE1B9] rounded-xl p-2 text-sm font-bold"
                  />
                  <span className="text-[10px] text-amber-700">Absorbed in cost</span>
                </div>

                <div>
                  <label className="block text-xs font-bold text-[#4B5633] mb-1">
                    Cartons Used
                  </label>
                  <input
                    type="number"
                    value={cartonsUsed}
                    onChange={(e) => setCartonsUsed(parseInt(e.target.value) || 0)}
                    className="w-full bg-[#F8FAF6] border border-[#CFE1B9] rounded-xl p-2 text-sm font-bold"
                  />
                  <span className="text-[10px] text-[#5F6D44]">20 pkts/carton</span>
                </div>
              </div>

              {/* Conversion Costs */}
              <div className="pt-2 border-t border-[#E3ECD7] grid grid-cols-2 gap-3">
                <div>
                  <label className="block text-xs font-bold text-[#4B5633] mb-1">Labour Cost (₹)</label>
                  <input
                    type="number"
                    step="0.01"
                    value={labourCost}
                    onChange={(e) => setLabourCost(e.target.value)}
                    className="w-full bg-[#F8FAF6] border border-[#CFE1B9] rounded-xl p-2 text-sm font-bold"
                  />
                </div>
                <div>
                  <label className="block text-xs font-bold text-[#4B5633] mb-1">Gas / Power Overhead (₹)</label>
                  <input
                    type="number"
                    step="0.01"
                    value={overheadCost}
                    onChange={(e) => setOverheadCost(e.target.value)}
                    className="w-full bg-[#F8FAF6] border border-[#CFE1B9] rounded-xl p-2 text-sm font-bold"
                  />
                </div>
              </div>

              <button
                type="submit"
                disabled={loading}
                className="w-full py-3 bg-[#728156] hover:bg-[#5F6D44] text-white font-bold rounded-xl shadow transition"
              >
                Complete & Log Production Run
              </button>
            </form>
          </div>

          {/* Right Live Costing & Variance Feedback */}
          <div className="md:col-span-4 space-y-4">
            <div className="stitch-card p-4 space-y-3 bg-[#E7F5DC]/40">
              <h3 className="font-bold text-[#1F2617] text-sm uppercase">Live Variance Monitor</h3>
              <div className="p-3 bg-white rounded-xl border border-[#CFE1B9] space-y-2 text-xs">
                <div className="flex justify-between">
                  <span className="text-[#5F6D44]">Expected Yield:</span>
                  <span className="font-bold">{expectedOutput} pkts</span>
                </div>
                <div className="flex justify-between">
                  <span className="text-[#5F6D44]">Actual Output:</span>
                  <span className="font-bold">{actualGoodPackets} pkts</span>
                </div>
                <div className="flex justify-between items-center pt-2 border-t border-[#E3ECD7]">
                  <span className="font-bold">Yield Variance:</span>
                  <span
                    className={`stitch-pill ${
                      isYieldFlagged ? 'bg-red-100 text-[#C63D3D] font-bold' : 'bg-green-100 text-green-800'
                    }`}
                  >
                    {yieldVariancePct}% {isYieldFlagged && '⚠️ (>3%)'}
                  </span>
                </div>
              </div>

              {recentCompletedRun && (
                <div className="p-3 bg-white rounded-xl border border-[#B6C99B] space-y-2 text-xs shadow-sm">
                  <span className="text-[11px] font-bold text-[#728156] uppercase tracking-wider block">
                    Last Run Result
                  </span>
                  <div className="flex justify-between">
                    <span className="text-[#5F6D44]">Total Batch Cost:</span>
                    <span className="font-bold text-[#1F2617]">₹{recentCompletedRun.totalBatchCost}</span>
                  </div>
                  <div className="flex justify-between">
                    <span className="text-[#5F6D44]">Calculated Unit Cost:</span>
                    <span className="font-extrabold text-base text-[#1F2617]">
                      ₹{recentCompletedRun.calculatedUnitCost}
                    </span>
                  </div>
                  <div className="flex justify-between text-[11px] text-[#728156]">
                    <span>Selling Price: ₹50.00</span>
                    <span className="font-bold text-green-700">Margin: ~38.2%</span>
                  </div>
                </div>
              )}
            </div>
          </div>
        </div>
      )}

      {/* Raw Material Purchase Tab */}
      {activeTab === 'prod-purchase' && (
        <div className="stitch-card p-5 max-w-xl mx-auto space-y-4">
          <h2 className="text-base font-bold text-[#1F2617] border-b border-[#E3ECD7] pb-2 flex items-center gap-2">
            <ShoppingCart size={18} className="text-[#728156]" /> Log Raw Material / Packaging Purchase
          </h2>

          <form onSubmit={handlePurchaseSubmit} className="space-y-4 text-xs">
            <div>
              <label className="block font-bold text-[#4B5633] uppercase mb-1">Supplier Name</label>
              <input
                type="text"
                value={supplierName}
                onChange={(e) => setSupplierName(e.target.value)}
                className="w-full bg-[#F8FAF6] border border-[#CFE1B9] rounded-xl p-2 font-semibold text-sm"
                required
              />
            </div>

            <div className="grid grid-cols-2 gap-3">
              <div>
                <label className="block font-bold text-[#4B5633] uppercase mb-1">Invoice Number</label>
                <input
                  type="text"
                  value={invoiceNo}
                  onChange={(e) => setInvoiceNo(e.target.value)}
                  className="w-full bg-[#F8FAF6] border border-[#CFE1B9] rounded-xl p-2 font-semibold text-sm"
                  required
                />
              </div>
              <div>
                <label className="block font-bold text-[#4B5633] uppercase mb-1">Purchase Date</label>
                <input
                  type="date"
                  value={purchaseDate}
                  onChange={(e) => setPurchaseDate(e.target.value)}
                  className="w-full bg-[#F8FAF6] border border-[#CFE1B9] rounded-xl p-2 font-semibold text-sm"
                  required
                />
              </div>
            </div>

            <div>
              <label className="block font-bold text-[#4B5633] uppercase mb-1">Item</label>
              <select
                value={purchaseItemId}
                onChange={(e) => setPurchaseItemId(e.target.value)}
                className="w-full bg-[#F8FAF6] border border-[#CFE1B9] rounded-xl p-2 font-semibold text-sm"
              >
                {rawMaterials.map((r) => (
                  <option key={r.id} value={r.id}>
                    {r.name} ({r.skuCode}) — {r.unit}
                  </option>
                ))}
              </select>
            </div>

            <div className="grid grid-cols-2 gap-3">
              <div>
                <label className="block font-bold text-[#4B5633] uppercase mb-1">Quantity</label>
                <input
                  type="number"
                  step="0.001"
                  value={purchaseQty}
                  onChange={(e) => setPurchaseQty(e.target.value)}
                  className="w-full bg-[#F8FAF6] border border-[#CFE1B9] rounded-xl p-2 font-semibold text-sm"
                  required
                />
              </div>
              <div>
                <label className="block font-bold text-[#4B5633] uppercase mb-1">Unit Rate (₹)</label>
                <input
                  type="number"
                  step="0.01"
                  value={purchaseRate}
                  onChange={(e) => setPurchaseRate(e.target.value)}
                  className="w-full bg-[#F8FAF6] border border-[#CFE1B9] rounded-xl p-2 font-semibold text-sm"
                  required
                />
              </div>
            </div>

            <div className="p-3 bg-[#E7F5DC] rounded-xl border border-[#CFE1B9] flex justify-between items-center text-sm font-bold">
              <span>Total Spend:</span>
              <span className="text-[#1F2617]">
                ₹{(parseFloat(purchaseQty || 0) * parseFloat(purchaseRate || 0)).toFixed(2)}
              </span>
            </div>

            <button
              type="submit"
              disabled={loading}
              className="w-full py-3 bg-[#728156] hover:bg-[#5F6D44] text-white font-bold rounded-xl shadow transition"
            >
              Save Purchase & Add Inbound Ledger Layer
            </button>
          </form>
        </div>
      )}

      {/* Production History Tab */}
      {activeTab === 'prod-history' && (
        <div className="stitch-card p-5 space-y-4">
          <h2 className="text-base font-bold text-[#1F2617] border-b border-[#E3ECD7] pb-2">
            Production Runs History
          </h2>
          <div className="overflow-x-auto text-xs">
            <table className="w-full text-left">
              <thead>
                <tr className="bg-[#CFE1B9]/50 text-[#353D23] font-bold border-b border-[#CFE1B9]">
                  <th className="py-2.5 px-3">Run Code</th>
                  <th className="py-2.5 px-3">Date</th>
                  <th className="py-2.5 px-3">Product</th>
                  <th className="py-2.5 px-3">Output Pkts</th>
                  <th className="py-2.5 px-3">Yield Var %</th>
                  <th className="py-2.5 px-3">Unit Cost</th>
                  <th className="py-2.5 px-3">Total Batch</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-[#E3ECD7]">
                {runs.map((r) => (
                  <tr key={r.id} className="hover:bg-[#F8FAF6]">
                    <td className="py-2.5 px-3 font-bold">{r.runCode}</td>
                    <td className="py-2.5 px-3 text-[#5F6D44]">{r.runDate}</td>
                    <td className="py-2.5 px-3">{r.finishedItemName}</td>
                    <td className="py-2.5 px-3 font-semibold">{r.actualGoodPackets}</td>
                    <td className="py-2.5 px-3">
                      <span
                        className={`stitch-pill ${
                          r.yieldFlagged ? 'bg-red-100 text-[#C63D3D]' : 'bg-green-100 text-green-800'
                        }`}
                      >
                        {r.yieldVariancePct}%
                      </span>
                    </td>
                    <td className="py-2.5 px-3 font-bold text-[#1F2617]">₹{r.calculatedUnitCost}</td>
                    <td className="py-2.5 px-3 font-bold">₹{r.totalBatchCost}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}
    </div>
  );
}
