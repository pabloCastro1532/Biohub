# servicio-cientifico

Esqueleto del sábado (Yonnatan). Spring Boot 3.4 + Java 21 + Maven. Puerto 8081.

## Correr

Necesita MySQL (`cientifica`) y RabbitMQ del `docker compose` de Santiago. Mientras tanto:

```
mvn test                 # JwtServiceTest, no necesita nada externo
mvn spring-boot:run      # necesita MySQL y RabbitMQ arriba
```

## Probar el login falso y los roles

```
curl -s -X POST localhost:8081/api/auth/login -H "Content-Type: application/json" \
     -d '{"usuario":"director","clave":"biohub"}'
# usuarios: investigador, director, auditor, administrador, seguridad, visitante (clave: biohub)

curl -i localhost:8081/api/ejemplo/director                       # 401, sin token
curl -i localhost:8081/api/ejemplo/director -H "Authorization: Bearer <token de investigador>"   # 403
curl -i localhost:8081/api/ejemplo/director -H "Authorization: Bearer <token de director>"       # 200
curl -i localhost:8081/actuator/health
```

## Usuarios y login 

El login valida contra la tabla `usuario` (clave con BCrypt) y emite un JWT con el rol.
Usuario inexistente, clave incorrecta o usuario inactivo dan el mismo 401.

Al arrancar, `UsuariosDemo` crea un usuario por rol (investigador, director, auditor, administrador,
seguridad, visitante) con clave `biohub` si no existen. La clave se cambia con `CLAVE_DEMO` y la
creación se apaga con `USUARIOS_DEMO=false`.

## Ubicación de un hallazgo

```
curl -i localhost:8081/api/hallazgos/1/ubicacion                                        # 401, sin token
curl -s localhost:8081/api/hallazgos/1/ubicacion -H "Authorization: Bearer <token de director>"   # coordenadas exactas
curl -s localhost:8081/api/hallazgos/1/ubicacion -H "Authorization: Bearer <token de investigador>" # solo región
```

`GuardiaUbicacion` es el único punto que decide quién ve coordenadas exactas (solo el director). Cada consulta,
aceptada o rechazada, queda registrada antes de responder. `GuardiaUbicacionArquitecturaTest` (ArchUnit) falla si
otra clase lee latitud/longitud o usa `ProveedorUbicacion` directamente.

## Revocación y exposición (HU-27)

Solo el rol `SEGURIDAD` (token del usuario `seguridad`).

```
curl.exe -i -X POST localhost:8081/api/seguridad/usuarios/<id>/revocar -H "Authorization: Bearer <token>"      # 204
curl.exe -s "localhost:8081/api/seguridad/usuarios/<id>/exposicion?desde=2026-11-01T00:00:00&hasta=2026-11-30T23:59:59" -H "Authorization: Bearer <token>"
```

Revocar registra la acción en `accion_seguridad` y desactiva al usuario: su token recibe 401 en la siguiente petición y
no puede volver a iniciar sesión. La exposición lista los hallazgos cuyas coordenadas exactas vio el usuario (solo
accesos aceptados; por defecto, los últimos 30 días) y también queda registrada.

## Ejemplo para los otros tres

Proteger un endpoint por rol: `@PreAuthorize("hasRole('DIRECTOR')")` (ver `EjemploRolesController`).
El token lleva el rol sin prefijo (`DIRECTOR`); Spring lo ve como `ROLE_DIRECTOR`.

## Pendientes conocidos

- Flyway viene desactivado por defecto (`FLYWAY_ENABLED=false`); Compose lo activa. Migraciones en `db/migration` (V1 esquema, V2 datos demo).
- `JWT_SECRETO` (32+ caracteres) se define en Compose; el valor por defecto es solo para desarrollo.
- Capas previstas bajo `co.biohub.cientifico`: `api`, `servicio`, `dominio`, `repositorio`, `seguridad`, `eventos`.
