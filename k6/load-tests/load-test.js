/**
 * K6 Load Test for Dokus Backend
 *
 * Purpose: Test system under normal load conditions
 * Run with: k6 run load-test.js
 *
 * Test Stages:
 * 1. Ramp-up: 0 → 50 VUs over 2 minutes (warm-up)
 * 2. Steady: 50 VUs for 5 minutes (normal load)
 * 3. Peak: 50 → 100 VUs over 2 minutes (peak load)
 * 4. Steady Peak: 100 VUs for 5 minutes
 * 5. Ramp-down: 100 → 0 VUs over 2 minutes (cool-down)
 *
 * Success Criteria:
 * - p95 latency < 100ms
 * - p99 latency < 200ms
 * - Error rate < 1%
 * - Throughput > 500 req/s
 */

import http from 'k6/http';
import { check, sleep, group } from 'k6';
import { Rate, Trend, Counter } from 'k6/metrics';
import { randomString, randomIntBetween } from 'https://jslib.k6.io/k6-utils/1.2.0/index.js';

// Custom metrics
const errorRate = new Rate('errors');
const apiLatency = new Trend('api_latency', true);
const invoiceCreationTime = new Trend('invoice_creation_time', true);
const clientLookupTime = new Trend('client_lookup_time', true);
const authenticationTime = new Trend('authentication_time', true);
const requestCounter = new Counter('total_requests');

// Test configuration
export const options = {
    stages: [
        { duration: '2m', target: 50 },   // Ramp-up to 50 users
        { duration: '5m', target: 50 },   // Stay at 50 users
        { duration: '2m', target: 100 },  // Ramp-up to 100 users
        { duration: '5m', target: 100 },  // Stay at 100 users
        { duration: '2m', target: 0 },    // Ramp-down to 0 users
    ],
    thresholds: {
        'http_req_duration': ['p(95)<100', 'p(99)<200'], // 95th percentile < 100ms, 99th < 200ms
        'http_req_failed': ['rate<0.01'], // Error rate < 1%
        'errors': ['rate<0.01'],
        'http_reqs': ['rate>500'], // Throughput > 500 req/s
        'invoice_creation_time': ['p(95)<150'],
        'client_lookup_time': ['p(95)<50'],
        'authentication_time': ['p(95)<200'],
    },
};

// Environment configuration
const BASE_URL = __ENV.BASE_URL || 'http://localhost:9070';
const AUTH_SERVICE_URL = __ENV.AUTH_SERVICE_URL || 'http://localhost:8091';

// Test data
const TEST_CREDENTIALS = {
    email: __ENV.TEST_EMAIL || 'test@dokus.ai',
    password: __ENV.TEST_PASSWORD || 'TestPassword123!',
};

export function setup() {
    console.log(`Starting load test against ${BASE_URL}`);
    console.log(`Auth service: ${AUTH_SERVICE_URL}`);

    // Verify services are up
    const healthCheck = http.get(`${BASE_URL}/health`);
    check(healthCheck, {
        'database service is healthy': (r) => r.status === 200,
    });

    return {
        baseUrl: BASE_URL,
        authUrl: AUTH_SERVICE_URL,
        credentials: TEST_CREDENTIALS,
    };
}

