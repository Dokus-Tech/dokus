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

### Updating the Changelog

Keep [CHANGELOG.md](CHANGELOG.md) up-to-date with user-friendly descriptions of your changes:

**When to update:**
- New features
- Bug fixes users should know about
- Breaking changes
- Security fixes

**How to write entries:**
- Write for end users, not developers
- Focus on what changed, not how
- Use clear, simple language
- Group related changes together

**Examples:**

```markdown
✅ Good (user-friendly):
- Fixed crash when exporting invoices on Android
- Added support for multi-currency invoices
- Improved dashboard loading speed by 2x

❌ Avoid (too technical):
- Refactored InvoiceRepository.kt to use coroutines
- Updated Gradle to 8.5
- Fixed NPE in line 234 of MainActivity.kt
```

**Format:**

Add your entry to the `[Unreleased]` section:

```markdown
## [Unreleased]

### What's New
- Your new feature here

### Fixed
- Your bug fix here

### Security
- Your security improvement here
```

### Creating a Release

Dokus uses **tag-based releases**. GitHub releases are only created when you push a version tag.

**How it works:**
1. Pushing code to `main` runs tests and builds but does NOT create a release
2. Pushing a version tag (e.g., `v1.0.0`) triggers the release workflow
3. The release version comes from the tag name, not auto-generated

**Creating a release:**

```bash
# 1. Make sure you're on main and CI is passing
git checkout main
git pull origin main

# 2. Create an annotated tag with semantic version
git tag v1.0.0 -a -m "Release v1.0.0: Description of changes"

# 3. Push the tag to trigger the release workflow
git push origin v1.0.0

# 4. Monitor the release in GitHub Actions
```

**Semantic Versioning:**

We follow [Semantic Versioning](https://semver.org/) (MAJOR.MINOR.PATCH):

| Version Bump | When to Use | Example |
|--------------|-------------|---------|
| MAJOR (`v2.0.0`) | Breaking changes, incompatible API changes | Removing features, changing data formats |
| MINOR (`v1.1.0`) | New features, backward-compatible additions | New endpoints, new UI features |
| PATCH (`v1.0.1`) | Bug fixes, backward-compatible fixes | Security patches, crash fixes |

**Pre-release Tags:**

For testing releases before production:

```bash
# Beta release
git tag v1.0.0-beta.1 -a -m "Beta release for testing"

# Release candidate
git tag v1.0.0-rc.1 -a -m "Release candidate 1"
```

Pre-release tags (containing `-alpha`, `-beta`, or `-rc`) are automatically marked as pre-release in GitHub.

**Fixing Tag Mistakes:**

If you pushed a tag with the wrong version or message:

```bash
# Delete the tag locally
git tag -d v1.0.0

# Delete the tag from remote
git push origin :refs/tags/v1.0.0

# Delete the release from GitHub UI (if created)
# Then create the correct tag
git tag v1.0.1 -a -m "Correct version"
git push origin v1.0.1
```

**Important Notes:**
- Always ensure CI passes on `main` before creating a release tag
- Use annotated tags (`-a` flag) with descriptive messages
- Don't use auto-generated commit-count versions (e.g., v0.0.123)
- Release notes are auto-generated from commits since the last release

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

This project adheres to the [Contributor Covenant Code of Conduct](CODE_OF_CONDUCT.md).

By participating, you are expected to uphold this code. Please report unacceptable behavior to artem@invoid.vision.

**In short:**
- **Be respectful** - We're all trying to meet a deadline
- **Be constructive** - Criticism is fine, but offer solutions
- **Be inclusive** - Welcome to all skill levels
- **Be patient** - We're racing against time, but not at the cost of quality
- **Think security-first** - This handles financial data

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