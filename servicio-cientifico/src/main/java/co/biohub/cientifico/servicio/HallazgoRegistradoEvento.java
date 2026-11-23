package co.biohub.cientifico.servicio;

import java.time.LocalDate;
import java.util.Map;

/** Evento interno que se publica al confirmar la transacción. Sin coordenadas exactas. */
public record HallazgoRegistradoEvento(Long hallazgoId, Long especieId, int version, String region,
                                       LocalDate fecha, Map<String, Object> atributos) {}
