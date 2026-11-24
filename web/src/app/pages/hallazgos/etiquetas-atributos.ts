/**
 * Nombres para personas de los atributos de las plantillas. En la base de datos el atributo conserva su nombre
 * técnico (por ejemplo "en_bandada"); aquí solo se decide cómo se le muestra a quien llena o lee la ficha.
 */
interface Texto {
  etiqueta: string;
  ayuda?: string;
}

const CONOCIDOS: Record<string, Texto> = {
  en_bandada: { etiqueta: '¿Iba en bandada?', ayuda: 'Marca la casilla si el ave estaba con otras de su especie.' },
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

export function etiquetaAtributo(nombre: string): string {
  return CONOCIDOS[nombre]?.etiqueta ?? generico(nombre);
}

export function ayudaAtributo(nombre: string): string | null {
  return CONOCIDOS[nombre]?.ayuda ?? null;
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
