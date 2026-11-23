// Servidor falso SOLO para probar las pantallas sin Docker ni backend. NO se sube al repositorio.
// Imita: POST /api/auth/login, /api/equipos (HU-03), /api/equipos/.../responsable (HU-06 y 07) y /api/plantillas (HU-02).
// Usuarios de prueba (clave "biohub"): investigador, director, auditor, administrador, seguridad, visitante.
// Investigadores que se pueden asignar como responsable: investigador, inv.demo, inv.demo2.
// Uso: node mock-api.js   (en otra ventana: npm start). Se detiene con Ctrl + C. Los datos viven en memoria.
const http = require('http');

const PUERTO = 8081;
const CLAVE = 'biohub';
const USUARIOS = {
  investigador: 'INVESTIGADOR',
  director: 'DIRECTOR',
  auditor: 'AUDITOR',
  administrador: 'ADMINISTRADOR',
  seguridad: 'SEGURIDAD',
  visitante: 'VISITANTE',
  'inv.demo': 'INVESTIGADOR',
  'inv.demo2': 'INVESTIGADOR'
};
const GESTORES = ['ADMINISTRADOR', 'DIRECTOR'];
const LECTORES = ['ADMINISTRADOR', 'DIRECTOR', 'INVESTIGADOR'];

// ---------- datos en memoria ----------
const especies = [
  { id: 1, nombreCientifico: 'Panthera onca', nombreComun: 'Jaguar' },
  { id: 2, nombreCientifico: 'Ara macao', nombreComun: 'Guacamaya bandera' }
];
let plantillas = [
  {
    id: 1, especieId: 1, version: 1, activa: true,
    esquema: {
      type: 'object',
      properties: {
        peso_kg: { type: 'number', minimum: 0 },
        sexo: { type: 'string', enum: ['macho', 'hembra', 'desconocido'] },
        huella_cm: { type: 'number', minimum: 0 }
      },
      required: ['sexo']
    }
  },
  {
    id: 2, especieId: 2, version: 1, activa: true,
    esquema: {
      type: 'object',
      properties: { envergadura_cm: { type: 'number', minimum: 0 }, en_bandada: { type: 'boolean' } },
      required: ['en_bandada']
    }
  }
];
let siguientePlantilla = 3;
const equipos = [
  { id: 1, codigo: 'GPS-001', nombre: 'GPS Garmin', tipo: 'Navegación', descripcion: 'GPS de mano', activo: true },
  { id: 2, codigo: 'BIN-001', nombre: 'Binoculares 10x42', tipo: 'Observación', descripcion: null, activo: true }
];
const responsables = {}; // equipoId -> { responsable, asignadoPor, asignadoEn }

// ---------- utilidades ----------
function tokenFalso(usuario, rol) {
  const b64 = (o) => Buffer.from(JSON.stringify(o)).toString('base64url');
  return `${b64({ alg: 'none' })}.${b64({ sub: usuario, rol })}.mock`;
}

function leerToken(req) {
  const h = req.headers.authorization || '';
  if (!h.startsWith('Bearer ')) return null;
  try {
    return JSON.parse(Buffer.from(h.slice(7).split('.')[1], 'base64url').toString());
  } catch {
    return null;
  }
}

function atributosDe(esquema) {
  const requeridos = esquema.required || [];
  return Object.entries(esquema.properties || {}).map(([nombre, p]) => {
    let tipo;
    if (p.enum) tipo = 'ENUMERADO';
    else if (p.type === 'string') tipo = p.format === 'date' ? 'FECHA' : 'TEXTO';
    else if (p.type === 'number' || p.type === 'integer') tipo = 'NUMERO';
    else if (p.type === 'boolean') tipo = 'BOOLEANO';
    else throw new Error(`Tipo no soportado en el atributo '${nombre}': '${p.type}'`);
    return {
      nombre, tipo, requerido: requeridos.includes(nombre),
      valores: p.enum ? p.enum.map(String) : [],
      minimo: typeof p.minimum === 'number' ? p.minimum : null,
      maximo: typeof p.maximum === 'number' ? p.maximum : null
    };
  });
}

function plantillaVista(p) {
  return { ...p, atributos: atributosDe(p.esquema) };
}

function validar(esquema, datos) {
  const errores = [];
  const props = esquema.properties || {};
  for (const r of esquema.required || []) {
    if (datos[r] === undefined) errores.push(`$.${r}: es obligatorio`);
  }
  for (const [k, v] of Object.entries(datos)) {
    const p = props[k];
    if (!p) {
      if (esquema.additionalProperties === false) errores.push(`$.${k}: el atributo no existe en la plantilla`);
      continue;
    }
    if (p.enum && !p.enum.includes(v)) errores.push(`$.${k}: debe ser uno de [${p.enum.join(', ')}]`);
    else if (p.type === 'number' && typeof v !== 'number') errores.push(`$.${k}: debe ser un número`);
    else if (p.type === 'boolean' && typeof v !== 'boolean') errores.push(`$.${k}: debe ser verdadero o falso`);
    else if (typeof p.minimum === 'number' && typeof v === 'number' && v < p.minimum) errores.push(`$.${k}: debe ser mayor o igual a ${p.minimum}`);
  }
  return errores;
}

function responder(res, estado, cuerpo) {
  if (cuerpo === undefined) {
    res.writeHead(estado).end();
    return;
  }
  res.writeHead(estado, { 'Content-Type': 'application/json' });
  res.end(JSON.stringify(cuerpo));
}
const error = (res, estado, mensaje) => responder(res, estado, { error: mensaje });

