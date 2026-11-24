# BioHub

Plataforma del Instituto Nacional de Biodiversidad para **centralizar el inventario de hallazgos de especies**, **proteger la ubicación exacta** de las especies sensibles y ofrecer una **consulta pública** rápida aunque haya picos de visitas.

Proyecto de Ingeniería de Software 2 (ISW-2), Universidad El Bosque. Entrega 3.

---

## Para qué sirve

- Los **investigadores** registran hallazgos (qué especie, dónde, cuándo, con qué atributos) y el sistema guarda **todo el historial** de cambios sin poder borrarlo.
- El **director** es la única persona que ve las **coordenadas exactas**. Define las especies y sus plantillas de atributos, y puede **reclasificar** un hallazgo (cambiarle la especie con un motivo).
- El **público** consulta especies y hallazgos desde la portada, sin cuenta y **sin ver coordenadas exactas**.
- **Seguridad** recibe alertas por correo cuando alguien consulta ubicaciones de forma sospechosa y puede **revocar** su acceso al instante.
- **Administración** gestiona equipos de trabajo y cuentas.

## Ideas de diseño (decisiones de arquitectura)

| ASR | Qué se pidió | Cómo se resolvió | Estado |
|---|---|---|---|
| ASR-1 | La consulta pública aguanta picos de tráfico | Servicio de lectura separado, con 2 instancias detrás de un gateway, base de lectura propia y caché en Redis | Construido y medido |
| ASR-3 | El historial de un hallazgo no se puede alterar | Tabla `version_hallazgo` solo con `SELECT, INSERT` para el usuario del servicio y triggers que rechazan `UPDATE`/`DELETE` | Construido |
| ASR-4 | Solo el director ve la ubicación exacta y todo acceso queda registrado | Una única `GuardiaUbicacion`, registro de accesos, prueba ArchUnit que impide saltarse la guardia, alertas, revocación inmediata | Construido |
| ASR-6 | Agregar atributos a una especie sin tocar código | Cada especie tiene una plantilla (JSON Schema); los atributos se guardan como JSON y se validan contra ella | Construido |
| ASR-2 | Reportes pesados sin frenar el sistema | Procesador de reportes desacoplado por eventos | Solo diseño (contrato `reporte.solicitado`) |
| ASR-5 | Acceso de sistemas externos | Gateway externo con API key, límite de uso y versión `v1` | Solo diseño |

**Resultado de ASR-1** (k6 por el gateway): 5 consultas/s en tráfico normal y 300/s en pico, **p95 = 6 ms y 0 % de errores** (meta: p95 ≤ 2 s y < 2 % de errores).

## Arquitectura

```
                      Navegador (Angular)
                             |
                        Gateway :8080
                      /              \
        /api/publico/**              /api/**
              |                         |
   consulta-publica x2          servicio-cientifico :8081
     :8082 y :8083                (núcleo, en capas)
        |      |                        |
      Redis  MySQL (lectura)        MySQL (científico, seguridad)
        ^                               |
        '---------- RabbitMQ <----------'
              exchange topic "biohub.eventos"
```

- **servicio-cientifico**: el núcleo. Hallazgos, especies, plantillas, equipos, préstamos, cuentas, seguridad. Escribe en su propio esquema y publica eventos.
- **consulta-publica**: solo lectura. Mantiene su propia proyección de los hallazgos escuchando eventos, y cachea en Redis. Se levantan 2 instancias.
- **gateway**: Spring Cloud Gateway. Enruta `/api/publico/**` a las instancias públicas (con reintento si una falla) y el resto de `/api/**` al núcleo.
- **web**: Angular, con una pantalla por rol.
- **MySQL**: 4 esquemas, un usuario por servicio con permisos mínimos.
- **RabbitMQ**: comunica los servicios con eventos para que la consulta pública no dependa del núcleo.

### Eventos

