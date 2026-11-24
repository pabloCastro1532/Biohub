import { DatePipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject, signal } from '@angular/core';
import { mensajeDeError } from '../../core/errores';
import { REGIONES, etiquetaAtributo, valorLegible } from '../hallazgos/etiquetas-atributos';
import { EspecieResumen, EspeciesService, FichaEspecie } from './especies.service';

/**
 * HU-18: buscar especies del inventario por nombre y por región.
 * HU-19: ver la ficha de una especie. Toda la información es pública: solo región, nunca coordenadas exactas (ASR-4).
 */
@Component({
  selector: 'app-especies',
  standalone: true,
  imports: [DatePipe],
  template: `
    <header>
      <h1 class="text-2xl font-semibold tracking-tight">Buscar especies</h1>
      <p class="mt-1 text-musgo">
        Consulta el inventario nacional. Para proteger a las especies, solo se muestra la región o el departamento
        donde se encontraron, nunca la ubicación exacta.
      </p>
    </header>

    <div class="mt-6 grid gap-6 lg:grid-cols-2">
      <section aria-label="Búsqueda" class="min-w-0 space-y-6">
        <form class="tarjeta" (submit)="buscar($event)">
          <div class="flex flex-wrap items-end gap-4">
            <div class="grow">
              <label for="esp-texto" class="etiqueta">Nombre de la especie</label>
              <input
                id="esp-texto"
                class="campo w-full"
                placeholder="Por ejemplo: jaguar o Ara macao"
                maxlength="100"
                [value]="texto()"
                (input)="texto.set($any($event.target).value)"
              />
            </div>
            <div>
              <label for="esp-region" class="etiqueta">Departamento</label>
              <select id="esp-region" class="campo" (change)="region.set($any($event.target).value)">
                <option value="" [selected]="region() === ''">Todos los departamentos</option>
                @for (r of regiones; track r) {
                  <option [value]="r" [selected]="region() === r">{{ r }}</option>
                }
              </select>
            </div>
            <button type="submit" class="btn-primario" [disabled]="cargando()">Buscar</button>
            @if (texto() || region()) {
              <button type="button" class="btn-texto" (click)="limpiar()">Quitar filtros</button>
            }
          </div>
        </form>

        <div class="tarjeta !p-0 overflow-hidden">
          @if (cargando()) {
            <p class="p-6 text-musgo" role="status">Buscando...</p>
          } @else if (error()) {
            <p class="m-6 aviso-error" role="alert">{{ error() }}</p>
          } @else if (resultados().length === 0) {
            <p class="p-6 text-musgo" role="status">
              @if (texto() || region()) {
                Ninguna especie coincide con tu búsqueda. Prueba con otro nombre o quita la región.
              } @else {
                Todavía no hay especies en el inventario.
              }
            </p>
          } @else {
            <p class="px-6 pt-4 text-sm text-musgo" role="status">
              {{ resultados().length }} {{ resultados().length === 1 ? 'especie' : 'especies' }}
            </p>
            <div class="overflow-x-auto p-3">
              <table class="tabla">
                <thead>
                  <tr>
                    <th>Especie</th>
                    <th>Hallazgos</th>
                    <th>Último avistamiento</th>
                    <th><span class="sr-only">Acciones</span></th>
                  </tr>
                </thead>
                <tbody>
                  @for (e of resultados(); track e.id) {
                    <tr [class.bg-liquen]="ficha()?.id === e.id">
                      <td>
                        <span class="font-semibold">{{ e.nombreComun ?? e.nombreCientifico }}</span>
                        @if (e.nombreComun) {
                          <br /><span class="text-sm italic text-musgo">{{ e.nombreCientifico }}</span>
                        }
                      </td>
                      <td>{{ e.totalHallazgos }}</td>
                      <td>
                        @if (e.ultimaObservacion) {
                          {{ e.ultimaObservacion | date: 'dd/MM/yyyy' }}
                        } @else {
                          <span class="text-musgo">Sin hallazgos</span>
                        }
                      </td>
                      <td class="text-right">
                        <button type="button" class="btn-texto whitespace-nowrap" (click)="abrirFicha(e)">Ver ficha</button>
                      </td>
                    </tr>
                  }
                </tbody>
              </table>
            </div>
          }
        </div>
      </section>

      <section aria-label="Ficha de la especie" aria-live="polite" class="min-w-0">
        @if (cargandoFicha()) {
          <div class="tarjeta"><p class="text-musgo" role="status">Cargando la ficha...</p></div>
        } @else if (errorFicha()) {
          <div class="tarjeta"><p class="aviso-error" role="alert">{{ errorFicha() }}</p></div>
        } @else {
          @if (ficha(); as f) {
          <article class="tarjeta space-y-5">
            <div class="flex items-start justify-between gap-4">
              <div>
                <h2 class="text-xl font-semibold">{{ f.nombreComun ?? f.nombreCientifico }}</h2>
                @if (f.nombreComun) {
                  <p class="italic text-musgo">{{ f.nombreCientifico }}</p>
                }
              </div>
              <button type="button" class="btn-texto" (click)="cerrarFicha()">Cerrar</button>
            </div>

            <dl class="grid grid-cols-2 gap-4">
              <div>
                <dt class="etiqueta">Hallazgos registrados</dt>
                <dd class="text-lg font-semibold">{{ f.totalHallazgos }}</dd>
              </div>
              <div>
                <dt class="etiqueta">Último avistamiento</dt>
                <dd class="text-lg font-semibold">
                  @if (f.ultimaObservacion) {
                    {{ f.ultimaObservacion | date: 'dd/MM/yyyy' }}
                  } @else {
                    <span class="font-normal text-musgo">Sin hallazgos</span>
                  }
                </dd>
              </div>
            </dl>

            <div>
              <h3 class="etiqueta">Dónde se ha visto</h3>
              @if (f.regiones.length === 0) {
                <p class="text-musgo">Todavía no hay hallazgos de esta especie.</p>
              } @else {
                <ul class="mt-2 flex flex-wrap gap-2">
                  @for (r of f.regiones; track r.region) {
                    <li class="pildora bg-liquen text-musgo">{{ r.region }}: {{ r.total }}</li>
                  }
                </ul>
              }
            </div>

            @if (f.recientes.length > 0) {
              <div>
                <h3 class="etiqueta">Hallazgos más recientes</h3>
                <div class="overflow-x-auto">
                  <table class="tabla">
                    <thead>
                      <tr>
                        <th>Fecha</th>
                        <th>Región</th>
                        <th>Datos</th>
                      </tr>
                    </thead>
                    <tbody>
                      @for (h of f.recientes; track h.hallazgoId) {
                        <tr>
                          <td>{{ h.fecha | date: 'dd/MM/yyyy' }}</td>
                          <td>{{ h.region }}</td>
                          <td>
                            @for (a of atributos(h.atributos); track a[0]) {
                              <span class="mr-3 inline-block"><span class="text-musgo">{{ a[0] }}:</span> {{ a[1] }}</span>
                            } @empty {
                              <span class="text-musgo">Sin datos adicionales</span>
                            }
                          </td>
                        </tr>
                      }
                    </tbody>
                  </table>
                </div>
              </div>
            }
          </article>
          } @else {
            <div class="tarjeta">
              <p class="text-musgo">Elige una especie de la lista y pulsa "Ver ficha" para ver sus detalles.</p>
            </div>
          }
        }
      </section>
    </div>
  `
})
export class EspeciesComponent {
  private api = inject(EspeciesService);

