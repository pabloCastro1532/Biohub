import { DatePipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, computed, inject, signal } from '@angular/core';
import { AuthService } from '../../core/auth.service';
import { mensajeDeError } from '../../core/errores';
import { Alerta, Exposicion, SeguridadService, TipoAlerta } from './seguridad.service';

const ETIQUETA_TIPO: Record<TipoAlerta, string> = {
  CONSULTAS_EXCESIVAS: 'Consultas de ubicación excesivas',
  RECHAZOS_REPETIDOS: 'Rechazos repetidos'
};

const DESCRIPCION_TIPO: Record<TipoAlerta, string> = {
  CONSULTAS_EXCESIVAS: 'Más consultas de ubicación exacta de lo normal en una hora.',
  RECHAZOS_REPETIDOS: 'Varias consultas rechazadas seguidas en pocos minutos.'
};

/**
 * HU-26: el responsable de seguridad revisa las alertas del monitor.
 * HU-27: desde cada alerta puede revocar el acceso del usuario o ver qué coordenadas exactas consultó.
 */
@Component({
  selector: 'app-seguridad',
  standalone: true,
  imports: [DatePipe],
  template: `
    <header class="mb-8 flex flex-wrap items-end justify-between gap-4">
      <div>
        <h1 class="text-2xl font-semibold tracking-tight">Alertas de seguridad</h1>
        <p class="mt-1 text-musgo">Patrones anómalos de consulta de ubicaciones detectados por el monitor.</p>
      </div>
      <button type="button" (click)="cargar()" [disabled]="cargando()" class="btn-secundario">
        {{ cargando() ? 'Actualizando...' : 'Actualizar' }}
      </button>
    </header>

    <div class="space-y-6">
      @if (aviso()) {
        <p class="aviso-ok" role="status">{{ aviso() }}</p>
      }
      @if (errorAccion()) {
        <p class="aviso-error" role="alert">{{ errorAccion() }}</p>
      }

      <section class="grid gap-4 md:grid-cols-3" aria-label="Resumen">
        <div class="tarjeta">
          <p class="text-sm text-musgo">Alertas recientes</p>
          <p class="mt-1 text-3xl font-semibold">{{ alertas().length }}</p>
        </div>
        <div class="tarjeta">
          <p class="text-sm text-musgo">Consultas excesivas</p>
          <p class="mt-1 text-3xl font-semibold">{{ conteo('CONSULTAS_EXCESIVAS') }}</p>
        </div>
        <div class="tarjeta">
          <p class="text-sm text-musgo">Rechazos repetidos</p>
          <p class="mt-1 text-3xl font-semibold">{{ conteo('RECHAZOS_REPETIDOS') }}</p>
        </div>
      </section>

      @if (errorCarga()) {
        <p class="aviso-error" role="alert">{{ errorCarga() }}</p>
      }

      <section class="tarjeta !p-0 overflow-hidden">
        <div class="px-6 pb-2 pt-6">
          <h2 class="text-lg font-semibold">Historial de alertas</h2>
          <p class="mt-1 text-sm text-musgo">Las más recientes primero. Las generadas automáticamente no se pueden editar.</p>
        </div>

        @if (cargando() && alertas().length === 0) {
          <p class="px-6 pb-6 text-musgo">Cargando alertas...</p>
        } @else if (alertas().length === 0 && !errorCarga()) {
          <p class="px-6 pb-6 text-musgo">No hay alertas. El monitor no ha detectado patrones anómalos.</p>
        } @else if (alertas().length > 0) {
          <div class="overflow-x-auto px-3 pb-3">
            <table class="tabla">
              <thead>
                <tr>
                  <th>Fecha</th>
                  <th>Usuario</th>
                  <th>Tipo</th>
                  <th class="text-right">Cantidad</th>
                  <th>Ventana</th>
                  <th>Notificada</th>
                  <th>Qué hacer</th>
                </tr>
              </thead>
              <tbody>
                @for (a of alertas(); track a.id) {
                  <tr>
                    <td class="whitespace-nowrap">{{ a.generadaEn | date: 'dd/MM/yyyy HH:mm' }}</td>
                    <td class="font-medium">{{ a.usuario }}</td>
                    <td>
                      <span class="pildora" [class]="claseTipo(a.tipo)">{{ etiqueta(a.tipo) }}</span>
                      <span class="block text-xs text-musgo">{{ descripcion(a.tipo) }}</span>
                    </td>
                    <td class="text-right font-semibold">{{ a.cantidad }}</td>
                    <td class="whitespace-nowrap">{{ a.ventanaMinutos }} min</td>
                    <td>
                      @if (a.enviada) {
                        <span class="pildora bg-bio-100 text-bio-900">Sí</span>
                      } @else {
                        <span class="pildora bg-ambar-50 text-ambar-700">Pendiente</span>
                      }
                    </td>
                    <td class="whitespace-nowrap">
                      <button type="button" class="btn-texto" (click)="verExposicion(a)">Ver coordenadas consultadas</button>
                      @if (esRevocado(a.usuarioId)) {
                        <span class="pildora ml-2 bg-ambar-50 text-ambar-700">Acceso revocado</span>
                      } @else if (esMiCuenta(a)) {
                        <span class="ml-2 text-xs text-musgo">Es tu propia cuenta</span>
                      } @else if (confirmando() === a.usuarioId) {
                        <span class="ml-2 text-sm">¿Revocar a {{ a.usuario }}?</span>
                        <button type="button" class="btn-texto ml-2 !text-red-700" [disabled]="revocando()" (click)="revocar(a)">
                          {{ revocando() ? 'Revocando...' : 'Sí, revocar' }}
                        </button>
                        <button type="button" class="btn-texto ml-1" [disabled]="revocando()" (click)="confirmando.set(null)">Cancelar</button>
                      } @else {
                        <button type="button" class="btn-texto ml-2 !text-red-700" (click)="pedirConfirmacion(a)">Revocar acceso</button>
                      }
                    </td>
                  </tr>
                }
              </tbody>
            </table>
          </div>
        }
      </section>

      @if (objetivo(); as o) {
        <section class="tarjeta" aria-label="Coordenadas consultadas" aria-live="polite">
          <div class="flex flex-wrap items-start justify-between gap-3">
            <div>
              <h2 class="text-lg font-semibold">Coordenadas que consultó {{ o.nombre }}</h2>
              <p class="mt-1 text-sm text-musgo">
                Solo cuentan las consultas aceptadas, que son las que mostraron coordenadas exactas. Esta revisión también
                queda registrada. Si no eliges fechas, se muestran los últimos 30 días.
              </p>
            </div>
            <button type="button" class="btn-texto" (click)="cerrarExposicion()">Cerrar</button>
          </div>

          <form class="mt-4 flex flex-wrap items-end gap-4" (submit)="consultarExposicion($event)">
            <div>
              <label for="exp-desde" class="etiqueta">Desde</label>
              <input id="exp-desde" type="date" class="campo" [value]="desde()" (input)="desde.set($any($event.target).value)" />
            </div>
            <div>
              <label for="exp-hasta" class="etiqueta">Hasta</label>
              <input id="exp-hasta" type="date" class="campo" [value]="hasta()" (input)="hasta.set($any($event.target).value)" />
            </div>
            <button type="submit" class="btn-primario" [disabled]="cargandoExposicion()">
              {{ cargandoExposicion() ? 'Consultando...' : 'Consultar' }}
            </button>
          </form>

          @if (errorExposicion()) {
            <p class="aviso-error mt-4" role="alert">{{ errorExposicion() }}</p>
          } @else if (!cargandoExposicion() && exposiciones() !== null) {
            @if (exposiciones()!.length === 0) {
              <p class="mt-4 text-musgo">En ese periodo no vio coordenadas exactas de ningún hallazgo.</p>
            } @else {
              <p class="mt-4 text-sm">
                Vio las coordenadas de <strong>{{ exposiciones()!.length }}</strong>
                {{ exposiciones()!.length === 1 ? 'hallazgo' : 'hallazgos' }}, en <strong>{{ totalConsultas() }}</strong>
                {{ totalConsultas() === 1 ? 'consulta' : 'consultas' }}.
              </p>
              <div class="mt-2 overflow-x-auto">
                <table class="tabla">
                  <thead>
                    <tr>
                      <th>Hallazgo</th>
                      <th class="text-right">Veces consultado</th>
                      <th>Última consulta</th>
                    </tr>
                  </thead>
                  <tbody>
                    @for (x of exposiciones(); track x.hallazgoId) {
                      <tr>
                        <td class="font-medium">Hallazgo {{ x.hallazgoId }}</td>
                        <td class="text-right font-semibold">{{ x.consultas }}</td>
                        <td class="whitespace-nowrap">{{ x.ultimaConsulta | date: 'dd/MM/yyyy HH:mm' }}</td>
                      </tr>
                    }
                  </tbody>
                </table>
              </div>
            }
          }
        </section>
      }
    </div>
  `
})
export class SeguridadComponent {
  private api = inject(SeguridadService);
  private auth = inject(AuthService);

