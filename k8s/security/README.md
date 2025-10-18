# Dokus Security Hardening Guide

This directory contains comprehensive security configurations for the Dokus production environment.

## Table of Contents

1. [Overview](#overview)
2. [Web Application Firewall (WAF)](#web-application-firewall)
3. [Network Policies](#network-policies)
4. [Pod Security Standards](#pod-security-standards)
5. [TLS/SSL Configuration](#tlsssl-configuration)
6. [Rate Limiting](#rate-limiting)
7. [Security Scanning](#security-scanning)
8. [Deployment](#deployment)
9. [Monitoring](#monitoring)
10. [Compliance](#compliance)

---

## Overview

The Dokus security architecture implements defense-in-depth with multiple layers of protection:

```
Internet
   ↓
[Cloudflare/CDN] - DDoS protection
   ↓
[NGINX Ingress + WAF] - ModSecurity OWASP CRS, rate limiting, TLS termination
   ↓
[Network Policies] - Zero-trust pod-to-pod communication
   ↓
[Application Pods] - Non-root, read-only filesystem, dropped capabilities
   ↓
[Database/Redis] - Encrypted at rest, network-isolated
```

### Security Principles

1. **Zero Trust**: Nothing is trusted by default
2. **Least Privilege**: Minimal permissions for all components
3. **Defense in Depth**: Multiple layers of security
4. **Fail Secure**: Deny by default, explicit allow rules
5. **Audit Everything**: Comprehensive logging and monitoring

---

## Web Application Firewall

### ModSecurity Configuration

File: `waf-modsecurity.yml`

**Features:**
- OWASP ModSecurity Core Rule Set (CRS) 3.3.4
- Paranoia Level 2 (balanced security/false positives)
- Custom rules for Dokus application
- SQL injection protection
- XSS protection
- Directory traversal protection
- Bot detection
- Rate limiting per IP (100 req/min)

### Deployment

```bash
# Apply WAF configuration
kubectl apply -f k8s/security/waf-modsecurity.yml

# Verify ModSecurity is enabled
kubectl describe ingress dokus-ingress-waf -n dokus-production | grep modsecurity

# Check ModSecurity logs
kubectl logs -f deployment/nginx-ingress-controller -n ingress-nginx | grep modsec
```

### Custom Rules

The WAF includes custom rules specific to Dokus:

**Rule 10001**: Block access to sensitive files (`.env`, `.git`, etc.)
**Rule 10002**: SQL injection detection in arguments
**Rule 10003**: XSS attack detection
**Rule 10010-10011**: Rate limiting (100 req/min per IP)
**Rule 10020**: API endpoint content-type validation
**Rule 10030**: Bot/scraper blocking
**Rule 10040**: Directory traversal protection
**Rule 10050**: JWT token format validation
**Rule 10060**: Large payload blocking on non-upload endpoints

### Tuning

To adjust WAF sensitivity:

```yaml
# In waf-modsecurity.yml ConfigMap
setvar:tx.paranoia_level=1  # Less strict (more false negatives)
setvar:tx.paranoia_level=3  # More strict (more false positives)
```

To whitelist specific IPs:

```yaml
# In Ingress annotations
nginx.ingress.kubernetes.io/limit-whitelist: "1.2.3.4,5.6.7.8"
```

---

## Network Policies

File: `network-policies.yml`

### Zero-Trust Networking

All network traffic is denied by default. Only explicitly allowed traffic is permitted.

**Default Deny Policy:**
```yaml
apiVersion: networking.k8s.io/v1
kind: NetworkPolicy
metadata:
  name: default-deny-all
spec:
  podSelector: {}
  policyTypes:
  - Ingress
  - Egress
```

### Allowed Traffic Flows

1. **NGINX Ingress → Database Service** (port 9070)
2. **NGINX Ingress → Auth Service** (port 8091)
3. **Database Service → PostgreSQL** (port 5432)
4. **Database Service → Redis** (port 6379)
5. **Auth Service → Database Service** (RPC port 9070)
6. **Prometheus → All Services** (metrics scraping)
7. **Grafana → Prometheus** (metrics visualization)

### Verification

```bash
# Apply network policies
kubectl apply -f k8s/security/network-policies.yml

# Verify policies are created
kubectl get networkpolicies -n dokus-production

# Test network isolation
kubectl run test-pod --image=busybox -n dokus-production --rm -it -- sh
# Try to access Redis (should fail)
wget -O- redis:6379
```

### Troubleshooting

If services can't communicate:

```bash
# Check network policy logs (if using Cilium)
kubectl -n kube-system logs -f cilium-xxxxx

# Describe network policies
kubectl describe networkpolicy -n dokus-production

# Check if CNI supports NetworkPolicy
kubectl get nodes -o wide
```

---

## Pod Security Standards

File: `pod-security-standards.yml`

### Enforcement Level: Restricted

The namespace enforces the **restricted** pod security standard:

```yaml
labels:
  pod-security.kubernetes.io/enforce: restricted
  pod-security.kubernetes.io/audit: restricted
  pod-security.kubernetes.io/warn: restricted
```

### Required Security Context

All pods must include:

```yaml
securityContext:
  runAsNonRoot: true
  runAsUser: 1001
  fsGroup: 1001
  seccompProfile:
    type: RuntimeDefault

containers:
- name: app
  securityContext:
    readOnlyRootFilesystem: true
    allowPrivilegeEscalation: false
    capabilities:
      drop:
      - ALL
    runAsNonRoot: true
    runAsUser: 1001
```

### Resource Quotas

**Namespace limits:**
- CPU requests: 20 cores
- Memory requests: 40 GiB
- CPU limits: 40 cores
- Memory limits: 80 GiB
- Pods: 50
- PVCs: 10
- Storage: 500 GiB

**Per-container limits:**
- Max CPU: 4 cores
- Max memory: 8 GiB
- Min CPU: 50m
- Min memory: 64 MiB

### RBAC

Service accounts have **zero permissions** by default:

```bash
# Database service account
automountServiceAccountToken: false
rules: []  # No Kubernetes API access
```

### Verification

```bash
# Check pod security violations
kubectl get pods -n dokus-production -o json | jq '.items[].metadata.labels | select(.["pod-security.kubernetes.io/enforce"] != "restricted")'

# Verify all pods run as non-root
kubectl get pods -n dokus-production -o json | jq '.items[].spec.securityContext.runAsNonRoot'

# Check resource quotas
kubectl describe resourcequota -n dokus-production

# Check limit ranges
kubectl describe limitrange -n dokus-production
```

---

## TLS/SSL Configuration

### Let's Encrypt Certificates

Automated certificate management with cert-manager:

```yaml
annotations:
  cert-manager.io/cluster-issuer: "letsencrypt-prod"
spec:
  tls:
  - hosts:
    - api.dokus.ai
    secretName: dokus-tls-cert
```

### TLS Configuration

**Protocols:** TLS 1.2, TLS 1.3 only
**Ciphers:** Strong ciphers only (ECDHE, AES-GCM, ChaCha20-Poly1305)

```yaml
annotations:
  nginx.ingress.kubernetes.io/ssl-protocols: "TLSv1.2 TLSv1.3"
  nginx.ingress.kubernetes.io/ssl-ciphers: "ECDHE-ECDSA-AES128-GCM-SHA256:..."
```

### HSTS

HTTP Strict Transport Security enforced:

```yaml
nginx.ingress.kubernetes.io/hsts: "true"
nginx.ingress.kubernetes.io/hsts-max-age: "31536000"
nginx.ingress.kubernetes.io/hsts-include-subdomains: "true"
nginx.ingress.kubernetes.io/hsts-preload: "true"
```

### Certificate Renewal

Certificates auto-renew 30 days before expiration:

```bash
# Check certificate status
kubectl get certificate -n dokus-production

# Force renewal
kubectl delete secret dokus-tls-cert -n dokus-production
# cert-manager will recreate automatically

# Check cert-manager logs
kubectl logs -f deployment/cert-manager -n cert-manager
```

### Testing TLS Configuration

```bash
# Using testssl.sh
testssl.sh https://api.dokus.ai

# Using SSL Labs
# Visit: https://www.ssllabs.com/ssltest/analyze.html?d=api.dokus.ai

# Check certificate expiration
echo | openssl s_client -connect api.dokus.ai:443 -servername api.dokus.ai 2>/dev/null | openssl x509 -noout -dates
```

---

## Rate Limiting

Multiple layers of rate limiting:

### 1. ModSecurity (IP-based)
- 100 requests per minute per IP
- 600-second block duration for violations

### 2. NGINX Ingress (Global)
```yaml
nginx.ingress.kubernetes.io/limit-rps: "20"      # 20 req/sec
nginx.ingress.kubernetes.io/limit-rpm: "1000"    # 1000 req/min
nginx.ingress.kubernetes.io/limit-connections: "100"
nginx.ingress.kubernetes.io/limit-burst-multiplier: "5"
```

### 3. Application-Level
Implement rate limiting in application code for specific endpoints

### Monitoring Rate Limits

```bash
# Check rate limit hits
kubectl logs -f deployment/nginx-ingress-controller -n ingress-nginx | grep "limiting requests"

# Prometheus query for rate limit violations
rate(nginx_ingress_controller_requests{status="429"}[5m])
```

### Adjusting Rate Limits

For higher traffic:

```yaml
# Increase limits (carefully)
nginx.ingress.kubernetes.io/limit-rps: "50"
nginx.ingress.kubernetes.io/limit-rpm: "2000"
```

For stricter limits:

```yaml
# Decrease limits
nginx.ingress.kubernetes.io/limit-rps: "10"
nginx.ingress.kubernetes.io/limit-rpm: "500"
```

---

## Security Scanning

Script: `../../scripts/security-scan.sh`

### Automated Security Scans

```bash
# Run all scans
./scripts/security-scan.sh --all

# Individual scans
./scripts/security-scan.sh --images    # Container image vulnerabilities
./scripts/security-scan.sh --k8s       # Kubernetes config audit
./scripts/security-scan.sh --secrets   # Secret detection
./scripts/security-scan.sh --deps      # Dependency vulnerabilities
./scripts/security-scan.sh --tls       # TLS configuration
```

### Scan Types

**1. Container Image Scanning (Trivy)**
- Scans all container images for CVEs
- Reports CRITICAL and HIGH vulnerabilities
- Generates JSON reports

**2. Kubernetes Configuration Audit (kubeaudit)**
- Checks for security misconfigurations
- Validates pod security contexts
- Checks RBAC permissions

**3. Secret Detection (gitleaks)**
- Scans codebase for leaked secrets
- Detects passwords, API keys, tokens
- Reports potential leaks

**4. Dependency Vulnerabilities (Gradle)**
- Scans Gradle dependencies
- Uses OWASP Dependency-Check
- Generates HTML report

**5. TLS Configuration (testssl.sh)**
- Tests TLS/SSL configuration
- Checks cipher suites
- Validates certificates

### CI/CD Integration

Add to `.github/workflows/ci-cd.yml`:

```yaml
- name: Security Scan
  run: |
    ./scripts/security-scan.sh --images --secrets --deps
```

### Regular Scanning Schedule

**Daily:**
- Image vulnerability scanning
- Secret detection

**Weekly:**
- Kubernetes configuration audit
- Dependency vulnerability scanning

**Monthly:**
- Full security audit
- Penetration testing
- TLS configuration review

---

## Deployment

### Prerequisites

```bash
# Install cert-manager (for TLS certificates)
kubectl apply -f https://github.com/cert-manager/cert-manager/releases/download/v1.13.0/cert-manager.yaml

# Install NGINX Ingress Controller with ModSecurity
kubectl apply -f https://raw.githubusercontent.com/kubernetes/ingress-nginx/controller-v1.9.0/deploy/static/provider/cloud/deploy.yaml

# Verify installation
kubectl get pods -n cert-manager
kubectl get pods -n ingress-nginx
```

### Deployment Steps

```bash
# 1. Apply namespace with pod security standards
kubectl apply -f k8s/security/pod-security-standards.yml

# 2. Apply network policies
kubectl apply -f k8s/security/network-policies.yml

# 3. Apply WAF configuration
kubectl apply -f k8s/security/waf-modsecurity.yml

# 4. Verify security configuration
kubectl get networkpolicies -n dokus-production
kubectl get resourcequota -n dokus-production
kubectl get limitrange -n dokus-production
kubectl get ingress dokus-ingress-waf -n dokus-production
```

### Post-Deployment Verification

```bash
# Check TLS certificate
kubectl get certificate -n dokus-production

# Test WAF
curl -X GET "https://api.dokus.ai/etc/passwd"  # Should be blocked

# Test rate limiting
for i in {1..25}; do curl -s -o /dev/null -w "%{http_code}\n" https://api.dokus.ai/health; done
# Should see 429 responses

# Check security headers
curl -I https://api.dokus.ai
# Should see: X-Frame-Options, X-Content-Type-Options, etc.
```

---

## Monitoring

### Security Metrics

Monitor these metrics in Prometheus/Grafana:

**WAF Events:**
```promql
rate(nginx_ingress_controller_requests{status="403"}[5m])  # Blocked requests
```

**Rate Limit Hits:**
```promql
rate(nginx_ingress_controller_requests{status="429"}[5m])  # Rate limited
```

**Authentication Failures:**
```promql
rate(http_requests_total{status="401"}[5m])  # Auth failures
```

**Abnormal Error Rates:**
```promql
rate(http_requests_total{status=~"5.."}[5m]) > 0.05  # >5% error rate
```

### Security Alerts

Configure alerts in Prometheus:

```yaml
- alert: HighWAFBlockRate
  expr: rate(nginx_ingress_controller_requests{status="403"}[5m]) > 10
  for: 5m
  annotations:
    summary: "High rate of WAF blocks"

- alert: RateLimitExceeded
  expr: rate(nginx_ingress_controller_requests{status="429"}[5m]) > 5
  for: 2m
  annotations:
    summary: "Many clients hitting rate limits"

- alert: AuthenticationAttack
  expr: rate(http_requests_total{status="401"}[5m]) > 20
  for: 1m
  annotations:
    summary: "Possible authentication brute-force attack"
```

### Log Analysis

```bash
# View WAF logs
kubectl logs -f deployment/nginx-ingress-controller -n ingress-nginx | grep modsec

# View application security logs
kubectl logs -f deployment/dokus-database -n dokus-production | grep -i "security\|auth\|error"

# Export logs to external SIEM
# Configure fluent-bit or fluentd to ship logs to Splunk, ELK, etc.
```

---

## Compliance

### Security Checklist

- [x] TLS 1.2+ with strong ciphers
- [x] HSTS enabled
- [x] WAF with OWASP CRS
- [x] Rate limiting (IP and global)
- [x] Network policies (zero-trust)
- [x] Pod security standards (restricted)
- [x] Non-root containers
- [x] Read-only root filesystems
- [x] Dropped capabilities
- [x] Resource limits
- [x] RBAC (least privilege)
- [x] Secret encryption at rest
- [x] Automated certificate management
- [x] Security headers
- [x] DDoS protection (rate limiting)
- [x] Audit logging
- [x] Monitoring and alerting

### Standards Compliance

**OWASP Top 10 Protection:**
1. ✓ Injection (SQL, XSS) - WAF with CRS
2. ✓ Broken Authentication - JWT validation, rate limiting
3. ✓ Sensitive Data Exposure - TLS, encryption at rest
4. ✓ XML External Entities - Input validation
5. ✓ Broken Access Control - RBAC, network policies
6. ✓ Security Misconfiguration - Pod security standards
7. ✓ XSS - WAF rules, CSP headers
8. ✓ Insecure Deserialization - Input validation
9. ✓ Known Vulnerabilities - Regular scanning
10. ✓ Insufficient Logging - Comprehensive logging

**CIS Kubernetes Benchmark:**
- Pod Security Standards: Restricted
- Network Policies: Enforced
- RBAC: Enabled
- Audit Logging: Enabled
- Secrets Encryption: Configured
- Resource Quotas: Set
- Image Scanning: Automated

### Regular Security Reviews

**Monthly:**
- Review access logs for anomalies
- Update WAF rules based on attack patterns
- Review and rotate secrets
- Update dependencies

**Quarterly:**
- Full penetration testing
- Security architecture review
- Compliance audit
- Disaster recovery drill

**Annually:**
- External security audit
- Certification renewal (if applicable)
- Incident response plan update

---

## Incident Response

### Detection

1. Monitor security alerts in Grafana
2. Review WAF logs daily
3. Check for anomalous traffic patterns
4. Review authentication failures

### Response Procedure

**1. Identify**
```bash
# Check current alerts
kubectl get events -n dokus-production --sort-by='.lastTimestamp'

# Review logs
kubectl logs deployment/dokus-database -n dokus-production --tail=1000

# Check metrics
# Open Grafana dashboard
```

**2. Contain**
```bash
# Block attacker IP at WAF level
kubectl edit ingress dokus-ingress-waf -n dokus-production
# Add to limit-whitelist (or create deny rule)

# Scale down compromised pods
kubectl scale deployment/dokus-database --replicas=0 -n dokus-production

# Isolate with network policy
kubectl apply -f emergency-deny-all.yml
```

**3. Eradicate**
```bash
# Rotate compromised secrets
kubectl delete secret compromised-secret -n dokus-production
kubectl create secret generic ...

# Rebuild and redeploy images
docker build --pull --no-cache ...
kubectl set image deployment/dokus-database ...

# Patch vulnerabilities
./gradlew dependencyUpdates
```

**4. Recover**
```bash
# Restore from backup if needed
./scripts/restore-database.sh

# Scale back up
kubectl scale deployment/dokus-database --replicas=3 -n dokus-production

# Verify health
kubectl get pods -n dokus-production
curl https://api.dokus.ai/health
```

**5. Post-Incident**
- Document incident
- Update runbooks
- Improve detection
- Share lessons learned

---

## Additional Resources

- [OWASP ModSecurity CRS](https://coreruleset.org/)
- [Kubernetes Security Best Practices](https://kubernetes.io/docs/concepts/security/pod-security-standards/)
- [NGINX Ingress Security](https://kubernetes.github.io/ingress-nginx/user-guide/nginx-configuration/annotations/)
- [CIS Kubernetes Benchmark](https://www.cisecurity.org/benchmark/kubernetes)
- [NIST Cybersecurity Framework](https://www.nist.gov/cyberframework)

---

## Support

For security issues:
- Email: security@dokus.ai
- Emergency: Slack #security-incidents
- Regular issues: GitHub Security Advisories

**DO NOT** disclose security vulnerabilities in public issues.
