import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

/** Una especie en los resultados de la búsqueda. A propósito no hay coordenadas en ningún tipo de este archivo (ASR-4). */
export interface EspecieResumen {
  id: number;
  nombreCientifico: string;
  nombreComun: string | null;
  totalHallazgos: number;
  ultimaObservacion: string | null;
}

export interface RegionConteo {
  region: string;
  total: number;
}

export interface HallazgoReciente {
  hallazgoId: number;
  region: string;
  fecha: string;
  atributos: Record<string, unknown>;
}

export interface FichaEspecie {
  id: number;
  nombreCientifico: string;
  nombreComun: string | null;
  totalHallazgos: number;
  ultimaObservacion: string | null;
  regiones: RegionConteo[];
  recientes: HallazgoReciente[];
}

/**
 * Contrato con el servicio de consulta pública (HU-18 y HU-19):
 *   GET /api/publico/especies?q=&region=&limite=   búsqueda
 *   GET /api/publico/especies/{id}                 ficha
 * Es de solo lectura y abierto a cualquier rol.
 */
@Injectable({ providedIn: 'root' })
export class EspeciesService {
  private http = inject(HttpClient);

  buscar(texto: string, region: string): Observable<EspecieResumen[]> {
    let params = new HttpParams();
    if (texto.trim()) {
      params = params.set('q', texto.trim());
    }
    if (region.trim()) {
      params = params.set('region', region.trim());
    }
    return this.http.get<EspecieResumen[]>('/api/publico/especies', { params });
  }

  ficha(id: number): Observable<FichaEspecie> {
    return this.http.get<FichaEspecie>(`/api/publico/especies/${id}`);
  }
}
