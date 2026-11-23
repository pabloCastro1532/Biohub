package co.biohub.cientifico.dominio.atributos;

import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/** Reúne todas las fábricas disponibles y entrega la que corresponde a cada tipo. */
@Component
public class RegistroDeFabricas {

    private final Map<TipoAtributo, FabricaAtributo> fabricas = new EnumMap<>(TipoAtributo.class);

    public RegistroDeFabricas(List<FabricaAtributo> disponibles) {
        for (FabricaAtributo f : disponibles) {
            fabricas.put(f.tipo(), f);
        }
    }

    public Atributo crear(AtributoDefinicion definicion) {
        FabricaAtributo fabrica = fabricas.get(definicion.tipo());
        if (fabrica == null) {
            throw new IllegalArgumentException("No hay fábrica para el tipo " + definicion.tipo());
        }
        return fabrica.crear(definicion);
    }
}
