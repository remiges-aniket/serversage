import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate, Trend } from 'k6/metrics';

// Custom metrics
const errorRate = new Rate('errors');
const responseTime = new Trend('response_time');

// Test configuration - Simple but effective
export const options = {
  stages: [
    { duration: '30s', target: 5 },  // Ramp up to 5 users
    { duration: '2m', target: 5 },   // Stay at 5 users
    { duration: '30s', target: 10 }, // Ramp up to 10 users
    { duration: '2m', target: 10 },  // Stay at 10 users
    { duration: '30s', target: 0 },  // Ramp down
  ],
  thresholds: {
    http_req_duration: ['p(95)<3000'], // 95% of requests under 3s
    http_req_failed: ['rate<0.15'],    // Error rate under 15%
  },
};

const BASE_URL = 'http://localhost:8081';

// Test data
const users = [
  { name: 'Alice Johnson', email: 'alice@test.com', role: 'USER' },
  { name: 'Bob Admin', email: 'bob@test.com', role: 'ADMIN' },
  { name: 'Charlie Manager', email: 'charlie@test.com', role: 'MANAGER' },
];

const products = [
  { name: 'Gaming Laptop', description: 'High-performance laptop', price: 1299.99, stockQuantity: 50, category: 'electronics' },
  { name: 'Office Chair', description: 'Ergonomic chair', price: 299.99, stockQuantity: 25, category: 'furniture' },
  { name: 'Programming Book', description: 'Learn to code', price: 49.99, stockQuantity: 100, category: 'books' },
];

function makeRequest(method, url, payload = null) {
  const options = {
    headers: { 'Content-Type': 'application/json' },
    timeout: '10s',
  };

  let response;
  try {
    if (payload) {
      response = http[method.toLowerCase()](url, JSON.stringify(payload), options);
    } else {
      response = http[method.toLowerCase()](url, options);
    }
  } catch (error) {
    console.error(`Request failed: ${error}`);
    errorRate.add(1);
    return null;
  }

  const success = check(response, {
    'status is success': (r) => r && r.status >= 200 && r.status < 400,
    'response time OK': (r) => r && r.timings.duration < 5000,
  });

  errorRate.add(!success);
  responseTime.add(response.timings.duration);

  return response;
}

export default function () {
  const scenario = Math.floor(Math.random() * 8);

  switch (scenario) {
    case 0: testUserEndpoints(); break;
    case 1: testProductEndpoints(); break;
    case 2: testOrderEndpoints(); break;
    case 3: testAnalyticsEndpoints(); break;
    case 4: testErrorScenarios(); break;
    case 5: testSearchEndpoints(); break;
    case 6: testHealthEndpoints(); break;
    default: testMixedWorkload(); break;
  }

  sleep(Math.random() * 2 + 0.5); // Random sleep 0.5-2.5s
}

function testUserEndpoints() {
  console.log('Testing User Endpoints...');
  
  // Get all users
  makeRequest('GET', `${BASE_URL}/api/users`);
  
  // Get user by ID
  makeRequest('GET', `${BASE_URL}/api/users/${Math.floor(Math.random() * 10) + 1}`);
  
  // Create user
  const user = users[Math.floor(Math.random() * users.length)];
  const newUser = { ...user, email: `${Date.now()}_${user.email}` };
  const createResponse = makeRequest('POST', `${BASE_URL}/api/users`, newUser);
  
  if (createResponse && createResponse.status === 201) {
    const created = JSON.parse(createResponse.body);
    // Get user profile
    makeRequest('GET', `${BASE_URL}/api/users/${created.id}/profile`);
    // Update user
    makeRequest('PUT', `${BASE_URL}/api/users/${created.id}`, { ...created, role: 'MANAGER' });
  }
  
  // Search users
  makeRequest('GET', `${BASE_URL}/api/users/search?keyword=test`);
  
  // Get users by role
  makeRequest('GET', `${BASE_URL}/api/users/role/USER`);
  
  // Get user stats
  makeRequest('GET', `${BASE_URL}/api/users/stats`);
}

function testProductEndpoints() {
  console.log('Testing Product Endpoints...');
  
  // Get all products
  makeRequest('GET', `${BASE_URL}/api/products`);
  
  // Create product
  const product = products[Math.floor(Math.random() * products.length)];
  const newProduct = { ...product, name: `${Date.now()}_${product.name}` };
  const createResponse = makeRequest('POST', `${BASE_URL}/api/products`, newProduct);
  
  if (createResponse && createResponse.status === 201) {
    const created = JSON.parse(createResponse.body);
    // Update product
    makeRequest('PUT', `${BASE_URL}/api/products/${created.id}`, { ...created, price: created.price * 1.1 });
    // Update stock
    makeRequest('PATCH', `${BASE_URL}/api/products/${created.id}/stock?quantity=5`);
  }
  
  // Search products
  makeRequest('GET', `${BASE_URL}/api/products/search?keyword=laptop`);
  
  // Get by category
  makeRequest('GET', `${BASE_URL}/api/products/category/electronics`);
  
  // Get low stock
  makeRequest('GET', `${BASE_URL}/api/products/low-stock?threshold=10`);
}

