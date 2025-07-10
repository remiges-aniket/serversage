import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
  stages: [
    { duration: '1m', target: 5 },
    { duration: '2m', target: 10 },
    { duration: '1m', target: 0 },
  ],
};

const BASE_URL = 'http://host.docker.internal:8081';

export default function () {
  // Test different endpoints to generate exemplars
  const endpoints = [
    '/api/users',
    '/api/products',
    '/api/orders',
    '/api/analytics/dashboard',
    '/api/products/999', // Error scenario
    '/api/users/search?keyword=test',
  ];

  const endpoint = endpoints[Math.floor(Math.random() * endpoints.length)];
  
  const response = http.get(`${BASE_URL}${endpoint}`, {
    tags: { endpoint: endpoint },
  });

  check(response, {
    'status is not 0': (r) => r.status !== 0,
  });

  sleep(1);
}
