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
import { Hallazgo, HallazgosService } from './hallazgos.service';

/**
 * HU-01: registrar un hallazgo con sus datos básicos y los atributos de su especie (plantilla activa, HU-02).
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
                <input id="reg-region" formControlName="region" maxlength="120" class="campo" />
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
                  (valoresChange)="valoresRegistro.set($event)"
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
                  </tr>
                }
              </tbody>
            </table>
          </div>
        }
      </section>

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
      error: (err: HttpErrorResponse) => this.errorRegistro.set(mensajeDeError(err))
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
          this.errorRegistro.set(mensajeDeError(err));
        }
      });
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
