import { Rol } from './auth.service';

export interface OpcionMenu {
  ruta: string;
  etiqueta: string;
  /** Historia de usuario a la que corresponde la pantalla (se muestra en el marcador de posición). */
  historia: string;
  roles: Rol[];
}

const TODOS: Rol[] = ['INVESTIGADOR', 'DIRECTOR', 'AUDITOR', 'ADMINISTRADOR', 'SEGURIDAD', 'VISITANTE'];

/**
 * Menú por rol. Cada integrante reemplaza el marcador de posición de su ruta por su pantalla real
 * en app.routes.ts, sin tocar este archivo. Ajustar los roles aquí si el equipo decide otra cosa.
 */
export const MENU: OpcionMenu[] = [
  { ruta: '/inicio', etiqueta: 'Inicio', historia: '', roles: TODOS },
  { ruta: '/especies', etiqueta: 'Buscar especies', historia: 'HU-18 y HU-19', roles: TODOS },
  { ruta: '/hallazgos', etiqueta: 'Hallazgos', historia: 'HU-01, 08, 10, 11 y 12', roles: ['INVESTIGADOR', 'DIRECTOR', 'AUDITOR', 'ADMINISTRADOR'] },
  { ruta: '/plantillas', etiqueta: 'Plantillas por especie', historia: 'HU-02', roles: ['ADMINISTRADOR', 'DIRECTOR'] },
  { ruta: '/ubicacion', etiqueta: 'Ubicación', historia: 'HU-15, 16, 17 y 25', roles: ['DIRECTOR', 'AUDITOR', 'SEGURIDAD'] },
  { ruta: '/seguridad', etiqueta: 'Seguridad y revocación', historia: 'HU-26 y HU-27', roles: ['SEGURIDAD', 'ADMINISTRADOR'] },
  { ruta: '/equipos', etiqueta: 'Equipos de campo', historia: 'HU-03 a 07 y HU-24', roles: ['INVESTIGADOR', 'DIRECTOR', 'ADMINISTRADOR'] }
];
