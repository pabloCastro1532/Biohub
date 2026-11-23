package co.biohub.cientifico.dominio.atributos;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Lee las propiedades de un JSON Schema y construye un {@link Atributo} por cada una, usando el registro de fábricas. */
public final class EsquemaAAtributos {

    private EsquemaAAtributos() {
    }

    public static List<Atributo> leer(JsonNode esquema, RegistroDeFabricas registro) {
        Set<String> requeridos = new HashSet<>();
        esquema.path("required").forEach(n -> requeridos.add(n.asText()));

        List<Atributo> atributos = new ArrayList<>();
        Iterator<Map.Entry<String, JsonNode>> propiedades = esquema.path("properties").fields();
        while (propiedades.hasNext()) {
            Map.Entry<String, JsonNode> entrada = propiedades.next();
            String nombre = entrada.getKey();
            JsonNode p = entrada.getValue();

            TipoAtributo tipo = tipoDe(nombre, p);
            List<String> valores = new ArrayList<>();
            if (p.has("enum")) {
                p.get("enum").forEach(v -> valores.add(v.asText()));
            }
            AtributoDefinicion definicion = new AtributoDefinicion(
                    nombre, tipo, requeridos.contains(nombre), valores, numero(p, "minimum"), numero(p, "maximum"));
            atributos.add(registro.crear(definicion));
        }
        return atributos;
    }

    private static TipoAtributo tipoDe(String nombre, JsonNode propiedad) {
        if (propiedad.has("enum")) {
            return TipoAtributo.ENUMERADO;
        }
        String type = propiedad.path("type").asText("");
        return switch (type) {
            case "string" -> "date".equals(propiedad.path("format").asText()) ? TipoAtributo.FECHA : TipoAtributo.TEXTO;
            case "number", "integer" -> TipoAtributo.NUMERO;
            case "boolean" -> TipoAtributo.BOOLEANO;
            default -> throw new IllegalArgumentException(
                    "Tipo no soportado en el atributo '" + nombre + "': '" + type + "'");
        };
    }

    private static Double numero(JsonNode propiedad, String campo) {
        JsonNode valor = propiedad.get(campo);
        return valor != null && valor.isNumber() ? Double.valueOf(valor.asDouble()) : null;
    }
}
