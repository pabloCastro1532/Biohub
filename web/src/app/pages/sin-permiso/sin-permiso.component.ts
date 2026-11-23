import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';

@Component({
  selector: 'app-sin-permiso',
  standalone: true,
  imports: [RouterLink],
  template: `
    <header class="mb-6">
      <h1 class="text-2xl font-semibold tracking-tight">Esta sección no está disponible para tu rol</h1>
      <p class="mt-1 text-musgo">Si crees que deberías tener acceso, pídeselo al administrador.</p>
    </header>
    <a routerLink="/inicio" class="btn-secundario">Volver al inicio</a>
  `
})
export class SinPermisoComponent {}
