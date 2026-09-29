// 주문 접수 부하 테스트 (S2)
// - setup: 테스트 사용자 생성 → 로그인 → 토큰 준비
// - 본 테스트: 체결되지 않는 지정가 매수(1주, 낮은 가격)를 목표 초당 요청 수로 전송 (open model)
// - bff 응답은 "접수" 까지만 의미함. 실제 처리 지연은 Grafana 의 Stream 대기/처리 시간으로 확인
//
// 실행 (저장소 루트):
//   docker compose -f docker-compose.yml -f docker-compose.monitoring.yml run --rm k6 run /scripts/order-intake.js
// 환경변수로 조절: USERS, RATES(단계별 초당 요청 수), STAGE(단계 길이), STOCK_CODES, PRICE

import http from 'k6/http';
import { check, sleep, fail } from 'k6';
import exec from 'k6/execution';

const BASE_URL = __ENV.BASE_URL || 'http://bff-server:8080';
const API = `${BASE_URL}/api/v1`;
const USERS = Number(__ENV.USERS || 50);
const RATES = (__ENV.RATES || '20,50,100').split(',').map(Number);
const STAGE = __ENV.STAGE || '1m';
// A 그룹, B 그룹 종목을 섞어서 두 stock-server 모두에 부하
const STOCK_CODES = (__ENV.STOCK_CODES || '005930,000660,373220,000810,032830,086790').split(',');
const PRICE = Number(__ENV.PRICE || 1000); // 현재가보다 충분히 낮게 → 체결되지 않고 대기열에 쌓임
const RUN_ID = __ENV.RUN_ID || String(Date.now()).slice(-6);
const PASSWORD = 'loadtest1!';
const JSON_HEADERS = { 'Content-Type': 'application/json' };

export const options = {
  setupTimeout: '5m',
  scenarios: {
    order_intake: {
      executor: 'ramping-arrival-rate',
      startRate: RATES[0],
      timeUnit: '1s',
      preAllocatedVUs: 50,
      maxVUs: 500,
      stages: [
        ...RATES.map((rate) => ({ target: rate, duration: STAGE })),
        { target: 0, duration: '10s' },
      ],
    },
  },
  thresholds: {
    'http_req_failed{name:order}': ['rate<0.01'],
    'http_req_duration{name:order}': ['p(95)<500'],
  },
};

export function setup() {
  const tokens = [];

  for (let i = 0; i < USERS; i++) {
    const username = `lt${RUN_ID}${i}`;
    const res = http.post(`${API}/users`, JSON.stringify({
      username,
      nickname: `n${RUN_ID}${i}`.slice(0, 10),
      password: PASSWORD,
    }), { headers: JSON_HEADERS, tags: { name: 'signup' } });

    if (res.status >= 300) {
      fail(`회원가입 실패 username=${username} status=${res.status} body=${res.body}`);
    }
  }

  // 계좌 생성은 Redis Stream 으로 비동기 처리되므로 잠시 대기
  sleep(5);

  for (let i = 0; i < USERS; i++) {
    const res = http.post(`${API}/users/authenticate`, JSON.stringify({
      username: `lt${RUN_ID}${i}`,
      password: PASSWORD,
    }), { headers: JSON_HEADERS, tags: { name: 'login' } });

    const token = res.json('accessToken');
    if (!token) {
      fail(`로그인 실패 status=${res.status} body=${res.body}`);
    }
    tokens.push(token);
  }

  return { tokens };
}

export default function (data) {
  const n = exec.scenario.iterationInTest;
  const token = data.tokens[n % data.tokens.length];
  const stockCode = STOCK_CODES[n % STOCK_CODES.length];

  const res = http.post(`${API}/orders`, JSON.stringify({
    stockCode,
    orderType: 'BUY',
    orderPrice: PRICE,
    orderQuantity: 1,
  }), {
    headers: { ...JSON_HEADERS, Authorization: `Bearer ${token}` },
    tags: { name: 'order' },
  });

  check(res, { '주문 접수 200': (r) => r.status === 200 });
}
