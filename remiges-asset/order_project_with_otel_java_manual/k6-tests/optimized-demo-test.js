import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate, Trend, Counter } from 'k6/metrics';

// Custom metrics for better observability
const errorRate = new Rate('errors');
const responseTime = new Trend('response_time');
const orderCreations = new Counter('order_creations');
const userCreations = new Counter('user_creations');
const productCreations = new Counter('product_creations');

// Optimized test configuration for demo purposes
export const options = {
  stages: [
    { duration: '1m', target: 2 },   // Gentle ramp up to 2 users
    { duration: '3m', target: 3 },   // Stay at 3 users for main demo
    { duration: '2m', target: 5 },   // Slight increase to 5 users
    { duration: '2m', target: 3 },   // Back to 3 users
    { duration: '1m', target: 0 },   // Ramp down
  ],
  thresholds: {
    http_req_duration: ['p(95)<2000'], // 95% of requests under 2s
    http_req_failed: ['rate<0.10'],    // Error rate under 10%
    errors: ['rate<0.10'],
  },
};

const BASE_URL = 'http://localhost:8081';

// Realistic test data pools
const userPool = [
  { name: 'Alice Johnson', email: 'alice.johnson@demo.com', role: 'USER' },
  { name: 'Bob Smith', email: 'bob.smith@demo.com', role: 'ADMIN' },
  { name: 'Charlie Brown', email: 'charlie.brown@demo.com', role: 'MANAGER' },
  { name: 'Diana Prince', email: 'diana.prince@demo.com', role: 'USER' },
  { name: 'Edward Norton', email: 'edward.norton@demo.com', role: 'USER' },
  { name: 'Fiona Green', email: 'fiona.green@demo.com', role: 'MANAGER' },
];

const productPool = [
  { name: 'MacBook Pro 16"', description: 'High-performance laptop for professionals', price: 2499.99, stockQuantity: 25, category: 'electronics' },
  { name: 'Dell XPS 13', description: 'Ultrabook for business users', price: 1299.99, stockQuantity: 30, category: 'electronics' },
  { name: 'Herman Miller Chair', description: 'Ergonomic office chair', price: 899.99, stockQuantity: 15, category: 'furniture' },
  { name: 'Standing Desk', description: 'Adjustable height desk', price: 599.99, stockQuantity: 20, category: 'furniture' },
  { name: 'Clean Code Book', description: 'Software engineering best practices', price: 49.99, stockQuantity: 100, category: 'books' },
  { name: 'Design Patterns', description: 'Gang of Four design patterns', price: 59.99, stockQuantity: 75, category: 'books' },
];

// Global state to track created entities
let createdUsers = [];
let createdProducts = [];
let createdOrders = [];

function makeRequest(method, url, payload = null, description = '') {
  const options = {
    headers: { 'Content-Type': 'application/json' },
    timeout: '15s',
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
    console.error(`Request failed [${description}]: ${error}`);
    errorRate.add(1);
    return null;
  }

  const duration = Date.now() - startTime;
  responseTime.add(duration);

  const success = check(response, {
    [`${description} - status is success`]: (r) => r && r.status >= 200 && r.status < 400,
    [`${description} - response time OK`]: (r) => r && r.timings.duration < 3000,
  });

  if (!success) {
    errorRate.add(1);
    if (response) {
      console.log(`Request failed [${description}]: Status ${response.status}, Body: ${response.body.substring(0, 200)}`);
    }
  }

  return response;
}

export default function () {
  // Randomize user behavior with weighted scenarios
  const scenario = getWeightedScenario();
  
  switch (scenario) {
    case 'user_management':
      testUserManagement();
      break;
    case 'product_management':
      testProductManagement();
      break;
    case 'order_creation':
      testOrderCreation();
      break;
    case 'analytics_dashboard':
      testAnalyticsDashboard();
      break;
    case 'search_operations':
      testSearchOperations();
      break;
    case 'error_scenarios':
      testErrorScenarios();
      break;
    default:
      testMixedWorkload();
  }

  // Realistic user think time - randomized between 2-8 seconds
  const thinkTime = Math.random() * 6 + 2;
  sleep(thinkTime);
}

