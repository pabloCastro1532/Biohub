package co.biohub.publica.servicio;

/** Limpia los parámetros de la búsqueda (HU-18) antes de consultar, para que dos formas de pedir lo mismo usen la misma caché. */
public final class ParametrosBusqueda {

    public static final int LIMITE_POR_DEFECTO = 20;
    public static final int LIMITE_MAXIMO = 50;
    public static final int LARGO_MAXIMO_TEXTO = 100;

    private ParametrosBusqueda() {
    }

    /** Texto para LIKE: "%jaguar%". Vacío o en blanco = sin filtro (null). Los comodines del usuario se escapan. */
    public static String patron(String q) {
        String texto = limpiar(q, "El texto de búsqueda");
        if (texto == null) {
            return null;
        }
        String escapado = texto.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
        return "%" + escapado + "%";
    }

    public static String region(String region) {
        return limpiar(region, "La región");
    }

    public static int limite(Integer limite) {
        if (limite == null) {
            return LIMITE_POR_DEFECTO;
        }
        return Math.max(1, Math.min(limite, LIMITE_MAXIMO));
    }

    private static String limpiar(String valor, String nombre) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        String texto = valor.trim();
        if (texto.length() > LARGO_MAXIMO_TEXTO) {
            throw new SolicitudInvalidaException(nombre + " no puede pasar de " + LARGO_MAXIMO_TEXTO + " caracteres");
        }
        return texto;
    }
}