| Evento | Quién publica | Quién lo consume |
|---|---|---|
| `hallazgo.registrado` (también al editar y al reclasificar) | servicio-cientifico | consulta-publica |
| `hallazgo.reclasificado` | servicio-cientifico | consulta-publica (cola `consulta-publica.reclasificaciones`) |
| `especie.registrada` | servicio-cientifico | consulta-publica |
| acceso a coordenadas | servicio-cientifico | módulo de seguridad (`seguridad.accesos`) |
| `reporte.solicitado` | sin publicador (solo contrato, ASR-2) | sin consumidor |

Los contratos (JSON Schema de cada evento) están en [`contratos/`](contratos/).

## Tecnologías

| Parte | Tecnología | Para qué |
|---|---|---|
| Backend | Java 21, Spring Boot 3.4.5 | Los 3 servicios Java |
| Seguridad | Spring Security, JWT (jjwt 0.12), BCrypt | Login, roles, token firmado, contraseñas con hash |
| Persistencia | Spring Data JPA, MySQL 8, Flyway | Datos y migraciones versionadas |
| Atributos flexibles | JSON en MySQL + JSON Schema (networknt) | Plantillas de especie validadas |
| Mensajería | RabbitMQ 3.13 | Eventos entre servicios |
| Caché | Redis 7 | Respuestas de la consulta pública |
| Gateway | Spring Cloud Gateway | Entrada única, balanceo, reintentos |
| Frontend | Angular 17, Tailwind CSS 3.4 | Aplicación web por roles |
| Correo (demo) | Mailpit | Bandeja local para ver las alertas |
| Contenedores | Docker Compose | Todo el entorno con un comando |
| Pruebas | JUnit 5, ArchUnit, k6 | Unitarias, arquitectura y carga |

## Estructura del repositorio

```
servicio-cientifico/   núcleo (api, dominio, servicio, repositorio, mensajeria, seguridad, notificacion)
consulta-publica/      lectura pública, proyección y caché
gateway/               Spring Cloud Gateway
web/                   Angular (accesos, equipos, especies, hallazgos, plantillas, seguridad, ubicación, inicio)
infra/                 docker-compose.yml y sql/ (creación de esquemas y usuarios)
contratos/             JSON Schema de los eventos
pruebas/               prueba de carga k6 (ASR-1)
```

## Cómo levantarlo

Requisitos: **Docker Desktop**, **Node.js 18+** y, solo si quieren correr los servicios sin Docker, **JDK 21** y **Maven**.

### 1. Backend completo (PowerShell)

```powershell
cd infra
docker compose --profile completo up -d --build
```

La primera vez tarda unos minutos (compila las imágenes). Para comprobar que está arriba:

```powershell
curl http://localhost:8081/actuator/health
```

Si el puerto 8080 está ocupado, usen otro para el gateway:

```powershell
$env:GATEWAY_PORT = 8090
docker compose --profile completo up -d --build
```

### 2. Frontend

```powershell
cd web
npm.cmd install
npm.cmd start
```

Abre `http://localhost:4200`. En desarrollo el proxy de Angular habla directo con el núcleo (8081) y con la consulta pública (8082), así que funciona aunque el gateway no esté arriba. Si el puerto 4200 está ocupado, Angular propone otro.

> Si en la consola del frontend aparece `ECONNREFUSED` hacia `/api/...`, el backend no está arriba: revisen `docker compose ps` desde `infra`.

### 3. Solo la infraestructura (para correr un servicio desde el IDE)

```powershell
cd infra
docker compose up -d mysql rabbitmq redis mailpit
```

Luego, por ejemplo: `FLYWAY_ENABLED=true mvn spring-boot:run` dentro de `servicio-cientifico`.

### Reiniciar desde cero

Si cambian los `.sql` de `infra/sql` o quieren la base limpia (datos demo otra vez):

```powershell
cd infra
docker compose down -v
docker compose --profile completo up -d --build
```

