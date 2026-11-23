import { HttpErrorResponse } from '@angular/common/http';
import { Component, computed, inject, signal } from '@angular/core';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { AuthService } from '../../core/auth.service';
import { mensajeDeError } from '../../core/errores';
import { AtributoVista, Especie, Plantilla, PlantillasService, Validacion } from './plantillas.service';

/**
 * HU-02: atributos personalizados por especie.
 * Cada especie tiene su plantilla; el formulario de prueba se arma solo a partir de ella,
 * así que cambiar la plantilla cambia el formulario sin tocar el código de la pantalla.
 */
@Component({
  selector: 'app-plantillas',
  standalone: true,
  imports: [ReactiveFormsModule],
  template: `
    <div class="space-y-6">
      <section class="bg-white rounded-xl shadow p-6">
        <h2 class="text-lg font-bold text-bio-700">Plantilla de atributos por especie</h2>
        <div class="mt-4 max-w-md">
          <label for="especie" class="block text-sm font-medium mb-1">Especie</label>
          <select id="especie" (change)="elegir($event)" class="campo">
            <option value="">Elige una especie</option>
            @for (e of especies(); track e.id) {
              <option [value]="e.id">{{ e.nombreCientifico }}{{ e.nombreComun ? ' (' + e.nombreComun + ')' : '' }}</option>
            }
          </select>
        </div>
        @if (error()) {
          <p class="mt-3 text-sm text-red-600" role="alert">{{ error() }}</p>
        }
      </section>

      @if (plantilla(); as p) {
        @if (aviso()) {
          <p class="rounded-lg bg-bio-100 px-4 py-2 text-sm text-bio-900" role="status">{{ aviso() }}</p>
        }

        <section class="bg-white rounded-xl shadow p-6">
          <h3 class="font-bold">
            Plantilla activa · versión {{ p.version }}
          </h3>
          @if (p.atributos.length === 0) {
            <p class="mt-3 text-slate-500">Esta plantilla no define atributos.</p>
          } @else {
            <div class="mt-3 overflow-x-auto">
              <table class="w-full text-left text-sm">
                <thead class="border-b border-slate-200 text-slate-500">
                  <tr>
                    <th class="py-2 pr-4 font-medium">Atributo</th>
                    <th class="py-2 pr-4 font-medium">Tipo</th>
                    <th class="py-2 pr-4 font-medium">Obligatorio</th>
                    <th class="py-2 font-medium">Restricción</th>
                  </tr>
                </thead>
                <tbody>
                  @for (a of p.atributos; track a.nombre) {
                    <tr class="border-b border-slate-100">
                      <td class="py-2 pr-4 font-semibold">{{ a.nombre }}</td>
                      <td class="py-2 pr-4">{{ a.tipo }}</td>
                      <td class="py-2 pr-4">{{ a.requerido ? 'Sí' : 'No' }}</td>
                      <td class="py-2">{{ restriccion(a) }}</td>
                    </tr>
                  }
                </tbody>
              </table>
            </div>
          }
        </section>

        <section class="bg-white rounded-xl shadow p-6">
          <h3 class="font-bold">Probar un registro</h3>
          <p class="text-sm text-slate-500">El formulario sale de la plantilla. Nada se guarda al validar.</p>

          <div class="mt-4 grid gap-4 md:grid-cols-2">
            @for (a of p.atributos; track a.nombre) {
              <div>
                <label [for]="'attr-' + a.nombre" class="block text-sm font-medium mb-1">
                  {{ a.nombre }}{{ a.requerido ? ' *' : '' }}
                </label>
                @switch (a.tipo) {
                  @case ('ENUMERADO') {
                    <select [id]="'attr-' + a.nombre" (change)="poner(a, $event)" class="campo">
                      <option value="">—</option>
                      @for (v of a.valores; track v) {
                        <option [value]="v">{{ v }}</option>
                      }
                    </select>
                  }
                  @case ('BOOLEANO') {
                    <input [id]="'attr-' + a.nombre" type="checkbox" (change)="poner(a, $event)" class="h-5 w-5" />
                  }
                  @case ('NUMERO') {
                    <input [id]="'attr-' + a.nombre" type="number" (input)="poner(a, $event)" class="campo" />
                  }
                  @case ('FECHA') {
                    <input [id]="'attr-' + a.nombre" type="date" (input)="poner(a, $event)" class="campo" />
                  }
                  @default {
                    <input [id]="'attr-' + a.nombre" type="text" (input)="poner(a, $event)" class="campo" />
                  }
                }
              </div>
            }
          </div>

          <div class="mt-4 flex flex-wrap items-center gap-4">
            <button
              type="button"
              (click)="validar(p)"
              class="rounded-lg bg-bio-600 px-4 py-2 font-semibold text-white hover:bg-bio-700"
            >
              Validar
            </button>
            <code class="text-xs text-slate-500">{{ vistaPrevia() }}</code>
          </div>

          @if (resultado(); as r) {
            @if (r.valido) {
              <p class="mt-3 rounded-lg bg-bio-100 px-4 py-2 text-sm text-bio-900" role="status">
                Los atributos cumplen la plantilla.
              </p>
            } @else {
              <ul class="mt-3 rounded-lg bg-red-50 px-4 py-2 text-sm text-red-700" role="alert">
                @for (m of r.errores; track m) {
                  <li>{{ m }}</li>
                }
              </ul>
            }
          }
        </section>

        @if (puedeEditar()) {
          <section class="bg-white rounded-xl shadow p-6">
            <h3 class="font-bold">Editar esquema</h3>
            <p class="text-sm text-slate-500">
              Guardar crea una versión nueva y la deja activa. La versión anterior se conserva.
            </p>
            <label for="esquema" class="sr-only">Esquema JSON</label>
            <textarea
              id="esquema"
              [formControl]="esquemaTexto"
              rows="14"
              spellcheck="false"
              class="campo mt-3 font-mono text-xs"
            ></textarea>
            <div class="mt-3 flex flex-wrap items-center gap-4">
              <button
                type="button"
                (click)="guardar(p)"
                [disabled]="guardando()"
                class="rounded-lg bg-bio-600 px-4 py-2 font-semibold text-white hover:bg-bio-700 disabled:opacity-50"
              >
                {{ guardando() ? 'Guardando...' : 'Guardar como nueva versión' }}
              </button>
              @if (errorEsquema()) {
                <p class="text-sm text-red-600" role="alert">{{ errorEsquema() }}</p>
              }
            </div>
          </section>
        }
      }
    </div>
  `,
  styles: [
    `
      .campo {
        width: 100%;
        border-radius: 0.5rem;
        border: 1px solid rgb(203 213 225);
        padding: 0.5rem 0.75rem;
        background: white;
      }
      .campo:focus {
        outline: none;
        box-shadow: 0 0 0 2px #2f9e63;
      }
    `
  ]
})
export class PlantillasComponent {
  private api = inject(PlantillasService);
  private auth = inject(AuthService);

