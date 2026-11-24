// B18: prueba de carga con k6 (RNF-04, RNF-05).
//   100 usuarios concurrentes; objetivo: p95 < 3 s y menos de 1 % de errores.
// Requisitos: sistema levantado y preguntas publicadas (./scripts/seed-preguntas.sh 8).
// Ejecutar:
//   k6 run k6/carga-simulacros.js
//   k6 run -e API=http://host:8080 -e VUS=100 k6/carga-simulacros.js
//   docker run --rm -i --network host -v $PWD/k6:/k6 grafana/k6 run /k6/carga-simulacros.js
import http from 'k6/http';
import { check, group, sleep } from 'k6';
import { Trend, Rate } from 'k6/metrics';

const API = __ENV.API || 'http://localhost:8080';
const VUS = parseInt(__ENV.VUS || '100', 10);
const PASSWORD = __ENV.PASSWORD || 'Clave123*';
const QUESTIONS = parseInt(__ENV.QUESTIONS || '10', 10);

const tStart = new Trend('t_iniciar_simulacro', true);
const tAnswer = new Trend('t_responder', true);
const tFinish = new Trend('t_finalizar', true);
const tStats = new Trend('t_estadisticas', true);
const businessOk = new Rate('flujo_completo_ok');

export const options = {
  setupTimeout: '5m',
  scenarios: {
    // RNF-05: 100 estudiantes presentando simulacros a la vez
    estudiantes: {
      executor: 'ramping-vus',
      startVUs: 0,
      stages: [
        { duration: '30s', target: VUS },   // rampa
        { duration: '2m', target: VUS },    // carga sostenida
        { duration: '15s', target: 0 },
      ],
      gracefulRampDown: '30s',
    },
  },
  thresholds: {
    // RNF-04: tiempo de respuesta < 3 s
    http_req_duration: ['p(95)<3000'],
    'http_req_duration{endpoint:responder}': ['p(95)<1000'],
    http_req_failed: ['rate<0.01'],
    flujo_completo_ok: ['rate>0.99'],
  },
};

const json = { headers: { 'Content-Type': 'application/json' } };
const auth = (token, tag) => ({
  headers: { 'Content-Type': 'application/json', Authorization: `Bearer ${token}` },
  tags: { endpoint: tag },
});

/** Crea (si no existen) y autentica un estudiante por VU. */
export function setup() {
  const health = http.get(`${API}/actuator/health`);
  if (health.status !== 200) throw new Error(`El gateway no responde en ${API}`);
  const tokens = [];
  for (let i = 1; i <= VUS; i++) {
    const login = `k6est${String(i).padStart(3, '0')}`;
    http.post(`${API}/api/auth/register`, JSON.stringify({
      login, fullName: `Estudiante carga ${i}`, email: `${login}@carga.test`, password: PASSWORD,
    }), json); // 201 o 409 si ya existe
    const res = http.post(`${API}/api/auth/login`, JSON.stringify({ login, password: PASSWORD }), json);
    if (res.status !== 200) throw new Error(`Login de ${login} falló: ${res.status}`);
    tokens.push(res.json('accessToken'));
  }
  const catalog = http.get(`${API}/api/simulations/catalog`, auth(tokens[0], 'catalogo'));
  const available = (catalog.json() || []).reduce((s, c) => s + c.available, 0);
  if (available < QUESTIONS) throw new Error(`Solo hay ${available} preguntas publicadas; ejecute scripts/seed-preguntas.sh`);
  return { tokens };
}

export default function (data) {
  const token = data.tokens[(__VU - 1) % data.tokens.length];
  let completed = false;

  group('simulacro completo', () => {
    http.get(`${API}/api/simulations/catalog`, auth(token, 'catalogo'));

    let res = http.post(`${API}/api/simulations`, JSON.stringify({
      strategy: 'MIXED', questionCount: QUESTIONS, timeLimitMinutes: 30,
    }), auth(token, 'iniciar'));
    if (res.status === 409) {
      // quedó uno abierto de una iteración anterior: se retoma
      res = http.get(`${API}/api/simulations/mine/active`, auth(token, 'activo'));
    }
    tStart.add(res.timings.duration);
    if (!check(res, { 'simulacro iniciado': (r) => r.status === 201 || r.status === 200 })) return;
    const sim = res.json();

    for (const item of sim.items) {
      sleep(Math.random() * 1.5 + 0.5); // tiempo de lectura simulado
      const option = ['A', 'B', 'C', 'D'][Math.floor(Math.random() * 4)];
      const a = http.put(`${API}/api/simulations/${sim.id}/answers/${item.questionId}`,
        JSON.stringify({ selectedOption: option }), auth(token, 'responder'));
      tAnswer.add(a.timings.duration);
      check(a, { 'respuesta aceptada': (r) => r.status === 200 });
    }

    const f = http.post(`${API}/api/simulations/${sim.id}/finish`, null, auth(token, 'finalizar'));
    tFinish.add(f.timings.duration);
    const finished = check(f, {
      'finalizado': (r) => r.status === 200,
      'calificado': (r) => r.status === 200 && r.json('globalScore') !== undefined,
    });

    const s = http.get(`${API}/api/tracking/students/me/stats`, auth(token, 'estadisticas'));
    tStats.add(s.timings.duration);
    completed = finished && check(s, { 'estadísticas disponibles': (r) => r.status === 200 });
  });

  businessOk.add(completed);
  sleep(1);
}

export function handleSummary(data) {
  const p95 = data.metrics.http_req_duration.values['p(95)'];
  const failed = data.metrics.http_req_failed.values.rate;
  const line = `RNF-04/05 · VUs=${VUS} · p95=${p95.toFixed(0)} ms (meta < 3000) · errores=${(failed * 100).toFixed(2)} %\n`;
  return {
    stdout: line,
    'k6/results/resumen.json': JSON.stringify(data, null, 2),
  };
}
