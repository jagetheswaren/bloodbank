/**
 * Issues Module
 * Safe FEFO Blood Issuing & Issue History
 */

import { BloodBankApi } from './api.js';
import { UI } from './ui.js';

document.addEventListener('DOMContentLoaded', () => {
  const issuesTableBody = document.getElementById('issues-tbody');
  const issueForm = document.getElementById('issue-blood-form');

  if (issuesTableBody) {
    loadIssuesList();
  } else if (issueForm) {
    initIssueForm();
  }
});

// --- Issues History List ---
async function loadIssuesList() {
  const tbody = document.getElementById('issues-tbody');
  UI.renderTableSkeleton(tbody, 5, 6);

  try {
    const page = await BloodBankApi.getIssues(0, 50);
    const issues = page.content || [];

    if (issues.length === 0) {
      tbody.innerHTML = `
        <tr>
          <td colspan="6">
            <div class="empty-state">
              <svg class="empty-state-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
                <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"></path>
                <polyline points="14 2 14 8 20 8"></polyline>
                <line x1="16" y1="13" x2="8" y2="13"></line>
                <line x1="16" y1="17" x2="8" y2="17"></line>
                <polyline points="10 9 9 9 8 9"></polyline>
              </svg>
              <h3 class="empty-state-title">No blood issue records found</h3>
              <p class="empty-state-text">Process a clinical blood issue request to allocate verified safe units using FEFO.</p>
              <a href="/issues/new" class="btn btn-primary btn-sm">Issue Blood</a>
            </div>
          </td>
        </tr>
      `;
      return;
    }

    tbody.innerHTML = issues.map(i => `
      <tr>
        <td><span class="font-mono-code">${escapeHtml(i.issueCode)}</span></td>
        <td><strong>${escapeHtml(i.patientName)}</strong></td>
        <td>${escapeHtml(i.hospitalName)}</td>
        <td><span style="font-weight:700;color:var(--primary);">${escapeHtml(i.bloodGroup)}</span></td>
        <td>
          <span class="font-mono-code">${escapeHtml(i.unitCode || i.bloodUnitCode || '—')}</span>
        </td>
        <td>${UI.formatDate(i.issueDate)}</td>
      </tr>
    `).join('');
  } catch (error) {
    console.error('Failed to load issues:', error);
    tbody.innerHTML = `<tr><td colspan="6" style="text-align:center;color:var(--danger);padding:2rem;">Failed to load issue records: ${escapeHtml(error.message)}</td></tr>`;
  }
}

// --- Issue Blood Form ---
async function initIssueForm() {
  const form = document.getElementById('issue-blood-form');
  const bgSelect = document.getElementById('issue-blood-group');
  const stockHintEl = document.getElementById('selected-bg-stock-hint');
  const submitBtn = document.getElementById('submit-issue-btn');
  const receiptSection = document.getElementById('issue-receipt-section');
  const formSection = document.getElementById('issue-form-section');

  let stockMap = {};
  try {
    stockMap = await BloodBankApi.getStock();
  } catch (err) {
    console.error('Failed to fetch stock for issue validation:', err);
  }

  function updateStockHint() {
    const selectedBg = bgSelect.value;
    if (!selectedBg) {
      stockHintEl.innerHTML = 'Select a blood group to view currently available safe units.';
      stockHintEl.style.color = 'var(--text-muted)';
      return;
    }

    const availableCount = stockMap[selectedBg] ?? 0;
    if (availableCount > 0) {
      stockHintEl.innerHTML = `<strong>${availableCount} safe unit(s)</strong> available for ${selectedBg} (near-expiry and expired units excluded).`;
      stockHintEl.style.color = 'var(--status-available)';
    } else {
      stockHintEl.innerHTML = `<strong style="color:var(--danger)">0 safe units available</strong> for ${selectedBg}. Blood issuing will be safely blocked until new donations arrive.`;
      stockHintEl.style.color = 'var(--danger)';
    }
  }

  bgSelect.addEventListener('change', updateStockHint);
  updateStockHint();

  form.addEventListener('submit', async (e) => {
    e.preventDefault();

    const bloodGroup = bgSelect.value;
    const units = parseInt(form.elements['units'].value, 10);
    const patientName = form.elements['patientName'].value.trim();
    const hospitalName = form.elements['hospitalName'].value.trim();
    const notes = form.elements['notes'].value.trim();

    if (!bloodGroup) {
      UI.showToast('Please select a blood group.', 'warning');
      return;
    }
    if (!patientName) {
      UI.showToast('Please enter patient name.', 'warning');
      return;
    }
    if (!hospitalName) {
      UI.showToast('Please enter hospital name.', 'warning');
      return;
    }

    const payload = {
      bloodGroup: bloodGroup,
      numberOfUnits: units || 1,
      patientName: patientName,
      hospitalName: hospitalName,
      notes: notes || 'Clinical requisition'
    };

    UI.setButtonLoading(submitBtn, true, 'Confirm & Issue Blood Unit', 'Processing FEFO Allocation...');

    try {
      const response = await BloodBankApi.createIssue(payload);
      UI.showToast('Blood unit allocated and issued successfully via FEFO!', 'success');

      formSection.style.display = 'none';
      receiptSection.style.display = 'block';

      renderIssueReceipt(response);
    } catch (err) {
      UI.setButtonLoading(submitBtn, false, 'Confirm & Issue Blood Unit');
      UI.showToast(`Issue failed: ${err.message}`, 'error', 8000);
    }
  });

  function renderIssueReceipt(res) {
    const firstUnit = (res.issuedUnits && res.issuedUnits.length > 0) ? res.issuedUnits[0] : {};
    document.getElementById('receipt-issue-code').textContent = firstUnit.issueCode || res.issueCode || '—';
    document.getElementById('receipt-patient-name').textContent = res.patientName || firstUnit.patientName || '—';
    document.getElementById('receipt-hospital-name').textContent = res.hospitalName || firstUnit.hospitalName || '—';
    document.getElementById('receipt-blood-group').textContent = res.requestedBloodGroup || firstUnit.bloodGroup || '—';
    document.getElementById('receipt-allocated-unit').textContent = firstUnit.unitCode || res.bloodUnitCode || '—';
    document.getElementById('receipt-issue-date').textContent = UI.formatDate(firstUnit.issueDate || res.issueDate);
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
