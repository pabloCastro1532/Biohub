import { DatePipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, computed, inject, signal } from '@angular/core';
import { mensajeDeError } from '../../core/errores';
import { Alerta, SeguridadService, TipoAlerta } from './seguridad.service';

const ETIQUETA_TIPO: Record<TipoAlerta, string> = {
  CONSULTAS_EXCESIVAS: 'Consultas de ubicación excesivas',
  RECHAZOS_REPETIDOS: 'Rechazos repetidos'
};

const DESCRIPCION_TIPO: Record<TipoAlerta, string> = {
  CONSULTAS_EXCESIVAS: 'Más consultas de ubicación exacta de lo normal en una hora.',
  RECHAZOS_REPETIDOS: 'Varias consultas rechazadas seguidas en pocos minutos.'
};

/** HU-26: el responsable de seguridad revisa las alertas del monitor. Solo lectura. */
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
export class SeguridadComponent {
  private api = inject(SeguridadService);

  alertas = signal<Alerta[]>([]);
  cargando = signal(true);
  errorCarga = signal<string | null>(null);

  constructor() {
    this.cargar();
  }

  conteo = (tipo: TipoAlerta): number => this.alertas().filter((a) => a.tipo === tipo).length;

  etiqueta = (tipo: TipoAlerta): string => ETIQUETA_TIPO[tipo];
  descripcion = (tipo: TipoAlerta): string => DESCRIPCION_TIPO[tipo];

  claseTipo(tipo: TipoAlerta): string {
    return tipo === 'RECHAZOS_REPETIDOS' ? 'bg-ambar-50 text-ambar-700' : 'bg-liquen text-musgo';
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
