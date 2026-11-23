package co.biohub.cientifico.servicio;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ValidadorEsquemaTest {

    private final ObjectMapper mapper = new ObjectMapper();
    private final ValidadorEsquema validador = new ValidadorEsquema();

    private JsonNode json(String texto) throws Exception {
        return mapper.readTree(texto);
    }

    private static final String JAGUAR = """
            {
              "type": "object",
              "properties": {
                "peso_kg": { "type": "number", "minimum": 0 },
                "sexo": { "type": "string", "enum": ["macho", "hembra", "desconocido"] },
                "huella_cm": { "type": "number", "minimum": 0 }
              },
              "required": ["sexo"],
              "additionalProperties": false
            }
            """;

    @Test
    void aceptaAtributosValidos() throws Exception {
        List<String> errores = validador.validar(json(JAGUAR), json("{\"sexo\":\"macho\",\"peso_kg\":80.5}"));
        assertTrue(errores.isEmpty());
    }

    @Test
    void rechazaFaltaDeAtributoRequerido() throws Exception {
        List<String> errores = validador.validar(json(JAGUAR), json("{\"peso_kg\":80}"));
        assertEquals(1, errores.size());
        assertTrue(errores.get(0).contains("sexo"));
    }

    @Test
    void rechazaTipoIncorrecto() throws Exception {
        List<String> errores = validador.validar(json(JAGUAR), json("{\"sexo\":\"macho\",\"peso_kg\":\"mucho\"}"));
        assertFalse(errores.isEmpty());
    }

    @Test
    void rechazaValorFueraDeLaLista() throws Exception {
        List<String> errores = validador.validar(json(JAGUAR), json("{\"sexo\":\"otro\"}"));
        assertFalse(errores.isEmpty());
    }

    @Test
    void rechazaAtributoQueLaPlantillaNoDeclara() throws Exception {
        List<String> errores = validador.validar(json(JAGUAR), json("{\"sexo\":\"macho\",\"color\":\"negro\"}"));
        assertFalse(errores.isEmpty());
    }

    @Test
    void atributoNuevoEnLaPlantillaSeAceptaSinCambiarCodigo() throws Exception {
        String conColor = JAGUAR.replace("\"huella_cm\"", "\"color\": { \"type\": \"string\" }, \"huella_cm\"");
        List<String> errores = validador.validar(json(conColor), json("{\"sexo\":\"macho\",\"color\":\"negro\"}"));
        assertTrue(errores.isEmpty());
    }
}