function vistaResponsable(e) {
  const r = responsables[e.id];
  return { equipoId: e.id, equipoCodigo: e.codigo, responsable: r ? r.responsable : null, asignadoPor: r ? r.asignadoPor : null, asignadoEn: r ? r.asignadoEn : null };
}

// ---------- rutas ----------
function manejar(req, res, datos) {
  const url = req.url.split('?')[0];
  const m = req.method;

  if (m === 'POST' && url === '/api/auth/login') {
    const rol = USUARIOS[datos.usuario];
    if (!rol || datos.clave !== CLAVE) return responder(res, 401);
    return responder(res, 200, { token: tokenFalso(datos.usuario, rol), rol });
  }

  const sesion = leerToken(req);
  if (!sesion) return responder(res, 401);
  const exigir = (roles) => (roles.includes(sesion.rol) ? true : (responder(res, 403), false));

  let g;
  // ----- equipos -----
  if (url === '/api/equipos' && m === 'GET') return exigir(LECTORES) && responder(res, 200, [...equipos].sort((a, b) => a.nombre.localeCompare(b.nombre)));
  if (url === '/api/equipos' && m === 'POST') {
    if (!exigir(GESTORES)) return;
    const codigo = String(datos.codigo || '').trim().toUpperCase();
    if (!codigo || !String(datos.nombre || '').trim() || !String(datos.tipo || '').trim()) return error(res, 400, 'Datos incompletos o mal formados');
    if (equipos.some((e) => e.codigo === codigo)) return error(res, 409, `Ya existe un equipo con el código ${codigo}`);
    const nuevo = { id: equipos.length + 1, codigo, nombre: datos.nombre.trim(), tipo: datos.tipo.trim(), descripcion: datos.descripcion || null, activo: true };
    equipos.push(nuevo);
    return responder(res, 201, nuevo);
  }
  if (url === '/api/equipos/responsables' && m === 'GET') {
    if (!exigir(LECTORES)) return;
    return responder(res, 200, equipos.filter((e) => responsables[e.id]).map(vistaResponsable));
  }
  if ((g = url.match(/^\/api\/equipos\/(\d+)\/responsable$/))) {
    const e = equipos.find((x) => x.id === Number(g[1]));
    if (m === 'GET') {
      if (!exigir(LECTORES)) return;
      return e ? responder(res, 200, vistaResponsable(e)) : error(res, 404, `No existe el equipo ${g[1]}`);
    }
    if (m === 'PUT') {
      if (!exigir(GESTORES)) return;
      const nombre = String(datos.usuario || '').trim();
      if (!nombre) return error(res, 400, 'Datos incompletos o mal formados');
      if (!e) return error(res, 404, `No existe el equipo ${g[1]}`);
      if (!USUARIOS[nombre]) return error(res, 404, `No existe el usuario ${nombre}`);
      if (USUARIOS[nombre] !== 'INVESTIGADOR') return error(res, 400, `El usuario ${nombre} no es un investigador activo`);
      if (responsables[e.id] && responsables[e.id].responsable === nombre) return error(res, 409, `${nombre} ya es el responsable del equipo ${e.codigo}`);
      responsables[e.id] = { responsable: nombre, asignadoPor: sesion.sub, asignadoEn: new Date().toISOString() };
      return responder(res, 200, vistaResponsable(e));
    }
  }

  // ----- plantillas -----
  if (url === '/api/plantillas/especies' && m === 'GET') return exigir(LECTORES) && responder(res, 200, especies);
  if ((g = url.match(/^\/api\/plantillas\/activa\/(\d+)$/)) && m === 'GET') {
    if (!exigir(LECTORES)) return;
    const p = plantillas.find((x) => x.especieId === Number(g[1]) && x.activa);
    return p ? responder(res, 200, plantillaVista(p)) : error(res, 404, `La especie ${g[1]} no tiene plantilla activa`);
  }
  if ((g = url.match(/^\/api\/plantillas\/activa\/(\d+)\/validar$/)) && m === 'POST') {
    if (!exigir(LECTORES)) return;
    const p = plantillas.find((x) => x.especieId === Number(g[1]) && x.activa);
    if (!p) return error(res, 404, `La especie ${g[1]} no tiene plantilla activa`);
    const errores = validar(p.esquema, datos);
    return responder(res, 200, { valido: errores.length === 0, errores });
  }
  if ((g = url.match(/^\/api\/plantillas\/(\d+)$/)) && m === 'PUT') {
    if (!exigir(GESTORES)) return;
    const vieja = plantillas.find((x) => x.id === Number(g[1]));
    if (!vieja) return error(res, 404, `No existe la plantilla ${g[1]}`);
    if (!datos.esquema || typeof datos.esquema !== 'object') return error(res, 400, 'Datos incompletos o mal formados');
    try {
      atributosDe(datos.esquema);
    } catch (err) {
      return error(res, 400, err.message);
    }
    vieja.activa = false;
    const nueva = { id: siguientePlantilla++, especieId: vieja.especieId, version: vieja.version + 1, activa: true, esquema: datos.esquema };
    plantillas.push(nueva);
    return responder(res, 200, plantillaVista(nueva));
  }

  responder(res, 404);
}

http
  .createServer((req, res) => {
    let cuerpo = '';
    req.on('data', (d) => (cuerpo += d));
    req.on('end', () => {
      let datos = {};
      if (cuerpo) {
        try {
          datos = JSON.parse(cuerpo);
        } catch {
          return responder(res, 400);
        }
      }
      manejar(req, res, datos);
    });
  })
  .listen(PUERTO, () => console.log(`Servidor falso en http://localhost:${PUERTO}  (usuarios: ${Object.keys(USUARIOS).join(', ')}; clave: ${CLAVE})`));
