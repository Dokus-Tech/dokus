
---

**Revolut structure × Perplexity calm × Belgian reliability**
  

**Version:** 1.0

**Status:** Canonical source of truth

**Audience:** Product, design, frontend (CMP), contributors

---

## **0. DESIGN MISSION (NON-NEGOTIABLE)**

  

Dokus is **not**:

- an accounting system
    
- a finance toy
    
- an AI demo
    

  

Dokus **is**:

  

> **A financial early-warning system for small Belgian businesses.**

  

Design must:

- Reduce anxiety
    
- Surface risk early
    
- Respect uncertainty
    
- Remain calm under pressure
    
- Feel boring in a reassuring way
    

  

If a screen looks impressive but slightly stressful → it is wrong.

---

## **1. CORE DESIGN PRINCIPLES**

1. **Clarity over decoration**
    
2. **Hierarchy over density**
    
3. **Structure over cleverness**
    
4. **Brand as accent, not paint**
    
5. **Calm beats cool**
    

---

## **2. TYPOGRAPHY**

  

### **Primary Typeface**

  

**Inter (Variable)**

  

Reasons:

- Neutral tone
    
- Excellent numerals
    
- Tables & finance friendly
    
- Industry standard (Stripe, Revolut, Linear)
    

  

### **Allowed Weights**

- 400 (Regular)
    
- 500 (Medium)
    
- 600 (Semibold)
    

  

❌ Avoid ≥700 except hero numbers.

---

### **Type Scale**

|**Token**|**Size**|**Weight**|**Usage**|
|---|---|---|---|
|displayLarge|48|600|Onboarding / empty states|
|headlineLarge|32|600|Page titles|
|headlineMedium|24|600|Section headers|
|titleLarge|20|500|Card titles|
|titleMedium|16|500|List titles|
|bodyLarge|16|400|Default body|
|bodyMedium|14|400|Secondary text|
|labelLarge|14|500|Buttons|
|labelMedium|12|500|Metadata|
|labelSmall|11|500|Captions|

Line height:

- Body: **1.35**
    
- Titles: **1.2**
    

---

## **3. BRAND COLOR (SIGNATURE ONLY)**

  

### **Dokus Gold**

```
HEX: #D4AF37
ARGB: 0xFFD4AF37
```

### **Rules**

- ❌ Never primary button
    
- ❌ Never chart color
    
- ❌ Never money values
    
- ❌ Never navigation highlight
    
- ❌ Never background fill
    

  

Allowed only as:

- 1–2dp accent border
    
- Small icon tint
    
- “Dokus insight” marker
    
- Rare divider highlight
    

  

> Gold is **jewelry**, not wallpaper.

---

## **4. MATERIAL 3 COLOR SYSTEM**

  

### **Light Theme**

```
primary              = #3B82F6
onPrimary            = #FFFFFF

primaryContainer     = #E8F0FF
onPrimaryContainer   = #0B1B3F

secondary            = #64748B
onSecondary          = #FFFFFF

background           = #F8FAFC
onBackground         = #0F172A

surface              = #FFFFFF
onSurface            = #0F172A

surfaceVariant       = #F1F5F9
onSurfaceVariant     = #334155

outline              = #CBD5E1
outlineVariant       = #E2E8F0

error                = #DC2626
onError              = #FFFFFF
errorContainer       = #FEE2E2
onErrorContainer     = #7F1D1D
```

### **Dark Theme (Professional)**

```
primary              = #60A5FA
onPrimary            = #020617

primaryContainer     = #1E3A8A
onPrimaryContainer   = #DBEAFE

secondary            = #94A3B8
onSecondary          = #020617

background           = #020617
onBackground         = #E5E7EB

surface              = #020617
onSurface            = #E5E7EB

surfaceVariant       = #0F172A
onSurfaceVariant     = #CBD5E1

outline              = #334155
outlineVariant       = #1E293B

error                = #F87171
onError              = #020617
errorContainer       = #7F1D1D
onErrorContainer     = #FEE2E2
```

---

## **5. LAYOUT SYSTEM**

  

### **Desktop (≥1200px)**

```
[ Navigation ] [ Main Content ] [ Context Panel ]
```

- Navigation: persistent sidebar
    
- Main: lists, dashboards
    
- Context panel: details, guidance, AI
    

  

### **Mobile**

- Bottom navigation
    
- Single column
    
- Same components, lower density
    

  

❌ Desktop must never be “scaled mobile”.

---

## **6. SURFACE SYSTEM (CRITICAL)**

  

Dokus has **three surface types**.

Nothing else is allowed.

---

### **6.1 BASE CARD (DEFAULT WORKHORSE)**

  

**Used for ~80% of UI**

  

Purpose:

- Host data
    
- Anchor structure
    
- Create predictable rhythm
    

  

#### **Light theme**

- Background: surface
    
- Opacity: 100%
    
- Border: 1dp outlineVariant
    
- Radius: **12dp**
    
- Elevation: **1**
    
- Shadow: soft, wide, low opacity
    

  

