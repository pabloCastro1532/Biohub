import { DatePipe } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { Rol } from '../../core/auth.service';
import { mensajeDeError } from '../../core/errores';
import { Usuario, UsuariosService } from './usuarios.service';

interface OpcionRol {
  valor: Rol;
  nombre: string;
  descripcion: string;
}

/** Los seis roles con un nombre claro y lo que puede hacer cada uno, para elegir sin adivinar. */
const ROLES: OpcionRol[] = [
  { valor: 'INVESTIGADOR', nombre: 'Investigador', descripcion: 'Registra y edita hallazgos y gestiona equipos de campo. No ve coordenadas exactas.' },
  { valor: 'DIRECTOR', nombre: 'Director científico', descripcion: 'Es el único rol que ve coordenadas exactas. Define plantillas y agrega especies.' },
  { valor: 'AUDITOR', nombre: 'Auditor', descripcion: 'Consulta hallazgos, historiales y el registro de accesos.' },
  { valor: 'ADMINISTRADOR', nombre: 'Administrador', descripcion: 'Crea cuentas, define plantillas, agrega especies y gestiona equipos.' },
  { valor: 'SEGURIDAD', nombre: 'Responsable de seguridad', descripcion: 'Atiende las alertas y puede revocar el acceso de un usuario.' },
  { valor: 'VISITANTE', nombre: 'Visitante', descripcion: 'Solo busca especies en el inventario público.' }
];

const CLAVE_MINIMA = 8;

/** Gestión de cuentas: el administrador crea usuarios con su rol y devuelve el acceso a quien fue revocado (HU-27). */
@Component({
  selector: 'app-usuarios',
  standalone: true,
  imports: [DatePipe],
  template: `
    <header>
      <h1 class="text-2xl font-semibold tracking-tight">Usuarios</h1>
      <p class="mt-1 text-musgo">
        Crea las cuentas de investigadores, directores y demás personal, y elige qué puede hacer cada una.
      </p>
    </header>

    <div class="mt-6 grid gap-6 lg:grid-cols-2">
      <section aria-label="Crear una cuenta" class="min-w-0">
        <form class="tarjeta space-y-4" (submit)="crear($event)">
          <h2 class="text-lg font-semibold">Crear una cuenta</h2>

          <div>
            <label for="usr-nombre" class="etiqueta">Nombre de usuario *</label>
            <input
              id="usr-nombre"
              class="campo"
              maxlength="40"
              autocomplete="off"
              placeholder="Ej.: maria.perez"
              [value]="nombre()"
              (input)="nombre.set($any($event.target).value)"
            />
            <p class="mt-1 text-sm text-musgo">Letras sin tilde, números, punto, guion o guion bajo. Con ese nombre inicia sesión.</p>
          </div>

          <div>
            <label for="usr-rol" class="etiqueta">Rol *</label>
            <select id="usr-rol" class="campo" (change)="rol.set($any($event.target).value)">
              <option value="" [selected]="rol() === ''">Elige un rol</option>
              @for (r of roles; track r.valor) {
                <option [value]="r.valor" [selected]="rol() === r.valor">{{ r.nombre }}</option>
              }
            </select>
            @if (descripcionRol()) {
              <p class="mt-1 text-sm text-musgo">{{ descripcionRol() }}</p>
            }
          </div>

          <div class="grid gap-4 md:grid-cols-2">
            <div>
              <label for="usr-clave" class="etiqueta">Clave *</label>
              <input
                id="usr-clave"
                class="campo"
                type="password"
                autocomplete="new-password"
                [value]="clave()"
                (input)="clave.set($any($event.target).value)"
              />
            </div>
            <div>
              <label for="usr-clave2" class="etiqueta">Repite la clave *</label>
              <input
                id="usr-clave2"
                class="campo"
                type="password"
                autocomplete="new-password"
                [value]="clave2()"
                (input)="clave2.set($any($event.target).value)"
              />
            </div>
          </div>
          <p class="-mt-2 text-sm text-musgo">Mínimo 8 caracteres. Entrégasela a la persona en privado: no se vuelve a mostrar.</p>

          @if (errorCrear()) {
            <p class="aviso-error" role="alert">{{ errorCrear() }}</p>
          }
          @if (avisoCrear()) {
            <p class="aviso-ok" role="status">{{ avisoCrear() }}</p>
          }

          <button type="submit" class="btn-primario" [disabled]="creando()">
            {{ creando() ? 'Creando...' : 'Crear cuenta' }}
          </button>
        </form>
      </section>

      <section aria-label="Cuentas" class="min-w-0">
        <div class="tarjeta !p-0 overflow-hidden">
          <div class="flex items-center justify-between gap-4 px-4 pt-4">
            <h2 class="text-lg font-semibold">Cuentas existentes</h2>
            <button type="button" class="btn-secundario" (click)="cargar()" [disabled]="cargando()">Actualizar</button>
          </div>

          @if (cargando()) {
            <p class="p-6 text-musgo" role="status">Cargando cuentas...</p>
          } @else if (errorLista()) {
            <p class="m-6 aviso-error" role="alert">{{ errorLista() }}</p>
          } @else if (usuarios().length === 0) {
            <p class="p-6 text-musgo">Todavía no hay cuentas.</p>
          } @else {
            @if (errorAccion()) {
              <p class="mx-4 mt-3 aviso-error" role="alert">{{ errorAccion() }}</p>
            }
            <div class="overflow-x-auto">
              <table class="tabla">
                <thead>
                  <tr>
                    <th>Usuario</th>
                    <th>Rol</th>
                    <th>Estado</th>
                    <th>Creada</th>
                    <th>Qué hacer</th>
                  </tr>
                </thead>
                <tbody>
                  @for (u of usuarios(); track u.id) {
                    <tr>
                      <td class="font-medium">{{ u.nombreUsuario }}</td>
                      <td>{{ nombreRol(u.rol) }}</td>
                      <td>
                        @if (u.activo) {
                          <span class="pildora bg-bio-100 text-bio-900">Activa</span>
                        } @else {
                          <span class="pildora bg-ambar-50 text-ambar-700">Acceso revocado</span>
                        }
                      </td>
                      <td class="whitespace-nowrap">{{ u.creadoEn | date: 'dd/MM/yyyy HH:mm' }}</td>
                      <td class="whitespace-nowrap">
                        @if (u.activo) {
                          <span class="text-musgo">—</span>
                        } @else if (confirmando() === u.id) {
                          <span class="text-sm">¿Devolver el acceso a {{ u.nombreUsuario }}?</span>
                          <button type="button" class="btn-texto ml-2" [disabled]="reactivando()" (click)="reactivar(u)">
                            {{ reactivando() ? 'Devolviendo...' : 'Sí, devolver' }}
                          </button>
                          <button type="button" class="btn-texto ml-1" [disabled]="reactivando()" (click)="confirmando.set(null)">Cancelar</button>
                        } @else {
                          <button type="button" class="btn-texto" (click)="pedirConfirmacion(u)">Devolver acceso</button>
                        }
                      </td>
                    </tr>
                  }
                </tbody>
              </table>
            </div>
          }
        </div>
      </section>
    </div>
  `
})
export class UsuariosComponent implements OnInit {
  private api = inject(UsuariosService);

