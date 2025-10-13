# Product Strategy

**Last Updated:** October 2025  
**Owner:** Solo Founder  
**Status:** Pre-Launch

---

## Vision

**"Make financial compliance invisible for European developers and freelancers"**

We're not building "another accounting tool." We're building the **anti-accounting software** - a system so automated and intelligent that developers never think about finances except when making business decisions.

---

## The Problem (Validated)

### Primary Pain Points

**For IT Freelancers in Belgium (Target Segment):**

1. **Compliance Anxiety** (Critical - 2026 Deadline)
   - "Will I get fined €1,500-5,000 for wrong e-invoicing?"
   - "Am I missing tax deductions?"
   - "Is my VAT calculation correct?"

2. **Time Waste** (10-15 hours/month)
   - Manual data entry from bank statements
   - Chasing receipts and invoices
   - Reconciling transactions
   - Preparing documents for accountant

3. **Expensive Professional Services**
   - €1,500-3,000/year for basic bookkeeping
   - €800-1,500 for tax preparation
   - €150-300/hour for accountant consultations

4. **Tool Fragmentation**
   - Separate tools for invoicing, expenses, time tracking
   - No integration = duplicate data entry
   - Poor visibility into actual profitability

### Why Existing Solutions Fail

| Solution | Problem |
|----------|---------|
| **Accountable** (€15-22/mo) | Generic for all freelancers, complex UI, no developer focus |
| **Yuki** (€25+/mo) | Requires accountant, complex onboarding, expensive |
| **Exact Online** (€22+/mo) | Enterprise features, overwhelming for solopreneurs |
| **QuickBooks** (€15+/mo) | US-focused, poor European compliance |
| **Excel/Google Sheets** | Manual hell, error-prone, no automation |

**The gap:** No one serves developer freelancers specifically with affordable, automated, Peppol-ready financial management.

---

## The Solution

### Positioning Statement

**"Dokus is financial software built by developers, for developers—featuring API-first architecture, zero accounting jargon, and automatic compliance. Finally, finances that speak your language."**

### Core Value Propositions

**1. Compliance Made Invisible**
- Peppol-ready invoicing (Belgium 2026 mandatory)
- Automatic VAT calculation (21%, 12%, 6% Belgian rates)
- Tax deduction tracking with AI categorization
- 7-year audit log for GDPR compliance

**2. Time Back to Coding**
- Save 10+ hours monthly on bookkeeping
- Automated bank feed reconciliation
- Receipt OCR with photo upload
- One-click payment links in invoices

**3. Developer-First Experience**
- RESTful API for custom integrations
- Webhook notifications for events
- CLI tool for power users (future)
- Technical documentation, not marketing speak

**4. Transparent, Affordable Pricing**
- €18/month for full features
- No hidden fees or surprise charges
- Cancel anytime, export all data
- 15% annual discount

---

## Target Customer Profile (ICP)

### Primary: IT Freelance Developers (Belgium)

**Demographics:**
- Age: 25-45
- Location: Belgium (Brussels, Antwerp, Ghent regions)
- Legal structure: Independent/freelancer (eenmanszaak or SPRL)
- Revenue: €40K-120K annually
- Company size: 0-1 employees (solo or 1 part-time assistant)

**Psychographics:**
- Values automation and efficiency
- Comfortable with technical tools
- Prefers self-service over hand-holding
- Active in developer communities (Indie Hackers, HackerNews, local meetups)
- Pragmatic: will pay for quality tools that save time

**Current Behavior:**
- Uses 3-5 separate tools for business management
- Spends 10-15 hours monthly on finances
- Pays €2,500-5,000/year for accountant
- Procrastinates on bookkeeping until month-end or quarter-end
- Anxious about compliance and taxes

**Jobs to Be Done:**
- "When tax season arrives, I want organized financial records so I minimize accountant fees and avoid penalties"
- "When invoicing clients, I want instant payment options so I improve cash flow and get paid faster"
- "When recording expenses, I want automatic categorization so I claim all deductions without manual work"

### Secondary: Micro SaaS Founders (Belgium)

**Key Differences from Primary:**
- 1-5 employees (not solo)
- €100K-500K ARR
- Need team features and role permissions
- Higher willingness to pay (€35/mo Business tier)

**When to Target:** Year 2+ after proving product with freelancers

---

## Differentiation Strategy

### Developer-First Positioning (Phase 1: Months 0-12)

**Why It Works:**
1. **Lower acquisition cost** - Focused community channels vs broad small business marketing
2. **Higher word-of-mouth** - Developers share tools they love in communities
3. **Premium pricing justified** - "Built for us" creates willingness to pay
4. **Less competition** - Accountable, Xero, etc don't speak developer language

**Marketing Channels:**
- Indie Hackers (post progress updates, engage in discussions)
- HackerNews (Show HN launch, relevant comments)
- Reddit r/webdev, r/freelance, r/belgium
- Local tech meetups (Brussels.js, Ghent tech events)
- Dev podcasts (sponsor or guest)

