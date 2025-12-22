# Auth Service

Authentication, authorization, and multi-tenant organization management service for the Dokus platform.

## Overview

The Auth service provides comprehensive identity and access management:

- **User Authentication**: Login, registration, password reset, email verification
- **JWT Token Management**: Access tokens, refresh tokens with rotation, token blacklisting
- **Multi-Tenant Architecture**: Organization/tenant management, membership, role-based access
- **Team Management**: Invitations, role updates, ownership transfer
- **Rate Limiting**: Protection against brute-force attacks
- **Email Services**: Transactional emails for verification and password reset

## Architecture

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                             REST API Routes                                  │
│  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌───────┐│
│  │ Identity │ │ Account  │ │  Tenant  │ │   Team   │ │  Avatar  │ │Lookup ││
│  │  Routes  │ │  Routes  │ │  Routes  │ │  Routes  │ │  Routes  │ │Routes ││
│  └────┬─────┘ └────┬─────┘ └────┬─────┘ └────┬─────┘ └────┬─────┘ └───┬───┘│
└───────┼────────────┼────────────┼────────────┼────────────┼───────────┼────┘
        │            │            │            │            │           │
        ▼            ▼            ▼            ▼            ▼           ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                              Services Layer                                  │
│  ┌───────────────┐ ┌───────────────┐ ┌───────────────┐ ┌───────────────────┐│
│  │  AuthService  │ │  TeamService  │ │ EmailService  │ │ RateLimitService  ││
│  │  (Core Auth)  │ │(Team Mgmt)    │ │ (SMTP/Email)  │ │ (Brute Force)     ││
│  └───────────────┘ └───────────────┘ └───────────────┘ └───────────────────┘│
│  ┌───────────────────────┐ ┌───────────────────────┐ ┌─────────────────────┐│
│  │EmailVerificationSvc   │ │PasswordResetService   │ │TokenBlacklistService││
│  └───────────────────────┘ └───────────────────────┘ └─────────────────────┘│
└─────────────────────────────────────────────────────────────────────────────┘
        │                                    │
        ▼                                    ▼
┌─────────────────────────────┐    ┌─────────────────────────────┐
│      PostgreSQL             │    │         Redis               │
│  (Users, Tenants, Tokens)   │    │  (Rate Limits, Blacklist)   │
└─────────────────────────────┘    └─────────────────────────────┘
```

## Services

| Service | Purpose | Key Methods |
|---------|---------|-------------|
| `AuthService` | Core authentication logic | `login`, `register`, `logout`, `refreshToken`, `selectOrganization`, `deactivateAccount` |
| `TeamService` | Team/membership management | `listTeamMembers`, `createInvitation`, `updateMemberRole`, `removeMember`, `transferOwnership` |
| `EmailService` | Transactional email delivery | `sendPasswordResetEmail`, `sendEmailVerificationEmail`, `sendWelcomeEmail` |
| `RateLimitService` | Login attempt rate limiting | `checkLoginAttempts`, `recordFailedLogin`, `resetLoginAttempts` |
| `EmailVerificationService` | Email verification flow | `sendVerificationEmail`, `verifyEmail`, `resendVerificationEmail` |
| `PasswordResetService` | Password reset flow | `requestReset`, `resetPassword` |
| `TokenBlacklistService` | JWT invalidation | `blacklistToken`, `isBlacklisted`, `blacklistAllUserTokens` |

## API Routes

### Identity Routes (Unauthenticated)
Base path: `/api/v1/identity`

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/login` | Authenticate with email/password |
| POST | `/register` | Create new user account |
| POST | `/refresh` | Refresh access token |
| POST | `/password-resets` | Request password reset email |
| PATCH | `/password-resets/{token}` | Complete password reset |
| PATCH | `/email-verifications/{token}` | Verify email address |

