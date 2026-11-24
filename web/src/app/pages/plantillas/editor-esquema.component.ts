import { Component, Input, OnInit, computed, signal } from '@angular/core';
import { ayudaAtributo, etiquetaAtributo } from '../hallazgos/etiquetas-atributos';
import { TipoAtributo } from './plantillas.service';

/** Cómo se llama cada tipo de dato para quien edita la plantilla. */
export const NOMBRE_TIPO: Record<TipoAtributo, string> = {
  TEXTO: 'Texto',
  NUMERO: 'Número',
  BOOLEANO: 'Sí / No',
  ENUMERADO: 'Lista de opciones',
  FECHA: 'Fecha'
};

type Esquema = Record<string, unknown>;
type Propiedad = Record<string, unknown>;

interface Fila {
  /** Número interno para distinguir filas en pantalla. */
  id: number;
  /** Nombre técnico guardado en el esquema. Solo los datos nuevos lo dejan en null: se arma al guardar. */
  clave: string | null;
  titulo: string;
  tipo: TipoAtributo;
  requerido: boolean;
  /** Número "entero" en el esquema original; se conserva tal cual. */
  entero: boolean;
  minimo: string;
  maximo: string;
  /** Una opción por línea. */
  opciones: string;
  ayuda: string;
  /** Lo que ya tenía el esquema para este dato, para no perder nada que el editor no maneja. */
  original: Propiedad | null;
}

/** Palabras del esquema que el editor controla; el resto de lo que traiga un dato se conserva. */
const MANEJADAS = ['type', 'format', 'enum', 'minimum', 'maximum', 'title', 'description'];

/** "Largo de la cola (cm)" -> "largo_de_la_cola_cm": el nombre interno que se guarda en la base de datos. */
function aClave(titulo: string): string {
  const base = titulo
    .normalize('NFD')
    .replace(/[̀-ͯ]/g, '')
    .toLowerCase()
    .replace(/[^a-z0-9]+/g, '_')
    .replace(/^_+|_+$/g, '');
  const limpio = base.length > 0 ? base : 'dato';
  return /^[0-9]/.test(limpio) ? `dato_${limpio}` : limpio;
}

function esNumero(texto: string): boolean {
  return texto.trim() !== '' && Number.isFinite(Number(texto.replace(',', '.')));
}

function aNumero(texto: string): number {
  return Number(texto.replace(',', '.'));
}

function tipoDe(p: Propiedad): TipoAtributo {
  if (Array.isArray(p['enum'])) {
    return 'ENUMERADO';
  }
  switch (p['type']) {
    case 'boolean':
      return 'BOOLEANO';
    case 'number':
    case 'integer':
      return 'NUMERO';
    case 'string':
      return p['format'] === 'date' ? 'FECHA' : 'TEXTO';
    default:
      return 'TEXTO';
  }
}

function listaDeOpciones(texto: string): string[] {
  const vistas = new Set<string>();
  for (const linea of texto.split(/\r?\n/)) {
    const v = linea.trim();
    if (v) {
      vistas.add(v);
    }
  }
  return [...vistas];
}

/**
 * Editor de los datos de una plantilla sin escribir JSON: cada dato es una fila con su nombre, su tipo y lo que
 * necesita ese tipo. El esquema JSON se arma aquí y es lo que se manda al servidor; lo que el editor no maneja
 * (otras palabras del esquema) se conserva.
 */
