package co.biohub.cientifico.servicio;

import co.biohub.cientifico.dominio.Equipo;
import co.biohub.cientifico.dominio.Prestamo;
import co.biohub.cientifico.repositorio.EquipoRepository;
import co.biohub.cientifico.repositorio.PrestamoRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * HU-04: prestar un equipo a un grupo y registrar su devolución.
 * HU-05: un equipo no se puede prestar mientras tenga un préstamo abierto.
 */
@Service
public class PrestamoService {

    private final PrestamoRepository prestamos;
    private final EquipoRepository equipos;

    public PrestamoService(PrestamoRepository prestamos, EquipoRepository equipos) {
        this.prestamos = prestamos;
        this.equipos = equipos;
    }

    @Transactional
    public PrestamoVista prestar(Long equipoId, String grupo, String observacion, String usuario) {
        if (grupo == null || grupo.isBlank()) {
            throw new SolicitudInvalidaException("El grupo es obligatorio");
        }
        Equipo equipo = equipos.findById(equipoId)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe el equipo " + equipoId));
        if (!equipo.isActivo()) {
            throw new SolicitudInvalidaException("El equipo " + equipo.getCodigo() + " no está activo");
        }

        // HU-05: sin doble asignación
        prestamos.findByEquipoIdAndDevueltoEnIsNull(equipoId).ifPresent(abierto -> {
            throw new ConflictoException("El equipo " + equipo.getCodigo()
                    + " ya está prestado al grupo " + abierto.getGrupo());
        });

        String observacionLimpia = observacion == null || observacion.isBlank() ? null : observacion.trim();
        try {
            Prestamo guardado = prestamos.saveAndFlush(
                    Prestamo.nuevo(equipoId, grupo.trim(), observacionLimpia, usuario));
            return vista(guardado, equipo.getCodigo());
        } catch (DataIntegrityViolationException e) {
            // dos préstamos simultáneos del mismo equipo: el UNIQUE de la base de datos detiene el segundo
            throw new ConflictoException("El equipo " + equipo.getCodigo() + " ya está prestado");
        }
    }

    @Transactional
    public PrestamoVista devolver(Long prestamoId, String usuario) {
        Prestamo prestamo = prestamos.findById(prestamoId)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe el préstamo " + prestamoId));

        if (prestamos.cerrar(prestamoId, LocalDateTime.now(), usuario) == 0) {
            throw new ConflictoException("El préstamo " + prestamoId + " ya fue devuelto");
        }
        Prestamo actualizado = prestamos.findById(prestamoId).orElse(prestamo);
        String codigo = equipos.findById(actualizado.getEquipoId()).map(Equipo::getCodigo).orElse(null);
        return vista(actualizado, codigo);
    }

    @Transactional(readOnly = true)
    public List<PrestamoVista> listar(boolean soloAbiertos) {
        List<Prestamo> lista = soloAbiertos
                ? prestamos.findByDevueltoEnIsNullOrderByPrestadoEnDesc()
                : prestamos.findAllByOrderByPrestadoEnDesc();

        List<Long> ids = lista.stream().map(Prestamo::getEquipoId).distinct().toList();
        Map<Long, String> codigos = equipos.findAllById(ids).stream()
                .collect(Collectors.toMap(Equipo::getId, Equipo::getCodigo, (a, b) -> a, java.util.HashMap::new));
        return lista.stream().map(p -> vista(p, codigos.get(p.getEquipoId()))).toList();
    }

    private PrestamoVista vista(Prestamo p, String codigoEquipo) {
        return new PrestamoVista(p.getId(), p.getEquipoId(), codigoEquipo, p.getGrupo(), p.getObservacion(),
                p.getPrestadoPor(), p.getPrestadoEn(), p.getDevueltoEn(), p.getDevueltoPor());
    }
}
