# Contributing to Dokus

First off, **thank you** for considering contributing to Dokus! 🙏

With less than 2 months until Belgium's mandatory e-invoicing deadline, we need all the help we can get to
deliver a solid PEPPOL solution for Belgian freelancers.

## 📅 Current Status (November 2025)

**Code releases publicly in December 2025.** Until then, here's how you can contribute:

### Before December Release

Even without code access, you can help shape Dokus:

1. **⭐ Star the repository** - Helps gauge interest and motivates development
2. **📝 Open issues** for:
    - Feature requests (what would make this perfect for your workflow?)
    - Questions about architecture or implementation
    - PEPPOL compliance requirements we might have missed
    - Belgian tax/VAT edge cases to consider
3. **💬 Start discussions** about:
    - API design preferences
    - UI/UX suggestions for the multiplatform apps
    - Integration needs (banks, payment providers, accounting software)
    - Your current invoicing pain points
4. **📢 Spread the word** - Share with other Belgian freelancers who need this

### After December Release

Once code is public, we'll need help with:

- 🐛 Bug fixes (especially platform-specific issues)
- ✨ Feature implementation
- 🌍 Translations (NL/FR priority)
- 📚 Documentation improvements
- 🧪 Testing on different platforms (Android, iOS, Windows, macOS, Linux, Web)
- 🔍 Security reviews (especially important for financial software)

## 🛠️ Development Setup (Coming December)

Full setup instructions will be provided with the code release. Expected requirements:

### Prerequisites

- JDK 17+
- Android Studio (for Android/iOS development)
- Docker & Docker Compose (for backend services)
- PostgreSQL (for local development)

### Tech Stack You'll Work With

- **Kotlin Multiplatform** - Shared business logic
- **Compose Multiplatform** - UI across all platforms
- **Ktor** - Backend services
- **KotlinX RPC** - Type-safe service communication
- **PostgreSQL** - Data persistence
- **Redis** - Caching

## 📋 How to Contribute

### Reporting Bugs

Before creating bug reports, please check existing issues. When you create a bug report, include:

- **Clear title** describing the issue
- **Platform** where the bug occurs (Android/iOS/Desktop/Web)
- **Steps to reproduce**
- **Expected behavior**
- **Actual behavior**
- **Screenshots** if applicable
- **System information** (OS version, device type)

### Suggesting Features

Feature requests are welcome! Please:

- **Check existing issues** first
- **One feature per issue**
- **Explain the problem** this feature would solve
- **Describe your ideal solution**
- **Consider PEPPOL compliance** - we can't compromise on this

### Pull Requests (After December)

1. Fork the repo
2. Create your feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add some amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

#### PR Guidelines

- **One feature/fix per PR** - Makes review easier
- **Write tests** - Especially for financial calculations
- **Update documentation** - If you change APIs
- **Follow existing patterns** - Consistency matters
- **Platform testing** - Test on at least 2 platforms if doing UI work

## 💻 Code Style

### Kotlin Style Guide

We follow [Kotlin official style guide](https://kotlinlang.org/docs/coding-conventions.html) with
these additions:

```kotlin
// ✅ Good: Descriptive names
fun calculateVatAmount(invoiceTotal: BigDecimal, vatRate: BigDecimal): BigDecimal

// ❌ Bad: Unclear abbreviations
fun calcVat(tot: BigDecimal, rt: BigDecimal): BigDecimal

// ✅ Good: Explicit types for public APIs
val invoiceTotal: BigDecimal = calculateTotal()

// ✅ Good: Use data classes for DTOs
data class Invoice(
    val id: UUID,
    val clientId: UUID,
    val amount: BigDecimal,
    val vatAmount: BigDecimal,
    val createdAt: Instant
)
```

### Commit Messages

Follow [Conventional Commits](https://www.conventionalcommits.org/):

```
feat: add PEPPOL UBL 2.1 export
fix: correct VAT calculation for mixed rates
docs: update deployment instructions
test: add tests for bank matching algorithm
chore: update Compose Multiplatform to 1.9.1
```

### Testing Requirements

- **Financial calculations** - Must have unit tests with edge cases
- **PEPPOL compliance** - Must include integration tests
- **Platform-specific code** - Must be tested on target platform
- **Database operations** - Must handle multi-tenancy correctly

## 🎯 Priority Areas

Given our deadline, we're focusing on:

1. **PEPPOL integration** (Critical path)
2. **Invoice creation and management**
3. **Document upload and storage**
4. **Platform-specific UI polish**
5. **Belgian tax compliance edge cases**

## 🚫 What NOT to Work On

To maintain focus, please avoid:

- Features beyond MVP scope (payment gateways, advanced reporting)
- Major architectural changes without discussion
- Non-Belgian tax systems (for now)
- Complex accounting features (we're keeping it simple)

## 🤝 Code of Conduct

### Our Standards

- **Be respectful** - We're all trying to meet a deadline
- **Be constructive** - Criticism is fine, but offer solutions
- **Be inclusive** - Welcome to all skill levels
- **Be patient** - We're racing against time, but not at the cost of quality
- **Think security-first** - This handles financial data

### Unacceptable Behavior

- Harassment or discrimination
- Publishing private information
- Trolling or insulting comments
- Anything that compromises financial data security

## 📜 License

By contributing, you agree that your contributions will be licensed under
the [AGPL v3 License](LICENSE).

This means:

- Your code remains open-source
- Any derivatives must also be AGPL v3
- SaaS deployments must share source code

## 🙋 Questions?

- **General questions** → Open a [Discussion](https://github.com/Dokus-Tech/dokus/discussions)
- **Bug or feature** → Open an [Issue](https://github.com/Dokus-Tech/dokus/issues)
- **Security issues** → Email artem@invoid.vision directly (DO NOT open public issues)

## 🚀 Why Contribute?

- **Real impact** - Help thousands of Belgian freelancers meet compliance
- **Resume builder** - Contribute to a production financial system
- **Learn KMP** - Work with cutting-edge Kotlin Multiplatform
- **Be early** - Shape the project from the beginning
- **Recognition** - Contributors listed in releases and README

## ⏰ The Clock is Ticking

We have less than 2 months to deliver a working PEPPOL solution. Every contribution, no matter how small,
brings us closer to helping Belgian freelancers avoid those €1,500-5,000 fines.

Let's build this together! 🇧🇪

---

**Thank you for helping make financial management less painful for developers!**

*Questions? Reach out in [Discussions](https://github.com/Dokus-Tech/dokus/discussions) or email
artem@invoid.vision*