@Component({
  selector: 'app-editor-esquema',
  standalone: true,
  template: `
    <div class="space-y-4">
      @for (f of filas(); track f.id; let i = $index) {
        <fieldset class="rounded-xl border border-slate-200 bg-white p-4">
          <legend class="px-1 text-sm font-semibold text-musgo">Dato {{ i + 1 }}</legend>

          <div class="grid gap-4 md:grid-cols-2">
            <div>
              <label [for]="'nombre-' + f.id" class="etiqueta">Nombre del dato *</label>
              <input
                [id]="'nombre-' + f.id"
                type="text"
                class="campo"
                placeholder="Ej.: Largo de la cola (cm)"
                [value]="f.titulo"
                (input)="cambiar(f.id, { titulo: texto($event) })"
              />
            </div>

            <div>
              <label [for]="'tipo-' + f.id" class="etiqueta">¿Qué se va a registrar?</label>
              <select
                [id]="'tipo-' + f.id"
                class="campo"
                [disabled]="f.clave !== null"
                (change)="cambiar(f.id, { tipo: tipoElegido($event) })"
              >
                @for (t of tipos; track t) {
                  <option [value]="t" [selected]="f.tipo === t">{{ nombreTipo[t] }}</option>
                }
              </select>
              @if (f.clave !== null) {
                <p class="mt-1 text-xs text-musgo">
                  El tipo de un dato que ya existe no se cambia. Si lo necesitas distinto, quítalo y agrega uno nuevo.
                </p>
              }
            </div>

            @if (f.tipo === 'NUMERO') {
              <div>
                <label [for]="'min-' + f.id" class="etiqueta">Valor mínimo (opcional)</label>
                <input
                  [id]="'min-' + f.id"
                  type="text"
                  inputmode="decimal"
                  class="campo"
                  placeholder="Ej.: 0"
                  [value]="f.minimo"
                  (input)="cambiar(f.id, { minimo: texto($event) })"
                />
              </div>
              <div>
                <label [for]="'max-' + f.id" class="etiqueta">Valor máximo (opcional)</label>
                <input
                  [id]="'max-' + f.id"
                  type="text"
                  inputmode="decimal"
                  class="campo"
                  placeholder="Ej.: 500"
                  [value]="f.maximo"
                  (input)="cambiar(f.id, { maximo: texto($event) })"
                />
              </div>
            }

            @if (f.tipo === 'ENUMERADO') {
              <div class="md:col-span-2">
                <label [for]="'opc-' + f.id" class="etiqueta">Opciones a elegir (una por línea) *</label>
                <textarea
                  [id]="'opc-' + f.id"
                  rows="4"
                  class="campo"
                  placeholder="macho&#10;hembra&#10;no determinado"
                  [value]="f.opciones"
                  (input)="cambiar(f.id, { opciones: texto($event) })"
                ></textarea>
              </div>
            }

            <div class="md:col-span-2">
              <label [for]="'ayuda-' + f.id" class="etiqueta">Ayuda para quien lo llena (opcional)</label>
              <input
                [id]="'ayuda-' + f.id"
                type="text"
                class="campo"
                placeholder="Ej.: Distancia entre las puntas de las alas abiertas."
                [value]="f.ayuda"
                (input)="cambiar(f.id, { ayuda: texto($event) })"
              />
            </div>
          </div>

          <div class="mt-4 flex flex-wrap items-center justify-between gap-3">
            <label class="flex items-center gap-2 text-sm">
              <input
                type="checkbox"
                class="h-5 w-5 rounded border-slate-300 accent-bio-600"
                [checked]="f.requerido"
                (change)="cambiar(f.id, { requerido: marcado($event) })"
              />
              Es obligatorio llenarlo
            </label>
            <button type="button" class="btn-texto" (click)="quitar(f.id)">Quitar este dato</button>
          </div>

          @if (revisado() && problemasDe(f).length > 0) {
            <ul class="aviso-error mt-3 space-y-1" role="alert">
              @for (m of problemasDe(f); track m) {
                <li>{{ m }}</li>
              }
            </ul>
          }
        </fieldset>
      }

      @if (filas().length === 0) {
        <p class="text-musgo">Esta plantilla no tiene datos. Agrega el primero.</p>
      }

      <button type="button" class="btn-secundario" (click)="agregar()">Agregar un dato</button>

      @if (revisado() && sinDatos()) {
        <p class="aviso-error" role="alert">La plantilla necesita al menos un dato.</p>
      }
    </div>

    <details class="mt-5 text-sm text-musgo">
      <summary class="cursor-pointer select-none">Ver cómo se guarda (para personal técnico)</summary>
      <pre class="mt-2 overflow-x-auto rounded-lg bg-liquen p-3 text-xs">{{ vistaJson() }}</pre>
    </details>
  `
})
export class EditorEsquemaComponent implements OnInit {
  /** El esquema de la versión activa, de donde arranca la edición. */
  @Input({ required: true }) esquema: Esquema = {};

  readonly tipos: TipoAtributo[] = ['TEXTO', 'NUMERO', 'BOOLEANO', 'ENUMERADO', 'FECHA'];
  readonly nombreTipo = NOMBRE_TIPO;

  filas = signal<Fila[]>([]);
  /** Los avisos de cada fila aparecen cuando la persona intenta guardar, no mientras todavía está escribiendo. */
  revisado = signal(false);

  sinDatos = computed(() => this.filas().length === 0);
  vistaJson = computed(() => JSON.stringify(this.armar(), null, 2));

  private siguienteId = 1;

  ngOnInit(): void {
    const propiedades = (this.esquema['properties'] ?? {}) as Record<string, Propiedad>;
    const requeridos = new Set(((this.esquema['required'] ?? []) as unknown[]).map(String));

    this.filas.set(
      Object.entries(propiedades).map(([clave, p]) => {
        const tipo = tipoDe(p);
        return {
          id: this.siguienteId++,
          clave,
          titulo: typeof p['title'] === 'string' && p['title'] ? (p['title'] as string) : etiquetaAtributo(clave),
          tipo,
          requerido: requeridos.has(clave),
          entero: p['type'] === 'integer',
          minimo: typeof p['minimum'] === 'number' ? String(p['minimum']) : '',
          maximo: typeof p['maximum'] === 'number' ? String(p['maximum']) : '',
          opciones: Array.isArray(p['enum']) ? (p['enum'] as unknown[]).map(String).join('\n') : '',
          ayuda: typeof p['description'] === 'string' ? (p['description'] as string) : (ayudaAtributo(clave) ?? ''),
          original: p
        };
      })
    );
  }

