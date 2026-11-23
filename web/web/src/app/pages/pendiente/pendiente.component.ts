import { Component, inject } from '@angular/core';
import { ActivatedRoute } from '@angular/router';

/**
 * Marcador de posición de las pantallas que aún no existen. Cada historia reemplaza su ruta en
 * app.routes.ts por su propio componente.
 */
@Component({
  selector: 'app-pendiente',
  standalone: true,
  template: `
    <section class="bg-white rounded-xl shadow p-8">
      <h2 class="text-xl font-bold text-bio-700">{{ titulo }}</h2>
      <p class="mt-2 text-slate-600">
        Pantalla pendiente de construir
        @if (historia) {
          <span>({{ historia }})</span>
        }.
      </p>
    </section>
  `
})
export class PendienteComponent {
  private ruta = inject(ActivatedRoute);
  titulo: string = this.ruta.snapshot.data['titulo'] ?? '';
  historia: string = this.ruta.snapshot.data['historia'] ?? '';
}