function getWeightedScenario() {
  const scenarios = [
    { name: 'user_management', weight: 15 },
    { name: 'product_management', weight: 15 },
    { name: 'order_creation', weight: 25 },      // Higher weight for order creation
    { name: 'analytics_dashboard', weight: 20 },  // Higher weight for dashboard views
    { name: 'search_operations', weight: 15 },
    { name: 'error_scenarios', weight: 5 },       // Lower weight for errors
    { name: 'mixed_workload', weight: 5 },
  ];

  const totalWeight = scenarios.reduce((sum, s) => sum + s.weight, 0);
  const random = Math.random() * totalWeight;
  
  let currentWeight = 0;
  for (const scenario of scenarios) {
    currentWeight += scenario.weight;
    if (random <= currentWeight) {
      return scenario.name;
    }
  }
  
  return 'mixed_workload';
}

function testUserManagement() {
  console.log('🧑 Testing User Management...');
  
  // Get all users first
  makeRequest('GET', `${BASE_URL}/api/users`, null, 'Get all users');
  sleep(randomDelay(0.5, 1.5));
  
  // Create a new user
  const user = userPool[Math.floor(Math.random() * userPool.length)];
  const newUser = { 
    ...user, 
    email: `${Date.now()}_${Math.random().toString(36).substring(7)}_${user.email}` 
  };
  
  const createResponse = makeRequest('POST', `${BASE_URL}/api/users`, newUser, 'Create user');
  
  if (createResponse && createResponse.status === 201) {
    const created = JSON.parse(createResponse.body);
    createdUsers.push(created);
    userCreations.add(1);
    
    sleep(randomDelay(0.3, 0.8));
    
    // Get user profile
    makeRequest('GET', `${BASE_URL}/api/users/${created.id}/profile`, null, 'Get user profile');
    
    sleep(randomDelay(0.5, 1.0));
    
    // Update user role occasionally
    if (Math.random() < 0.3) {
      makeRequest('PUT', `${BASE_URL}/api/users/${created.id}`, 
        { ...created, role: 'MANAGER' }, 'Update user');
    }
  }
  
  // Search users
  const searchKeywords = ['demo', 'test', 'admin', 'user'];
  const keyword = searchKeywords[Math.floor(Math.random() * searchKeywords.length)];
  makeRequest('GET', `${BASE_URL}/api/users/search?keyword=${keyword}`, null, 'Search users');
  
  sleep(randomDelay(0.3, 0.7));
  
  // Get user statistics
  makeRequest('GET', `${BASE_URL}/api/users/stats`, null, 'Get user stats');
}

function testProductManagement() {
  console.log('📦 Testing Product Management...');
  
  // Get all products
  makeRequest('GET', `${BASE_URL}/api/products`, null, 'Get all products');
  sleep(randomDelay(0.5, 1.0));
  
  // Create a new product
  const product = productPool[Math.floor(Math.random() * productPool.length)];
  const newProduct = { 
    ...product, 
    name: `${Date.now()}_${product.name}`,
    stockQuantity: Math.floor(Math.random() * 50) + 10 // Random stock 10-60
  };
  
  const createResponse = makeRequest('POST', `${BASE_URL}/api/products`, newProduct, 'Create product');
  
  if (createResponse && createResponse.status === 201) {
    const created = JSON.parse(createResponse.body);
    createdProducts.push(created);
    productCreations.add(1);
    
    sleep(randomDelay(0.3, 0.8));
    
    // Update product price occasionally
    if (Math.random() < 0.4) {
      const updatedPrice = created.price * (0.9 + Math.random() * 0.2); // ±10% price change
      makeRequest('PUT', `${BASE_URL}/api/products/${created.id}`, 
        { ...created, price: Math.round(updatedPrice * 100) / 100 }, 'Update product');
    }
  }
  
  // Get products by category
  const categories = ['electronics', 'furniture', 'books'];
  const category = categories[Math.floor(Math.random() * categories.length)];
  makeRequest('GET', `${BASE_URL}/api/products/category/${category}`, null, 'Get products by category');
  
  sleep(randomDelay(0.3, 0.7));
  
  // Check low stock products
  makeRequest('GET', `${BASE_URL}/api/products/low-stock?threshold=20`, null, 'Get low stock products');
}

