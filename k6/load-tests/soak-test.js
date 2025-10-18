/**
 * K6 Soak Test (Endurance Test) for Dokus Backend
 *
 * Purpose: Verify system stability over extended period
 * Run with: k6 run soak-test.js
 *
 * Test Duration: 2 hours
 * Test Load: Constant 30 VUs (moderate load)
 *
 * Goals:
 * - Identify memory leaks
 * - Detect performance degradation over time
 * - Verify resource cleanup (connections, file handles, etc.)
 * - Ensure no cascading failures
 *
 * Success Criteria:
 * - Latency remains stable (p95 < 100ms throughout)
 * - No memory leaks (monitor externally)
 * - Error rate stays below 1%
 * - Response times don't degrade over time
 */

import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate, Trend, Counter } from 'k6/metrics';
import { randomIntBetween, randomString } from 'https://jslib.k6.io/k6-utils/1.2.0/index.js';

// Custom metrics
const errorRate = new Rate('errors');
const apiLatency = new Trend('api_latency', true);
const requestCounter = new Counter('total_requests');
const latencyDrift = new Trend('latency_drift', true);

// Baseline tracking
let baselineLatency = 0;
let iterationCount = 0;

// Test configuration
export const options = {
    stages: [
        { duration: '5m', target: 30 },     // Ramp-up to steady load
        { duration: '2h', target: 30 },     // Soak for 2 hours
        { duration: '5m', target: 0 },      // Ramp-down
    ],
    thresholds: {
        'http_req_duration': ['p(95)<100', 'p(99)<200'], // Latency must stay consistent
        'http_req_failed': ['rate<0.01'], // Error rate must stay below 1%
        'errors': ['rate<0.01'],
        'latency_drift': ['p(95)<50'], // Latency shouldn't drift more than 50ms from baseline
    },
};

// Environment configuration
const BASE_URL = __ENV.BASE_URL || 'http://localhost:9070';
const AUTH_SERVICE_URL = __ENV.AUTH_SERVICE_URL || 'http://localhost:8091';

export function setup() {
    console.log(`Starting soak test against ${BASE_URL}`);
    console.log('⏱️  This test will run for 2 hours');
    console.log('Monitor system resources (memory, CPU, connections) externally');

    // Verify system is healthy
    const healthCheck = http.get(`${BASE_URL}/health`);
    check(healthCheck, {
        'system is healthy before soak': (r) => r.status === 200,
    });

    // Establish baseline
    const samples = [];
    for (let i = 0; i < 10; i++) {
        const res = http.get(`${BASE_URL}/health`);
        samples.push(res.timings.duration);
    }
    const baseline = samples.reduce((a, b) => a + b, 0) / samples.length;

    console.log(`Baseline latency: ${baseline.toFixed(2)}ms`);

    return {
        baseUrl: BASE_URL,
        authUrl: AUTH_SERVICE_URL,
        baselineLatency: baseline,
    };
}

