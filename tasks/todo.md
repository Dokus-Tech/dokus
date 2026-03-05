# Code Review Fixes — `feature/show-recoverable-peppol-error`

## Tasks
- [x] 1. Move `runSuspendCatching` to `foundation/backend-common` and fix all peppol usages
- [x] 2. Change `PeppolDirectoryUnavailable` to `recoverable = true`
- [x] 3. Scope `RecommandApiException` mapping by HTTP status code
- [x] 4. Add TODO to `OptOutPeppolUseCase`
- [x] 5. Stop leaking raw exception messages in `toPeppolRegistrationRouteException`
- [x] 6. Add companion object constants to `PeppolDirectoryUnavailable`
- [x] 7. Add loading guard on retry button in `SetupErrorContent`

## Review Summary

### Changes made (22 files)
- Moved `RunSuspendCatching.kt` from `backendApp/util/` → `foundation/backend-common/utils/`
- Updated 5 backendApp imports to point to new location
- Replaced `runCatching` → `runSuspendCatching` in all suspend functions across 8 peppol module files
- Left 2 non-suspend `runCatching` calls untouched (RecommandMapper, PeppolWebhookSyncService.parseTimestamp)
- `PeppolDirectoryUnavailable`: `recoverable = false` → `true`, added companion object with `HTTP_STATUS`/`ERROR_CODE`
- `toPeppolVerificationException`: maps 5xx → PeppolDirectoryUnavailable, 4xx → InternalError
- `toPeppolRegistrationRouteException`: logs raw message server-side, passes only operation context to InternalError
- `SetupErrorContent`: added `retryClicked` local state to guard double-tap
- Added TODO to `OptOutPeppolUseCase` for future Settings integration
- Updated 4 test files to match new behavior
