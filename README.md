# BioHub

Instituto Nacional de Biodiversidad. Entrega 3, ISW-2, Universidad El Bosque.

```
servicio-cientifico/   Spring Boot, monolito modular en capas (puerto 8081)
consulta-publica/      Spring Boot, solo lectura (8082 y 8083)      [Santiago, domingo]
gateway/               Spring Cloud Gateway (8080)                  [Santiago, domingo]
procesador-reportes/   Spring Batch (8084)                          [solo nivel 3]
web/                   Angular 17 (4200)
infra/                 docker-compose.yml y sql/
contratos/             esquemas JSON de los eventos y topología de RabbitMQ
pruebas/               k6 y scripts de ASR
docs/
```

## Levantar la infraestructura

```
cd infra
docker compose up -d mysql rabbitmq redis mailpit
```

- RabbitMQ: http://localhost:15672 (guest / guest)
- Mailpit (correos de alerta): http://localhost:8025
- MySQL: localhost:3306, usuario `root` / `root`

Si cambian `infra/sql/00_esquemas.sql` hay que recrear el volumen: `docker compose down -v`.

## Levantar el servicio científico

Perfil completo (construye la imagen y migra la BD con Flyway):

```
cd infra
docker compose --profile completo up -d --build
curl http://localhost:8081/actuator/health
```

O desde el IDE, con la infraestructura arriba: `FLYWAY_ENABLED=true mvn spring-boot:run` dentro de `servicio-cientifico/`.

## Acuerdos

Ramas cortas por historia (`feature/hu-18`), `main` siempre ejecutable, revisión de otra persona en todo cambio
que toque la guardia de ubicación o las tablas de versiones. Versión de Spring Boot: la misma en todos los módulos.
