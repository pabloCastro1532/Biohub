import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';

@Component({
  selector: 'app-sin-permiso',
  standalone: true,
  imports: [RouterLink],
  template: `
    <section class="bg-white rounded-xl shadow p-8">
      <h2 class="text-xl font-bold text-red-700">Sin permiso</h2>
      <p class="mt-2 text-slate-600">Tu rol no puede acceder a esta sección.</p>
      <a routerLink="/inicio" class="mt-4 inline-block text-bio-700 underline">Volver al inicio</a>
    </section>
  `
})
export class SinPermisoComponent {}
