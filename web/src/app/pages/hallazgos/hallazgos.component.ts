import { DatePipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, computed, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { forkJoin, of } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { AuthService } from '../../core/auth.service';
import { mensajeDeError } from '../../core/errores';
import { AtributoVista, Especie, PlantillasService } from '../plantillas/plantillas.service';
import { AtributosFormComponent } from './atributos-form.component';
import { REGIONES, etiquetaAtributo, traducirErrores, valorLegible } from './etiquetas-atributos';
import { Hallazgo, HallazgosService, ImagenHallazgo, VersionHallazgo } from './hallazgos.service';

/**
 * HU-01: registrar un hallazgo con sus datos básicos y los atributos de su especie (plantilla activa, HU-02).
 * HU-08: editar un hallazgo con un motivo obligatorio (crea una versión nueva).
 * HU-09: reclasificar un hallazgo (cambiarle la especie) con un motivo obligatorio; solo el director.
 * HU-10, HU-11 y HU-12: ver el historial de versiones con autor, motivo y qué cambió.
 * Imágenes: subir fotos JPG o PNG a un hallazgo (investigador y director) y verlas (también el auditor).
 * La ubicación exacta nunca se muestra aquí (ASR-4): la lista solo trae la región.
 */
@Component({
  selector: 'app-hallazgos',
  standalone: true,
  imports: [ReactiveFormsModule, DatePipe, AtributosFormComponent],
  template: `
    <header class="mb-8">
      <h1 class="text-2xl font-semibold tracking-tight">Hallazgos</h1>
      <p class="mt-1 text-musgo">Avistamientos registrados por el equipo.</p>
    </header>

    <div class="space-y-6">
      @if (aviso()) {
        <p class="aviso-ok" role="status">{{ aviso() }}</p>
      }

      @if (puedeEscribir()) {
        <section class="tarjeta">
          <h2 class="text-lg font-semibold">Registrar hallazgo</h2>
          <form [formGroup]="formRegistro" (ngSubmit)="registrar()" class="mt-4 space-y-4">
            <div class="grid gap-4 md:grid-cols-2">
              <div>
                <label for="reg-especie" class="etiqueta">Especie</label>
                <select id="reg-especie" formControlName="especieId" (change)="elegirEspecie()" class="campo">
                  <option [ngValue]="null">Elige una especie</option>
                  @for (e of especies(); track e.id) {
                    <option [ngValue]="e.id">{{ nombreDe(e.id) }}</option>
                  }
                </select>
              </div>
              <div>
                <label for="reg-region" class="etiqueta">Región</label>
                <select id="reg-region" formControlName="region" class="campo">
                  <option value="">Elige una región</option>
                  @for (r of regiones(); track r) {
                    <option [value]="r">{{ r }}</option>
                  }
                </select>
              </div>
              <div>
                <label for="reg-fecha" class="etiqueta">Fecha de observación</label>
                <input id="reg-fecha" type="date" formControlName="fechaObservacion" [max]="hoy" class="campo" />
              </div>
              <div class="grid grid-cols-2 gap-3">
                <div>
                  <label for="reg-lat" class="etiqueta">Latitud</label>
                  <input id="reg-lat" type="number" step="any" formControlName="latitud" class="campo" />
                </div>
                <div>
                  <label for="reg-lon" class="etiqueta">Longitud</label>
                  <input id="reg-lon" type="number" step="any" formControlName="longitud" class="campo" />
                </div>
              </div>
            </div>
            @if (formRegistro.controls.latitud.invalid && formRegistro.controls.latitud.touched) {
              <p class="text-sm text-red-700" role="alert">La latitud debe estar entre -90 y 90.</p>
            }
            @if (formRegistro.controls.longitud.invalid && formRegistro.controls.longitud.touched) {
              <p class="text-sm text-red-700" role="alert">La longitud debe estar entre -180 y 180.</p>
            }
            <p class="text-xs text-musgo">
              La ubicación exacta se guarda de forma protegida y no se vuelve a mostrar en esta pantalla; solo la región.
            </p>

            @if (atributosRegistro().length > 0) {
              <div>
                <h3 class="mb-2 text-sm font-medium text-musgo">Atributos de la especie</h3>
                <app-atributos-form
                  [atributos]="atributosRegistro()"
                  [valores]="vacio"
                  prefijo="reg"
                  (valoresChange)="valoresRegistro.set($event); errorRegistro.set(null)"
                />
              </div>
            }

            <div class="flex flex-wrap items-center gap-4">
              <button type="submit" [disabled]="formRegistro.invalid || guardando()" class="btn-primario">
                {{ guardando() ? 'Guardando...' : 'Registrar hallazgo' }}
              </button>
              @if (errorRegistro()) {
                <p class="text-sm text-red-700" role="alert">{{ errorRegistro() }}</p>
              }
            </div>
          </form>
        </section>
      }

      <section class="tarjeta !p-0 overflow-hidden">
        <div class="px-6 pb-2 pt-6">
          <h2 class="text-lg font-semibold">Hallazgos registrados</h2>
        </div>

        @if (cargando()) {
          <p class="px-6 pb-6 text-musgo">Cargando hallazgos...</p>
        } @else if (errorCarga()) {
          <p class="mx-6 mb-6 aviso-error" role="alert">{{ errorCarga() }}</p>
        } @else if (hallazgos().length === 0) {
          <p class="px-6 pb-6 text-musgo">
            Todavía no hay hallazgos.@if (puedeEscribir()) { Registra el primero con el formulario de arriba.}
          </p>
        } @else {
          <div class="overflow-x-auto px-3 pb-3">
            <table class="tabla">
              <thead>
                <tr>
                  <th>N.º</th>
                  <th>Especie</th>
                  <th>Región</th>
                  <th>Observado el</th>
                  <th>Versión</th>
                  <th>Registrado por</th>
                  <th><span class="sr-only">Acciones</span></th>
                </tr>
              </thead>
              <tbody>
                @for (h of visibles(); track h.id) {
                  <tr>
                    <td class="font-semibold">{{ h.id }}</td>
                    <td>{{ nombreDe(h.especieId) }}</td>
                    <td>{{ h.region }}</td>
                    <td>{{ h.fechaObservacion | date: 'dd/MM/yyyy' }}</td>
                    <td><span class="pildora bg-liquen text-musgo">v{{ h.version }}</span></td>
                    <td>{{ h.autor ?? '—' }}</td>
                    <td class="whitespace-nowrap text-right">
                      <button type="button" class="btn-texto" (click)="verHistorial(h)">Historial</button>
                      <button type="button" class="btn-texto" (click)="abrirImagenes(h)">Imágenes</button>
                      @if (puedeEscribir()) {
                        <button type="button" class="btn-texto" (click)="abrirEdicion(h)">Editar</button>
                      }
                      @if (puedeReclasificar()) {
                        <button type="button" class="btn-texto" (click)="abrirReclasificacion(h)">Reclasificar</button>
                      }
                    </td>
                  </tr>
                }
              </tbody>
            </table>
          </div>
          @if (totalPaginas() > 1) {
            <nav class="flex flex-wrap items-center justify-between gap-3 px-6 pb-6" aria-label="Páginas de hallazgos">
              <p class="text-sm text-musgo" role="status">
                Mostrando {{ desde() }} a {{ hasta() }} de {{ hallazgos().length }} hallazgos, los más recientes primero.
              </p>
              <div class="flex items-center gap-2">
                <button type="button" class="btn-secundario" (click)="irAPagina(pagina() - 1)" [disabled]="pagina() === 0">
                  Anterior
                </button>
                <span class="text-sm text-musgo">Página {{ pagina() + 1 }} de {{ totalPaginas() }}</span>
                <button
                  type="button"
                  class="btn-secundario"
                  (click)="irAPagina(pagina() + 1)"
                  [disabled]="pagina() >= totalPaginas() - 1"
                >
                  Siguiente
                </button>
              </div>
            </nav>
          }
        }
      </section>

      @if (editando(); as h) {
        <section class="tarjeta" aria-labelledby="titulo-edicion">
          <div class="flex items-start justify-between gap-4">
            <div>
              <h2 id="titulo-edicion" class="text-lg font-semibold">Editar hallazgo {{ h.id }}</h2>
              <p class="mt-1 text-sm text-musgo">
                Guardar crea la versión {{ h.version + 1 }}; la anterior no se borra. La especie no cambia.
              </p>
            </div>
            <button type="button" class="btn-texto" (click)="cerrarEdicion()">Cancelar</button>
          </div>

          <form [formGroup]="formEdicion" (ngSubmit)="guardarEdicion()" class="mt-4 space-y-4">
            <div class="grid gap-4 md:grid-cols-2">
              <div>
                <label for="ed-region" class="etiqueta">Región</label>
                <select id="ed-region" formControlName="region" class="campo">
                  <option value="">Elige una región</option>
                  @for (r of regiones(); track r) {
                    <option [value]="r">{{ r }}</option>
                  }
                </select>
              </div>
              <div>
                <label for="ed-fecha" class="etiqueta">Fecha de observación</label>
                <input id="ed-fecha" type="date" formControlName="fechaObservacion" [max]="hoy" class="campo" />
              </div>
              <div>
                <label for="ed-lat" class="etiqueta">Latitud</label>
                <input id="ed-lat" type="number" step="any" formControlName="latitud" class="campo" />
              </div>
              <div>
                <label for="ed-lon" class="etiqueta">Longitud</label>
                <input id="ed-lon" type="number" step="any" formControlName="longitud" class="campo" />
              </div>
            </div>
            @if (formEdicion.controls.latitud.invalid && formEdicion.controls.latitud.touched) {
              <p class="text-sm text-red-700" role="alert">La latitud debe estar entre -90 y 90.</p>
            }
            @if (formEdicion.controls.longitud.invalid && formEdicion.controls.longitud.touched) {
              <p class="text-sm text-red-700" role="alert">La longitud debe estar entre -180 y 180.</p>
            }
            <p class="text-xs text-musgo">
              Por seguridad la ubicación exacta no se muestra aquí. Escríbela de nuevo; si no cambió, repite la que ya registraste.
            </p>

            @if (atributosEdicion().length > 0) {
              <div>
                <h3 class="mb-2 text-sm font-medium text-musgo">Atributos de la especie</h3>
                <app-atributos-form
                  [atributos]="atributosEdicion()"
                  [valores]="valoresInicialesEdicion()"
                  prefijo="ed"
                  (valoresChange)="valoresEdicion.set($event); errorEdicion.set(null)"
                />
              </div>
            }

            <div>
              <label for="ed-motivo" class="etiqueta">Motivo del cambio</label>
              <textarea id="ed-motivo" formControlName="motivo" rows="3" maxlength="500" class="campo"></textarea>
              <p class="mt-1 text-xs text-musgo">Obligatorio. Queda guardado en el historial junto con tu nombre.</p>
            </div>

            <div class="flex flex-wrap items-center gap-4">
              <button type="submit" [disabled]="formEdicion.invalid || guardandoEdicion()" class="btn-primario">
                {{ guardandoEdicion() ? 'Guardando...' : 'Guardar cambios' }}
              </button>
              @if (errorEdicion()) {
                <p class="text-sm text-red-700" role="alert">{{ errorEdicion() }}</p>
              }
            </div>
          </form>
        </section>
      }

      @if (reclasificando(); as h) {
        <section class="tarjeta" aria-labelledby="titulo-reclasificacion">
          <div class="flex items-start justify-between gap-4">
            <div>
              <h2 id="titulo-reclasificacion" class="text-lg font-semibold">Reclasificar hallazgo {{ h.id }}</h2>
              <p class="mt-1 text-sm text-musgo">
                Ahora figura como <span class="font-medium text-slate-900">{{ nombreDe(h.especieId) }}</span>. Guardar crea la
                versión {{ h.version + 1 }} con la especie nueva; la anterior no se borra. La región, la fecha y la ubicación se conservan.
              </p>
            </div>
            <button type="button" class="btn-texto" (click)="cerrarReclasificacion()">Cancelar</button>
          </div>

          <form [formGroup]="formReclasificacion" (ngSubmit)="guardarReclasificacion()" class="mt-4 space-y-4">
            <div class="md:w-1/2">
              <label for="rec-especie" class="etiqueta">Especie nueva</label>
              <select id="rec-especie" formControlName="especieId" (change)="elegirEspecieNueva()" class="campo">
                <option [ngValue]="null">Elige la especie correcta</option>
                @for (e of especiesParaReclasificar(); track e.id) {
                  <option [ngValue]="e.id">{{ nombreDe(e.id) }}</option>
                }
              </select>
            </div>

            @if (atributosReclasificacion().length > 0) {
              <div>
                <h3 class="mb-2 text-sm font-medium text-musgo">Atributos de la especie nueva</h3>
                <app-atributos-form
                  [atributos]="atributosReclasificacion()"
                  [valores]="valoresInicialesReclasificacion()"
                  prefijo="rec"
                  (valoresChange)="valoresReclasificacion.set($event); errorReclasificacion.set(null)"
                />
              </div>
            }

            <div>
              <label for="rec-motivo" class="etiqueta">Motivo de la reclasificación</label>
              <textarea id="rec-motivo" formControlName="motivo" rows="3" maxlength="500" class="campo"></textarea>
              <p class="mt-1 text-xs text-musgo">Obligatorio. Queda guardado en el historial junto con tu nombre.</p>
            </div>

            <div class="flex flex-wrap items-center gap-4">
              <button type="submit" [disabled]="formReclasificacion.invalid || guardandoReclasificacion()" class="btn-primario">
                {{ guardandoReclasificacion() ? 'Guardando...' : 'Reclasificar hallazgo' }}
              </button>
              @if (errorReclasificacion()) {
                <p class="text-sm text-red-700" role="alert">{{ errorReclasificacion() }}</p>
              }
            </div>
          </form>
        </section>
      }

      @if (imagenesDe(); as h) {
        <section class="tarjeta" aria-labelledby="titulo-imagenes">
          <div class="flex items-start justify-between gap-4">
            <div>
              <h2 id="titulo-imagenes" class="text-lg font-semibold">Imágenes del hallazgo {{ h.id }}</h2>
              <p class="mt-1 text-sm text-musgo">{{ nombreDe(h.especieId) }}. Hasta 5 imágenes por hallazgo.</p>
            </div>
            <button type="button" class="btn-texto" (click)="cerrarImagenes()">Cerrar</button>
          </div>

          @if (puedeEscribir()) {
            <div class="mt-4">
              <label for="archivo-imagen" class="etiqueta">Agregar una imagen (JPG o PNG, hasta 2 MB)</label>
              <input
                id="archivo-imagen"
                type="file"
                accept="image/jpeg,image/png"
                class="campo"
                [disabled]="subiendoImagen()"
                (change)="elegirImagen($event)"
              />
              <p class="mt-1 text-xs text-musgo">
                Se guarda sin los datos de la cámara, así que no lleva la ubicación de la foto. No se puede borrar después.
              </p>
              @if (subiendoImagen()) {
                <p class="mt-2 text-sm text-musgo" role="status">Subiendo imagen...</p>
              }
            </div>
          }
          @if (errorImagenes()) {
            <p class="mt-4 aviso-error" role="alert">{{ errorImagenes() }}</p>
          }

          @if (cargandoImagenes()) {
            <p class="mt-4 text-musgo">Cargando imágenes...</p>
          } @else if (imagenes().length === 0) {
            <p class="mt-4 text-musgo">Este hallazgo todavía no tiene imágenes.</p>
          } @else {
            <ul class="mt-4 grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
              @for (i of imagenes(); track i.id) {
                <li class="rounded-lg border border-linea p-3">
                  @if (i.url) {
                    <img [src]="i.url" [alt]="'Imagen ' + i.nombreArchivo" class="h-40 w-full rounded-md object-cover" />
                  } @else {
                    <div class="flex h-40 items-center justify-center rounded-md bg-liquen text-sm text-musgo">Sin vista previa</div>
                  }
                  <p class="mt-2 truncate text-sm font-medium">{{ i.nombreArchivo }}</p>
                  <p class="text-xs text-musgo">{{ i.autor ?? 'Autor desconocido' }} · {{ i.creadaEn | date: 'dd/MM/yyyy HH:mm' }}</p>
                </li>
              }
            </ul>
          }
        </section>
      }

      @if (historialDe(); as h) {
        <section class="tarjeta" aria-labelledby="titulo-historial">
          <div class="flex items-start justify-between gap-4">
            <div>
              <h2 id="titulo-historial" class="text-lg font-semibold">Historial del hallazgo {{ h.id }}</h2>
              <p class="mt-1 text-sm text-musgo">{{ nombreDe(h.especieId) }}. Cada versión conserva quién la hizo y por qué.</p>
            </div>
            <button type="button" class="btn-texto" (click)="cerrarHistorial()">Cerrar</button>
          </div>

          @if (cargandoHistorial()) {
            <p class="mt-4 text-musgo">Cargando historial...</p>
          } @else if (errorHistorial()) {
            <p class="mt-4 aviso-error" role="alert">{{ errorHistorial() }}</p>
          } @else {
            <ol class="mt-4 space-y-3">
              @for (v of versiones(); track v.numero) {
                <li class="rounded-lg border border-linea p-4">
                  <div class="flex flex-wrap items-center gap-2">
                    <span class="pildora bg-liquen text-musgo">v{{ v.numero }}</span>
                    <span class="text-sm font-medium">{{ v.autor ?? 'Autor desconocido' }}</span>
                    <span class="text-sm text-musgo">{{ v.creadoEn | date: 'dd/MM/yyyy HH:mm' }}</span>
                  </div>
                  <p class="mt-2 text-sm">
                    <span class="font-medium">Motivo:</span>
                    {{ v.motivo ?? 'Registro inicial' }}
                  </p>
                  @if (v.cambios.length > 0) {
                    <p class="mt-2 flex flex-wrap items-center gap-2 text-sm">
                      <span class="font-medium">Cambió:</span>
                      @for (c of v.cambios; track c) {
                        <span class="pildora bg-bio-50 text-bio-900">{{ c }}</span>
                      }
                    </p>
                  }
                  <p class="mt-2 text-sm text-musgo">
                    {{ nombreDe(v.especieId) }} · {{ v.region }} · observado el {{ v.fechaObservacion | date: 'dd/MM/yyyy' }}
                    @if (resumen(v.atributos); as r) { · {{ r }} }
                  </p>
                </li>
              }
            </ol>
          }
        </section>
      }

    </div>
  `
})
export class HallazgosComponent {
  private api = inject(HallazgosService);
  private plantillas = inject(PlantillasService);
  private fb = inject(FormBuilder);
  private auth = inject(AuthService);

  readonly hoy = new Date().toISOString().slice(0, 10);
  /** Valores iniciales del formulario de registro: ninguno. */
  readonly vacio: Record<string, unknown> = {};

  hallazgos = signal<Hallazgo[]>([]);

  // --- tabla paginada: con muchos hallazgos los paneles de editar y de historial quedaban al final de la página ---
  readonly porPagina = 10;
  pagina = signal(0);
  totalPaginas = computed(() => Math.max(1, Math.ceil(this.hallazgos().length / this.porPagina)));
  /** Los más recientes primero, de a 10. */
  visibles = computed(() => {
    const ordenados = [...this.hallazgos()].sort((a, b) => b.id - a.id);
    const inicio = Math.min(this.pagina(), this.totalPaginas() - 1) * this.porPagina;
    return ordenados.slice(inicio, inicio + this.porPagina);
  });
  desde = computed(() => (this.hallazgos().length === 0 ? 0 : Math.min(this.pagina(), this.totalPaginas() - 1) * this.porPagina + 1));
  hasta = computed(() => Math.min(this.desde() + this.porPagina - 1, this.hallazgos().length));
  especies = signal<Especie[]>([]);
  cargando = signal(true);
  errorCarga = signal<string | null>(null);
  aviso = signal<string | null>(null);

  /** La lista fija más cualquier región que ya exista en un hallazgo (por si se registró antes de tener la lista). */
  regiones = computed(() =>
    [...new Set([...REGIONES, ...this.hallazgos().map((h) => h.region.trim()).filter((r) => r.length > 0)])].sort(
      (x, y) => x.localeCompare(y, 'es')
    )
  );

  /** Registrar: investigador y director (igual que el backend). El auditor solo consulta. */
  puedeEscribir = computed(() => this.auth.tieneRol('INVESTIGADOR', 'DIRECTOR'));
  /** Reclasificar: solo el director científico (igual que el backend). */
  puedeReclasificar = computed(() => this.auth.tieneRol('DIRECTOR'));

  // --- registrar ---
  formRegistro = this.fb.group({
    especieId: this.fb.control<number | null>(null, Validators.required),
    region: this.fb.nonNullable.control('', [Validators.required, Validators.maxLength(120)]),
    fechaObservacion: this.fb.nonNullable.control('', Validators.required),
    latitud: this.fb.control<number | null>(null, [Validators.required, Validators.min(-90), Validators.max(90)]),
    longitud: this.fb.control<number | null>(null, [Validators.required, Validators.min(-180), Validators.max(180)])
  });
  atributosRegistro = signal<AtributoVista[]>([]);
  valoresRegistro = signal<Record<string, unknown>>({});
  guardando = signal(false);
  errorRegistro = signal<string | null>(null);

  // --- editar (HU-08) ---
  editando = signal<Hallazgo | null>(null);
  atributosEdicion = signal<AtributoVista[]>([]);
  valoresInicialesEdicion = signal<Record<string, unknown>>({});
  valoresEdicion = signal<Record<string, unknown>>({});
  guardandoEdicion = signal(false);
  errorEdicion = signal<string | null>(null);
  formEdicion = this.fb.group({
    region: this.fb.nonNullable.control('', [Validators.required, Validators.maxLength(120)]),
    fechaObservacion: this.fb.nonNullable.control('', Validators.required),
    latitud: this.fb.control<number | null>(null, [Validators.required, Validators.min(-90), Validators.max(90)]),
    longitud: this.fb.control<number | null>(null, [Validators.required, Validators.min(-180), Validators.max(180)]),
    motivo: this.fb.nonNullable.control('', [Validators.required, Validators.maxLength(500)])
  });

  // --- reclasificar (HU-09) ---
  reclasificando = signal<Hallazgo | null>(null);
  atributosReclasificacion = signal<AtributoVista[]>([]);
  valoresInicialesReclasificacion = signal<Record<string, unknown>>({});
  valoresReclasificacion = signal<Record<string, unknown>>({});
  guardandoReclasificacion = signal(false);
  errorReclasificacion = signal<string | null>(null);
  formReclasificacion = this.fb.group({
    especieId: this.fb.control<number | null>(null, Validators.required),
    motivo: this.fb.nonNullable.control('', [Validators.required, Validators.maxLength(500)])
  });
  /** Todas las especies menos la actual del hallazgo. */
  especiesParaReclasificar = computed(() => {
    const h = this.reclasificando();
    return this.especies().filter((e) => h === null || e.id !== h.especieId);
  });

  // --- historial (HU-10, HU-11 y HU-12) ---
  historialDe = signal<Hallazgo | null>(null);

  // --- imágenes ---
  imagenesDe = signal<Hallazgo | null>(null);
  imagenes = signal<(ImagenHallazgo & { url: string | null })[]>([]);
  cargandoImagenes = signal(false);
  subiendoImagen = signal(false);
  errorImagenes = signal<string | null>(null);
  versiones = signal<VersionHallazgo[]>([]);
  cargandoHistorial = signal(false);
  errorHistorial = signal<string | null>(null);

  constructor() {
    this.cargar();
  }

  nombreDe(especieId: number): string {
    const e = this.especies().find((x) => x.id === especieId);
    return e ? (e.nombreComun ?? e.nombreCientifico) : `Especie ${especieId}`;
  }

  // ----- registrar -----
  elegirEspecie(): void {
    const id = this.formRegistro.controls.especieId.value;
    this.valoresRegistro.set({});
    this.atributosRegistro.set([]);
    if (id === null) {
      return;
    }
    this.plantillas.activaDe(id).subscribe({
      next: (p) => this.atributosRegistro.set(p.atributos),
      error: (err: HttpErrorResponse) => this.errorRegistro.set(traducirErrores(mensajeDeError(err)))
    });
  }

  registrar(): void {
    if (this.formRegistro.invalid) {
      return;
    }
    const v = this.formRegistro.getRawValue();
    this.guardando.set(true);
    this.errorRegistro.set(null);
    this.aviso.set(null);

    this.api
      .registrar({
        especieId: v.especieId as number,
        region: v.region,
        latitud: v.latitud as number,
        longitud: v.longitud as number,
        fechaObservacion: v.fechaObservacion,
        atributos: this.valoresRegistro()
      })
      .subscribe({
        next: (h) => {
          this.guardando.set(false);
          this.aviso.set(`Hallazgo ${h.id} registrado (versión ${h.version}).`);
          this.formRegistro.reset();
          this.atributosRegistro.set([]);
          this.valoresRegistro.set({});
          this.cargar();
        },
        error: (err: HttpErrorResponse) => {
          this.guardando.set(false);
          this.errorRegistro.set(traducirErrores(mensajeDeError(err)));
        }
      });
  }

  irAPagina(n: number): void {
    this.pagina.set(Math.max(0, Math.min(n, this.totalPaginas() - 1)));
  }

  /** Lleva la vista al panel que se acaba de abrir: está debajo de la tabla y si no, parece que el botón no hizo nada. */
  private llevarA(idTitulo: string): void {
    setTimeout(() => document.getElementById(idTitulo)?.scrollIntoView({ behavior: 'smooth', block: 'start' }), 60);
  }

  // ----- editar -----
  abrirEdicion(h: Hallazgo): void {
    this.aviso.set(null);
    this.errorEdicion.set(null);
    this.editando.set(h);
    this.formEdicion.reset({
      region: h.region,
      fechaObservacion: h.fechaObservacion,
      latitud: null,
      longitud: null,
      motivo: ''
    });
    this.llevarA('titulo-edicion');
    this.valoresInicialesEdicion.set({ ...h.atributos });
    this.valoresEdicion.set({ ...h.atributos });
    this.atributosEdicion.set([]);
    this.plantillas.activaDe(h.especieId).subscribe({
      next: (p) => this.atributosEdicion.set(p.atributos),
      error: (err: HttpErrorResponse) => this.errorEdicion.set(traducirErrores(mensajeDeError(err)))
    });
  }

  cerrarEdicion(): void {
    this.editando.set(null);
    this.errorEdicion.set(null);
  }

  guardarEdicion(): void {
    const h = this.editando();
    if (h === null || this.formEdicion.invalid) {
      return;
    }
    const v = this.formEdicion.getRawValue();
    this.guardandoEdicion.set(true);
    this.errorEdicion.set(null);

    this.api
      .editar(h.id, {
        region: v.region,
        fechaObservacion: v.fechaObservacion,
        latitud: v.latitud as number,
        longitud: v.longitud as number,
        atributos: this.valoresEdicion(),
        motivo: v.motivo
      })
      .subscribe({
        next: (nuevo) => {
          this.guardandoEdicion.set(false);
          this.aviso.set(`Hallazgo ${nuevo.id} actualizado (versión ${nuevo.version}).`);
          this.cerrarEdicion();
          this.cargar();
          this.verHistorial(nuevo);
        },
        error: (err: HttpErrorResponse) => {
          this.guardandoEdicion.set(false);
          this.errorEdicion.set(traducirErrores(mensajeDeError(err)));
        }
      });
  }

  // ----- reclasificar (HU-09) -----
  abrirReclasificacion(h: Hallazgo): void {
    this.aviso.set(null);
    this.errorReclasificacion.set(null);
    this.reclasificando.set(h);
    this.formReclasificacion.reset({ especieId: null, motivo: '' });
    this.atributosReclasificacion.set([]);
    this.valoresInicialesReclasificacion.set({});
    this.valoresReclasificacion.set({});
    this.llevarA('titulo-reclasificacion');
  }

  cerrarReclasificacion(): void {
    this.reclasificando.set(null);
    this.errorReclasificacion.set(null);
  }

  /** Al elegir la especie nueva se muestran SUS atributos; los que se llaman igual que los actuales llegan ya llenos. */
  elegirEspecieNueva(): void {
    const h = this.reclasificando();
    const id = this.formReclasificacion.controls.especieId.value;
    this.atributosReclasificacion.set([]);
    this.valoresInicialesReclasificacion.set({});
    this.valoresReclasificacion.set({});
    this.errorReclasificacion.set(null);
    if (h === null || id === null) {
      return;
    }
    this.plantillas.activaDe(id).subscribe({
      next: (p) => {
        const nombres = new Set(p.atributos.map((a) => a.nombre));
        const heredados = Object.fromEntries(Object.entries(h.atributos).filter(([k]) => nombres.has(k)));
        this.valoresInicialesReclasificacion.set(heredados);
        this.valoresReclasificacion.set({ ...heredados });
        this.atributosReclasificacion.set(p.atributos);
      },
      error: (err: HttpErrorResponse) => this.errorReclasificacion.set(traducirErrores(mensajeDeError(err)))
    });
  }

  guardarReclasificacion(): void {
    const h = this.reclasificando();
    if (h === null || this.formReclasificacion.invalid) {
      return;
    }
    const v = this.formReclasificacion.getRawValue();
    this.guardandoReclasificacion.set(true);
    this.errorReclasificacion.set(null);

    this.api
      .reclasificar(h.id, { especieId: v.especieId as number, atributos: this.valoresReclasificacion(), motivo: v.motivo })
      .subscribe({
        next: (nuevo) => {
          this.guardandoReclasificacion.set(false);
          this.aviso.set(`Hallazgo ${nuevo.id} reclasificado como ${this.nombreDe(nuevo.especieId)} (versión ${nuevo.version}).`);
          this.cerrarReclasificacion();
          this.cargar();
          this.verHistorial(nuevo);
        },
        error: (err: HttpErrorResponse) => {
          this.guardandoReclasificacion.set(false);
          this.errorReclasificacion.set(traducirErrores(mensajeDeError(err)));
        }
      });
  }

  // ----- imágenes -----
  abrirImagenes(h: Hallazgo): void {
    this.imagenesDe.set(h);
    this.llevarA('titulo-imagenes');
    this.errorImagenes.set(null);
    this.cargarImagenes(h.id);
  }

  cerrarImagenes(): void {
    this.liberarImagenes();
    this.imagenesDe.set(null);
  }

  /** Valida lo básico en el navegador (el servidor lo vuelve a comprobar) y sube la imagen elegida. */
  elegirImagen(evento: Event): void {
    const campo = evento.target as HTMLInputElement;
    const archivo = campo.files?.[0];
    const h = this.imagenesDe();
    if (!archivo || !h) {
      return;
    }
    campo.value = '';
    this.errorImagenes.set(null);
    if (archivo.type !== 'image/jpeg' && archivo.type !== 'image/png') {
      this.errorImagenes.set('Solo se aceptan imágenes JPG o PNG.');
      return;
    }
    if (archivo.size > 2 * 1024 * 1024) {
      this.errorImagenes.set('La imagen no puede pasar de 2 MB.');
      return;
    }
    this.subiendoImagen.set(true);
    this.api.subirImagen(h.id, archivo).subscribe({
      next: () => {
        this.subiendoImagen.set(false);
        this.cargarImagenes(h.id);
      },
      error: (err: HttpErrorResponse) => {
        this.subiendoImagen.set(false);
        this.errorImagenes.set(mensajeDeError(err));
      }
    });
  }

  private cargarImagenes(id: number): void {
    this.liberarImagenes();
    this.cargandoImagenes.set(true);
    this.api.imagenes(id).subscribe({
      next: (lista) => {
        this.imagenes.set(lista.map((i) => ({ ...i, url: null })));
        this.cargandoImagenes.set(false);
        // Cada archivo se pide con el token de sesión y se muestra desde una dirección temporal del navegador.
        for (const i of lista) {
          this.api.verImagen(id, i.id).subscribe({
            next: (blob) => {
              const url = URL.createObjectURL(blob);
              this.imagenes.update((todas) => todas.map((x) => (x.id === i.id ? { ...x, url } : x)));
            },
            error: () => undefined
          });
        }
      },
      error: (err: HttpErrorResponse) => {
        this.errorImagenes.set(mensajeDeError(err));
        this.cargandoImagenes.set(false);
      }
    });
  }

  private liberarImagenes(): void {
    for (const i of this.imagenes()) {
      if (i.url) {
        URL.revokeObjectURL(i.url);
      }
    }
    this.imagenes.set([]);
  }

  // ----- historial -----
  verHistorial(h: Hallazgo): void {
    this.historialDe.set(h);
    this.llevarA('titulo-historial');
    this.versiones.set([]);
    this.errorHistorial.set(null);
    this.cargandoHistorial.set(true);
    this.api.versiones(h.id).subscribe({
      // La más reciente primero: es la que más se consulta.
      next: (lista) => {
        this.versiones.set([...lista].reverse());
        this.cargandoHistorial.set(false);
      },
      error: (err: HttpErrorResponse) => {
        this.errorHistorial.set(mensajeDeError(err));
        this.cargandoHistorial.set(false);
      }
    });
  }

  cerrarHistorial(): void {
    this.historialDe.set(null);
  }

  /** Los atributos de una versión en una línea ("Sexo: macho, Peso (kg): 80"); vacío si no hay. */
  resumen(atributos: Record<string, unknown>): string {
    return Object.entries(atributos ?? {})
      .map(([k, v]) => {
        const nombre = etiquetaAtributo(k);
        // "¿Iba en bandada? No" en vez de "¿Iba en bandada?: No".
        return nombre.endsWith('?') ? `${nombre} ${valorLegible(v)}` : `${nombre}: ${valorLegible(v)}`;
      })
      .join(', ');
  }

  private cargar(): void {
    this.cargando.set(true);
    forkJoin({
      hallazgos: this.api.listar(),
      // Si el rol no puede ver la lista de especies, la pantalla sigue y muestra "Especie N".
      especies: this.plantillas.especies().pipe(catchError(() => of([] as Especie[])))
    }).subscribe({
      next: ({ hallazgos, especies }) => {
        this.hallazgos.set(hallazgos);
        this.especies.set(especies);
        this.errorCarga.set(null);
        this.cargando.set(false);
      },
      error: (err: HttpErrorResponse) => {
        this.errorCarga.set(mensajeDeError(err));
        this.cargando.set(false);
      }
    });
  }
}
