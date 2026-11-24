/**
 * Nombres para personas de los atributos de las plantillas. En la base de datos el atributo conserva su nombre
 * técnico (por ejemplo "en_bandada"); aquí solo se decide cómo se le muestra a quien llena o lee la ficha.
 */
interface Texto {
  etiqueta: string;
  ayuda?: string;
}

const CONOCIDOS: Record<string, Texto> = {
  en_bandada: { etiqueta: '¿Iba en bandada?', ayuda: 'Indica si el ave estaba con otras de su especie.' },
  envergadura_cm: { etiqueta: 'Envergadura (cm)', ayuda: 'Distancia entre las puntas de las alas abiertas.' },
  sexo: { etiqueta: 'Sexo' },
  peso_kg: { etiqueta: 'Peso (kg)' },
  huella_cm: { etiqueta: 'Tamaño de la huella (cm)', ayuda: 'Largo de la huella que dejó el animal.' }
};

/** Para un atributo que no está en la lista: "largo_cola_cm" -> "Largo cola (cm)". */
function generico(nombre: string): string {
  const unidad = /_(cm|kg|m|g|mm)$/.exec(nombre);
  const base = (unidad ? nombre.slice(0, unidad.index) : nombre).replace(/_/g, ' ').trim();
  const texto = base.charAt(0).toUpperCase() + base.slice(1);
  return unidad ? `${texto} (${unidad[1]})` : texto;
}

/**
 * Nombres y ayudas que las personas escriben en el editor de plantillas (título y descripción de cada dato).
 * Se llenan al leer una plantilla; tienen prioridad sobre la lista de arriba. Si dos especies usan el mismo nombre
 * técnico con títulos distintos, vale el de la plantilla leída más recientemente.
 */
const PERSONALIZADOS: Record<string, Texto> = {};

export function registrarTextos(atributos: { nombre: string; titulo?: string | null; descripcion?: string | null }[]): void {
  for (const a of atributos) {
    if (a.titulo) {
      PERSONALIZADOS[a.nombre] = { etiqueta: a.titulo, ayuda: a.descripcion ?? undefined };
    } else {
      delete PERSONALIZADOS[a.nombre];
    }
  }
}

export function etiquetaAtributo(nombre: string): string {
  return PERSONALIZADOS[nombre]?.etiqueta ?? CONOCIDOS[nombre]?.etiqueta ?? generico(nombre);
}

export function ayudaAtributo(nombre: string): string | null {
  return PERSONALIZADOS[nombre]?.ayuda ?? CONOCIDOS[nombre]?.ayuda ?? null;
}

/** Un valor listo para leer: los booleanos como "Sí" o "No". */
export function valorLegible(valor: unknown): string {
  if (valor === true) {
    return 'Sí';
  }
  if (valor === false) {
    return 'No';
  }
  return String(valor);
}

/**
 * Regiones para registrar un hallazgo: los departamentos de Colombia y Bogotá. La búsqueda pública compara la región
 * tal cual, así que elegirla de una lista evita "Antioquia" escrita de cuatro maneras distintas.
 */
export const REGIONES: readonly string[] = [
  'Amazonas', 'Antioquia', 'Arauca', 'Atlántico', 'Bogotá D.C.', 'Bolívar', 'Boyacá', 'Caldas', 'Caquetá',
  'Casanare', 'Cauca', 'Cesar', 'Chocó', 'Córdoba', 'Cundinamarca', 'Guainía', 'Guaviare', 'Huila', 'La Guajira',
  'Magdalena', 'Meta', 'Nariño', 'Norte de Santander', 'Putumayo', 'Quindío', 'Risaralda',
  'San Andrés y Providencia', 'Santander', 'Sucre', 'Tolima', 'Valle del Cauca', 'Vaupés', 'Vichada'
];

const TIPOS: Record<string, string> = {
  number: 'un número',
  integer: 'un número entero',
  boolean: 'Sí o No',
  string: 'un texto',
  object: 'un objeto',
  array: 'una lista'
};

/**
 * Convierte los mensajes técnicos del validador de plantillas ("$: required property 'en_bandada' not found") en frases
 * para quien llena la ficha. Lo que no reconoce lo deja tal cual, sin el prefijo "$.".
 */
export function traducirErrores(texto: string): string {
  const nombre = (n: string): string => `«${etiquetaAtributo(n)}»`;
  let t = texto
    .replace(/\$: required property '([^']+)' not found/g, (_m, n: string) => `falta completar ${nombre(n)}, que es obligatorio`)
    .replace(
      /\$\.([\w]+): (?:must have a minimum value of|must be greater than or equal to) ([-\d.]+)/g,
      (_m, n: string, v: string) => `${nombre(n)} no puede ser menor que ${v}`
    )
    .replace(
      /\$\.([\w]+): (?:must have a maximum value of|must be less than or equal to) ([-\d.]+)/g,
      (_m, n: string, v: string) => `${nombre(n)} no puede ser mayor que ${v}`
    )
    .replace(
      /\$\.([\w]+): does not have a value in the enumeration \[([^\]]*)\]/g,
      (_m, n: string, v: string) => `${nombre(n)}: elige una de estas opciones (${v})`
    )
    .replace(
      /\$\.([\w]+): (\w+) found, (\w+) expected/g,
      (_m, n: string, _hallado: string, esperado: string) => `${nombre(n)} debe ser ${TIPOS[esperado] ?? esperado}`
    )
    .replace(
      /\$\.([\w]+): must be (?:a|an) (\w+)/g,
      (_m, n: string, esperado: string) => `${nombre(n)} debe ser ${TIPOS[esperado] ?? esperado}`
    )
    .replace(
      /\$: property '([^']+)' is not defined[^;.]*/g,
      (_m, n: string) => `${nombre(n)} no es un atributo de esta especie`
    );
  t = t.replace(/\$\.?:?\s*/g, '').trim();
  return t.length > 0 ? t.charAt(0).toUpperCase() + t.slice(1) : t;
}
