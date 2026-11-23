import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

export interface Equipo {
  id: number;
  codigo: string;
  nombre: string;
  tipo: string;
  descripcion: string | null;
  activo: boolean;
}

export interface NuevoEquipo {
  codigo: string;
  nombre: string;
  tipo: string;
  descripcion: string | null;
}

/** Responsable vigente de un equipo (HU-07). Si no tiene, los tres últimos campos vienen nulos. */
export interface Responsable {
  equipoId: number;
  equipoCodigo: string;
  responsable: string | null;
  asignadoPor: string | null;
  asignadoEn: string | null;
}

/** Contrato con /api/equipos (HU-03, HU-06 y HU-07). */
@Injectable({ providedIn: 'root' })
export class EquiposService {
  private http = inject(HttpClient);

  listar(): Observable<Equipo[]> {
    return this.http.get<Equipo[]>('/api/equipos');
  }

  registrar(equipo: NuevoEquipo): Observable<Equipo> {
    return this.http.post<Equipo>('/api/equipos', equipo);
  }

  /** Los equipos que hoy tienen responsable. */
  responsables(): Observable<Responsable[]> {
    return this.http.get<Responsable[]>('/api/equipos/responsables');
  }

  asignar(equipoId: number, usuario: string): Observable<Responsable> {
    return this.http.put<Responsable>(`/api/equipos/${equipoId}/responsable`, { usuario });
  }
}
