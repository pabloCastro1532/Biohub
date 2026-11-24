# pruebas

## ASR-1: consulta publica bajo picos (k6)

`k6/asr1.js` mide la busqueda publica por el gateway (puerto 8080): primero con trafico normal y luego con 60 veces ese
volumen, mientras se registran hallazgos en paralelo. Criterios: p95 <= 2 s, errores < 2 %, el registro no empeora mas de 10 %.

- Trafico normal usado: 5 busquedas por segundo (variable `TASA_NORMAL`); pico: 5 x 60 = 300 por segundo (`FACTOR`).
- Cada fase dura 60 s (`FASE_SEGUNDOS`), con 10 s de pausa entre fases.
- La prueba crea hallazgos reales (1 por segundo): al terminar conviene `docker compose down -v` para dejar la base limpia.

Con el sistema arriba (`docker compose -f infra/docker-compose.yml --profile completo up -d --build`):

```
.\pruebas\k6\correr.ps1
```

Deja el resultado completo en `pruebas/k6/resultado-asr1.json` (no se sube al repositorio).
