# First 90 Days Execution Plan (Solo Founder, High Intensity)

**Start Date:** October 2024  
**Day 1 Target:** Development environment ready, first code committed  
**Day 90 Target:** 150-250 trials, 40-70 paying customers, €720-1,260 MRR

**Your Advantage:** You can work 60-80 hour weeks. This timeline assumes you're going hard.

---

## Overview: Sprint to PMF

You have 90 days to prove this works. Not "work-life balance" 90 days. **Actual grind.**

Most founders work 40 hours/week and hit 20-30 customers in 90 days.  
You'll work 60-80 hours/week and hit 40-70 customers.

### Success Definition (Day 90)

1. **Product works:** Users create invoices, get paid, track expenses without major bugs
2. **People pay:** 25-30% trial-to-paid conversion
3. **One channel works:** Accountant partnerships OR content delivering <€150 CAC
4. **You're alive:** Haven't burned out (track this)

---

## Days 1-30: Foundation & MVP (Oct 15 - Nov 15, 2024)

### Week 1: Setup (Oct 15-21)

**Mon-Tue: Development Environment (16 hours)**
```bash
Day 1 (8 hours):
✅ Set up monorepo structure
✅ Configure Docker Compose (PostgreSQL + Redis)
✅ Create base Ktor projects
✅ Set up GitHub with CI/CD

Day 2 (8 hours):
✅ Database schema design
✅ Create migrations with Flyway
✅ Connection pooling (HikariCP)
✅ Redis configuration
✅ Write first tests
```

**Wed-Thu: Core Infrastructure (16 hours)**
```bash
Day 3 (8 hours):
✅ Implement all database tables
✅ Set up Exposed entities
✅ Write database access layer
✅ Test suite for DB operations

Day 4 (8 hours):
✅ JWT generation/validation
✅ User registration endpoint
✅ Password hashing (Argon2id)
✅ Login endpoint + tests
```

**Fri-Sun: Invoice Foundation (24 hours)**
```bash
Day 5-7 (24 hours total):
✅ Invoice entity + CRUD
✅ Belgian VAT calculation (21%, 12%, 6%)
✅ Invoice number generation
✅ Client management CRUD
✅ Link invoices to clients
✅ Comprehensive tests

Weekend work: Yes. You're building.
```

**Week 1 Output:**
- 56 hours worked
- Can create users, clients, invoices
- All tested, all working

---

### Week 2: Core Features (Oct 22-28)

**Mon-Tue: Expense Tracking (16 hours)**
```bash
✅ Expense entity + CRUD
✅ File upload to S3 (receipts)
✅ Basic categorization
✅ Dashboard with totals (revenue, expenses, profit)
✅ Date range filtering
✅ Tests
```

**Wed-Thu: Peppol Integration START (16 hours)**
```bash
⚠️ CRITICAL PATH - START EARLY

✅ Select Pagero or EDICOM (research 2 hours)
✅ Register sandbox account
✅ Read API docs thoroughly
✅ Implement UBL XML generation
✅ Build transmission function skeleton
✅ Start certification process (submit forms)

Note: Certification takes 2-4 weeks, so START NOW
```

**Fri-Sun: Payments (24 hours)**
```bash
✅ Stripe SDK integration
✅ Mollie SDK integration
✅ Generate payment links for invoices
✅ Payment webhook handlers
✅ Update invoice status on payment
✅ Test full flow end-to-end
```

**Week 2 Output:**
- 56 hours worked
- Expense tracking live
- Peppol certification in progress
- Payment links working

---

### Week 3: Polish & Reports (Oct 29 - Nov 4)

**Mon-Wed: Reporting + UI (24 hours)**
```bash
✅ Basic P&L report (revenue - expenses)
✅ Dashboard with key metrics
✅ Recent transactions view
✅ Client detail pages
✅ Invoice PDF generation
✅ Email invoice delivery
```

