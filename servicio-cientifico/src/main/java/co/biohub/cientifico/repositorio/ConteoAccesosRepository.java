package co.biohub.cientifico.repositorio;

import co.biohub.cientifico.dominio.RegistroAcceso;
import co.biohub.cientifico.dominio.ResultadoAcceso;
import org.springframework.data.repository.Repository;

import java.time.LocalDateTime;

/**
 * Solo conteos sobre registro_acceso, para el monitor de seguridad (HU-26). Va aparte de RegistroAccesoRepository
 * para que el monitor no pueda guardar ni leer filas: solo contar.
 */
public interface ConteoAccesosRepository extends Repository<RegistroAcceso, Long> {

    long countByUsuarioIdAndOcurridoEnBetween(Long usuarioId, LocalDateTime desde, LocalDateTime hasta);

    long countByUsuarioIdAndResultadoAndOcurridoEnBetween(Long usuarioId, ResultadoAcceso resultado,
                                                          LocalDateTime desde, LocalDateTime hasta);
}
