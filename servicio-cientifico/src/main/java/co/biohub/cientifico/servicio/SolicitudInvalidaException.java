package co.biohub.cientifico.servicio;

/** La petición no cumple las reglas del negocio (se responde 400). */
public class SolicitudInvalidaException extends RuntimeException {

    public SolicitudInvalidaException(String mensaje) {
        super(mensaje);
    }
}
