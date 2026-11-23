# web

Aplicación local de BioHub: Angular 17 (componentes standalone) y Tailwind 3. Habla solo con el backend por `/api`.

## Correr

Con el servicio científico arriba (`docker compose --profile completo up -d --build` desde `infra/`):

```
npm install
npm start
```

Abre http://localhost:4200. `npm start` usa `proxy.conf.json`, que redirige `/api` a `http://localhost:8081`
(así no hay problemas de CORS en desarrollo). Cuando exista el gateway, el destino pasa a `http://localhost:8080`.

Usuarios de prueba (clave `biohub`): investigador, director, auditor, administrador, seguridad, visitante.

## Estructura

- `src/app/core/auth.service.ts`: login, sesión (sessionStorage) y rol.
- `src/app/core/auth.interceptor.ts`: agrega `Authorization: Bearer` y cierra la sesión ante un 401.
- `src/app/core/auth.guard.ts`: `authGuard` y `rolGuard('DIRECTOR', ...)` para proteger rutas.
- `src/app/core/menu.ts`: menú por rol y la historia de cada pantalla.
- `src/app/app.routes.ts`: cada ruta del menú tiene un marcador de posición.

## Cómo agregar la pantalla de una historia

1. Crea el componente en `src/app/pages/<nombre>/`.
2. En `app.routes.ts`, reemplaza `PendienteComponent` de esa ruta por el tuyo (o agrega la ruta aparte).
3. Para llamar al backend usa `HttpClient` con rutas que empiecen por `/api`: el token se agrega solo.

Requiere Node 20 (Angular 17 pide 18.13 o superior).