**Thu-Fri: Security & Testing (16 hours)**
```bash
✅ Audit logging for all financial operations
✅ MFA implementation
✅ Rate limiting
✅ Input validation hardening
✅ SQL injection audit
✅ XSS prevention check
✅ Integration test suite
```

**Weekend: Deployment (16 hours)**
```bash
✅ Deploy to Render/Railway staging
✅ Set up PostgreSQL on AWS RDS
✅ Configure Redis
✅ Set up Sentry monitoring
✅ Create admin tools
✅ Document deployment process
✅ Automated backups configured
```

**Week 3 Output:**
- 56 hours worked
- MVP deployed to staging
- Security hardened
- Ready for beta users

---

### Week 4: Beta Prep & Launch (Nov 5-11)

**Mon-Tue: Final Polish (16 hours)**
```bash
✅ Fix bugs from staging testing
✅ Improve error messages
✅ Add loading states
✅ Mobile responsiveness
✅ Email templates (transactional)
✅ Terms of service + Privacy policy
```

**Wed: Beta Launch Prep (8 hours)**
```bash
✅ Write beta invitation email
✅ Create beta user onboarding guide
✅ Set up beta Slack channel
✅ Prepare feedback form
✅ List 30 people to invite from network
```

**Thu-Sun: Beta Launch (32 hours)**
```bash
Thu AM: Send 30 beta invitations
Thu PM-Sun: Rolling onboarding

✅ Schedule 30-min onboarding calls (3-5 per day)
✅ Walk each user through first invoice
✅ Show payment link feature
✅ Help add first expense
✅ Record all feedback
✅ Fix critical bugs immediately

You're doing:
- 5-6 onboarding calls per day
- Bug fixes between calls
- Evening: prioritize tomorrow's fixes
```

**Week 4 Output:**
- 56 hours worked
- 15-20 beta users onboarded
- Critical bugs identified and fixed
- First paying customers (some beta users)

**Month 1 Total: 224 hours (~56 hours/week)**

---

## Days 31-60: Beta Iteration & Launch Prep (Nov 12 - Dec 11)

### Week 5: Rapid Iteration (Nov 12-18)

**Mon: Feedback Analysis (8 hours)**
```markdown
Review all feedback:
- What features requested most?
- Where do users get stuck?
- What bugs are most painful?
- What makes them go "wow"?

Create priority list: Top 10 issues to fix
```

**Tue-Sun: Ship Improvements (60 hours)**
```markdown
✅ Fix top 5 bugs
✅ Improve onboarding flow
✅ Add most-requested feature
✅ Optimize slow queries
✅ Better mobile experience
✅ More beta user onboarding (10 more users)

You're in flow state:
- Morning: Deep coding (4-6 hours)
- Afternoon: Customer calls (2-3 hours)
- Evening: More coding (2-4 hours)
```

**Week 5 Output:**
- 68 hours worked
- 25-30 total beta users
- 10-15 paying customers
- MRR: €180-270

---

### Week 6: Analytics & Conversion (Nov 19-25)

**Mon-Tue: Analytics Setup (16 hours)**
```bash
✅ Implement Mixpanel tracking
✅ Track all key events:
  - Sign up, create invoice, send invoice
  - Receive payment, add expense, view dashboard
✅ Set up funnel analysis
✅ Create conversion dashboard
✅ Tag users by source
```

**Wed-Fri: Conversion Optimization (24 hours)**
```bash
Analyze data:
- What % complete first invoice? (Goal: 80%+)
- What % send first invoice? (Goal: 60%+)
- What % convert to paid? (Goal: 25%+)

A/B tests:
✅ Two onboarding variations
✅ Different pricing page copy
✅ Email drip timing

Ship improvements based on data
```

**Weekend: Content Sprint (20 hours)**
```markdown
Write 4 SEO articles (5 hours each):

1. "Dokus vs Accountable: Belgium Developer Comparison"
2. "How to Prepare for Belgium 2026 E-Invoicing"
3. "Accounting for IT Freelancers: Complete Guide"
4. "Belgium VAT Guide for Developers"

Publish all four, optimize for SEO
```

