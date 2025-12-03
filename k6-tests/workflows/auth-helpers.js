import http from 'k6/http';
import { check } from 'k6';

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';

/**
 * Register a new user
 * @param {string} username 
 * @param {string} password 
 * @param {string} role - ADMIN, FACULTY_ADMIN, or STUDENT
 * @param {string} facultyId 
 * @param {object} additionalFields - email, firstName, lastName
 * @returns {object} Response with status and body
 */
export function registerUser(username, password, role, facultyId, additionalFields = {}) {
  const payload = {
    username,
    password,
    role,
    facultyId,
    email: additionalFields.email || `${username}@university.com`,
    firstName: additionalFields.firstName || username,
    lastName: additionalFields.lastName || 'User',
    ...additionalFields
  };

  const registerRes = http.post(
    `${BASE_URL}/api/auth/register`,
    JSON.stringify(payload),
    { headers: { 'Content-Type': 'application/json' } }
  );

  const success = check(registerRes, {
    [`${role} user registered`]: (r) => r.status === 201 || r.status === 200,
  });

  if (!success) {
    console.error(`Registration failed for ${username}: ${registerRes.status} - ${registerRes.body}`);
  }

  return registerRes;
}

/**
 * Login a user and return the token
 * @param {string} username 
 * @param {string} password 
 * @returns {string|null} JWT token or null if login failed
 */
export function loginUser(username, password) {
  const loginRes = http.post(
    `${BASE_URL}/api/auth/login`,
    JSON.stringify({ username, password }),
    { headers: { 'Content-Type': 'application/json' } }
  );

  const success = check(loginRes, {
    [`${username} logged in`]: (r) => r.status === 200,
  });

  if (!success) {
    console.error(`Login failed for ${username}: ${loginRes.status} - ${loginRes.body}`);
    return null;
  }

  return loginRes.json('token');
}

/**
 * Register and login a user in one call
 * @param {string} username 
 * @param {string} password 
 * @param {string} role 
 * @param {string} facultyId 
 * @param {object} additionalFields 
 * @returns {object} { token, username, role }
 */
export function registerAndLogin(username, password, role, facultyId, additionalFields = {}) {
  registerUser(username, password, role, facultyId, additionalFields);
  const token = loginUser(username, password);
  
  return { token, username, role };
}

/**
 * Create authorization headers
 * @param {string} token 
 * @returns {object} Headers object with Content-Type and Authorization
 */
export function createAuthHeaders(token) {
  return {
    'Content-Type': 'application/json',
    'Authorization': `Bearer ${token}`,
  };
}
