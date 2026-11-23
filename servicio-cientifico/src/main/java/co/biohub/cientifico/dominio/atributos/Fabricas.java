package co.biohub.cientifico.dominio.atributos;

import org.springframework.stereotype.Component;

/**
 * Las fábricas concretas. Para soportar un tipo nuevo (ASR-6) basta agregar su producto en
 * {@link Atributos}, su valor en {@link TipoAtributo} y una fábrica aquí: el registro la detecta solo.
 */
public final class Fabricas {

    private Fabricas() {
    }

    @Component
    public static class FabricaTexto implements FabricaAtributo {
        @Override
        public TipoAtributo tipo() { return TipoAtributo.TEXTO; }

        @Override
        public Atributo crear(AtributoDefinicion definicion) { return new Atributos.Texto(definicion); }
    }

    @Component
    public static class FabricaNumero implements FabricaAtributo {
        @Override
        public TipoAtributo tipo() { return TipoAtributo.NUMERO; }

        @Override
        public Atributo crear(AtributoDefinicion definicion) { return new Atributos.Numero(definicion); }
    }

    @Component
    public static class FabricaBooleano implements FabricaAtributo {
        @Override
        public TipoAtributo tipo() { return TipoAtributo.BOOLEANO; }

        @Override
        public Atributo crear(AtributoDefinicion definicion) { return new Atributos.Booleano(definicion); }
    }

    @Component
    public static class FabricaEnumerado implements FabricaAtributo {
        @Override
        public TipoAtributo tipo() { return TipoAtributo.ENUMERADO; }

        @Override
        public Atributo crear(AtributoDefinicion definicion) { return new Atributos.Enumerado(definicion); }
    }

    @Component
    public static class FabricaFecha implements FabricaAtributo {
        @Override
        public TipoAtributo tipo() { return TipoAtributo.FECHA; }

        @Override
        public Atributo crear(AtributoDefinicion definicion) { return new Atributos.Fecha(definicion); }
    }
}
