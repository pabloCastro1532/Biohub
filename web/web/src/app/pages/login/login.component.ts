import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '../../core/auth.service';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [ReactiveFormsModule],
  template: `
    <div class="min-h-screen flex items-center justify-center bg-bio-50 px-4">
      <form
        [formGroup]="form"
        (ngSubmit)="entrar()"
        class="w-full max-w-sm bg-white rounded-xl shadow p-8 space-y-5"
      >
        <div class="text-center">
          <h1 class="text-2xl font-bold text-bio-700">BioHub</h1>
          <p class="text-sm text-slate-500">Instituto Nacional de Biodiversidad</p>
        </div>

        <div>
          <label for="usuario" class="block text-sm font-medium mb-1">Usuario</label>
          <input
            id="usuario"
            formControlName="usuario"
            autocomplete="username"
            class="w-full rounded-lg border border-slate-300 px-3 py-2 focus:outline-none focus:ring-2 focus:ring-bio-500"
          />
        </div>

        <div>
          <label for="clave" class="block text-sm font-medium mb-1">Clave</label>
          <input
            id="clave"
            type="password"
            formControlName="clave"
            autocomplete="current-password"
            class="w-full rounded-lg border border-slate-300 px-3 py-2 focus:outline-none focus:ring-2 focus:ring-bio-500"
          />
        </div>

        @if (error()) {
          <p class="text-sm text-red-600" role="alert">{{ error() }}</p>
        }

        <button
          type="submit"
          [disabled]="form.invalid || cargando()"
          class="w-full rounded-lg bg-bio-600 px-4 py-2 font-semibold text-white hover:bg-bio-700 disabled:opacity-50"
        >
          {{ cargando() ? 'Ingresando...' : 'Ingresar' }}
        </button>
      </form>
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
