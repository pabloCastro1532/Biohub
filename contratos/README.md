# Contratos de eventos

Cinco eventos (cuatro de la Entrega 2 y especie.registrada). Cada archivo `.schema.json` es un JSON Schema; todo mensaje lleva
`id_evento` (UUID) y `ocurrido_en` (ISO 8601). **Ningún evento que sale del servicio científico lleva
coordenadas exactas**, solo `region`.

## Topología de RabbitMQ

Un exchange `topic` durable llamado `biohub.eventos`. Colas durables, una por consumidor:

| Evento | Routing key | Cola | Consume |
| --- | --- | --- | --- |
| HallazgoRegistrado | `hallazgo.registrado` | `consulta-publica.hallazgos` | consulta-publica |
| HallazgoReclasificado | `hallazgo.reclasificado` | `consulta-publica.hallazgos` | consulta-publica |
| EspecieRegistrada | `especie.registrada` | `consulta-publica.especies` | consulta-publica |
| AccesoCoordenadasRegistrado | `acceso.coordenadas.registrado` | `seguridad.accesos` | MonitorSeguridad (servicio-cientifico) |
| ReporteSolicitado | `reporte.solicitado` | `reportes.solicitudes` | procesador-reportes (nivel 3) |

Reglas: se publica después de confirmar la transacción (`@TransactionalEventListener(AFTER_COMMIT)`);
el consumidor guarda `id_evento` en `eventos_procesados` y descarta repetidos (idempotencia).
Con dos instancias de consulta-publica comparten la misma cola: cada mensaje lo recibe una sola.
