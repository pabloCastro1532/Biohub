package co.biohub.cientifico.repositorio;

import co.biohub.cientifico.dominio.AlertaSeguridad;
import co.biohub.cientifico.dominio.TipoAlerta;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface AlertaSeguridadRepository extends JpaRepository<AlertaSeguridad, Long> {

    /** Para no repetir la misma alerta mientras el usuario sigue dentro de la misma ventana. */
    boolean existsByUsuarioIdAndTipoAndGeneradaEnBetween(Long usuarioId, TipoAlerta tipo,
                                                         LocalDateTime desde, LocalDateTime hasta);

    List<AlertaSeguridad> findTop200ByOrderByGeneradaEnDescIdDesc();
}
