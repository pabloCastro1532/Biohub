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
    <header class="mb-8">
      <h1 class="text-2xl font-semibold tracking-tight">Equipos de campo</h1>
      <p class="mt-1 text-musgo">Qué equipos tiene el Instituto y quién responde por cada uno.</p>
    </header>

    <div class="space-y-6">
      @if (aviso()) {
        <p class="aviso-ok" role="status">{{ aviso() }}</p>
      }

      @if (puedeGestionar()) {
        <section class="tarjeta">
          <h2 class="text-lg font-semibold">Registrar equipo</h2>
          <form [formGroup]="form" (ngSubmit)="registrar()" class="mt-4 grid gap-4 md:grid-cols-2">
            <div>
              <label for="codigo" class="etiqueta">Código</label>
              <input id="codigo" formControlName="codigo" maxlength="40" class="campo" />
            </div>
            <div>
              <label for="nombre" class="etiqueta">Nombre</label>
              <input id="nombre" formControlName="nombre" maxlength="120" class="campo" />
            </div>
            <div>
              <label for="tipo" class="etiqueta">Tipo</label>
              <input id="tipo" formControlName="tipo" maxlength="60" class="campo" />
            </div>
            <div>
              <label for="descripcion" class="etiqueta">Descripción (opcional)</label>
              <input id="descripcion" formControlName="descripcion" maxlength="400" class="campo" />
            </div>
            <div class="flex flex-wrap items-center gap-4 md:col-span-2">
              <button type="submit" [disabled]="form.invalid || guardando()" class="btn-primario">
                {{ guardando() ? 'Guardando...' : 'Registrar equipo' }}
              </button>
              @if (errorForm()) {
                <p class="text-sm text-red-700" role="alert">{{ errorForm() }}</p>
              }
            </div>
          </form>
        </section>
      }

      <section class="tarjeta !p-0 overflow-hidden">
        <div class="px-6 pb-2 pt-6">
          <h2 class="text-lg font-semibold">Inventario</h2>
        </div>

        @if (cargando()) {
          <p class="px-6 pb-6 text-musgo">Cargando equipos...</p>
        } @else if (errorCarga()) {
          <p class="mx-6 mb-6 aviso-error" role="alert">{{ errorCarga() }}</p>
        } @else if (equipos().length === 0) {
          <p class="px-6 pb-6 text-musgo">
            Todavía no hay equipos.@if (puedeGestionar()) { Registra el primero con el formulario de arriba.}
          </p>
        } @else {
          <div class="overflow-x-auto px-3 pb-3">
            <table class="tabla">
              <thead>
                <tr>
                  <th>Código</th>
                  <th>Nombre</th>
                  <th>Tipo</th>
                  <th>Responsable actual</th>
                  @if (puedeGestionar()) {
                    <th><span class="sr-only">Acciones</span></th>
                  }
                </tr>
              </thead>
              <tbody>
                @for (e of equipos(); track e.id) {
                  <tr>
                    <td class="font-semibold">{{ e.codigo }}</td>
                    <td>
                      {{ e.nombre }}
                      @if (e.descripcion) {
                        <span class="block text-xs text-musgo">{{ e.descripcion }}</span>
                      }
                    </td>
                    <td><span class="pildora bg-liquen text-musgo">{{ e.tipo }}</span></td>
                    <td>
                      @if (responsables().get(e.id); as r) {
                        <span class="font-medium">{{ r.responsable }}</span>
                        <span class="block text-xs text-musgo">desde {{ r.asignadoEn | date: 'dd/MM/yyyy HH:mm' }}</span>
                      } @else {
                        <span class="pildora bg-ambar-50 text-ambar-700">Sin responsable</span>
                      }
                    </td>
                    @if (puedeGestionar()) {
                      <td class="text-right">
                        <button type="button" (click)="abrirAsignar(e)" class="btn-secundario !px-3 !py-1.5 text-sm">
                          Asignar responsable
                        </button>
                      </td>
                    }
                  </tr>
                  @if (asignando() === e.id) {
                    <tr class="bg-liquen/60">
                      <td colspan="5" class="!py-4">
                        <div class="flex flex-wrap items-end gap-3">
                          <div class="w-full max-w-xs">
                            <label [for]="'resp-' + e.id" class="etiqueta">Usuario del investigador</label>
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
                            class="btn-primario"
                          >
                            Guardar responsable
                          </button>
                          <button type="button" (click)="asignando.set(null)" class="btn-texto">Cancelar</button>
                        </div>
                        @if (errorAsignar()) {
                          <p class="mt-2 text-sm text-red-700" role="alert">{{ errorAsignar() }}</p>
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
  `
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
