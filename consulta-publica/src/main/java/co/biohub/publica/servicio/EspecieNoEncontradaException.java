package co.biohub.publica.servicio;

public class EspecieNoEncontradaException extends RuntimeException {

    public EspecieNoEncontradaException(Long id) {
        super("No existe la especie " + id);
    }
}
