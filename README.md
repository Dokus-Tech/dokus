# 🧾 Dokus: AI-Powered Financial Management for Belgian Freelancers

> **⚠️ Less than 2 months until Belgium's mandatory e-invoicing deadline (January 1, 2026)**

**Your Finances. Your Server. Your Rules. Every Platform.**

*The first open-source PEPPOL solution with AI built-in, not bolted on.*

---

## 🚨 The Problem

Starting January 1, 2026, Belgium requires **all B2B invoices** to use PEPPOL e-invoicing.

**Penalties for non-compliance:**

- €1,500 first offense
- €5,000 subsequent offenses

**Current "solutions":**

- **Traditional SaaS:** €15-30/month forever, vendor lock-in, your data on their servers
- **Legacy software:** Built for accountants, not developers
- **Accountants:** €2,000-3,000/year + still need software

**The real problems:**

1. **No open-source PEPPOL solution** exists for Belgian freelancers
2. **Zero AI innovation** - In 2025, you're still manually categorizing expenses and chasing
   payments
3. **Same old interfaces** - Built like it's 2010, not designed for modern workflows

Until now.

---

## 💡 Dokus: The Modern Alternative

**Dokus** is the first AI-powered, open-source financial platform with native PEPPOL support for
Belgium.

**Native apps for every platform from day one:**

- 📱 **iOS & Android** - Full mobile apps, work anywhere
- 🖥️ **Windows, macOS, Linux** - Native desktop applications
- 🌐 **Web** - Browser-based access when needed
- 🎯 **One codebase** - Compose Multiplatform powers all platforms

**Core principles:**

- ✅ **PEPPOL-ready** - Full UBL 2.1 compliance built-in
- ✅ **AI-native** - Intelligent automation, not just digitization
- ✅ **Self-hosted backend** - Your server, your control
- ✅ **Open-source (AGPL v3)** - Audit the code, no black boxes
- ✅ **Free forever** - Self-host = €0/month
- ✅ **No vendor lock-in** - Export everything, anytime

Built for the AI era - where your financial software should be as smart as your code editor.

---

## 🚀 Quick Start

### Option 1: One-Click Deployment (Recommended)

**Deploy a fully functional Dokus server in under 5 minutes:**

```bash
# Download the deployment package
git clone https://github.com/Dokus-Tech/dokus.git
cd dokus/deployment

# Run the installation script for your platform:
./dokus.sh      # macOS / Linux
dokus.bat       # Windows
```

The script will:
- ✅ Install Docker if needed
- ✅ Configure all services
- ✅ Pull latest images from our registry
- ✅ Start all backend services
- ✅ Optionally configure auto-start on boot

**That's it!** Your Dokus server is running. See [deployment/README.md](deployment/README.md) for details.

### Option 2: Development Setup

```bash
# Clone the repository
git clone https://github.com/Dokus-Tech/dokus.git
cd dokus

# Run development environment
./dev.sh

# Run the app on your platform:
./gradlew :composeApp:run              # Desktop (Windows/macOS/Linux)
./gradlew :composeApp:wasmJsBrowserRun # Web browser
./gradlew :composeApp:assembleDebug    # Android APK
# iOS: Open in Xcode and run
```

**One codebase, all platforms.**

---

## ⚡ Features

### MVP (December 2025 Release)

- ✅ **Invoice Creation with PEPPOL** - Compliant e-invoicing from day one
- ✅ **Multi-platform Apps** - Android, iOS, Windows, macOS, Web (all from single codebase)
- ✅ **Document Upload** - Store and organize your financial documents

### Roadmap (Q1 2026 and beyond)

1. 👥 **Client Management** - Track clients and projects
2. 💰 **Expense & Invoice Management** - Complete financial overview
3. 🏦 **Bank Transaction Matching** - Auto-match transactions to documents
4. 🤖 **AI Assistant** - Context-aware help using your company's full financial data
5. 📦 **Inventory Management** - Track products and services
6. 📊 **Forecasting** - VAT projections and company balance predictions
7. 📤 **Export Options** - PDF, CSV, Excel for all data

---

## 🛠️ Tech Stack

**Frontend (All Platforms):**

- **Compose Multiplatform** - Single UI codebase for Android, iOS, Desktop, Web
- **Kotlin Multiplatform** - Shared business logic across all platforms
- **Native performance** - Compiles to native code on each platform

**Backend:**

- **Ktor 3.3.1** - Lightweight, async web framework
- **KotlinX RPC** - Type-safe service communication
- **PostgreSQL** - Rock-solid data persistence
- **Redis** - Caching and sessions
- **Docker** - One-command deployment