## Direcciones y puertos

| Qué | Dirección |
|---|---|
| Aplicación web | http://localhost:4200 |
| Gateway | http://localhost:8080 (o `GATEWAY_PORT`) |
| Servicio científico | http://localhost:8081 |
| Consulta pública (2 instancias) | http://localhost:8082 y http://localhost:8083 |
| RabbitMQ (gestión) | http://localhost:15672 (`guest` / `guest`) |
| Mailpit (correos de alerta) | http://localhost:8025 |
| MySQL | localhost:3306 (`root` / `root`) |
| Redis | localhost:6379 |

## Usuarios de demostración

El usuario es el nombre del rol en minúsculas y la contraseña es `biohub` para todos.

| Usuario | Qué puede hacer |
|---|---|
| (sin cuenta) | Portada pública: buscar especies y hallazgos sin coordenadas exactas |
| `investigador` | Registrar y editar hallazgos, ver su historial, pedir equipos |
| `director` | Único que ve coordenadas exactas; crea especies y plantillas; **reclasifica hallazgos** |
| `auditor` | Consultar el historial de versiones y los accesos |
| `administrador` | Gestionar equipos y cuentas |
| `seguridad` | Ver alertas de acceso sospechoso y **revocar** cuentas |

## Funcionalidades

22 historias de usuario comprometidas (77 puntos), todas construidas, más HU-09:

- Registrar, editar y consultar hallazgos con historial inmutable de versiones.
- **HU-09 Reclasificar un hallazgo**: el director elige otra especie, completa los atributos de su plantilla y escribe un motivo (máx. 500 caracteres). Se crea una versión nueva (la anterior se conserva) y la consulta pública mueve el hallazgo a la especie nueva y limpia la caché de ambas.
- Especies y plantillas de atributos configurables (ASR-6).
- Ubicación exacta solo para el director, con registro de cada acceso.
- Alertas de seguridad: más de 50 consultas de ubicación en 60 minutos, o 5 intentos rechazados en 10 minutos, generan una alerta y un correo (visible en Mailpit).
- Revocación de acceso: la cuenta queda inactiva y el token deja de valer de inmediato (401).
- Gestión de equipos, préstamos y cuentas.
- Portada pública y consulta con caché.

**Fuera de alcance** (acordado): HU-13, HU-14, HU-20 y HU-21.

## Pruebas

```powershell
# Unitarias y de arquitectura (ArchUnit), dentro de cada módulo
mvn test

# Carga de la consulta pública (ASR-1), con el sistema arriba
.\pruebas\k6\correr.ps1
```

La prueba de carga crea hallazgos reales; al terminar conviene hacer `docker compose down -v`. Más detalle en [`pruebas/README.md`](pruebas/README.md).

## Limitaciones conocidas

- Los eventos se publican después del commit y **sin reintento**: si RabbitMQ está caído en ese instante, la consulta pública no se entera. La mejora natural es el patrón *outbox*.
- Los atributos de un hallazgo se muestran en la consulta pública tal como se guardan, sin filtrar por sensibilidad.
- ASR-2 (reportes) y ASR-5 (gateway externo) están diseñados, no construidos.
- El rol `visitante` quedó sin uso real; la consulta pública no necesita cuenta.

## Acuerdos de trabajo

- Una rama corta por historia de usuario (`feature/hu-xx`); `main` siempre debe poder levantarse.
- Pull request con título y descripción cortos; se integra con *merge commit* (sin squash) y no se borra la rama desde GitHub.
- Los cambios en la guardia de ubicación o en la tabla de versiones requieren revisión de otra persona.
- Los 4 módulos Java usan la misma versión de Spring Boot.
- Mensajes de commit en minúscula, sin tildes, con prefijo `HU-xx:` cuando corresponde.

## Equipo ISW-2

Pablo Castro, Yonnatan, Valentina Osorio y Santiago Rojas.