function testOrderCreation() {
  console.log('🛒 Testing Order Creation...');
  
  // Get available users and products first
  const usersResponse = makeRequest('GET', `${BASE_URL}/api/users`, null, 'Get users for order');
  const productsResponse = makeRequest('GET', `${BASE_URL}/api/products`, null, 'Get products for order');
  
  if (!usersResponse || !productsResponse || 
      usersResponse.status !== 200 || productsResponse.status !== 200) {
    console.log('Failed to get users or products for order creation');
    return;
  }
  
  sleep(randomDelay(0.5, 1.0));
  
  try {
    const users = JSON.parse(usersResponse.body);
    const products = JSON.parse(productsResponse.body);
    
    if (users.length === 0 || products.length === 0) {
      console.log('No users or products available for order creation');
      return;
    }
    
    // Create realistic order
    const user = users[Math.floor(Math.random() * users.length)];
    const product = products[Math.floor(Math.random() * products.length)];
    const quantity = Math.floor(Math.random() * 3) + 1; // 1-3 items
    
    const order = {
      userId: user.id,
      productId: product.id,
      quantity: quantity
    };
    
    const createResponse = makeRequest('POST', `${BASE_URL}/api/orders`, order, 'Create order');
    
    if (createResponse && createResponse.status === 201) {
      const created = JSON.parse(createResponse.body);
      createdOrders.push(created);
      orderCreations.add(1);
      
      sleep(randomDelay(0.5, 1.5));
      
      // Update order status occasionally
      if (Math.random() < 0.6) {
        const statuses = ['CONFIRMED', 'SHIPPED'];
        const newStatus = statuses[Math.floor(Math.random() * statuses.length)];
        makeRequest('PATCH', `${BASE_URL}/api/orders/${created.id}/status?status=${newStatus}`, 
          null, 'Update order status');
      }
      
      sleep(randomDelay(0.3, 0.8));
      
      // Process payment occasionally
      if (Math.random() < 0.4) {
        makeRequest('POST', `${BASE_URL}/api/orders/${created.id}/payment?amount=${created.totalAmount}`, 
          null, 'Process payment');
      }
    }
    
  } catch (error) {
    console.error('Error in order creation:', error);
  }
  
  // Get orders by status
  const statuses = ['PENDING', 'CONFIRMED', 'SHIPPED'];
  const status = statuses[Math.floor(Math.random() * statuses.length)];
  makeRequest('GET', `${BASE_URL}/api/orders/status/${status}`, null, 'Get orders by status');
}

function testAnalyticsDashboard() {
  console.log('📊 Testing Analytics Dashboard...');
  
  // Main dashboard - most important for demo
  makeRequest('GET', `${BASE_URL}/api/analytics/dashboard`, null, 'Get dashboard');
  sleep(randomDelay(1.0, 2.0));
  
  // User statistics
  makeRequest('GET', `${BASE_URL}/api/analytics/users/statistics`, null, 'Get user statistics');
  sleep(randomDelay(0.5, 1.0));
  
  // Product statistics
  makeRequest('GET', `${BASE_URL}/api/analytics/products/statistics`, null, 'Get product statistics');
  sleep(randomDelay(0.5, 1.0));
  
  // Order statistics
  makeRequest('GET', `${BASE_URL}/api/analytics/orders/statistics`, null, 'Get order statistics');
  sleep(randomDelay(0.5, 1.0));
  
  // Performance metrics
  makeRequest('GET', `${BASE_URL}/api/analytics/performance/metrics`, null, 'Get performance metrics');
}

