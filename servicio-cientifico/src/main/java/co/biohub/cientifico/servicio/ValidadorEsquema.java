package co.biohub.cientifico.servicio;

import com.fasterxml.jackson.databind.JsonNode;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.ValidationMessage;
import org.springframework.stereotype.Component;

import java.util.List;

/** Valida datos contra un JSON Schema. No toca la base de datos. */
@Component
public class ValidadorEsquema {

    private final JsonSchemaFactory fabrica = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012);

    /** Devuelve la lista de errores; vacía si los datos cumplen el esquema. */
    public List<String> validar(JsonNode esquema, JsonNode datos) {
        JsonSchema schema = fabrica.getSchema(esquema);
        return schema.validate(datos).stream()
                .map(ValidationMessage::getMessage)
                .sorted()
                .toList();
    }
}
