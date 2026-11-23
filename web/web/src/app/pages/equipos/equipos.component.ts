import { DatePipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, computed, inject, signal } from '@angular/core';
import { FormBuilder, FormControl, ReactiveFormsModule, Validators } from '@angular/forms';
import { forkJoin } from 'rxjs';
import { AuthService } from '../../core/auth.service';
import { mensajeDeError } from '../../core/errores';
import { Equipo, EquiposService, Responsable } from './equipos.service';

/** HU-03 (registrar equipo), HU-06 (asignar responsable) y HU-07 (ver responsable actual). */
@Component({
  selector: 'app-equipos',
  standalone: true,
  imports: [ReactiveFormsModule, DatePipe],
  template: `
    <div class="space-y-6">
      @if (aviso()) {
        <p class="rounded-lg bg-bio-100 px-4 py-2 text-sm text-bio-900" role="status">{{ aviso() }}</p>
      }

      @if (puedeGestionar()) {
        <section class="bg-white rounded-xl shadow p-6">
          <h2 class="text-lg font-bold text-bio-700">Registrar equipo</h2>
          <form [formGroup]="form" (ngSubmit)="registrar()" class="mt-4 grid gap-4 md:grid-cols-2">
            <div>
              <label for="codigo" class="block text-sm font-medium mb-1">Código</label>
              <input id="codigo" formControlName="codigo" maxlength="40" class="campo" />
            </div>
            <div>
              <label for="nombre" class="block text-sm font-medium mb-1">Nombre</label>
              <input id="nombre" formControlName="nombre" maxlength="120" class="campo" />
            </div>
            <div>
              <label for="tipo" class="block text-sm font-medium mb-1">Tipo</label>
              <input id="tipo" formControlName="tipo" maxlength="60" class="campo" />
            </div>
            <div>
              <label for="descripcion" class="block text-sm font-medium mb-1">Descripción (opcional)</label>
              <input id="descripcion" formControlName="descripcion" maxlength="400" class="campo" />
            </div>
            <div class="md:col-span-2 flex items-center gap-4">
              <button
                type="submit"
                [disabled]="form.invalid || guardando()"
                class="rounded-lg bg-bio-600 px-4 py-2 font-semibold text-white hover:bg-bio-700 disabled:opacity-50"
              >
                {{ guardando() ? 'Guardando...' : 'Registrar' }}
              </button>
              @if (errorForm()) {
                <p class="text-sm text-red-600" role="alert">{{ errorForm() }}</p>
              }
            </div>
          </form>
        </section>
      }

      <section class="bg-white rounded-xl shadow p-6">
        <h2 class="text-lg font-bold text-bio-700">Equipos de campo</h2>

        @if (cargando()) {
          <p class="mt-4 text-slate-500">Cargando...</p>
        } @else if (errorCarga()) {
          <p class="mt-4 text-sm text-red-600" role="alert">{{ errorCarga() }}</p>
        } @else if (equipos().length === 0) {
          <p class="mt-4 text-slate-500">Todavía no hay equipos registrados.</p>
        } @else {
          <div class="mt-4 overflow-x-auto">
            <table class="w-full text-left text-sm">
              <thead class="border-b border-slate-200 text-slate-500">
                <tr>
                  <th class="py-2 pr-4 font-medium">Código</th>
                  <th class="py-2 pr-4 font-medium">Nombre</th>
                  <th class="py-2 pr-4 font-medium">Tipo</th>
                  <th class="py-2 pr-4 font-medium">Responsable actual</th>
                  @if (puedeGestionar()) {
                    <th class="py-2 font-medium"><span class="sr-only">Acciones</span></th>
                  }
                </tr>
              </thead>
              <tbody>
                @for (e of equipos(); track e.id) {
                  <tr class="border-b border-slate-100">
                    <td class="py-2 pr-4 font-semibold">{{ e.codigo }}</td>
                    <td class="py-2 pr-4">{{ e.nombre }}</td>
                    <td class="py-2 pr-4">{{ e.tipo }}</td>
                    <td class="py-2 pr-4">
                      @if (responsables().get(e.id); as r) {
                        <span class="font-medium">{{ r.responsable }}</span>
                        <span class="block text-xs text-slate-500">
                          desde {{ r.asignadoEn | date: 'dd/MM/yyyy HH:mm' }}
                        </span>
                      } @else {
                        <span class="text-slate-400">Sin responsable</span>
                      }
                    </td>
                    @if (puedeGestionar()) {
                      <td class="py-2 text-right">
                        <button
                          type="button"
                          (click)="abrirAsignar(e)"
                          class="rounded-lg border border-bio-600 px-3 py-1 text-bio-700 hover:bg-bio-50"
                        >
                          Asignar responsable
                        </button>
                      </td>
                    }
                  </tr>
                  @if (asignando() === e.id) {
                    <tr class="bg-bio-50">
                      <td colspan="5" class="px-4 py-3">
                        <div class="flex flex-wrap items-end gap-3">
                          <div>
                            <label [for]="'resp-' + e.id" class="block text-sm font-medium mb-1">
                              Usuario del investigador
                            </label>
                            <input
                              [id]="'resp-' + e.id"
                              [formControl]="usuarioAsignar"
                              (keyup.enter)="guardarAsignacion(e)"
                              class="campo"
                            />
                          </div>
                          <button
                            type="button"
                            (click)="guardarAsignacion(e)"
                            [disabled]="usuarioAsignar.invalid || asignandoEnCurso()"
                            class="rounded-lg bg-bio-600 px-4 py-2 font-semibold text-white hover:bg-bio-700 disabled:opacity-50"
                          >
                            Guardar
                          </button>
                          <button
                            type="button"
                            (click)="asignando.set(null)"
                            class="rounded-lg px-3 py-2 text-slate-600 hover:bg-white"
                          >
                            Cancelar
                          </button>
                        </div>
                        @if (errorAsignar()) {
                          <p class="mt-2 text-sm text-red-600" role="alert">{{ errorAsignar() }}</p>
                        }
                      </td>
                    </tr>
                  }
                }
              </tbody>
            </table>
          </div>
        }
      </section>
    </div>
  `,
  styles: [
    `
      .campo {
        width: 100%;
        border-radius: 0.5rem;
        border: 1px solid rgb(203 213 225);
        padding: 0.5rem 0.75rem;
        background: white;
      }
      .campo:focus {
        outline: none;
        box-shadow: 0 0 0 2px #2f9e63;
      }
    `
  ]
})
export class EquiposComponent {
  private api = inject(EquiposService);
  private fb = inject(FormBuilder);
  private auth = inject(AuthService);