function testSearchOperations() {
  console.log('🔍 Testing Search Operations...');
  
  const searchTerms = ['laptop', 'chair', 'book', 'demo', 'test', 'admin'];
  const term = searchTerms[Math.floor(Math.random() * searchTerms.length)];
  
  // Search users
  makeRequest('GET', `${BASE_URL}/api/users/search?keyword=${term}`, null, 'Search users');
  sleep(randomDelay(0.3, 0.8));
  
  // Search products
  makeRequest('GET', `${BASE_URL}/api/products/search?keyword=${term}`, null, 'Search products');
}

function testErrorScenarios() {
  console.log('⚠️ Testing Error Scenarios...');
  
  // Trigger specific errors for observability testing (reduced frequency)
  const errorScenarios = [
    () => makeRequest('GET', `${BASE_URL}/api/users/999`, null, 'Array index error'),
    () => makeRequest('GET', `${BASE_URL}/api/users/998`, null, 'Null pointer error'),
    () => makeRequest('POST', `${BASE_URL}/api/users`, {
      name: 'DB Error User',
      email: 'dberror@test.com',
      role: 'USER'
    }, 'Database error'),
    () => makeRequest('POST', `${BASE_URL}/api/users`, {
      name: '',
      email: 'invalid-email',
      role: 'INVALID'
    }, 'Validation error'),
  ];
  
  // Only trigger one error scenario per execution
  const scenario = errorScenarios[Math.floor(Math.random() * errorScenarios.length)];
  scenario();
}

function testMixedWorkload() {
  console.log('🔄 Testing Mixed Workload...');
  
  // Simulate realistic user browsing behavior
  makeRequest('GET', `${BASE_URL}/api/analytics/dashboard`, null, 'Dashboard view');
  sleep(randomDelay(1.0, 2.0));
  
  makeRequest('GET', `${BASE_URL}/api/products`, null, 'Browse products');
  sleep(randomDelay(0.8, 1.5));
  
  makeRequest('GET', `${BASE_URL}/api/orders`, null, 'Check orders');
  sleep(randomDelay(0.5, 1.0));
  
  // Health check
  makeRequest('GET', `${BASE_URL}/actuator/health`, null, 'Health check');
}

function randomDelay(min, max) {
  return Math.random() * (max - min) + min;
}

export function handleSummary(data) {
  const summary = generateDetailedSummary(data);
  console.log(summary);
  return {
    'stdout': summary,
  };
}

function generateDetailedSummary(data) {
  const duration = Math.round(data.state.testRunDurationMs / 1000);
  const totalRequests = data.metrics.http_reqs?.count || 0;
  const requestRate = data.metrics.http_reqs?.rate || 0;
  const errorRate = data.metrics.http_req_failed?.rate || 0;
  const avgResponseTime = data.metrics.http_req_duration?.avg || 0;
  const p95ResponseTime = data.metrics.http_req_duration?.['p(95)'] || 0;
  
  return `
🎯 ServerSage Demo Load Test Results
=====================================

⏱️  Test Duration: ${duration}s
👥 Max Concurrent Users: ${Math.max(...Object.values(data.metrics.vus?.values || {}))}

📊 Request Statistics:
   Total Requests: ${totalRequests}
   Request Rate: ${requestRate.toFixed(2)}/s
   Error Rate: ${(errorRate * 100).toFixed(2)}%

⚡ Response Times:
   Average: ${avgResponseTime.toFixed(0)}ms
   95th Percentile: ${p95ResponseTime.toFixed(0)}ms

📈 Business Metrics:
   Users Created: ${data.metrics.user_creations?.count || 0}
   Products Created: ${data.metrics.product_creations?.count || 0}
   Orders Created: ${data.metrics.order_creations?.count || 0}

✅ Test Status: ${Object.values(data.thresholds || {}).every(t => t.ok) ? 'PASSED' : 'FAILED'}

🚀 Demo Ready! Check Grafana dashboards for detailed observability data.
   • Business Dashboard: http://localhost:3000/d/business
   • Technical Dashboard: http://localhost:3000/d/technical
   • JVM Monitoring: http://localhost:3000/d/jvm
`;
}