  alertas = signal<Alerta[]>([]);
  cargando = signal(true);
  errorCarga = signal<string | null>(null);

  /** Revocar (HU-27): primero se pide confirmar, y los ya revocados quedan marcados sin poder repetirse. */
  confirmando = signal<number | null>(null);
  revocando = signal(false);
  revocados = signal<ReadonlySet<number>>(new Set());
  aviso = signal<string | null>(null);
  errorAccion = signal<string | null>(null);

  /** Coordenadas consultadas por un usuario (HU-27). */
  objetivo = signal<{ id: number; nombre: string } | null>(null);
  desde = signal('');
  hasta = signal('');
  exposiciones = signal<Exposicion[] | null>(null);
  cargandoExposicion = signal(false);
  errorExposicion = signal<string | null>(null);
  totalConsultas = computed(() => (this.exposiciones() ?? []).reduce((suma, x) => suma + x.consultas, 0));

  /** Cada consulta lleva un número: si llega una respuesta vieja después de otra más nueva, se descarta. */
  private numeroExposicion = 0;

  constructor() {
    this.cargar();
  }

  conteo = (tipo: TipoAlerta): number => this.alertas().filter((a) => a.tipo === tipo).length;

  etiqueta = (tipo: TipoAlerta): string => ETIQUETA_TIPO[tipo];
  descripcion = (tipo: TipoAlerta): string => DESCRIPCION_TIPO[tipo];

