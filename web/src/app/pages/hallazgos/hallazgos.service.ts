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

/** Datos para reclasificar un hallazgo (HU-09): la especie nueva, sus atributos y el motivo. Lugar y fecha se conservan. */
export interface Reclasificacion {
  especieId: number;
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

/** Datos de una imagen de un hallazgo (sin el archivo, que se pide aparte). Las imágenes no llevan la ubicación de la foto. */
export interface ImagenHallazgo {
  id: number;
  nombreArchivo: string;
  tipoContenido: string;
  tamano: number;
  autor: string | null;
  creadaEn: string;
}

/** Contrato con /api/hallazgos (HU-01, HU-08, HU-09, HU-10, HU-11 y HU-12) y sus imágenes. */
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

  /** HU-09: cambia la especie del hallazgo creando una versión nueva (solo el director). */
  reclasificar(id: number, r: Reclasificacion): Observable<Hallazgo> {
    return this.http.post<Hallazgo>(`/api/hallazgos/${id}/reclasificar`, r);
  }

  /** HU-10, HU-11 y HU-12: todas las versiones, de la primera a la última. */
  versiones(id: number): Observable<VersionHallazgo[]> {
    return this.http.get<VersionHallazgo[]>(`/api/hallazgos/${id}/versiones`);
  }

  /** Imágenes de un hallazgo, de la más antigua a la más nueva. */
  imagenes(id: number): Observable<ImagenHallazgo[]> {
    return this.http.get<ImagenHallazgo[]>(`/api/hallazgos/${id}/imagenes`);
  }

  /** Sube una imagen JPG o PNG (hasta 2 MB) y devuelve la lista actualizada. Solo investigador y director. */
  subirImagen(id: number, archivo: File): Observable<ImagenHallazgo[]> {
    const datos = new FormData();
    datos.append('archivo', archivo);
    return this.http.post<ImagenHallazgo[]>(`/api/hallazgos/${id}/imagenes`, datos);
  }

  /** El archivo de una imagen. Se pide con HttpClient (y no con un enlace directo) para que viaje el token de sesión. */
  verImagen(id: number, imagenId: number): Observable<Blob> {
    return this.http.get(`/api/hallazgos/${id}/imagenes/${imagenId}`, { responseType: 'blob' });
  }
}
