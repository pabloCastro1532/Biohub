import { Component, inject } from '@angular/core';
import { ActivatedRoute } from '@angular/router';

/**
 * Marcador de posición de las pantallas que aún no existen. Cada historia agrega su pantalla
 * en el mapa PANTALLAS de app.routes.ts.
 */
@Component({
  selector: 'app-pendiente',
  standalone: true,
  template: `
    <header class="mb-6">
      <h1 class="text-2xl font-semibold tracking-tight">{{ titulo }}</h1>
      <p class="mt-1 text-musgo">
        Esta pantalla todavía no está lista@if (historia) {
          <span> ({{ historia }})</span>
        }.
      </p>
    </header>
  `
})
export class PendienteComponent {
  private ruta = inject(ActivatedRoute);
  titulo: string = this.ruta.snapshot.data['titulo'] ?? '';
  historia: string = this.ruta.snapshot.data['historia'] ?? '';
}
