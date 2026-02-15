# Error Handling Reference

This document explains the current error-handling architecture used by Dokus backend and clients.

## Source of Truth

Core exception model:
- `foundation/domain/src/commonMain/kotlin/tech/dokus/domain/exceptions/DokusException.kt`
- `foundation/domain/src/commonMain/kotlin/tech/dokus/domain/exceptions/DokusExceptionExtensions.kt`

Backend HTTP mapping:
- `foundation/backend-common/src/main/kotlin/tech/dokus/foundation/backend/configure/ErrorHandling.kt`

Client-side localized rendering:
- `foundation/aura/src/commonMain/kotlin/tech/dokus/foundation/aura/extensions/DokusExceptionExtensions.kt`
- `foundation/aura/src/commonMain/kotlin/tech/dokus/foundation/aura/components/common/ErrorBox.kt`
- `foundation/aura/src/commonMain/composeResources/values/exceptions.xml`

Rate limiting implementation:
- `backendApp/src/main/kotlin/tech/dokus/backend/services/auth/RedisRateLimitService.kt`
- `foundation/backend-common/src/main/kotlin/tech/dokus/foundation/backend/middleware/RateLimitPlugin.kt`

## Error Model

All domain errors should be represented as `DokusException` (or wrapped into it).

Key properties:
- Stable error code.
- Human-readable message.
- HTTP status mapping.
- Recoverability hint when retry may be meaningful.

## Backend Behavior

`configureErrorHandling()` registers centralized handling so routes/services can throw domain exceptions directly.

High-level mapping:
- Validation and bad request -> `400`
- Authentication failure -> `401`
- Authorization failure -> `403`
- Not found -> `404`
- Conflict -> `409`
- Too many attempts / throttling -> `429`
- Unexpected internal failure -> `500`

Guidelines:
- Throw specific domain exceptions for expected business failures.
- Avoid leaking internal stack traces in API responses.
- Keep error messages actionable for UI and logs.

## Rate Limiting and `429`

Authentication and related sensitive flows may return `TooManyLoginAttempts` / `429`.

Expected client behavior:
- Show clear retry messaging.
- Honor server-provided retry hints when available.
- Avoid aggressive automatic retries.

## Client Behavior

Use the shared aura extension helpers to convert exceptions into localized display text.

UI expectations:
- Prefer explicit, calm messaging over technical noise.
- Show retry CTA only for recoverable scenarios.
- Preserve field-level validation feedback when available.

## Retry Strategy

Use retry selectively.

Good candidates:
- transient network failures
- temporary backend availability issues

Bad candidates:
- validation errors
- auth/permission failures
- deterministic business rule violations

If retry logic is required in domain/application code, use shared retry abstractions in:
- `foundation/domain/src/commonMain/kotlin/tech/dokus/domain/asbtractions/RetryHandler.kt`

## Logging Guidance

- Log error codes and contextual identifiers (tenant, request, document, etc.).
- Do not log secrets, tokens, or full sensitive payloads.
- Keep stack traces for server observability, not end-user output.

## Adding New Exceptions

1. Add new exception type in `DokusException` hierarchy.
2. Define code/status/recoverability semantics.
3. Ensure backend mapping includes correct HTTP behavior.
4. Add localization entries for user-facing rendering.
5. Add tests for mapping and client rendering behavior.
