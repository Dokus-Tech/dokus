# 🚀 Dokus: Financial Management for Developers

> **Peppol e-invoicing, automated expense tracking, and instant payments. Built specifically for developers. Zero accounting jargon. API-first. Open-source. Runs on Android, iOS, Desktop, and Web.**

[![GitHub License](https://img.shields.io/github/license/dokus/dokus?color=green)](LICENSE)
[![Contributors](https://img.shields.io/github/contributors/dokus/dokus)](CONTRIBUTING.md)
[![Stars](https://img.shields.io/github/stars/dokus/dokus?style=social)](https://github.com/dokus/dokus/stargazers)
[![Status](https://img.shields.io/badge/status-beta-orange)](#status)

---

## 🎯 Vision

**Make financial compliance invisible for European developers and freelancers.**

We're not building "another accounting tool." We're building the **anti-accounting software**—a system so automated and intelligent that developers never think about finances except when making business decisions.

No manual data entry. No accounting jargon. No compliance anxiety. Just code, invoice, get paid. Everything else happens automatically.

---

## 💡 Why Dokus?

You're a freelance developer. You make €50-150K/year. But you spend **10+ hours every month** on
accounting:

- 📝 Manual invoice creation (boring)
- 💰 Chasing late payments (stressful)
- 🧾 Tracking receipts & expenses (tedious)
- 😰 Will I get fined for non-compliance? (Belgium 2026 deadline)
- 💸 Accountant bills: €1,500-3,000/year (expensive)

**Dokus fixes this.**

Invoicing, expense tracking, automated payments, and Peppol compliance—all the financial stuff you
hate, automated. So you can focus on coding.

---

## ⚡ Core Features

### Invoicing (Peppol-Ready)

- ✅ Create invoices in 2 minutes
- ✅ Automatic Peppol e-invoicing transmission (Belgium 2026 compliant)
- ✅ Instant payment links (Stripe + Mollie)
- ✅ Get paid 3+ days faster
- ✅ PDF download + email delivery

### Expenses

- 📸 Photo receipts → automatic expense records
- 🏷️ Auto-categorization (software, hardware, travel, meals)
- 🧮 VAT tracking (21%, 12%, 6% Belgian rates)
- 📊 Deductibility calculation
- 📤 Export for your accountant

### Reporting

- 📈 Real-time P&L (profit/loss instantly)
- 📋 VAT summary (quarterly)
- 💡 Tax estimate alerts
- 📊 Dashboard with key metrics
- 📄 Export to Excel, PDF, CSV

### Automation

- 🤖 Recurring invoices (set once, send automatically)
- 🔔 Payment reminders (smart timing, no weekends)
- 🔗 Webhook notifications (build on top of Dokus)
- 🔐 Multi-tenant security (tenant_id on every query)

---

## 🛠️ Self-Hosted & Open-Source

**Dokus is AGPL v3 licensed.** That means:

- ✅ **Self-host for free** - Run on your own server, no cloud subscription
- ✅ **Full source code access** - Audit everything, modify as needed
- ✅ **No vendor lock-in** - Export your data anytime
- ✅ **Community-driven** - Contribute features, fixes, improvements
- ✅ **Transparent** - See exactly how we handle your financial data

**Currently:** Self-hosted only (you own the infrastructure).  
**Coming soon:** Cloud hosting option for those who prefer managed deployments.

Either way: Your data, your control, open-source forever.

---

## 🚀 Quick Start

### Option 1: Run the Multiplatform App

**Prerequisites:** JDK 17+

```bash
# Clone the repo
git clone https://github.com/dokus/dokus.git
cd dokus

# Run on Web (with hot reload)
./gradlew wasmJsBrowserRun -t

# Run on Desktop (macOS/Windows/Linux)
./gradlew :composeApp:run

# Build Android APK
./gradlew :composeApp:assembleDebug

# Package Desktop DMG (macOS)
./gradlew :composeApp:packageReleaseDmg
```

**Note:** For iOS development, you'll need a Mac with Xcode installed. Open the project in Android Studio and run the iOS target.

### Option 2: Run with Backend Services (Full Stack)

**Prerequisites:** Docker & Docker Compose, JDK 17+

```bash
# Start infrastructure (PostgreSQL + Redis)
docker-compose -f docker-compose.dev.yml up postgres-dev redis-dev -d

# Build backend services
./gradlew :features:auth:backend:build
./gradlew :features:invoicing:backend:build

# Run backend services (in separate terminals or with docker-compose)
docker-compose -f docker-compose.dev.yml up -d

# Run the client app
./gradlew wasmJsBrowserRun -t  # Web
# OR
./gradlew :composeApp:run      # Desktop
```

**Access Points:**
- Web App: http://localhost:8080
- Auth Service: http://localhost:9091
- Database Service: http://localhost:9071
- Invoicing Service: http://localhost:9092
- PgAdmin (optional): http://localhost:5050 (profile: `--profile tools`)

### Option 3: Development Environment Setup

```bash
# Run all tests across platforms
./gradlew allTests

# Run platform-specific tests
./gradlew testDebugUnitTest     # Android
./gradlew desktopTest           # Desktop/JVM
./gradlew iosSimulatorArm64Test # iOS Simulator (ARM)

# Full verification (build + test)
./gradlew check

# Clean build artifacts
./gradlew clean
```

### Environment Configuration

Configure API endpoints using BuildKonfig:

```bash
# Production (default): api.dokus.ai:443
./gradlew build

# Local development: 127.0.0.1:8000
./gradlew build -PENV=local

# Android Emulator: 10.0.2.2:8000
./gradlew build -PENV=localAndroid

# Custom configuration
./gradlew build -PAPI_HOST=staging.dokus.ai -PAPI_PORT=8080

# Enable debug logging
./gradlew build -PDEBUG=true
```

---

## 📊 Tech Stack

**Client:** Kotlin Multiplatform 2.2.20 + Compose Multiplatform 1.9.1
**Backend Services:** Ktor 3.3.1 + KotlinX RPC 0.10.0
**Database:** PostgreSQL 17 (NUMERIC for exact calculations) + Exposed ORM 0.61.0
**Cache:** Redis 8
**DI:** Koin 4.1.1
**Navigation:** Compose Navigation 2.9.1
**Platforms:** Android, iOS, Desktop (JVM), Web (WASM)
**Logging:** Kermit 2.0.8
**Peppol:** Pagero/EDICOM integration (UBL 2.1)
**Payments:** Stripe + Mollie webhooks

**Why this stack?**

- **True Multiplatform:** Single codebase for Android, iOS, Desktop, and Web
- **Type-safe everywhere:** Kotlin across all platforms and backend services
- **Modern UI:** Compose Multiplatform for native-feeling UIs on all platforms
- **Async by default:** Perfect for I/O-heavy financial operations
- **Lightweight:** Fast startup, low resource usage
- **Production-proven:** PostgreSQL reliability + Ktor performance
- **Developer-friendly:** Clear, readable code with excellent tooling

---

## 🔐 Security & Privacy

- ✅ **Multi-tenant by default** - Strict data isolation (tenant_id on every query)
- ✅ **Encryption at rest** - AES-256-GCM for sensitive data
- ✅ **HTTPS enforced** - TLS 1.3 minimum
- ✅ **JWT tokens** - Stateless authentication, RS256 signing
- ✅ **Audit logging** - 7-year immutable logs of all financial operations
- ✅ **No credit card storage** - Stripe/Mollie handle payments
- ✅ **GDPR compliant** - Data export + deletion support

**Self-hosting means:** Your data stays on your server. We never see it.

---

## 💻 Development

### Project Structure

```
dokus/
├── composeApp/                 # Main KMP application entry point
│   └── src/
│       ├── commonMain/         # Shared UI & logic
│       ├── androidMain/        # Android-specific code
│       ├── iosMain/            # iOS-specific code
│       ├── desktopMain/        # Desktop (JVM) code
│       └── wasmJsMain/         # Web (WASM) code
├── foundation/                 # Foundation modules shared across all platforms
│   ├── design-system/          # UI components & theming (Compose Multiplatform)
│   ├── app-common/             # Shared app logic (ViewModels, state)
│   ├── platform/               # Platform abstractions (logging, config)
│   ├── navigation/             # Type-safe navigation
│   ├── domain/                 # Domain models & use cases
│   ├── apispec/                # API specifications (KotlinX RPC)
│   ├── database/               # Database migrations (Flyway)
│   └── sstorage/               # Secure storage abstraction
├── features/                   # Feature modules (backend + presentation)
│   ├── auth/
│   │   ├── backend/            # Authentication service (Ktor)
│   │   ├── presentation/       # Auth UI (Compose Multiplatform)
│   │   └── data/               # Auth data layer
│   ├── invoicing/backend/      # Invoicing service + Peppol
│   ├── expense/backend/        # Expense tracking service
│   ├── payment/backend/        # Payment service (Stripe/Mollie)
│   └── reporting/backend/      # Analytics service
└── build-logic/                # Custom Gradle plugins & build configuration
```

**Architecture:**
- **Kotlin Multiplatform:** Single codebase for all client platforms
- **Compose Multiplatform:** Shared UI components across platforms
- **Microservices:** Independent backend services communicating via KotlinX RPC
- **Type-safe:** RPC contracts shared between client and services
- **Modular:** Feature-based modules for scalability

### Contributing

We love contributions! Whether it's:

- 🐛 Bug fixes
- ✨ New features
- 📚 Documentation improvements
- 🌍 Translations (AGPL allows this)

See [CONTRIBUTING.md](CONTRIBUTING.md) for guidelines.

**Build Dokus with us:**

- Star ⭐ the repo (gives visibility)
- Open issues (questions, bugs, ideas)
- Submit PRs (we review quickly)
- Join discussions (community decisions)

---

## 📖 Documentation

- **[Setup Guide](docs/SETUP.md)** - Installation & configuration
- **[Architecture](docs/ARCHITECTURE.md)** - System design & decisions
- **[Database Schema](docs/DATABASE.md)** - PostgreSQL + Exposed ORM
- **[API Reference](docs/API.md)** - REST endpoints & webhooks
- **[Peppol Integration](docs/PEPPOL.md)** - UBL 2.1 + Access Point
- **[Security](docs/SECURITY.md)** - Best practices & audit logging
- **[Deployment](docs/DEPLOYMENT.md)** - Docker, K8s, cloud platforms

---

## 🎯 Roadmap

### ✅ Current (Beta)

- **Multiplatform client apps:** Android, iOS, Desktop, Web (WASM)
- Invoicing with Peppol e-invoicing
- Expense tracking + receipts
- Payment links (Stripe + Mollie)
- Basic reporting
- Multi-tenant security
- Type-safe RPC between client and backend

### 🔄 Coming Soon (Q1 2026)

- Receipt OCR (photo → categorized expense)
- Bank account sync (Plaid/Tink)
- Automated payment reminders
- Recurring invoice templates
- Offline-first support (SQLDelight + sync)
- Platform-specific optimizations (iOS widgets, Android Material You)

### 🚀 Future (2026+)

- Team features (roles, permissions)
- Multi-currency support
- Advanced analytics + forecasting
- Integration marketplace
- White-label reports (for accountants)
- Public API access for third-party integrations
- Wear OS / watchOS companion apps
- Desktop menu bar / system tray integration

---

## 🇧🇪 Belgium 2026 Mandate

**January 1, 2026:** Belgium requires Peppol e-invoicing for all B2B transactions.

**Penalties for non-compliance:**

- €1,500 first offense
- €5,000 subsequent offenses

**Dokus solves this:**

- ✅ Peppol-ready from day 1
- ✅ Automatic compliance (zero jargon)
- ✅ UBL 2.1 XML generation
- ✅ Pagero/EDICOM transmission
- ✅ Delivery confirmation logging

**Don't get caught unprepared.** Deploy Dokus, sleep easy.

---

## 📈 Metrics That Matter

**For Users:**

- ⏱️ **10+ hours saved/month** on bookkeeping
- 💰 **€750-1,500 value** (at €75-150/hour developer rate)
- 📊 **95%+ monthly retention** (if PMF is real)
- 😊 **NPS > 50** (users love it)

**For Contributors:**

- 🌟 **Active community** (daily commits, quick PR reviews)
- 📝 **Great documentation** (easy to understand + contribute)
- 🤝 **Maintainer responsive** (reply within 24h)
- 🎉 **Recognition in hall of fame** (contributor credits)

---

## 🤝 Community

- **GitHub Discussions:** Ask questions, share ideas, debate design
- **GitHub Issues:** Report bugs, request features
- **Contributing:** See CONTRIBUTING.md
- **Hall of Fame:** Top contributors featured in releases

---

## 📄 License

Dokus is licensed under **AGPL v3**.

**What this means:**

- ✅ Use for free (self-host)
- ✅ Modify for your needs
- ✅ Contribute improvements back
- ❌ Can't close-source modifications
- ❌ SaaS users must share their license

[Full License Text](LICENSE)

---

## 🌱 Status

**Current:** Beta (Feature-complete MVP, being tested with real users)

**Stability:** Production-ready for self-hosted deployment

**Data Safety:** We dogfood Dokus with our own finances (high confidence in security)

**Support:** Community-driven (GitHub issues + discussions)

---

## 💬 FAQ

### Q: Is my financial data safe?

**A:** Self-hosting means your data never leaves your server. Multi-tenant isolation ensures data
separation. Full audit logs for compliance. Encryption at rest for sensitive data. More
details: [docs/SECURITY.md](docs/SECURITY.md)

### Q: Can I use Dokus in production?

**A:** Yes. It's being used by beta users with real financial data. AGPL license, PostgreSQL-backed,
audit-logged. That said, we provide no SLA for self-hosted deployments (you're responsible). Test in
staging first.

### Q: How does Peppol transmission work?

**A:** We partner with Pagero/EDICOM (Access Point providers). You host Dokus, Dokus generates UBL
2.1 XML, we transmit via their API. They handle delivery to the recipient's Access Point. Fully
automated.

### Q: Can I self-host backend services on a cheap VPS?

**A:** Yes. Minimum requirements for backend services:

- 2GB RAM
- 1 vCPU
- 20GB storage
- PostgreSQL 17
- Redis 8
- JDK 17+

Runs fine on €5-10/month VPS. Scales to 1000+ users on single instance.

**Note:** The client applications (Android, iOS, Desktop, Web) run on user devices and don't require server hosting.

### Q: Will there be a cloud version?

**A:** Yes! Cloud hosting is coming in early 2026. For now, self-host on your own infrastructure (
cheap VPS, Docker, etc). When cloud launches, you'll have the option of managed hosting for
convenience—but self-hosting will always be free and fully supported.

### Q: What happens to my data if Dokus shuts down?

**A:** Your data is in your PostgreSQL database (you own it). Export anytime (CSV, Excel, PDF). Full
source code is open—you can fork and run it forever.

### Q: Can I contribute?

**A:** **YES PLEASE.** We need:

- **KMP/Compose developers:** UI features for Android, iOS, Desktop, Web
- **Backend developers:** Kotlin/Ktor microservices
- **Platform-specific developers:** iOS (Swift interop), Android (platform APIs)
- **DevOps:** Docker, Kubernetes, deployment automation
- **Writers:** Documentation, tutorials, architecture guides
- **Translators:** Multi-language support (i18n)
- **Testers:** Platform-specific testing, bug reports
- **Designers:** UI/UX improvements for multiplatform consistency

See [CONTRIBUTING.md](CONTRIBUTING.md) to get started.

---

## 🙏 Why We Built This

We're developers. We hate accounting. We hate spending hours every month on invoices, expenses,
taxes. We hate not knowing if we're compliant. We hate paying accountants €2-3K/year for basic
bookkeeping.

So we built Dokus for ourselves. Then we realized thousands of other developers have the same
problem.

**Now we're open-sourcing it** so:

1. We prove we're trustworthy (open code, transparent)
2. Community can help improve it faster
3. No vendor lock-in (you control your data)
4. Belgium developers get Peppol solution before competitors

**Join us.**

---

## 📞 Contact & Support

- **Issues:** GitHub Issues (bugs, features)
- **Discussions:** GitHub Discussions (questions, ideas)
- **Email:** hello@dokus.app
- **Twitter:** [@dokus_app](https://twitter.com/dokus_app)
- **Built in Belgium** 🇧🇪

---

## ⭐ Give Us a Star

If Dokus is useful, star us on GitHub. It helps other developers discover the project.

[![Star us on GitHub](https://img.shields.io/github/stars/dokus/dokus?style=social)](https://github.com/dokus/dokus/stargazers)

---

## 📜 Acknowledgments

- **Peppol Network** - For the open standard
- **Pagero/EDICOM** - For Access Point partnership
- **JetBrains** - For Kotlin Multiplatform and Compose Multiplatform
- **Kotlin Community** - For an amazing multiplatform ecosystem
- **Open-source Contributors** - Building Dokus with us

---

**Stop paying accountants for basic bookkeeping. Stop worrying about compliance deadlines. Start
building.**

**Deploy Dokus. Get back to coding.** 🚀

---

*Dokus: Financial management. Zero accounting jargon. Truly multiplatform.*

*Open-source. Self-hosted. Built with Kotlin Multiplatform. By developers, for developers.*