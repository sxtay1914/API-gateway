import { textSummary } from 'https://jslib.k6.io/k6-summary/0.0.1/index.js';

export function handleSummary(data) {
  return {
    'performance-report.html': htmlReport(data),
    'performance-metrics.json': JSON.stringify(data),
    stdout: textSummary(data, { indent: ' ', enableColors: true }),
  };
}
function htmlReport(data) {
  return `
    <!DOCTYPE html>
    <html>
    <head>
      <title>Performance Test Report</title>
      <style>
        body { font-family: Arial, sans-serif; margin: 40px; }
        .metric { margin: 10px 0; padding: 10px; background: #f5f5f5; border-radius: 5px; }
        .pass { border-left: 5px solid #4CAF50; }
        .fail { border-left: 5px solid #f44336; }
      </style>
    </head>
    <body>
      <h1>Performance Test Results</h1>
      <div class="metric ${data.metrics.checks.values.rate === 1 ? 'pass' : 'fail'}">
        <strong>Checks:</strong> ${(data.metrics.checks.values.rate * 100).toFixed(2)}% passed
      </div>
      <div class="metric">
        <strong>Average Response Time:</strong> ${data.metrics.http_req_duration.values.avg.toFixed(2)}ms
      </div>
      <div class="metric">
        <strong>95th Percentile:</strong> ${data.metrics.http_req_duration.values['p(95)'].toFixed(2)}ms
      </div>
      <div class="metric">
        <strong>Error Rate:</strong> ${(data.metrics.http_req_failed.values.rate * 100).toFixed(2)}%
      </div>
    </body>
    </html>
  `;
}
