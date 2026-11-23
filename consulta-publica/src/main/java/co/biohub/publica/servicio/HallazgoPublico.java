package co.biohub.publica.servicio;

import java.time.LocalDate;
import java.util.Map;

/** Lo único que esta base guarda de un hallazgo (contratos/hallazgo-registrado.schema.json): sin ubicación exacta. */
public record HallazgoPublico(long hallazgoId, long especieId, int version, String region, LocalDate fecha,
                              Map<String, Object> atributos) {}
