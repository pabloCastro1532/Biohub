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

/** Datos para editar un hallazgo (HU-08). La especie no cambia. El servidor exige coordenadas y motivo. */
export interface EdicionHallazgo {
  region: string;
  latitud: number;
  longitud: number;
  fechaObservacion: string;
  atributos: Record<string, unknown>;
  motivo: string;
}

/** Una versión del historial (HU-10, HU-11 y HU-12). A propósito no trae coordenadas: solo dice que la "ubicación" cambió. */
export interface VersionHallazgo {
  numero: number;
  especieId: number;
  region: string;
  fechaObservacion: string;
  atributos: Record<string, unknown>;
  autor: string | null;
  motivo: string | null;
  creadoEn: string;
  cambios: string[];
}

/** Contrato con /api/hallazgos (HU-01, HU-08, HU-10, HU-11 y HU-12). */
@Injectable({ providedIn: 'root' })
export class HallazgosService {
  private http = inject(HttpClient);

  listar(): Observable<Hallazgo[]> {
    return this.http.get<Hallazgo[]>('/api/hallazgos');
  }

  registrar(h: NuevoHallazgo): Observable<Hallazgo> {
    return this.http.post<Hallazgo>('/api/hallazgos', h);
  }

  /** HU-08: guarda una versión nueva del hallazgo con el motivo del cambio. */
  editar(id: number, e: EdicionHallazgo): Observable<Hallazgo> {
    return this.http.put<Hallazgo>(`/api/hallazgos/${id}`, e);
  }

  /** HU-10, HU-11 y HU-12: todas las versiones, de la primera a la última. */
  versiones(id: number): Observable<VersionHallazgo[]> {
    return this.http.get<VersionHallazgo[]>(`/api/hallazgos/${id}/versiones`);
  }
}
