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
import { Hallazgo, HallazgosService, VersionHallazgo } from './hallazgos.service';

/**
 * HU-01: registrar un hallazgo con sus datos básicos y los atributos de su especie (plantilla activa, HU-02).
 * HU-08: editar un hallazgo con un motivo obligatorio (crea una versión nueva).
 * HU-10, HU-11 y HU-12: ver el historial de versiones con autor, motivo y qué cambió.
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
                @for (h of hallazgos(); track h.id) {
                  <tr>
                    <td class="font-semibold">{{ h.id }}</td>
                    <td>{{ nombreDe(h.especieId) }}</td>
                    <td>{{ h.region }}</td>
                    <td>{{ h.fechaObservacion | date: 'dd/MM/yyyy' }}</td>
                    <td><span class="pildora bg-liquen text-musgo">v{{ h.version }}</span></td>
                    <td>{{ h.autor ?? '—' }}</td>
                    <td class="whitespace-nowrap text-right">
                      <button type="button" class="btn-texto" (click)="verHistorial(h)">Historial</button>
                      @if (puedeEscribir()) {
                        <button type="button" class="btn-texto" (click)="abrirEdicion(h)">Editar</button>
                      }
                    </td>
                  </tr>
                }
              </tbody>
            </table>
          </div>
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
                    {{ v.region }} · observado el {{ v.fechaObservacion | date: 'dd/MM/yyyy' }}
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

  // --- historial (HU-10, HU-11 y HU-12) ---
  historialDe = signal<Hallazgo | null>(null);
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

  // ----- historial -----
  verHistorial(h: Hallazgo): void {
    this.historialDe.set(h);
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
