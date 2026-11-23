package co.biohub.cientifico.repositorio;

import co.biohub.cientifico.dominio.Especie;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EspecieRepository extends JpaRepository<Especie, Long> {

    List<Especie> findAllByOrderByNombreCientificoAsc();
}
