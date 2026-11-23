import { Component, computed, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { AuthService } from '../../core/auth.service';
import { MENU } from '../../core/menu';

@Component({
  selector: 'app-inicio',
  standalone: true,
  imports: [RouterLink],
  template: `
    <header class="mb-8">
      <h1 class="text-2xl font-semibold tracking-tight">Hola, {{ auth.usuario() }}</h1>
      <p class="mt-1 text-musgo">Entraste como {{ rolLegible() }}. Estas son tus secciones.</p>
    </header>

    <ul class="grid gap-4 sm:grid-cols-2">
      @for (op of secciones(); track op.ruta) {
        <li>
          <a
            [routerLink]="op.ruta"
            class="block h-full rounded-xl border border-linea bg-white p-5 transition-colors hover:border-bio-600 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-bio-500"
          >
            <span class="font-semibold text-bio-700">{{ op.etiqueta }}</span>
            <span class="mt-1 block text-sm text-musgo">{{ op.historia }}</span>
          </a>
        </li>
      }
    </ul>
  `
})
export class InicioComponent {
  auth = inject(AuthService);

  rolLegible = computed(() => (this.auth.rol() ?? '').toLowerCase());

  secciones = computed(() => {
    const rol = this.auth.rol();
    return MENU.filter((o) => o.ruta !== '/inicio' && rol !== null && o.roles.includes(rol));
  });
}
