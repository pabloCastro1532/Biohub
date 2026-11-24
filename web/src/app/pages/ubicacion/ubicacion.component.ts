import { DatePipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, OnDestroy, computed, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { AuthService } from '../../core/auth.service';
import { mensajeDeError } from '../../core/errores';
import { Hallazgo, HallazgosService } from '../hallazgos/hallazgos.service';
import { Especie, PlantillasService } from '../plantillas/plantillas.service';
import { Ubicacion, UbicacionService } from './ubicacion.service';

/** Una consulta hecha desde esta pantalla. A propósito no guarda coordenadas: solo qué se consultó y qué se obtuvo. */
interface ConsultaHecha {
  hallazgoId: number;
  hora: Date;
  exacta: boolean;
}

/** Segundos que se muestran las coordenadas exactas antes de ocultarse solas. */
const SEGUNDOS_VISIBLES = 30;

/**
 * HU-15: consultar la ubicación de un hallazgo.
 * HU-16: el director científico recibe las coordenadas exactas.
 * HU-17: cualquier otro rol recibe solo la región.
 * HU-25: cada consulta queda registrada en el servidor, aceptada o rechazada.
 */
@Component({
  selector: 'app-ubicacion',
  standalone: true,
  imports: [DatePipe, RouterLink],
  template: `
    <header>
      <h1 class="text-2xl font-semibold tracking-tight">Ubicación de un hallazgo</h1>
      <p class="mt-1 text-musgo">
        La ubicación exacta de las especies es confidencial. Solo la ve el director científico, y cada consulta queda
        registrada.
      </p>
    </header>

    <div class="mt-6 grid gap-6 lg:grid-cols-2">
      <section class="min-w-0 space-y-6" aria-label="Consulta">
        <div class="tarjeta space-y-4">
          @if (esDirector()) {
            <p class="aviso-ok">
              Como director científico puedes ver las coordenadas exactas. Se ocultan solas a los {{ segundos }} segundos.
            </p>
          } @else {
            <p class="aviso-error">
              Tu rol solo permite ver la región del hallazgo. Esta consulta quedará registrada como rechazada, y varias
              seguidas generan una alerta de seguridad.
            </p>
          }

          <form (submit)="consultar($event)" class="space-y-4">
            <div>
              <label for="ubi-hallazgo" class="etiqueta">Hallazgo</label>
              @if (listaDisponible()) {
                <select id="ubi-hallazgo" class="campo w-full" (change)="elegir($any($event.target).value)">
                  <option value="">Elige un hallazgo</option>
                  @for (o of opciones(); track o.id) {
                    <option [value]="o.id" [selected]="idTexto() === '' + o.id">{{ o.texto }}</option>
                  }
                </select>
              } @else {
                <input
                  id="ubi-hallazgo"
                  type="number"
                  min="1"
                  step="1"
                  class="campo w-full"
                  placeholder="Número del hallazgo, por ejemplo 12"
                  [value]="idTexto()"
                  (input)="elegir($any($event.target).value)"
                />
                <p class="mt-1 text-xs text-musgo">Tu rol no puede ver la lista de hallazgos: escribe el número.</p>
              }
            </div>
            <button type="submit" class="btn-primario" [disabled]="idNumero() === null || consultando()">
              {{ consultando() ? 'Consultando...' : 'Consultar ubicación' }}
            </button>
          </form>

          @if (error()) {
            <p class="aviso-error" role="alert">{{ error() }}</p>
          }
        </div>

        <div class="tarjeta">
          <h2 class="text-lg font-semibold">Consultas de esta sesión</h2>
          <p class="mt-1 text-sm text-musgo">
            Todas quedaron registradas en el servidor con tu usuario, el hallazgo, la fecha y el resultado.
            @if (puedeVerRegistro()) {
              Puedes revisarlas en el <a routerLink="/accesos" class="underline">registro de accesos</a>.
            }
          </p>
          @if (consultas().length === 0) {
            <p class="mt-4 text-musgo">Todavía no has consultado ninguna ubicación.</p>
          } @else {
            <ul class="mt-4 space-y-2">
              @for (c of consultas(); track c.hora.getTime()) {
                <li class="flex flex-wrap items-center gap-2 text-sm">
                  <span class="font-semibold">Hallazgo {{ c.hallazgoId }}</span>
                  <span class="text-musgo">{{ c.hora | date: 'HH:mm:ss' }}</span>
                  @if (c.exacta) {
                    <span class="pildora bg-liquen text-musgo">Aceptada</span>
                  } @else {
                    <span class="pildora bg-liquen text-musgo">Solo región</span>
                  }
                </li>
              }
            </ul>
          }
        </div>
      </section>

      <section class="min-w-0" aria-label="Resultado" aria-live="polite">
        @if (resultado(); as r) {
          <article class="tarjeta space-y-5">
            <div class="flex items-start justify-between gap-4">
              <div>
                <h2 class="text-xl font-semibold">Hallazgo {{ r.hallazgoId }}</h2>
                <p class="text-musgo">{{ nombreDe(r.hallazgoId) }}</p>
              </div>
              <button type="button" class="btn-texto" (click)="ocultar()">Ocultar</button>
            </div>

            <dl class="grid grid-cols-2 gap-4">
              <div class="col-span-2">
                <dt class="etiqueta">Región</dt>
                <dd class="text-lg font-semibold">{{ r.region }}</dd>
              </div>
              @if (r.exacta) {
                <div>
                  <dt class="etiqueta">Latitud</dt>
                  <dd class="text-lg font-semibold">{{ r.latitud }}</dd>
                </div>
                <div>
                  <dt class="etiqueta">Longitud</dt>
                  <dd class="text-lg font-semibold">{{ r.longitud }}</dd>
                </div>
              }
            </dl>

            @if (r.exacta) {
              <p class="text-sm text-musgo">
                Ubicación confidencial: no la compartas fuera del Instituto. Se oculta sola a los {{ segundos }} segundos.
              </p>
            } @else {
              <p class="text-sm text-musgo">
                Solo se muestra la región. La ubicación exacta está reservada al director científico.
              </p>
            }
          </article>
        } @else {
          <div class="tarjeta">
            <p class="text-musgo">Elige un hallazgo y consulta su ubicación. El resultado aparece aquí.</p>
          </div>
        }
      </section>
    </div>
  `
})
export class UbicacionComponent implements OnDestroy {
  private api = inject(UbicacionService);
  private hallazgosApi = inject(HallazgosService);
  private plantillas = inject(PlantillasService);
  private auth = inject(AuthService);
  private temporizador: ReturnType<typeof setTimeout> | null = null;

  readonly segundos = SEGUNDOS_VISIBLES;

  hallazgos = signal<Hallazgo[]>([]);
  especies = signal<Especie[]>([]);
  /** El director y el auditor pueden listar hallazgos; seguridad no, y escribe el número. */
  listaDisponible = signal(false);
  idTexto = signal('');
  consultando = signal(false);
  error = signal<string | null>(null);
  resultado = signal<Ubicacion | null>(null);
  consultas = signal<ConsultaHecha[]>([]);

  esDirector = computed(() => this.auth.tieneRol('DIRECTOR'));
  puedeVerRegistro = computed(() => this.auth.tieneRol('AUDITOR', 'SEGURIDAD'));

  idNumero = computed(() => {
    const n = Number(this.idTexto());
    return this.idTexto().trim() !== '' && Number.isInteger(n) && n >= 1 ? n : null;
  });

  opciones = computed(() =>
    [...this.hallazgos()]
      .sort((a, b) => b.id - a.id)
      .map((h) => ({ id: h.id, texto: `N.º ${h.id} · ${this.nombreEspecie(h.especieId)} · ${h.region}` }))
  );

  constructor() {
    this.hallazgosApi.listar().subscribe({
      next: (lista) => {
        this.hallazgos.set(lista);
        this.listaDisponible.set(true);
      },
      error: () => this.listaDisponible.set(false)
    });
    // Los nombres de especie solo los puede pedir quien gestiona fichas; para el resto se muestra "Especie N".
    this.plantillas.especies().subscribe({ next: (e) => this.especies.set(e), error: () => undefined });
  }

  ngOnDestroy(): void {
    this.cancelarTemporizador();
  }

  nombreDe(hallazgoId: number): string {
    const h = this.hallazgos().find((x) => x.id === hallazgoId);
    return h ? this.nombreEspecie(h.especieId) : '';
  }

  private nombreEspecie(especieId: number): string {
    const e = this.especies().find((x) => x.id === especieId);
    return e ? (e.nombreComun ?? e.nombreCientifico) : `Especie ${especieId}`;
  }

  elegir(valor: string): void {
    this.idTexto.set(valor);
    this.error.set(null);
    this.ocultar();
  }

  consultar(evento: Event): void {
    evento.preventDefault();
    const id = this.idNumero();
    if (id === null || this.consultando()) {
      return;
    }
    this.consultando.set(true);
    this.error.set(null);
    this.ocultar();

    this.api.consultar(id).subscribe({
      next: (u) => {
        this.consultando.set(false);
        this.resultado.set(u);
        this.consultas.update((antes) => [{ hallazgoId: id, hora: new Date(), exacta: u.exacta }, ...antes].slice(0, 10));
        if (u.exacta) {
          // Las coordenadas no se quedan en pantalla: se ocultan solas.
          this.temporizador = setTimeout(() => this.resultado.set(null), SEGUNDOS_VISIBLES * 1000);
        }
      },
      error: (err: HttpErrorResponse) => {
        this.consultando.set(false);
        this.error.set(
          err.status === 404 ? `No existe un hallazgo con el número ${id}.` : mensajeDeError(err)
        );
      }
    });
  }

  ocultar(): void {
    this.cancelarTemporizador();
    this.resultado.set(null);
  }

  private cancelarTemporizador(): void {
    if (this.temporizador !== null) {
      clearTimeout(this.temporizador);
      this.temporizador = null;
    }
  }
}