**Messaging Examples:**
- ❌ "Simple accounting for small businesses"
- ✅ "Accounting with a REST API and zero jargon"
- ❌ "Track your expenses easily"
- ✅ "OCR your receipts, webhook when categorized"

### Compliance Urgency Hook (Belgium 2026)

**Timeline:**
- January 1, 2026: B2B e-invoicing mandatory in Belgium
- Penalties: €1,500 first offense, €5,000 third offense

**Our Advantage:**
- 14 months to capture market before deadline
- Peppol-ready from launch (competitors scrambling to add)
- Positioning: "Be compliant before penalties hit"

**Campaign Phases:**
- Q2-Q3 2025: "Early adopter" messaging, avoid the rush
- Q4 2025: "Deadline approaching" urgency
- Q1 2026: "Avoid penalties today" emergency

---

## Feature Philosophy

### Must Have (Table Stakes)
- Secure authentication (MFA required)
- Invoice creation with Peppol transmission
- Expense tracking with receipt storage
- Basic P&L reporting
- Client database
- VAT calculation

### Should Have (Differentiators)
- **Instant payment links** (solve cash flow pain)
- **Automated bank sync** (eliminate manual entry)
- **Receipt OCR** (photo → categorized expense)
- **Real-time tax estimates** (remove anxiety)
- **API access** (developer appeal)

### Could Have (Future Phases)
- Multi-currency (Year 2, international clients)
- Team features (Year 2, micro-agencies)
- Time tracking integration (Year 2)
- Mobile apps (iOS/Android in Months 3-6)
- AI forecasting (Year 2, need historical data)

### Won't Have (Scope Creep)
- Full accounting/double-entry (use Xero integration instead)
- Inventory management (not for service businesses)
- Payroll (too complex, low margin, use Payfit integration)
- CRM features (use existing tools, offer integrations)

---

## Product Roadmap

### Months 1-3: MVP Launch

**Goal:** 10-20 paying customers proving core value

**Features:**
- [ ] User authentication (OAuth + email/password + MFA)
- [ ] Invoice CRUD with Peppol transmission
- [ ] Expense tracking with receipt photo upload
- [ ] Client management
- [ ] Basic dashboard (revenue, expenses, profit)
- [ ] Payment links (Stripe + Mollie integration)
- [ ] Simple P&L report
- [ ] Email delivery for invoices

**Success Metrics:**
- 80%+ trial users send at least 1 invoice in first week
- 25%+ trial-to-paid conversion
- 10-20 paying customers by Month 3
- €180-360 MRR

### Months 3-6: Product-Led Growth

**Goal:** 50-100 paying customers, <5% monthly churn

**Features:**
- [ ] Automated payment reminders (smart timing)
- [ ] Recurring invoice templates
- [ ] Bank account connection (Plaid/Tink)
- [ ] Automated transaction categorization
- [ ] Cash flow forecasting (basic)
- [ ] Enhanced P&L with comparisons
- [ ] Mobile app (React Native or Kotlin Multiplatform)
- [ ] Receipt OCR (Google Vision API)

**Success Metrics:**
- 50-100 paying customers
- €900-1,800 MRR
- 30%+ of users activate automation features
- <5% monthly churn
- 5+ accountant partnerships generating referrals

### Months 6-12: Scale & Differentiation

**Goal:** 200-500 paying customers, strong retention

**Features:**
- [ ] AI expense categorization (learns from patterns)
- [ ] Real-time VAT estimates
- [ ] Advanced analytics dashboard
- [ ] Integration marketplace (Xero, QuickBooks export)
- [ ] Team features (roles, permissions)
- [ ] Multi-currency support
- [ ] API documentation and access
- [ ] White-label reports for accountants

**Success Metrics:**
- 200-500 paying customers
- €3,600-9,000 MRR
- 95%+ gross revenue retention
- 10+ accountant partnerships
- 100%+ net revenue retention (upgrades offset churn)

---

## Success Criteria

### Product-Market Fit Indicators

**Strong Signals (3+ required):**
- ✅ 40%+ users would be "very disappointed" if Dokus disappeared (Sean Ellis test)
- ✅ 95%+ monthly gross revenue retention
- ✅ 30%+ trial-to-paid conversion for PQLs
- ✅ Organic word-of-mouth accounts for 20%+ of signups
- ✅ NPS > 50
- ✅ CAC payback < 12 months

**Weak Signals (need iteration):**
- ⚠️ <25% trial-to-paid conversion
- ⚠️ <90% monthly retention
- ⚠️ Users describe it as "nice to have" not "must have"
- ⚠️ Low feature engagement (automation features unused)
- ⚠️ High support burden (lots of confused users)

### Pivot Triggers

**Consider major product changes if:**
- 6 months post-launch with <50 paying customers
- Churn consistently >7% monthly after Month 6
- CAC exceeds €300 across all channels
- Unable to get accountant partnerships (0 after 3 months outreach)
- Belgium 2026 deadline passes with <500 customers

