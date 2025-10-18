/**
 * K6 Smoke Test for Dokus Backend
 *
 * Purpose: Verify system works with minimal load (1-5 VUs for 1 minute)
 * Run with: k6 run smoke-test.js
 *
 * Success Criteria:
 * - No errors
 * - p95 latency < 100ms
 * - All health checks pass
 */

import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate, Trend } from 'k6/metrics';

// Custom metrics
const errorRate = new Rate('errors');
const apiLatency = new Trend('api_latency', true);

// Test configuration
export const options = {
    vus: 5, // 5 virtual users
    duration: '1m', // 1 minute
    thresholds: {
        'http_req_duration': ['p(95)<100'], // 95% of requests must complete below 100ms
        'http_req_failed': ['rate<0.01'], // Error rate must be below 1%
        'errors': ['rate<0.01'], // Custom error rate below 1%
    },
};

// Environment configuration
const BASE_URL = __ENV.BASE_URL || 'http://localhost:9070';
const AUTH_TOKEN = __ENV.AUTH_TOKEN || '';

export function setup() {
    console.log(`Starting smoke test against ${BASE_URL}`);

    // Verify health endpoint is reachable
    const healthCheck = http.get(`${BASE_URL}/health`);
    check(healthCheck, {
        'health endpoint is reachable': (r) => r.status === 200,
    });

    return { baseUrl: BASE_URL, token: AUTH_TOKEN };
}

export default function(data) {
    const headers = {
        'Content-Type': 'application/json',
    };

    if (data.token) {
        headers['Authorization'] = `Bearer ${data.token}`;
    }

    // Test 1: Health check
    const healthRes = http.get(`${data.baseUrl}/health`, { headers });
    check(healthRes, {
        'health check status is 200': (r) => r.status === 200,
        'health check response is valid': (r) => {
            try {
                const body = JSON.parse(r.body);
                return body.status === 'ok' || body.status === 'healthy';
            } catch (e) {
                return false;
            }
        },
    }) || errorRate.add(1);

    apiLatency.add(healthRes.timings.duration);

    sleep(1);

    // Test 2: Metrics endpoint
    const metricsRes = http.get(`${data.baseUrl}/metrics`, { headers });
    check(metricsRes, {
        'metrics endpoint accessible': (r) => r.status === 200 || r.status === 404, // 404 is ok if not exposed
    }) || errorRate.add(1);

    sleep(1);
}

export function teardown(data) {
    console.log(`Smoke test completed for ${data.baseUrl}`);
}
