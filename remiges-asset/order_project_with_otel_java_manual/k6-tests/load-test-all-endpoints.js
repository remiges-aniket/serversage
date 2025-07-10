import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate, Trend, Counter } from 'k6/metrics';

// Custom metrics
const errorRate = new Rate('errors');
const responseTime = new Trend('response_time');
const requestCount = new Counter('requests_total');

// Test configuration
export const options = {
  stages: [
    { duration: '2m', target: 10 }, // Ramp up to 10 users
    { duration: '5m', target: 10 }, // Stay at 10 users
    { duration: '2m', target: 20 }, // Ramp up to 20 users
    { duration: '5m', target: 20 }, // Stay at 20 users
    { duration: '2m', target: 0 },  // Ramp down to 0 users
  ],
  thresholds: {
    http_req_duration: ['p(95)<2000'], // 95% of requests must complete below 2s
    http_req_failed: ['rate<0.1'],     // Error rate must be below 10%
  },
};

const BASE_URL = 'http://app:8081';

// Test data
const testUsers = [
  { name: 'Alice Johnson', email: 'alice.johnson@example.com', role: 'USER' },
  { name: 'Bob Smith', email: 'bob.smith@example.com', role: 'ADMIN' },
  { name: 'Charlie Brown', email: 'charlie.brown@example.com', role: 'MANAGER' },
  { name: 'Diana Prince', email: 'diana.prince@example.com', role: 'EMPLOYEE' },
  { name: 'Eve Wilson', email: 'eve.wilson@example.com', role: 'USER' },
];

const testProducts = [
  { name: 'Gaming Laptop', description: 'High-performance gaming laptop', price: 1299.99, stockQuantity: 50, category: 'electronics' },
  { name: 'Wireless Headphones', description: 'Noise-cancelling wireless headphones', price: 199.99, stockQuantity: 100, category: 'electronics' },
  { name: 'Office Chair', description: 'Ergonomic office chair', price: 299.99, stockQuantity: 25, category: 'furniture' },
  { name: 'Programming Book', description: 'Advanced programming concepts', price: 49.99, stockQuantity: 200, category: 'books' },
  { name: 'Coffee Maker', description: 'Automatic coffee maker', price: 89.99, stockQuantity: 75, category: 'appliances' },
];

// Helper function to make requests with error handling
function makeRequest(method, url, payload = null, params = {}) {
  const options = {
    headers: {
      'Content-Type': 'application/json',
      'User-Agent': 'K6-LoadTest/1.0',
    },
    tags: {
      endpoint: url.replace(BASE_URL, ''),
      method: method,
    },
  };

  let response;
  const startTime = Date.now();

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

  const duration = Date.now() - startTime;
  responseTime.add(duration);
  requestCount.add(1);

  const success = check(response, {
    'status is 2xx or 3xx': (r) => r && r.status >= 200 && r.status < 400,
    'response time < 5000ms': () => duration < 5000,
  });

  if (!success) {
    errorRate.add(1);
    console.log(`Request failed: ${method} ${url} - Status: ${response?.status}, Duration: ${duration}ms`);
  } else {
    errorRate.add(0);
  }

  return response;
}

// Test scenarios
export default function () {
  const scenario = Math.floor(Math.random() * 10);

  switch (scenario) {
    case 0:
      testUserManagement();
      break;
    case 1:
      testProductManagement();
      break;
    case 2:
      testOrderManagement();
      break;
    case 3:
      testAnalytics();
      break;
    case 4:
      testErrorScenarios();
      break;
    case 5:
      testSearchAndFiltering();
      break;
    case 6:
      testBatchOperations();
      break;
    case 7:
      testAsyncOperations();
      break;
    case 8:
      testHealthAndMetrics();
      break;
    default:
      testMixedWorkload();
      break;
  }

  sleep(Math.random() * 2 + 1); // Random sleep between 1-3 seconds
}

function testUserManagement() {
  // Get all users
  makeRequest('GET', `${BASE_URL}/api/users`);

  // Get user by ID
  const userId = Math.floor(Math.random() * 100) + 1;
  makeRequest('GET', `${BASE_URL}/api/users/${userId}`);

  // Create user
  const user = testUsers[Math.floor(Math.random() * testUsers.length)];
  const timestamp = Date.now();
  const newUser = {
    ...user,
    email: `${user.email.split('@')[0]}_${timestamp}@example.com`
  };
  const createResponse = makeRequest('POST', `${BASE_URL}/api/users`, newUser);

  if (createResponse && createResponse.status === 201) {
    const createdUser = JSON.parse(createResponse.body);
    
    // Update user
    const updatedUser = { ...createdUser, role: 'MANAGER' };
    makeRequest('PUT', `${BASE_URL}/api/users/${createdUser.id}`, updatedUser);

    // Get user profile
    makeRequest('GET', `${BASE_URL}/api/users/${createdUser.id}/profile`);
  }

  // Search users
  makeRequest('GET', `${BASE_URL}/api/users/search?keyword=john`);

  // Get users by role
  const roles = ['ADMIN', 'USER', 'MANAGER', 'EMPLOYEE'];
  const role = roles[Math.floor(Math.random() * roles.length)];
  makeRequest('GET', `${BASE_URL}/api/users/role/${role}`);
}

