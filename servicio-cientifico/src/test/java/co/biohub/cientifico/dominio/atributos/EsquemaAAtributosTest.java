package co.biohub.cientifico.dominio.atributos;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EsquemaAAtributosTest {

    private final ObjectMapper mapper = new ObjectMapper();

    private final RegistroDeFabricas registro = new RegistroDeFabricas(List.of(
            new Fabricas.FabricaTexto(),
            new Fabricas.FabricaNumero(),
            new Fabricas.FabricaBooleano(),
            new Fabricas.FabricaEnumerado(),
            new Fabricas.FabricaFecha()));

    @Test
    void construyeUnAtributoPorPropiedadConSuTipo() throws Exception {
        JsonNode esquema = mapper.readTree("""
                {
                  "type": "object",
                  "properties": {
                    "peso_kg": { "type": "number", "minimum": 0 },
                    "sexo": { "type": "string", "enum": ["macho", "hembra"] },
                    "en_bandada": { "type": "boolean" },
                    "observado": { "type": "string", "format": "date" },
                    "nota": { "type": "string" }
                  },
                  "required": ["sexo"]
                }
                """);

        List<AtributoVista> vistas = EsquemaAAtributos.leer(esquema, registro).stream()
                .map(Atributo::describir)
                .toList();

        assertEquals(5, vistas.size());
        assertEquals("numero", vistas.get(0).tipo());
        assertEquals(0.0, vistas.get(0).minimo());
        assertEquals("enumerado", vistas.get(1).tipo());
        assertEquals(List.of("macho", "hembra"), vistas.get(1).valores());
        assertTrue(vistas.get(1).requerido());
        assertEquals("booleano", vistas.get(2).tipo());
        assertEquals("fecha", vistas.get(3).tipo());
        assertEquals("texto", vistas.get(4).tipo());
    }

    @Test
    void rechazaUnTipoNoSoportado() throws Exception {
        JsonNode esquema = mapper.readTree("""
                { "type": "object", "properties": { "datos": { "type": "array" } } }
                """);

        assertThrows(IllegalArgumentException.class, () -> EsquemaAAtributos.leer(esquema, registro));
    }
}
