package co.biohub.cientifico.repositorio;

import co.biohub.cientifico.dominio.PlantillaEspecie;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PlantillaRepository extends JpaRepository<PlantillaEspecie, Long> {

    List<PlantillaEspecie> findByActivaTrueOrderByEspecieIdAsc();

    Optional<PlantillaEspecie> findFirstByEspecieIdAndActivaTrueOrderByVersionDesc(Long especieId);

    Optional<PlantillaEspecie> findFirstByEspecieIdOrderByVersionDesc(Long especieId);

    @Modifying(clearAutomatically = true)
    @Query("update PlantillaEspecie p set p.activa = false where p.especieId = :especieId and p.activa = true")
    int desactivarDe(@Param("especieId") Long especieId);
}
