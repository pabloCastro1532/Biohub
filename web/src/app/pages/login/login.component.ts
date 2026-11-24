import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../core/auth.service';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [ReactiveFormsModule, RouterLink],
  template: `
    <div class="min-h-screen md:grid md:grid-cols-[1.1fr_1fr]">
      <section class="contornos flex flex-col justify-between bg-bosque-900 px-8 py-10 text-white md:px-14 md:py-14">
        <a routerLink="/" class="text-xl font-semibold tracking-tight hover:underline focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-white/60">BioHub</a>
        <div class="my-12 max-w-md md:my-0">
          <h1 class="text-3xl font-semibold leading-tight tracking-tight md:text-4xl">
            El inventario de la biodiversidad del país, en un solo lugar.
          </h1>
          <p class="mt-4 text-white/70">
            Hallazgos, plantillas por especie y equipos de campo. La ubicación exacta solo la ve quien tiene el rol para verla.
          </p>
        </div>
        <p class="text-sm text-white/50">Instituto Nacional de Biodiversidad</p>
      </section>

      <section class="flex items-center justify-center px-6 py-12">
        <form [formGroup]="form" (ngSubmit)="entrar()" class="w-full max-w-sm space-y-5">
          <div>
            <h2 class="text-2xl font-semibold tracking-tight">Ingresar</h2>
            <p class="mt-1 text-sm text-musgo">Usa el usuario y la clave que te dio el Instituto.</p>
          </div>

          <div>
            <label for="usuario" class="etiqueta">Usuario</label>
            <input id="usuario" formControlName="usuario" autocomplete="username" class="campo" />
          </div>

          <div>
            <label for="clave" class="etiqueta">Clave</label>
            <input id="clave" type="password" formControlName="clave" autocomplete="current-password" class="campo" />
          </div>

          @if (error()) {
            <p class="aviso-error" role="alert">{{ error() }}</p>
          }

          <button type="submit" [disabled]="form.invalid || cargando()" class="btn-primario w-full">
            {{ cargando() ? 'Ingresando...' : 'Ingresar' }}
          </button>

          <p class="text-center text-sm">
            <a routerLink="/" class="font-medium text-bio-700 hover:underline">Volver a la búsqueda de especies</a>
          </p>
        </form>
      </section>
    </div>
  `
})
export class LoginComponent {
  private fb = inject(FormBuilder);
  private auth = inject(AuthService);
  private router = inject(Router);

  cargando = signal(false);
  error = signal<string | null>(null);

  form = this.fb.nonNullable.group({
    usuario: ['', Validators.required],
    clave: ['', Validators.required]
  });

  entrar(): void {
    if (this.form.invalid) {
      return;
    }
    this.cargando.set(true);
    this.error.set(null);
    const { usuario, clave } = this.form.getRawValue();

    this.auth.login(usuario, clave).subscribe({
      next: () => this.router.navigate(['/inicio']),
      error: (e: HttpErrorResponse) => {
        this.cargando.set(false);
        this.error.set(
          e.status === 401
            ? 'Usuario o clave incorrectos.'
            : 'No se pudo conectar con el servidor. Intenta de nuevo.'
        );
      }
    });
  }
}
