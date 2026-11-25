import React, { useState, useEffect } from 'react';
import { useAuth } from '../../context/AuthContext';
import { api } from '../../services/api';
import { 
  FileCheck, 
  Truck, 
  XCircle, 
  Download, 
  DollarSign, 
  AlertCircle, 
  CheckCircle2, 
  Edit3,
  RefreshCw,
  Layers
} from 'lucide-react';

export default function AdminWorkspace({ activeTab }) {
  const { currentUser } = useAuth();

  const [orders, setOrders] = useState([]);
  const [invoices, setInvoices] = useState([]);
  const [balances, setBalances] = useState([]);
  const [loading, setLoading] = useState(false);
  const [msg, setMsg] = useState({ type: '', text: '' });

  // Payment Modal State
  const [paymentModalOpen, setPaymentModalOpen] = useState(false);
  const [paymentInvoice, setPaymentInvoice] = useState(null);
  const [paymentAmount, setPaymentAmount] = useState('');
  const [paymentMode, setPaymentMode] = useState('NEFT');

  // Edit/Approve State
  const [editingOrder, setEditingOrder] = useState(null);
  const [adjustedQty, setAdjustedQty] = useState('');
  const [adjustedPrice, setAdjustedPrice] = useState('');

  useEffect(() => {
    loadData();
  }, []);

  const loadData = async () => {
    try {
      setLoading(true);
      const [orderList, invList, balList] = await Promise.all([
        api.getOrders(),
        api.getInvoices(),
        api.getInventoryBalances(),
      ]);
      setOrders(orderList);
      setInvoices(invList);
      setBalances(balList);
    } catch (e) {
      console.error(e);
    } finally {
      setLoading(false);
    }
  };

  const handleApproveInvoice = async (orderId, adjustments = null) => {
    setMsg({ type: '', text: '' });
    try {
      setLoading(true);
      const res = await api.approveAndInvoice(orderId, adjustments);
      setMsg({
        type: 'success',
        text: `Invoice ${res.invoiceNumber} generated successfully! Grand Total: ₹${res.grandTotal}`,
      });
      setEditingOrder(null);
      loadData();
    } catch (err) {
      setMsg({ type: 'error', text: err.message || 'Approval Failed' });
    } finally {
      setLoading(false);
    }
  };

  const handleDispatch = async (invoiceId) => {
    setMsg({ type: '', text: '' });
    try {
      setLoading(true);
      const res = await api.dispatchInvoice(invoiceId);
      setMsg({
        type: 'success',
        text: `Invoice ${res.invoiceNumber} dispatched! Stock deducted from stock_ledger.`,
      });
      loadData();
    } catch (err) {
      setMsg({ type: 'error', text: err.message || 'Dispatch Failed' });
    } finally {
      setLoading(false);
    }
  };

  const handleCancelInvoice = async (invoiceId) => {
    if (!window.confirm('Cancel this invoice via Credit Note? Reversing ledger adjustments will be applied if dispatched.')) {
      return;
    }
    setMsg({ type: '', text: '' });
    try {
      setLoading(true);
      const res = await api.cancelInvoice(invoiceId, currentUser.id, 'Admin cancellation via credit note');
      setMsg({
        type: 'success',
        text: `Invoice ${res.invoiceNumber} cancelled via Credit Note. Stock restored.`,
      });
      loadData();
    } catch (err) {
      setMsg({ type: 'error', text: err.message || 'Cancellation Failed' });
    } finally {
      setLoading(false);
    }
  };

  const handleRecordPayment = async (e) => {
    e.preventDefault();
    if (!paymentInvoice) return;
    setMsg({ type: '', text: '' });
    try {
      setLoading(true);
      const res = await api.recordPayment({
        invoiceId: paymentInvoice.id,
        amountPaid: parseFloat(paymentAmount),
        paymentMode: paymentMode,
        paymentDate: new Date().toISOString().split('T')[0],
        recordedBy: currentUser.id,
      });
      setMsg({
        type: 'success',
        text: `Recorded payment of ₹${res.amountPaid} via ${res.paymentMode}. Remaining Due: ₹${res.invoiceBalanceDue}`,
      });
      setPaymentModalOpen(false);
      loadData();
    } catch (err) {
      setMsg({ type: 'error', text: err.message || 'Payment Failed' });
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="space-y-6">
      {msg.text && (
        <div
          className={`p-3.5 rounded-xl text-sm flex items-center gap-2 ${
            msg.type === 'success'
              ? 'bg-green-50 border border-green-200 text-green-800'
              : 'bg-red-50 border border-red-200 text-red-800'
          }`}
        >
          {msg.type === 'success' ? <CheckCircle2 size={18} /> : <AlertCircle size={18} />}
          <span>{msg.text}</span>
        </div>
      )}

      {/* Orders & Dispatch Tab */}
      {activeTab === 'admin-orders' && (
        <div className="stitch-card p-5 space-y-4">
          <div className="flex items-center justify-between border-b border-[#E3ECD7] pb-3">
            <div>
              <h2 className="text-base font-bold text-[#1F2617]">Incoming Orders & Stock Verification</h2>
              <p className="text-xs text-[#5F6D44]">Verify live finished-goods availability, review GST, and approve into invoices</p>
            </div>
            <button
              onClick={loadData}
              className="p-1.5 rounded-lg border border-[#CFE1B9] hover:bg-[#E7F5DC] text-[#4B5633]"
              title="Refresh"
            >
              <RefreshCw size={14} />
            </button>
          </div>

          <div className="overflow-x-auto text-xs">
            <table className="w-full text-left">
              <thead>
                <tr className="bg-[#CFE1B9]/50 text-[#353D23] font-bold border-b border-[#CFE1B9]">
                  <th className="py-2.5 px-3">Order No</th>
                  <th className="py-2.5 px-3">Customer & State</th>
                  <th className="py-2.5 px-3">Product & Qty</th>
                  <th className="py-2.5 px-3">Live Stock Badge</th>
                  <th className="py-2.5 px-3">GST Split</th>
                  <th className="py-2.5 px-3">Total Amount</th>
                  <th className="py-2.5 px-3">Status</th>
                  <th className="py-2.5 px-3 text-right">Actions</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-[#E3ECD7]">
                {orders.map((o) => {
                  const line = o.lines?.[0];
                  const isTN = o.customerState?.toLowerCase().includes('tamil nadu');

                  return (
                    <tr key={o.id} className="hover:bg-[#F8FAF6]">
                      <td className="py-3 px-3 font-bold text-[#1F2617]">{o.orderNumber}</td>
                      <td className="py-3 px-3">
                        <span className="font-semibold text-[#1F2617] block">{o.customerName}</span>
                        <span className="text-[10px] text-[#5F6D44]">{o.customerState} ({o.customerGstin || 'URP'})</span>
                      </td>
                      <td className="py-3 px-3">
                        <span className="font-medium text-[#1F2617] block">{line?.itemName}</span>
                        <span className="text-[10px] text-[#5F6D44]">{line?.qty} pkts @ ₹{line?.unitPrice}</span>
                      </td>
                      <td className="py-3 px-3">
                        {o.stockShortageFlagged ? (
                          <span className="stitch-pill bg-red-100 text-[#C63D3D] font-bold inline-flex items-center gap-1">
                            <AlertCircle size={12} /> Shortage: -{o.shortageQty} Pkts
                          </span>
                        ) : (
                          <span className="stitch-pill bg-green-100 text-green-800 font-bold inline-flex items-center gap-1">
                            <CheckCircle2 size={12} /> In Stock
                          </span>
                        )}
                      </td>
                      <td className="py-3 px-3">
                        {isTN ? (
                          <span className="text-[11px] text-[#5F6D44]">
                            CGST 2.5% + SGST 2.5%
                          </span>
                        ) : (
                          <span className="text-[11px] text-blue-700 font-medium">
                            IGST 5.0%
                          </span>
                        )}
                      </td>
                      <td className="py-3 px-3 font-bold text-[#1F2617]">₹{o.totalEstimatedAmount}</td>
                      <td className="py-3 px-3">
                        <span className="stitch-pill bg-[#CFE1B9] text-[#353D23] font-bold">
                          {o.status}
                        </span>
                      </td>
                      <td className="py-3 px-3 text-right space-x-1.5 whitespace-nowrap">
                        {o.status === 'SUBMITTED' && (
                          <>
                            <button
                              onClick={() => {
                                setEditingOrder(o);
                                setAdjustedQty(line?.qty || '');
                                setAdjustedPrice(line?.unitPrice || '');
                              }}
                              className="px-2 py-1 bg-white border border-[#B6C99B] hover:bg-[#E7F5DC] rounded text-xs font-semibold"
                            >
                              Edit
                            </button>
                            <button
                              onClick={() => handleApproveInvoice(o.id)}
                              className="px-2.5 py-1 bg-[#728156] hover:bg-[#5F6D44] text-white rounded text-xs font-bold shadow-sm"
                            >
                              Approve & Invoice
                            </button>
                          </>
                        )}
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        </div>
      )}

      {/* Invoices Tab */}
      {activeTab === 'admin-invoices' && (
        <div className="stitch-card p-5 space-y-4">
          <h2 className="text-base font-bold text-[#1F2617] border-b border-[#E3ECD7] pb-2">
            Generated GST Tax Invoices & Dispatch Controls
          </h2>
          <div className="overflow-x-auto text-xs">
            <table className="w-full text-left">
              <thead>
                <tr className="bg-[#CFE1B9]/50 text-[#353D23] font-bold border-b border-[#CFE1B9]">
                  <th className="py-2.5 px-3">Invoice No</th>
                  <th className="py-2.5 px-3">Date</th>
                  <th className="py-2.5 px-3">Customer</th>
                  <th className="py-2.5 px-3">Taxable</th>
                  <th className="py-2.5 px-3">Tax (CGST/SGST/IGST)</th>
                  <th className="py-2.5 px-3">Grand Total</th>
                  <th className="py-2.5 px-3">Balance Due</th>
                  <th className="py-2.5 px-3">Status</th>
                  <th className="py-2.5 px-3 text-right">Actions</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-[#E3ECD7]">
                {invoices.map((inv) => (
                  <tr key={inv.id} className="hover:bg-[#F8FAF6]">
                    <td className="py-3 px-3 font-bold text-[#1F2617]">{inv.invoiceNumber}</td>
                    <td className="py-3 px-3 text-[#5F6D44]">{inv.invoiceDate}</td>
                    <td className="py-3 px-3 font-medium text-[#1F2617]">{inv.customerName}</td>
                    <td className="py-3 px-3 font-semibold">₹{inv.taxableAmount}</td>
                    <td className="py-3 px-3 text-[11px] text-[#5F6D44]">
                      {inv.igstAmount > 0 ? (
                        <span>IGST: ₹{inv.igstAmount}</span>
                      ) : (
                        <span>CGST: ₹{inv.cgstAmount} | SGST: ₹{inv.sgstAmount}</span>
                      )}
                    </td>
                    <td className="py-3 px-3 font-bold text-[#1F2617]">₹{inv.grandTotal}</td>
                    <td className="py-3 px-3 font-extrabold text-[#C63D3D]">₹{inv.balanceDue}</td>
                    <td className="py-3 px-3">
                      <span
                        className={`stitch-pill ${
                          inv.status === 'DISPATCHED'
                            ? 'bg-green-100 text-green-800'
                            : inv.status === 'ISSUED'
                            ? 'bg-blue-100 text-blue-800'
                            : 'bg-red-100 text-red-800'
                        }`}
                      >
                        {inv.status}
                      </span>
                    </td>
                    <td className="py-3 px-3 text-right space-x-1.5 whitespace-nowrap">
                      {/* PDF Download Button */}
                      <a
                        href={api.getPdfInvoiceUrl(inv.id)}
                        target="_blank"
                        rel="noreferrer"
                        className="inline-flex items-center gap-1 px-2 py-1 bg-white border border-[#B6C99B] hover:bg-[#E7F5DC] rounded text-xs font-semibold text-[#1F2617]"
                      >
                        <Download size={12} /> PDF
                      </a>

                      {inv.status === 'ISSUED' && (
                        <button
                          onClick={() => handleDispatch(inv.id)}
                          className="px-2.5 py-1 bg-[#728156] hover:bg-[#5F6D44] text-white rounded text-xs font-bold"
                        >
                          Dispatch
                        </button>
                      )}

                      {inv.status !== 'CANCELLED' && (
                        <>
                          {inv.balanceDue > 0 && (
                            <button
                              onClick={() => {
                                setPaymentInvoice(inv);
                                setPaymentAmount(inv.balanceDue.toString());
                                setPaymentModalOpen(true);
                              }}
                              className="px-2 py-1 bg-[#CFE1B9] hover:bg-[#B6C99B] text-[#1F2617] rounded text-xs font-bold"
                            >
                              Pay
                            </button>
                          )}
                          <button
                            onClick={() => handleCancelInvoice(inv.id)}
                            className="px-2 py-1 bg-red-50 hover:bg-red-100 text-[#C63D3D] rounded text-xs font-semibold"
                          >
                            Cancel (CN)
                          </button>
                        </>
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}

      {/* Live Inventory Stock Balances Tab */}
      {activeTab === 'admin-stock' && (
        <div className="stitch-card p-5 space-y-4">
          <h2 className="text-base font-bold text-[#1F2617] border-b border-[#E3ECD7] pb-2">
            Dynamic Stock Ledger Inventory Balances
          </h2>
          <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
            {balances.map((b) => (
              <div key={b.itemId} className="p-4 bg-[#F8FAF6] border border-[#CFE1B9] rounded-xl space-y-2 text-xs">
                <div className="flex justify-between items-start">
                  <div>
                    <span className="font-bold text-[#1F2617] text-sm block">{b.name}</span>
                    <span className="text-[#5F6D44] font-medium">{b.skuCode} ({b.itemType})</span>
                  </div>
                  <span
                    className={`stitch-pill ${
                      b.belowReorderLevel ? 'bg-red-100 text-[#C63D3D] font-bold' : 'bg-green-100 text-green-800'
                    }`}
                  >
                    {b.belowReorderLevel ? 'Low Stock' : 'Adequate'}
                  </span>
                </div>
                <div className="flex justify-between items-baseline pt-2 border-t border-[#E3ECD7]">
                  <span className="text-[#5F6D44]">Ledger Balance:</span>
                  <span className="text-lg font-black text-[#1F2617]">
                    {b.currentBalance} {b.unit}
                  </span>
                </div>
                <div className="flex justify-between text-[11px] text-[#5F6D44]">
                  <span>Reorder Level: {b.reorderLevel} {b.unit}</span>
                  <span>Std Rate: ₹{b.standardRate}</span>
                </div>
              </div>
            ))}
          </div>
        </div>
      )}

      {/* Payment Recording Modal */}
      {paymentModalOpen && paymentInvoice && (
        <div className="fixed inset-0 bg-black/40 backdrop-blur-sm z-50 flex items-center justify-center p-4">
          <div className="bg-white rounded-2xl max-w-md w-full p-6 space-y-4 shadow-xl border border-[#CFE1B9]">
            <h3 className="font-bold text-base text-[#1F2617] border-b pb-2 flex items-center gap-2">
              <DollarSign size={18} className="text-[#728156]" /> Record Payment — {paymentInvoice.invoiceNumber}
            </h3>

            <div className="text-xs space-y-1.5 p-3 bg-[#E7F5DC] rounded-xl">
              <div className="flex justify-between">
                <span>Customer:</span>
                <span className="font-bold">{paymentInvoice.customerName}</span>
              </div>
              <div className="flex justify-between">
                <span>Grand Total:</span>
                <span className="font-bold">₹{paymentInvoice.grandTotal}</span>
              </div>
              <div className="flex justify-between text-red-700 font-bold">
                <span>Balance Due:</span>
                <span>₹{paymentInvoice.balanceDue}</span>
              </div>
            </div>

            <form onSubmit={handleRecordPayment} className="space-y-3 text-xs">
              <div>
                <label className="block font-bold text-[#4B5633] mb-1">Payment Amount (₹)</label>
                <input
                  type="number"
                  step="0.01"
                  max={paymentInvoice.balanceDue}
                  value={paymentAmount}
                  onChange={(e) => setPaymentAmount(e.target.value)}
                  className="w-full bg-[#F8FAF6] border border-[#CFE1B9] rounded-xl p-2 font-bold text-sm"
                  required
                />
              </div>

              <div>
                <label className="block font-bold text-[#4B5633] mb-1">Payment Mode</label>
                <select
                  value={paymentMode}
                  onChange={(e) => setPaymentMode(e.target.value)}
                  className="w-full bg-[#F8FAF6] border border-[#CFE1B9] rounded-xl p-2 font-semibold text-sm"
                >
                  <option value="NEFT">NEFT / Bank Transfer</option>
                  <option value="UPI">UPI</option>
                  <option value="CASH">Cash</option>
                  <option value="CHEQUE">Cheque</option>
                </select>
              </div>

              <div className="flex justify-end gap-2 pt-3 border-t">
                <button
                  type="button"
                  onClick={() => setPaymentModalOpen(false)}
                  className="px-3 py-1.5 rounded-lg border font-semibold hover:bg-gray-50"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  disabled={loading}
                  className="px-4 py-1.5 bg-[#728156] hover:bg-[#5F6D44] text-white rounded-lg font-bold"
                >
                  Confirm Payment
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Edit Before Approve Modal */}
      {editingOrder && (
        <div className="fixed inset-0 bg-black/40 backdrop-blur-sm z-50 flex items-center justify-center p-4">
          <div className="bg-white rounded-2xl max-w-md w-full p-6 space-y-4 shadow-xl border border-[#CFE1B9]">
            <h3 className="font-bold text-base text-[#1F2617] border-b pb-2 flex items-center gap-2">
              <Edit3 size={18} className="text-[#728156]" /> Adjust Order Before Approval — {editingOrder.orderNumber}
            </h3>

            <div className="text-xs space-y-1.5 p-3 bg-[#F8FAF6] rounded-xl">
              <div>Customer: <span className="font-bold">{editingOrder.customerName}</span></div>
              <div>State: <span className="font-bold">{editingOrder.customerState}</span></div>
            </div>

            <div className="space-y-3 text-xs">
              <div>
                <label className="block font-bold text-[#4B5633] mb-1">Adjusted Quantity (pkts)</label>
                <input
                  type="number"
                  value={adjustedQty}
                  onChange={(e) => setAdjustedQty(e.target.value)}
                  className="w-full bg-[#F8FAF6] border border-[#CFE1B9] rounded-xl p-2 font-bold text-sm"
                />
              </div>

              <div>
                <label className="block font-bold text-[#4B5633] mb-1">Adjusted Unit Price (₹)</label>
                <input
                  type="number"
                  step="0.01"
                  value={adjustedPrice}
                  onChange={(e) => setAdjustedPrice(e.target.value)}
                  className="w-full bg-[#F8FAF6] border border-[#CFE1B9] rounded-xl p-2 font-bold text-sm"
                />
              </div>

              <div className="flex justify-end gap-2 pt-3 border-t">
                <button
                  type="button"
                  onClick={() => setEditingOrder(null)}
                  className="px-3 py-1.5 rounded-lg border font-semibold hover:bg-gray-50"
                >
                  Cancel
                </button>
                <button
                  type="button"
                  onClick={() => {
                    const line = editingOrder.lines?.[0];
                    handleApproveInvoice(editingOrder.id, {
                      adjustedLines: [
                        {
                          itemId: line?.itemId,
                          qty: parseInt(adjustedQty) || line?.qty,
                          unitPrice: parseFloat(adjustedPrice) || line?.unitPrice,
                        },
                      ],
                    });
                  }}
                  className="px-4 py-1.5 bg-[#728156] hover:bg-[#5F6D44] text-white rounded-lg font-bold"
                >
                  Approve with Adjustments
                </button>
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
