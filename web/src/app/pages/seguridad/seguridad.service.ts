import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

export type TipoAlerta = 'CONSULTAS_EXCESIVAS' | 'RECHAZOS_REPETIDOS';

/** Una alerta del monitor de seguridad, con el usuario ya resuelto (HU-26). */
export interface Alerta {
  id: number;
  usuarioId: number;
  usuario: string;
  tipo: TipoAlerta;
  cantidad: number;
  ventanaMinutos: number;
  generadaEn: string;
  enviada: boolean;
}

/** Un hallazgo cuyas coordenadas exactas vio un usuario, con cuántas veces y cuándo fue la última (HU-27). */
export interface Exposicion {
  hallazgoId: number;
  consultas: number;
  ultimaConsulta: string;
}

/**
 * Contrato con el backend de seguridad:
 *   GET  /api/alertas                              alertas del monitor (solo lectura; las genera el monitor)
 *   POST /api/seguridad/usuarios/{id}/revocar      revoca el acceso del usuario (HU-27)
 *   GET  /api/seguridad/usuarios/{id}/exposicion   coordenadas que vio en un periodo (HU-27)
 */
@Injectable({ providedIn: 'root' })
export class SeguridadService {
  private http = inject(HttpClient);

  listar(): Observable<Alerta[]> {
    return this.http.get<Alerta[]>('/api/alertas');
  }

  /** El usuario queda desactivado y recibe 401 en su siguiente petición. */
  revocar(usuarioId: number): Observable<void> {
    return this.http.post<void>(`/api/seguridad/usuarios/${usuarioId}/revocar`, {});
  }

  /** Fechas en formato "2026-11-24T00:00:00" (hora de Bogotá); sin ellas, el servidor usa los últimos 30 días. */
  exposicion(usuarioId: number, desde?: string, hasta?: string): Observable<Exposicion[]> {
    let params = new HttpParams();
    if (desde) {
      params = params.set('desde', desde);
    }
    if (hasta) {
      params = params.set('hasta', hasta);
    }
    return this.http.get<Exposicion[]>(`/api/seguridad/usuarios/${usuarioId}/exposicion`, { params });
  }
}
