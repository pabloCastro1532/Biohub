import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

export type ResultadoAcceso = 'ACEPTADO' | 'RECHAZADO';

/** Una consulta de ubicación exacta. A propósito no trae coordenadas: solo quién, qué hallazgo y con qué resultado. */
export interface Acceso {
  id: number;
  usuarioId: number;
  usuario: string;
  hallazgoId: number;
  resultado: ResultadoAcceso;
  ocurridoEn: string;
}

/** Contrato con GET /api/accesos (HU-25). Los registros los escribe el servidor; la pantalla solo los lee. */
@Injectable({ providedIn: 'root' })
export class AccesosService {
  private http = inject(HttpClient);

  /** Hasta 200 accesos, el más reciente primero. */
  listar(resultado: ResultadoAcceso | null): Observable<Acceso[]> {
    let params = new HttpParams();
    if (resultado) {
      params = params.set('resultado', resultado);
    }
    return this.http.get<Acceso[]>('/api/accesos', { params });
  }
}
