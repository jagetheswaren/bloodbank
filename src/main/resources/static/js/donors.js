/**
 * Donors Module
 * Listing, Search, Registration, Eligibility Check & Profiles
 */

import { BloodBankApi } from './api.js';
import { UI } from './ui.js';

document.addEventListener('DOMContentLoaded', () => {
  const donorsTableBody = document.getElementById('donors-tbody');
  const donorForm = document.getElementById('donor-register-form');
  const donorProfileEl = document.getElementById('donor-profile-container');

  if (donorsTableBody) {
    initDonorsList();
  } else if (donorForm) {
    initDonorForm();
  } else if (donorProfileEl) {
    initDonorProfile();
  }
});

// --- Donors List Page ---
let currentDonors = [];

async function initDonorsList() {
  const tbody = document.getElementById('donors-tbody');
  const searchInput = document.getElementById('donor-search-input');
  const bloodGroupFilter = document.getElementById('donor-bg-filter');

  await loadDonors();

  if (searchInput) {
    searchInput.addEventListener('input', () => filterAndRenderDonors());
  }

  if (bloodGroupFilter) {
    bloodGroupFilter.addEventListener('change', () => filterAndRenderDonors());
  }
}

async function loadDonors() {
  const tbody = document.getElementById('donors-tbody');
  UI.renderTableSkeleton(tbody, 5, 7);

  try {
    const page = await BloodBankApi.getDonors(0, 50);
    currentDonors = page.content || [];
    filterAndRenderDonors();
  } catch (error) {
    console.error('Failed to load donors:', error);
    tbody.innerHTML = `<tr><td colspan="7" style="text-align:center;color:var(--danger);padding:2rem;">Failed to load donors: ${escapeHtml(error.message)}</td></tr>`;
  }
}

function filterAndRenderDonors() {
  const tbody = document.getElementById('donors-tbody');
  const search = (document.getElementById('donor-search-input')?.value || '').trim().toLowerCase();
  const bgFilter = document.getElementById('donor-bg-filter')?.value || '';

  let filtered = currentDonors;

  if (bgFilter) {
    filtered = filtered.filter(d => d.bloodGroup === bgFilter);
  }

  if (search) {
    filtered = filtered.filter(d =>
      (d.name && d.name.toLowerCase().includes(search)) ||
      (d.donorCode && d.donorCode.toLowerCase().includes(search)) ||
      (d.email && d.email.toLowerCase().includes(search)) ||
      (d.phone && d.phone.includes(search))
    );
  }

  if (filtered.length === 0) {
    tbody.innerHTML = `
      <tr>
        <td colspan="7">
          <div class="empty-state">
            <svg class="empty-state-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
              <path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"></path>
              <circle cx="9" cy="7" r="4"></circle>
              <path d="M23 21v-2a4 4 0 0 0-3-3.87"></path>
              <path d="M16 3.13a4 4 0 0 1 0 7.75"></path>
            </svg>
            <h3 class="empty-state-title">No donors found</h3>
            <p class="empty-state-text">No donors match your search or filter criteria.</p>
            <a href="/donors/new" class="btn btn-primary btn-sm">Register First Donor</a>
          </div>
        </td>
      </tr>
    `;
    return;
  }

  tbody.innerHTML = filtered.map(d => `
    <tr>
      <td><span class="font-mono-code">${escapeHtml(d.donorCode)}</span></td>
      <td><strong>${escapeHtml(d.name)}</strong></td>
      <td><span style="font-weight:700;color:var(--primary);">${escapeHtml(d.bloodGroup)}</span></td>
      <td>${escapeHtml(d.email)}</td>
      <td>${escapeHtml(d.phone)}</td>
      <td>${d.active ? '<span class="badge badge-available">Active</span>' : '<span class="badge badge-inactive">Inactive</span>'}</td>
      <td>
        <div style="display:flex;gap:0.4rem;">
          <a href="/donors/${d.id}/view" class="btn btn-secondary btn-sm" title="View Profile">Profile</a>
          <button class="btn btn-ghost btn-sm check-eligibility-btn" data-id="${d.id}" data-name="${escapeHtml(d.name)}" title="Check Eligibility">Check</button>
          ${d.active ? `<button class="btn btn-ghost btn-sm deactivate-donor-btn" data-id="${d.id}" data-name="${escapeHtml(d.name)}" style="color:var(--danger)" title="Deactivate">Deactivate</button>` : ''}
        </div>
      </td>
    </tr>
  `).join('');

  // Attach event handlers
  tbody.querySelectorAll('.check-eligibility-btn').forEach(btn => {
    btn.addEventListener('click', () => showEligibilityModal(btn.dataset.id, btn.dataset.name));
  });

  tbody.querySelectorAll('.deactivate-donor-btn').forEach(btn => {
    btn.addEventListener('click', () => handleDeactivateDonor(btn.dataset.id, btn.dataset.name));
  });
}

