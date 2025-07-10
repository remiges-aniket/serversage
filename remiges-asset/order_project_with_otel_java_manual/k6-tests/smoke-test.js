import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
  vus: 1, // 1 user looping for 1 minute
  duration: '1m',
  thresholds: {
    http_req_duration: ['p(99)<1500'], // 99% of requests must complete below 1.5s
  },
};

const BASE_URL = 'http://app:8081';

export default function () {
  // Health check
  let response = http.get(`${BASE_URL}/actuator/health`);
  check(response, { 'health check status was 200': (r) => r.status == 200 });

  sleep(1);

  // Get users
  response = http.get(`${BASE_URL}/api/users`);
  check(response, { 'get users status was 200': (r) => r.status == 200 });

  sleep(1);

  // Get analytics dashboard
  response = http.get(`${BASE_URL}/api/analytics/dashboard`);
  check(response, { 'analytics dashboard status was 200': (r) => r.status == 200 });

  sleep(1);

  // Test error scenario
  response = http.get(`${BASE_URL}/api/products/999`);
  check(response, { 'error scenario triggered': (r) => r.status == 500 });

  sleep(2);
}
