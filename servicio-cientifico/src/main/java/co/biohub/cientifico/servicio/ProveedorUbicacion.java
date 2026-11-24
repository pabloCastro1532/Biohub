package co.biohub.cientifico.servicio;

import co.biohub.cientifico.dominio.VersionHallazgo;
import co.biohub.cientifico.repositorio.VersionHallazgoRepository;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Lee la ubicación de un hallazgo. Es la única clase, junto con HallazgoService (que solo compara
 * versiones), que toca las coordenadas exactas. Solo GuardiaUbicacion puede llamarla: lo verifica
 * una prueba de ArchUnit (ASR-4).
 */
@Component
public class ProveedorUbicacion {

    public record Ubicacion(String region, BigDecimal latitud, BigDecimal longitud) {}

    private final VersionHallazgoRepository versiones;

    public ProveedorUbicacion(VersionHallazgoRepository versiones) {
        this.versiones = versiones;
    }

    /** Solo la región o el departamento: lo que puede ver cualquier usuario autenticado. */
    public String region(Long hallazgoId) {
        return actual(hallazgoId).getRegion();
    }

    /** La ubicación exacta de la versión vigente. */
    public Ubicacion exacta(Long hallazgoId) {
        VersionHallazgo v = actual(hallazgoId);
        return new Ubicacion(v.getRegion(), v.getLatitud(), v.getLongitud());
    }

    private VersionHallazgo actual(Long hallazgoId) {
        return versiones.findFirstByHallazgoIdOrderByNumeroDesc(hallazgoId)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe el hallazgo " + hallazgoId));
    }
}