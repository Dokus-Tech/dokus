/**
 * K6 Spike Test for Dokus Backend
 *
 * Purpose: Test system behavior under sudden traffic spikes
 * Run with: k6 run spike-test.js
 *
 * Test Stages:
 * 1. Normal load: 10 VUs for 1 minute (baseline)
 * 2. Spike: 10 → 200 VUs in 10 seconds (sudden spike)
 * 3. Hold spike: 200 VUs for 1 minute
 * 4. Recovery: 200 → 10 VUs in 10 seconds (spike ends)
 * 5. Post-recovery: 10 VUs for 1 minute (verify recovery)
 *
 * Success Criteria:
 * - System doesn't crash during spike
 * - Error rate < 5% during spike
 * - System returns to normal performance after spike
 * - No cascading failures
 */

import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate, Trend, Counter } from 'k6/metrics';
import { randomIntBetween } from 'https://jslib.k6.io/k6-utils/1.2.0/index.js';

// Custom metrics
const errorRate = new Rate('errors');
const apiLatency = new Trend('api_latency', true);
const requestCounter = new Counter('total_requests');
const spikeErrors = new Counter('spike_phase_errors');
const recoveryErrors = new Counter('recovery_phase_errors');

// Test configuration
export const options = {
    stages: [
        { duration: '1m', target: 10 },     // Normal load baseline
        { duration: '10s', target: 200 },   // Sudden spike!
        { duration: '1m', target: 200 },    // Hold spike
        { duration: '10s', target: 10 },    // Spike ends
        { duration: '2m', target: 10 },     // Recovery verification
    ],
    thresholds: {
        'http_req_duration': ['p(95)<200'], // Slightly relaxed during spike
        'http_req_failed': ['rate<0.05'], // Allow up to 5% errors
        'errors': ['rate<0.05'],
    },
};

// Environment configuration
const BASE_URL = __ENV.BASE_URL || 'http://localhost:9070';
const AUTH_SERVICE_URL = __ENV.AUTH_SERVICE_URL || 'http://localhost:8091';

// Test phases for tracking
let currentPhase = 'baseline';

export function setup() {
    console.log(`Starting spike test against ${BASE_URL}`);
    console.log('⚡ This test simulates sudden traffic spikes');

    // Verify system is healthy before spike
    const healthCheck = http.get(`${BASE_URL}/health`);
    check(healthCheck, {
        'system is healthy before spike': (r) => r.status === 200,
    });

    return { baseUrl: BASE_URL, authUrl: AUTH_SERVICE_URL };
}

export default function(data) {
    requestCounter.add(1);

    // Determine current phase based on time
    const elapsed = __VU * __ITER; // Approximation
    if (__VU > 100) {
        currentPhase = 'spike';
    } else if (__VU < 20 && elapsed > 100) {
        currentPhase = 'recovery';
    }

    const headers = {
        'Content-Type': 'application/json',
    };

    // Health check
    const healthRes = http.get(`${data.baseUrl}/health`, {
        headers,
        timeout: '5s',
    });

    const healthSuccess = check(healthRes, {
        'health check responds': (r) => r.status === 200,
        'health check latency acceptable': (r) => r.timings.duration < 500,
    });

    if (!healthSuccess) {
        errorRate.add(1);
        if (currentPhase === 'spike') {
            spikeErrors.add(1);
        } else if (currentPhase === 'recovery') {
            recoveryErrors.add(1);
        }
    }

    apiLatency.add(healthRes.timings.duration);

    // Metrics endpoint check
    const metricsRes = http.get(`${data.baseUrl}/metrics`, {
        headers,
        timeout: '5s',
    });

    check(metricsRes, {
        'metrics endpoint responds': (r) => r.status === 200 || r.status === 404,
    }) || errorRate.add(1);

    // RPC call simulation (50% of requests)
    if (Math.random() < 0.5) {
        const rpcPayload = JSON.stringify({
            jsonrpc: '2.0',
            method: 'TenantService/listTenants',
            params: { limit: 10, offset: 0 },
            id: randomIntBetween(1, 100000),
        });

        const rpcRes = http.post(
            `${data.baseUrl}/api/rpc`,
            rpcPayload,
            {
                headers,
                timeout: '5s',
            }
        );

        requestCounter.add(1);

        const rpcSuccess = check(rpcRes, {
            'rpc call completes': (r) => r.status === 200 || r.status === 401 || r.status === 403,
        });

        if (!rpcSuccess) {
            errorRate.add(1);
            if (currentPhase === 'spike') {
                spikeErrors.add(1);
            } else if (currentPhase === 'recovery') {
                recoveryErrors.add(1);
            }
        }

        apiLatency.add(rpcRes.timings.duration);
    }

    // Short think time
    sleep(randomIntBetween(0.5, 1.5));
}

export function teardown(data) {
    console.log(`Spike test completed for ${data.baseUrl}`);
    console.log('Verify recovery metrics to ensure system stability');
}

export function handleSummary(data) {
    const metrics = data.metrics;

    console.log('\n========================================');
    console.log('SPIKE TEST RESULTS');
    console.log('========================================');

    if (metrics.http_reqs) {
        console.log(`Total Requests: ${metrics.http_reqs.values.count}`);
    }

    if (metrics.errors) {
        console.log(`Overall Error Rate: ${(metrics.errors.values.rate * 100).toFixed(2)}%`);
    }

    if (metrics.spike_phase_errors) {
        console.log(`Errors During Spike: ${metrics.spike_phase_errors.values.count}`);
    }

    if (metrics.recovery_phase_errors) {
        console.log(`Errors During Recovery: ${metrics.recovery_phase_errors.values.count}`);
    }

    if (metrics.http_req_duration) {
        console.log(`\nLatency During Test:`);
        console.log(`  p50: ${metrics.http_req_duration.values['p(50)'].toFixed(2)}ms`);
        console.log(`  p95: ${metrics.http_req_duration.values['p(95)'].toFixed(2)}ms`);
        console.log(`  p99: ${metrics.http_req_duration.values['p(99)'].toFixed(2)}ms`);
        console.log(`  max: ${metrics.http_req_duration.values.max.toFixed(2)}ms`);
    }

    // Evaluate results
    const passedSpike = metrics.spike_phase_errors
        ? metrics.spike_phase_errors.values.count < (metrics.http_reqs.values.count * 0.05)
        : true;

    const recovered = metrics.recovery_phase_errors
        ? metrics.recovery_phase_errors.values.count < (metrics.http_reqs.values.count * 0.01)
        : true;

    console.log(`\nSpike Handling: ${passedSpike ? '✓ PASSED' : '✗ FAILED'}`);
    console.log(`Recovery: ${recovered ? '✓ PASSED' : '✗ FAILED'}`);
    console.log('========================================\n');

    return {
        'stdout': JSON.stringify(data, null, 2),
        'summary.json': JSON.stringify(data),
    };
}
