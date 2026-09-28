# BloodBank User Interface & Experience (UI/UX) Guide

## 1. Design Language & Philosophy

The Blood Bank Inventory & Donor Eligibility Tracker frontend is designed with a **human-centered, clinical, and reassuring aesthetic**. It intentionally departs from generic AI dashboard templates, heavy dark-mode crypto aesthetics, or neon gradients. Instead, it prioritizes:

- **Trust & Clinical Legibility**: Soft warm backgrounds with high-contrast text and purposeful medical accents.
- **Visual Restraint**: Primary burgundy (`#8B1528`) is applied thoughtfully for brand anchors, primary actions, and blood group typography.
- **Cognitive Clarity**: Information is structured in natural hierarchy—summary KPI cards at top, detailed tables below, and guided workflows for complex actions like eligibility checks and FEFO issuing.
- **Accessibility & Feedback**: Explicit status indicators (not color alone), accessible contrast ratios, and gentle micro-animations.

---

## 2. Color Palette & Semantic Tokens

| Token | Hex Value | Usage / Semantic Role |
| :--- | :--- | :--- |
| `--primary` | `#8B1528` | Deep clinical burgundy for brand anchors, primary CTA buttons, and blood group badges. |
| `--primary-hover` | `#721020` | Darker burgundy on hover states. |
| `--primary-subtle` | `#FDF2F4` | Very soft tint for active navigation items and callout backgrounds. |
| `--bg-page` | `#F8F9FA` | Warm off-white background ensuring calm reading comfort. |
| `--bg-surface` | `#FFFFFF` | Clean white card and modal containers. |
| `--bg-subtle` | `#F1F5F9` | Slate-tinted neutral for table headers and inactive controls. |
| `--text-primary` | `#1E293B` | Deep slate-charcoal for headings and primary content. |
| `--text-secondary` | `#475569` | Mid-tone slate for supporting descriptions and metadata. |
| `--status-available` | `#15803D` | Forest green for verified usable inventory stock and eligible donors. |
| `--status-near-expiry` | `#B45309` | Amber for units within the 7-day near-expiry quarantine window. |
| `--status-expired` | `#B91C1C` | Crimson red for units exceeding the 42-day lifespan. |
| `--status-issued` | `#0369A1` | Ocean blue for completed patient transfusion allocations. |

---

## 3. Typography & Hierarchy

The interface employs the standard system UI font stack:
```css
font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Helvetica Neue", Arial, sans-serif;
```
For tracking codes, timestamps, and database IDs, a clean monospace stack is used:
```css
font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
```

### Hierarchy
- **Page Titles**: `1.65rem` (`26px`), 700 weight, deep slate.
- **Section Headers**: `1.05rem` (`17px`), 600 weight.
- **KPI Values**: `1.85rem` (`30px`), 700 weight, tabular figures.
- **Data Tables**: `0.88rem` (`14px`), 400–600 weight.
- **Microcopy & Metadata**: `0.75rem` (`12px`), 500 weight, muted slate.

---

## 4. Layout Architecture

- **App Shell**: Fluid desktop layout featuring a fixed 260px sidebar and sticky topbar.
- **Responsive Drawer**: At screen widths `< 860px`, the sidebar smoothly transforms into an accessible off-canvas drawer with an interactive backdrop overlay.
- **Page Container**: Constrained to `--max-content-width: 1360px` with responsive padding ensuring readability on widescreen displays.
- **8-Blood Group Grid**: Responsive CSS grid displaying A+, A-, B+, B-, AB+, AB-, O+, O- with real-time stock levels and visual capacity indicators.

---

## 5. Components & Interactions

### A. Guided Donation Workflow
Rather than confronting the operator with a raw form, the **Record Donation** page implements a 2-step guided interaction:
1. **Donor Selection & Instant Eligibility Evaluation**: Immediately queries `/api/donors/{id}/eligibility` and renders a prominent green or amber banner detailing the 90-day recovery gap.
2. **Collection Inputs (Guarded)**: The unit selection and collection date fields only unlock if the donor is clinically eligible.
3. **Transaction Receipt**: On submission, an animated receipt card displays the donation code, donor name, and the generated BloodUnit codes with their calculated 42-day expiration dates.

