import { Component, computed, inject } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { AuthService } from '../core/auth.service';
import { MENU } from '../core/menu';

/** Trazo (24x24, sin relleno) de cada opción del menú. Las rutas nuevas sin icono usan el genérico. */
const ICONOS: Record<string, string> = {
  '/inicio': 'M3 11.5 12 4l9 7.5M5 10v9h5v-5h4v5h5v-9',
  '/especies': 'M20 4c-8 0-14 4-14 11 0 1.5.4 2.8 1 4M20 4c0 8-4 14-11 14-1.5 0-2.8-.4-4-1M6 19c2-4 5-7 9-9',
  '/hallazgos': 'M12 21s7-6.2 7-11.5A7 7 0 0 0 5 9.5C5 14.8 12 21 12 21ZM12 12a2.5 2.5 0 1 0 0-5 2.5 2.5 0 0 0 0 5Z',
  '/plantillas': 'M5 4h14v4H5zM5 11h6v9H5zM14 11h5v9h-5z',
  '/ubicacion': 'M12 3v3M12 18v3M3 12h3M18 12h3M12 16a4 4 0 1 0 0-8 4 4 0 0 0 0 8Z',
  '/seguridad': 'M12 3 5 6v5c0 4.4 3 8.2 7 10 4-1.8 7-5.6 7-10V6l-7-3ZM9.5 12l2 2 3.5-4',
  '/equipos': 'M4 8h16v10H4zM9 8V6h6v2M4 13h16'
};
const ICONO_GENERICO = 'M4 6h16M4 12h16M4 18h10';

/** Marco de la aplicación: menú lateral según el rol, usuario con su rol y el contenido de cada ruta. */
@Component({
  selector: 'app-shell',
  standalone: true,
  imports: [RouterOutlet, RouterLink, RouterLinkActive],
  template: `
    <div class="min-h-screen md:flex">
      <aside class="contornos relative flex shrink-0 flex-col bg-bosque-900 text-white md:sticky md:top-0 md:h-screen md:w-64">
        <div class="px-6 pb-4 pt-6">
          <p class="text-xl font-semibold tracking-tight">BioHub</p>
          <p class="text-xs text-white/60">Instituto Nacional de Biodiversidad</p>
        </div>

        <nav class="flex-1 overflow-y-auto px-3 py-2" aria-label="Secciones">
          <ul class="flex gap-1 overflow-x-auto md:block md:space-y-1 md:overflow-visible">
            @for (op of opciones(); track op.ruta) {
              <li class="shrink-0">
                <a
                  [routerLink]="op.ruta"
                  routerLinkActive="bg-white/15 text-white"
                  class="flex items-center gap-3 rounded-lg px-3 py-2 text-white/75 transition-colors hover:bg-white/10 hover:text-white focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-white/60"
                >
                  <svg
                    class="h-5 w-5 shrink-0"
                    viewBox="0 0 24 24"
                    fill="none"
                    stroke="currentColor"
                    stroke-width="1.6"
                    stroke-linecap="round"
                    stroke-linejoin="round"
                    aria-hidden="true"
                  >
                    <path [attr.d]="icono(op.ruta)" />
                  </svg>
                  <span class="whitespace-nowrap">{{ op.etiqueta }}</span>
                </a>
              </li>
            }
          </ul>
        </nav>

        <div class="flex items-center justify-between gap-3 border-t border-white/10 px-6 py-4">
          <div class="min-w-0">
            <p class="truncate text-sm font-medium">{{ auth.usuario() }}</p>
            <p class="text-xs text-white/60">{{ rolLegible() }}</p>
          </div>
          <button
            type="button"
            (click)="auth.logout()"
            class="rounded-lg border border-white/25 px-3 py-1.5 text-sm text-white/90 transition-colors hover:bg-white/10 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-white/60"
          >
            Salir
          </button>
        </div>
      </aside>

      <main class="min-w-0 flex-1 px-5 py-8 md:px-10">
        <div class="mx-auto max-w-5xl">
          <router-outlet />
        </div>
      </main>
    </div>
  `
})
export class ShellComponent {
  auth = inject(AuthService);

  opciones = computed(() => {
    const rol = this.auth.rol();
    return MENU.filter((o) => rol !== null && o.roles.includes(rol));
  });

  rolLegible = computed(() => {
    const rol = this.auth.rol();
    return rol ? rol.charAt(0) + rol.slice(1).toLowerCase() : '';
  });

  icono(ruta: string): string {
    return ICONOS[ruta] ?? ICONO_GENERICO;
  }
}
