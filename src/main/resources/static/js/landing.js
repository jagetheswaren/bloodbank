/**
 * BloodBank - Landing Page Interactive Logic
 * Sections 50, 52, 53, 54, 83, 100
 * Handles 3D hero perspective, scroll reveal, live KPI counters, and navbar states.
 */

document.addEventListener('DOMContentLoaded', () => {
  initNavbarScroll();
  initHeroPerspective();
  initScrollReveal();
  initLiveKpiData();
  initCtaTransitions();
  initMobileDrawer();
});

/**
 * 1. Navbar Scroll Transition
 */
function initNavbarScroll() {
  const nav = document.querySelector('.landing-nav');
  if (!nav) return;

  const handleScroll = () => {
    if (window.scrollY > 24) {
      nav.classList.add('is-scrolled');
    } else {
      nav.classList.remove('is-scrolled');
    }
  };

  window.addEventListener('scroll', handleScroll, { passive: true });
  handleScroll();
}

/**
 * 2. Pseudo-3D Hero Cursor Perspective & Parallax (Section 52)
 */
function initHeroPerspective() {
  const container = document.querySelector('.blood-core-viewport');
  const core = document.querySelector('.blood-core-3d');
  if (!container || !core) return;

  // Respect reduced motion and mobile touch
  const isReducedMotion = window.matchMedia('(prefers-reduced-motion: reduce)').matches;
  const isTouch = ('ontouchstart' in window) || (navigator.maxTouchPoints > 0);
  if (isReducedMotion || isTouch) return;

  let bounds = container.getBoundingClientRect();
  window.addEventListener('resize', () => {
    bounds = container.getBoundingClientRect();
  });

  container.addEventListener('mousemove', (e) => {
    const x = e.clientX - bounds.left - bounds.width / 2;
    const y = e.clientY - bounds.top - bounds.height / 2;

    const rotX = -(y / bounds.height) * 16;
    const rotY = (x / bounds.width) * 18;

    core.style.transform = `rotateX(${rotX}deg) rotateY(${rotY}deg) translateY(-4px)`;
  });

  container.addEventListener('mouseleave', () => {
    core.style.transform = '';
  });

  // Pause animation when tab is inactive to preserve CPU / battery
  document.addEventListener('visibilitychange', () => {
    if (document.hidden) {
      core.style.animationPlayState = 'paused';
    } else {
      core.style.animationPlayState = 'running';
    }
  });
}

/**
 * 3. Scroll Reveal via IntersectionObserver (Section 83)
 */
function initScrollReveal() {
  const revealElements = document.querySelectorAll('[data-reveal], [data-reveal-stagger]');
  if (!revealElements.length) return;

  const observer = new IntersectionObserver((entries, obs) => {
    entries.forEach(entry => {
      if (entry.isIntersecting) {
        entry.target.classList.add('is-revealed');
        // Animate any number counters inside this revealed container
        const counters = entry.target.querySelectorAll('[data-count]');
        counters.forEach(animateCounter);
        obs.unobserve(entry.target);
      }
    });
  }, {
    threshold: 0.15,
    rootMargin: '0px 0px -40px 0px'
  });

  revealElements.forEach(el => observer.observe(el));
}

/**
 * 4. Animated Number Counters
 */
function animateCounter(el) {
  const target = parseInt(el.getAttribute('data-count'), 10);
  if (isNaN(target)) return;

  const duration = 1200; // ms
  const frameRate = 1000 / 60;
  const totalFrames = Math.round(duration / frameRate);
  let frame = 0;

  const counterInterval = setInterval(() => {
    frame++;
    const progress = frame / totalFrames;
    // Ease out quartic
    const current = Math.round(target * (1 - Math.pow(1 - progress, 4)));
    el.textContent = current;

    if (frame >= totalFrames) {
      el.textContent = target;
      clearInterval(counterInterval);
    }
  }, frameRate);
}

