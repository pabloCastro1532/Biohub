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

## Ejemplo para los otros tres

Proteger un endpoint por rol: `@PreAuthorize("hasRole('DIRECTOR')")` (ver `EjemploRolesController`).
El token lleva el rol sin prefijo (`DIRECTOR`); Spring lo ve como `ROLE_DIRECTOR`.

## Pendientes conocidos

- Flyway viene desactivado por defecto (`FLYWAY_ENABLED=false`); Compose lo activa. Migraciones en `db/migration` (V1 esquema, V2 datos demo).
- `SesionesRevocadas` nunca revoca; HU-27 lo reemplaza.
- `JWT_SECRETO` (32+ caracteres) se define en Compose; el valor por defecto es solo para desarrollo.
- Capas previstas bajo `co.biohub.cientifico`: `api`, `servicio`, `dominio`, `repositorio`, `seguridad`, `eventos`.
