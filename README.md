# Dokus

Dokus is a Kotlin Multiplatform financial management tool designed specifically for Belgian IT freelancers and independent contractors.

Managing finances as a freelancer is complex—tracking income, expenses, VAT, social contributions, and generating compliant invoices. Unlike generic accounting software, Dokus understands Belgian tax regulations and freelancer workflows, helping you stay compliant while focusing on your work.

**Key differentiator:** Dokus is the only open source financial tool with built-in Peppol e-invoicing support, preparing you for Belgium's mandatory B2B e-invoicing deadline (January 1, 2026).

<p align="center">
  <img src="docs/demo.gif" alt="Dokus Demo" width="80%" />
  <br>
  <em>Create compliant invoices, track expenses, and manage finances—all in one place</em>
</p>

---

<p align="center">
  <a href="https://github.com/yourusername/dokus/actions">
    <img src="https://github.com/yourusername/dokus/workflows/CI/badge.svg" alt="Build Status" />
  </a>
  <a href="https://github.com/yourusername/dokus/blob/main/LICENSE">
    <img src="https://img.shields.io/badge/license-MIT-blue.svg" alt="License: MIT" />
  </a>
  <a href="https://github.com/yourusername/dokus/releases">
    <img src="https://img.shields.io/github/v/release/yourusername/dokus?color=blue" alt="Latest Release" />
  </a>
  <a href="https://github.com/yourusername/dokus/stargazers">
    <img src="https://img.shields.io/github/stars/yourusername/dokus?style=social" alt="GitHub Stars" />
  </a>
  <a href="https://discord.gg/your-invite">
    <img src="https://img.shields.io/discord/your-id?color=7289da&logo=discord&logoColor=white&label=discord" alt="Discord" />
  </a>
  <a href="https://kotlinlang.org">
    <img src="https://img.shields.io/badge/kotlin-2.0.0-blue.svg?logo=kotlin" alt="Kotlin 2.0" />
  </a>
  <img src="https://img.shields.io/badge/platform-Android%20%7C%20iOS%20%7C%20Desktop%20%7C%20Web-lightgrey" alt="Platforms" />
</p>

---

## 🚀 Quick Start

### For Users (Belgian Freelancers)

Get started immediately with Dokus:

