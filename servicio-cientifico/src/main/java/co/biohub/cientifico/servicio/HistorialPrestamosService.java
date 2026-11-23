package co.biohub.cientifico.servicio;

import co.biohub.cientifico.dominio.Equipo;
import co.biohub.cientifico.repositorio.EquipoRepository;
import co.biohub.cientifico.repositorio.PrestamoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** HU-24: historial de préstamos de un equipo. Solo lee la tabla que llenan HU-04 y HU-05. */
@Service
public class HistorialPrestamosService {

    private final PrestamoRepository prestamos;
    private final EquipoRepository equipos;

    public HistorialPrestamosService(PrestamoRepository prestamos, EquipoRepository equipos) {
        this.prestamos = prestamos;
        this.equipos = equipos;
    }

    /** Todos los préstamos del equipo, el más reciente primero. Un equipo que nunca se prestó da una lista vacía. */
    @Transactional(readOnly = true)
    public List<PrestamoVista> deEquipo(Long equipoId) {
        Equipo equipo = equipos.findById(equipoId)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe el equipo " + equipoId));
        return prestamos.findByEquipoIdOrderByPrestadoEnDesc(equipoId).stream()
                .map(p -> new PrestamoVista(p.getId(), p.getEquipoId(), equipo.getCodigo(), p.getGrupo(),
                        p.getObservacion(), p.getPrestadoPor(), p.getPrestadoEn(), p.getDevueltoEn(),
                        p.getDevueltoPor()))
                .toList();
    }
}
