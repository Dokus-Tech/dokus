# Dokus Documentation Index

**Welcome to Dokus - Financial Management for Small Businesses**

This is your complete guide to building, launching, and scaling Dokus. Everything you need is documented here.

---

## 📚 Quick Start

New to the project? Start here:

1. **[[README|Project Overview]]** - What is Dokus?
2. **[[Quick-Start|Quick Start Guide]]** - Get running in 30 minutes
3. **[[Storyboard|Product Storyboard]]** - User journeys and flows
4. **[[12-First-90-Days|First 90 Days Plan]]** - Your roadmap to MVP

---

## 🎯 Strategy & Product

### Product Vision
- **[[01-Product-Strategy|Product Strategy]]** - Market analysis, positioning, pricing
- **[[Storyboard|Storyboard]]** - User stories and flows

### Technical Design
- **[[02-Technical-Architecture|Technical Architecture]]** - System design, tech stack, infrastructure
- **[[Execution-Flow.canvas|Execution Flow]]** - Visual architecture diagram

---

## 💾 Database & Backend

### Database Design
- **[[Database-Schema|Database Schema Reference]]** 📘 **START HERE FOR DATABASE**
  - Complete table definitions with Kotlin code examples
  - Multi-tenancy security patterns
  - Financial data precision guide
  - SQL migration scripts
  - Usage examples

### Integration Guides
- **[[KotlinX-RPC-Integration|KotlinX RPC Integration]]** 🔌 **START HERE FOR RPC**
  - Why KotlinX RPC over REST
  - Service interface design
  - Repository pattern with code examples
  - Server setup with Ktor
  - Client implementation (web, mobile)
  - Real-time updates with Flows

### Best Practices
- **[[Database-Best-Practices|Database Best Practices]]** ✅ **READ THIS**
  - Multi-tenant security rules
  - Financial data handling
  - Performance optimization
  - Transaction management
  - Common pitfalls and solutions

---

## 🏗️ Architecture Deep Dives

### Core Concepts

