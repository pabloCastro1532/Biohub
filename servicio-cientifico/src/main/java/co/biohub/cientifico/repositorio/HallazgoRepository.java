package co.biohub.cientifico.repositorio;

import co.biohub.cientifico.dominio.Hallazgo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface HallazgoRepository extends JpaRepository<Hallazgo, Long> {

    List<Hallazgo> findAllByOrderByIdDesc();
}
