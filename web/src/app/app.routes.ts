import { Type } from '@angular/core';
import { Routes } from '@angular/router';
import { EspeciesComponent } from './pages/especies/especies.component';
import { SeguridadComponent } from './pages/seguridad/seguridad.component';
import { authGuard, rolGuard } from './core/auth.guard';
import { MENU } from './core/menu';
import { ShellComponent } from './layout/shell.component';
import { InicioComponent } from './pages/inicio/inicio.component';
import { LoginComponent } from './pages/login/login.component';
import { AccesosComponent } from './pages/accesos/accesos.component';
import { EquiposComponent } from './pages/equipos/equipos.component';
import { HallazgosComponent } from './pages/hallazgos/hallazgos.component';
import { PendienteComponent } from './pages/pendiente/pendiente.component';
import { PlantillasComponent } from './pages/plantillas/plantillas.component';
import { SinPermisoComponent } from './pages/sin-permiso/sin-permiso.component';
import { UbicacionComponent } from './pages/ubicacion/ubicacion.component';

/**
 * Pantalla real de cada ruta del menú. Cada integrante agrega UNA línea aquí al construir su historia;
 * las rutas que no están en esta lista muestran el marcador de posición.
 */
const PANTALLAS: Record<string, Type<unknown>> = {
  '/especies': EspeciesComponent,
  '/seguridad': SeguridadComponent,
  '/accesos': AccesosComponent,
  '/equipos': EquiposComponent,
  '/hallazgos': HallazgosComponent,
  '/plantillas': PlantillasComponent,
  '/ubicacion': UbicacionComponent
};

/** Rutas protegidas por rol, tomadas del menú. */
const rutasDeMenu: Routes = MENU.filter((o) => o.ruta !== '/inicio').map((o) => ({
  path: o.ruta.slice(1),
  component: PANTALLAS[o.ruta] ?? PendienteComponent,
  canActivate: [rolGuard(...o.roles)],
  data: { titulo: o.etiqueta, historia: o.historia }
}));

export const routes: Routes = [
  { path: 'login', component: LoginComponent },
  {
    path: '',
    component: ShellComponent,
    canActivate: [authGuard],
    children: [
      { path: '', pathMatch: 'full', redirectTo: 'inicio' },
      { path: 'inicio', component: InicioComponent },
      ...rutasDeMenu,
      { path: 'sin-permiso', component: SinPermisoComponent }
    ]
  },
  { path: '**', redirectTo: '' }
];