**Week 6 Output:**
- 60 hours worked
- Analytics tracking everything
- Trial-to-paid improving to 25%+
- 4 SEO articles live

---

### Week 7: Accountant Partnerships START (Nov 26-Dec 2)

**Mon-Tue: Partner Prep (16 hours)**
```markdown
✅ Build list of 200 Belgium accounting firms
  - Find on Google, LinkedIn
  - Focus on tech-friendly, 50-500 employees
  - Get contact info (email + LinkedIn)

✅ Create partner portal (basic version)
✅ Design email templates
✅ Create partnership deck (PDF)
✅ Set up commission tracking
```

**Wed-Sun: Outreach Blitz (48 hours)**
```markdown
Send 20 emails per day (Wed-Sat):

Morning routine:
- Research 20 firms (1 hour)
- Write 20 personalized emails (2 hours)
- Send and track responses

Afternoon:
- Follow up with interested parties
- Schedule calls
- Continue coding/support

Weekend:
- Prep for next week's outreach
- Refine email templates based on responses
- Schedule partner calls for Week 8

Target: 80 emails sent, 8-12 calls scheduled
```

**Week 7 Output:**
- 64 hours worked
- 80 accountants contacted
- 8-12 partner calls scheduled
- Partner materials ready

---

### Week 8: Launch Prep (Dec 3-9)

**Mon-Wed: Product Hunt Prep (24 hours)**
```markdown
✅ Create Product Hunt listing
✅ Design thumbnail + screenshots
✅ Write compelling description
✅ Record 2-3 minute demo video
✅ Prepare launch day social posts
✅ Email beta users for upvotes
✅ Schedule launch for Tuesday 12:01am PST
```

**Thu-Fri: Marketing Assets (16 hours)**
```markdown
✅ Create comparison pages (vs top 3 competitors)
✅ Design landing page variations (A/B test ready)
✅ Write email templates (welcome, onboarding, conversion)
✅ Set up email automation (Mailchimp/ConvertKit)
✅ Prepare press release
✅ Create social media content calendar
```

**Weekend: Final Push (20 hours)**
```markdown
✅ 4 more SEO articles (total: 8)
✅ Conduct partner calls (schedule 5)
✅ Fix any critical bugs
✅ Test entire flow 10 times
✅ Prepare for launch week madness

You're excited. Launch is 1 week away.
```

**Week 8 Output:**
- 60 hours worked
- Product Hunt ready
- 3-5 accountant partners signed
- 8 SEO articles published
- 20-25 paying customers
- MRR: €360-450

**Month 2 Total: 252 hours (~63 hours/week)**

---

## Days 61-90: LAUNCH & SCALE (Dec 10-Jan 10, 2025)

### Week 9: LAUNCH WEEK (Dec 10-16)

**Mon: Soft Launch (12 hours)**
```markdown
✅ LinkedIn post announcing launch
✅ Twitter thread with product story
✅ Post on Indie Hackers
✅ Email personal network (200 people)
✅ Warm up Product Hunt voters
✅ Post in Belgian developer communities
```

**Tue: Product Hunt Launch (18 hours)**
```markdown
00:01 PST: Launch goes live

Your schedule:
- 06:00-12:00: Engage with EVERY comment
- 12:00-18:00: Email beta users, share on social
- 18:00-24:00: Final push for votes

✅ Respond to every single comment within 30 min
✅ Share milestones on Twitter
✅ Engage in other products' comments
✅ Stay online ALL DAY

Goal: Top 10 Product of the Day
```

**Wed-Thu: Distribution Blitz (20 hours)**
```markdown
✅ Submit to 20+ directories:
  - Capterra, G2, GetApp
  - BetaList, AlternativeTo
  - European startup directories
  - SaaS listing sites

✅ Email 50 more accounting firms
✅ Post retrospective on Indie Hackers
✅ Handle support flood (50+ inquiries)
✅ Fix any bugs discovered at scale
```

