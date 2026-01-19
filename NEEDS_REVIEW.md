# Needs Review

## Overview
This file tracks refactoring attempts that failed twice and need manual review.

---

## Protocol
If a refactoring attempt fails the build twice:
1. Revert all changes
2. Log the issue here with details
3. Move to the next item
4. Come back later with fresh eyes or seek help

---

## Items Needing Review

### CashflowRemoteDataSourceImpl.kt - 2026-01-19
**Planned Action:** Split into 8 separate data source implementations
**Status:** Intentionally skipped
**Reason:** Original developers already analyzed and decided against splitting:
- File has `@Suppress("LargeClass")` annotation with comment: "Single API facade; split would add indirection without reducing IO surface."
- File has `@Suppress("TooManyFunctions")` annotation with comment: "Implementation mirrors interface methods"
- All 70+ methods are thin HTTP wrappers with identical patterns
- Splitting would require:
  - Splitting the `CashflowRemoteDataSource` interface
  - Updating DI module registrations
  - Adding delegation/composition in consuming code
- Risk-benefit analysis: High risk of breaking changes, low benefit since all methods are simple HTTP calls
**Recommendation:** Keep as-is - the original developers' reasoning is sound

---

## Template
```
### [File Name] - [Date]
**Attempted Action:** [Description]
**Failure 1:** [Error message/description]
**Failure 2:** [Error message/description]
**Reverted:** Yes
**Analysis:** [Why it might have failed]
**Suggested Fix:** [Ideas for resolution]
```
