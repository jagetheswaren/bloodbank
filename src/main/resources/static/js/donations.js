/**
 * Donations Module
 * Guided Donation Workflow with 90-day Eligibility Guard & Donations Listing
 */

import { BloodBankApi } from './api.js';
import { UI } from './ui.js';

document.addEventListener('DOMContentLoaded', () => {
  const donationsTableBody = document.getElementById('donations-tbody');
  const recordDonationWorkflow = document.getElementById('record-donation-workflow');

  if (donationsTableBody) {
    loadDonationsList();
  } else if (recordDonationWorkflow) {
    initRecordDonationWorkflow();
  }
});

// --- Donations Listing Page ---
async function loadDonationsList() {
  const tbody = document.getElementById('donations-tbody');
  UI.renderTableSkeleton(tbody, 5, 6);

  try {
    const page = await BloodBankApi.getDonations(0, 50);
    const donations = page.content || [];

    if (donations.length === 0) {
      tbody.innerHTML = `
        <tr>
          <td colspan="6">
            <div class="empty-state">
              <svg class="empty-state-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
                <path d="M12 2v6m0 0l-3-3m3 3l3-3"></path>
                <path d="M20.88 18.09A5 5 0 0 0 18 9h-1.26A8 8 0 1 0 3 16.29"></path>
              </svg>
              <h3 class="empty-state-title">No donations recorded yet</h3>
              <p class="empty-state-text">Record an eligible donor's donation to generate blood units and populate inventory.</p>
              <a href="/donations/new" class="btn btn-primary btn-sm">Record First Donation</a>
            </div>
          </td>
        </tr>
      `;
      return;
    }

    tbody.innerHTML = donations.map(d => `
      <tr>
        <td><span class="font-mono-code">${escapeHtml(d.donationCode)}</span></td>
        <td>
          <strong>${escapeHtml(d.donorName || '—')}</strong>
          ${d.donorId ? `<br><small style="color:var(--text-muted);"><a href="/donors/${d.donorId}/view">View Profile</a></small>` : ''}
        </td>
        <td><span style="font-weight:700;color:var(--primary);">${escapeHtml(d.bloodGroup)}</span></td>
        <td>${escapeHtml(d.unitsCreated || d.units || 1)} unit(s)</td>
        <td>
          ${d.bloodUnits && d.bloodUnits.length
            ? d.bloodUnits.map(u => `<span class="font-mono-code" style="margin-right:4px;">${escapeHtml(u.unitCode)}</span>`).join('')
            : '<span style="color:var(--text-muted);">—</span>'}
        </td>
        <td>${UI.formatDate(d.donationDate)}</td>
      </tr>
    `).join('');
  } catch (error) {
    console.error('Failed to load donations:', error);
    tbody.innerHTML = `<tr><td colspan="6" style="text-align:center;color:var(--danger);padding:2rem;">Failed to load donations: ${escapeHtml(error.message)}</td></tr>`;
  }
}