export default function(data) {
    requestCounter.add(1);
    iterationCount++;

    const headers = {
        'Content-Type': 'application/json',
    };

    // Health check - track latency over time
    const healthStart = new Date().getTime();
    const healthRes = http.get(`${data.baseUrl}/health`, {
        headers,
        timeout: '10s',
    });
    const healthDuration = new Date().getTime() - healthStart;

    const healthSuccess = check(healthRes, {
        'health check responds': (r) => r.status === 200,
        'health check is fast': (r) => r.timings.duration < 200,
    });

    if (!healthSuccess) {
        errorRate.add(1);
    }

    apiLatency.add(healthRes.timings.duration);

    // Track latency drift from baseline
    const drift = Math.abs(healthDuration - data.baselineLatency);
    latencyDrift.add(drift);

    // Every 100 iterations, check for significant drift
    if (iterationCount % 100 === 0) {
        if (drift > data.baselineLatency * 0.5) {
            console.warn(`⚠️  Latency drift detected: +${drift.toFixed(2)}ms from baseline`);
        }
    }

    sleep(randomIntBetween(1, 2));

    // RPC operations - simulate realistic workload
    const operations = [
        'TenantService/listTenants',
        'ClientService/listClients',
        'InvoiceService/listInvoices',
        'PaymentService/listPayments',
    ];

    const randomOp = operations[randomIntBetween(0, operations.length - 1)];

    const rpcPayload = JSON.stringify({
        jsonrpc: '2.0',
        method: randomOp,
        params: { limit: 20, offset: 0 },
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

    check(rpcRes, {
        'rpc call completes': (r) => r.status === 200 || r.status === 401 || r.status === 403,
        'rpc response is valid': (r) => {
            try {
                const body = JSON.parse(r.body);
                return body.result !== undefined || body.error !== undefined;
            } catch (e) {
                return false;
            }
        },
    }) || errorRate.add(1);

    apiLatency.add(rpcRes.timings.duration);

    sleep(randomIntBetween(2, 4));

    // Occasionally create resources (10% of requests)
    if (Math.random() < 0.1) {
        const createClientPayload = JSON.stringify({
            jsonrpc: '2.0',
            method: 'ClientService/createClient',
            params: {
                name: `Test Client ${randomString(8)}`,
                email: `test-${randomString(8)}@example.com`,
                vatNumber: `BE${randomIntBetween(1000000000, 9999999999)}`,
                address: {
                    street: `Street ${randomIntBetween(1, 100)}`,
                    city: 'Brussels',
                    postalCode: `1${randomIntBetween(000, 999)}`,
                    country: 'BE',
                },
            },
            id: randomIntBetween(1, 100000),
        });

        const createRes = http.post(
            `${data.baseUrl}/api/rpc`,
            createClientPayload,
            {
                headers,
                timeout: '10s',
            }
        );

        requestCounter.add(1);

        check(createRes, {
            'create operation completes': (r) => r.status !== 0,
        }) || errorRate.add(1);

        apiLatency.add(createRes.timings.duration);
    }

    // Think time
    sleep(randomIntBetween(3, 5));
}

export function teardown(data) {
    console.log(`Soak test completed for ${data.baseUrl}`);
    console.log('Review metrics for performance degradation over time');
}

export function handleSummary(data) {
    const metrics = data.metrics;

    console.log('\n========================================');
    console.log('SOAK TEST RESULTS (2 HOURS)');
    console.log('========================================');

    if (metrics.http_reqs) {
        console.log(`Total Requests: ${metrics.http_reqs.values.count}`);
        console.log(`Request Rate: ${metrics.http_reqs.values.rate.toFixed(2)} req/s`);
    }

    if (metrics.errors) {
        console.log(`Error Rate: ${(metrics.errors.values.rate * 100).toFixed(4)}%`);
    }

    if (metrics.http_req_duration) {
        console.log(`\nLatency Statistics:`);
        console.log(`  Average: ${metrics.http_req_duration.values.avg.toFixed(2)}ms`);
        console.log(`  p50: ${metrics.http_req_duration.values['p(50)'].toFixed(2)}ms`);
        console.log(`  p95: ${metrics.http_req_duration.values['p(95)'].toFixed(2)}ms`);
        console.log(`  p99: ${metrics.http_req_duration.values['p(99)'].toFixed(2)}ms`);
        console.log(`  max: ${metrics.http_req_duration.values.max.toFixed(2)}ms`);
    }

    if (metrics.latency_drift) {
        console.log(`\nLatency Drift:`);
        console.log(`  Average Drift: ${metrics.latency_drift.values.avg.toFixed(2)}ms`);
        console.log(`  p95 Drift: ${metrics.latency_drift.values['p(95)'].toFixed(2)}ms`);
    }

    // Evaluate stability
    const stableLatency = metrics.http_req_duration
        ? metrics.http_req_duration.values['p(95)'] < 100
        : false;

    const lowErrors = metrics.errors
        ? metrics.errors.values.rate < 0.01
        : false;

    const noDrift = metrics.latency_drift
        ? metrics.latency_drift.values['p(95)'] < 50
        : true;

    console.log(`\nStability Assessment:`);
    console.log(`  Latency Stable: ${stableLatency ? '✓ PASSED' : '✗ FAILED'}`);
    console.log(`  Low Error Rate: ${lowErrors ? '✓ PASSED' : '✗ FAILED'}`);
    console.log(`  No Degradation: ${noDrift ? '✓ PASSED' : '✗ FAILED'}`);
    console.log('========================================\n');

    // Output detailed results
    return {
        'stdout': JSON.stringify(data, null, 2),
        'summary.json': JSON.stringify(data),
        'soak-test-report.html': htmlReport(data),
    };
}

function htmlReport(data) {
    // Basic HTML report template
    return `
<!DOCTYPE html>
<html>
<head>
    <title>Dokus Soak Test Report</title>
    <style>
        body { font-family: Arial, sans-serif; margin: 40px; }
        h1 { color: #333; }
        .metric { margin: 20px 0; }
        .pass { color: green; }
        .fail { color: red; }
    </style>
</head>
<body>
    <h1>Dokus Backend Soak Test Report</h1>
    <div class="metric">
        <h2>Test Duration: 2 hours</h2>
        <p>Total Requests: ${data.metrics.http_reqs ? data.metrics.http_reqs.values.count : 'N/A'}</p>
        <p>Error Rate: ${data.metrics.errors ? (data.metrics.errors.values.rate * 100).toFixed(4) : 'N/A'}%</p>
    </div>
    <div class="metric">
        <h2>Latency</h2>
        <p>p95: ${data.metrics.http_req_duration ? data.metrics.http_req_duration.values['p(95)'].toFixed(2) : 'N/A'}ms</p>
        <p>p99: ${data.metrics.http_req_duration ? data.metrics.http_req_duration.values['p(99)'].toFixed(2) : 'N/A'}ms</p>
    </div>
</body>
</html>
`;
}
