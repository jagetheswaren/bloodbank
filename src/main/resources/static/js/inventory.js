/**
 * Inventory Module
 * Usable Stock, Near-Expiry Window, Expired Units & Safe Unit Filtering
 */

import { BloodBankApi } from './api.js';
import { UI } from './ui.js';

let currentUnits = [];

document.addEventListener('DOMContentLoaded', async () => {
  const inventoryTable = document.getElementById('inventory-tbody');
  if (!inventoryTable) return;

  const viewMode = inventoryTable.dataset.viewMode || 'all'; // 'all', 'near-expiry', 'expired'
  await loadInventoryUnits(viewMode);

  const searchInput = document.getElementById('unit-search-input');
  const bgFilter = document.getElementById('inventory-bg-filter');
  const statusFilter = document.getElementById('inventory-status-filter');

  if (searchInput) searchInput.addEventListener('input', () => filterAndRenderUnits());
  if (bgFilter) bgFilter.addEventListener('change', () => filterAndRenderUnits());
  if (statusFilter) statusFilter.addEventListener('change', () => filterAndRenderUnits());
});

async function loadInventoryUnits(viewMode) {
  const tbody = document.getElementById('inventory-tbody');
  UI.renderTableSkeleton(tbody, 6, 6);

  try {
    let page;
    if (viewMode === 'near-expiry') {
      page = await BloodBankApi.getNearExpiry(0, 100);
    } else if (viewMode === 'expired') {
      page = await BloodBankApi.getExpired(0, 100);
    } else {
      page = await BloodBankApi.getInventory(0, 100);
    }

    currentUnits = page.content || [];
    filterAndRenderUnits();
  } catch (error) {
    console.error('Failed to load inventory:', error);
    tbody.innerHTML = `<tr><td colspan="6" style="text-align:center;color:var(--danger);padding:2rem;">Failed to load inventory units: ${escapeHtml(error.message)}</td></tr>`;
  }
}

function filterAndRenderUnits() {
  const tbody = document.getElementById('inventory-tbody');
  const search = (document.getElementById('unit-search-input')?.value || '').trim().toLowerCase();
  const bgFilter = document.getElementById('inventory-bg-filter')?.value || '';
  const statusFilter = document.getElementById('inventory-status-filter')?.value || '';

  let filtered = currentUnits;

  if (bgFilter) {
    filtered = filtered.filter(u => u.bloodGroup === bgFilter);
  }

  if (statusFilter) {
    filtered = filtered.filter(u => u.status === statusFilter);
  }

  if (search) {
    filtered = filtered.filter(u =>
      (u.unitCode && u.unitCode.toLowerCase().includes(search)) ||
      (u.donationCode && u.donationCode.toLowerCase().includes(search))
    );
  }

  if (filtered.length === 0) {
    const viewMode = tbody.dataset.viewMode || 'all';
    let emptyTitle = 'No blood units found';
    let emptyText = 'No blood units match the selected filters.';

    if (viewMode === 'near-expiry' && !search && !bgFilter && !statusFilter) {
      emptyTitle = 'No Near-Expiry Units';
      emptyText = 'No blood units are currently within the 7-day near-expiry window.';
    } else if (viewMode === 'expired' && !search && !bgFilter && !statusFilter) {
      emptyTitle = 'No Expired Units';
      emptyText = 'No expired blood units are currently recorded.';
    }

    tbody.innerHTML = `
      <tr>
        <td colspan="6">
          <div class="empty-state">
            <svg class="empty-state-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
              <rect x="2" y="7" width="20" height="14" rx="2" ry="2"></rect>
              <path d="M16 21V5a2 2 0 0 0-2-2h-4a2 2 0 0 0-2 2v16"></path>
            </svg>
            <h3 class="empty-state-title">${escapeHtml(emptyTitle)}</h3>
            <p class="empty-state-text">${escapeHtml(emptyText)}</p>
          </div>
        </td>
      </tr>
    `;
    return;
  }

  const today = new Date();
  today.setHours(0, 0, 0, 0);

  tbody.innerHTML = filtered.map(u => {
    let expiryNote = '';
    if (u.expiryDate) {
      const exp = new Date(u.expiryDate);
      exp.setHours(0, 0, 0, 0);
      const diffDays = Math.round((exp - today) / (1000 * 60 * 60 * 24));
      if (diffDays < 0) {
        expiryNote = `<span style="color:var(--status-expired);font-weight:600;font-size:0.75rem;">Expired ${Math.abs(diffDays)}d ago</span>`;
      } else if (diffDays <= 7) {
        expiryNote = `<span style="color:var(--status-near-expiry);font-weight:600;font-size:0.75rem;">Expires in ${diffDays}d</span>`;
      } else {
        expiryNote = `<span style="color:var(--text-muted);font-size:0.75rem;">${diffDays} days remaining</span>`;
      }
    }

    return `
      <tr>
        <td><span class="font-mono-code">${escapeHtml(u.unitCode)}</span></td>
        <td><span style="font-weight:700;color:var(--primary);font-size:1.05rem;">${escapeHtml(u.bloodGroup)}</span></td>
        <td>${UI.formatDate(u.collectionDate)}</td>
        <td>
          ${UI.formatDate(u.expiryDate)}<br>
          ${expiryNote}
        </td>
        <td>${UI.formatBadge(u.status)}</td>
        <td><span class="font-mono-code" style="color:var(--text-secondary);">${escapeHtml(u.donationCode || '—')}</span></td>
      </tr>
    `;
  }).join('');
}

function escapeHtml(str) {
  if (typeof str !== 'string') return str;
  return str.replace(/[&<>'"]/g, tag => ({
    '&': '&amp;',
    '<': '&lt;',
    '>': '&gt;',
    "'": '&#39;',
    '"': '&quot;'
  }[tag] || tag));
}