  roles = ROLES;

  usuarios = signal<Usuario[]>([]);
  cargando = signal(false);
  errorLista = signal('');

  nombre = signal('');
  rol = signal('');
  clave = signal('');
  clave2 = signal('');
  creando = signal(false);
  errorCrear = signal('');
  avisoCrear = signal('');

  confirmando = signal<number | null>(null);
  reactivando = signal(false);
  errorAccion = signal('');

  ngOnInit(): void {
    this.cargar();
  }

  cargar(): void {
    this.cargando.set(true);
    this.errorLista.set('');
    this.errorAccion.set('');
    this.api.listar().subscribe({
      next: (lista) => {
        this.usuarios.set(lista);
        this.cargando.set(false);
      },
      error: (e) => {
        this.errorLista.set(mensajeDeError(e));
        this.cargando.set(false);
      }
    });
  }

  nombreRol(rol: Rol): string {
    return ROLES.find((r) => r.valor === rol)?.nombre ?? rol;
  }

  descripcionRol(): string {
    return ROLES.find((r) => r.valor === this.rol())?.descripcion ?? '';
  }

  crear(ev: Event): void {
    ev.preventDefault();
    this.errorCrear.set('');
    this.avisoCrear.set('');

    const nombre = this.nombre().trim();
    if (!nombre) {
      this.errorCrear.set('Escribe el nombre de usuario.');
      return;
    }
    if (!this.rol()) {
      this.errorCrear.set('Elige un rol.');
      return;
    }
    if (this.clave().length < CLAVE_MINIMA) {
      this.errorCrear.set(`La clave debe tener al menos ${CLAVE_MINIMA} caracteres.`);
      return;
    }
    if (this.clave() !== this.clave2()) {
      this.errorCrear.set('Las dos claves no son iguales. Escríbelas de nuevo.');
      return;
    }

    this.creando.set(true);
    this.api.crear(nombre, this.rol() as Rol, this.clave()).subscribe({
      next: (u) => {
        this.creando.set(false);
        this.avisoCrear.set(`Se creó la cuenta ${u.nombreUsuario} (${this.nombreRol(u.rol)}). Ya puede iniciar sesión.`);
        this.nombre.set('');
        this.rol.set('');
        this.clave.set('');
        this.clave2.set('');
        this.cargar();
      },
      error: (e) => {
        this.creando.set(false);
        this.errorCrear.set(mensajeDeError(e));
      }
    });
  }

  pedirConfirmacion(u: Usuario): void {
    this.errorAccion.set('');
    this.confirmando.set(u.id);
  }

  reactivar(u: Usuario): void {
    this.reactivando.set(true);
    this.errorAccion.set('');
    this.api.reactivar(u.id).subscribe({
      next: () => {
        this.reactivando.set(false);
        this.confirmando.set(null);
        this.cargar();
      },
      error: (e) => {
        this.reactivando.set(false);
        this.confirmando.set(null);
        this.errorAccion.set(mensajeDeError(e));
      }
    });
  }
}
