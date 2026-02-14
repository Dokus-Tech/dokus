# Deployment Guide

This directory contains Docker-based deployment assets for Dokus.

## What Is Deployed

- `dokus-server` (single Ktor backend process)
- PostgreSQL
- Redis
- MinIO
- Traefik

## Profiles

`dokus.sh` supports three profiles:

- `lite`: self-host, low-resource defaults (default)
- `pro`: self-host, higher performance defaults
- `cloud`: HTTPS deployment with Let's Encrypt + domain routing

## Quick Start

From this directory:

```bash
./dokus.sh setup
./dokus.sh start
./dokus.sh status
```

You can also force a profile:

```bash
./dokus.sh --profile=pro start
./dokus.sh --profile=cloud start
```

## Common Commands

```bash
./dokus.sh start
./dokus.sh stop
./dokus.sh restart
./dokus.sh status
./dokus.sh logs
./dokus.sh logs dokus-server
./dokus.sh update
./dokus.sh db
./dokus.sh connect
./dokus.sh profile
./dokus.sh profile pro
./dokus.sh debug
```

## Compose Files

- `docker-compose.lite.yml`
- `docker-compose.pro.yml`
- `docker-compose.cloud.yml`
- `docker-compose.debug.yml` (overlay for JDWP)
- `docker-compose.local.yml` (local development overlay)

## Environment

Start from:
- `.env.example`

Do not commit secrets in `.env`.

## Access Endpoints

For self-host profiles (`lite`, `pro`):
- Gateway: `http://localhost:8000`
- Health: `http://localhost:8000/health`
- Server info: `http://localhost:8000/api/v1/server/info`

For cloud profile:
- Gateway: `https://<your-domain>`

## Notes

- `dokus.sh` persists selected profile in `.dokus-profile`.
- Debug mode (`./dokus.sh debug`) toggles remote JVM debugging on port `5005`.
- If using private images, authenticate to the registry before startup.
