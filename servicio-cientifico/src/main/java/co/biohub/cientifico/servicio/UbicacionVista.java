package co.biohub.cientifico.servicio;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.math.BigDecimal;

/**
 * Respuesta de una consulta de ubicación. Si el usuario no es de alto rango, latitud y longitud
 * son null y no aparecen en el JSON: solo se ve la región.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record UbicacionVista(Long hallazgoId, String region, BigDecimal latitud, BigDecimal longitud,
                             boolean exacta) {

    public static UbicacionVista soloRegion(Long hallazgoId, String region) {
        return new UbicacionVista(hallazgoId, region, null, null, false);
    }

    public static UbicacionVista conCoordenadas(Long hallazgoId, String region,
                                                BigDecimal latitud, BigDecimal longitud) {
        return new UbicacionVista(hallazgoId, region, latitud, longitud, true);
    }
}