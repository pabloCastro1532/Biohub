import { DatePipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, computed, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { AuthService } from '../../core/auth.service';
import { mensajeDeError } from '../../core/errores';
import { Equipo, EquiposService } from './equipos.service';
import { Prestamo, PrestamosService } from './prestamos.service';

/**
 * HU-04 (prestar un equipo a un grupo), HU-05 (un equipo no se presta dos veces a la vez)
 * y HU-24 (historial de préstamos de un equipo).
 * Va dentro de la pantalla de equipos. Prestar y devolver: administrador y director; el investigador solo consulta.
 */
@Component({
  selector: 'app-prestamos',
  standalone: true,
  imports: [ReactiveFormsModule, DatePipe],
  template: `
    <h2 class="text-xl font-semibold tracking-tight">Préstamos a grupos</h2>
    <p class="mt-1 text-musgo">Qué equipo tiene cada grupo. Un equipo no se puede prestar mientras siga prestado.</p>

    <div class="mt-6 space-y-6">
      @if (aviso()) {
        <p class="aviso-ok" role="status">{{ aviso() }}</p>
      }

      @if (puedeGestionar()) {
        <section class="tarjeta">
          <h3 class="text-lg font-semibold">Prestar un equipo</h3>
          <form [formGroup]="form" (ngSubmit)="prestar()" class="mt-4 grid gap-4 md:grid-cols-3">
            <div>
              <label for="pre-equipo" class="etiqueta">Equipo</label>
              <select id="pre-equipo" formControlName="equipoId" class="campo">
                <option [ngValue]="null">Elige un equipo</option>
                @for (e of equiposActivos(); track e.id) {
                  <option [ngValue]="e.id">{{ e.codigo }} · {{ e.nombre }}</option>
                }
              </select>
            </div>
            <div>
              <label for="pre-grupo" class="etiqueta">Grupo</label>
              <input id="pre-grupo" formControlName="grupo" maxlength="120" class="campo" />
            </div>
            <div>
              <label for="pre-obs" class="etiqueta">Observación (opcional)</label>
              <input id="pre-obs" formControlName="observacion" maxlength="400" class="campo" />
            </div>
            <div class="flex flex-wrap items-center gap-4 md:col-span-3">
              <button type="submit" [disabled]="form.invalid || guardando()" class="btn-primario">
                {{ guardando() ? 'Guardando...' : 'Prestar equipo' }}
              </button>
              @if (errorForm()) {
                <p class="text-sm text-red-700" role="alert">{{ errorForm() }}</p>
              }
            </div>
          </form>
        </section>
      }

      <section class="tarjeta !p-0 overflow-hidden">
        <div class="flex flex-wrap items-center justify-between gap-3 px-6 pb-2 pt-6">
          <h3 class="text-lg font-semibold">Registro de préstamos</h3>
          <label class="flex items-center gap-2 text-sm">
            <input
              type="checkbox"
              [checked]="soloAbiertos()"
              (change)="cambiarFiltro($event)"
              class="h-4 w-4 rounded border-slate-300 accent-bio-600"
            />
            Solo los que siguen prestados
          </label>
        </div>

        @if (cargando()) {
          <p class="px-6 pb-6 text-musgo">Cargando préstamos...</p>
        } @else if (errorCarga()) {
          <p class="mx-6 mb-6 aviso-error" role="alert">{{ errorCarga() }}</p>
        } @else if (prestamos().length === 0) {
          <p class="px-6 pb-6 text-musgo">
            @if (soloAbiertos()) { No hay equipos prestados ahora. } @else { Todavía no se ha prestado ningún equipo. }
          </p>
        } @else {
          <div class="overflow-x-auto px-3 pb-3">
            <table class="tabla">
              <thead>
                <tr>
                  <th>Equipo</th>
                  <th>Grupo</th>
                  <th>Prestado</th>
                  <th>Estado</th>
                  <th><span class="sr-only">Acciones</span></th>
                </tr>
              </thead>
              <tbody>
                @for (p of prestamos(); track p.id) {
                  <tr>
                    <td class="font-semibold">{{ p.equipoCodigo ?? 'Equipo ' + p.equipoId }}</td>
                    <td>
                      {{ p.grupo }}
                      @if (p.observacion) {
                        <span class="block text-xs text-musgo">{{ p.observacion }}</span>
                      }
                    </td>
                    <td>
                      {{ p.prestadoEn | date: 'dd/MM/yyyy HH:mm' }}
                      <span class="block text-xs text-musgo">por {{ p.prestadoPor }}</span>
                    </td>
                    <td>
                      @if (p.devueltoEn) {
                        <span class="pildora bg-liquen text-musgo">Devuelto</span>
                        <span class="block text-xs text-musgo">{{ p.devueltoEn | date: 'dd/MM/yyyy HH:mm' }}</span>
                      } @else {
                        <span class="pildora bg-ambar-50 text-ambar-700">Prestado</span>
                      }
                    </td>
                    <td class="text-right">
                      <div class="flex flex-wrap justify-end gap-2">
                        <button
                          type="button"
                          (click)="verHistorial(p)"
                          class="btn-secundario !px-3 !py-1.5 text-sm"
                          [attr.aria-label]="'Ver historial del equipo ' + (p.equipoCodigo ?? p.equipoId)"
                        >
                          Ver historial
                        </button>
                        @if (puedeGestionar() && !p.devueltoEn) {
                          <button
                            type="button"
                            (click)="devolver(p)"
                            [disabled]="devolviendo() === p.id"
                            class="btn-secundario !px-3 !py-1.5 text-sm"
                          >
                            Registrar devolución
                          </button>
                        }
                      </div>
                    </td>
                  </tr>
                }
              </tbody>
            </table>
          </div>
        }
        @if (errorTabla()) {
          <p class="mx-6 mb-6 aviso-error" role="alert">{{ errorTabla() }}</p>
        }
      </section>

      @if (equipoHistorial(); as eq) {
        <section class="tarjeta" aria-labelledby="hist-titulo">
          <div class="flex flex-wrap items-start justify-between gap-3">
            <div>
              <h3 id="hist-titulo" class="text-lg font-semibold">Historial del equipo {{ eq.codigo }}</h3>
              @if (!cargandoHistorial() && !errorHistorial()) {
                <p class="text-sm text-musgo">
                  @if (historial().length === 0) {
                    Este equipo nunca se ha prestado.
                  } @else if (historial().length === 1) {
                    1 préstamo en total.
                  } @else {
                    {{ historial().length }} préstamos en total, del más reciente al más antiguo.
                  }
                </p>
              }
            </div>
            <button type="button" (click)="cerrarHistorial()" class="btn-texto">Cerrar</button>
          </div>

          @if (cargandoHistorial()) {
            <p class="mt-4 text-musgo">Cargando historial...</p>
          } @else if (errorHistorial()) {
            <p class="mt-4 aviso-error" role="alert">{{ errorHistorial() }}</p>
          } @else if (historial().length > 0) {
            <div class="mt-4 overflow-x-auto">
              <table class="tabla">
                <thead>
                  <tr>
                    <th>Grupo</th>
                    <th>Prestado</th>
                    <th>Devuelto</th>
                  </tr>
                </thead>
                <tbody>
                  @for (h of historial(); track h.id) {
                    <tr>
                      <td>
                        {{ h.grupo }}
                        @if (h.observacion) {
                          <span class="block text-xs text-musgo">{{ h.observacion }}</span>
                        }
                      </td>
                      <td>
                        {{ h.prestadoEn | date: 'dd/MM/yyyy HH:mm' }}
                        <span class="block text-xs text-musgo">por {{ h.prestadoPor }}</span>
                      </td>
                      <td>
                        @if (h.devueltoEn) {
                          {{ h.devueltoEn | date: 'dd/MM/yyyy HH:mm' }}
                          <span class="block text-xs text-musgo">por {{ h.devueltoPor }}</span>
                        } @else {
                          <span class="pildora bg-ambar-50 text-ambar-700">Sigue prestado</span>
                        }
                      </td>
                    </tr>
                  }
                </tbody>
              </table>
            </div>
          }
        </section>
      }
    </div>
  `
})
export class PrestamosComponent {
  private api = inject(PrestamosService);
  private equiposApi = inject(EquiposService);
  private fb = inject(FormBuilder);
  private auth = inject(AuthService);

  equipos = signal<Equipo[]>([]);
  equiposActivos = computed(() => this.equipos().filter((e) => e.activo));
  prestamos = signal<Prestamo[]>([]);
  soloAbiertos = signal(false);
  cargando = signal(true);
  errorCarga = signal<string | null>(null);
  errorTabla = signal<string | null>(null);
  aviso = signal<string | null>(null);

  guardando = signal(false);
  errorForm = signal<string | null>(null);
  devolviendo = signal<number | null>(null);

  /** HU-24: equipo cuyo historial se está viendo. */
  equipoHistorial = signal<{ id: number; codigo: string } | null>(null);
  historial = signal<Prestamo[]>([]);
  cargandoHistorial = signal(false);
  errorHistorial = signal<string | null>(null);

  /** Prestar y devolver: administrador y director (igual que el backend). */
  puedeGestionar = computed(() => this.auth.tieneRol('ADMINISTRADOR', 'DIRECTOR'));

  form = this.fb.group({
    equipoId: this.fb.control<number | null>(null, Validators.required),
    grupo: this.fb.nonNullable.control('', [Validators.required, Validators.maxLength(120)]),
    observacion: this.fb.nonNullable.control('', Validators.maxLength(400))
  });

  constructor() {
    this.equiposApi.listar().subscribe({
      next: (e) => this.equipos.set(e),
      error: () => this.equipos.set([])
    });
    this.cargar();
  }

  cambiarFiltro(ev: Event): void {
    this.soloAbiertos.set((ev.target as HTMLInputElement).checked);
    this.cargar();
  }

  verHistorial(p: Prestamo): void {
    this.equipoHistorial.set({ id: p.equipoId, codigo: p.equipoCodigo ?? 'Equipo ' + p.equipoId });
    this.cargarHistorial(p.equipoId);
  }

  cerrarHistorial(): void {
    this.equipoHistorial.set(null);
    this.historial.set([]);
    this.errorHistorial.set(null);
  }

  prestar(): void {
    if (this.form.invalid) {
      return;
    }
    const v = this.form.getRawValue();
    this.guardando.set(true);
    this.errorForm.set(null);
    this.aviso.set(null);

    this.api
      .prestar({ equipoId: v.equipoId as number, grupo: v.grupo, observacion: v.observacion.trim() || null })
      .subscribe({
        next: (p) => {
          this.guardando.set(false);
          this.aviso.set(`El equipo ${p.equipoCodigo} quedó prestado al grupo ${p.grupo}.`);
          this.form.reset();
          this.cargar();
          this.refrescarHistorialSiAplica(p.equipoId);
        },
        error: (err: HttpErrorResponse) => {
          this.guardando.set(false);
          // HU-05: si el equipo ya está prestado, el mensaje dice a qué grupo
          this.errorForm.set(mensajeDeError(err));
        }
      });
  }

  devolver(p: Prestamo): void {
    this.devolviendo.set(p.id);
    this.errorTabla.set(null);
    this.aviso.set(null);

    this.api.devolver(p.id).subscribe({
      next: (r) => {
        this.devolviendo.set(null);
        this.aviso.set(`El equipo ${r.equipoCodigo} fue devuelto por el grupo ${r.grupo}.`);
        this.cargar();
        this.refrescarHistorialSiAplica(r.equipoId);
      },
      error: (err: HttpErrorResponse) => {
        this.devolviendo.set(null);
        this.errorTabla.set(mensajeDeError(err));
        this.cargar();
      }
    });
  }

  private refrescarHistorialSiAplica(equipoId: number): void {
    if (this.equipoHistorial()?.id === equipoId) {
      this.cargarHistorial(equipoId);
    }
  }

  private cargarHistorial(equipoId: number): void {
    this.cargandoHistorial.set(true);
    this.errorHistorial.set(null);
    this.api.historialDeEquipo(equipoId).subscribe({
      next: (lista) => {
        this.historial.set(lista);
        this.cargandoHistorial.set(false);
      },
      error: (err: HttpErrorResponse) => {
        this.historial.set([]);
        this.errorHistorial.set(mensajeDeError(err));
        this.cargandoHistorial.set(false);
      }
    });
  }

  private cargar(): void {
    this.cargando.set(true);
    this.api.listar(this.soloAbiertos()).subscribe({
      next: (lista) => {
        this.prestamos.set(lista);
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