- 🌐 **Web Demo:** [Try Dokus](https://dokus.app/demo)
- 🤖 **Android:** [Download APK](https://github.com/yourusername/dokus/releases/latest)
- 🍎 **iOS:** [TestFlight Beta](https://testflight.apple.com/join/your-code)
- 💻 **Desktop:**
    - [Windows Installer](https://github.com/yourusername/dokus/releases/latest/download/dokus-windows.exe)
    - [macOS DMG](https://github.com/yourusername/dokus/releases/latest/download/dokus-macos.dmg)
    - [Linux AppImage](https://github.com/yourusername/dokus/releases/latest/download/dokus-linux.AppImage)
- ☁️ **Cloud:** [Start 30-Day Free Trial](https://dokus.app/signup) (€15/month after trial)

### For Developers

Get the code and start contributing:

```bash
# Clone the repository
git clone https://github.com/yourusername/dokus.git
cd dokus

# Run the desktop application
./gradlew :composeApp:run

# Run the backend server
./gradlew :server:run
```

See [CONTRIBUTING.md](CONTRIBUTING.md) for detailed development setup, architecture decisions, and contribution guidelines.

---

## ✨ Features

- 🧾 **Smart Invoicing** - Create professional, VAT-compliant invoices in seconds with automatic numbering and Belgian formatting
- 📨 **Peppol E-Invoicing** - Ready for Belgium's 2026 mandatory B2B e-invoicing with built-in AS4 message handling
- 💰 **Automatic BTW Calculation** - Handles 21%, 12%, 6% rates with IC/EX codes for intra-community transactions
- 📊 **Real-Time Tax Estimates** - See your quarterly tax liability update in real-time as you add income and expenses
- 📷 **Receipt OCR** - Snap photos of receipts and watch them automatically categorize and extract amounts
- 🏦 **Bank Sync** - Connect to Belgian banks (BNP Paribas, ING, KBC, Belfius) or import CSV statements
- 💸 **Instant Payment Links** - Add Stripe/Mollie payment buttons to invoices for instant client payments
- 📈 **Belgian Tax Reporting** - P&L statements, quarterly VAT returns, and annual tax summaries
- 📱 **True Cross-Platform** - Same features on Android, iOS, Desktop (Windows/macOS/Linux), and Web
- 🔒 **Privacy-First** - Self-host with full control, or use managed cloud—your choice
- 🌐 **Full API Access** - RESTful API with webhooks for custom integrations and automation
- 🇧🇪 **Belgian-Specific** - Built for Belgian freelancers, by Belgian freelancers—understands local regulations

---

## 🛠️ Built With

### Why Kotlin Multiplatform?

Dokus uses Kotlin Multiplatform to share business logic across all platforms while maintaining native performance and user experience:

- **Single Codebase** - Write business logic once, run everywhere (Android, iOS, Desktop, Web)
- **Native Performance** - Compiles to native code on each platform, no JavaScript bridge overhead
- **Type Safety** - Catch errors at compile time with type-safe SQL (SQLDelight) and HTTP (Ktor)
- **Shared Validation** - Belgian tax rules, VAT calculations, and Peppol logic shared across all platforms
- **Developer Productivity** - Fix bugs once instead of four times, add features simultaneously everywhere

### Technology Stack

**Shared Core:**
- [Kotlin Multiplatform](https://kotlinlang.org/docs/multiplatform.html) - Shared business logic
- [Compose Multiplatform](https://www.jetbrains.com/lp/compose-multiplatform/) - Declarative UI framework
- [SQLDelight](https://cashapp.github.io/sqldelight/) - Type-safe SQL with shared queries
- [Ktor](https://ktor.io/) - HTTP client and server
- [Koin](https://insert-koin.io/) - Dependency injection
- [kotlinx.serialization](https://github.com/Kotlin/kotlinx.serialization) - JSON serialization
- [kotlinx.datetime](https://github.com/Kotlin/kotlinx-datetime) - Date/time handling

**Backend:**
- [Ktor Server](https://ktor.io/) - Async HTTP server
- [Exposed ORM](https://github.com/JetBrains/Exposed) - Database access
- [PostgreSQL](https://www.postgresql.org/) - Production database
- [Redis](https://redis.io/) - Caching and sessions

**Integrations:**
- **Peppol AS4** - E-invoicing protocol implementation
- **Stripe & Mollie** - Payment processing
- **Bank APIs** - Belgian bank integrations (planned)

### Architecture

```
dokus/
├── composeApp/          # Main application (all platforms)
│   ├── commonMain/      # Shared UI and business logic
│   ├── androidMain/     # Android-specific code
│   ├── iosMain/         # iOS-specific code (SwiftUI wrapper)
│   ├── desktopMain/     # Desktop-specific code
│   └── wasmJsMain/      # Web-specific code
├── application/         # Feature modules
│   ├── core/            # Shared utilities
│   ├── onboarding/      # User onboarding flow
│   ├── home/            # Dashboard
│   ├── contacts/        # Client management
│   ├── cashflow/        # Invoicing and expenses
│   └── banking/         # Bank integration
├── foundation/          # Foundation layer
│   ├── ui/              # Design system
│   ├── domain/          # Business models
│   ├── platform/        # Platform abstractions
│   └── apispec/         # API specifications
└── server/              # Backend services
    ├── auth/            # Authentication
    ├── invoicing/       # Invoice management
    ├── expenses/        # Expense tracking
    └── peppol/          # Peppol integration
```

**Design Principles:**
- **Feature Modules** - Each feature is self-contained and independently testable
- **Clean Architecture** - Clear separation between UI, business logic, and data layers
- **Multi-Tenancy** - Every query filtered by tenant for security
- **Type Safety** - Compile-time verification across the entire stack

---

## 🤝 Contributing

We love contributions from the community! Whether you're:

- 🐛 Reporting bugs and issues
- 💡 Suggesting new features or improvements
- 📝 Improving documentation
- 🌍 Translating to Dutch, French, or other languages
- 💻 Writing code and submitting pull requests

**Your help is welcome.**

### Good First Issues

New to the project? Look for issues labeled [`good-first-issue`](https://github.com/yourusername/dokus/labels/good-first-issue). These are:

- ✅ Well-defined with clear acceptance criteria
- ✅ Suitable for newcomers to the codebase
- ✅ Come with mentoring offers from maintainers
- ✅ Reviewed within 24 hours

### Development Setup

See our comprehensive [CONTRIBUTING.md](CONTRIBUTING.md) guide for:

- 🛠️ Development environment setup (Kotlin, Android Studio, Xcode)
- 🏗️ Architecture decisions and design patterns
- 📋 Code standards and style guide
- 🧪 Testing guidelines (unit, integration, e2e)
- 📝 Pull request process and review expectations
- 🚀 Deployment and release process

### Quick Dev Setup

```bash
# Install dependencies
./gradlew build

# Run tests
./gradlew test

# Run desktop app
./gradlew :composeApp:run

# Run backend server
./gradlew :server:run
```

### Code of Conduct

This project follows the [Contributor Covenant](CODE_OF_CONDUCT.md) Code of Conduct. Please be respectful and welcoming to all contributors.

---

## 🗺️ Roadmap

### ✅ Completed (v0.1.0 - October 2025)

- [x] Multi-platform app (Android, iOS, Desktop, Web)
- [x] Invoice creation with Belgian VAT rates
- [x] Expense tracking with categories
- [x] Client management
- [x] Basic P&L reports
- [x] CSV export for accounting
- [x] Multi-tenant architecture

### 🚧 In Progress (v0.2.0 - November 2025)

- [ ] Peppol AS4 e-invoicing integration
- [ ] Bank CSV import and reconciliation
- [ ] Receipt OCR with photo upload
- [ ] Payment link integration (Stripe/Mollie)
- [ ] Mobile app refinements
- [ ] Dark mode support

### 📋 Planned (v0.3.0 - December 2025)

- [ ] Real-time bank API integration (Belgian banks)
- [ ] Quarterly VAT filing assistant
- [ ] Automated payment reminders
- [ ] Multi-currency support (for international clients)
- [ ] Advanced analytics dashboard
- [ ] Accountant portal and sharing

### 💭 Future Ideas (2026+)

- [ ] Time tracking and project management
- [ ] Team collaboration features
- [ ] AI-powered expense categorization
- [ ] Recurring invoice templates
- [ ] Mobile receipt scanning improvements
- [ ] Integration marketplace (Xero, QuickBooks export)

Have ideas for features? [Open a feature request](https://github.com/yourusername/dokus/issues/new?template=feature_request.md)!

---

## 🇧🇪 Why Belgium 2026?

On **January 1, 2026**, Belgium mandates Peppol e-invoicing for all B2B transactions. Non-compliance carries penalties of **€1,500 (first offense)** up to **€5,000 (repeat offenses)**.

**Dokus is Peppol-ready from day one**, so you can:
- ✅ Send compliant e-invoices through the Peppol network
- ✅ Receive e-invoices from clients automatically
- ✅ Avoid penalties and last-minute scrambling
- ✅ Take advantage of 120% tax deductibility (through 2028)

**Software costs are 120% tax deductible in Belgium through 2028**, making Dokus effectively cost ~€7/month after tax benefits.

---

## 💎 Why Open Source?

**Financial software should be transparent.**

When software handles your money, invoices, expenses, and tax calculations, you deserve to:

- 🔍 **Audit the code** - Verify calculations are correct and secure
- 🔒 **Trust the security** - No hidden backdoors or data collection
- 🆓 **Own your data** - Self-host and maintain complete control
- 🤝 **Shape the future** - Contribute features you need
- 🌍 **Support the community** - Help other freelancers succeed

Dokus is MIT licensed for maximum freedom. Use it, modify it, contribute back—or don't. It's your choice.

---

## 📄 License

Dokus is licensed under the [MIT License](LICENSE). This means you can:

- ✅ Use it commercially
- ✅ Modify the source code
- ✅ Distribute your modifications
- ✅ Use it privately
- ✅ Sublicense it

The only requirement: Include the original license and copyright notice.

---

## 🔗 Links

- **Website:** [dokus.app](https://dokus.app)
- **Documentation:** [docs.dokus.app](https://docs.dokus.app)
- **Discord Community:** [Join us](https://discord.gg/your-invite)
- **Twitter/X:** [@dokus_app](https://twitter.com/dokus_app)
- **LinkedIn:** [Dokus](https://linkedin.com/company/dokus)
- **Blog:** [dokus.app/blog](https://dokus.app/blog)

---

## 💬 Support

### Free Community Support

- **Discord:** [Join our community](https://discord.gg/your-invite) for help, discussions, and networking
- **GitHub Issues:** [Report bugs](https://github.com/yourusername/dokus/issues/new?template=bug_report.md) or [request features](https://github.com/yourusername/dokus/issues/new?template=feature_request.md)
- **Discussions:** [Community Q&A](https://github.com/yourusername/dokus/discussions)

### Cloud Support (Paid)

- **Email Support:** support@dokus.app (48-hour response time)
- **Priority Support:** Available on Business plan (24-hour response)

---

## ⭐ Star History

If you find Dokus useful, please star the repository! It helps others discover the project.

[![Star History Chart](https://api.star-history.com/svg?repos=yourusername/dokus&type=Date)](https://star-history.com/#yourusername/dokus&Date)

---

## 🙏 Acknowledgments

Built with love for Belgian IT freelancers, by freelancers who understand the pain of quarterly accounting.

Special thanks to:
- The Kotlin Multiplatform team for an amazing framework
- JetBrains for Compose Multiplatform
- The open source community for inspiration and support
- Early adopters and beta testers for invaluable feedback

---

## 📊 Project Status

**Active Development** - Dokus is under active development with weekly releases. We're working toward a v1.0 stable release in Q1 2026.

- 🚀 **Latest Release:** [v0.1.0](https://github.com/yourusername/dokus/releases/latest)
- 📅 **Next Release:** v0.2.0 (November 2025) - Peppol integration
- 🎯 **Roadmap Progress:** [View Milestones](https://github.com/yourusername/dokus/milestones)

---

<p align="center">
  <strong>Made with ❤️ for Belgian IT freelancers</strong>
  <br><br>
  <a href="https://github.com/yourusername/dokus/stargazers">⭐ Star us on GitHub</a>
  •
  <a href="https://discord.gg/your-invite">💬 Join Discord</a>
  •
  <a href="https://dokus.app">🌐 Try Dokus</a>
</p>

---

<p align="center">
  <sub>Dokus • Open Source Financial Management • Peppol Ready • Belgium 2026 Compliant</sub>
</p>