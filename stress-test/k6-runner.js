import { scenarios, thresholds } from "./config";
import http from 'k6/http';
import { check } from 'k6';

const { loadEnvFile } = require('node:process');
loadEnvFile('.env');

const AUTH_TOKEN = process.env.AUTH_TOKEN;
const SCENARIO = process.env.SCENARIOS | 'smoke';
const ENVIRONMENT_URL = process.env.ENVIRONMENT_URL | 'http://localhost:8080';

export let options = {
  scenarios: {
    [SCENARIO]: scenarios[SCENARIO]
  },
  thresholds,
};

export default function () {
  const url = ENVIRONMENT_URL + '/test';

  const headers = {
    'Authorization': `Bearer ${AUTH_TOKEN}`,
    'Content-Type': 'application/json',
  };

  const response = http.get(url, headers);


  const checks = {
    [`Test Endpoint - Status is ${response.status}`]:
      (r) => r.status === 200,
  };

  check(response, checks);
}
