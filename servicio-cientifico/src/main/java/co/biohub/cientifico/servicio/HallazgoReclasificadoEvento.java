package co.biohub.cientifico.servicio;

/** Evento interno (HU-09) que se publica al confirmar la transacción. Mismo contenido que contratos/hallazgo-reclasificado.schema.json. */
public record HallazgoReclasificadoEvento(Long hallazgoId, Long especieAnterior, Long especieNueva,
                                          String autor, String motivo) {}
