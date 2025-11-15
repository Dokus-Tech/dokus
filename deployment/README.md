# Dokus Server Deployment

This folder contains everything you need to deploy a fully functional Dokus server on any machine.

## 🚀 Quick Start

### Prerequisites

- **None!** The installation script will install Docker if needed

### Installation

#### macOS / Linux

```bash
chmod +x dokus.sh
./dokus.sh
```

#### Windows

```cmd
dokus.bat
```

That's it! The script will:
1. Check for Docker (install if needed)
2. Configure environment variables
3. Pull the latest Dokus images
4. Start all services
5. Optionally configure auto-start on boot

## 📋 What's Included

This deployment includes all Dokus backend services:

- **Auth Service** (port 6091) - User authentication and authorization
- **Invoicing Service** (port 6092) - Invoice management
- **Expense Service** (port 6093) - Expense tracking
- **Payment Service** (port 6094) - Payment processing
- **Reporting Service** (port 6095) - Financial reports
- **Audit Service** (port 6096) - Audit logging
- **Banking Service** (port 6097) - Banking integration

**Infrastructure:**
- PostgreSQL databases (one per service)
- Redis cache
- RabbitMQ message broker

## ⚙️ Configuration

The `.env` file contains all configuration. Required variables:

```env
# Database
DB_PASSWORD=your-secure-password

# Redis
REDIS_PASSWORD=your-secure-password

# RabbitMQ
RABBITMQ_PASSWORD=your-secure-password

# JWT Authentication
JWT_SECRET=your-super-secret-key-at-least-32-chars
```

### Advanced Configuration

You can customize:
- Database usernames
- Service ports (in docker-compose.yml)
- Log levels
- JWT issuer/audience

## 🔧 Management Commands

### View Service Status
```bash
docker compose ps
```

### View Logs
```bash
# All services
docker compose logs -f

# Specific service
docker compose logs -f auth-service
```

### Stop Services
```bash
docker compose stop
```

### Restart Services
```bash
docker compose restart
```

### Update to Latest Version
```bash
docker compose pull
docker compose up -d
```

### Complete Removal
```bash
# WARNING: This removes all data!
docker compose down -v
```

## 🌐 Accessing Services

After installation, services are available at:

- Auth API: http://localhost:6091
- Invoicing API: http://localhost:6092
- Expense API: http://localhost:6093
- Payment API: http://localhost:6094
- Reporting API: http://localhost:6095
- Audit API: http://localhost:6096
- Banking API: http://localhost:6097
- RabbitMQ Management UI: http://localhost:15672

## 🔐 Security Notes

### For Production Deployment

1. **Use Strong Passwords**: Generate secure random passwords for all services
2. **Configure Firewall**: Only expose necessary ports
3. **Enable HTTPS**: Use a reverse proxy (nginx/traefik) with SSL certificates
4. **Regular Backups**: Backup PostgreSQL databases and volumes
5. **Update Regularly**: Pull latest images weekly for security updates

### Generating Secure Passwords

```bash
# Generate a secure password (32 characters)
openssl rand -base64 32

# Generate JWT secret (64 characters)
openssl rand -base64 64
```

## 🐳 Docker Registry

Images are pulled from: `94.111.226.82:5000`

This is an open HTTP registry. The installation script configures Docker to allow insecure connections.

## 🔄 Auto-Start Configuration

The installation script can configure Dokus to start automatically:

- **macOS**: Creates a LaunchAgent
- **Linux**: Creates a systemd service
- **Windows**: Uses Docker Desktop's restart policy

### Disabling Auto-Start

**macOS:**
```bash
launchctl unload ~/Library/LaunchAgents/com.dokus.server.plist
```

**Linux:**
```bash
sudo systemctl disable dokus.service
```

**Windows:**
Docker Desktop Settings → General → Uncheck "Start Docker Desktop when you log in"

## 🆘 Troubleshooting

### Services Won't Start

1. Check Docker is running: `docker info`
2. Check logs: `docker compose logs`
3. Verify .env configuration
4. Ensure ports aren't already in use

### Can't Pull Images

1. Verify internet connection
2. Check Docker registry configuration (insecure-registries)
3. Try manually: `docker pull 94.111.226.82:5000/dokus-auth:latest`

### Database Connection Errors

1. Wait longer for services to start (can take 2-3 minutes)
2. Check database containers: `docker compose ps | grep postgres`
3. Restart services: `docker compose restart`

### Permission Denied (Linux)

```bash
# Add user to docker group
sudo usermod -aG docker $USER

# Log out and log back in
```

## 📦 System Requirements

**Minimum:**
- 4 GB RAM
- 10 GB disk space
- 2 CPU cores

**Recommended:**
- 8 GB RAM
- 20 GB disk space
- 4 CPU cores

## 📚 Additional Resources

- [Main Dokus Repository](https://github.com/yourusername/dokus)
- [API Documentation](https://docs.dokus.ai)
- [Docker Documentation](https://docs.docker.com)

## 🤝 Support

For issues or questions:
- Open an issue on GitHub
- Check existing documentation
- Review Docker logs for error details

## 📄 License

Copyright © 2025 Dokus. All rights reserved.
