# Dokus 🇧🇪

> Open source PEPPOL invoicing for Belgium's 2026 mandate

[![GitHub stars](https://img.shields.io/github/stars/dokus/dokus?style=social)](https://github.com/dokus/dokus/stargazers)
[![License](https://img.shields.io/badge/license-AGPL--3.0-blue.svg)](LICENSE)
[![Discord](https://img.shields.io/discord/xxx?label=Discord&logo=discord)](https://discord.gg/dokus)

## 🚨 Belgium mandates PEPPOL e-invoicing January 1, 2026

Penalties for non-compliance: €1,500-5,000

Dokus is the first open-source solution that:

✅ Generates PEPPOL BIS 3.0 compliant UBL invoices  
✅ Handles Belgian VAT rules (21%, 12%, 6%)  
✅ Sends invoices via PEPPOL network  
✅ Self-host (AGPL v3) or use our cloud (€18/mo)

## ⚡ Quick Start (5 minutes)

### Self-Hosted (Docker)
```bash
git clone https://github.com/dokus/dokus
cd dokus
docker-compose up
```

Visit http://localhost:8080

### Cloud (Free Trial)
[Start 30-day free trial →](https://dokus.io/signup)

## 📖 Documentation

- [Installation Guide](docs/installation.md)
- [PEPPOL Setup](docs/peppol.md)
- [API Reference](docs/api.md)
- [Belgian Compliance](docs/belgium-2026.md)

## 🛠️ Built With

- Kotlin + Ktor (Backend)
- PostgreSQL (Database)
- PEPPOL BIS 3.0 (Invoicing)
- AGPL v3 (License)

## 🤝 Contributing

We welcome contributions! See [CONTRIBUTING.md](CONTRIBUTING.md)

## 📝 License

AGPL-3.0 - See [LICENSE](LICENSE)

Self-host for free. Cloud hosting available at [dokus.io](https://dokus.io)

## 🙋 Support

- [Discord Community](https://discord.gg/dokus)
- [Documentation](https://docs.dokus.io)
- [GitHub Issues](https://github.com/dokus/dokus/issues)

---

**⭐ Star us on GitHub if this helps you avoid 2026 penalties!**