  /** Los departamentos que se usan al registrar un hallazgo: la búsqueda compara la región tal cual, así que se elige de la lista. */
  readonly regiones = REGIONES;

  texto = signal('');
  region = signal('');
  resultados = signal<EspecieResumen[]>([]);
  cargando = signal(true);
  error = signal<string | null>(null);

  ficha = signal<FichaEspecie | null>(null);
  cargandoFicha = signal(false);
  errorFicha = signal<string | null>(null);

  /** Cada búsqueda y cada ficha lleva un número: si llega una respuesta vieja después de una nueva, se descarta. */
  private numeroBusqueda = 0;
  private numeroFicha = 0;

  constructor() {
    this.consultar();
  }

  buscar(ev: Event): void {
    ev.preventDefault();
    this.consultar();
  }

  limpiar(): void {
    this.texto.set('');
    this.region.set('');
    this.consultar();
  }

  abrirFicha(e: EspecieResumen): void {
    const numero = ++this.numeroFicha;
    this.cargandoFicha.set(true);
    this.errorFicha.set(null);
    this.api.ficha(e.id).subscribe({
      next: (f) => {
        if (numero !== this.numeroFicha) {
          return;
        }
        this.ficha.set(f);
        this.cargandoFicha.set(false);
      },
      error: (err: HttpErrorResponse) => {
        if (numero !== this.numeroFicha) {
          return;
        }
        this.ficha.set(null);
        this.errorFicha.set(mensajeDeError(err));
        this.cargandoFicha.set(false);
      }
    });
  }

  cerrarFicha(): void {
    this.numeroFicha++;
    this.ficha.set(null);
    this.errorFicha.set(null);
    this.cargandoFicha.set(false);
  }

  /** Atributos de un hallazgo como pares [nombre legible, valor legible]. */
  atributos(atributos: Record<string, unknown>): [string, string][] {
    return Object.entries(atributos ?? {}).map(([clave, valor]) => [etiquetaAtributo(clave), valorLegible(valor)]);
  }

  private consultar(): void {
    const numero = ++this.numeroBusqueda;
    this.cargando.set(true);
    this.api.buscar(this.texto(), this.region()).subscribe({
      next: (lista) => {
        if (numero !== this.numeroBusqueda) {
          return;
        }
        this.resultados.set(lista);
        this.error.set(null);
        this.cargando.set(false);
      },
      error: (err: HttpErrorResponse) => {
        if (numero !== this.numeroBusqueda) {
          return;
        }
        this.resultados.set([]);
        this.error.set(mensajeDeError(err));
        this.cargando.set(false);
      }
    });
  }
}