/**
 * 5. Fetch Real API Data for Live Overview KPIs (Section 54, 102)
 */
async function initLiveKpiData() {
  try {
    // 1. Fetch live safe stock map
    const stockRes = await fetch('/api/inventory/stock');
    if (stockRes.ok) {
      const stockMap = await stockRes.json();
      let totalSafeUnits = 0;
      Object.values(stockMap).forEach(v => totalSafeUnits += (typeof v === 'number' ? v : 0));
      updateKpiElement('kpi-safe-units', totalSafeUnits);
    }

    // 2. Fetch inventory list for near-expiry and expired counts
    const invRes = await fetch('/api/inventory?size=100');
    if (invRes.ok) {
      const invData = await invRes.json();
      const units = invData.content || [];
      const nearExpiryCount = units.filter(u => u.status === 'NEAR_EXPIRY').length;
      const expiredCount = units.filter(u => u.status === 'EXPIRED').length;
      updateKpiElement('kpi-near-expiry', nearExpiryCount);
      updateKpiElement('kpi-expired', expiredCount);
    }

    // 3. Fetch active donors count
    const donorRes = await fetch('/api/donors?size=100&activeOnly=true');
    if (donorRes.ok) {
      const donorData = await donorRes.json();
      const activeDonors = donorData.totalElements || (donorData.content ? donorData.content.length : 0);
      updateKpiElement('kpi-active-donors', activeDonors);
    }

    // 4. Fetch recent issues count
    const issueRes = await fetch('/api/issues?size=100');
    if (issueRes.ok) {
      const issueData = await issueRes.json();
      const totalIssues = issueData.totalElements || (issueData.content ? issueData.content.length : 0);
      updateKpiElement('kpi-total-issues', totalIssues);
    }

  } catch (err) {
    console.warn('[Landing] Could not fetch live KPI metrics from API:', err);
  }
}

function updateKpiElement(id, value) {
  const el = document.getElementById(id);
  if (!el) return;
  el.setAttribute('data-count', value);
  // If already revealed or visible, trigger animation
  if (el.closest('.is-revealed') || window.getComputedStyle(el).opacity === '1') {
    animateCounter(el);
  } else {
    el.textContent = value;
  }
}

/**
 * 6. CTA Button Transition (Section 104)
 */
function initCtaTransitions() {
  const ctaButtons = document.querySelectorAll('.js-cta-dashboard');
  ctaButtons.forEach(btn => {
    btn.addEventListener('click', (e) => {
      const originalText = btn.textContent;
      btn.textContent = 'Opening Dashboard...';
      btn.style.opacity = '0.85';
    });
  });
}

/**
 * 7. Mobile Drawer Navigation for Landing Page
 */
function initMobileDrawer() {
  const hamburgerBtn = document.querySelector('.landing-hamburger-btn');
  const drawer = document.querySelector('.landing-mobile-drawer');
  const overlay = document.querySelector('.landing-mobile-overlay');
  const closeBtn = document.querySelector('.landing-mobile-drawer-close');
  const drawerLinks = document.querySelectorAll('.landing-mobile-links a');

  if (!hamburgerBtn || !drawer || !overlay) return;

  const openDrawer = () => {
    drawer.classList.add('open');
    overlay.classList.add('active');
    document.body.style.overflow = 'hidden';
  };

  const closeDrawer = () => {
    drawer.classList.remove('open');
    overlay.classList.remove('active');
    document.body.style.overflow = '';
  };

  hamburgerBtn.addEventListener('click', openDrawer);
  if (closeBtn) closeBtn.addEventListener('click', closeDrawer);
  overlay.addEventListener('click', closeDrawer);
  drawerLinks.forEach(link => link.addEventListener('click', closeDrawer));

  // Escape key closes drawer
  document.addEventListener('keydown', (e) => {
    if (e.key === 'Escape' && drawer.classList.contains('open')) {
      closeDrawer();
    }
  });
}
