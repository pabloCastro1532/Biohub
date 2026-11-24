import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

/** Un préstamo de un equipo a un grupo. Mientras devueltoEn sea nulo, el equipo sigue prestado. */
export interface Prestamo {
  id: number;
  equipoId: number;
  equipoCodigo: string | null;
  grupo: string;
  observacion: string | null;
  prestadoPor: string;
  prestadoEn: string;
  devueltoEn: string | null;
  devueltoPor: string | null;
}

export interface NuevoPrestamo {
  equipoId: number;
  grupo: string;
  observacion: string | null;
}

/** Contrato con /api/prestamos (HU-04 y HU-05) y /api/equipos/{id}/prestamos (HU-24). */
@Injectable({ providedIn: 'root' })
export class PrestamosService {
  private http = inject(HttpClient);

  listar(soloAbiertos: boolean): Observable<Prestamo[]> {
    const params = new HttpParams().set('abiertos', soloAbiertos);
    return this.http.get<Prestamo[]>('/api/prestamos', { params });
  }

  /** Si el equipo ya está prestado, el servidor responde 409 con el grupo que lo tiene (HU-05). */
  prestar(p: NuevoPrestamo): Observable<Prestamo> {
    return this.http.post<Prestamo>('/api/prestamos', p);
  }

  /** HU-24: todos los préstamos de un equipo, el más reciente primero. */
  historialDeEquipo(equipoId: number): Observable<Prestamo[]> {
    return this.http.get<Prestamo[]>(`/api/equipos/${equipoId}/prestamos`);
  }

  devolver(id: number): Observable<Prestamo> {
    return this.http.post<Prestamo>(`/api/prestamos/${id}/devolucion`, {});
  }
}
