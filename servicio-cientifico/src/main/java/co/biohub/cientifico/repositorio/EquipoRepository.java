package co.biohub.cientifico.repositorio;

import co.biohub.cientifico.dominio.Equipo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EquipoRepository extends JpaRepository<Equipo, Long> {

    boolean existsByCodigo(String codigo);

    List<Equipo> findAllByOrderByNombreAsc();
}