// --- Guided Donation Workflow ---
async function initRecordDonationWorkflow() {
  const donorSelect = document.getElementById('donation-donor-select');
  const eligibilityBannerContainer = document.getElementById('eligibility-check-result');
  const donationDetailsSection = document.getElementById('donation-details-section');
  const donationReceiptSection = document.getElementById('donation-receipt-section');
  const donationForm = document.getElementById('donation-form');
  const submitBtn = document.getElementById('submit-donation-btn');

  // Check URL parameters for preselected donor
  const urlParams = new URLSearchParams(window.location.search);
  const preselectedDonorId = urlParams.get('donorId');

  // Populate donor select options
  try {
    const donorsPage = await BloodBankApi.getDonors(0, 100, true);
    const donors = donorsPage.content || [];

    donorSelect.innerHTML = '<option value="">-- Choose an active donor --</option>' +
      donors.map(d => `<option value="${d.id}" data-bg="${d.bloodGroup}" ${preselectedDonorId && preselectedDonorId == d.id ? 'selected' : ''}>${escapeHtml(d.name)} (${escapeHtml(d.donorCode)}) - ${d.bloodGroup}</option>`).join('');

    if (preselectedDonorId) {
      await handleDonorSelected(preselectedDonorId);
    }
  } catch (err) {
    console.error('Failed to load active donors:', err);
    donorSelect.innerHTML = '<option value="">Error loading donors</option>';
  }

  donorSelect.addEventListener('change', async () => {
    const donorId = donorSelect.value;
    if (!donorId) {
      eligibilityBannerContainer.innerHTML = '';
      donationDetailsSection.style.display = 'none';
      return;
    }
    await handleDonorSelected(donorId);
  });

  async function handleDonorSelected(donorId) {
    eligibilityBannerContainer.innerHTML = '<div class="skeleton" style="height:80px;border-radius:var(--radius-lg);margin-bottom:1.5rem;"></div>';
    donationDetailsSection.style.display = 'none';

    try {
      const el = await BloodBankApi.checkEligibility(donorId);
      const isEligible = el.eligible;

      eligibilityBannerContainer.innerHTML = `
        <div class="eligibility-banner ${isEligible ? 'eligible' : 'not-eligible'}">
          <div class="eligibility-icon-wrap">
            ${isEligible
              ? '<svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><polyline points="20 6 9 17 4 12"></polyline></svg>'
              : '<svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><circle cx="12" cy="12" r="10"></circle><line x1="12" y1="8" x2="12" y2="12"></line><line x1="12" y1="16" x2="12.01" y2="16"></line></svg>'}
          </div>
          <div>
            <h4 class="eligibility-title">${isEligible ? 'Donor is Eligible to Donate' : 'Donor is Not Currently Eligible'}</h4>
            <p class="eligibility-message">${escapeHtml(el.message)}</p>
            <div class="eligibility-metrics">
              <div class="eligibility-metric-item">
                <span class="metric-label">Last Donation</span>
                <span class="metric-value">${UI.formatDate(el.lastDonationDate)}</span>
              </div>
              <div class="eligibility-metric-item">
                <span class="metric-label">Next Eligible Date</span>
                <span class="metric-value">${UI.formatDate(el.nextEligibleDate)}</span>
              </div>
              ${!isEligible ? `
              <div class="eligibility-metric-item">
                <span class="metric-label">Remaining Mandatory Gap</span>
                <span class="metric-value" style="color:var(--warning);">${el.remainingDays} days</span>
              </div>` : ''}
            </div>
          </div>
        </div>
      `;

      if (isEligible) {
        donationDetailsSection.style.display = 'block';
        donationDetailsSection.scrollIntoView({ behavior: 'smooth', block: 'nearest' });
      } else {
        donationDetailsSection.style.display = 'none';
        UI.showToast(`Eligibility rule: ${el.message}`, 'warning', 6000);
      }
    } catch (err) {
      eligibilityBannerContainer.innerHTML = `<div class="alert alert-danger">Eligibility check error: ${escapeHtml(err.message)}</div>`;
    }
  }

  // Handle donation form submission
  donationForm.addEventListener('submit', async (e) => {
    e.preventDefault();

    const donorId = donorSelect.value;
    const units = parseInt(donationForm.elements['units'].value, 10);
    const donationDate = donationForm.elements['donationDate'].value;
    const notes = donationForm.elements['notes'].value.trim();

    if (!donorId) {
      UI.showToast('Please select an eligible donor.', 'warning');
      return;
    }

    const payload = {
      donorId: Number(donorId),
      numberOfUnits: units || 1,
      donationDate: donationDate,
      notes: notes || 'Routine clinical donation'
    };

    UI.setButtonLoading(submitBtn, true, 'Confirm & Record Donation', 'Processing Donation...');

    try {
      const response = await BloodBankApi.createDonation(payload);
      UI.showToast('Donation accepted and blood units generated!', 'success');

      // Hide form & steps, show receipt
      document.getElementById('donation-steps-wrap').style.display = 'none';
      donationReceiptSection.style.display = 'block';

      renderDonationReceipt(response);
    } catch (err) {
      UI.setButtonLoading(submitBtn, false, 'Confirm & Record Donation');
      UI.showToast(`Donation rejected: ${err.message}`, 'error', 7000);
    }
  });

  function renderDonationReceipt(res) {
    document.getElementById('receipt-donation-code').textContent = res.donationCode;
    document.getElementById('receipt-donor-name').textContent = res.donorName || '—';
    document.getElementById('receipt-blood-group').textContent = res.bloodGroup;
    document.getElementById('receipt-units-count').textContent = `${res.unitsCreated || res.units} unit(s)`;
    document.getElementById('receipt-collection-date').textContent = UI.formatDate(res.donationDate);

    const unitsList = document.getElementById('receipt-units-list');
    if (res.bloodUnits && res.bloodUnits.length > 0) {
      unitsList.innerHTML = res.bloodUnits.map(u => `
        <div style="background:var(--bg-surface);padding:0.75rem 1rem;border-radius:var(--radius-sm);border:1px solid var(--border-light);margin-bottom:0.5rem;display:flex;justify-content:space-between;align-items:center;">
          <div>
            <span class="font-mono-code">${escapeHtml(u.unitCode)}</span>
            <span style="font-weight:700;color:var(--primary);margin-left:0.5rem;">${escapeHtml(u.bloodGroup)}</span>
          </div>
          <div style="font-size:0.8rem;color:var(--text-muted);">
            Expires: <strong>${UI.formatDate(u.expiryDate)}</strong> (42-day shelf life)
          </div>
        </div>
      `).join('');
    } else {
      unitsList.innerHTML = '<p style="color:var(--text-muted);font-size:0.85rem;">Blood units registered to inventory.</p>';
    }
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