  especies = signal<Especie[]>([]);
  plantilla = signal<Plantilla | null>(null);
  error = signal<string | null>(null);
  aviso = signal<string | null>(null);

  /** Lo que el usuario va llenando en el formulario de prueba; solo trae los campos con valor. */
  valores = signal<Record<string, unknown>>({});
  resultado = signal<Validacion | null>(null);
  vistaPrevia = computed(() => JSON.stringify(this.valores()));

  esquemaTexto = new FormControl('', { nonNullable: true });
  errorEsquema = signal<string | null>(null);
  guardando = signal(false);

  /** Crear o editar plantillas: administrador y director (igual que el backend). */
  puedeEditar = computed(() => this.auth.tieneRol('ADMINISTRADOR', 'DIRECTOR'));

  constructor() {
    this.api.especies().subscribe({
      next: (e) => this.especies.set(e),
      error: (err: HttpErrorResponse) => this.error.set(mensajeDeError(err))
    });
  }

  elegir(ev: Event): void {
    const id = Number((ev.target as HTMLSelectElement).value);
    this.error.set(null);
    this.aviso.set(null);
    if (!id) {
      this.plantilla.set(null);
      return;
    }
    this.api.activaDe(id).subscribe({
      next: (p) => this.mostrar(p),
      error: (err: HttpErrorResponse) => {
        this.plantilla.set(null);
        this.error.set(mensajeDeError(err));
      }
    });
  }

  /** Lee el campo según su tipo y lo guarda; un campo vacío se quita para que "obligatorio" falle como corresponde. */
  poner(a: AtributoVista, ev: Event): void {
    const campo = ev.target as HTMLInputElement;
    const siguiente = { ...this.valores() };

    if (a.tipo === 'BOOLEANO') {
      siguiente[a.nombre] = campo.checked;
    } else if (campo.value === '') {
      delete siguiente[a.nombre];
    } else {
      siguiente[a.nombre] = a.tipo === 'NUMERO' ? Number(campo.value) : campo.value;
    }
    this.valores.set(siguiente);
    this.resultado.set(null);
  }

  validar(p: Plantilla): void {
    this.api.validar(p.especieId, this.valores()).subscribe({
      next: (r) => this.resultado.set(r),
      error: (err: HttpErrorResponse) => this.resultado.set({ valido: false, errores: [mensajeDeError(err)] })
    });
  }

  guardar(p: Plantilla): void {
    let esquema: Record<string, unknown>;
    try {
      esquema = JSON.parse(this.esquemaTexto.value) as Record<string, unknown>;
    } catch (e) {
      this.errorEsquema.set('El JSON no es válido: ' + (e as Error).message);
      return;
    }
    this.guardando.set(true);
    this.errorEsquema.set(null);

    this.api.editar(p.id, esquema).subscribe({
      next: (nueva) => {
        this.guardando.set(false);
        this.mostrar(nueva);
        this.aviso.set(`Versión ${nueva.version} guardada y activa.`);
      },
      error: (err: HttpErrorResponse) => {
        this.guardando.set(false);
        this.errorEsquema.set(mensajeDeError(err));
      }
    });
  }

  restriccion(a: AtributoVista): string {
    if (a.tipo === 'ENUMERADO') {
      return a.valores.join(', ');
    }
    if (a.minimo !== null && a.maximo !== null) {
      return `entre ${a.minimo} y ${a.maximo}`;
    }
    if (a.minimo !== null) {
      return `mínimo ${a.minimo}`;
    }
    if (a.maximo !== null) {
      return `máximo ${a.maximo}`;
    }
    return '—';
  }

  private mostrar(p: Plantilla): void {
    this.plantilla.set(p);
    this.valores.set({});
    this.resultado.set(null);
    this.errorEsquema.set(null);
    this.esquemaTexto.setValue(JSON.stringify(p.esquema, null, 2));
  }
}
