import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate, Trend } from 'k6/metrics';

// Custom metrics to generate exemplars
const errorRate = new Rate('custom_error_rate');
const responseTime = new Trend('custom_response_time', true); // Enable exemplars

export const options = {
  stages: [
    { duration: '1m', target: 5 },
    { duration: '3m', target: 10 },
    { duration: '1m', target: 0 },
  ],
  thresholds: {
    http_req_duration: ['p(95)<2000'],
    custom_error_rate: ['rate<0.1'],
  },
};

const BASE_URL = 'http://app:8081';

export default function () {
  // Generate various request patterns to create exemplars
  const scenarios = [
    testSuccessfulRequests,
    testErrorRequests,
    testSlowRequests,
    testMixedRequests,
  ];

  const scenario = scenarios[Math.floor(Math.random() * scenarios.length)];
  scenario();

  sleep(Math.random() * 2 + 0.5);
}

function testSuccessfulRequests() {
  const startTime = Date.now();
  
  const response = http.get(`${BASE_URL}/api/users`, {
    tags: { scenario: 'success', endpoint: 'users' },
  });

  const duration = Date.now() - startTime;
  responseTime.add(duration, { trace_id: generateTraceId(), scenario: 'success' });

  const success = check(response, {
    'status is 200': (r) => r.status === 200,
  });

  errorRate.add(!success, { scenario: 'success' });
}

function testErrorRequests() {
  const startTime = Date.now();
  
  // Intentionally trigger errors for exemplar generation
  const response = http.get(`${BASE_URL}/api/products/999`, {
    tags: { scenario: 'error', endpoint: 'products' },
  });

  const duration = Date.now() - startTime;
  responseTime.add(duration, { trace_id: generateTraceId(), scenario: 'error' });

  const success = check(response, {
    'status is 500': (r) => r.status === 500,
  });

  errorRate.add(!success, { scenario: 'error' });
}

function testSlowRequests() {
  const startTime = Date.now();
  
  // Test timeout scenario
  const response = http.get(`${BASE_URL}/api/products/category/timeout`, {
    tags: { scenario: 'slow', endpoint: 'products' },
    timeout: '10s',
  });

  const duration = Date.now() - startTime;
  responseTime.add(duration, { trace_id: generateTraceId(), scenario: 'slow' });

  const success = check(response, {
    'request completed': (r) => r.status !== 0,
  });

  errorRate.add(!success, { scenario: 'slow' });
}

function testMixedRequests() {
  const endpoints = [
    '/api/users',
    '/api/products',
    '/api/orders',
    '/api/analytics/dashboard',
    '/api/users/search?keyword=test',
    '/api/products/category/electronics',
  ];

  const endpoint = endpoints[Math.floor(Math.random() * endpoints.length)];
  const startTime = Date.now();
  
  const response = http.get(`${BASE_URL}${endpoint}`, {
    tags: { scenario: 'mixed', endpoint: endpoint.split('?')[0] },
  });

  const duration = Date.now() - startTime;
  responseTime.add(duration, { trace_id: generateTraceId(), scenario: 'mixed' });

  const success = check(response, {
    'status is 2xx or 3xx': (r) => r.status >= 200 && r.status < 400,
  });

  errorRate.add(!success, { scenario: 'mixed' });
}

function generateTraceId() {
  // Generate a mock trace ID for exemplar correlation
  return Math.random().toString(36).substring(2, 15) + Math.random().toString(36).substring(2, 15);
}
