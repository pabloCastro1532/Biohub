import { HttpClient } from '@angular/common/http';
import { Injectable, computed, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { Observable, tap } from 'rxjs';

/** Los seis roles de la Entrega 2 (el backend los envía en mayúsculas). */
export type Rol =
  | 'INVESTIGADOR'
  | 'DIRECTOR'
  | 'AUDITOR'
  | 'ADMINISTRADOR'
  | 'SEGURIDAD'
  | 'VISITANTE';

interface LoginRespuesta {
  token: string;
  rol: Rol;
}

interface Sesion {
  token: string;
  rol: Rol;
  usuario: string;
}

const CLAVE_SESION = 'biohub.sesion';

/**
 * Contrato con el backend: POST /api/auth/login {usuario, clave} -> {token, rol}.
 * El token se manda en "Authorization: Bearer ..." (lo hace el interceptor).
 */
@Injectable({ providedIn: 'root' })
export class AuthService {
  private http = inject(HttpClient);
  private router = inject(Router);

  private sesion = signal<Sesion | null>(this.leerSesion());

  readonly estaAutenticado = computed(() => this.sesion() !== null);
  readonly rol = computed(() => this.sesion()?.rol ?? null);
  readonly usuario = computed(() => this.sesion()?.usuario ?? null);

  login(usuario: string, clave: string): Observable<LoginRespuesta> {
    return this.http.post<LoginRespuesta>('/api/auth/login', { usuario, clave }).pipe(
      tap((r) => this.guardar({ token: r.token, rol: r.rol, usuario }))
    );
  }

  logout(): void {
    this.sesion.set(null);
    sessionStorage.removeItem(CLAVE_SESION);
    this.router.navigate(['/login']);
  }

  token(): string | null {
    return this.sesion()?.token ?? null;
  }

  tieneRol(...roles: Rol[]): boolean {
    const actual = this.rol();
    return actual !== null && roles.includes(actual);
  }

  private guardar(s: Sesion): void {
    this.sesion.set(s);
    try {
      sessionStorage.setItem(CLAVE_SESION, JSON.stringify(s));
    } catch {
      // sin almacenamiento disponible: la sesión vive solo en memoria
    }
  }

  private leerSesion(): Sesion | null {
    try {
      const crudo = sessionStorage.getItem(CLAVE_SESION);
      return crudo ? (JSON.parse(crudo) as Sesion) : null;
    } catch {
      return null;
    }
  }
}