function testProductManagement() {
  // Get all products
  makeRequest('GET', `${BASE_URL}/api/products`);

  // Create product
  const product = testProducts[Math.floor(Math.random() * testProducts.length)];
  const timestamp = Date.now();
  const newProduct = {
    ...product,
    name: `${product.name} ${timestamp}`
  };
  const createResponse = makeRequest('POST', `${BASE_URL}/api/products`, newProduct);

  if (createResponse && createResponse.status === 201) {
    const createdProduct = JSON.parse(createResponse.body);
    
    // Update product
    const updatedProduct = { ...createdProduct, price: createdProduct.price * 1.1 };
    makeRequest('PUT', `${BASE_URL}/api/products/${createdProduct.id}`, updatedProduct);

    // Update stock
    makeRequest('PATCH', `${BASE_URL}/api/products/${createdProduct.id}/stock?quantity=10`);

    // Get recommendations
    makeRequest('GET', `${BASE_URL}/api/products/${createdProduct.id}/recommendations`);
  }

  // Search products
  makeRequest('GET', `${BASE_URL}/api/products/search?keyword=laptop`);

  // Get products by category
  const categories = ['electronics', 'furniture', 'books', 'appliances'];
  const category = categories[Math.floor(Math.random() * categories.length)];
  makeRequest('GET', `${BASE_URL}/api/products/category/${category}`);

  // Get products by price range
  makeRequest('GET', `${BASE_URL}/api/products/price-range?minPrice=50&maxPrice=500`);

  // Get low stock products
  makeRequest('GET', `${BASE_URL}/api/products/low-stock?threshold=20`);
}

function testOrderManagement() {
  // Get all orders
  makeRequest('GET', `${BASE_URL}/api/orders`);

  // Create order
  const order = {
    userId: Math.floor(Math.random() * 10) + 1,
    productId: Math.floor(Math.random() * 5) + 1,
    quantity: Math.floor(Math.random() * 3) + 1
  };
  const createResponse = makeRequest('POST', `${BASE_URL}/api/orders`, order);

  if (createResponse && createResponse.status === 201) {
    const createdOrder = JSON.parse(createResponse.body);
    
    // Update order status
    const statuses = ['CONFIRMED', 'SHIPPED', 'DELIVERED'];
    const status = statuses[Math.floor(Math.random() * statuses.length)];
    makeRequest('PATCH', `${BASE_URL}/api/orders/${createdOrder.id}/status?status=${status}`);

    // Process payment
    makeRequest('POST', `${BASE_URL}/api/orders/${createdOrder.id}/payment?amount=${createdOrder.totalAmount}`);
  }

  // Get orders by user
  const userId = Math.floor(Math.random() * 10) + 1;
  makeRequest('GET', `${BASE_URL}/api/orders/user/${userId}`);

  // Get orders by status
  const statuses = ['PENDING', 'CONFIRMED', 'SHIPPED', 'DELIVERED'];
  const status = statuses[Math.floor(Math.random() * statuses.length)];
  makeRequest('GET', `${BASE_URL}/api/orders/status/${status}`);

  // Get high value orders
  makeRequest('GET', `${BASE_URL}/api/orders/high-value?threshold=1000`);

  // Check inventory
  makeRequest('GET', `${BASE_URL}/api/orders/inventory-check?productId=1&quantity=5`);
}

function testAnalytics() {
  // Dashboard statistics
  makeRequest('GET', `${BASE_URL}/api/analytics/dashboard`);

  // User statistics
  makeRequest('GET', `${BASE_URL}/api/analytics/users/statistics`);

  // Product statistics
  makeRequest('GET', `${BASE_URL}/api/analytics/products/statistics`);

  // Order statistics
  makeRequest('GET', `${BASE_URL}/api/analytics/orders/statistics`);

  // Monthly revenue
  makeRequest('GET', `${BASE_URL}/api/analytics/revenue/monthly?year=2024`);

  // Performance metrics
  makeRequest('GET', `${BASE_URL}/api/analytics/performance/metrics`);

  // Detailed health check
  makeRequest('GET', `${BASE_URL}/api/analytics/health/detailed`);

  // Generate report
  const reportTypes = ['sales', 'users', 'products'];
  const reportType = reportTypes[Math.floor(Math.random() * reportTypes.length)];
  makeRequest('POST', `${BASE_URL}/api/analytics/reports/generate?reportType=${reportType}&dateRange=last-30-days`);

  // Error summary
  makeRequest('GET', `${BASE_URL}/api/analytics/errors/summary`);

  // Cache statistics
  makeRequest('GET', `${BASE_URL}/api/analytics/cache/statistics`);
}