  equipos = signal<Equipo[]>([]);
  responsables = signal<Map<number, Responsable>>(new Map());
  cargando = signal(true);
  errorCarga = signal<string | null>(null);
  aviso = signal<string | null>(null);

  guardando = signal(false);
  errorForm = signal<string | null>(null);

  asignando = signal<number | null>(null);
  asignandoEnCurso = signal(false);
  errorAsignar = signal<string | null>(null);

  /** Registrar equipos y asignar responsables: administrador y director (igual que el backend). */
  puedeGestionar = computed(() => this.auth.tieneRol('ADMINISTRADOR', 'DIRECTOR'));

  form = this.fb.nonNullable.group({
    codigo: ['', [Validators.required, Validators.maxLength(40)]],
    nombre: ['', [Validators.required, Validators.maxLength(120)]],
    tipo: ['', [Validators.required, Validators.maxLength(60)]],
    descripcion: ['', Validators.maxLength(400)]
  });

  usuarioAsignar = new FormControl('', { nonNullable: true, validators: [Validators.required] });

  constructor() {
    this.cargar();
  }

  registrar(): void {
    if (this.form.invalid) {
      return;
    }
    this.guardando.set(true);
    this.errorForm.set(null);
    this.aviso.set(null);
    const v = this.form.getRawValue();

    this.api
      .registrar({ codigo: v.codigo, nombre: v.nombre, tipo: v.tipo, descripcion: v.descripcion.trim() || null })
      .subscribe({
        next: (e) => {
          this.guardando.set(false);
          this.aviso.set(`Equipo ${e.codigo} registrado.`);
          this.form.reset();
          this.cargar();
        },
        error: (err: HttpErrorResponse) => {
          this.guardando.set(false);
          this.errorForm.set(mensajeDeError(err));
        }
      });
  }

  abrirAsignar(e: Equipo): void {
    this.usuarioAsignar.reset('');
    this.errorAsignar.set(null);
    this.asignando.set(e.id);
  }

  guardarAsignacion(e: Equipo): void {
    const usuario = this.usuarioAsignar.value.trim();
    if (!usuario) {
      return;
    }
    this.asignandoEnCurso.set(true);
    this.errorAsignar.set(null);

    this.api.asignar(e.id, usuario).subscribe({
      next: (r) => {
        this.asignandoEnCurso.set(false);
        this.asignando.set(null);
        this.aviso.set(`El equipo ${r.equipoCodigo} quedó a cargo de ${r.responsable}.`);
        this.cargar();
      },
      error: (err: HttpErrorResponse) => {
        this.asignandoEnCurso.set(false);
        this.errorAsignar.set(mensajeDeError(err));
      }
    });
  }

  private cargar(): void {
    this.cargando.set(true);
    forkJoin({ equipos: this.api.listar(), responsables: this.api.responsables() }).subscribe({
      next: ({ equipos, responsables }) => {
        this.equipos.set(equipos);
        this.responsables.set(new Map(responsables.map((r): [number, Responsable] => [r.equipoId, r])));
        this.errorCarga.set(null);
        this.cargando.set(false);
      },
      error: (err: HttpErrorResponse) => {
        this.errorCarga.set(mensajeDeError(err));
        this.cargando.set(false);
      }
    });
  }
}
