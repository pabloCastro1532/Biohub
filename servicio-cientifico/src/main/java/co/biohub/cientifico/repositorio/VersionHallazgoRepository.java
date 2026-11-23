package co.biohub.cientifico.repositorio;

import co.biohub.cientifico.dominio.VersionHallazgo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface VersionHallazgoRepository extends JpaRepository<VersionHallazgo, Long> {

    /** La versión más reciente de un hallazgo, la que se muestra como "actual". */
    Optional<VersionHallazgo> findFirstByHallazgoIdOrderByNumeroDesc(Long hallazgoId);

    /** Todas las versiones, de la primera a la última (historial, HU-10 y HU-12). */
    List<VersionHallazgo> findByHallazgoIdOrderByNumeroAsc(Long hallazgoId);
}
