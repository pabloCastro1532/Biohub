# gateway

API Gateway minimo de BioHub (Spring Cloud Gateway, puerto 8080). Es el unico punto de entrada: 

| Ruta | Destino |
|---|---|
| `/api/publico/**` | `consulta-publica` (2 instancias, reparto por turnos; si una falla, el GET se reintenta en la otra) |
| `/api/**` | `servicio-cientifico` |

Alcance de esta entrega (ASR-5, "gateway minimo"): enruta, reparte y pone tiempos limite. La API key, el limite de 60 por
minuto y el contrato v1 (HU-20 y HU-21) quedan como diseno futuro.

```
curl http://localhost:8080/actuator/health
curl "http://localhost:8080/api/publico/especies?q=jaguar"
```
