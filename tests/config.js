export const scenarios = {
  "smoke": {
    executor: 'constant-arrival-rate',
    rate: 1,
    timeUnit: '1s',
    duration: '30s',
    preAllocatedVUs: 2,
  },
  stress: {
    executor: 'ramping-arrival-rate',
    startRate: 1,
    timeUnit: '1s',
    preAllocatedVUs: 200,
    maxVUs: 2000,
    stages: [
      { duration: '30s', target: 100 },  // warm up
      { duration: '1m', target: 500 },  // normal load
      { duration: '1m', target: 1000 },  // stress
      { duration: '1m', target: 2000 },  // breaking point
      { duration: '30s', target: 0 },  // cool down
    ],
  },
}

export const thresholds = {
  http_req_duration: ['p(95)<1000', 'p(99)<1500'],
  http_req_failed: ['rate<0.05'],
  checks: ['rate>0.95']
}