**Fri-Sun: Conversion Focus (22 hours)**
```markdown
✅ Analyze launch traffic funnel
✅ Fix onboarding bottlenecks
✅ Respond to every support ticket
✅ Start Google Ads (€500 budget)
✅ A/B test pricing page variations
✅ First partner referrals come in

Weekend: You're exhausted but exhilarated
```

**Week 9 Output:**
- 72 hours worked (launch week is brutal)
- 80-120 trial signups
- 35-50 paying customers
- MRR: €630-900
- Product Hunt Top 10 (hopefully!)

---

### Week 10: Post-Launch Optimization (Dec 17-23)

**Mon-Wed: Data Analysis (24 hours)**
```markdown
Review launch metrics:
- Where did traffic come from?
- What % converted to trial?
- What % of trials converted to paid?
- Where are people dropping off?

✅ Fix top 3 conversion blockers
✅ Improve onboarding based on data
✅ Ship automation features:
  - Automated payment reminders
  - Recurring invoice templates
  - Better dashboard
```

**Thu-Fri: Partnership Acceleration (16 hours)**
```markdown
✅ Follow up with all partner prospects
✅ Close 3-5 more partnerships
✅ First partner referral conversions
✅ Optimize partner onboarding
✅ Create partner success playbook
```

**Weekend: Content (12 hours)**
```markdown
✅ Write 2 more comparison articles
✅ Publish launch retrospective
✅ Guest post on Belgian tech blog
✅ Respond to community questions

Christmas is next week - most competitors are off.
You're shipping.
```

**Week 10 Output:**
- 52 hours worked
- 8-10 accountant partnerships total
- 45-60 paying customers
- MRR: €810-1,080

---

### Week 11: Holiday Grind (Dec 24-30)

**Mon-Wed: Pre-Holiday Push (20 hours)**
```markdown
Most people are winding down. You're not.

✅ Ship 2-3 small features
✅ Write 3 more articles
✅ Email 30 more accountants
✅ Optimize conversion funnel
✅ Prepare January marketing calendar
```

**Thu-Fri: Christmas (8 hours)**
```markdown
Take Christmas Day mostly off (4 hours max):
- Morning: Check critical alerts
- Answer urgent support
- No coding

Day after: Back to work
```

**Weekend: New Year Push (20 hours)**
```markdown
Everyone's on vacation. You're building.

✅ Ship feature improvements
✅ Content sprint: 4 articles
✅ Plan Q1 2025 strategy
✅ Set up advanced analytics
✅ Test everything thoroughly

New Year's Eve: 4 hours max, then celebrate
```

**Week 11 Output:**
- 48 hours worked (holiday week)
- 50-65 paying customers
- MRR: €900-1,170
- Competitors are sleeping, you're winning

---

### Week 12: Sprint to Goal (Dec 31 - Jan 6, 2025)

**Mon-Wed: Final Push (28 hours)**
```markdown
✅ Email 40 more accountants
✅ Launch referral program
✅ Ship mobile responsiveness improvements
✅ Write 3 more articles (total: 18+)
✅ Increase Google Ads to €800
✅ Close 3 more partnerships
```

**Thu-Fri: Metrics Analysis (16 hours)**
```markdown
Review entire 90 days:

Acquisition:
- Total signups: ___
- By source: ___
- CAC by channel: ___

Activation:
- First invoice: ___%
- Time to first invoice: ___ min
- Trial-to-paid: ___%

Revenue:
- Paying customers: ___
- MRR: €___
- ARPU: €___

Retention:
- Week 1: ___%
- Month 1: ___%

Document everything learned.
```

**Weekend: Planning & Rest (8 hours)**
```markdown
✅ Set Q1 2025 OKRs
✅ Plan next 90 days
✅ Update roadmap
✅ Celebrate wins!

Take Sunday OFF. Actually rest.
You earned it.
```

**Week 12 Output:**
- 52 hours worked
- 60-80 paying customers
- MRR: €1,080-1,440
- Clear path to €10K MRR visible

