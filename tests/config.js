export const scenarios = {
  "smoke": {
    executor: 'constant-arrival-rate',
    rate: 1,
    timeUnit: '1s',
    duration: '30s',
    preAllocatedVUs: 2,
  }
}

export const thresholds = {
  http_req_duration: ['p(95)<1000', 'p(99)<1500'],
  http_req_failed: ['rate<0.05'],
  checks: ['rate>0.95']
}
