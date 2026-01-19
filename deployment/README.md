# Dokus Deployment (Self-Host + Cloud)

This folder contains everything you need to deploy the Dokus backend as a **single server** (modular monolith) with shared infrastructure.

## 🚀 Quick Start

### macOS / Linux

```bash
chmod +x dokus.sh
./dokus.sh
```

### Windows

Run `dokus.sh` via WSL2 (recommended), or start via `docker compose` manually.

The script will:
1. Check/install Docker if needed
2. Interactively create a `.env` with secure defaults
3. Pull images from the registry
4. Start the stack and verify health

## 📋 What’s Included

**Backend (single service):**
- `dokus-server` — one Ktor server hosting all backend routes (auth + cashflow + contacts + payments + processor worker)

**Infrastructure:**
- PostgreSQL (single database)
- Redis (rate limiting + token blacklist)
- MinIO (S3-compatible object storage) **kept by design**
- Traefik (gateway on `:8000` + optional dashboard on `:8080`)

## 🧩 Deployment Profiles

`./dokus.sh` supports:
- `lite` — Raspberry Pi / low resource
- `pro` — Mac mini/Mac Studio / high performance
- `cloud` — HTTPS + Let’s Encrypt (domain required)

## 🌐 Access

Self-hosted (pro/lite):
- API gateway: `http://localhost:8000`
- Health: `http://localhost:8000/health`
- Server info: `http://localhost:8000/api/v1/server/info`
- MinIO API via gateway: `http://localhost:8000/storage`
- MinIO console (if exposed by profile): `http://localhost:9001`

Cloud (cloud profile):
- API gateway: `https://<your-domain>`
- MinIO API via gateway: `https://<your-domain>/storage`

## 🔧 Management

From this folder (`deployment/`):

```bash
./dokus.sh status
./dokus.sh logs dokus-server
./dokus.sh restart
./dokus.sh update
```

Or directly with Docker Compose:

```bash
docker compose -f docker-compose.pro.yml ps
docker compose -f docker-compose.pro.yml logs -f dokus-server
```

## 🔐 Registry Note

Images are pulled from `docker.invoid.vision` (HTTPS with authentication). Run `docker login docker.invoid.vision` before pulling images.
