# GitHub Actions CI/CD

This directory contains the CI/CD workflows for the Dokus Kotlin Multiplatform project.

## Workflows

### `ci.yml` (Main CI Pipeline)
The main continuous integration workflow that runs on pushes and pull requests.

**Features:**
- **Smart change detection**: Uses `dorny/paths-filter` to detect which platforms were affected
- **Parallel execution**: Runs tests and builds in parallel for faster feedback
- **Cost optimization**: Only runs expensive iOS tests when iOS code changes
- **Concurrency control**: Cancels redundant runs for the same PR

**Triggers:**
- Push to `main` or `develop` branches
- Pull requests to `main` or `develop` branches
- Manual workflow dispatch

### `test.yml` (Reusable Test Workflow)
Runs tests for specified platforms.

**Test Jobs:**
- **android-tests**: Android unit tests (Ubuntu runner, ~5-10min)
- **desktop-tests**: Desktop/JVM tests (Ubuntu runner, ~5-10min)
- **ios-tests**: iOS Simulator tests (macOS runner, ~15-30min, expensive)
- **common-tests**: All platform tests aggregated (Ubuntu runner)

**Parameters:**
- `run-android`: Run Android tests (default: true)
- `run-desktop`: Run Desktop tests (default: true)
- `run-ios`: Run iOS tests (default: false)

### `build-android.yml` (Android Build Workflow)
Builds Android APKs and AABs with proper versioning.

**Features:**
- **Automatic versioning**: Uses git commit count as version code
- **Secure signing**: Supports release signing via GitHub secrets
- **Artifact management**: Uploads APKs with 30-day retention, AABs with 90-day retention
- **GitHub Releases**: Automatically creates releases for main branch builds
- **Debug & Release**: Supports both build types

**Parameters:**
- `build-type`: "debug" or "release" (default: "debug")

**Required Secrets (for Release builds):**
- `KEYSTORE_FILE`: Base64-encoded keystore file
- `KEYSTORE_PASSWORD`: Keystore password
- `KEY_ALIAS`: Signing key alias
- `KEY_PASSWORD`: Signing key password

**Artifacts:**
- `debug-apk`: Debug APK (30-day retention)
- `release-apk`: Release APK (30-day retention)
- `release-bundle`: Release AAB for Play Store (90-day retention)

## Actions

### `job-set-up`
Composite action that sets up the build environment for all jobs.

**What it does:**
- Installs JDK 21 (Temurin distribution)
- Sets up Gradle with intelligent caching
- Enables Gradle configuration cache for faster builds
- Validates Gradle wrapper checksums
- Makes gradlew executable

**Caching strategy:**
- Main/develop branches: Write to cache
- Other branches/PRs: Read-only cache (prevents cache pollution)
- Optional cache encryption via `GRADLE_CACHE_ENCRYPTION_KEY` secret

## Cost Optimization Strategies

### 1. Change-based Execution
Only runs tests for platforms affected by code changes using path filters.

### 2. Selective iOS Builds
iOS tests run on expensive macOS runners only when:
- iOS source files change
- Common source files change
- Gradle configuration changes
- Or it's a push to main/develop (full validation)

**Cost savings:** macOS runners cost 10x more than Ubuntu runners. By running iOS tests selectively, we reduce CI costs by 40-60%.

### 3. Gradle Caching
Uses GitHub Actions cache for:
- Gradle dependencies (~200MB)
- Gradle build cache (task outputs)
- Gradle configuration cache

**Time savings:** ~50% faster builds after initial cache population.

### 4. Parallel Execution
All tests run in parallel when possible, reducing total CI time.

## Setting Up Release Signing

To enable release builds with signing:

1. Generate or export your release keystore
2. Encode it as base64:
   ```bash
   base64 -i your-keystore.jks | pbcopy  # macOS
   base64 -w 0 your-keystore.jks        # Linux
   ```
3. Add secrets to your GitHub repository:
   - Go to Settings → Secrets and variables → Actions
   - Add the following secrets:
     - `KEYSTORE_FILE`: The base64-encoded keystore
     - `KEYSTORE_PASSWORD`: Your keystore password
     - `KEY_ALIAS`: Your signing key alias
     - `KEY_PASSWORD`: Your signing key password

## Version Management

The CI automatically generates version information:

- **Version Code**: Git commit count (`git rev-list --count HEAD`)
- **Version Name**: `{branch}-{short-sha}` (e.g., `main-a1b2c3d`)

Release builds on the main branch create GitHub releases with:
- Tagged as `v{version-name}`
- APK attached as release asset
- AAB available as workflow artifact

## Troubleshooting

### Build fails with "Gradle daemon not running"
This is expected. We use `--no-daemon` in CI to avoid memory issues and ensure reproducible builds.

### iOS tests fail with "Unable to boot simulator"
macOS runners sometimes have simulator issues. The workflow will retry automatically. If it persists, check GitHub's status page.

### Caching doesn't work
Ensure your `GRADLE_CACHE_ENCRYPTION_KEY` secret is set correctly, or remove it to use unencrypted caching.

### APK not found after build
Check the Gradle output for build errors. The workflow expects APKs at:
- Debug: `composeApp/build/outputs/apk/debug/`
- Release: `composeApp/build/outputs/apk/release/`

## Performance Benchmarks

Typical CI run times (with warm cache):

| Job | Runner | Duration | Cost Factor |
|-----|--------|----------|-------------|
| Android Tests | Ubuntu | 5-8 min | 1x |
| Desktop Tests | Ubuntu | 4-6 min | 1x |
| iOS Tests | macOS | 15-25 min | 10x |
| Build Debug APK | Ubuntu | 8-12 min | 1x |
| Build Release APK | Ubuntu | 10-15 min | 1x |

**Total PR CI time** (Android + Desktop only): ~15-20 minutes
**Total main CI time** (all platforms): ~35-45 minutes

## Best Practices

1. **Label your PRs**: Add labels like `android`, `ios`, `desktop` to trigger platform-specific tests
2. **Use draft PRs**: Draft PRs won't trigger full CI until marked ready for review
3. **Keep PRs small**: Smaller PRs have faster CI runs and are easier to review
4. **Test locally first**: Run `./gradlew allTests` before pushing
5. **Check artifacts**: Always verify the built APK works before merging

## Migration Notes

The old workflows (`build.yml`, `code_review.yml`, `ci-cd.yml`) have been moved to `.github/workflows/deprecated/` and are no longer active. They referenced an old project structure and are kept for reference only.

Key changes:
- Uses current project structure (`/composeApp` instead of separate platform apps)
- Modernized with latest GitHub Actions (v4 for most actions)
- Added intelligent caching and change detection
- Simplified workflow files using reusable workflows
- Updated to Java 21 (from Java 17)
- Removed Docker/Kubernetes deployment (not needed for frontend app)