function testErrorScenarios() {
  // Array index out of bounds
  makeRequest('GET', `${BASE_URL}/api/products/999`);

  // Null pointer exception
  makeRequest('GET', `${BASE_URL}/api/products/998`);

  // Rate limiting
  makeRequest('GET', `${BASE_URL}/api/orders/997`);

  // Database error simulation
  const dbErrorUser = {
    name: 'DB Error User',
    email: 'dberror@example.com',
    role: 'USER'
  };
  makeRequest('POST', `${BASE_URL}/api/users`, dbErrorUser);

  // Business logic error (high price)
  const expensiveProduct = {
    name: 'Expensive Item',
    description: 'Very expensive item',
    price: 15000.00,
    stockQuantity: 1,
    category: 'luxury'
  };
  makeRequest('POST', `${BASE_URL}/api/products`, expensiveProduct);

  // Timeout scenario
  makeRequest('GET', `${BASE_URL}/api/products/category/timeout`);

  // Validation errors
  const invalidUser = {
    name: '',
    email: 'invalid-email',
    role: 'INVALID_ROLE'
  };
  makeRequest('POST', `${BASE_URL}/api/users`, invalidUser);
}

function testSearchAndFiltering() {
  // User searches
  const userKeywords = ['john', 'admin', 'manager', 'test'];
  const userKeyword = userKeywords[Math.floor(Math.random() * userKeywords.length)];
  makeRequest('GET', `${BASE_URL}/api/users/search?keyword=${userKeyword}`);

  // Product searches
  const productKeywords = ['laptop', 'chair', 'book', 'coffee'];
  const productKeyword = productKeywords[Math.floor(Math.random() * productKeywords.length)];
  makeRequest('GET', `${BASE_URL}/api/products/search?keyword=${productKeyword}`);

  // Date range filtering
  const startDate = '2024-01-01T00:00:00';
  const endDate = '2024-12-31T23:59:59';
  makeRequest('GET', `${BASE_URL}/api/orders/date-range?startDate=${startDate}&endDate=${endDate}`);
}

function testBatchOperations() {
  // Create multiple users
  const batchUsers = [];
  for (let i = 0; i < 3; i++) {
    const user = testUsers[Math.floor(Math.random() * testUsers.length)];
    const timestamp = Date.now() + i;
    batchUsers.push({
      ...user,
      email: `${user.email.split('@')[0]}_batch_${timestamp}@example.com`
    });
  }
  makeRequest('POST', `${BASE_URL}/api/users/batch`, batchUsers);
}

function testAsyncOperations() {
  // Async product creation
  const product = testProducts[Math.floor(Math.random() * testProducts.length)];
  const timestamp = Date.now();
  const newProduct = {
    ...product,
    name: `${product.name} Async ${timestamp}`
  };
  makeRequest('POST', `${BASE_URL}/api/products/async`, newProduct);
}

function testHealthAndMetrics() {
  // Health check
  makeRequest('GET', `${BASE_URL}/actuator/health`);

  // Application info
  makeRequest('GET', `${BASE_URL}/actuator/info`);

  // Metrics endpoint
  makeRequest('GET', `${BASE_URL}/actuator/metrics`);
}

function testMixedWorkload() {
  // Simulate realistic user behavior
  const actions = [
    () => makeRequest('GET', `${BASE_URL}/api/users`),
    () => makeRequest('GET', `${BASE_URL}/api/products`),
    () => makeRequest('GET', `${BASE_URL}/api/orders`),
    () => makeRequest('GET', `${BASE_URL}/api/analytics/dashboard`),
    () => makeRequest('GET', `${BASE_URL}/api/users/search?keyword=test`),
    () => makeRequest('GET', `${BASE_URL}/api/products/category/electronics`),
    () => makeRequest('GET', `${BASE_URL}/actuator/health`),
  ];

  // Execute 2-4 random actions
  const numActions = Math.floor(Math.random() * 3) + 2;
  for (let i = 0; i < numActions; i++) {
    const action = actions[Math.floor(Math.random() * actions.length)];
    action();
    sleep(0.5); // Small delay between actions
  }
}
