import { Component, computed, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { AuthService, Rol } from '../../core/auth.service';
import { MENU } from '../../core/menu';

type Acciones = Partial<Record<Rol | 'TODOS', string[]>>;

/**
 * Qué puede hacer cada rol en cada sección, dicho en lenguaje de quien usa la aplicación.
 * Debe coincidir con los permisos del servidor: si un rol no está aquí, esa sección no se le lista en el inicio.
 */
const QUE_PUEDES_HACER: Record<string, Acciones> = {
  '/especies': {
    TODOS: ['Buscar especies del inventario', 'Ver la ficha de cada especie, solo con la región del hallazgo']
  },
  '/hallazgos': {
    INVESTIGADOR: [
      'Registrar un hallazgo con su especie, región, fecha y ubicación',
      'Corregir un hallazgo indicando el motivo del cambio',
      'Ver todas las versiones de un hallazgo y quién hizo cada cambio'
    ],
    DIRECTOR: [
      'Registrar un hallazgo con su especie, región, fecha y ubicación',
      'Corregir un hallazgo indicando el motivo del cambio',
      'Ver todas las versiones de un hallazgo y quién hizo cada cambio'
    ],
    AUDITOR: ['Consultar los hallazgos', 'Ver todas las versiones de un hallazgo y quién hizo cada cambio']
  },
  '/plantillas': {
    ADMINISTRADOR: [
      'Ver qué datos se registran de cada especie',
      'Agregar un dato nuevo a una especie sin afectar los hallazgos ya registrados'
    ],
    DIRECTOR: [
      'Ver qué datos se registran de cada especie',
      'Agregar un dato nuevo a una especie sin afectar los hallazgos ya registrados'
    ]
  },
  '/ubicacion': {
    DIRECTOR: ['Consultar la ubicación exacta de un hallazgo; cada consulta queda registrada'],
    AUDITOR: ['Consultar la región de un hallazgo; la ubicación exacta es solo del director y cada consulta queda registrada'],
    SEGURIDAD: ['Consultar la región de un hallazgo; la ubicación exacta es solo del director y cada consulta queda registrada']
  },
  '/accesos': {
    SEGURIDAD: [
      'Ver quién consultó ubicaciones exactas, cuándo y si se le aceptó o se le negó',
      'Filtrar los accesos por resultado o por usuario'
    ],
    AUDITOR: [
      'Ver quién consultó ubicaciones exactas, cuándo y si se le aceptó o se le negó',
      'Filtrar los accesos por resultado o por usuario'
    ]
  },
  '/seguridad': {
    SEGURIDAD: [
      'Recibir alertas cuando alguien intenta consultar ubicaciones sin permiso',
      'Revocar el acceso de un usuario',
      'Ver qué ubicaciones quedaron expuestas'
    ]
  },
  '/equipos': {
    ADMINISTRADOR: [
      'Registrar equipos de campo',
      'Asignar un responsable a cada equipo',
      'Prestar un equipo a un grupo y registrar cuando lo devuelve',
      'Ver el historial de préstamos de cada equipo'
    ],
    DIRECTOR: [
      'Registrar equipos de campo',
      'Asignar un responsable a cada equipo',
      'Prestar un equipo a un grupo y registrar cuando lo devuelve',
      'Ver el historial de préstamos de cada equipo'
    ],
    INVESTIGADOR: [
      'Ver qué equipos hay y quién responde por cada uno',
      'Ver a qué grupo está prestado cada equipo y su historial de préstamos'
    ]
  }
};

@Component({
  selector: 'app-inicio',
  standalone: true,
  imports: [RouterLink],
  template: `
    <header class="mb-8">
      <h1 class="text-2xl font-semibold tracking-tight">Hola, {{ auth.usuario() }}</h1>
      <p class="mt-1 text-musgo">Entraste como {{ rolLegible() }}. Esto es lo que puedes hacer en BioHub:</p>
    </header>

    <ul class="grid gap-4 sm:grid-cols-2">
      @for (s of secciones(); track s.ruta) {
        <li class="rounded-xl border border-linea bg-white p-5">
          <a
            [routerLink]="s.ruta"
            class="font-semibold text-bio-700 hover:underline focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-bio-500"
          >
            {{ s.etiqueta }}
          </a>
          <ul class="mt-3 list-disc space-y-1.5 pl-5 text-sm marker:text-bio-500">
            @for (a of s.acciones; track a) {
              <li>{{ a }}</li>
            }
          </ul>
        </li>
      }
    </ul>
  `
})
export class InicioComponent {
  auth = inject(AuthService);

  rolLegible = computed(() => (this.auth.rol() ?? '').toLowerCase());

  /** Las secciones del menú a las que entra este rol y en las que tiene algo que hacer. */
  secciones = computed(() => {
    const rol = this.auth.rol();
    if (rol === null) {
      return [];
    }
    return MENU.filter((o) => o.ruta !== '/inicio' && o.roles.includes(rol))
      .map((o) => ({
        ruta: o.ruta,
        etiqueta: o.etiqueta,
        acciones: QUE_PUEDES_HACER[o.ruta]?.[rol] ?? QUE_PUEDES_HACER[o.ruta]?.TODOS ?? []
      }))
      .filter((s) => s.acciones.length > 0);
  });
}
