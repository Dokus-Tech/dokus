/**
 * K6 Stress Test for Dokus Backend
 *
 * Purpose: Find the breaking point of the system
 * Run with: k6 run stress-test.js
 *
 * Test Stages:
 * 1. Warm-up: 0 → 100 VUs over 2 minutes
 * 2. Ramp to breaking point: 100 → 400 VUs over 10 minutes
 * 3. Hold at breaking point: 400 VUs for 5 minutes
 * 4. Recovery: 400 → 0 VUs over 3 minutes
 *
 * Goal: Identify:
 * - Maximum throughput
 * - Resource exhaustion points
 * - Error patterns under extreme load
 * - Recovery time after stress
 */

import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate, Trend, Counter } from 'k6/metrics';
import { randomIntBetween } from 'https://jslib.k6.io/k6-utils/1.2.0/index.js';

// Custom metrics
const errorRate = new Rate('errors');
const apiLatency = new Trend('api_latency', true);
const requestCounter = new Counter('total_requests');
const successCounter = new Counter('successful_requests');
const timeoutCounter = new Counter('timeout_errors');

// Test configuration
export const options = {
    stages: [
        { duration: '2m', target: 100 },   // Warm-up
        { duration: '5m', target: 200 },   // Approaching normal
        { duration: '5m', target: 300 },   // Beyond normal capacity
        { duration: '5m', target: 400 },   // Stress
        { duration: '5m', target: 400 },   // Hold at stress
        { duration: '3m', target: 0 },     // Recovery
    ],
    thresholds: {
        // Relaxed thresholds - we expect degradation
        'http_req_duration': ['p(95)<500', 'p(99)<2000'],
        'http_req_failed': ['rate<0.1'], // Allow up to 10% errors
        'errors': ['rate<0.1'],
    },
};

// Environment configuration
const BASE_URL = __ENV.BASE_URL || 'http://localhost:9070';

export function setup() {
    console.log(`Starting stress test against ${BASE_URL}`);
    console.log('⚠️  This test WILL push the system to its limits');

    return { baseUrl: BASE_URL };
}

export default function(data) {
    requestCounter.add(1);

    const headers = {
        'Content-Type': 'application/json',
    };

    // Health check - most basic endpoint
    const healthRes = http.get(`${data.baseUrl}/health`, {
        headers,
        timeout: '10s', // Increased timeout for stress conditions
    });

    const healthSuccess = check(healthRes, {
        'health check completed': (r) => r.status !== 0, // Any response is better than timeout
        'health check status is 200': (r) => r.status === 200,
        'health check response time acceptable': (r) => r.timings.duration < 1000,
    });

    if (healthSuccess) {
        successCounter.add(1);
        apiLatency.add(healthRes.timings.duration);
    } else {
        errorRate.add(1);
        if (healthRes.status === 0) {
            timeoutCounter.add(1);
        }
    }

    // Simulate random workload
    if (Math.random() < 0.3) {
        // 30% of requests try RPC calls
        const rpcPayload = JSON.stringify({
            jsonrpc: '2.0',
            method: 'TenantService/getTenantInfo',
            params: { tenantId: `tenant-${randomIntBetween(1, 100)}` },
            id: randomIntBetween(1, 100000),
        });

        const rpcRes = http.post(
            `${data.baseUrl}/api/rpc`,
            rpcPayload,
            {
                headers,
                timeout: '10s',
            }
        );

        requestCounter.add(1);

        const rpcSuccess = check(rpcRes, {
            'rpc call completed': (r) => r.status !== 0,
        });

        if (rpcSuccess) {
            successCounter.add(1);
            apiLatency.add(rpcRes.timings.duration);
        } else {
            errorRate.add(1);
            if (rpcRes.status === 0) {
                timeoutCounter.add(1);
            }
        }
    }

    // Very short think time to maximize load
    sleep(randomIntBetween(0.1, 0.5));
}

export function teardown(data) {
    console.log(`Stress test completed for ${data.baseUrl}`);
    console.log('Check metrics to identify breaking point');
}

export function handleSummary(data) {
    // Custom summary to highlight stress test insights
    const metrics = data.metrics;

    console.log('\n========================================');
    console.log('STRESS TEST RESULTS');
    console.log('========================================');

    if (metrics.http_reqs) {
        console.log(`Total Requests: ${metrics.http_reqs.values.count}`);
    }

    if (metrics.successful_requests) {
        console.log(`Successful Requests: ${metrics.successful_requests.values.count}`);
    }

    if (metrics.errors) {
        console.log(`Error Rate: ${(metrics.errors.values.rate * 100).toFixed(2)}%`);
    }

    if (metrics.timeout_errors) {
        console.log(`Timeout Errors: ${metrics.timeout_errors.values.count}`);
    }

    if (metrics.http_req_duration) {
        console.log(`\nLatency Percentiles:`);
        console.log(`  p50: ${metrics.http_req_duration.values['p(50)'].toFixed(2)}ms`);
        console.log(`  p95: ${metrics.http_req_duration.values['p(95)'].toFixed(2)}ms`);
        console.log(`  p99: ${metrics.http_req_duration.values['p(99)'].toFixed(2)}ms`);
    }

    console.log('========================================\n');

    return {
        'stdout': JSON.stringify(data, null, 2),
        'summary.json': JSON.stringify(data),
    };
}
