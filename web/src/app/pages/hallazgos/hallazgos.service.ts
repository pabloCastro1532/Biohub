import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

/** Un hallazgo con su versión más reciente. A propósito no trae la ubicación exacta (solo la región). */
export interface Hallazgo {
  id: number;
  version: number;
  especieId: number;
  region: string;
  fechaObservacion: string;
  atributos: Record<string, unknown>;
  autor: string | null;
  creadoEn: string;
}

export interface NuevoHallazgo {
  especieId: number;
  region: string;
  latitud: number;
  longitud: number;
  fechaObservacion: string;
  atributos: Record<string, unknown>;
}

/** Contrato con /api/hallazgos (HU-01). */
@Injectable({ providedIn: 'root' })
export class HallazgosService {
  private http = inject(HttpClient);

  listar(): Observable<Hallazgo[]> {
    return this.http.get<Hallazgo[]>('/api/hallazgos');
  }

  registrar(h: NuevoHallazgo): Observable<Hallazgo> {
    return this.http.post<Hallazgo>('/api/hallazgos', h);
  }
}