async function showEligibilityModal(donorId, donorName) {
  try {
    const el = await BloodBankApi.checkEligibility(donorId);
    const isEligible = el.eligible;

    const content = `
      <div class="eligibility-banner ${isEligible ? 'eligible' : 'not-eligible'}" style="margin-bottom:0;">
        <div class="eligibility-icon-wrap">
          ${isEligible
            ? '<svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><polyline points="20 6 9 17 4 12"></polyline></svg>'
            : '<svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><circle cx="12" cy="12" r="10"></circle><line x1="12" y1="8" x2="12" y2="12"></line><line x1="12" y1="16" x2="12.01" y2="16"></line></svg>'}
        </div>
        <div>
          <h4 class="eligibility-title">${isEligible ? 'Eligible to Donate' : 'Not Currently Eligible'}</h4>
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
              <span class="metric-label">Remaining Gap</span>
              <span class="metric-value" style="color:var(--warning);">${el.remainingDays} days</span>
            </div>` : ''}
          </div>
        </div>
      </div>
    `;

    await UI.confirm(`Eligibility: ${donorName}`, content, 'Close', false);
  } catch (err) {
    UI.showToast(`Eligibility check failed: ${err.message}`, 'error');
  }
}

async function handleDeactivateDonor(donorId, donorName) {
  const confirmed = await UI.confirm(
    'Deactivate Donor?',
    `Are you sure you want to deactivate ${donorName}? Their historical donation and blood unit records will be preserved safely.`,
    'Deactivate',
    true
  );

  if (!confirmed) return;

  try {
    await BloodBankApi.deactivateDonor(donorId);
    UI.showToast(`Donor ${donorName} deactivated safely.`, 'success');
    await loadDonors();
  } catch (err) {
    UI.showToast(`Failed to deactivate donor: ${err.message}`, 'error');
  }
}

// --- Donor Registration Form ---
function initDonorForm() {
  const form = document.getElementById('donor-register-form');
  const submitBtn = document.getElementById('submit-donor-btn');

  form.addEventListener('submit', async (e) => {
    e.preventDefault();

    // Reset error states
    form.querySelectorAll('.form-group').forEach(fg => fg.classList.remove('has-error'));

    const formData = {
      name: form.elements['name'].value.trim(),
      bloodGroup: form.elements['bloodGroup'].value,
      email: form.elements['email'].value.trim(),
      phone: form.elements['phone'].value.trim(),
      dateOfBirth: form.elements['dateOfBirth'].value,
      gender: form.elements['gender'].value,
      address: form.elements['address'].value.trim()
    };

    // Client-side validations
    let hasError = false;
    if (!formData.name) {
      showFieldError('name', 'Full name is required.');
      hasError = true;
    }
    if (!formData.bloodGroup) {
      showFieldError('bloodGroup', 'Please select a blood group.');
      hasError = true;
    }
    if (!formData.email || !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(formData.email)) {
      showFieldError('email', 'Please provide a valid email address.');
      hasError = true;
    }
    if (!formData.phone || !/^\+?[0-9]{7,15}$/.test(formData.phone)) {
      showFieldError('phone', 'Please provide a valid phone number (digits only, e.g. 9876543210).');
      hasError = true;
    }
    if (!formData.dateOfBirth) {
      showFieldError('dateOfBirth', 'Date of birth is required.');
      hasError = true;
    }

    if (hasError) return;

    UI.setButtonLoading(submitBtn, true, 'Register Donor', 'Registering...');

    try {
      const created = await BloodBankApi.createDonor(formData);
      UI.showToast(`Donor ${created.name} registered successfully with code ${created.donorCode}!`, 'success');
      setTimeout(() => {
        window.location.href = `/donors/${created.id}/view`;
      }, 700);
    } catch (err) {
      UI.setButtonLoading(submitBtn, false, 'Register Donor');
      UI.showToast(`Registration failed: ${err.message}`, 'error');
    }
  });
}

function showFieldError(fieldName, msg) {
  const input = document.querySelector(`[name="${fieldName}"]`);
  if (input) {
    const group = input.closest('.form-group');
    if (group) {
      group.classList.add('has-error');
      const errEl = group.querySelector('.form-error');
      if (errEl) errEl.textContent = msg;
    }
  }
}

// --- Donor Profile View ---
async function initDonorProfile() {
  const container = document.getElementById('donor-profile-container');
  const donorId = container.dataset.donorId;
  if (!donorId) return;

  try {
    const [donor, eligibility, donations] = await Promise.all([
      BloodBankApi.getDonor(donorId),
      BloodBankApi.checkEligibility(donorId),
      BloodBankApi.getDonationsByDonor(donorId).catch(() => [])
    ]);

    renderDonorProfile(donor, eligibility, donations);
  } catch (err) {
    container.innerHTML = `<div class="alert alert-danger">Failed to load donor profile: ${escapeHtml(err.message)}</div>`;
  }
}

function renderDonorProfile(donor, el, donations) {
  // Donor Header
  document.getElementById('profile-donor-name').textContent = donor.name;
  document.getElementById('profile-donor-code').textContent = donor.donorCode;
  document.getElementById('profile-blood-group').textContent = donor.bloodGroup;
  document.getElementById('profile-status').innerHTML = donor.active ? '<span class="badge badge-available">Active</span>' : '<span class="badge badge-inactive">Inactive</span>';

  // Meta Fields
  document.getElementById('profile-email').textContent = donor.email;
  document.getElementById('profile-phone').textContent = donor.phone;
  document.getElementById('profile-dob').textContent = UI.formatDate(donor.dateOfBirth);
  document.getElementById('profile-gender').textContent = donor.gender || '—';
  document.getElementById('profile-address').textContent = donor.address || '—';

  // Eligibility Panel
  const eligContainer = document.getElementById('profile-eligibility-panel');
  const isEligible = el.eligible;
  eligContainer.innerHTML = `
    <div class="eligibility-banner ${isEligible ? 'eligible' : 'not-eligible'}">
      <div class="eligibility-icon-wrap">
        ${isEligible
          ? '<svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><polyline points="20 6 9 17 4 12"></polyline></svg>'
          : '<svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><circle cx="12" cy="12" r="10"></circle><line x1="12" y1="8" x2="12" y2="12"></line><line x1="12" y1="16" x2="12.01" y2="16"></line></svg>'}
      </div>
      <div>
        <h3 class="eligibility-title">${isEligible ? 'Eligible to Donate Blood' : 'Not Currently Eligible'}</h3>
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
            <span class="metric-label">Remaining Gap</span>
            <span class="metric-value" style="color:var(--warning);">${el.remainingDays} days</span>
          </div>` : ''}
        </div>
      </div>
    </div>
  `;

  // Action buttons
  const donateBtn = document.getElementById('profile-donate-btn');
  if (donateBtn) {
    if (isEligible && donor.active) {
      donateBtn.href = `/donations/new?donorId=${donor.id}`;
      donateBtn.classList.remove('disabled');
    } else {
      donateBtn.classList.add('disabled');
      donateBtn.title = isEligible ? 'Donor is inactive' : 'Donor is not currently eligible';
    }
  }

  // Donations Table
  const tbody = document.getElementById('profile-donations-tbody');
  if (tbody) {
    if (donations.length === 0) {
      tbody.innerHTML = `<tr><td colspan="5" style="text-align:center;color:var(--text-muted);padding:1.5rem;">No historical donations recorded for this donor.</td></tr>`;
    } else {
      tbody.innerHTML = donations.map(d => `
        <tr>
          <td><span class="font-mono-code">${escapeHtml(d.donationCode)}</span></td>
          <td>${UI.formatDate(d.donationDate)}</td>
          <td>${escapeHtml(d.unitsCreated || 1)} unit(s)</td>
          <td>${d.bloodUnits && d.bloodUnits.length ? d.bloodUnits.map(u => `<span class="font-mono-code" style="margin-right:4px;">${escapeHtml(u.unitCode)}</span>`).join('') : '—'}</td>
          <td>${escapeHtml(d.notes || '—')}</td>
        </tr>
      `).join('');
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
