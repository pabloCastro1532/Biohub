package co.biohub.cientifico.servicio;

/** El recurso pedido no existe (se responde 404). */
public class RecursoNoEncontradoException extends RuntimeException {

    public RecursoNoEncontradoException(String mensaje) {
        super(mensaje);
    }
}