  claseTipo(tipo: TipoAlerta): string {
    return tipo === 'RECHAZOS_REPETIDOS' ? 'bg-ambar-50 text-ambar-700' : 'bg-liquen text-musgo';
  }

  esRevocado = (usuarioId: number): boolean => this.revocados().has(usuarioId);

  /** Revocar la propia cuenta dejaría al responsable sin acceso a esta pantalla. */
  esMiCuenta = (a: Alerta): boolean => a.usuario === this.auth.usuario();

  pedirConfirmacion(a: Alerta): void {
    this.aviso.set(null);
    this.errorAccion.set(null);
    this.confirmando.set(a.usuarioId);
  }

  revocar(a: Alerta): void {
    this.revocando.set(true);
    this.errorAccion.set(null);
    this.api.revocar(a.usuarioId).subscribe({
      next: () => {
        this.revocando.set(false);
        this.confirmando.set(null);
        this.revocados.update((r) => new Set(r).add(a.usuarioId));
        this.aviso.set(`Se revocó el acceso de ${a.usuario}. Recibirá un error de sesión en su siguiente petición.`);
      },
      error: (err: HttpErrorResponse) => {
        this.revocando.set(false);
        this.confirmando.set(null);
        this.errorAccion.set(mensajeDeError(err));
      }
    });
  }

  verExposicion(a: Alerta): void {
    this.objetivo.set({ id: a.usuarioId, nombre: a.usuario });
    this.desde.set('');
    this.hasta.set('');
    this.pedirExposicion();
  }

  consultarExposicion(ev: Event): void {
    ev.preventDefault();
    this.pedirExposicion();
  }

  cerrarExposicion(): void {
    this.numeroExposicion++;
    this.objetivo.set(null);
    this.exposiciones.set(null);
    this.errorExposicion.set(null);
    this.cargandoExposicion.set(false);
  }

  private pedirExposicion(): void {
    const o = this.objetivo();
    if (!o) {
      return;
    }
    if (this.desde() && this.hasta() && this.desde() > this.hasta()) {
      this.errorExposicion.set('La fecha "Desde" no puede ser posterior a "Hasta".');
      this.exposiciones.set(null);
      return;
    }
    const numero = ++this.numeroExposicion;
    this.cargandoExposicion.set(true);
    this.errorExposicion.set(null);
    const desde = this.desde() ? `${this.desde()}T00:00:00` : undefined;
    const hasta = this.hasta() ? `${this.hasta()}T23:59:59` : undefined;
    this.api.exposicion(o.id, desde, hasta).subscribe({
      next: (lista) => {
        if (numero !== this.numeroExposicion) {
          return;
        }
        this.exposiciones.set(lista);
        this.cargandoExposicion.set(false);
      },
      error: (err: HttpErrorResponse) => {
        if (numero !== this.numeroExposicion) {
          return;
        }
        this.exposiciones.set(null);
        this.errorExposicion.set(mensajeDeError(err));
        this.cargandoExposicion.set(false);
      }
    });
  }

  cargar(): void {
    this.cargando.set(true);
    this.api.listar().subscribe({
      next: (alertas) => {
        this.alertas.set(alertas);
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
