import http from 'k6/http';
import { check, sleep } from 'k6';
import { textSummary } from 'https://jslib.k6.io/k6-summary/0.0.1/index.js';

export const options = {
  vus: Number(__ENV.VUS || 5),
  duration: __ENV.DURATION || '30s',
};

const BASE = __ENV.API_BASE || 'http://localhost:8080';

export default function () {
  const res = http.get(`${BASE}/api/tasks`);
  check(res, { 'status is 200': (r) => r.status === 200 });
  sleep(1);
}

export function handleSummary(data) {
  const summaryPath = __ENV.SUMMARY || 'reports/k6-summary.json';
  return {
    stdout: textSummary(data, { indent: ' ', enableColors: true }),
    [summaryPath]: JSON.stringify(data, null, 2),
  };
}
