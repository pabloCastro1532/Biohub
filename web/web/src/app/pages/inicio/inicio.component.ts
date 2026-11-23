import { Component, inject } from '@angular/core';
import { AuthService } from '../../core/auth.service';

@Component({
  selector: 'app-inicio',
  standalone: true,
  template: `
    <section class="bg-white rounded-xl shadow p-8">
      <h2 class="text-xl font-bold text-bio-700">Bienvenido, {{ auth.usuario() }}</h2>
      <p class="mt-2 text-slate-600">
        Tu rol es <span class="font-semibold">{{ auth.rol() }}</span>. Usa el menú de la izquierda
        para ir a las secciones que tienes disponibles.
      </p>
    </section>
  `
})
export class InicioComponent {
  auth = inject(AuthService);
}
