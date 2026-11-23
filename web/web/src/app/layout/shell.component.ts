import { Component, computed, inject } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { AuthService } from '../core/auth.service';
import { MENU } from '../core/menu';

/** Marco de la aplicación: encabezado con usuario y rol, menú según el rol y el contenido de cada ruta. */
@Component({
  selector: 'app-shell',
  standalone: true,
  imports: [RouterOutlet, RouterLink, RouterLinkActive],
  template: `
    <div class="min-h-screen flex flex-col">
      <header class="bg-bio-700 text-white">
        <div class="mx-auto max-w-6xl px-4 py-3 flex items-center justify-between">
          <span class="text-lg font-bold">BioHub</span>
          <div class="flex items-center gap-4 text-sm">
            <span>{{ auth.usuario() }} · <span class="font-semibold">{{ auth.rol() }}</span></span>
            <button (click)="auth.logout()" class="rounded bg-white/15 px-3 py-1 hover:bg-white/25">
              Salir
            </button>
          </div>
        </div>
      </header>

      <div class="mx-auto max-w-6xl w-full flex-1 flex flex-col md:flex-row gap-6 px-4 py-6">
        <nav class="md:w-56 shrink-0">
          <ul class="space-y-1">
            @for (op of opciones(); track op.ruta) {
              <li>
                <a
                  [routerLink]="op.ruta"
                  routerLinkActive="bg-bio-100 text-bio-900 font-semibold"
                  class="block rounded-lg px-3 py-2 text-slate-700 hover:bg-bio-50"
                >
                  {{ op.etiqueta }}
                </a>
              </li>
            }
          </ul>
        </nav>

        <main class="flex-1 min-w-0">
          <router-outlet />
        </main>
      </div>
    </div>
  `
})
export class ShellComponent {
  auth = inject(AuthService);

  opciones = computed(() => {
    const rol = this.auth.rol();
    return MENU.filter((o) => rol !== null && o.roles.includes(rol));
  });
}
