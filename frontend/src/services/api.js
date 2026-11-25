const API_BASE = 'http://localhost:8080/api';

export async function apiFetch(endpoint, options = {}) {
  const url = endpoint.startsWith('http') ? endpoint : `${API_BASE}${endpoint}`;
  const response = await fetch(url, {
    ...options,
    headers: {
      'Content-Type': 'application/json',
      ...options.headers,
    },
  });

  if (!response.ok) {
    let errorMsg = `HTTP Error ${response.status}`;
    try {
      const errJson = await response.json();
      if (errJson.message) {
        errorMsg = errJson.message;
      }
    } catch (e) {
      // not json
    }
    throw new Error(errorMsg);
  }

  const contentType = response.headers.get('content-type');
  if (contentType && contentType.includes('application/json')) {
    return response.json();
  }
  return response.text();
}

export const api = {
  // Users & Lookup
  getUsers: () => apiFetch('/users'),
  getCustomers: () => apiFetch('/customers'),
  getItems: () => apiFetch('/items'),
  getInventoryBalances: () => apiFetch('/inventory/balances'),

  // Purchases
  createPurchase: (data) => apiFetch('/purchases', { method: 'POST', body: JSON.stringify(data) }),
  getPurchases: () => apiFetch('/purchases'),

  // BOM & Production
  getBom: (finishedItemId) => apiFetch(`/bom/${finishedItemId}`),
  createProductionRun: (data) => apiFetch('/production-runs', { method: 'POST', body: JSON.stringify(data) }),
  getProductionRuns: () => apiFetch('/production-runs'),

  // Sales Orders
  createOrder: (data) => apiFetch('/orders', { method: 'POST', body: JSON.stringify(data) }),
  getOrders: () => apiFetch('/orders'),
  getOrdersBySalesman: (salesmanId) => apiFetch(`/orders/salesman/${salesmanId}`),
  approveAndInvoice: (orderId, data) => apiFetch(`/orders/${orderId}/approve-invoice`, { method: 'POST', body: JSON.stringify(data || {}) }),
  rejectOrder: (orderId, reason) => apiFetch(`/orders/${orderId}/reject`, { method: 'POST', body: JSON.stringify({ reason }) }),

  // Invoices & Dispatch
  getInvoices: () => apiFetch('/invoices'),
  dispatchInvoice: (invoiceId) => apiFetch(`/invoices/${invoiceId}/dispatch`, { method: 'POST' }),
  cancelInvoice: (invoiceId, userId, reason) => apiFetch(`/invoices/${invoiceId}/cancel`, { method: 'POST', body: JSON.stringify({ userId, reason }) }),
  getPdfInvoiceUrl: (invoiceId) => `${API_BASE}/invoices/${invoiceId}/pdf`,

  // Payments & Receivables
  recordPayment: (data) => apiFetch('/payments', { method: 'POST', body: JSON.stringify(data) }),
  getCustomerOutstanding: (customerId) => apiFetch(`/customers/${customerId}/outstanding`),

  // Reports
  getSalesReport: (start, end) => apiFetch(`/reports/sales?startDate=${start}&endDate=${end}`),
  getGstSummaryReport: (start, end) => apiFetch(`/reports/gst-summary?startDate=${start}&endDate=${end}`),
  getPurchaseReport: (start, end) => apiFetch(`/reports/purchases?startDate=${start}&endDate=${end}`),
  getStockSummaryReport: (start, end) => apiFetch(`/reports/stock-summary?startDate=${start}&endDate=${end}`),
  getAuditStockLedger: (itemId, start, end) => apiFetch(`/reports/stock-ledger?${itemId ? `itemId=${itemId}&` : ''}startDate=${start}&endDate=${end}`),
  getProductionReport: (start, end) => apiFetch(`/reports/production?startDate=${start}&endDate=${end}`),
  getReceivablesReport: (start, end) => apiFetch(`/reports/receivables?startDate=${start}&endDate=${end}`),
  getExportUrl: (type, format, start, end) => `${API_BASE}/reports/${type}/export?format=${format}&startDate=${start}&endDate=${end}`,

  // Dashboard
  getOwnerDashboard: () => apiFetch('/dashboard/owner'),
};
