package co.biohub.cientifico.dominio.atributos;

/** Producto del Factory Method: un atributo de ficha ya construido según su tipo. */
public interface Atributo {

    AtributoDefinicion definicion();

    /** Cómo se muestra este atributo en el formulario. */
    AtributoVista describir();
}