### B. FEFO Allocation Form
The **Issue Blood** interface features:
- An educational banner explaining First-Expire, First-Out rules.
- Real-time stock querying that updates whenever a blood group is selected (e.g. *"4 safe units available for O+"* vs *"0 safe units available"*).
- Instant feedback and single-unit allocation enforcement.

### C. Feedback Systems
- **Toast Notifications**: Lightweight, auto-dismissing feedback messages for successful operations and persistent banners for error states.
- **Table Skeletons**: Soft shimmer skeleton loaders (1.5s linear gradient) eliminate jarring content shifts during asynchronous API fetches.
- **Confirm Modals**: Custom accessible modal dialogs replace native browser `confirm()` popups for critical actions like donor deactivation.

---

## 6. Responsive Breakpoints

| Breakpoint | Target Devices | Adaptations |
| :--- | :--- | :--- |
| `> 1024px` | Desktop & Laptops | Full 260px sidebar, 4-column blood stock grid, multi-column forms. |
| `860px - 1024px` | Tablets Landscape | 2-column blood stock grid, adapted KPI cards. |
| `< 860px` | Tablets Portrait & Mobile | Sidebar shifts to drawer with hamburger menu button; KPI cards stack into 2 columns. |
| `< 600px` | Smartphones | Blood stock tiles stack to 1 column; filter bars and forms stack vertically. |

---

## 7. Animation Principles

All animations respect user preferences:
```css
@media (prefers-reduced-motion: reduce) {
  *, *::before, *::after {
    animation-duration: 0.01ms !important;
    transition-duration: 0.01ms !important;
  }
}
```

- **Entrance Transitions**: `fadeIn` (250ms) and `slideInUp` (300ms) with `cubic-bezier(0.16, 1, 0.3, 1)`.
- **Card Hover Elevation**: `translateY(-2px)` with subtle shadow deepening.
- **Shimmer Skeletons**: 1.5s horizontal shimmer cycle.
- **No Distractions**: No floating particles, bouncing icons, or continuous ambient animations.

---

## 8. Page Routing & API Integration

| Frontend Route | Template | Primary API Endpoints | Controller / Service |
| :--- | :--- | :--- | :--- |
| `/` | `index.html` | Static architectural overview | `WebViewController.index()` |
| `/dashboard` | `dashboard.html` | `GET /api/inventory/stock`<br>`GET /api/donors`<br>`GET /api/inventory/near-expiry`<br>`GET /api/inventory/expired`<br>`GET /api/issues`<br>`GET /api/donations` | `InventoryService`<br>`DonorService`<br>`DonationService`<br>`IssueService` |
| `/donors` | `donors.html` | `GET /api/donors`<br>`GET /api/donors/{id}/eligibility`<br>`DELETE /api/donors/{id}` | `DonorController`<br>`DonorService` |
| `/donors/new` | `donor-form.html` | `POST /api/donors` | `DonorController`<br>`DonorService` |
| `/donors/{id}/view`| `donor-details.html`| `GET /api/donors/{id}`<br>`GET /api/donors/{id}/eligibility`<br>`GET /api/donations/donor/{id}` | `DonorController`<br>`DonationController` |
| `/donations` | `donations.html` | `GET /api/donations` | `DonationController`<br>`DonationService` |
| `/donations/new` | `donation-form.html` | `GET /api/donors?active=true`<br>`GET /api/donors/{id}/eligibility`<br>`POST /api/donations` | `DonorService`<br>`DonationService` |
| `/inventory` | `inventory.html` | `GET /api/inventory` | `InventoryController`<br>`InventoryService` |
| `/inventory/near-expiry` | `near-expiry.html` | `GET /api/inventory/near-expiry` | `InventoryController`<br>`InventoryService` |
| `/inventory/expired` | `expired.html` | `GET /api/inventory/expired` | `InventoryController`<br>`InventoryService` |
| `/issues` | `issues.html` | `GET /api/issues` | `IssueController`<br>`IssueService` |
| `/issues/new` | `issue-blood.html` | `GET /api/inventory/stock`<br>`POST /api/issues` | `InventoryService`<br>`IssueService` |
| `/about` | `about.html` | Technical documentation view | `WebViewController.about()` |
| `/swagger-ui/index.html` | Swagger UI | `GET /v3/api-docs` | Springdoc OpenAPI |
