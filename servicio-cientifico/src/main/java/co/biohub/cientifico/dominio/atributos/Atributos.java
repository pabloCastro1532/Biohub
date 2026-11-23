package co.biohub.cientifico.dominio.atributos;

import java.util.List;

/** Los productos concretos del Factory Method, uno por tipo de atributo. */
public final class Atributos {

    private Atributos() {
    }

    public record Texto(AtributoDefinicion definicion) implements Atributo {
        @Override
        public AtributoVista describir() {
            return new AtributoVista(definicion.nombre(), "texto", definicion.requerido(), List.of(), null, null);
        }
    }

    public record Numero(AtributoDefinicion definicion) implements Atributo {
        @Override
        public AtributoVista describir() {
            return new AtributoVista(definicion.nombre(), "numero", definicion.requerido(), List.of(),
                    definicion.minimo(), definicion.maximo());
        }
    }

    public record Booleano(AtributoDefinicion definicion) implements Atributo {
        @Override
        public AtributoVista describir() {
            return new AtributoVista(definicion.nombre(), "booleano", definicion.requerido(), List.of(), null, null);
        }
    }

    public record Enumerado(AtributoDefinicion definicion) implements Atributo {
        @Override
        public AtributoVista describir() {
            return new AtributoVista(definicion.nombre(), "enumerado", definicion.requerido(),
                    definicion.valores(), null, null);
        }
    }

    public record Fecha(AtributoDefinicion definicion) implements Atributo {
        @Override
        public AtributoVista describir() {
            return new AtributoVista(definicion.nombre(), "fecha", definicion.requerido(), List.of(), null, null);
        }
    }
}
