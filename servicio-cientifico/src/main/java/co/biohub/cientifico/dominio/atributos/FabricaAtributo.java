package co.biohub.cientifico.dominio.atributos;

/** Creador del Factory Method: cada implementación sabe construir un tipo de atributo. */
public interface FabricaAtributo {

    TipoAtributo tipo();

    Atributo crear(AtributoDefinicion definicion);
}
