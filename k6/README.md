# K6 Load Testing Suite for Dokus Backend

This directory contains comprehensive load tests for the Dokus backend using [K6](https://k6.io/).

## Prerequisites

### Install K6

**macOS:**
```bash
brew install k6
```

**Linux:**
```bash
sudo gpg -k
sudo gpg --no-default-keyring --keyring /usr/share/keyrings/k6-archive-keyring.gpg --keyserver hkp://keyserver.ubuntu.com:80 --recv-keys C5AD17C747E3415A3642D57D77C6C491D6AC1D69
echo "deb [signed-by=/usr/share/keyrings/k6-archive-keyring.gpg] https://dl.k6.io/deb stable main" | sudo tee /etc/apt/sources.list.d/k6.list
sudo apt-get update
sudo apt-get install k6
```

**Windows:**
```powershell
choco install k6
```

**Docker:**
```bash
docker pull grafana/k6
```

## Test Suite Overview

### 1. Smoke Test (`smoke-test.js`)

**Purpose:** Verify the system works with minimal load
**Duration:** 1 minute
**Virtual Users:** 5
**Run:** `k6 run load-tests/smoke-test.js`

**When to run:**
- After deploying changes
- Before running other tests
- As part of CI/CD pipeline

**Success Criteria:**
- All endpoints respond
- No errors
- p95 latency < 100ms

---

### 2. Load Test (`load-test.js`)

**Purpose:** Test system under normal and peak load conditions
**Duration:** 16 minutes
**Virtual Users:** 0 → 50 → 100 → 0
**Run:** `k6 run load-tests/load-test.js`

**Test Profile:**
- Ramp-up: 0 → 50 VUs over 2 minutes
- Steady: 50 VUs for 5 minutes
- Peak: 50 → 100 VUs over 2 minutes
- Steady Peak: 100 VUs for 5 minutes
- Ramp-down: 100 → 0 VUs over 2 minutes

**Success Criteria:**
- p95 latency < 100ms
- p99 latency < 200ms
- Error rate < 1%
- Throughput > 500 req/s

---

### 3. Stress Test (`stress-test.js`)

**Purpose:** Find the breaking point of the system
**Duration:** 22 minutes
**Virtual Users:** 0 → 400 → 0
**Run:** `k6 run load-tests/stress-test.js`

**Test Profile:**
- Warm-up: 0 → 100 VUs over 2 minutes
- Ramp to stress: 100 → 400 VUs over 10 minutes
- Hold: 400 VUs for 5 minutes
- Recovery: 400 → 0 VUs over 3 minutes

**Goal:** Identify maximum capacity and failure modes

---

### 4. Spike Test (`spike-test.js`)

**Purpose:** Test system behavior under sudden traffic spikes
**Duration:** 4 minutes 20 seconds
**Virtual Users:** 10 → 200 → 10
**Run:** `k6 run load-tests/spike-test.js`

**Test Profile:**
- Baseline: 10 VUs for 1 minute
- Spike: 10 → 200 VUs in 10 seconds
- Hold: 200 VUs for 1 minute
- Recovery: 200 → 10 VUs in 10 seconds
- Post-recovery: 10 VUs for 2 minutes

**Success Criteria:**
- System doesn't crash
- Error rate < 5% during spike
- System recovers to normal performance

---

### 5. Soak Test (`soak-test.js`)

**Purpose:** Verify system stability over extended period
**Duration:** 2 hours 10 minutes
**Virtual Users:** 30 (constant)
**Run:** `k6 run load-tests/soak-test.js`

**Test Profile:**
- Ramp-up: 0 → 30 VUs over 5 minutes
- Soak: 30 VUs for 2 hours
- Ramp-down: 30 → 0 VUs over 5 minutes

**Goal:** Detect memory leaks, performance degradation, resource cleanup issues

---

## Configuration

All tests support environment variables for configuration:

```bash
# Database service URL
export BASE_URL="http://localhost:9070"

# Auth service URL
export AUTH_SERVICE_URL="http://localhost:8091"

# Test credentials (for load test)
export TEST_EMAIL="test@dokus.ai"
export TEST_PASSWORD="TestPassword123!"
```

### Running Tests Against Production

**⚠️ CAUTION:** Never run stress or soak tests against production without approval!

```bash
# Load test against staging
BASE_URL="https://api-staging.dokus.ai" k6 run load-tests/load-test.js

# Smoke test against production
BASE_URL="https://api.dokus.ai" k6 run load-tests/smoke-test.js
```

---

## Output Options

### Console Output (default)
```bash
k6 run load-tests/smoke-test.js
```

### JSON Output
```bash
k6 run --out json=results.json load-tests/load-test.js
```

### InfluxDB Integration
```bash
k6 run --out influxdb=http://localhost:8086/k6 load-tests/load-test.js
```

### Grafana Cloud
```bash
K6_CLOUD_TOKEN=<your-token> k6 run --out cloud load-tests/load-test.js
```

### CSV Export
```bash
k6 run --out csv=results.csv load-tests/load-test.js
```

---

## Recommended Test Sequence

### Before Deployment
```bash
# 1. Smoke test (quick sanity check)
k6 run load-tests/smoke-test.js

# 2. Load test (verify performance under expected load)
k6 run load-tests/load-test.js

# 3. Spike test (verify resilience)
k6 run load-tests/spike-test.js
```

### After Deployment
```bash
# Production smoke test
BASE_URL="https://api.dokus.ai" k6 run load-tests/smoke-test.js
```

### Performance Validation
```bash
# Full validation suite (long-running)
./run-all-tests.sh
```

### Before Major Release
```bash
# 1. Full load test
k6 run load-tests/load-test.js

# 2. Stress test (find limits)
k6 run load-tests/stress-test.js

# 3. Soak test (verify stability) - overnight
k6 run load-tests/soak-test.js
```

---

## Interpreting Results

### Key Metrics

**http_req_duration:**
- p50 (median): Typical response time
- p95: 95% of requests complete within this time
- p99: 99% of requests complete within this time

**http_req_failed:**
- Rate: Percentage of failed requests
- Should be < 1% under normal conditions

**http_reqs:**
- Rate: Requests per second (throughput)
- Target: > 500 req/s for load test

**Custom Metrics:**
- `api_latency`: Application-level latency
- `invoice_creation_time`: Time to create invoices
- `client_lookup_time`: Time to query clients
- `authentication_time`: Login flow duration

### Success Indicators

✅ **Passed:**
- All thresholds met
- Latency stable throughout test
- No cascading failures
- Error rate < 1%

⚠️ **Warning:**
- Thresholds barely met
- Latency increases over time
- Error rate 1-5%

❌ **Failed:**
- Thresholds exceeded
- System crashes
- Error rate > 5%
- Performance degrades significantly

---

## Troubleshooting

### High Latency

**Possible causes:**
- Database connection pool exhaustion
- CPU/memory constraints
- Network bottlenecks
- Inefficient queries

**Investigation:**
```bash
# Check database connections
kubectl exec -it postgres-0 -- psql -U produser -d dokus -c "SELECT count(*) FROM pg_stat_activity;"

# Check pod resources
kubectl top pods -n dokus-production

# Check Prometheus metrics
curl http://localhost:9090/api/v1/query?query=rate(http_request_duration_seconds[5m])
```

### High Error Rate

**Possible causes:**
- Authentication failures
- Database unavailability
- Rate limiting
- Service crashes

**Investigation:**
```bash
# Check pod logs
kubectl logs -f deployment/dokus-database -n dokus-production

# Check auth service
kubectl logs -f deployment/dokus-auth -n dokus-production

# Check Grafana dashboards
open https://grafana.dokus.ai
```

### Timeouts

**Possible causes:**
- Insufficient resources
- Database locks
- Network issues

**Resolution:**
- Increase timeout in test (not recommended for production tests)
- Scale horizontally (increase replicas)
- Optimize database queries

---

## Integration with CI/CD

### GitHub Actions Example

```yaml
- name: Run K6 Load Tests
  run: |
    k6 run --out json=results.json k6/load-tests/smoke-test.js
    k6 run --out json=results.json k6/load-tests/load-test.js

- name: Upload Results
  uses: actions/upload-artifact@v3
  with:
    name: k6-results
    path: results.json
```

### Docker Compose Example

```yaml
version: '3.8'
services:
  k6:
    image: grafana/k6
    volumes:
      - ./k6:/k6
    environment:
      - BASE_URL=http://database:9070
    command: run /k6/load-tests/smoke-test.js
```

---

## Monitoring During Tests

### Grafana Dashboard

Access the Dokus Grafana dashboard at `https://grafana.dokus.ai` and monitor:

- Request rate
- Latency percentiles (p50, p95, p99)
- Error rate
- Database connection pool usage
- CPU and memory usage
- Pod restarts

### Prometheus Queries

```promql
# Request rate
rate(http_requests_total[5m])

# p95 latency
histogram_quantile(0.95, rate(http_request_duration_seconds_bucket[5m]))

# Error rate
rate(http_requests_total{status=~"5.."}[5m])

# Database connections
hikaricp_connections_active / hikaricp_connections_total
```

---

## Best Practices

1. **Start small:** Always run smoke test first
2. **Monitor resources:** Watch CPU, memory, and database connections
3. **Ramp gradually:** Don't jump to maximum load immediately
4. **Isolate tests:** Run tests against dedicated test/staging environments
5. **Document baseline:** Record performance metrics after each major release
6. **Automate:** Integrate tests into CI/CD pipeline
7. **Review regularly:** Update test scenarios to match production traffic patterns

---

## Resources

- [K6 Documentation](https://k6.io/docs/)
- [K6 Best Practices](https://k6.io/docs/testing-guides/test-types/)
- [Dokus Monitoring Guide](../k8s/README.md#monitoring)
- [Prometheus Metrics](../k8s/monitoring/prometheus.yml)

---

## Support

For issues or questions:
- Check logs: `kubectl logs -f deployment/dokus-database -n dokus-production`
- Review Grafana: https://grafana.dokus.ai
- Contact DevOps team: devops@dokus.ai