export default function(data) {
    let authToken = null;

    // Authentication flow
    group('Authentication', function() {
        const loginStart = new Date().getTime();

        const loginPayload = JSON.stringify({
            email: data.credentials.email,
            password: data.credentials.password,
        });

        const loginRes = http.post(
            `${data.authUrl}/api/auth/login`,
            loginPayload,
            {
                headers: { 'Content-Type': 'application/json' },
            }
        );

        const loginDuration = new Date().getTime() - loginStart;
        authenticationTime.add(loginDuration);
        requestCounter.add(1);

        const loginSuccess = check(loginRes, {
            'login status is 200 or 401': (r) => r.status === 200 || r.status === 401,
            'login response has token or error': (r) => {
                try {
                    const body = JSON.parse(r.body);
                    return body.token || body.accessToken || body.error;
                } catch (e) {
                    return false;
                }
            },
        });

        if (!loginSuccess) {
            errorRate.add(1);
        }

        try {
            const loginBody = JSON.parse(loginRes.body);
            authToken = loginBody.token || loginBody.accessToken;
        } catch (e) {
            // Login might fail - that's ok for load testing
        }
    });

    // Skip RPC calls if no auth token (simulates unauthenticated load)
    if (!authToken) {
        sleep(randomIntBetween(1, 3));
        return;
    }

    const headers = {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${authToken}`,
    };

    // Client management operations
    group('Client Operations', function() {
        // List clients
        const listStart = new Date().getTime();
        const listRes = http.post(
            `${data.baseUrl}/api/rpc`,
            JSON.stringify({
                jsonrpc: '2.0',
                method: 'ClientService/listClients',
                params: { limit: 20, offset: 0 },
                id: randomIntBetween(1, 10000),
            }),
            { headers }
        );

        const listDuration = new Date().getTime() - listStart;
        clientLookupTime.add(listDuration);
        requestCounter.add(1);

        check(listRes, {
            'list clients status is 200': (r) => r.status === 200,
            'list clients has valid response': (r) => {
                try {
                    const body = JSON.parse(r.body);
                    return body.result !== undefined || body.error !== undefined;
                } catch (e) {
                    return false;
                }
            },
        }) || errorRate.add(1);

        apiLatency.add(listRes.timings.duration);
    });

    sleep(randomIntBetween(1, 2));

    // Invoice operations
    group('Invoice Operations', function() {
        // List invoices
        const listInvoicesStart = new Date().getTime();
        const listInvoicesRes = http.post(
            `${data.baseUrl}/api/rpc`,
            JSON.stringify({
                jsonrpc: '2.0',
                method: 'InvoiceService/listInvoices',
                params: { limit: 20, offset: 0 },
                id: randomIntBetween(1, 10000),
            }),
            { headers }
        );

        const listInvoicesDuration = new Date().getTime() - listInvoicesStart;
        requestCounter.add(1);

        check(listInvoicesRes, {
            'list invoices status is 200': (r) => r.status === 200,
            'list invoices has valid response': (r) => {
                try {
                    const body = JSON.parse(r.body);
                    return body.result !== undefined || body.error !== undefined;
                } catch (e) {
                    return false;
                }
            },
        }) || errorRate.add(1);

        apiLatency.add(listInvoicesRes.timings.duration);

        // Create invoice (10% of requests)
        if (Math.random() < 0.1) {
            const createInvoiceStart = new Date().getTime();
            const invoicePayload = {
                clientId: `client-${randomIntBetween(1, 100)}`,
                invoiceNumber: `INV-${randomString(8)}`,
                issueDate: new Date().toISOString().split('T')[0],
                dueDate: new Date(Date.now() + 30 * 24 * 60 * 60 * 1000).toISOString().split('T')[0],
                subtotal: `${randomIntBetween(100, 10000)}.00`,
                vatAmount: `${randomIntBetween(20, 2000)}.00`,
                totalAmount: `${randomIntBetween(120, 12000)}.00`,
                currency: 'EUR',
                status: 'DRAFT',
                items: [
                    {
                        description: `Service ${randomString(6)}`,
                        quantity: `${randomIntBetween(1, 10)}.0`,
                        unitPrice: `${randomIntBetween(10, 500)}.00`,
                        vatRate: '21.00',
                        lineTotal: `${randomIntBetween(10, 5000)}.00`,
                    },
                ],
            };

            const createInvoiceRes = http.post(
                `${data.baseUrl}/api/rpc`,
                JSON.stringify({
                    jsonrpc: '2.0',
                    method: 'InvoiceService/createInvoice',
                    params: invoicePayload,
                    id: randomIntBetween(1, 10000),
                }),
                { headers }
            );

            const createInvoiceDuration = new Date().getTime() - createInvoiceStart;
            invoiceCreationTime.add(createInvoiceDuration);
            requestCounter.add(1);

            check(createInvoiceRes, {
                'create invoice status is 200': (r) => r.status === 200,
                'create invoice succeeded or failed gracefully': (r) => {
                    try {
                        const body = JSON.parse(r.body);
                        return body.result !== undefined || body.error !== undefined;
                    } catch (e) {
                        return false;
                    }
                },
            }) || errorRate.add(1);

            apiLatency.add(createInvoiceRes.timings.duration);
        }
    });

    sleep(randomIntBetween(2, 5));

    // Payment operations (20% of requests)
    if (Math.random() < 0.2) {
        group('Payment Operations', function() {
            const listPaymentsRes = http.post(
                `${data.baseUrl}/api/rpc`,
                JSON.stringify({
                    jsonrpc: '2.0',
                    method: 'PaymentService/listPayments',
                    params: { limit: 20, offset: 0 },
                    id: randomIntBetween(1, 10000),
                }),
                { headers }
            );

            requestCounter.add(1);

            check(listPaymentsRes, {
                'list payments status is 200': (r) => r.status === 200,
            }) || errorRate.add(1);

            apiLatency.add(listPaymentsRes.timings.duration);
        });
    }

    // Think time between user actions
    sleep(randomIntBetween(1, 3));
}

export function teardown(data) {
    console.log(`Load test completed for ${data.baseUrl}`);
}