**Multi-Tenancy**
- Every table has `tenant_id`
- Always filter by tenant in queries
- Shared database for MVP
- See: [[Database-Schema#Design Principles|Design Principles]]

**Type-Safe RPC**
- Shared interfaces between client/server
- Compile-time verification
- No REST boilerplate
- See: [[KotlinX-RPC-Integration#Why KotlinX RPC|Why KotlinX RPC]]

**Financial Precision**
- NUMERIC types for all money
- Never use FLOAT/REAL
- Proper rounding rules
- See: [[Database-Best-Practices#Financial Data Handling|Financial Data Handling]]

**Audit Compliance**
- Immutable audit logs
- 7-year retention
- GDPR compliant
- See: [[Database-Schema#AuditLogs|Audit Logs]]

---

## 🔧 Implementation Guides

### Database Setup

1. **Schema Design**
   - Read: [[Database-Schema|Complete Database Schema]]
   - Review all 14 tables and their relationships
   - Understand multi-tenancy pattern

2. **ORM Implementation**
   - Copy Exposed schema code from documentation
   - Set up DatabaseFactory with HikariCP
   - Create Flyway migrations

3. **Security**
   - Implement tenant isolation in all queries
   - Follow: [[Database-Best-Practices#Multi-Tenant Security|Security Guide]]

### RPC Services Setup

1. **Project Structure**
   - Create monorepo structure
   - See: [[KotlinX-RPC-Integration#Project Structure|Project Structure]]

2. **Service Interfaces**
   - Define shared RPC interfaces
   - Create DTOs for all operations
   - Example: [[KotlinX-RPC-Integration#Service Interfaces|Service Interfaces]]

3. **Repository Layer**
   - Implement repository pattern
   - Add tenant validation
   - Example: [[KotlinX-RPC-Integration#Repository Pattern|Repository Pattern]]

4. **Service Implementation**
   - Implement business logic
   - Connect to repositories
   - Example: [[KotlinX-RPC-Integration#Service Implementation|Service Implementation]]

5. **Server Setup**
   - Configure Ktor with KotlinX RPC
   - Register services
   - Example: [[KotlinX-RPC-Integration#Server Setup|Server Setup]]

---

## 📖 Reference Documentation

### Database Tables

| Table | Purpose | Link |
|-------|---------|------|
| **Tenants** | Customer accounts | [[Database-Schema#Tenants\|Schema]] |
| **Users** | Account access | [[Database-Schema#Users\|Schema]] |
| **Clients** | Invoice recipients | [[Database-Schema#Clients\|Schema]] |
| **Invoices** | Billing documents | [[Database-Schema#Invoices\|Schema]] |
| **InvoiceItems** | Line items | [[Database-Schema#InvoiceItems\|Schema]] |
| **Expenses** | Business costs | [[Database-Schema#Expenses\|Schema]] |
| **Payments** | Payment tracking | [[Database-Schema#Payments\|Schema]] |
| **BankConnections** | Bank integration | [[Database-Schema#BankConnections\|Schema]] |
| **BankTransactions** | Imported transactions | [[Database-Schema#BankTransactions\|Schema]] |
| **VatReturns** | Quarterly VAT | [[Database-Schema#VatReturns\|Schema]] |
| **AuditLogs** | Audit trail | [[Database-Schema#AuditLogs\|Schema]] |
| **TenantSettings** | Configuration | [[Database-Schema#TenantSettings\|Schema]] |
| **Attachments** | File uploads | [[Database-Schema#Attachments\|Schema]] |

### Code Examples

**Creating an Invoice:**
- [[Database-Schema#Creating an Invoice|Database Layer]]
- [[KotlinX-RPC-Integration#Service Implementation|Service Layer]]

**Querying with Filters:**
- [[Database-Schema#Querying Invoices|Database Layer]]

**Recording Payments:**
- [[Database-Schema#Recording Payment|Database Layer]]

**Real-time Updates:**
- [[KotlinX-RPC-Integration#Service Implementation|Flow-based Streaming]]

---

## ⚡ Best Practices Checklist

### Security ✅
- [ ] Every query filters by `tenant_id`
- [ ] Sensitive data encrypted (MFA secrets, tokens)
- [ ] Audit logs for all financial operations
- [ ] Input validation before database
- [ ] No database entities exposed in DTOs
- [ ] See: [[Database-Best-Practices#Security Checklist|Full Security Checklist]]

### Performance ✅
- [ ] Appropriate indexes created
- [ ] N+1 queries eliminated
- [ ] Batch operations for bulk inserts
- [ ] Connection pooling configured
- [ ] Transaction isolation set
- [ ] See: [[Database-Best-Practices#Performance Checklist|Full Performance Checklist]]

### Financial Data ✅
- [ ] NUMERIC types for all money
- [ ] Proper rounding (HALF_UP)
- [ ] Calculations in correct order
- [ ] VAT calculations verified
- [ ] See: [[Database-Best-Practices#Financial Data Handling|Financial Best Practices]]

---

## 🚀 Deployment & Operations

### Pre-Launch Checklist
- [ ] Database schema created
- [ ] Migrations tested
- [ ] Backups automated
- [ ] Monitoring configured
- [ ] Error tracking set up
- [ ] Performance tested
- [ ] Security audit completed

### Monitoring
- **Metrics to Track:**
  - Database query performance
  - Connection pool usage
  - Table sizes
  - Slow queries
- **See:** [[Database-Best-Practices#Monitoring|Monitoring Guide]]

### Backup & Recovery
- Daily automated backups
- Point-in-time recovery
- Test restore monthly
- **See:** [[Database-Best-Practices#Backup & Recovery|Backup Guide]]

---

## 🎓 Learning Path

### For Backend Developers

**Week 1: Database Foundation**
1. Read [[Database-Schema|Database Schema]] completely
2. Understand multi-tenancy pattern
3. Set up local PostgreSQL
4. Run migrations
5. Practice queries with tenant isolation

**Week 2: ORM & Repositories**
1. Read [[Database-Best-Practices|Best Practices]]
2. Implement repository pattern
3. Write tests for tenant isolation
4. Practice transactions

**Week 3: RPC Services**
1. Read [[KotlinX-RPC-Integration|RPC Integration]]
2. Define service interfaces
3. Implement business logic
4. Set up Ktor server

**Week 4: Integration & Testing**
1. Connect all layers
2. Write integration tests
3. Test multi-tenant scenarios
4. Performance optimization

### For Frontend Developers

**Week 1: Understanding Backend**
1. Read [[KotlinX-RPC-Integration#Client Usage|Client Usage]]
2. Understand RPC interfaces
3. Review DTOs

**Week 2: Client Implementation**
1. Set up KotlinX RPC client
2. Implement service calls
3. Handle real-time updates (Flows)

---

## 📋 Common Tasks

### Add a New Table

1. Define Exposed schema
   - See: [[Database-Schema#Table Definitions|Table Examples]]
2. Create migration script
3. Add to `allTables` array
4. Create repository
5. Update audit logging

### Add a New RPC Service

1. Define interface in `shared/`
   - See: [[KotlinX-RPC-Integration#Service Interfaces|Interface Examples]]
2. Create DTOs
3. Implement repository
4. Implement service
5. Register in Ktor
6. Write tests

### Optimize Slow Query

1. Enable slow query logging
2. Find slow queries in logs
3. Check if index exists
4. Add index if needed
5. Test performance improvement
   - See: [[Database-Best-Practices#Performance Optimization|Optimization Guide]]

---

## 🐛 Troubleshooting

### Common Issues

**"Access denied to resource"**
- Missing `tenant_id` filter in query
- See: [[Database-Best-Practices#Multi-Tenant Security|Security Guide]]

**"Rounding errors in calculations"**
- Using FLOAT instead of NUMERIC
- See: [[Database-Best-Practices#Financial Data Handling|Financial Data Guide]]

**"N+1 query performance problem"**
- Not using joins
- See: [[Database-Best-Practices#Avoid N+1 Queries|N+1 Solution]]

**"Connection pool exhausted"**
- Too few connections or leaks
- See: [[Database-Best-Practices#Connection Pooling|Pool Configuration]]

**"Transaction deadlock"**
- Inconsistent lock order
- See: [[Database-Best-Practices#Deadlock Prevention|Deadlock Guide]]

---

## 🔗 External Resources

### Documentation
- [Exposed ORM Docs](https://github.com/JetBrains/Exposed/wiki)
- [KotlinX RPC Docs](https://kotlin.github.io/kotlinx-rpc/)
- [PostgreSQL Docs](https://www.postgresql.org/docs/)
- [Ktor Docs](https://ktor.io/docs/)

### Tools
- [Flyway Migrations](https://flywaydb.org/)
- [HikariCP](https://github.com/brettwooldridge/HikariCP)
- [IntelliJ IDEA](https://www.jetbrains.com/idea/)

---

## 📞 Need Help?

1. **Check this documentation** - Most answers are here
2. **Search for similar issues** - Use Obsidian search
3. **Review code examples** - Follow patterns in guides
4. **Test in isolation** - Create minimal reproduction

---

## 🗺️ Documentation Map

```
Dokus Documentation
│
├── 📋 Getting Started
│   ├── README
│   ├── Quick-Start
│   └── 12-First-90-Days
│
├── 🎯 Strategy
│   ├── 01-Product-Strategy
│   └── Storyboard
│
├── 🏗️ Architecture
│   ├── 02-Technical-Architecture
│   └── Execution-Flow.canvas
│
├── 💾 Database (⭐ START HERE)
│   ├── Database-Schema         ← Complete reference
│   ├── KotlinX-RPC-Integration ← Implementation guide
│   └── Database-Best-Practices ← Security & performance
│
└── 📚 This Index
    └── Documentation-Index     ← You are here
```

---

## 🎯 Your Next Steps

Based on where you are in the project:

**Planning Phase:**
1. Read [[01-Product-Strategy|Product Strategy]]
2. Review [[Storyboard|Storyboard]]
3. Study [[12-First-90-Days|90-Day Plan]]

**Architecture Phase:**
1. Read [[02-Technical-Architecture|Technical Architecture]]
2. Study [[Database-Schema|Database Schema]]
3. Review [[KotlinX-RPC-Integration|RPC Integration]]

**Implementation Phase:**
1. Set up database with [[Database-Schema|Schema Guide]]
2. Implement RPC services with [[KotlinX-RPC-Integration|RPC Guide]]
3. Follow [[Database-Best-Practices|Best Practices]]

**Launch Phase:**
1. Complete [[Database-Best-Practices#Security Checklist|Security Checklist]]
2. Complete [[Database-Best-Practices#Performance Checklist|Performance Checklist]]
3. Set up monitoring and backups

---

**Last Updated:** October 2025  
**Version:** 1.0  
**Status:** Complete

*This documentation is your bible. Follow it, and Dokus will succeed.* 🚀
