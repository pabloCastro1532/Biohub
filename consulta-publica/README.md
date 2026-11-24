# consulta-publica

Servicio de consulta pública de BioHub (HU-18 búsqueda, HU-19 ficha). Solo lectura, sin coordenadas exactas (ASR-4),
pensado para varias instancias y picos de tráfico (ASR-1).

- Lee solo el esquema `lectura` (usuario `biohub_publica`, sin acceso a `cientifica`).
- Se alimenta del evento `hallazgo.registrado` (cola `consulta-publica.hallazgos`, ver `../contratos/README.md`): es idempotente y solo avanza de versión.
- Redis guarda fichas y búsquedas con TTL corto (`CACHE_TTL`, 60 s por defecto) y se invalida cuando llega un evento. Si Redis cae, responde desde la BD.

```
GET /api/publico/especies?q=jaguar&region=Meta&limite=20     búsqueda
GET /api/publico/especies/{id}                               ficha
```

Puerto 8082 (en Compose: consulta-publica-1 en 8082 y consulta-publica-2 en 8083).
