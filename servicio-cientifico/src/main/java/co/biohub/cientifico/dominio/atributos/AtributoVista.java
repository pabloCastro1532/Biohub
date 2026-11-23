package co.biohub.cientifico.dominio.atributos;

import java.util.List;

/** Descripción de un atributo para que Angular arme el campo del formulario. */
public record AtributoVista(
        String nombre,
        String tipo,
        boolean requerido,
        List<String> valores,
        Double minimo,
        Double maximo) {
}
