import http from 'k6/http';
import { sleep } from 'k6';

export const options = {
  stages: [
    { duration: '10s', target: 10 }, // Ramp up to 10 VUs over 10 seconds
    { duration: '30s', target: 10 }, // Stay at 10 VUs for 30 seconds
    { duration: '10s', target: 0 },   // Ramp down to 0 VUs over 10 seconds
  ],
};

export default function () {
  // GET all users
  let response = http.get('http://localhost:8081/api/users');
  check(response, {
    "status code is 200": (r) => r.status === 200,
  });

  // GET user by ID (assuming ID 1 exists)
  response = http.get('http://localhost:8081/api/users/1');
  check(response, {
    "status code is 200": (r) => r.status === 200,
  });

  // POST create a new user
  const payload = JSON.stringify({
    "name": "John Doe",
    "email": "john.doe@example.com",
    "role": "USER",
  });
  const headers = {
    'Content-Type': 'application/json',
  };
  response = http.post('http://localhost:8081/api/users', payload, { headers });
  check(response, {
    "status code is 201": (r) => r.status === 201,
  });

  // PUT update an existing user (assuming ID 1 exists)
  const updatePayload = JSON.stringify({
    "name": "Jane Doe",
    "email": "jane.doe@example.com",
    "role": "USER",
  });
  response = http.put('http://localhost:8081/api/users/1', updatePayload, { headers });
  check(response, {
    "status code is 200": (r) => r.status === 200,
  });

  // DELETE a user (assuming ID 1 exists)
  response = http.del('http://localhost:8081/api/users/1');
  check(response, {
    "status code is 204": (r) => r.status === 204,
  });

  sleep(1); // Pause for 1 second between iterations
}
