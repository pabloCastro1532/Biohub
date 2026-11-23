package co.biohub.cientifico.repositorio;

import co.biohub.cientifico.dominio.AsignacionResponsable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface AsignacionResponsableRepository extends JpaRepository<AsignacionResponsable, Long> {

    /** La asignación vigente de un equipo, si la hay (HU-07). */
    Optional<AsignacionResponsable> findByEquipoIdAndHastaIsNull(Long equipoId);

    /** Todas las asignaciones vigentes (una por equipo como máximo). */
    List<AsignacionResponsable> findByHastaIsNull();

    /** Cierra la asignación solo si seguía vigente. */
    @Modifying(clearAutomatically = true)
    @Query("update AsignacionResponsable a set a.hasta = :cuando where a.id = :id and a.hasta is null")
    int cerrar(@Param("id") Long id, @Param("cuando") LocalDateTime cuando);
}
