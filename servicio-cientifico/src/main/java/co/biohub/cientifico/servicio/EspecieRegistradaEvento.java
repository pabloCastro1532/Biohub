package co.biohub.cientifico.servicio;

/** Evento interno que se publica al confirmar la transacción de una especie nueva. */
public record EspecieRegistradaEvento(Long especieId, String nombreCientifico, String nombreComun) {}