#### **Dark theme**

- Background: surfaceVariant
    
- Opacity: 100%
    
- Border: 1dp outlineVariant
    
- Radius: **12dp**
    
- Elevation: **0**
    
- Shadow: none
    

---

### **6.2 SOFT CARD (MICRO-GLASS VARIANT)**

  

Used sparingly:

- Dashboard hero areas
    
- Transitional sections
    
- Calm emphasis
    

  

Properties:

- Background: surface
    
- Opacity: **96–98%**
    
- Blur: **none**
    
- Border: 1dp outlineVariant
    
- Radius: **12dp**
    
- Elevation: **1**
    

  

This is **not frosted glass**.

It must remain printable.

---

### **6.3 GLASS SURFACE (ACCENT ONLY)**

  

Used for:

- Guidance panels
    
- AI explanation
    
- Side context panels
    
- Modals / sheets
    

  

Properties:

- Opacity: **92–95%**
    
- Blur: **8–16dp**
    
- Border: 1dp outlineVariant @ 20%
    
- Radius: **16dp**
    
- Elevation: **2**
    

  

❌ Never allowed for:

- Tables
    
- Forms
    
- Invoices
    
- VAT
    
- Forecast charts
    

---

## **7. BASE CARD COMPOSABLE CONTRACT**

  

### **Conceptual API (CMP)**

```
DokusCard
 ├─ variant: Default | Soft
 ├─ padding: Default | Dense
 ├─ header (optional)
 ├─ content (required)
 └─ footer (optional)
```

### **Defaults**

- Padding (default): 16dp
    
- Padding (dense): 12dp
    
- Header spacing: 12dp
    
- Footer spacing: 12dp
    

  

Rules:

- Cards never overlap
    
- Cards align to grid
    
- No card-in-card unless inner is **Soft**
    
- No glass-on-glass
    

---

## **8. COMPONENT DEFINITIONS**

  

### **8.1 KPI Card**

- Variant: **Default**
    
- Content:
    
    - Primary value
        
    - Delta vs previous period
        
    - Indicator vs forecast
        
    
- ❌ No charts inside
    

---

### **8.2 Forecast Chart (Signature)**

- Surface: **Base Card**
    
- Lines:
    
    - Actual → solid
        
    - Forecast → dashed
        
    
- Markers:
    
    - VAT
        
    - Corporate tax
        
    
- No decoration
    
- No brand color usage
    

---

### **8.3 Status Chip (Mandatory)**

  

States:

- Processing (blue)
    
- Needs review (amber)
    
- Completed (green)
    
- Error (red)
    

  

Always icon + text.

---

### **8.4 Guidance Panel**

- Surface: **Glass**
    
- Tone: explanatory, calm
    
- Optional gold accent border (1–2dp)
    

  

Used to reduce anxiety, never to upsell.

---

## **9. DASHBOARD SCOPE (STRICT)**

  

Dashboard answers **three questions only**:

1. Where am I now?
    
2. Where am I heading?
    
3. What will hurt me if I do nothing?
    

  

### **Allowed on dashboard**

- Spending (current period)
    
- Income (current period)
    
- Margin (current period)
    
- Forecasted balance
    
- Forecasted VAT
    
- Forecasted corporate tax
    
- Net balance after taxes
    

  

### **Forbidden on dashboard**

- Full tables
    
- Accounting jargon
    
- Line-by-line expenses
    
- Amortisation schedules (summary only)
    

---

## **10. NAVIGATION**

  

### **Desktop**

- Persistent sidebar
    
- Text + icon
    
- No icon-only nav
    
- No hamburger
    

  

### **Mobile**

- Bottom navigation
    
- Max 5 items
    
- “More” contains secondary routes
    

---

## **11. ICONOGRAPHY**

- Material Symbols Rounded
    
- Size: 20–24dp
    
- Icons support text only
    
- Icons never replace labels
    

---

## **12. SPACING SYSTEM**

  

Base unit: **8dp**

- Card padding: 16dp
    
- Section spacing: 24dp
    
- Page padding:
    
    - Desktop: 24dp
        
    - Mobile: 16dp
        
    

  

Whitespace = confidence.

---

## **13. MOTION**

- Duration: 150–200ms
    
- Ease-out only
    
- No bounce
    
- No celebration
    

  

Motion confirms actions — it never entertains.

---

## **14. WHAT DOKUS MUST NEVER BECOME**

  

❌ Crypto dashboard

❌ Trading app

❌ Playful SaaS

❌ Government portal

❌ AI toy

---

## **FINAL DESIGN STATEMENT**

  

> **Dokus should feel like a Swiss watch for Belgian administration —**

> **precise, calm, modern, and quietly valuable.**

---

This file is **the law**.

If something is not defined here, it must not be invented.

  

If you want next, I can:

- Convert this into **CMP theme + composables**
    
- Define **tablet-specific rules**
    
- Create a **design QA checklist**
    
- Or enforce this via **PR review rules**
    

  

This is a strong, defensible system.