**Month 3 Total: 224 hours (~56 hours/week)**

---

## 90-Day Summary

### Total Hours Worked
- **Month 1:** 224 hours (56/week)
- **Month 2:** 252 hours (63/week)
- **Month 3:** 224 hours (56/week)
- **Total:** 700 hours over 13 weeks

### Expected Results (Conservative)
- **Signups:** 200-300 total
- **Paying customers:** 60-80
- **MRR:** €1,080-1,440 (€13K-17K ARR)
- **Partnerships:** 10-15 accountants
- **Content:** 18-20 articles
- **Product:** Stable, users love it

### Expected Results (Aggressive - You Work Hard)
- **Signups:** 300-400 total
- **Paying customers:** 80-100
- **MRR:** €1,440-1,800 (€17K-22K ARR)
- **Partnerships:** 15-20 accountants
- **Content:** 25+ articles
- **Product:** Feature-rich, scaling

---

## Your Daily Schedule (Optimized for Output)

### Peak Performance Days (Mon-Fri)

**6:00-7:00am:** Wake up, coffee, review yesterday's metrics  
**7:00-12:00pm:** DEEP WORK - Coding, no interruptions (5 hours)  
**12:00-1:00pm:** Lunch + walk (mental break)  
**1:00-3:00pm:** Calls (customers, partners, support) (2 hours)  
**3:00-6:00pm:** Marketing/content/growth work (3 hours)  
**6:00-7:00pm:** Dinner  
**7:00-10:00pm:** More coding OR admin/planning (3 hours)  
**10:00pm:** Hard stop, plan tomorrow, sleep

**Total:** 13 hours productive work

### Weekend Days (Sat-Sun)

**8:00am-12:00pm:** Deep work (4 hours)  
**12:00-2:00pm:** Break  
**2:00-6:00pm:** Work (4 hours)  
**Evening:** Off (or 2-3 hours if needed)

**Total:** 8-11 hours

### Monthly Average
- Weekdays: 13 hours × 5 = 65 hours
- Weekends: 10 hours × 2 = 20 hours
- **Weekly: 85 hours peak, 60 hours sustained**

You CAN do this for 90 days. After that, optimize.

---

## Red Flags (Stop and Reassess)

🚨 **Product:**
- Users can't complete basic flows
- >10% critical bug rate
- Peppol not working

🚨 **Market:**
- <50 signups after launch week
- <15% trial-to-paid after Month 2
- 0 accountant partnerships after 100 emails

🚨 **Personal:**
- Quality of work declining
- Sleeping <5 hours consistently
- Health issues emerging
- Lost motivation entirely

**Action:** Take 2-3 days completely off, reassess

---

## Success Checklist

### By Day 30
- [ ] MVP deployed to production
- [ ] 15-20 beta users
- [ ] 10-15 paying customers
- [ ] €180-270 MRR
- [ ] Peppol certification in progress

### By Day 60
- [ ] 50-80 trial users
- [ ] 25-40 paying customers
- [ ] €450-720 MRR
- [ ] 5-8 accountant partnerships
- [ ] 8-12 SEO articles published
- [ ] Product Hunt launch planned

### By Day 90
- [ ] 200-400 trial signups
- [ ] 60-100 paying customers
- [ ] €1,080-1,800 MRR
- [ ] 10-20 accountant partnerships
- [ ] 18-25 articles published
- [ ] Product stable and scaling

---

## The Solo Founder Reality

**You will:**
- Work harder than you ever have
- Question yourself constantly
- Feel lonely sometimes
- Wonder if it's worth it
- Have moments of doubt

**But also:**
- Ship faster than teams
- Learn more in 90 days than 2 years at a job
- Build something real
- Own 100% of it
- Create freedom for yourself

**The difference between success and failure:**
- Successful founders keep going when it's hard
- Failed founders quit during the dip

You're not going to quit.

---

**Now execute. Update this doc with actual results as you go.**

Next: [[Storyboard|Storyboard]] for full 5-year journey
