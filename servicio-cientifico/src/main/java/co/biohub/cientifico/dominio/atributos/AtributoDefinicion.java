package co.biohub.cientifico.dominio.atributos;

import java.util.List;

/** Lo que una plantilla dice de un atributo: nombre, tipo y restricciones. Es la entrada de las fábricas. */
public record AtributoDefinicion(
        String nombre,
        TipoAtributo tipo,
        boolean requerido,
        List<String> valores,
        Double minimo,
        Double maximo) {
}