**Potential Pivots:**
- Focus on Netherlands (larger market, less competition)
- Broader small business target (not just IT freelancers)
- Pure compliance tool (Peppol SaaS, no full accounting)
- White-label for accountants (B2B2C model)

---

## Competitive Positioning

### Competitive Matrix

| Feature | Dokus | Accountable | Yuki | Exact Online |
|---------|-------|-------------|------|--------------|
| **Price** | €18/mo | €15-22/mo | €25+/mo | €22+/mo |
| **Developer-focused** | ✅ | ❌ | ❌ | ❌ |
| **API Access** | ✅ | ❌ | Limited | ✅ |
| **Peppol Ready** | ✅ | ⚠️ | ✅ | ✅ |
| **Instant Payments** | ✅ | ❌ | ❌ | ❌ |
| **Bank Sync** | ✅ | ✅ | ✅ | ✅ |
| **Receipt OCR** | ✅ | ✅ | ❌ | ❌ |
| **Onboarding Time** | 7 min | 20 min | 60 min | 45 min |
| **Target Market** | IT freelancers | All freelancers | SMB | SMB |

### Why We Win

**Against Accountable:**
- Developer positioning (they're generic)
- Better UX for technical users
- API access for power users
- Lower friction onboarding

**Against Yuki:**
- 28% cheaper (€18 vs €25+)
- Self-service (no accountant required)
- Faster onboarding
- Better for solopreneurs

**Against Exact Online:**
- 18% cheaper (€18 vs €22)
- Simpler interface (not overwhelming)
- Instant payment links
- Developer community

**Against Excel/Sheets:**
- Automated everything (vs manual hell)
- Compliance guaranteed
- Real-time visibility
- Professional invoices

---

## Product Principles (Decision Framework)

When making product decisions, use these principles:

1. **Automation > Features**
   - One automated flow beats ten manual features
   - Ask: "Can the computer do this instead of the human?"

2. **Developer Experience > Generic UX**
   - Technical accuracy over simplified explanations
   - CLI/API over only GUI
   - Webhooks over polling

3. **Compliance by Default**
   - Never let users create non-compliant invoices
   - Automatic audit logging for everything
   - 7-year data retention enforced

4. **Speed > Perfection**
   - Ship 80% solution in 2 weeks > 100% solution in 2 months
   - Iterate based on real usage data
   - Kill features that don't drive retention

5. **Focus > Feature Parity**
   - Better to be exceptional at core job-to-be-done
   - Integrate with best-in-class tools vs building everything
   - Say no to 90% of feature requests

---

## Go-to-Market Fit

### Why Belgium First?

**Advantages:**
1. ✅ Urgent compliance deadline (January 2026)
2. ✅ 40,000 IT freelancers (concentrated market)
3. ✅ Lower CAC (€40-80 vs €150-300)
4. ✅ Tax incentives (120% deduction for e-invoicing software)
5. ✅ Smaller market = faster validation

**Disadvantages:**
1. ⚠️ Smaller overall market vs Netherlands
2. ⚠️ Multi-language complexity (Dutch, French, English)
3. ⚠️ Complex regional regulations

### Why Not Netherlands First?

**Disadvantages for Year 1:**
1. ❌ No immediate compliance deadline (maybe 2030)
2. ❌ Higher competition (Exact, Yuki established)
3. ❌ Larger market = slower to prove PMF
4. ❌ Higher CAC without urgency

**When to Enter:** 2026-2027 after Belgium PMF proven

---

## Product Metrics Dashboard (Weekly Review)

```markdown
### Acquisition
- Trial signups: ___ (goal: +20% WoW)
- Trial sources: Organic _%, Paid _%, Partnerships _%, Referral _%

### Activation
- Users completing first invoice: __% (goal: 80%+)
- Time to first invoice: __ minutes (goal: <10 min)
- Users adding 3+ expenses: __% (goal: 60%+)

### Engagement
- DAU/MAU ratio: __% (goal: 30%+)
- Features used per session: __ (goal: 3+)
- Weekly active users: ___ (goal: 70%+ of paid users)

### Revenue
- MRR: €___ (goal: +15-25% MoM)
- ARPU: €___ (goal: €15-18)
- Trial-to-paid conversion: __% (goal: 25-30%)

### Retention
- Monthly churn: __% (goal: <5%)
- Gross revenue retention: __% (goal: 95%+)
- Net revenue retention: __% (goal: 100%+)
```

---

**Next Steps:**
1. Read [[03-Go-to-Market-Strategy|Go-to-Market Strategy]]
2. Review [[05-Feature-Roadmap|Feature Roadmap]] for detailed specs
3. Check [[10-Metrics-KPIs|Metrics & KPIs]] for tracking setup

---

*This is a living document. Update weekly during First 90 Days, monthly thereafter.*
