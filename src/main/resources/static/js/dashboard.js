/**
 * Dashboard Page Logic
 * Loads real KPI metrics, 8-blood group stock distribution, and recent operations
 */

import { BloodBankApi } from './api.js';
import { UI } from './ui.js';

const BLOOD_GROUPS = ['A+', 'A-', 'B+', 'B-', 'AB+', 'AB-', 'O+', 'O-'];

document.addEventListener('DOMContentLoaded', async () => {
  await loadDashboardData();
});

async function loadDashboardData() {
  try {
    // 1. Fetch Real Usable Stock
    const stockMap = await BloodBankApi.getStock();
    renderStockGrid(stockMap);

    // Calculate total available safe units
    let totalUsableUnits = 0;
    if (stockMap) {
      Object.values(stockMap).forEach(count => {
        totalUsableUnits += Number(count || 0);
      });
    }
    const kpiAvailableEl = document.getElementById('kpi-available-units');
    if (kpiAvailableEl) kpiAvailableEl.textContent = totalUsableUnits;

    // 2. Fetch Donors Count
    const donorsPage = await BloodBankApi.getDonors(0, 1);
    const kpiDonorsEl = document.getElementById('kpi-total-donors');
    if (kpiDonorsEl) kpiDonorsEl.textContent = donorsPage.totalElements ?? '—';

    // 3. Fetch Near-Expiry Count
    const nearExpiryPage = await BloodBankApi.getNearExpiry(0, 1);
    const kpiNearExpiryEl = document.getElementById('kpi-near-expiry');
    if (kpiNearExpiryEl) kpiNearExpiryEl.textContent = nearExpiryPage.totalElements ?? '0';

    // 4. Fetch Expired Count
    const expiredPage = await BloodBankApi.getExpired(0, 1);
    const kpiExpiredEl = document.getElementById('kpi-expired');
    if (kpiExpiredEl) kpiExpiredEl.textContent = expiredPage.totalElements ?? '0';

    // 5. Fetch Issues Count
    const issuesPage = await BloodBankApi.getIssues(0, 1);
    const kpiIssuesEl = document.getElementById('kpi-issued-records');
    if (kpiIssuesEl) kpiIssuesEl.textContent = issuesPage.totalElements ?? '0';

    // 6. Load Recent Activity
    loadRecentDonations();
    loadRecentIssues();

  } catch (error) {
    console.error('Error loading dashboard data:', error);
    UI.showToast(`Unable to load dashboard data: ${error.message}`, 'error');
  }
}

function renderStockGrid(stockMap = {}) {
  const container = document.getElementById('blood-stock-grid');
  if (!container) return;

  container.innerHTML = '';

  BLOOD_GROUPS.forEach(bg => {
    const count = stockMap[bg] ?? 0;
    const tile = document.createElement('div');
    tile.className = 'stock-tile';

    let statusClass = 'healthy';
    let statusText = 'Healthy';
    let fillWidth = Math.min((count / 10) * 100, 100);

    if (count === 0) {
      statusClass = 'empty';
      statusText = 'Unavailable';
    } else if (count === 1) {
      statusClass = 'critical';
      statusText = 'Critical';
    } else if (count <= 3) {
      statusClass = 'low';
      statusText = 'Low';
    }

    tile.innerHTML = `
      <div class="stock-tile-top">
        <span class="blood-type-badge">${bg}</span>
        <span class="stock-status-pill ${statusClass}">${statusText}</span>
      </div>
      <div class="stock-count-wrap">
        <span class="stock-number">${count}</span>
        <span class="stock-unit">usable units</span>
      </div>
      <div class="stock-indicator">
        <div class="stock-indicator-fill ${statusClass}" style="width: ${fillWidth}%;"></div>
      </div>
    `;

    container.appendChild(tile);
  });
}

async function loadRecentDonations() {
  const tbody = document.getElementById('recent-donations-tbody');
  if (!tbody) return;

  try {
    const page = await BloodBankApi.getDonations(0, 5);
    const donations = page.content || [];

    if (donations.length === 0) {
      tbody.innerHTML = `<tr><td colspan="5" style="text-align:center;color:var(--text-muted);padding:1.5rem;">No donations recorded yet.</td></tr>`;
      return;
    }

    tbody.innerHTML = donations.map(d => `
      <tr>
        <td><span class="font-mono-code">${escapeHtml(d.donationCode)}</span></td>
        <td><strong>${escapeHtml(d.donorName || '—')}</strong></td>
        <td><span style="font-weight:700;color:var(--primary);">${escapeHtml(d.bloodGroup)}</span></td>
        <td>${escapeHtml(d.unitsCreated || d.units || 1)} unit(s)</td>
        <td>${UI.formatDate(d.donationDate)}</td>
      </tr>
    `).join('');
  } catch (err) {
    tbody.innerHTML = `<tr><td colspan="5" style="text-align:center;color:var(--danger);padding:1rem;">Failed to load recent donations.</td></tr>`;
  }
}

async function loadRecentIssues() {
  const tbody = document.getElementById('recent-issues-tbody');
  if (!tbody) return;

  try {
    const page = await BloodBankApi.getIssues(0, 5);
    const issues = page.content || [];

    if (issues.length === 0) {
      tbody.innerHTML = `<tr><td colspan="5" style="text-align:center;color:var(--text-muted);padding:1.5rem;">No blood units issued yet.</td></tr>`;
      return;
    }

    tbody.innerHTML = issues.map(i => `
      <tr>
        <td><span class="font-mono-code">${escapeHtml(i.issueCode)}</span></td>
        <td><strong>${escapeHtml(i.patientName)}</strong></td>
        <td>${escapeHtml(i.hospitalName)}</td>
        <td><span style="font-weight:700;color:var(--primary);">${escapeHtml(i.bloodGroup)}</span></td>
        <td>${UI.formatDate(i.issueDate)}</td>
      </tr>
    `).join('');
  } catch (err) {
    tbody.innerHTML = `<tr><td colspan="5" style="text-align:center;color:var(--danger);padding:1rem;">Failed to load recent issues.</td></tr>`;
  }
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
