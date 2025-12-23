## [0.1.3] - 23-12-2025

### ✨ New Features
- 🎉 **Contacts Management**: Implement comprehensive contacts management UI with desktop panes and mobile bottom sheets for enhanced user experience across all platforms
- 🎉 **Folder Drag-and-Drop**: Enable folder drag-and-drop uploads for desktop, streamlining the file upload experience
- **Tag-Based Release Workflow**: Add tag-based release pipeline to Bitrise with automated artifact naming for Android, iOS, and macOS platforms

### 🔧 Improvements
- **Design System Enhancements**: Add TextFieldPhone component and generic PFilterDropdown component for consistent UI patterns
- **TextField Focus States**: Add focus border color to TextField for improved accessibility and visual feedback
- **Button Touch Targets**: Increase PBackButton touch target to 44dp for better mobile usability
- **Navigation Refactoring**: Separate navigation providers and simplify invoice exit flow by providing LocalNavController in Home screen
- **Auth Loading State**: Pass isLoading prop to PPrimaryButton for better user feedback during authentication flows
- Resource cleanup and import resolution

### 🔨 Other Changes
- Repository reset and workflow cleanup for CI/CD optimization

## [0.1.2] - 22-12-2025

### 📚 Documentation

- 🎉 **Comprehensive error handling guide**: Created detailed ERROR_HANDLING.md documenting DokusException patterns, client-side error handling via HttpClientExtensions, and backend error configuration
- Added rate limiting documentation for authentication module
- Added error handling section to cashflow backend README
- Documented common error scenarios and debugging approaches
- Added section documenting DokusErrorContent and DokusErrorText components
- Added dedicated documentation for TooManyLoginAttempts exception
- Documented DokusExceptionExtensions.localized functionality

### ✨ Code Documentation

- 🔍 **Enhanced API documentation**: Added comprehensive KDoc to core use cases and domain layer with detailed @param and @return documentation
- Added comprehensive KDoc to SelectTenantUseCase interface
- Added comprehensive KDoc to GetCurrentTenantUseCase
- Added @param and @return documentation to LoginUseCase invoke method
- Added @return documentation to LogoutUseCase invoke method
- Added @param and @return documentation to RegisterAndLoginUseCase
- Added @param and @return documentation to SelectTenantUseCaseImpl
- Added @return documentation to GetCurrentTenantUseCaseImpl
- Added comprehensive @param and @return documentation to additional use cases
- Added comprehensive KDoc to RegisterFormFields class with best practices and anti-patterns section

### 🔧 Maintenance

- Updated .gitignore

## [0.1.1] - 22/12/2025

### ✨ New Features
- 🎨 **Design System**: Implement calm, minimal visual baseline for improved UI consistency and aesthetics
- 🏷️ **Release Workflow**: Implement tag-based release workflow with automated deployment package creation
- Loading indicator added to PButton component for enhanced user feedback during async operations

### 🔧 Changed
- Frontend apps now use version from git tags instead of manual version management
- Release process moved from CI workflow to dedicated tag-triggered release workflow

### 🐛 Fixed
- Remove unused major/minor/build version components
- Build and import issues

# Changelog

All notable changes to Dokus are documented here.

## [Unreleased]

Currently working towards the December 2025 MVP release with PEPPOL invoicing support.

### What's New
- Multi-platform apps for Android, iOS, Desktop, and Web
- Complete backend infrastructure for Belgian freelancers
- PEPPOL e-invoicing integration (Belgium 2026 compliance)
- Self-hosted deployment with Docker
- Multi-tenant architecture for secure data isolation
- Comprehensive security with encryption and audit logging

## [0.1.0] - December 2025 (Planned)

The first public release focused on Belgium's January 2026 PEPPOL mandate.

### Features
- **PEPPOL Invoicing** - Create and send compliant e-invoices
- **Multi-Platform Apps** - Android, iOS, Desktop, and Web from one codebase
- **Document Storage** - Upload and manage invoices and receipts
- **Client Management** - Track your customers and their details
- **Belgian VAT Support** - Automatic calculations with 21%, 12%, and 6% rates
- **Self-Hosting** - Run on your own server for complete control

### Security
- End-to-end encryption for sensitive data
- Multi-tenant data isolation
- Complete audit trail for compliance
- Secure password storage
- HTTPS/TLS enforcement

---

**What's Coming Next?**

Check out our [roadmap](README.md#roadmap) to see what features we're building in 2026.