### Account Routes (Authenticated)
Base path: `/api/v1/account`

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/me` | Get current user profile |
| PATCH | `/profile` | Update user profile |
| POST | `/deactivate` | Deactivate user account |
| PUT | `/active-tenant` | Switch active tenant |
| POST | `/logout` | Logout and revoke tokens |
| POST | `/email-verifications` | Resend verification email |

### Tenant Routes (Authenticated)
Base path: `/api/v1/tenants`

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/` | List user's tenants |
| POST | `/` | Create new tenant |
| GET | `/{id}` | Get tenant by ID |
| GET | `/settings` | Get tenant settings |
| PUT | `/settings` | Update tenant settings |
| GET | `/address` | Get company address |
| PUT | `/address` | Update company address |

### Team Routes (Authenticated)
Base path: `/api/v1/team`

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/members` | List team members |
| PUT | `/members/{userId}/role` | Update member role (Owner only) |
| DELETE | `/members/{userId}` | Remove team member (Owner only) |
| PUT | `/owner` | Transfer ownership (Owner only) |
| GET | `/invitations` | List pending invitations |
| POST | `/invitations` | Create invitation (Owner only) |
| DELETE | `/invitations/{id}` | Cancel invitation (Owner only) |

### Avatar Routes (Authenticated)
Base path: `/api/v1/tenants/avatar`

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/` | Upload tenant avatar (multipart) |
| GET | `/` | Get avatar URLs |
| DELETE | `/` | Remove avatar |

### Lookup Routes (Authenticated)
Base path: `/api/v1/lookup`

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/company?name={name}` | Search Belgian companies (CBE) |

## Key Files

```
features/auth/backend/
├── src/main/kotlin/ai/dokus/auth/backend/
│   ├── Application.kt              # Entry point, Ktor configuration
│   ├── config/
│   │   └── DependencyInjection.kt  # Koin DI module setup
│   ├── database/
│   │   ├── AuthTables.kt           # Database table initialization
│   │   └── mappers/                # DB entity mappers
│   ├── jobs/
│   │   └── RateLimitCleanupJob.kt  # Background rate limit cleanup
│   ├── plugins/
│   │   ├── Routing.kt              # Health/server info routes
│   │   ├── Database.kt             # Database configuration
│   │   └── BackgroundJobs.kt       # Job scheduler
│   ├── routes/
│   │   ├── Routes.kt               # Route aggregator
│   │   ├── IdentityRoutes.kt       # Unauthenticated auth routes
│   │   ├── AccountRoutes.kt        # User account routes
│   │   ├── TenantRoutes.kt         # Tenant management routes
│   │   ├── TeamRoutes.kt           # Team management routes
│   │   ├── AvatarRoutes.kt         # Avatar upload/management
│   │   └── LookupRoutes.kt         # External data lookups
│   └── services/
│       ├── AuthService.kt          # Core authentication
│       ├── TeamService.kt          # Team management
│       ├── EmailService.kt         # Email interface
│       ├── SmtpEmailService.kt     # SMTP implementation
│       ├── DisabledEmailService.kt # No-op implementation
│       ├── RateLimitService.kt     # In-memory rate limiting
│       ├── RedisRateLimitService.kt# Redis-backed rate limiting
│       ├── EmailVerificationService.kt
│       └── PasswordResetService.kt
├── docs/                           # Detailed documentation
│   ├── RATE_LIMITING.md
│   ├── REFRESH_TOKEN_SERVICE.md
│   ├── README_REFRESH_TOKENS.md
│   ├── ACCOUNT_DEACTIVATION.md
│   └── AUTHSERVICE_INTEGRATION_EXAMPLE.md
├── build.gradle.kts
└── Dockerfile*                     # Container configurations
```

## Database Tables

The Auth service owns these tables:

| Table | Purpose |
|-------|---------|
| `TenantTable` | Organizations/workspaces |
| `TenantSettingsTable` | Tenant configuration |
| `UsersTable` | User accounts |
| `TenantMembersTable` | User-tenant memberships with roles |
| `TenantInvitationsTable` | Pending team invitations |
| `RefreshTokensTable` | Refresh token storage |
| `PasswordResetTokensTable` | Password reset tokens |
| `AddressTable` | Company addresses |

## Configuration

### HOCON Configuration

```hocon
ktor {
  deployment {
    port = 8081
    host = "0.0.0.0"
  }
}

