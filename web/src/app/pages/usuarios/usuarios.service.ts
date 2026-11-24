import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { Rol } from '../../core/auth.service';

/** Una cuenta tal como la muestra el backend: nunca trae la clave. */
export interface Usuario {
  id: number;
  nombreUsuario: string;
  rol: Rol;
  activo: boolean;
  creadoEn: string;
}

/**
 * Contrato con el backend (solo ADMINISTRADOR):
 *   GET  /api/usuarios                    lista de cuentas
 *   POST /api/usuarios                    crea una cuenta {nombreUsuario, rol, clave} -> 201
 *   POST /api/usuarios/{id}/reactivar     devuelve el acceso a una cuenta revocada -> 204
 * Las reglas (nombre, rol, clave, sin repetir) las valida el servidor y responde 400 o 409 con un mensaje claro.
 */
@Injectable({ providedIn: 'root' })
export class UsuariosService {
  private http = inject(HttpClient);

  listar(): Observable<Usuario[]> {
    return this.http.get<Usuario[]>('/api/usuarios');
  }

  crear(nombreUsuario: string, rol: Rol, clave: string): Observable<Usuario> {
    return this.http.post<Usuario>('/api/usuarios', { nombreUsuario, rol, clave });
  }

  reactivar(id: number): Observable<void> {
    return this.http.post<void>(`/api/usuarios/${id}/reactivar`, {});
  }
}
