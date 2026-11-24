import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

/**
 * Respuesta de una consulta de ubicación. Si el rol no es el de alto rango (director científico), el servidor no manda
 * latitud ni longitud y exacta es false: solo se ve la región (ASR-4).
 */
export interface Ubicacion {
  hallazgoId: number;
  region: string;
  latitud?: number;
  longitud?: number;
  exacta: boolean;
}

/** Contrato con GET /api/hallazgos/{id}/ubicacion (HU-15, HU-16 y HU-17). El servidor registra cada consulta (HU-25). */
@Injectable({ providedIn: 'root' })
export class UbicacionService {
  private http = inject(HttpClient);

  consultar(hallazgoId: number): Observable<Ubicacion> {
    return this.http.get<Ubicacion>(`/api/hallazgos/${hallazgoId}/ubicacion`);
  }
}
