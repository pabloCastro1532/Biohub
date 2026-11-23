package co.biohub.cientifico.servicio;

/** La operación choca con algo que ya existe, por ejemplo un código repetido (se responde 409). */
public class ConflictoException extends RuntimeException {

    public ConflictoException(String mensaje) {
        super(mensaje);
    }
}
