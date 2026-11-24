import { HttpClient } from '@angular/common/http';
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

/** Contrato con GET /api/alertas (solo lectura; las alertas las genera el monitor). */
@Injectable({ providedIn: 'root' })
export class SeguridadService {
  private http = inject(HttpClient);

  listar(): Observable<Alerta[]> {
    return this.http.get<Alerta[]>('/api/alertas');
  }
}
