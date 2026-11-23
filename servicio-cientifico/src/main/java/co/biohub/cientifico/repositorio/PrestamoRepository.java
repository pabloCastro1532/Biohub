package co.biohub.cientifico.repositorio;

import co.biohub.cientifico.dominio.Prestamo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface PrestamoRepository extends JpaRepository<Prestamo, Long> {

    /** El préstamo abierto de un equipo, si lo hay (HU-05). */
    Optional<Prestamo> findByEquipoIdAndDevueltoEnIsNull(Long equipoId);

    List<Prestamo> findAllByOrderByPrestadoEnDesc();

    List<Prestamo> findByDevueltoEnIsNullOrderByPrestadoEnDesc();

    /** Cierra el préstamo solo si seguía abierto. Devuelve 0 si ya estaba devuelto, así dos devoluciones simultáneas no chocan. */
    @Modifying(clearAutomatically = true)
    @Query("update Prestamo p set p.devueltoEn = :cuando, p.devueltoPor = :por where p.id = :id and p.devueltoEn is null")
    int cerrar(@Param("id") Long id, @Param("cuando") LocalDateTime cuando, @Param("por") String por);
}
