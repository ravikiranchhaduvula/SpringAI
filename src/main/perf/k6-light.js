import http from "k6/http";
import { check, sleep } from "k6";

// config: 1 virtual user, 60s test
export const options = {
  vus: 1,
  duration: "60s",  // one minute
  thresholds: {
    http_req_failed: ["rate<0.05"],      // <5% errors allowed
    http_req_duration: ["p(95)<5000"],   // p95 < 2s
  },
};

const payloads = [
  { q: "How can I reduce p95 latency?" },
  { q: "Place a hold on invoice INV-2002 for $45.00" },
];

export default function () {
  // pick one of our two sample questions
  const idx = __ITER % payloads.length;
  const res = http.post(
    `${__ENV.BASE_URL || "http://localhost:8080"}/ai/route`,
    JSON.stringify(payloads[idx]),
    { headers: { "Content-Type": "application/json" } }
  );

  check(res, {
    "status is 200": (r) => r.status === 200,
    "json body": (r) => r.headers["Content-Type"]?.includes("application/json"),
  });

  // pause so we only send ~2 requests per minute
  sleep(30);
}