  texto(ev: Event): string {
    return (ev.target as HTMLInputElement | HTMLTextAreaElement | HTMLSelectElement).value;
  }

  tipoElegido(ev: Event): TipoAtributo {
    return this.texto(ev) as TipoAtributo;
  }

  marcado(ev: Event): boolean {
    return (ev.target as HTMLInputElement).checked;
  }

  cambiar(id: number, parche: Partial<Fila>): void {
    this.filas.update((filas) => filas.map((f) => (f.id === id ? { ...f, ...parche } : f)));
  }

  agregar(): void {
    this.filas.update((filas) => [
      ...filas,
      {
        id: this.siguienteId++,
        clave: null,
        titulo: '',
        tipo: 'TEXTO',
        requerido: false,
        entero: false,
        minimo: '',
        maximo: '',
        opciones: '',
        ayuda: '',
        original: null
      }
    ]);
  }

  quitar(id: number): void {
    this.filas.update((filas) => filas.filter((f) => f.id !== id));
  }

  /** Lo que está mal en un dato, dicho en español. Vacío si está bien. */
  problemasDe(f: Fila): string[] {
    const m: string[] = [];
    const titulo = f.titulo.trim();
    if (!titulo) {
      m.push('Escribe el nombre del dato.');
    } else if (this.filas().some((o) => o.id !== f.id && o.titulo.trim().toLowerCase() === titulo.toLowerCase())) {
      m.push('Ya hay otro dato con este nombre. Usa uno distinto.');
    }
    if (f.tipo === 'NUMERO') {
      const hayMin = f.minimo.trim() !== '';
      const hayMax = f.maximo.trim() !== '';
      if (hayMin && !esNumero(f.minimo)) {
        m.push('El valor mínimo debe ser un número, por ejemplo 0 o 2.5.');
      }
      if (hayMax && !esNumero(f.maximo)) {
        m.push('El valor máximo debe ser un número, por ejemplo 100 o 2.5.');
      }
      if (hayMin && hayMax && esNumero(f.minimo) && esNumero(f.maximo) && aNumero(f.minimo) > aNumero(f.maximo)) {
        m.push('El mínimo no puede ser mayor que el máximo.');
      }
    }
    if (f.tipo === 'ENUMERADO' && listaDeOpciones(f.opciones).length < 2) {
      m.push('Escribe al menos dos opciones, una por línea.');
    }
    return m;
  }

  /**
   * Revisa todo y devuelve el esquema listo para guardar, o null si hay algo por corregir
   * (en ese caso los avisos quedan a la vista en cada dato).
   */
  revisar(): Esquema | null {
    this.revisado.set(true);
    const filas = this.filas();
    if (filas.length === 0 || filas.some((f) => this.problemasDe(f).length > 0)) {
      return null;
    }
    return this.armar();
  }

  /** Arma el esquema con lo que hay en pantalla, sin revisar. */
  private armar(): Esquema {
    const usadas = new Set(this.filas().flatMap((f) => (f.clave ? [f.clave] : [])));
    const propiedades: Record<string, Propiedad> = {};
    const requeridos: string[] = [];

    for (const f of this.filas()) {
      let clave = f.clave;
      if (clave === null) {
        const base = aClave(f.titulo);
        clave = base;
        for (let n = 2; usadas.has(clave); n++) {
          clave = `${base}_${n}`;
        }
        usadas.add(clave);
      }

      const p: Propiedad = {};
      for (const [k, v] of Object.entries(f.original ?? {})) {
        if (!MANEJADAS.includes(k)) {
          p[k] = v;
        }
      }
      switch (f.tipo) {
        case 'TEXTO':
          p['type'] = 'string';
          break;
        case 'FECHA':
          p['type'] = 'string';
          p['format'] = 'date';
          break;
        case 'BOOLEANO':
          p['type'] = 'boolean';
          break;
        case 'ENUMERADO':
          p['type'] = 'string';
          p['enum'] = listaDeOpciones(f.opciones);
          break;
        case 'NUMERO':
          p['type'] = f.entero ? 'integer' : 'number';
          if (esNumero(f.minimo)) {
            p['minimum'] = aNumero(f.minimo);
          }
          if (esNumero(f.maximo)) {
            p['maximum'] = aNumero(f.maximo);
          }
          break;
      }
      const titulo = f.titulo.trim();
      if (titulo) {
        p['title'] = titulo;
      }
      if (f.ayuda.trim()) {
        p['description'] = f.ayuda.trim();
      }

      propiedades[clave] = p;
      if (f.requerido) {
        requeridos.push(clave);
      }
    }

    const esquema: Esquema = { ...this.esquema, type: 'object', properties: propiedades };
    if (requeridos.length > 0) {
      esquema['required'] = requeridos;
    } else {
      delete esquema['required'];
    }
    return esquema;
  }
}
