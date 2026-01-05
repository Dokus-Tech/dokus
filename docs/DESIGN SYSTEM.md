DOKUS DESIGN SYSTEM — BASE (v1)

Positioning
Firstbase × Stripe × Tesla
Ledger-first. Quiet. Serious. Predictable.

If something looks “designed”, it’s probably wrong.

⸻

## Core principles
  1.	Structure over decoration
        Alignment, spacing, and hierarchy matter more than visual effects.
  2.	Tables are first-class citizens
        Documents, Forecast, and Cashflow are ledgers, not feeds.
  3.	Color is semantic
        Color communicates state, never decoration.
  4.	Silence by default
        No visual noise, no urgency unless legally required.

⸻

## Corner radius (locked)

Small, consistent, never expressive.

Allowed radius values
•	radius-xs → 2dp (rare: tiny elements, dividers)
•	radius-sm → 4dp (default)
•	radius-md → 6dp (inputs, buttons, modals)

Usage rules
•	Tables / rows → 0dp
•	Panels / surfaces → 4dp
•	Buttons → 6dp
•	Inputs → 6dp
•	Modals / sheets → 6dp

Large rounding, pills, or card bubbles are not allowed.

⸻

3. Elevation & shadows

Default: no shadows.
•	elevation-0 → everywhere by default
•	elevation-1 → modals only (very subtle)

If separation is needed, use borders, spacing, or background contrast.

⸻
## Color system

Light mode

Backgrounds
•	bg-app → #F8F9FB
•	bg-surface → #FFFFFF
•	bg-hover → #F1F5F9

Text
•	text-primary → #0F172A
•	text-secondary → #475569
•	text-muted → #94A3B8
•	text-disabled → #CBD5E1

Borders
•	border-default → #E2E8F0
•	border-subtle → #EDF2F7

⸻

Dark mode

Backgrounds
•	bg-app → #020617
•	bg-surface → #020617
•	bg-hover → #0B1220

Text
•	text-primary → #E5E7EB
•	text-secondary → #9CA3AF
•	text-muted → #6B7280
•	text-disabled → #4B5563

Borders
•	border-default → #1F2937

Dark mode must feel terminal-grade, not decorative.

⸻

5. Semantic colors (restricted)

Used only to communicate status.
•	status-processing → #64748B
•	status-confirmed → #16A34A
•	status-warning → #D97706
•	status-error → #B91C1C

Rules
•	No filled backgrounds by default
•	Use dot + text
•	One semantic color per row max

⸻

6. Typography

Single font family, multiple weights.

Hierarchy
•	Page title → Medium, 16–18sp
•	Section header → Medium, 14–15sp
•	Table header → Medium, 12–13sp
•	Table/body text → Regular, 13–14sp
•	Metadata → Regular + muted color

No oversized hero text. Alignment matters more than size.

⸻

7. Layout & spacing
   •	Max content width: 1200–1280px
   •	Centered layout, left-aligned content
   •	Spacing in 8dp increments
   •	Tables use tight vertical padding

Whitespace is clarity, not decoration.

⸻

8. Table / Ledger system

This is the core UI primitive in Dokus.

Rules
•	No cards
•	No row backgrounds
•	1px horizontal dividers
•	Subtle hover background
•	Full-row click target if interactive

Columns
•	Numeric values right-aligned
•	Dates aligned
•	Status column last

Grouping
•	Collapsible sections allowed
•	Headers act as dividers
•	No animations

⸻

9. Navigation

Menu principles
•	Flat by default
•	One level of nesting max
•	Text-first
•	Icons optional and subtle

Active state
•	Subtle background or thin left bar
•	No bright highlights

Disabled / Coming soon
•	Reduced opacity
•	Not clickable
•	Text suffix: · Coming soon

No badges, no locks, no hype.

⸻

10. Buttons & actions
    •	Primary actions are neutral
    •	Secondary actions are outline or text
    •	Destructive actions require confirmation

Shape
•	Radius: 6dp
•	No gradients
•	No elevation

⸻

11. Empty & processing states

Empty states
•	Calm text
•	No illustrations
•	No CTA spam

Example: “No documents yet.”

Processing
•	Inline, non-blocking
•	No full-screen loaders

⸻

12. Explicitly not allowed
    •	Card-heavy layouts
    •	Large rounded corners
    •	Bright accent colors
    •	Decorative icons
    •	Animated UI for delight
    •	“Friendly SaaS” tone

If it feels boring, it is correct.