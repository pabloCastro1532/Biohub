import { HttpErrorResponse } from '@angular/common/http';
import { Component, ViewChild, computed, inject, signal } from '@angular/core';
import { AuthService } from '../../core/auth.service';
import { mensajeDeError } from '../../core/errores';
import { AtributosFormComponent } from '../hallazgos/atributos-form.component';
import { etiquetaAtributo, traducirErrores } from '../hallazgos/etiquetas-atributos';
import { EditorEsquemaComponent, NOMBRE_TIPO } from './editor-esquema.component';
import { AtributoVista, Especie, Plantilla, PlantillasService, Validacion } from './plantillas.service';

/**
 * HU-02: atributos personalizados por especie.
 * Cada especie tiene su plantilla; el formulario de prueba se arma solo a partir de ella,
 * así que cambiar la plantilla cambia el formulario sin tocar el código de la pantalla.
 */
@Component({
  selector: 'app-plantillas',
  standalone: true,
  imports: [AtributosFormComponent, EditorEsquemaComponent],
  template: `
    <header class="mb-8">
      <h1 class="text-2xl font-semibold tracking-tight">Plantillas por especie</h1>
      <p class="mt-1 text-musgo">Cada especie define qué datos se piden al registrar uno de sus hallazgos.</p>
    </header>

    <div class="space-y-6">
      <section class="tarjeta">
        <label for="especie" class="etiqueta">Especie</label>
        <select id="especie" (change)="elegir($event)" class="campo max-w-md">
          <option value="">Elige una especie</option>
          @for (e of especies(); track e.id) {
            <option [value]="e.id">{{ e.nombreCientifico }}{{ e.nombreComun ? ' (' + e.nombreComun + ')' : '' }}</option>
          }
        </select>
        @if (error()) {
          <p class="mt-3 text-sm text-red-700" role="alert">{{ error() }}</p>
        }
      </section>

      @if (plantilla(); as p) {
        @if (aviso()) {
          <p class="aviso-ok" role="status">{{ aviso() }}</p>
        }

        <section class="tarjeta !p-0 overflow-hidden">
          <div class="flex items-center justify-between px-6 pb-2 pt-6">
            <h2 class="text-lg font-semibold">Datos que se piden</h2>
            <span class="pildora bg-bio-100 text-bio-900">Versión {{ p.version }}, activa</span>
          </div>
          @if (p.atributos.length === 0) {
            <p class="px-6 pb-6 text-musgo">Esta plantilla todavía no pide ningún dato.</p>
          } @else {
            <div class="overflow-x-auto px-3 pb-3">
              <table class="tabla">
                <thead>
                  <tr>
                    <th>Dato</th>
                    <th>Tipo</th>
                    <th>¿Obligatorio?</th>
                    <th>Opciones o límites</th>
                  </tr>
                </thead>
                <tbody>
                  @for (a of p.atributos; track a.nombre) {
                    <tr>
                      <td class="font-semibold">{{ nombreDe(a) }}</td>
                      <td><span class="pildora bg-liquen text-musgo">{{ tipoLegible(a) }}</span></td>
                      <td>
                        @if (a.requerido) {
                          <span class="pildora bg-bio-100 text-bio-900">Sí</span>
                        } @else {
                          <span class="text-musgo">No</span>
                        }
                      </td>
                      <td class="text-musgo">{{ restriccion(a) }}</td>
                    </tr>
                  }
                </tbody>
              </table>
            </div>
          }
        </section>

        <section class="tarjeta">
          <h2 class="text-lg font-semibold">Probar el formulario</h2>
          <p class="mt-1 text-sm text-musgo">
            Así lo verá quien registre un hallazgo de esta especie. Llénalo y pulsa «Comprobar datos»; no se guarda nada.
          </p>

          <div class="mt-5">
            <!-- Se vuelve a crear al cambiar de plantilla o de versión, para que no queden valores de la anterior. -->
            @for (clave of [p.id + '-' + p.version]; track clave) {
              <app-atributos-form [atributos]="p.atributos" prefijo="attr" (valoresChange)="alCambiar($event)" />
            }
          </div>

          <div class="mt-5 flex flex-wrap items-center gap-4">
            <button type="button" (click)="validar(p)" class="btn-primario">Comprobar datos</button>
          </div>

          @if (resultado(); as r) {
            @if (r.valido) {
              <p class="aviso-ok mt-4" role="status">Los datos están completos y son válidos.</p>
            } @else {
              <ul class="aviso-error mt-4 space-y-1" role="alert">
                @for (m of r.errores; track m) {
                  <li>{{ texto(m) }}</li>
                }
              </ul>
            }
          }
        </section>

        @if (puedeEditar()) {
          <section class="tarjeta">
            <h2 class="text-lg font-semibold">Cambiar los datos que se piden</h2>
            <p class="mt-1 text-sm text-musgo">
              Agrega, cambia o quita datos. Al guardar se crea una versión nueva, que queda activa; la anterior se
              conserva. El formulario de arriba mostrará los cambios cuando guardes.
            </p>

            <div class="mt-5">
              <!-- Se vuelve a crear al cambiar de plantilla o de versión, para que arranque con lo guardado. -->
              @for (clave of [p.id + '-' + p.version]; track clave) {
                <app-editor-esquema [esquema]="p.esquema" />
              }
            </div>

            <div class="mt-5 flex flex-wrap items-center gap-4">
              <button type="button" (click)="guardar(p)" [disabled]="guardando()" class="btn-primario">
                {{ guardando() ? 'Guardando...' : 'Guardar cambios' }}
              </button>
              @if (errorEsquema()) {
                <p class="text-sm text-red-700" role="alert">{{ errorEsquema() }}</p>
              }
            </div>
          </section>
        }
      }
    </div>
  `
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

  @ViewChild(EditorEsquemaComponent) editor?: EditorEsquemaComponent;
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

  /** El formulario de prueba avisa cada vez que el usuario cambia un campo. */
  alCambiar(valores: Record<string, unknown>): void {
    this.valores.set(valores);
    this.resultado.set(null);
  }

  nombreDe(a: AtributoVista): string {
    return a.titulo || etiquetaAtributo(a.nombre);
  }

  tipoLegible(a: AtributoVista): string {
    return NOMBRE_TIPO[a.tipo];
  }

  /** Un mensaje del validador en lenguaje claro. */
  texto(mensaje: string): string {
    return traducirErrores(mensaje);
  }

  validar(p: Plantilla): void {
    this.api.validar(p.especieId, this.valores()).subscribe({
      next: (r) => this.resultado.set(r),
      error: (err: HttpErrorResponse) => this.resultado.set({ valido: false, errores: [mensajeDeError(err)] })
    });
  }

  guardar(p: Plantilla): void {
    const esquema = this.editor?.revisar();
    if (!esquema) {
      this.errorEsquema.set('Hay datos por corregir. Revisa los avisos en rojo.');
      return;
    }
    this.guardando.set(true);
    this.errorEsquema.set(null);

    this.api.editar(p.id, esquema).subscribe({
      next: (nueva) => {
        this.guardando.set(false);
        this.mostrar(nueva);
        this.aviso.set(`Listo: se guardó la versión ${nueva.version} y ya está activa.`);
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
    if (a.tipo === 'BOOLEANO') {
      return 'Sí o No';
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
  }
}
