# Infrastructure Port Configuration

This document lists all infrastructure ports used by Dokus to avoid conflicts with other Docker containers.

## Port Assignment Strategy

All infrastructure ports have been reassigned to non-standard ranges to prevent conflicts:
- **PostgreSQL databases**: 15xxx range (instead of standard 5432)
- **Redis**: 16379 (instead of standard 6379)
- **RabbitMQ AMQP**: 25672 (instead of standard 5672)
- **RabbitMQ Management UI**: 25673 (instead of standard 15672)

## Local Development Ports (docker-compose.local.yml)

### PostgreSQL Databases
| Service                  | Container Name                | External Port | Internal Port | Database Name    |
|--------------------------|-------------------------------|---------------|---------------|------------------|
| postgres-auth-local      | dokus-postgres-auth-local     | 15541         | 5432          | dokus_auth       |
| postgres-cashflow-local  | dokus-postgres-cashflow-local | 15542         | 5432          | dokus_cashflow   |
| postgres-payment-local   | dokus-postgres-payment-local  | 15543         | 5432          | dokus_payment    |
| postgres-reporting-local | dokus-postgres-reporting-local| 15544         | 5432          | dokus_reporting  |
| postgres-audit-local     | dokus-postgres-audit-local    | 15545         | 5432          | dokus_audit      |
| postgres-banking-local   | dokus-postgres-banking-local  | 15546         | 5432          | dokus_banking    |

### Message Brokers & Caching
| Service         | Container Name      | External Port | Internal Port | Purpose            |
|-----------------|---------------------|---------------|---------------|--------------------|
| redis-local     | dokus-redis-local   | 16379         | 6379          | Caching            |
| rabbitmq-local  | dokus-rabbitmq-local| 25672         | 5672          | AMQP               |
| rabbitmq-local  | dokus-rabbitmq-local| 25673         | 15672         | Management UI      |

### Accessing Local Infrastructure
```bash
# Connect to PostgreSQL databases
psql -h localhost -p 15541 -U dev -d dokus_auth
psql -h localhost -p 15542 -U dev -d dokus_cashflow
psql -h localhost -p 15543 -U dev -d dokus_payment
psql -h localhost -p 15544 -U dev -d dokus_reporting
psql -h localhost -p 15545 -U dev -d dokus_audit
psql -h localhost -p 15546 -U dev -d dokus_banking

# Connect to Redis
redis-cli -p 16379 -a localredispass

# Access RabbitMQ Management UI
open http://localhost:25673
# Username: dokus
# Password: localrabbitpass
```

## Production Ports (deployment/docker-compose.yml)

### PostgreSQL Databases
| Service              | Container Name          | External Port | Internal Port | Database Name    |
|----------------------|-------------------------|---------------|---------------|------------------|
| postgres-auth        | dokus-postgres-auth     | 15441         | 5432          | dokus_auth       |
| postgres-cashflow    | dokus-postgres-cashflow | 15442         | 5432          | dokus_cashflow   |
| postgres-payment     | dokus-postgres-payment  | 15443         | 5432          | dokus_payment    |
| postgres-reporting   | dokus-postgres-reporting| 15444         | 5432          | dokus_reporting  |
| postgres-audit       | dokus-postgres-audit    | 15445         | 5432          | dokus_audit      |
| postgres-banking     | dokus-postgres-banking  | 15446         | 5432          | dokus_banking    |

### Message Brokers & Caching
| Service   | Container Name   | External Port | Internal Port | Purpose            |
|-----------|------------------|---------------|---------------|--------------------|
| redis     | dokus-redis      | 16379         | 6379          | Caching            |
| rabbitmq  | dokus-rabbitmq   | 25672         | 5672          | AMQP               |
| rabbitmq  | dokus-rabbitmq   | 25673         | 15672         | Management UI      |

## Application Service Ports

Application service ports remain unchanged:

### Local (docker-compose.local.yml)
| Service   | HTTP Port | Debug Port |
|-----------|-----------|------------|
| Auth      | 7091      | 5007       |
| Cashflow  | 7092      | 5008       |
| Payment   | 7093      | 5009       |
| Reporting | 7094      | 5010       |
| Audit     | 7095      | 5011       |
| Banking   | 7096      | 5012       |

### Production (deployment/docker-compose.yml)
| Service   | HTTP Port |
|-----------|-----------|
| Auth      | 6091      |
| Cashflow  | 6092      |
| Payment   | 6093      |
| Reporting | 6094      |
| Audit     | 6095      |
| Banking   | 6096      |

## Configuration Files Updated

The following files have been updated with new infrastructure ports:
- `docker-compose.local.yml` - Local development infrastructure
- `deployment/docker-compose.yml` - Production infrastructure
- `dev.sh` - Development script database configuration
- All `features/*/backend/src/main/resources/application.conf` files

## Port Conflict Prevention

These non-standard ports prevent conflicts with:
- Standard PostgreSQL (5432)
- Standard Redis (6379)
- Standard RabbitMQ (5672, 15672)
- Other local PostgreSQL instances
- Other Docker containers using standard ports

## Docker Network Configuration

All services communicate internally using standard ports (5432, 6379, 5672) via Docker's internal network. External port mappings are only used for:
1. Local development access from host machine
2. Production server access for administration
3. Service-to-service communication through Docker network uses internal ports

---
**Last Updated:** 2025-11-16
