# UUID Migration: Auth/Contacts/Notifications Repositories

Migrate all repository files from `java.util.UUID` to `kotlin.uuid.Uuid` for Exposed 1.0.0 compatibility.

## Migration Rules Applied
1. `UUID.fromString(someValueClass.toString())` -> `someValueClass.value`
2. `UUID.fromString(someString)` -> `Uuid.parse(someString)` (for raw strings)
3. `XId.parse(row[Table.id].value.toString())` -> `XId(row[Table.id].value)`
4. `TenantId.parse(row[Table.tenantId].toString())` -> `TenantId(row[Table.tenantId])`
5. Remove `.toKotlinUuid()` calls (no longer needed with Exposed 1.0.0)
6. Remove `.toJavaUuid()` calls (columns now accept kotlin.uuid.Uuid directly)
7. Remove `import java.util.UUID` / `import kotlin.uuid.toKotlinUuid` / `import kotlin.uuid.toJavaUuid`
8. Add `@OptIn(ExperimentalUuidApi::class)` and `kotlin.uuid.Uuid` import only if file uses raw `Uuid`
9. `UUID.randomUUID()` -> `Uuid.random()`
10. `val javaUuid = x.value.toJavaUuid()` -> just use `x.value` directly

## Files to Process

### Auth Repositories
- [ ] UserRepository.kt - 8x `.toJavaUuid()`, 1x `UserId(row[...].value.toString())`
- [ ] RefreshTokenRepository.kt - 6x `.uuid.toJavaUuid()`, 1x weird `UserId(Uuid.parse(userId.toString()).toString())`
- [ ] TenantRepository.kt - 7x `.toJavaUuid()`, 1x `.toKotlinUuid()`
- [ ] WelcomeEmailJobRepository.kt - `UUID.fromString()` + `.toKotlinUuid()`, data class uses `java.util.UUID`
- [ ] PasswordResetTokenRepository.kt - `.toJavaUuid()` + `UUID` in data class & method params
- [ ] InvitationRepository.kt - 8x `.toJavaUuid()`, 3x `.toKotlinUuid()`
- [ ] AddressRepository.kt - many `.toJavaUuid()`, `Uuid.random().toJavaUuid()`, `.toKotlinUuid()`

### Contact Repositories
- [ ] ContactRepository.kt - many `UUID.fromString(x.toString())`, mapper uses `.parse(..toString())`
- [ ] ContactNoteRepository.kt - many `UUID.fromString(x.toString())`, mapper uses `.parse(..toString())`
- [ ] ContactAddressRepository.kt - `.toJavaUuid()`, `Uuid.random().toJavaUuid()`, `.toKotlinUuid()`

### Notification Repositories
- [ ] NotificationRepository.kt - `UUID.fromString()`, `UUID.randomUUID()`, mapper uses `.parse(..toString())`
- [ ] NotificationPreferencesRepository.kt - `UUID.fromString(userId.toString())`

### Mappers
- [ ] UserMappers.kt - `UserId(..toString())`, `.toKotlinUuid()`
- [ ] TenantMappers.kt - 4x `.toKotlinUuid()`

## Review
_To be filled after completion_
