import React, { useState, useEffect } from 'react';
import { useAuth } from '../../context/AuthContext';
import { api } from '../../services/api';
import { Plus, Minus, Send, CheckCircle2, AlertTriangle, Clock, RefreshCw, Smartphone, Building2, ShoppingBag } from 'lucide-react';

export const SEED_CUSTOMERS = [
  { id: 1, name: 'Retailer A', stateName: 'Tamil Nadu', customerType: 'RETAILER', gstin: '33AABCR1234A1Z5' },
  { id: 2, name: 'Distributor B', stateName: 'Karnataka', customerType: 'DISTRIBUTOR', gstin: '29AABCD5678B1Z9' },
];

export const SEED_ITEMS = [
  { id: 6, skuCode: 'FG-MURUKKU-200', name: 'Murukku 200 g', itemType: 'FINISHED_GOOD', unit: 'PACKET', sellingPrice: 50.00, gstRate: 5.00 },
];

export default function SalesmanWorkspace({
  activeTab,
  isOffline,
  setIsOffline,
  queuedOrders,
  setQueuedOrders,
  onSyncOffline,
  syncTimestamp,
}) {
  const { currentUser } = useAuth();

  const [customers, setCustomers] = useState(SEED_CUSTOMERS);
  const [items, setItems] = useState(SEED_ITEMS);
  const [selectedCustomerId, setSelectedCustomerId] = useState(String(SEED_CUSTOMERS[0].id));
  const [selectedItemId, setSelectedItemId] = useState(String(SEED_ITEMS[0].id));
  const [quantity, setQuantity] = useState(50);
  const [myOrders, setMyOrders] = useState([]);
  const [loading, setLoading] = useState(false);
  const [successMsg, setSuccessMsg] = useState('');
  const [errorMsg, setErrorMsg] = useState('');

  // Load live data and refresh when currentUser or syncTimestamp changes
  useEffect(() => {
    loadData();
  }, [currentUser, syncTimestamp]);

  const loadData = async () => {
    try {
      setLoading(true);
      const [custList, itemList] = await Promise.all([api.getCustomers(), api.getItems()]);

      const validCustomers = Array.isArray(custList) && custList.length > 0 ? custList : SEED_CUSTOMERS;
      const validItems = Array.isArray(itemList) && itemList.length > 0 ? itemList : SEED_ITEMS;

      setCustomers(validCustomers);
      setItems(validItems);

      // Preserve selection if valid in new list, else fallback to first option
      setSelectedCustomerId((prev) => {
        return validCustomers.some((c) => String(c.id) === String(prev)) ? prev : String(validCustomers[0].id);
      });

      const finished = validItems.filter((i) => i.itemType === 'FINISHED_GOOD');
      const fgList = finished.length > 0 ? finished : validItems;
      setSelectedItemId((prev) => {
        return fgList.some((i) => String(i.id) === String(prev)) ? prev : String(fgList[0].id);
      });

      if (currentUser) {
        const orders = await api.getOrdersBySalesman(currentUser.id);
        if (Array.isArray(orders)) {
          setMyOrders(orders);
        }
      }
    } catch (err) {
      console.warn('Backend API lookup returned error; using offline seed catalog:', err);
    } finally {
      setLoading(false);
    }
  };

  // Safe item and customer resolution
  const selectedCustomer =
    customers.find((c) => String(c.id) === String(selectedCustomerId)) || customers[0] || SEED_CUSTOMERS[0];
  const finishedList = items.filter((i) => i.itemType === 'FINISHED_GOOD');
  const availableProducts = finishedList.length > 0 ? finishedList : items;
  const selectedItem =
    availableProducts.find((i) => String(i.id) === String(selectedItemId)) || availableProducts[0] || SEED_ITEMS[0];

  // Live GST Math calculation using exact integer-paisa math (RoundingMode.HALF_UP equivalent)
  const unitPrice = selectedItem?.sellingPrice || 50.0;
  const taxableAmountPaisa = Math.round(quantity * unitPrice * 100);
  const gstRate = selectedItem?.gstRate || 5.0;
  const isIntraState = selectedCustomer?.stateName?.toLowerCase().includes('tamil nadu');

  const totalTaxPaisa = Math.round((taxableAmountPaisa * gstRate) / 100);
  const cgstPaisa = isIntraState ? Math.round(totalTaxPaisa / 2) : 0;
  const sgstPaisa = isIntraState ? totalTaxPaisa - cgstPaisa : 0;
  const igstPaisa = !isIntraState ? totalTaxPaisa : 0;
  const grandTotalPaisa = taxableAmountPaisa + totalTaxPaisa;

  const taxableAmount = (taxableAmountPaisa / 100).toFixed(2);
  const cgst = (cgstPaisa / 100).toFixed(2);
  const sgst = (sgstPaisa / 100).toFixed(2);
  const igst = (igstPaisa / 100).toFixed(2);
  const grandTotal = (grandTotalPaisa / 100).toFixed(2);

  // Customer-wise sales summary aggregated for this salesman
  const customerSummary = customers.map((cust) => {
    const custOrders = myOrders.filter((o) => o.customerId === cust.id);
    const totalQty = custOrders.reduce((sum, o) => sum + (o.lines?.reduce((ls, l) => ls + l.qty, 0) || 0), 0);
    const totalAmount = custOrders.reduce((sum, o) => sum + parseFloat(o.totalEstimatedAmount || 0), 0);
    const shortageCount = custOrders.filter((o) => o.stockShortageFlagged).length;
    return {
      customer: cust,
      orderCount: custOrders.length,
      totalQty,
      totalAmount: totalAmount.toFixed(2),
      shortageCount,
    };
  });

  const handleSubmitOrder = async (e) => {
    e.preventDefault();
    setErrorMsg('');
    setSuccessMsg('');

    const targetCustomerId = Number(selectedCustomer?.id || selectedCustomerId);
    const targetItemId = Number(selectedItem?.id || selectedItemId);

    const payload = {
      customerId: targetCustomerId,
      salesmanId: currentUser.id,
      lines: [
        {
          itemId: targetItemId,
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
    <div className="space-y-5 max-w-4xl mx-auto">
      {/* Mobile-first Bar / Offline switch */}
      <div className="stitch-card p-3 sm:p-3.5 flex flex-wrap items-center justify-between gap-3 bg-gradient-to-r from-white to-[#E7F5DC]/40">
        <div className="flex items-center gap-2">
          <Smartphone className="text-[#728156]" size={18} />
          <span className="font-bold text-xs sm:text-sm text-[#1F2617]">Field Sales Companion</span>
          <span className="text-[11px] bg-[#CFE1B9] text-[#353D23] px-2 py-0.5 rounded-full font-semibold">
            {currentUser?.fullName}
          </span>
        </div>

        <div className="flex items-center gap-2.5">
          <label className="flex items-center gap-1.5 text-xs font-semibold text-[#4B5633] cursor-pointer">
            <input
              type="checkbox"
              checked={isOffline}
              onChange={(e) => setIsOffline(e.target.checked)}
              className="accent-[#728156] w-3.5 h-3.5 cursor-pointer"
            />
            <span>Offline Mode</span>
          </label>
          {queuedOrders.length > 0 && (
            <button
              onClick={onSyncOffline}
              className="bg-[#728156] text-white px-2.5 py-1 rounded-lg text-xs font-bold hover:bg-[#5F6D44] flex items-center gap-1 shadow-xs transition"
            >
              <RefreshCw size={12} /> Sync ({queuedOrders.length})
            </button>
          )}
        </div>
      </div>

      {successMsg && (
        <div className="p-3 bg-green-50 border border-green-200 text-green-800 rounded-xl text-xs sm:text-sm flex items-center gap-2">
          <CheckCircle2 size={16} className="text-green-600 flex-shrink-0" />
          <span>{successMsg}</span>
        </div>
      )}

      {errorMsg && (
        <div className="p-3 bg-red-50 border border-red-200 text-red-800 rounded-xl text-xs sm:text-sm flex items-center gap-2">
          <AlertTriangle size={16} className="text-red-600 flex-shrink-0" />
          <span>{errorMsg}</span>
        </div>
      )}

      {/* New Order View */}
      {activeTab === 'sales-new' && (
        <div className="grid grid-cols-1 md:grid-cols-12 gap-5">
          {/* Order Input Card */}
          <div className="md:col-span-7 stitch-card p-4 sm:p-5 space-y-4">
            <h2 className="text-base sm:text-lg font-bold text-[#1F2617] border-b border-[#E3ECD7] pb-2 flex items-center justify-between">
              <span>Book New Retail Order</span>
              <span className="text-xs font-normal text-[#5F6D44]">Direct Field Entry</span>
            </h2>

            <form onSubmit={handleSubmitOrder} className="space-y-4">
              {/* Customer Selector */}
              <div>
                <label className="block text-xs font-bold text-[#4B5633] uppercase mb-1">
                  Select Customer
                </label>
                <select
                  value={String(selectedCustomer?.id || selectedCustomerId)}
                  onChange={(e) => setSelectedCustomerId(e.target.value)}
                  className="w-full bg-[#F8FAF6] border border-[#CFE1B9] rounded-xl px-3 py-2 text-xs sm:text-sm font-semibold text-[#1F2617] focus:ring-2 focus:ring-[#728156] focus:outline-none cursor-pointer"
                >
                  {customers.map((c) => (
                    <option key={c.id} value={String(c.id)}>
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
                  value={String(selectedItem?.id || selectedItemId)}
                  onChange={(e) => setSelectedItemId(e.target.value)}
                  className="w-full bg-[#F8FAF6] border border-[#CFE1B9] rounded-xl px-3 py-2 text-xs sm:text-sm font-semibold text-[#1F2617] focus:ring-2 focus:ring-[#728156] focus:outline-none cursor-pointer"
                >
                  {availableProducts.map((item) => (
                    <option key={item.id} value={String(item.id)}>
                      {item.name} ({item.skuCode}) — ₹{Number(item.sellingPrice || 50).toFixed(2)}/pkt
                    </option>
                  ))}
                </select>
              </div>

              {/* Thumb-Friendly Quantity Stepper */}
              <div>
                <label className="block text-xs font-bold text-[#4B5633] uppercase mb-1">
                  Order Quantity (Packets)
                </label>
                <div className="flex items-center gap-2.5">
                  <button
                    type="button"
                    onClick={() => setQuantity(Math.max(1, quantity - 10))}
                    className="w-12 h-12 rounded-xl bg-[#CFE1B9] hover:bg-[#B6C99B] text-[#1F2617] font-bold text-xl flex items-center justify-center transition active:scale-95 shadow-xs"
                    aria-label="Decrease quantity by 10"
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
                    className="w-12 h-12 rounded-xl bg-[#728156] hover:bg-[#5F6D44] text-white font-bold text-xl flex items-center justify-center transition active:scale-95 shadow-xs"
                    aria-label="Increase quantity by 10"
                  >
                    <Plus size={18} />
                  </button>
                </div>
                <div className="flex justify-between text-xs text-[#5F6D44] mt-2 px-1 font-medium">
                  <button type="button" onClick={() => setQuantity(50)} className="hover:underline bg-[#E7F5DC] px-2 py-0.5 rounded text-[11px]">+50 pkts</button>
                  <button type="button" onClick={() => setQuantity(100)} className="hover:underline bg-[#E7F5DC] px-2 py-0.5 rounded text-[11px]">+100 pkts</button>
                  <button type="button" onClick={() => setQuantity(120)} className="hover:underline bg-[#E7F5DC] px-2 py-0.5 rounded text-[11px]">+120 pkts</button>
                  <button type="button" onClick={() => setQuantity(150)} className="hover:underline bg-[#E7F5DC] px-2 py-0.5 rounded text-[11px]">+150 pkts</button>
                </div>
              </div>

              {/* Submit Button */}
              <button
                type="submit"
                disabled={loading}
                className="w-full py-3.5 bg-[#728156] hover:bg-[#5F6D44] text-white font-bold rounded-xl shadow-md flex items-center justify-center gap-2 transition active:scale-[0.99] disabled:opacity-50 text-sm sm:text-base"
              >
                <Send size={16} />
                <span>{isOffline ? 'Queue Offline Order' : 'Submit Sales Order'}</span>
              </button>
            </form>
          </div>

          {/* Live GST & Order Breakdown Card */}
          <div className="md:col-span-5 stitch-card p-4 sm:p-5 bg-[#FAFCF8] flex flex-col justify-between space-y-4">
            <div>
              <h3 className="font-bold text-[#1F2617] text-xs sm:text-sm uppercase tracking-wider border-b border-[#E3ECD7] pb-2">
                Live Pricing & GST Breakdown
              </h3>

              <div className="space-y-2.5 mt-3 text-xs">
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
                      <p className="text-[10px] text-green-700 font-medium">Intra-State Supply (Tamil Nadu)</p>
                    </>
                  ) : (
                    <>
                      <div className="flex justify-between text-xs text-[#5F6D44]">
                        <span>IGST (5.0%):</span>
                        <span className="font-medium">₹{igst}</span>
                      </div>
                      <p className="text-[10px] text-blue-700 font-medium">Inter-State Supply (Karnataka)</p>
                    </>
                  )}
                </div>
              </div>
            </div>

            {/* Total Callout */}
            <div className="p-3 bg-[#E7F5DC] border border-[#CFE1B9] rounded-xl flex items-center justify-between">
              <div>
                <span className="text-[10px] uppercase tracking-wider text-[#5F6D44] font-bold block">
                  Grand Total (Incl. GST)
                </span>
                <span className="text-xl sm:text-2xl font-black text-[#1F2617]">₹{grandTotal}</span>
              </div>
              <span className="text-[11px] bg-[#728156] text-white px-2 py-0.5 rounded font-bold">
                5% GST
              </span>
            </div>
          </div>
        </div>
      )}

      {/* Orders List View */}
      {activeTab === 'sales-orders' && (
        <div className="stitch-card p-4 sm:p-5 space-y-4">
          <div className="flex items-center justify-between border-b border-[#E3ECD7] pb-3">
            <div>
              <h2 className="text-base font-bold text-[#1F2617]">My Sales Orders</h2>
              <p className="text-xs text-[#5F6D44]">Submitted orders, customer breakdown, and factory dispatch tracking</p>
            </div>
            <button
              onClick={loadData}
              className="p-1.5 rounded-lg border border-[#CFE1B9] hover:bg-[#E7F5DC] text-[#4B5633] transition"
              title="Refresh"
            >
              <RefreshCw size={14} />
            </button>
          </div>

          {/* Customer-Wise Sales Summary Cards */}
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
            {customerSummary.map(({ customer, orderCount, totalQty, totalAmount, shortageCount }) => (
              <div
                key={customer.id}
                className="p-3 bg-gradient-to-br from-white to-[#F4F8F0] border border-[#CFE1B9] rounded-xl shadow-xs space-y-2"
              >
                <div className="flex items-center justify-between">
                  <div className="flex items-center gap-1.5">
                    <Building2 size={14} className="text-[#728156]" />
                    <span className="font-bold text-xs text-[#1F2617]">{customer.name}</span>
                  </div>
                  <span className="text-[10px] bg-[#CFE1B9] text-[#353D23] px-2 py-0.5 rounded-full font-semibold">
                    {customer.stateName}
                  </span>
                </div>
                <div className="grid grid-cols-3 gap-2 text-center pt-1.5 border-t border-[#E3ECD7]">
                  <div>
                    <span className="text-[10px] text-[#5F6D44] block">Orders</span>
                    <span className="text-xs font-bold text-[#1F2617]">{orderCount}</span>
                  </div>
                  <div>
                    <span className="text-[10px] text-[#5F6D44] block">Booked Qty</span>
                    <span className="text-xs font-bold text-[#1F2617]">{totalQty} pkts</span>
                  </div>
                  <div>
                    <span className="text-[10px] text-[#5F6D44] block">Est. Sales</span>
                    <span className="text-xs font-bold text-[#728156]">₹{totalAmount}</span>
                  </div>
                </div>
                {shortageCount > 0 && (
                  <div className="text-[10px] text-[#C63D3D] font-semibold bg-red-50 border border-red-100 px-2 py-0.5 rounded text-center">
                    {shortageCount} order(s) flagged with stock shortage
                  </div>
                )}
              </div>
            ))}
          </div>

          {queuedOrders.length > 0 && (
            <div className="p-3 bg-amber-50 border border-amber-200 rounded-xl space-y-2">
              <span className="text-xs font-bold text-amber-900 flex items-center gap-1.5">
                <Clock size={13} /> Queued Offline Orders ({queuedOrders.length})
              </span>
              <div className="space-y-1.5 text-xs">
                {queuedOrders.map((q, idx) => (
                  <div key={idx} className="flex justify-between items-center bg-white p-2 rounded-lg border border-amber-100">
                    <div>
                      <span className="font-semibold block">{q.customerName}</span>
                      <span className="text-[11px] text-[#5F6D44]">{q.lines[0]?.qty} pkts Murukku</span>
                    </div>
                    <span className="font-bold text-amber-800 text-[11px] bg-amber-100 px-2 py-0.5 rounded">
                      Pending Sync
                    </span>
                  </div>
                ))}
              </div>
            </div>
          )}

          {/* Mobile-Friendly Orders Card View (390px Viewport) */}
          <div className="block md:hidden space-y-2.5">
            {myOrders.length === 0 ? (
              <div className="py-6 text-center text-[#5F6D44] text-xs">
                No orders placed yet.
              </div>
            ) : (
              myOrders.map((order) => {
                const totalQty = order.lines?.reduce((s, l) => s + l.qty, 0) || 0;
                return (
                  <div
                    key={order.id}
                    className="p-3 bg-white border border-[#CFE1B9] rounded-xl shadow-xs space-y-2 text-xs"
                  >
                    <div className="flex items-center justify-between">
                      <span className="font-bold text-[#1F2617]">{order.orderNumber}</span>
                      <span className="text-[11px] text-[#5F6D44]">
                        {new Date(order.orderDate).toLocaleDateString()}
                      </span>
                    </div>

                    <div className="flex items-center justify-between text-[#1F2617]">
                      <div>
                        <span className="font-semibold block">{order.customerName}</span>
                        <span className="text-[10px] text-[#728156]">{order.customerState}</span>
                      </div>
                      <div className="text-right">
                        <span className="font-extrabold text-[#1F2617] text-sm block">
                          ₹{order.totalEstimatedAmount}
                        </span>
                        <span className="text-[11px] text-[#5F6D44]">{totalQty} pkts</span>
                      </div>
                    </div>

                    <div className="flex items-center justify-between pt-1.5 border-t border-[#E3ECD7]">
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

                      {order.stockShortageFlagged ? (
                        <span className="stitch-pill bg-red-100 text-[#C63D3D] font-bold">
                          Shortage: -{order.shortageQty} pkts
                        </span>
                      ) : (
                        <span className="stitch-pill bg-green-50 text-green-700">Stock OK</span>
                      )}
                    </div>
                  </div>
                );
              })
            )}
          </div>

          {/* Desktop Table View */}
          <div className="hidden md:block overflow-x-auto">
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
