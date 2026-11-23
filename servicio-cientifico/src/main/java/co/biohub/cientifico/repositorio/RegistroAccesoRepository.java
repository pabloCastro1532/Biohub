package co.biohub.cientifico.repositorio;

import co.biohub.cientifico.dominio.RegistroAcceso;
import co.biohub.cientifico.dominio.ResultadoAcceso;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RegistroAccesoRepository extends JpaRepository<RegistroAcceso, Long> {

    /** Los 200 más recientes: alcanza para la pantalla de seguridad sin traer toda la tabla. */
    List<RegistroAcceso> findTop200ByOrderByOcurridoEnDescIdDesc();

    List<RegistroAcceso> findTop200ByResultadoOrderByOcurridoEnDescIdDesc(ResultadoAcceso resultado);

    List<RegistroAcceso> findTop200ByUsuarioIdOrderByOcurridoEnDescIdDesc(Long usuarioId);

    List<RegistroAcceso> findTop200ByUsuarioIdAndResultadoOrderByOcurridoEnDescIdDesc(Long usuarioId,
                                                                                     ResultadoAcceso resultado);
}