**Why this stack?**

- Write once, run everywhere (truly native apps, not webviews)
- 100% Kotlin from database to UI
- Excellent performance on all platforms
- Single team can maintain all platforms

---

## 🔐 Security & Compliance

- **Multi-tenant isolation** - `tenant_id` on every database query
- **Encrypted at rest** - Sensitive data protection
- **Audit logging** - 7-year immutable financial logs
- **GDPR compliant** - Data export and deletion
- **Self-hosted** - Your data never leaves your server

---

## 🗺️ Roadmap

### Phase 1: MVP (December 2025)

- [x] Core architecture
- [x] Database schema
- [x] Multi-tenant security
- [ ] PEPPOL integration (in progress)
- [ ] Invoice creation service
- [ ] Document upload system
- [ ] Multi-platform UI

### Phase 2: Launch (December 2025)

- [ ] Public repository release
- [ ] Docker images
- [ ] Setup documentation
- [ ] First 50 beta testers

### Phase 3: Extended Features (Q1 2026)

- [ ] Client management
- [ ] Expense tracking
- [ ] Bank transaction matching
- [ ] AI financial assistant

---

## 🤝 Contributing

**We need your help to meet the deadline!**

Even though the full code drops in December, you can:

- ⭐ **Star this repo** - Helps others discover the project
- 📝 **Open issues** - Feature requests, questions, ideas
- 💬 **Join discussions** - Shape the product direction
- 🧪 **Become a beta tester** - Get early access

See [CONTRIBUTING.md](CONTRIBUTING.md) for guidelines (coming December).

---

## 📊 Why Dokus?

1. **AI-first architecture** - Built for intelligent automation from the ground up
2. **Trust through transparency** - Open-source means you can audit exactly how your financial data
   is handled
3. **No vendor lock-in** - Own your data, export anytime, run forever
4. **Community-driven innovation** - Built by developers, for developers
5. **Future-proof** - AGPL ensures improvements benefit everyone

**We're not digitizing old workflows. We're reimagining financial management for the AI era.**

---

## 💰 Business Model

**Dokus is and will always be free to self-host.**

Future revenue streams:

- **Managed cloud hosting** - For those who prefer not to self-host
- **Priority support** - SLA-backed support for businesses
- **White-label options** - For accountants serving multiple clients

The core platform remains open-source forever (AGPL v3).

---

## 🇧🇪 Built for Belgium

Dokus understands Belgian requirements:

- PEPPOL B2B mandate (January 2026)
- Belgian VAT rates (21%, 12%, 6%, 0%)
- Structured communication (+++XXX/XXXX/XXXXX+++)
- Language support (NL/FR/EN)

---

## 📈 Project Status

**Current:** Active development, preparing for December 2025 public release

**Timeline:**

- **Now - November 2025:** Complete PEPPOL integration, finish MVP
- **December 2025:** Open-source release, beta testing begins
- **January 2026:** Production-ready for compliance deadline

**Updates:** Watch this repo for release announcements

---

## 🙋 FAQ

### When can I use Dokus?

Beta access starts December 2025 with core features: PEPPOL invoicing and document storage. Star the
repo to get notified.

### Will it be ready for the January deadline?

Yes. The December MVP focuses on the critical requirement: PEPPOL-compliant invoicing. Additional
features roll out through Q1 2026.

### How much will it cost?

Self-hosting is free forever. Optional cloud hosting pricing TBA.

### Can I contribute before December?

Yes! Open issues with feature requests, join discussions about requirements.

### Is this another abandoned open-source project?

No. We're Belgian freelancers ourselves - we NEED this to work by January.

### What makes Dokus different from existing solutions?

AI-native architecture + open-source + true multiplatform. While others add "AI features" as
marketing, we're building intelligence into the core - from smart document matching to predictive
forecasting.

---

## 📞 Contact

- **Email:** artem@invoid.vision
- **GitHub:** [@Dokus-Tech](https://github.com/Dokus-Tech)
- **Website:** [dokus.tech](https://dokus.tech)

---

## ⭐ Support the Project

**The best way to support Dokus:**

1. ⭐ Star this repository
2. 📢 Share with Belgian freelancers who need PEPPOL
3. 💡 Open issues with your requirements

Every star helps us gauge interest and motivates development.

---

## 📜 License

Dokus is licensed under **[AGPL v3](LICENSE)**.

This ensures Dokus remains open-source while preventing closed-source competitors from using our
code without contributing back.

---

**Built with urgency in Belgium 🇧🇪 Racing against the clock ⏰**

*Stop using 2010-era financial tools in 2025. Start using AI-powered financial management.*