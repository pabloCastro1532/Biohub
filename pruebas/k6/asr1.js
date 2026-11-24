// ASR-1: consulta publica bajo picos. Todo el trafico entra por el gateway (puerto 8080).
//
// Dos fases seguidas, con el registro de hallazgos corriendo en paralelo en las dos:
//   1) normal: TASA_NORMAL busquedas por segundo (por defecto 5)
//   2) pico:   TASA_NORMAL x FACTOR busquedas por segundo (por defecto 5 x 60 = 300)
// Criterios (documento de la Entrega 2):
//   p95 de la busqueda en el pico <= 2 s, errores < 2 %, y el registro de hallazgos no empeora mas de 10 %.
import http from 'k6/http';
import { check } from 'k6';

const BASE = __ENV.BASE_URL || 'http://localhost:8080';
const NORMAL = Number(__ENV.TASA_NORMAL || 5);
const FACTOR = Number(__ENV.FACTOR || 60);
const FASE = Number(__ENV.FASE_SEGUNDOS || 60);
const PAUSA = 10; // segundos entre fases, para que el sistema vuelva a reposo

export const options = {
  scenarios: {
    busqueda_normal: {
      executor: 'constant-arrival-rate', exec: 'buscar',
      rate: NORMAL, timeUnit: '1s', duration: `${FASE}s`,
      preAllocatedVUs: 10, maxVUs: 100,
    },
    registro_normal: {
      executor: 'constant-arrival-rate', exec: 'registrar',
      rate: 1, timeUnit: '1s', duration: `${FASE}s`,
      preAllocatedVUs: 2, maxVUs: 20,
    },
    busqueda_pico: {
      executor: 'constant-arrival-rate', exec: 'buscar', startTime: `${FASE + PAUSA}s`,
      rate: NORMAL * FACTOR, timeUnit: '1s', duration: `${FASE}s`,
      preAllocatedVUs: 200, maxVUs: 1000,
    },
    registro_pico: {
      executor: 'constant-arrival-rate', exec: 'registrar', startTime: `${FASE + PAUSA}s`,
      rate: 1, timeUnit: '1s', duration: `${FASE}s`,
      preAllocatedVUs: 5, maxVUs: 50,
    },
  },
  thresholds: {
    'http_req_duration{scenario:busqueda_pico}': ['p(95)<2000'],
    'http_req_failed{scenario:busqueda_pico}': ['rate<0.02'],
    // Estas dos solo existen para que k6 calcule sus cifras; el veredicto de +10 % se calcula al final.
    'http_req_duration{scenario:busqueda_normal}': ['p(95)<2000'],
    'http_req_duration{scenario:registro_normal}': ['p(95)<10000'],
    'http_req_duration{scenario:registro_pico}': ['p(95)<10000'],
  },
};

const CONSULTAS = ['', 'jaguar', 'ara', 'panthera', 'guacamaya', 'onca', 'macao', 'bandera'];
const REGIONES = ['', '', 'Meta', 'Amazonas', 'Casanare'];
const elegir = (lista) => lista[Math.floor(Math.random() * lista.length)];

export function setup() {
  const r = http.post(`${BASE}/api/auth/login`,
    JSON.stringify({ usuario: 'investigador', clave: __ENV.CLAVE_DEMO || 'biohub' }),
    { headers: { 'Content-Type': 'application/json' } });
  if (r.status !== 200) {
    throw new Error(`No se pudo iniciar sesion como investigador (estado ${r.status}). Revisa que el sistema este arriba.`);
  }
  return { token: r.json('token') };
}

// Lectura publica: 70 % busquedas (con y sin filtro) y 30 % fichas. Mezcla aciertos y fallos de cache.
export function buscar() {
  let res;
  if (Math.random() < 0.7) {
    const q = elegir(CONSULTAS);
    const region = elegir(REGIONES);
    let url = `${BASE}/api/publico/especies?limite=20`;
    if (q) url += `&q=${q}`;
    if (region) url += `&region=${region}`;
    res = http.get(url, { tags: { tipo: 'busqueda' } });
  } else {
    res = http.get(`${BASE}/api/publico/especies/${1 + Math.floor(Math.random() * 2)}`, { tags: { tipo: 'ficha' } });
  }
  check(res, { 'respuesta 200': (r) => r.status === 200 });
}

// Escritura cientifica: un hallazgo por segundo. Cada uno publica un evento que invalida la cache publica.
export function registrar(datos) {
  const jaguar = Math.random() < 0.5;
  const cuerpo = {
    especieId: jaguar ? 1 : 2,
    region: 'Meta',
    latitud: 4.15,
    longitud: -73.64,
    fechaObservacion: '2026-11-20',
    atributos: jaguar ? { sexo: 'macho' } : { en_bandada: true },
  };
  const res = http.post(`${BASE}/api/hallazgos`, JSON.stringify(cuerpo), {
    headers: { 'Content-Type': 'application/json', Authorization: `Bearer ${datos.token}` },
    tags: { tipo: 'registro' },
  });
  check(res, { 'hallazgo creado (201)': (r) => r.status === 201 });
}

const valor = (data, metrica, estadistico) => {
  const m = data.metrics[metrica];
  if (!m || !m.values) return undefined;
  const v = m.values[estadistico];
  // Un p95 de exactamente 0 ms significa que no hubo peticiones: se trata como "sin datos".
  return estadistico === 'rate' ? v : (v ? v : undefined);
};
const ms = (n) => (n === undefined ? 'sin datos' : `${n.toFixed(0)} ms`);

export function handleSummary(data) {
  const p95Normal = valor(data, 'http_req_duration{scenario:busqueda_normal}', 'p(95)');
  const p95Pico = valor(data, 'http_req_duration{scenario:busqueda_pico}', 'p(95)');
  const errores = p95Pico === undefined ? undefined : valor(data, 'http_req_failed{scenario:busqueda_pico}', 'rate');
  const regNormal = valor(data, 'http_req_duration{scenario:registro_normal}', 'p(95)');
  const regPico = valor(data, 'http_req_duration{scenario:registro_pico}', 'p(95)');
  const empeora = regNormal && regPico ? (regPico / regNormal - 1) * 100 : undefined;

  const si = (ok) => (ok ? 'CUMPLE' : 'NO CUMPLE');
  const lineas = [
    '',
    `ASR-1  normal: ${NORMAL} busquedas/s   pico: ${NORMAL * FACTOR} busquedas/s (x${FACTOR})   ${FASE} s por fase`,
    '',
    `Busqueda, p95 en trafico normal ........ ${ms(p95Normal)}`,
    `Busqueda, p95 en el pico (<= 2000 ms) .. ${ms(p95Pico)}   ${p95Pico === undefined ? '' : si(p95Pico <= 2000)}`,
    `Errores en el pico (< 2 %) ............. ${errores === undefined ? 'sin datos' : (errores * 100).toFixed(2) + ' %'}   ${errores === undefined ? '' : si(errores < 0.02)}`,
    `Registro, p95 normal ................... ${ms(regNormal)}`,
    `Registro, p95 en el pico ............... ${ms(regPico)}`,
    `Registro empeora (<= 10 %) ............. ${empeora === undefined ? 'sin datos' : empeora.toFixed(1) + ' %'}   ${empeora === undefined ? '' : si(empeora <= 10)}`,
    '',
  ];
  return {
    stdout: lineas.join('\n'),
    'resultado-asr1.json': JSON.stringify(data, null, 2),
  };
}
