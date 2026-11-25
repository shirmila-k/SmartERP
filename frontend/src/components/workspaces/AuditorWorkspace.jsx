import React, { useState, useEffect } from 'react';
import { api } from '../../services/api';
import { FileSpreadsheet, Download, RefreshCw, Layers, Calendar, Filter } from 'lucide-react';

export default function AuditorWorkspace() {
  const [activeTab, setActiveTab] = useState('sales');

  const today = new Date().toISOString().split('T')[0];
  const firstOfMonth = new Date(new Date().getFullYear(), new Date().getMonth(), 1)
    .toISOString()
    .split('T')[0];

  const [startDate, setStartDate] = useState(firstOfMonth);
  const [endDate, setEndDate] = useState(today);

  const [salesData, setSalesData] = useState(null);
  const [gstData, setGstData] = useState(null);
  const [purchaseData, setPurchaseData] = useState(null);
  const [stockSummaryData, setStockSummaryData] = useState(null);
  const [stockLedgerData, setStockLedgerData] = useState(null);
  const [productionData, setProductionData] = useState(null);
  const [receivablesData, setReceivablesData] = useState(null);

  const [loading, setLoading] = useState(false);

  useEffect(() => {
    loadActiveReport();
  }, [activeTab, startDate, endDate]);

  const loadActiveReport = async () => {
    try {
      setLoading(true);
      switch (activeTab) {
        case 'sales':
          setSalesData(await api.getSalesReport(startDate, endDate));
          break;
        case 'gst-summary':
          setGstData(await api.getGstSummaryReport(startDate, endDate));
          break;
        case 'purchases':
          setPurchaseData(await api.getPurchaseReport(startDate, endDate));
          break;
        case 'stock-summary':
          setStockSummaryData(await api.getStockSummaryReport(startDate, endDate));
          break;
        case 'stock-ledger':
          setStockLedgerData(await api.getAuditStockLedger(null, startDate, endDate));
          break;
        case 'production':
          setProductionData(await api.getProductionReport(startDate, endDate));
          break;
        case 'receivables':
          setReceivablesData(await api.getReceivablesReport(startDate, endDate));
          break;
        default:
          break;
      }
    } catch (e) {
      console.error(e);
    } finally {
      setLoading(false);
    }
  };

  const tabs = [
    { id: 'sales', label: 'Sales Register' },
    { id: 'gst-summary', label: 'GSTR-1 / 3B Outward' },
    { id: 'purchases', label: 'Purchase Register' },
    { id: 'stock-summary', label: 'Stock Summary' },
    { id: 'stock-ledger', label: 'Full Audit Stock Ledger' },
    { id: 'production', label: 'Production Variance & Cost' },
    { id: 'receivables', label: 'Receivables Ageing' },
  ];

  return (
    <div className="space-y-6">
      {/* Top Controls Bar */}
      <div className="stitch-card p-4 flex flex-wrap items-center justify-between gap-4 bg-white">
        <div>
          <h2 className="text-base font-bold text-[#1F2617] flex items-center gap-2">
            <FileSpreadsheet size={18} className="text-[#728156]" />
            Auditor Monthly Registers & GST Outward Summary
          </h2>
          <p className="text-xs text-[#5F6D44]">Strictly read-only auditor view • Exact BigDecimal ledger math</p>
        </div>

        {/* Date Filter & Export Controls */}
        <div className="flex flex-wrap items-center gap-2 text-xs">
          <div className="flex items-center gap-1 bg-[#F8FAF6] border border-[#CFE1B9] px-2.5 py-1 rounded-xl">
            <Calendar size={13} className="text-[#728156]" />
            <input
              type="date"
              value={startDate}
              onChange={(e) => setStartDate(e.target.value)}
              className="bg-transparent font-semibold focus:outline-none"
            />
            <span className="text-[#5F6D44] font-bold">to</span>
            <input
              type="date"
              value={endDate}
              onChange={(e) => setEndDate(e.target.value)}
              className="bg-transparent font-semibold focus:outline-none"
            />
          </div>

          {/* Export Buttons */}
          <a
            href={api.getExportUrl(activeTab === 'stock-ledger' ? 'stock-summary' : activeTab, 'xlsx', startDate, endDate)}
            download
            className="px-3 py-1.5 bg-[#728156] hover:bg-[#5F6D44] text-white rounded-lg font-bold flex items-center gap-1 shadow-sm transition"
          >
            <Download size={13} /> Export Excel (.xlsx)
          </a>

          <a
            href={api.getExportUrl(activeTab === 'stock-ledger' ? 'stock-summary' : activeTab, 'csv', startDate, endDate)}
            download
            className="px-3 py-1.5 bg-white border border-[#B6C99B] hover:bg-[#E7F5DC] text-[#1F2617] rounded-lg font-bold flex items-center gap-1 transition"
          >
            <Download size={13} /> Export CSV (.csv)
          </a>

          <button
            onClick={loadActiveReport}
            className="p-1.5 rounded-lg border border-[#CFE1B9] hover:bg-[#E7F5DC] text-[#4B5633]"
            title="Refresh Report"
          >
            <RefreshCw size={14} className={loading ? 'animate-spin' : ''} />
          </button>
        </div>
      </div>

      {/* Tabs */}
      <div className="flex items-center gap-1 border-b border-[#CFE1B9] pb-1 overflow-x-auto text-xs">
        {tabs.map((t) => (
          <button
            key={t.id}
            onClick={() => setActiveTab(t.id)}
            className={`px-3 py-1.5 rounded-t-lg font-bold transition whitespace-nowrap ${
              activeTab === t.id
                ? 'bg-[#728156] text-white shadow-sm'
                : 'text-[#5F6D44] hover:bg-[#CFE1B9]/50'
            }`}
          >
            {t.label}
          </button>
        ))}
      </div>

      {/* Report Content Panels */}
      <div className="stitch-card p-5">
        {/* 1. Sales Register */}
        {activeTab === 'sales' && salesData && (
          <div className="space-y-4 text-xs">
            <div className="grid grid-cols-4 gap-3 p-3 bg-[#E7F5DC]/40 rounded-xl border border-[#CFE1B9]">
              <div>
                <span className="text-[#5F6D44] block">Total Taxable:</span>
                <span className="font-bold text-sm text-[#1F2617]">₹{salesData.totalTaxableAmount}</span>
              </div>
              <div>
                <span className="text-[#5F6D44] block">CGST + SGST:</span>
                <span className="font-bold text-sm text-[#1F2617]">
                  ₹{(parseFloat(salesData.totalCgstAmount) + parseFloat(salesData.totalSgstAmount)).toFixed(2)}
                </span>
              </div>
              <div>
                <span className="text-[#5F6D44] block">IGST:</span>
                <span className="font-bold text-sm text-[#1F2617]">₹{salesData.totalIgstAmount}</span>
              </div>
              <div>
                <span className="text-[#5F6D44] block">Grand Total:</span>
                <span className="font-extrabold text-sm text-[#728156]">₹{salesData.totalGrandAmount}</span>
              </div>
            </div>

            <div className="overflow-x-auto">
              <table className="w-full text-left">
                <thead>
                  <tr className="bg-[#CFE1B9]/50 font-bold border-b border-[#CFE1B9]">
                    <th className="p-2">Invoice No</th>
                    <th className="p-2">Date</th>
                    <th className="p-2">Customer & State</th>
                    <th className="p-2">GSTIN</th>
                    <th className="p-2 text-right">Taxable</th>
                    <th className="p-2 text-right">CGST</th>
                    <th className="p-2 text-right">SGST</th>
                    <th className="p-2 text-right">IGST</th>
                    <th className="p-2 text-right">Grand Total</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-[#E3ECD7]">
                  {salesData.entries.map((e) => (
                    <tr key={e.invoiceId} className="hover:bg-[#F8FAF6]">
                      <td className="p-2 font-bold">{e.invoiceNumber}</td>
                      <td className="p-2 text-[#5F6D44]">{e.invoiceDate}</td>
                      <td className="p-2">
                        <span className="font-semibold block">{e.customerName}</span>
                        <span className="text-[10px] text-[#5F6D44]">{e.stateName}</span>
                      </td>
                      <td className="p-2 font-mono text-[11px]">{e.customerGstin || 'URP'}</td>
                      <td className="p-2 text-right">₹{e.taxableAmount}</td>
                      <td className="p-2 text-right">₹{e.cgstAmount}</td>
                      <td className="p-2 text-right">₹{e.sgstAmount}</td>
                      <td className="p-2 text-right">₹{e.igstAmount}</td>
                      <td className="p-2 text-right font-bold text-[#1F2617]">₹{e.grandTotal}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>
        )}

        {/* 2. GSTR-1 / 3B Outward Summary */}
        {activeTab === 'gst-summary' && gstData && (
          <div className="space-y-6 text-xs">
            {/* B2B vs B2C */}
            <div>
              <h3 className="font-bold text-sm text-[#1F2617] mb-2 uppercase">1. B2B vs B2C Classification</h3>
              <table className="w-full text-left border border-[#CFE1B9] rounded-xl overflow-hidden">
                <thead>
                  <tr className="bg-[#CFE1B9]/50 font-bold">
                    <th className="p-2">Category</th>
                    <th className="p-2">Invoices</th>
                    <th className="p-2 text-right">Taxable</th>
                    <th className="p-2 text-right">CGST</th>
                    <th className="p-2 text-right">SGST</th>
                    <th className="p-2 text-right">IGST</th>
                    <th className="p-2 text-right">Total Tax</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-[#E3ECD7]">
                  {gstData.b2bVsB2c.map((b, idx) => (
                    <tr key={idx}>
                      <td className="p-2 font-bold">{b.category}</td>
                      <td className="p-2">{b.invoiceCount}</td>
                      <td className="p-2 text-right font-semibold">₹{b.taxableAmount}</td>
                      <td className="p-2 text-right">₹{b.cgstAmount}</td>
                      <td className="p-2 text-right">₹{b.sgstAmount}</td>
                      <td className="p-2 text-right">₹{b.igstAmount}</td>
                      <td className="p-2 text-right font-bold text-[#728156]">₹{b.totalTax}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>

            {/* Place of Supply */}
            <div>
              <h3 className="font-bold text-sm text-[#1F2617] mb-2 uppercase">2. State-wise Place of Supply</h3>
              <table className="w-full text-left border border-[#CFE1B9] rounded-xl overflow-hidden">
                <thead>
                  <tr className="bg-[#CFE1B9]/50 font-bold">
                    <th className="p-2">State</th>
                    <th className="p-2">Supply Type</th>
                    <th className="p-2 text-right">Taxable</th>
                    <th className="p-2 text-right">CGST</th>
                    <th className="p-2 text-right">SGST</th>
                    <th className="p-2 text-right">IGST</th>
                    <th className="p-2 text-right">Total Tax</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-[#E3ECD7]">
                  {gstData.placeOfSupplySummary.map((pos, idx) => (
                    <tr key={idx}>
                      <td className="p-2 font-bold">{pos.stateName}</td>
                      <td className="p-2">{pos.supplyType}</td>
                      <td className="p-2 text-right">₹{pos.taxableAmount}</td>
                      <td className="p-2 text-right">₹{pos.cgstAmount}</td>
                      <td className="p-2 text-right">₹{pos.sgstAmount}</td>
                      <td className="p-2 text-right">₹{pos.igstAmount}</td>
                      <td className="p-2 text-right font-bold text-[#728156]">₹{pos.totalTax}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>

            {/* HSN Summary */}
            <div>
              <h3 className="font-bold text-sm text-[#1F2617] mb-2 uppercase">3. HSN-wise Outward Summary</h3>
              <table className="w-full text-left border border-[#CFE1B9] rounded-xl overflow-hidden">
                <thead>
                  <tr className="bg-[#CFE1B9]/50 font-bold">
                    <th className="p-2">HSN Code</th>
                    <th className="p-2">Description</th>
                    <th className="p-2 text-right">Total Qty</th>
                    <th className="p-2 text-right">Taxable Value</th>
                    <th className="p-2 text-right">CGST</th>
                    <th className="p-2 text-right">SGST</th>
                    <th className="p-2 text-right">IGST</th>
                    <th className="p-2 text-right">Total Tax</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-[#E3ECD7]">
                  {gstData.hsnSummary.map((h, idx) => (
                    <tr key={idx}>
                      <td className="p-2 font-mono font-bold">{h.hsnCode}</td>
                      <td className="p-2">{h.description}</td>
                      <td className="p-2 text-right">{h.totalQuantity} pkts</td>
                      <td className="p-2 text-right font-semibold">₹{h.taxableValue}</td>
                      <td className="p-2 text-right">₹{h.cgstAmount}</td>
                      <td className="p-2 text-right">₹{h.sgstAmount}</td>
                      <td className="p-2 text-right">₹{h.igstAmount}</td>
                      <td className="p-2 text-right font-bold text-[#728156]">₹{h.totalTax}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>
        )}

        {/* 3. Purchase Register */}
        {activeTab === 'purchases' && purchaseData && (
          <div className="space-y-4 text-xs">
            <div className="p-3 bg-[#E7F5DC]/40 rounded-xl border border-[#CFE1B9] flex justify-between items-center">
              <span className="font-bold text-sm">Total Material Spend:</span>
              <span className="text-xl font-extrabold text-[#1F2617]">₹{purchaseData.grandTotalSpend}</span>
            </div>
            <div className="overflow-x-auto">
              <table className="w-full text-left">
                <thead>
                  <tr className="bg-[#CFE1B9]/50 font-bold border-b border-[#CFE1B9]">
                    <th className="p-2">Date</th>
                    <th className="p-2">Supplier</th>
                    <th className="p-2">Invoice No</th>
                    <th className="p-2">Item Name (SKU)</th>
                    <th className="p-2 text-right">Qty</th>
                    <th className="p-2 text-right">Rate</th>
                    <th className="p-2 text-right">Total Spend</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-[#E3ECD7]">
                  {purchaseData.entries.map((p) => (
                    <tr key={p.purchaseId} className="hover:bg-[#F8FAF6]">
                      <td className="p-2 text-[#5F6D44]">{p.purchaseDate}</td>
                      <td className="p-2 font-bold">{p.supplierName}</td>
                      <td className="p-2 font-mono">{p.invoiceNo}</td>
                      <td className="p-2">{p.itemName} ({p.skuCode})</td>
                      <td className="p-2 text-right">{p.qty} {p.unit}</td>
                      <td className="p-2 text-right">₹{p.unitRate}</td>
                      <td className="p-2 text-right font-bold text-[#1F2617]">₹{p.totalAmount}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>
        )}

        {/* 4. Stock Summary */}
        {activeTab === 'stock-summary' && stockSummaryData && (
          <div className="overflow-x-auto text-xs">
            <table className="w-full text-left">
              <thead>
                <tr className="bg-[#CFE1B9]/50 font-bold border-b border-[#CFE1B9]">
                  <th className="p-2">SKU Code</th>
                  <th className="p-2">Item Name</th>
                  <th className="p-2">Type</th>
                  <th className="p-2 text-right">Opening Balance</th>
                  <th className="p-2 text-right">Receipts (+)</th>
                  <th className="p-2 text-right">Issues & Sales (-)</th>
                  <th className="p-2 text-right">Closing Balance</th>
                  <th className="p-2 text-right">Std Rate</th>
                  <th className="p-2 text-right">Closing Value</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-[#E3ECD7]">
                {stockSummaryData.items.map((i) => (
                  <tr key={i.itemId} className="hover:bg-[#F8FAF6]">
                    <td className="p-2 font-bold text-[#1F2617]">{i.skuCode}</td>
                    <td className="p-2">{i.itemName}</td>
                    <td className="p-2 text-[10px] text-[#5F6D44]">{i.itemType}</td>
                    <td className="p-2 text-right">{i.openingBalance} {i.unit}</td>
                    <td className="p-2 text-right text-green-700 font-medium">+{i.receipts}</td>
                    <td className="p-2 text-right text-red-700 font-medium">{i.issuesAndSales}</td>
                    <td className="p-2 text-right font-black text-[#1F2617]">{i.closingBalance} {i.unit}</td>
                    <td className="p-2 text-right">₹{i.standardRate}</td>
                    <td className="p-2 text-right font-bold text-[#728156]">₹{i.closingStockValue}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}

        {/* 5. Full Audit Stock Ledger */}
        {activeTab === 'stock-ledger' && stockLedgerData && (
          <div className="overflow-x-auto text-xs">
            <table className="w-full text-left">
              <thead>
                <tr className="bg-[#CFE1B9]/50 font-bold border-b border-[#CFE1B9]">
                  <th className="p-2">#ID</th>
                  <th className="p-2">Timestamp</th>
                  <th className="p-2">Item (SKU)</th>
                  <th className="p-2">Movement Type</th>
                  <th className="p-2 text-right">Qty Change</th>
                  <th className="p-2 text-right">Unit Rate</th>
                  <th className="p-2">Source Type</th>
                  <th className="p-2">Source ID</th>
                  <th className="p-2">Audit Remarks</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-[#E3ECD7]">
                {stockLedgerData.rows.map((l) => (
                  <tr key={l.id} className="hover:bg-[#F8FAF6]">
                    <td className="p-2 font-mono text-[10px]">{l.id}</td>
                    <td className="p-2 text-[#5F6D44]">{new Date(l.createdAt).toLocaleString()}</td>
                    <td className="p-2 font-semibold">{l.itemName} ({l.itemSku})</td>
                    <td className="p-2">
                      <span className="stitch-pill bg-[#CFE1B9] text-[#353D23] font-bold">
                        {l.movementType}
                      </span>
                    </td>
                    <td className={`p-2 text-right font-bold ${l.qtyChange > 0 ? 'text-green-700' : 'text-red-700'}`}>
                      {l.qtyChange > 0 ? `+${l.qtyChange}` : l.qtyChange}
                    </td>
                    <td className="p-2 text-right">₹{l.unitRate}</td>
                    <td className="p-2 font-mono text-[11px]">{l.sourceType}</td>
                    <td className="p-2 font-mono text-[11px]">{l.sourceId}</td>
                    <td className="p-2 text-[#5F6D44]">{l.remarks}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}

        {/* 6. Production Variance & Costing */}
        {activeTab === 'production' && productionData && (
          <div className="space-y-4 text-xs">
            <div className="grid grid-cols-4 gap-3 p-3 bg-[#E7F5DC]/40 rounded-xl border border-[#CFE1B9]">
              <div>
                <span className="text-[#5F6D44] block">Total Batches:</span>
                <span className="font-bold text-sm text-[#1F2617]">{productionData.totalRuns}</span>
              </div>
              <div>
                <span className="text-[#5F6D44] block">Good Packets Produced:</span>
                <span className="font-bold text-sm text-[#1F2617]">{productionData.totalOutputPackets}</span>
              </div>
              <div>
                <span className="text-[#5F6D44] block">Labour Spend:</span>
                <span className="font-bold text-sm text-[#1F2617]">₹{productionData.totalLabourCost}</span>
              </div>
              <div>
                <span className="text-[#5F6D44] block">Overhead Spend:</span>
                <span className="font-bold text-sm text-[#1F2617]">₹{productionData.totalOverheadCost}</span>
              </div>
            </div>

            <div className="space-y-3">
              {productionData.runs.map((r) => (
                <div key={r.runId} className="p-3 bg-white border border-[#CFE1B9] rounded-xl space-y-2">
                  <div className="flex justify-between items-center border-b pb-1.5">
                    <div>
                      <span className="font-bold text-sm text-[#1F2617]">{r.runCode} — {r.productName}</span>
                      <span className="text-[11px] text-[#5F6D44] block">Date: {r.runDate}</span>
                    </div>
                    <div className="flex items-center gap-2">
                      <span className="stitch-pill bg-gray-100 font-semibold">{r.actualGoodPackets} pkts output</span>
                      <span
                        className={`stitch-pill ${
                          r.yieldFlagged ? 'bg-red-100 text-[#C63D3D] font-bold' : 'bg-green-100 text-green-800'
                        }`}
                      >
                        Yield: {r.yieldVariancePct}% {r.yieldFlagged && '⚠️'}
                      </span>
                      <span className="font-bold text-sm text-[#728156]">
                        Cost: ₹{r.calculatedUnitCost}/pkt
                      </span>
                    </div>
                  </div>

                  <div className="grid grid-cols-3 gap-2 text-[11px]">
                    {r.rawMaterialConsumptions.map((c, idx) => (
                      <div key={idx} className="p-1.5 bg-[#F8FAF6] rounded border border-[#E3ECD7]">
                        <span className="font-bold block">{c.componentName}</span>
                        <span className="text-[#5F6D44]">Actual: {c.actualQty} (Std: {c.plannedQty})</span>
                        <span className={`block font-bold ${c.varianceFlagged ? 'text-[#C63D3D]' : 'text-green-700'}`}>
                          Var: {c.variancePct}% {c.varianceFlagged && '⚠️'}
                        </span>
                      </div>
                    ))}
                  </div>
                </div>
              ))}
            </div>
          </div>
        )}

        {/* 7. Receivables Ageing */}
        {activeTab === 'receivables' && receivablesData && (
          <div className="space-y-4 text-xs">
            <div className="grid grid-cols-3 gap-3 p-3 bg-[#E7F5DC]/40 rounded-xl border border-[#CFE1B9]">
              <div>
                <span className="text-[#5F6D44] block">Total Invoiced (All Time):</span>
                <span className="font-bold text-sm text-[#1F2617]">₹{receivablesData.totalInvoicedAll}</span>
              </div>
              <div>
                <span className="text-[#5F6D44] block">Collected in Period:</span>
                <span className="font-bold text-sm text-green-700">₹{receivablesData.totalCollectedPeriod}</span>
              </div>
              <div>
                <span className="text-[#5F6D44] block">Current Outstanding Balance:</span>
                <span className="font-extrabold text-sm text-[#C63D3D]">₹{receivablesData.totalOutstandingAll}</span>
              </div>
            </div>

            <div className="overflow-x-auto">
              <table className="w-full text-left">
                <thead>
                  <tr className="bg-[#CFE1B9]/50 font-bold border-b border-[#CFE1B9]">
                    <th className="p-2">Customer Name</th>
                    <th className="p-2">GSTIN & State</th>
                    <th className="p-2 text-right">Total Invoiced</th>
                    <th className="p-2 text-right">Total Paid</th>
                    <th className="p-2 text-right">Balance Due</th>
                    <th className="p-2 text-right text-green-800">0–30 Days</th>
                    <th className="p-2 text-right text-amber-800">31–60 Days</th>
                    <th className="p-2 text-right text-red-800">60+ Days</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-[#E3ECD7]">
                  {receivablesData.customerAgeing.map((c) => (
                    <tr key={c.customerId} className="hover:bg-[#F8FAF6]">
                      <td className="p-2 font-bold text-[#1F2617]">{c.customerName}</td>
                      <td className="p-2 text-[10px] text-[#5F6D44]">{c.gstin || 'URP'} • {c.stateName}</td>
                      <td className="p-2 text-right">₹{c.totalInvoiced}</td>
                      <td className="p-2 text-right text-green-700">₹{c.totalPaid}</td>
                      <td className="p-2 text-right font-bold text-[#C63D3D]">₹{c.balanceDue}</td>
                      <td className="p-2 text-right">₹{c.bucket0to30}</td>
                      <td className="p-2 text-right">₹{c.bucket31to60}</td>
                      <td className="p-2 text-right font-semibold text-red-700">₹{c.bucket60Plus}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>
        )}
      </div>
    </div>
  );
}