jwt {
  secret = ${JWT_SECRET}
  issuer = "dokus"
  audience = "dokus-users"
  access-token-expiry-minutes = 15
  refresh-token-expiry-days = 30
}

auth {
  max-concurrent-sessions = 5
}

email {
  enabled = true
  provider = "smtp"
  smtp {
    host = ${SMTP_HOST}
    port = ${SMTP_PORT}
    username = ${SMTP_USERNAME}
    password = ${SMTP_PASSWORD}
    from-address = "noreply@dokus.ai"
  }
}

redis {
  enabled = true
  host = "localhost"
  port = 6379
}
```

### Environment Variables

| Variable | Description | Required |
|----------|-------------|----------|
| `JWT_SECRET` | Secret for signing JWTs | Yes |
| `DATABASE_URL` | PostgreSQL connection string | Yes |
| `REDIS_URL` | Redis connection string | No (falls back to in-memory) |
| `SMTP_HOST` | SMTP server hostname | No (emails disabled) |
| `SMTP_PORT` | SMTP server port | No |
| `SMTP_USERNAME` | SMTP authentication | No |
| `SMTP_PASSWORD` | SMTP password | No |

## User Roles

| Role | Description | Key Permissions |
|------|-------------|-----------------|
| `Owner` | Workspace owner | All permissions, team management, ownership transfer |
| `Admin` | Administrator | All permissions except user management |
| `Accountant` | Financial role | Invoices, clients, reports, exports |
| `Editor` | Content editor | Invoices (CRUD), clients (read), reports |
| `Viewer` | Read-only | View invoices, clients, reports |

## Background Jobs

| Job | Interval | Purpose |
|-----|----------|---------|
| `RateLimitCleanupJob` | 1 hour | Cleans expired rate limit entries from memory |

## Security Features

- **Password Hashing**: BCrypt via `PasswordCryptoService4j`
- **JWT Tokens**: Short-lived access tokens (15 min) + long-lived refresh tokens (30 days)
- **Token Rotation**: Refresh tokens rotate on each use
- **Token Blacklisting**: Immediate invalidation on logout/deactivation
- **Rate Limiting**: 5 attempts per 15 minutes, 15-minute lockout
- **Session Limits**: Max 5 concurrent sessions per user

## Development

### Local Development

1. Ensure PostgreSQL is running
2. Optionally start Redis for distributed rate limiting
3. Configure environment variables
4. Run the service:
   ```bash
   ./gradlew :features:auth:backend:run
   ```

### Health Check

The service exposes health endpoints:
- `GET /health` - Basic health check
- `GET /health/ready` - Readiness probe
- `GET /health/live` - Liveness probe

## Detailed Documentation

For in-depth documentation on specific features, see the `docs/` folder:

- **[Rate Limiting](docs/RATE_LIMITING.md)** - Brute force protection implementation
- **[Refresh Tokens](docs/REFRESH_TOKEN_SERVICE.md)** - Token rotation and security
- **[Account Deactivation](docs/ACCOUNT_DEACTIVATION.md)** - User account lifecycle
- **[Integration Example](docs/AUTHSERVICE_INTEGRATION_EXAMPLE.md)** - How to integrate with other services

## Dependencies

- **Ktor** - Web framework with Netty engine
- **Exposed** - SQL ORM for Kotlin
- **Koin** - Dependency injection
- **Kotlinx Serialization** - JSON serialization
- **Auth0 JWT** - JWT parsing and validation
- **Jedis** - Redis client (optional)
- **Jakarta Mail** - SMTP email sending
