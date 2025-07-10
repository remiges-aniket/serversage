import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
  stages: [
    { duration: '2m', target: 100 }, // Below normal load
    { duration: '5m', target: 100 },
    { duration: '2m', target: 200 }, // Normal load
    { duration: '5m', target: 200 },
    { duration: '2m', target: 300 }, // Around the breaking point
    { duration: '5m', target: 300 },
    { duration: '2m', target: 400 }, // Beyond the breaking point
    { duration: '5m', target: 400 },
    { duration: '10m', target: 0 }, // Scale down. Recovery stage.
  ],
};

const BASE_URL = 'http://app:8081';

export default function () {
  const responses = http.batch([
    ['GET', `${BASE_URL}/api/users`],
    ['GET', `${BASE_URL}/api/products`],
    ['GET', `${BASE_URL}/api/orders`],
    ['GET', `${BASE_URL}/api/analytics/dashboard`],
  ]);

  check(responses[0], { 'users endpoint status was 200': (r) => r.status == 200 });
  check(responses[1], { 'products endpoint status was 200': (r) => r.status == 200 });
  check(responses[2], { 'orders endpoint status was 200': (r) => r.status == 200 });
  check(responses[3], { 'analytics endpoint status was 200': (r) => r.status == 200 });

  sleep(1);
}