function testOrderEndpoints() {
  console.log('Testing Order Endpoints...');
  
  // Get all orders
  makeRequest('GET', `${BASE_URL}/api/orders`);
  
  // Create order
  const order = {
    userId: Math.floor(Math.random() * 5) + 1,
    productId: Math.floor(Math.random() * 3) + 1,
    quantity: Math.floor(Math.random() * 3) + 1
  };
  const createResponse = makeRequest('POST', `${BASE_URL}/api/orders`, order);
  
  if (createResponse && createResponse.status === 201) {
    const created = JSON.parse(createResponse.body);
    // Update order status
    makeRequest('PATCH', `${BASE_URL}/api/orders/${created.id}/status?status=CONFIRMED`);
  }
  
  // Get orders by user
  makeRequest('GET', `${BASE_URL}/api/orders/user/${Math.floor(Math.random() * 5) + 1}`);
  
  // Get orders by status
  makeRequest('GET', `${BASE_URL}/api/orders/status/PENDING`);
}

function testAnalyticsEndpoints() {
  console.log('Testing Analytics Endpoints...');
  
  // Dashboard
  makeRequest('GET', `${BASE_URL}/api/analytics/dashboard`);
  
  // Statistics
  makeRequest('GET', `${BASE_URL}/api/analytics/users/statistics`);
  makeRequest('GET', `${BASE_URL}/api/analytics/products/statistics`);
  makeRequest('GET', `${BASE_URL}/api/analytics/orders/statistics`);
  
  // Performance metrics
  makeRequest('GET', `${BASE_URL}/api/analytics/performance/metrics`);
}

function testErrorScenarios() {
  console.log('Testing Error Scenarios...');
  
  // Trigger specific errors for observability testing
  makeRequest('GET', `${BASE_URL}/api/users/999`); // Array index error
  makeRequest('GET', `${BASE_URL}/api/users/998`); // Null pointer error
  makeRequest('GET', `${BASE_URL}/api/users/997`); // Rate limit error
  
  // Database error
  makeRequest('POST', `${BASE_URL}/api/users`, {
    name: 'DB Error User',
    email: 'dberror@test.com',
    role: 'USER'
  });
  
  // Validation error
  makeRequest('POST', `${BASE_URL}/api/users`, {
    name: '',
    email: 'invalid-email',
    role: 'INVALID'
  });
}

function testSearchEndpoints() {
  console.log('Testing Search Endpoints...');
  
  const keywords = ['test', 'admin', 'laptop', 'chair'];
  const keyword = keywords[Math.floor(Math.random() * keywords.length)];
  
  makeRequest('GET', `${BASE_URL}/api/users/search?keyword=${keyword}`);
  makeRequest('GET', `${BASE_URL}/api/products/search?keyword=${keyword}`);
}

function testHealthEndpoints() {
  console.log('Testing Health Endpoints...');
  
  makeRequest('GET', `${BASE_URL}/actuator/health`);
  makeRequest('GET', `${BASE_URL}/actuator/info`);
  makeRequest('GET', `${BASE_URL}/api/users/health-check`);
}

function testMixedWorkload() {
  console.log('Testing Mixed Workload...');
  
  // Simulate realistic user behavior
  makeRequest('GET', `${BASE_URL}/api/users`);
  sleep(0.5);
  makeRequest('GET', `${BASE_URL}/api/products`);
  sleep(0.5);
  makeRequest('GET', `${BASE_URL}/api/analytics/dashboard`);
}

export function handleSummary(data) {
  return {
    'stdout': textSummary(data, { indent: ' ', enableColors: true }),
  };
}

function textSummary(data, options = {}) {
  const indent = options.indent || '';
  const colors = options.enableColors || false;
  
  let summary = `
${indent}📊 K6 Load Test Results Summary
${indent}================================
${indent}
${indent}🎯 Test Configuration:
${indent}   Duration: ${data.state.testRunDurationMs / 1000}s
${indent}   Max VUs: ${Math.max(...Object.values(data.metrics.vus?.values || {}))}
${indent}
${indent}📈 Request Statistics:
${indent}   Total Requests: ${data.metrics.http_reqs?.count || 0}
${indent}   Request Rate: ${(data.metrics.http_reqs?.rate || 0).toFixed(2)}/s
${indent}   Failed Requests: ${data.metrics.http_req_failed?.rate ? (data.metrics.http_req_failed.rate * 100).toFixed(2) : 0}%
${indent}
${indent}⏱️  Response Times:
${indent}   Average: ${data.metrics.http_req_duration?.avg ? data.metrics.http_req_duration.avg.toFixed(2) : 0}ms
${indent}   95th Percentile: ${data.metrics.http_req_duration?.['p(95)'] ? data.metrics.http_req_duration['p(95)'].toFixed(2) : 0}ms
${indent}   Max: ${data.metrics.http_req_duration?.max ? data.metrics.http_req_duration.max.toFixed(2) : 0}ms
${indent}
${indent}✅ Thresholds:
`;

  if (data.thresholds) {
    Object.entries(data.thresholds).forEach(([name, threshold]) => {
      const status = threshold.ok ? '✅ PASS' : '❌ FAIL';
      summary += `${indent}   ${name}: ${status}\n`;
    });
  }

  summary += `${indent}
${indent}🚀 Test completed successfully!
${indent}Check Grafana dashboards for detailed observability data.
`;

  return summary;
}
