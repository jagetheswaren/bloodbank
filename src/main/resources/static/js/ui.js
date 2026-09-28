/**
 * Blood Bank UI Utilities & Components
 * Toasts, Modals, Formatters, Skeletons, Drawer Management
 */

export const UI = {
  // Toast Notifications
  showToast(message, type = 'info', duration = 4000) {
    let container = document.getElementById('toast-container');
    if (!container) {
      container = document.createElement('div');
      container.id = 'toast-container';
      container.className = 'toast-container';
      document.body.appendChild(container);
    }

    const toast = document.createElement('div');
    toast.className = `toast toast-${type}`;

    let iconSvg = '';
    if (type === 'success') {
      iconSvg = '<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" style="color:var(--success)"><polyline points="20 6 9 17 4 12"></polyline></svg>';
    } else if (type === 'error') {
      iconSvg = '<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" style="color:var(--danger)"><circle cx="12" cy="12" r="10"></circle><line x1="15" y1="9" x2="9" y2="15"></line><line x1="9" y1="9" x2="15" y2="15"></line></svg>';
    } else if (type === 'warning') {
      iconSvg = '<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" style="color:var(--warning)"><path d="M10.29 3.86L1.82 18a2 2 0 0 0 1.71 3h16.94a2 2 0 0 0 1.71-3L13.71 3.86a2 2 0 0 0-3.42 0z"></path><line x1="12" y1="9" x2="12" y2="13"></line><line x1="12" y1="17" x2="12.01" y2="17"></line></svg>';
    } else {
      iconSvg = '<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" style="color:var(--info)"><circle cx="12" cy="12" r="10"></circle><line x1="12" y1="16" x2="12" y2="12"></line><line x1="12" y1="8" x2="12.01" y2="8"></line></svg>';
    }

    toast.innerHTML = `
      ${iconSvg}
      <div class="toast-message">${escapeHtml(message)}</div>
      <button class="toast-close" aria-label="Close">&times;</button>
    `;

    toast.querySelector('.toast-close').addEventListener('click', () => {
      toast.style.opacity = '0';
      toast.style.transform = 'translateY(8px)';
      setTimeout(() => toast.remove(), 200);
    });

    container.appendChild(toast);

    if (duration > 0) {
      setTimeout(() => {
        if (toast.parentElement) {
          toast.style.opacity = '0';
          toast.style.transform = 'translateY(8px)';
          setTimeout(() => toast.remove(), 200);
        }
      }, duration);
    }
  },

  // Modal Dialogs
  openModal(modalId) {
    const modal = document.getElementById(modalId);
    if (modal) {
      modal.classList.add('open');
      document.body.style.overflow = 'hidden';
    }
  },

  closeModal(modalId) {
    const modal = document.getElementById(modalId);
    if (modal) {
      modal.classList.remove('open');
      document.body.style.overflow = '';
    }
  },

  confirm(title, message, confirmBtnText = 'Confirm', isDanger = false) {
    return new Promise((resolve) => {
      let modal = document.getElementById('global-confirm-modal');
      if (!modal) {
        modal = document.createElement('div');
        modal.id = 'global-confirm-modal';
        modal.className = 'modal-overlay';
        modal.innerHTML = `
          <div class="modal-content">
            <div class="modal-header">
              <h3 class="modal-title" id="confirm-modal-title"></h3>
            </div>
            <div class="modal-body" id="confirm-modal-message"></div>
            <div class="modal-footer">
              <button class="btn btn-secondary" id="confirm-modal-cancel">Cancel</button>
              <button class="btn ${isDanger ? 'btn-danger' : 'btn-primary'}" id="confirm-modal-ok"></button>
            </div>
          </div>
        `;
        document.body.appendChild(modal);
      }

      document.getElementById('confirm-modal-title').textContent = title;
      document.getElementById('confirm-modal-message').textContent = message;
      const okBtn = document.getElementById('confirm-modal-ok');
      const cancelBtn = document.getElementById('confirm-modal-cancel');

      okBtn.textContent = confirmBtnText;
      okBtn.className = `btn ${isDanger ? 'btn-danger' : 'btn-primary'}`;

      modal.classList.add('open');
      document.body.style.overflow = 'hidden';

      const cleanup = () => {
        modal.classList.remove('open');
        document.body.style.overflow = '';
        okBtn.replaceWith(okBtn.cloneNode(true));
        cancelBtn.replaceWith(cancelBtn.cloneNode(true));
      };

      document.getElementById('confirm-modal-ok').addEventListener('click', () => {
        cleanup();
        resolve(true);
      });

      document.getElementById('confirm-modal-cancel').addEventListener('click', () => {
        cleanup();
        resolve(false);
      });
    });
  },

  // Button Loading State
  setButtonLoading(btn, loading, defaultText = '', loadingText = 'Processing...') {
    if (!btn) return;
    if (loading) {
      btn.dataset.originalText = btn.innerHTML;
      btn.disabled = true;
      btn.innerHTML = `<span class="spinner" style="display:inline-block;width:14px;height:14px;border:2px solid currentColor;border-right-color:transparent;border-radius:50%;animation:rotate 0.6s linear infinite;margin-right:6px;vertical-align:middle;"></span>${loadingText}`;
    } else {
      btn.disabled = false;
      btn.innerHTML = defaultText || btn.dataset.originalText || 'Submit';
    }
  },

  // Status Badge Formatter
  formatBadge(status) {
    if (!status) return '<span class="badge badge-inactive">Unknown</span>';
    const s = status.toUpperCase();
    if (s === 'AVAILABLE' || s === 'ACTIVE') {
      return `<span class="badge badge-available">${s}</span>`;
    } else if (s === 'NEAR_EXPIRY' || s === 'NEAR EXPIRY') {
      return `<span class="badge badge-near-expiry">Near Expiry</span>`;
    } else if (s === 'EXPIRED') {
      return `<span class="badge badge-expired">Expired</span>`;
    } else if (s === 'ISSUED') {
      return `<span class="badge badge-issued">Issued</span>`;
    } else if (s === 'DISCARDED') {
      return `<span class="badge badge-discarded">Discarded</span>`;
    } else if (s === 'INACTIVE') {
      return `<span class="badge badge-inactive">Inactive</span>`;
    }
    return `<span class="badge badge-inactive">${escapeHtml(status)}</span>`;
  },

  // Date Formatter
  formatDate(dateStr) {
    if (!dateStr) return '—';
    try {
      const d = new Date(dateStr);
      if (isNaN(d.getTime())) return dateStr;
      return d.toLocaleDateString('en-GB', {
        day: '2-digit',
        month: 'short',
        year: 'numeric'
      });
    } catch {
      return dateStr;
    }
  },

  // Table Skeleton Loader
  renderTableSkeleton(tbody, rows = 5, cols = 6) {
    if (!tbody) return;
    tbody.innerHTML = '';
    for (let r = 0; r < rows; r++) {
      const tr = document.createElement('tr');
      for (let c = 0; c < cols; c++) {
        const td = document.createElement('td');
        const skeleton = document.createElement('div');
        skeleton.className = 'skeleton';
        skeleton.style.height = '18px';
        skeleton.style.width = (c === 0 ? '60%' : c === cols - 1 ? '40%' : '80%');
        td.appendChild(skeleton);
        tr.appendChild(td);
      }
      tbody.appendChild(tr);
    }
  },

  // Mobile Drawer Setup
  initDrawer() {
    const toggleBtn = document.getElementById('menu-toggle');
    const sidebar = document.getElementById('app-sidebar');
    const overlay = document.getElementById('sidebar-overlay');

    if (toggleBtn && sidebar && overlay) {
      toggleBtn.addEventListener('click', () => {
        sidebar.classList.toggle('drawer-open');
        overlay.classList.toggle('active');
      });

      overlay.addEventListener('click', () => {
        sidebar.classList.remove('drawer-open');
        overlay.classList.remove('active');
      });
    }
  }
};

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

// Auto-init drawer when DOM loads
if (typeof document !== 'undefined') {
  document.addEventListener('DOMContentLoaded', () => {
    UI.initDrawer();
  });
}
