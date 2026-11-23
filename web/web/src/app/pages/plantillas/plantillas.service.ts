import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

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

/** Contrato con /api/plantillas (HU-02). */
@Injectable({ providedIn: 'root' })
export class PlantillasService {
  private http = inject(HttpClient);

  especies(): Observable<Especie[]> {
    return this.http.get<Especie[]>('/api/plantillas/especies');
  }

  activaDe(especieId: number): Observable<Plantilla> {
    return this.http.get<Plantilla>(`/api/plantillas/activa/${especieId}`);
  }

  /** Guarda el esquema como una versión nueva, que pasa a ser la activa. */
  editar(id: number, esquema: Record<string, unknown>): Observable<Plantilla> {
    return this.http.put<Plantilla>(`/api/plantillas/${id}`, { esquema });
  }

  /** Comprueba unos atributos contra la plantilla activa de la especie, sin guardar nada. */
  validar(especieId: number, atributos: Record<string, unknown>): Observable<Validacion> {
    return this.http.post<Validacion>(`/api/plantillas/activa/${especieId}/validar`, atributos);
  }
}
