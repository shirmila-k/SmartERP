import React, { useState, useEffect } from 'react';
import { useAuth } from '../../context/AuthContext';
import { api } from '../../services/api';
import { Plus, Minus, Send, CheckCircle2, AlertTriangle, Clock, RefreshCw, Smartphone } from 'lucide-react';

export default function SalesmanWorkspace({ activeTab, isOffline, setIsOffline, queuedOrders, setQueuedOrders, onSyncOffline }) {
  const { currentUser } = useAuth();

  const [customers, setCustomers] = useState([]);
  const [items, setItems] = useState([]);
  const [selectedCustomerId, setSelectedCustomerId] = useState('');
  const [selectedItemId, setSelectedItemId] = useState('');
  const [quantity, setQuantity] = useState(50);
  const [myOrders, setMyOrders] = useState([]);
  const [loading, setLoading] = useState(false);
  const [successMsg, setSuccessMsg] = useState('');
  const [errorMsg, setErrorMsg] = useState('');

  // Load initial data
  useEffect(() => {
    loadData();
  }, [currentUser]);

  const loadData = async () => {
    try {
      setLoading(true);
      const [custList, itemList] = await Promise.all([api.getCustomers(), api.getItems()]);
      setCustomers(custList);
      setItems(itemList);

      if (custList.length > 0 && !selectedCustomerId) {
        setSelectedCustomerId(custList[0].id);
      }
      const finished = itemList.filter((i) => i.itemType === 'FINISHED_GOOD');
      if (finished.length > 0 && !selectedItemId) {
        setSelectedItemId(finished[0].id);
      }

      if (currentUser) {
        const orders = await api.getOrdersBySalesman(currentUser.id);
        setMyOrders(orders);
      }
    } catch (err) {
      console.error(err);
    } finally {
      setLoading(false);
    }
  };

  const selectedCustomer = customers.find((c) => c.id === Number(selectedCustomerId));
  const selectedItem = items.find((i) => i.id === Number(selectedItemId));

  // Live GST Math calculation
  const unitPrice = selectedItem?.sellingPrice || 50.0;
  const taxableAmount = (quantity * unitPrice).toFixed(2);
  const gstRate = selectedItem?.gstRate || 5.0;
  const isIntraState = selectedCustomer?.stateName?.toLowerCase().includes('tamil nadu');

  const totalTax = ((taxableAmount * gstRate) / 100).toFixed(2);
  const cgst = isIntraState ? (totalTax / 2).toFixed(2) : '0.00';
  const sgst = isIntraState ? (totalTax / 2).toFixed(2) : '0.00';
  const igst = !isIntraState ? totalTax : '0.00';
  const grandTotal = (parseFloat(taxableAmount) + parseFloat(totalTax)).toFixed(2);

  const handleSubmitOrder = async (e) => {
    e.preventDefault();
    setErrorMsg('');
    setSuccessMsg('');

    const payload = {
      customerId: Number(selectedCustomerId),
      salesmanId: currentUser.id,
      lines: [
        {
          itemId: Number(selectedItemId),
          qty: quantity,
          unitPrice: unitPrice,
        },
      ],
    };

    if (isOffline) {
      // Save locally
      const offlineItem = {
        ...payload,
        id: 'OFFLINE-' + Date.now(),
        customerName: selectedCustomer?.name,
        customerState: selectedCustomer?.stateName,
        itemName: selectedItem?.name,
        totalEstimatedAmount: grandTotal,
        orderDate: new Date().toISOString(),
        isLocalOffline: true,
      };
      const updated = [...queuedOrders, offlineItem];
      setQueuedOrders(updated);
      localStorage.setItem('offline_orders', JSON.stringify(updated));
      setSuccessMsg('Network offline. Order queued in local device storage!');
      return;
    }

    try {
      setLoading(true);
      const res = await api.createOrder(payload);
      if (res.stockShortageFlagged) {
        setSuccessMsg(`Order ${res.orderNumber} submitted with Stock Shortage Alert (${res.shortageQty} pkts shortage)!`);
      } else {
        setSuccessMsg(`Order ${res.orderNumber} successfully submitted! Total: ₹${grandTotal}`);
      }
      loadData();
    } catch (err) {
      setErrorMsg(err.message || 'Failed to submit order');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="space-y-6 max-w-4xl mx-auto">
      {/* Mobile-first Bar / Offline switch */}
      <div className="stitch-card p-3.5 flex flex-wrap items-center justify-between gap-3 bg-gradient-to-r from-white to-[#E7F5DC]/40">
        <div className="flex items-center gap-2">
          <Smartphone className="text-[#728156]" size={20} />
          <span className="font-bold text-sm text-[#1F2617]">Field Sales Companion</span>
          <span className="text-xs bg-[#CFE1B9] text-[#353D23] px-2 py-0.5 rounded-full font-semibold">
            Salesman: {currentUser?.fullName}
          </span>
        </div>

        <div className="flex items-center gap-3">
          <label className="flex items-center gap-2 text-xs font-semibold text-[#4B5633] cursor-pointer">
            <input
              type="checkbox"
              checked={isOffline}
              onChange={(e) => setIsOffline(e.target.checked)}
              className="accent-[#728156] w-4 h-4 cursor-pointer"
            />
            Simulate Weak Network / Offline
          </label>
          {queuedOrders.length > 0 && (
            <button
              onClick={onSyncOffline}
              className="bg-[#728156] text-white px-3 py-1 rounded-lg text-xs font-bold hover:bg-[#5F6D44] flex items-center gap-1 shadow-sm"
            >
              <RefreshCw size={13} /> Sync ({queuedOrders.length})
            </button>
          )}
        </div>
      </div>

      {successMsg && (
        <div className="p-3 bg-green-50 border border-green-200 text-green-800 rounded-xl text-sm flex items-center gap-2">
          <CheckCircle2 size={16} className="text-green-600 flex-shrink-0" />
          <span>{successMsg}</span>
        </div>
      )}

      {errorMsg && (
        <div className="p-3 bg-red-50 border border-red-200 text-red-800 rounded-xl text-sm flex items-center gap-2">
          <AlertTriangle size={16} className="text-red-600 flex-shrink-0" />
          <span>{errorMsg}</span>
        </div>
      )}

      {/* New Order View */}
      {activeTab === 'sales-new' && (
        <div className="grid grid-cols-1 md:grid-cols-12 gap-6">
          {/* Order Input Card */}
          <div className="md:col-span-7 stitch-card p-5 space-y-4">
            <h2 className="text-lg font-bold text-[#1F2617] border-b border-[#E3ECD7] pb-2 flex items-center justify-between">
              <span>Book New Retail Order</span>
              <span className="text-xs font-normal text-[#5F6D44]">Step 1 of 2</span>
            </h2>

            <form onSubmit={handleSubmitOrder} className="space-y-4">
              {/* Customer Selector */}
              <div>
                <label className="block text-xs font-bold text-[#4B5633] uppercase mb-1">
                  Select Customer
                </label>
                <select
                  value={selectedCustomerId}
                  onChange={(e) => setSelectedCustomerId(e.target.value)}
                  className="w-full bg-[#F8FAF6] border border-[#CFE1B9] rounded-xl px-3 py-2 text-sm font-semibold text-[#1F2617] focus:ring-2 focus:ring-[#728156] focus:outline-none"
                >
                  {customers.map((c) => (
                    <option key={c.id} value={c.id}>
                      {c.name} — {c.stateName} ({c.customerType})
                    </option>
                  ))}
                </select>
                {selectedCustomer && (
                  <p className="text-[11px] text-[#728156] mt-1 font-medium">
                    GSTIN: {selectedCustomer.gstin || 'Unregistered'} • State: {selectedCustomer.stateName}
                  </p>
                )}
              </div>

              {/* Product Selector */}
              <div>
                <label className="block text-xs font-bold text-[#4B5633] uppercase mb-1">
                  Product
                </label>
                <select
                  value={selectedItemId}
                  onChange={(e) => setSelectedItemId(e.target.value)}
                  className="w-full bg-[#F8FAF6] border border-[#CFE1B9] rounded-xl px-3 py-2 text-sm font-semibold text-[#1F2617] focus:ring-2 focus:ring-[#728156] focus:outline-none"
                >
                  {items
                    .filter((i) => i.itemType === 'FINISHED_GOOD')
                    .map((item) => (
                      <option key={item.id} value={item.id}>
                        {item.name} ({item.skuCode}) — ₹{item.sellingPrice}/pkt
                      </option>
                    ))}
                </select>
              </div>

              {/* Thumb-Friendly Quantity Stepper */}
              <div>
                <label className="block text-xs font-bold text-[#4B5633] uppercase mb-1">
                  Order Quantity (Packets)
                </label>
                <div className="flex items-center gap-3">
                  <button
                    type="button"
                    onClick={() => setQuantity(Math.max(1, quantity - 10))}
                    className="w-12 h-12 rounded-xl bg-[#CFE1B9] hover:bg-[#B6C99B] text-[#1F2617] font-bold text-xl flex items-center justify-center transition active:scale-95 shadow-sm"
                  >
                    <Minus size={18} />
                  </button>

                  <input
                    type="number"
                    min="1"
                    value={quantity}
                    onChange={(e) => setQuantity(Math.max(1, parseInt(e.target.value) || 1))}
                    className="flex-1 text-center font-extrabold text-2xl py-2 bg-[#F8FAF6] border border-[#CFE1B9] rounded-xl focus:ring-2 focus:ring-[#728156] focus:outline-none text-[#1F2617]"
                  />

                  <button
                    type="button"
                    onClick={() => setQuantity(quantity + 10)}
                    className="w-12 h-12 rounded-xl bg-[#728156] hover:bg-[#5F6D44] text-white font-bold text-xl flex items-center justify-center transition active:scale-95 shadow-sm"
                  >
                    <Plus size={18} />
                  </button>
                </div>
                <div className="flex justify-between text-xs text-[#5F6D44] mt-1.5 px-1 font-medium">
                  <button type="button" onClick={() => setQuantity(50)} className="hover:underline">+50 pkts</button>
                  <button type="button" onClick={() => setQuantity(100)} className="hover:underline">+100 pkts</button>
                  <button type="button" onClick={() => setQuantity(120)} className="hover:underline">+120 pkts</button>
                  <button type="button" onClick={() => setQuantity(150)} className="hover:underline">+150 pkts</button>
                </div>
              </div>

              {/* Submit Button */}
              <button
                type="submit"
                disabled={loading}
                className="w-full py-3.5 bg-[#728156] hover:bg-[#5F6D44] text-white font-bold rounded-xl shadow-md flex items-center justify-center gap-2 transition active:scale-[0.99] disabled:opacity-50 text-base"
              >
                <Send size={18} />
                <span>{isOffline ? 'Queue Offline Order' : 'Submit Sales Order'}</span>
              </button>
            </form>
          </div>

          {/* Live GST & Order Breakdown Card */}
          <div className="md:col-span-5 stitch-card p-5 bg-[#FAFCF8] flex flex-col justify-between space-y-4">
            <div>
              <h3 className="font-bold text-[#1F2617] text-sm uppercase tracking-wider border-b border-[#E3ECD7] pb-2">
                Live Pricing & GST Breakdown
              </h3>

              <div className="space-y-3 mt-3 text-xs">
                <div className="flex justify-between py-1 border-b border-dashed border-[#E3ECD7]">
                  <span className="text-[#5F6D44]">Product</span>
                  <span className="font-semibold text-[#1F2617]">{selectedItem?.name}</span>
                </div>
                <div className="flex justify-between py-1 border-b border-dashed border-[#E3ECD7]">
                  <span className="text-[#5F6D44]">Rate</span>
                  <span className="font-semibold text-[#1F2617]">₹{unitPrice.toFixed(2)} / pkt</span>
                </div>
                <div className="flex justify-between py-1 border-b border-dashed border-[#E3ECD7]">
                  <span className="text-[#5F6D44]">Quantity</span>
                  <span className="font-semibold text-[#1F2617]">{quantity} packets</span>
                </div>
                <div className="flex justify-between py-1 border-b border-[#CFE1B9] text-sm font-bold">
                  <span>Taxable Value</span>
                  <span className="text-[#1F2617]">₹{taxableAmount}</span>
                </div>

                {/* GST Place of Supply */}
                <div className="p-2.5 bg-white rounded-lg border border-[#CFE1B9] space-y-1.5">
                  <div className="flex justify-between text-[11px] font-bold text-[#728156]">
                    <span>Place of Supply:</span>
                    <span>{selectedCustomer?.stateName || 'Tamil Nadu'}</span>
                  </div>
                  {isIntraState ? (
                    <>
                      <div className="flex justify-between text-xs text-[#5F6D44]">
                        <span>CGST (2.5%):</span>
                        <span className="font-medium">₹{cgst}</span>
                      </div>
                      <div className="flex justify-between text-xs text-[#5F6D44]">
                        <span>SGST (2.5%):</span>
                        <span className="font-medium">₹{sgst}</span>
                      </div>
                      <p className="text-[10px] text-green-700 italic">Intra-State Supply (Tamil Nadu)</p>
                    </>
                  ) : (
                    <>
                      <div className="flex justify-between text-xs text-[#5F6D44]">
                        <span>IGST (5.0%):</span>
                        <span className="font-medium">₹{igst}</span>
                      </div>
                      <p className="text-[10px] text-blue-700 italic">Inter-State Supply (Karnataka)</p>
                    </>
                  )}
                </div>
              </div>
            </div>

            {/* Total Callout */}
            <div className="p-3 bg-[#E7F5DC] border border-[#CFE1B9] rounded-xl flex items-center justify-between">
              <div>
                <span className="text-[11px] uppercase tracking-wider text-[#5F6D44] font-bold block">
                  Grand Total (Incl. GST)
                </span>
                <span className="text-2xl font-black text-[#1F2617]">₹{grandTotal}</span>
              </div>
              <span className="text-xs bg-[#728156] text-white px-2 py-1 rounded font-bold">
                5% GST
              </span>
            </div>
          </div>
        </div>
      )}

      {/* Orders List View */}
      {activeTab === 'sales-orders' && (
        <div className="stitch-card p-5 space-y-4">
          <div className="flex items-center justify-between border-b border-[#E3ECD7] pb-3">
            <div>
              <h2 className="text-base font-bold text-[#1F2617]">My Sales Orders</h2>
              <p className="text-xs text-[#5F6D44]">Submitted orders and real-time factory dispatch tracking</p>
            </div>
            <button
              onClick={loadData}
              className="p-1.5 rounded-lg border border-[#CFE1B9] hover:bg-[#E7F5DC] text-[#4B5633]"
              title="Refresh"
            >
              <RefreshCw size={14} />
            </button>
          </div>

          {queuedOrders.length > 0 && (
            <div className="p-3 bg-amber-50 border border-amber-200 rounded-xl space-y-2">
              <span className="text-xs font-bold text-amber-900 flex items-center gap-1.5">
                <Clock size={13} /> Queued Offline Orders ({queuedOrders.length})
              </span>
              <div className="space-y-1 text-xs">
                {queuedOrders.map((q, idx) => (
                  <div key={idx} className="flex justify-between bg-white p-2 rounded border border-amber-100">
                    <span>{q.customerName} — {q.lines[0]?.qty} pkts</span>
                    <span className="font-bold text-amber-800">Pending Sync</span>
                  </div>
                ))}
              </div>
            </div>
          )}

          <div className="overflow-x-auto">
            <table className="w-full text-xs text-left">
              <thead>
                <tr className="bg-[#CFE1B9]/50 text-[#353D23] font-bold border-b border-[#CFE1B9]">
                  <th className="py-2.5 px-3">Order No</th>
                  <th className="py-2.5 px-3">Date</th>
                  <th className="py-2.5 px-3">Customer</th>
                  <th className="py-2.5 px-3">Qty</th>
                  <th className="py-2.5 px-3">Est. Total</th>
                  <th className="py-2.5 px-3">Status</th>
                  <th className="py-2.5 px-3">Stock Warning</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-[#E3ECD7]">
                {myOrders.length === 0 ? (
                  <tr>
                    <td colSpan="7" className="py-6 text-center text-[#5F6D44]">
                      No orders placed yet.
                    </td>
                  </tr>
                ) : (
                  myOrders.map((order) => {
                    const totalQty = order.lines?.reduce((s, l) => s + l.qty, 0) || 0;
                    return (
                      <tr key={order.id} className="hover:bg-[#F8FAF6] transition">
                        <td className="py-2.5 px-3 font-bold text-[#1F2617]">{order.orderNumber}</td>
                        <td className="py-2.5 px-3 text-[#5F6D44]">
                          {new Date(order.orderDate).toLocaleDateString()}
                        </td>
                        <td className="py-2.5 px-3 font-medium text-[#1F2617]">
                          {order.customerName}
                          <span className="text-[10px] text-[#728156] block">{order.customerState}</span>
                        </td>
                        <td className="py-2.5 px-3 font-semibold">{totalQty} pkts</td>
                        <td className="py-2.5 px-3 font-bold text-[#1F2617]">₹{order.totalEstimatedAmount}</td>
                        <td className="py-2.5 px-3">
                          <span
                            className={`stitch-pill ${
                              order.status === 'DISPATCHED'
                                ? 'bg-green-100 text-green-800'
                                : order.status === 'INVOICED'
                                ? 'bg-blue-100 text-blue-800'
                                : order.status === 'APPROVED'
                                ? 'bg-[#CFE1B9] text-[#353D23]'
                                : 'bg-amber-100 text-amber-800'
                            }`}
                          >
                            {order.status}
                          </span>
                        </td>
                        <td className="py-2.5 px-3">
                          {order.stockShortageFlagged ? (
                            <span className="stitch-pill bg-red-100 text-[#C63D3D] font-bold">
                              Shortage: -{order.shortageQty} pkts
                            </span>
                          ) : (
                            <span className="stitch-pill bg-green-50 text-green-700">Stock OK</span>
                          )}
                        </td>
                      </tr>
                    );
                  })
                )}
              </tbody>
            </table>
          </div>
        </div>
      )}
    </div>
  );
}
