import { Component, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { AuthService } from '../../core/auth.service';
import { EspeciesComponent } from '../especies/especies.component';

/**
 * Portada pública (HU-18 y HU-19): cualquier persona busca especies sin iniciar sesión. Solo se muestra la región o
 * el departamento, nunca la ubicación exacta (ASR-4). Quien trabaja en el Instituto entra con "Iniciar sesión".
 */
@Component({
  selector: 'app-portada',
  standalone: true,
  imports: [RouterLink, EspeciesComponent],
  template: `
    <div class="min-h-screen bg-liquen/40">
      <section class="contornos bg-bosque-900 text-white">
        <div class="mx-auto flex max-w-5xl items-center justify-between gap-4 px-5 py-5 md:px-10">
          <div>
            <p class="text-xl font-semibold tracking-tight">BioHub</p>
            <p class="text-xs text-white/60">Instituto Nacional de Biodiversidad</p>
          </div>
          @if (auth.estaAutenticado()) {
            <a routerLink="/inicio" class="btn border border-white/30 bg-white text-bosque-900 hover:bg-white/90">Ir a mi cuenta</a>
          } @else {
            <a routerLink="/login" class="btn border border-white/30 bg-white text-bosque-900 hover:bg-white/90">Iniciar sesión</a>
          }
        </div>

        <div class="mx-auto max-w-5xl px-5 pb-12 pt-6 md:px-10 md:pb-16">
          <h1 class="max-w-2xl text-3xl font-semibold leading-tight tracking-tight md:text-4xl">
            Conoce la biodiversidad del país.
          </h1>
          <p class="mt-4 max-w-xl text-white/75">
            Busca una especie por su nombre y mira en qué departamentos se ha encontrado. Para proteger a las especies,
            nunca se muestra el lugar exacto del hallazgo.
          </p>
        </div>
      </section>

      <main class="mx-auto max-w-5xl px-5 py-8 md:px-10">
        <app-especies />
      </main>

      <footer class="mx-auto max-w-5xl px-5 pb-10 text-sm text-musgo md:px-10">
        <p>
          ¿Trabajas en el Instituto?
          @if (auth.estaAutenticado()) {
            <a routerLink="/inicio" class="font-medium text-bio-700 hover:underline">Ir a mi cuenta</a>.
          } @else {
            <a routerLink="/login" class="font-medium text-bio-700 hover:underline">Inicia sesión</a> para registrar
            hallazgos y gestionar el inventario.
          }
        </p>
      </footer>
    </div>
  `
})
export class PortadaComponent {
  auth = inject(AuthService);
}
