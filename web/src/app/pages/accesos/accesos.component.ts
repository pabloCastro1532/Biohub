import { DatePipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, computed, inject, signal } from '@angular/core';
import { mensajeDeError } from '../../core/errores';
import { Acceso, AccesosService, ResultadoAcceso } from './accesos.service';

/**
 * HU-25: quién consultó la ubicación exacta de un hallazgo, cuándo y con qué resultado (aceptada o rechazada).
 * Solo lectura: el servidor registra cada consulta, nadie puede crear ni borrar filas desde aquí. Sin coordenadas (ASR-4).
 */
@Component({
  selector: 'app-accesos',
  standalone: true,
  imports: [DatePipe],
  template: `
    <header>
      <h1 class="text-2xl font-semibold tracking-tight">Registro de accesos</h1>
      <p class="mt-1 text-musgo">
        Cada vez que alguien consulta una ubicación exacta queda constancia, también cuando se le niega.
        Aquí no se muestran las coordenadas, solo quién consultó y con qué resultado.
      </p>
    </header>

    <div class="mt-6 space-y-6">
      <section class="tarjeta">
        <div class="flex flex-wrap items-end gap-4">
          <div>
            <label for="acc-resultado" class="etiqueta">Resultado</label>
            <select id="acc-resultado" class="campo" (change)="cambiarResultado($event)">
              <option value="">Todos</option>
              <option value="ACEPTADO">Aceptados</option>
              <option value="RECHAZADO">Rechazados</option>
            </select>
          </div>
          <div>
            <label for="acc-usuario" class="etiqueta">Usuario</label>
            <input
              id="acc-usuario"
              class="campo"
              placeholder="Parte del nombre"
              [value]="filtroUsuario()"
              (input)="filtroUsuario.set($any($event.target).value)"
            />
          </div>
          <button type="button" class="btn-secundario" (click)="cargar()" [disabled]="cargando()">Actualizar</button>
        </div>

        @if (!cargando() && !error()) {
          <p class="mt-4 text-sm text-musgo" role="status">
            {{ visibles().length }} {{ visibles().length === 1 ? 'acceso' : 'accesos' }}:
            {{ aceptados() }} {{ aceptados() === 1 ? 'aceptado' : 'aceptados' }} y
            {{ rechazados() }} {{ rechazados() === 1 ? 'rechazado' : 'rechazados' }}.
            @if (accesos().length === 200) {
              Se muestran los 200 más recientes.
            }
          </p>
        }
      </section>

      <section class="tarjeta !p-0 overflow-hidden">
        @if (cargando()) {
          <p class="p-6 text-musgo">Cargando accesos...</p>
        } @else if (error()) {
          <p class="m-6 aviso-error" role="alert">{{ error() }}</p>
        } @else if (accesos().length === 0) {
          <p class="p-6 text-musgo">Todavía no se ha consultado ninguna ubicación exacta.</p>
        } @else if (visibles().length === 0) {
          <p class="p-6 text-musgo">Ningún acceso coincide con ese usuario.</p>
        } @else {
          <div class="overflow-x-auto p-3">
            <table class="tabla">
              <thead>
                <tr>
                  <th>Fecha y hora</th>
                  <th>Usuario</th>
                  <th>Hallazgo</th>
                  <th>Resultado</th>
                </tr>
              </thead>
              <tbody>
                @for (a of visibles(); track a.id) {
                  <tr>
                    <td>{{ a.ocurridoEn | date: 'dd/MM/yyyy HH:mm:ss' }}</td>
                    <td class="font-semibold">{{ a.usuario }}</td>
                    <td>N.º {{ a.hallazgoId }}</td>
                    <td>
                      @if (a.resultado === 'ACEPTADO') {
                        <span class="pildora bg-liquen text-musgo">Aceptado</span>
                      } @else {
                        <span class="pildora bg-ambar-50 text-ambar-700">Rechazado</span>
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
export class AccesosComponent {
  private api = inject(AccesosService);

  accesos = signal<Acceso[]>([]);
  resultado = signal<ResultadoAcceso | null>(null);
  filtroUsuario = signal('');
  cargando = signal(true);
  error = signal<string | null>(null);

  visibles = computed(() => {
    const texto = this.filtroUsuario().trim().toLowerCase();
    return texto ? this.accesos().filter((a) => a.usuario.toLowerCase().includes(texto)) : this.accesos();
  });
  aceptados = computed(() => this.visibles().filter((a) => a.resultado === 'ACEPTADO').length);
  rechazados = computed(() => this.visibles().filter((a) => a.resultado === 'RECHAZADO').length);

  constructor() {
    this.cargar();
  }

  cambiarResultado(ev: Event): void {
    const v = (ev.target as HTMLSelectElement).value;
    this.resultado.set(v === 'ACEPTADO' || v === 'RECHAZADO' ? v : null);
    this.cargar();
  }

  cargar(): void {
    this.cargando.set(true);
    this.api.listar(this.resultado()).subscribe({
      next: (lista) => {
        this.accesos.set(lista);
        this.error.set(null);
        this.cargando.set(false);
      },
      error: (err: HttpErrorResponse) => {
        this.accesos.set([]);
        this.error.set(mensajeDeError(err));
        this.cargando.set(false);
      }
    });
  }
}
