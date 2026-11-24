import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, map } from 'rxjs';
import { registrarTextos } from '../hallazgos/etiquetas-atributos';

export interface Especie {
  id: number;
  nombreCientifico: string;
  nombreComun: string | null;
}

export type TipoAtributo = 'TEXTO' | 'NUMERO' | 'BOOLEANO' | 'ENUMERADO' | 'FECHA';

/** Un atributo de la plantilla, con lo necesario para armar su campo en un formulario. */
export interface AtributoVista {
  nombre: string;
  tipo: TipoAtributo;
  requerido: boolean;
  valores: string[];
  minimo: number | null;
  maximo: number | null;
  /** Nombre para personas y ayuda que el editor guarda en el esquema; el servidor no los devuelve, se leen de allí. */
  titulo?: string | null;
  descripcion?: string | null;
}

export interface Plantilla {
  id: number;
  especieId: number;
  version: number;
  activa: boolean;
  esquema: Record<string, unknown>;
  atributos: AtributoVista[];
}

export interface Validacion {
  valido: boolean;
  errores: string[];
}

/**
 * El servidor describe el tipo de cada atributo en minúsculas ("booleano", "numero"...) y las pantallas lo comparan
 * en mayúsculas. Se normaliza aquí, en un solo lugar, para que cada campo se arme con su tipo real.
 */
function normalizar(p: Plantilla): Plantilla {
  const propiedades = (p.esquema?.['properties'] ?? {}) as Record<string, { title?: unknown; description?: unknown }>;
  const atributos = p.atributos.map((a) => {
    const prop = propiedades[a.nombre];
    return {
      ...a,
      tipo: a.tipo.toUpperCase() as TipoAtributo,
      titulo: typeof prop?.title === 'string' && prop.title.trim() ? prop.title : null,
      descripcion: typeof prop?.description === 'string' && prop.description.trim() ? prop.description : null
    };
  });
  registrarTextos(atributos);
  return { ...p, atributos };
}

/** Contrato con /api/plantillas (HU-02). */
@Injectable({ providedIn: 'root' })
export class PlantillasService {
  private http = inject(HttpClient);

  especies(): Observable<Especie[]> {
    return this.http.get<Especie[]>('/api/plantillas/especies');
  }

  /** Agrega una especie al catálogo (administrador y director). Nace con una plantilla inicial. */
  crearEspecie(nombreCientifico: string, nombreComun: string): Observable<Especie> {
    return this.http.post<Especie>('/api/especies', { nombreCientifico, nombreComun: nombreComun || null });
  }

  activaDe(especieId: number): Observable<Plantilla> {
    return this.http.get<Plantilla>(`/api/plantillas/activa/${especieId}`).pipe(map(normalizar));
  }

  /** Guarda el esquema como una versión nueva, que pasa a ser la activa. */
  editar(id: number, esquema: Record<string, unknown>): Observable<Plantilla> {
    return this.http.put<Plantilla>(`/api/plantillas/${id}`, { esquema }).pipe(map(normalizar));
  }

  /** Comprueba unos atributos contra la plantilla activa de la especie, sin guardar nada. */
  validar(especieId: number, atributos: Record<string, unknown>): Observable<Validacion> {
    return this.http.post<Validacion>(`/api/plantillas/activa/${especieId}/validar`, atributos);
  }
}
