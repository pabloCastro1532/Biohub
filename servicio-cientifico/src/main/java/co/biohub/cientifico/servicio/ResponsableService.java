package co.biohub.cientifico.servicio;

import co.biohub.cientifico.dominio.AsignacionResponsable;
import co.biohub.cientifico.dominio.Equipo;
import co.biohub.cientifico.repositorio.AsignacionResponsableRepository;
import co.biohub.cientifico.repositorio.EquipoRepository;
import co.biohub.cientifico.repositorio.UsuarioConsulta;
import co.biohub.cientifico.repositorio.UsuarioConsulta.UsuarioBasico;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * HU-06: asignar un equipo a un investigador responsable. Reasignar cierra la asignación anterior y la conserva.
 * HU-07: consultar el responsable actual de un equipo.
 */
@Service
public class ResponsableService {

    private static final String ROL_RESPONSABLE = "INVESTIGADOR";

    private final AsignacionResponsableRepository asignaciones;
    private final EquipoRepository equipos;
    private final UsuarioConsulta usuarios;

    public ResponsableService(AsignacionResponsableRepository asignaciones, EquipoRepository equipos,
                              UsuarioConsulta usuarios) {
        this.asignaciones = asignaciones;
        this.equipos = equipos;
        this.usuarios = usuarios;
    }

    @Transactional
    public ResponsableVista asignar(Long equipoId, String nombreUsuario, String asignadoPor) {
        if (nombreUsuario == null || nombreUsuario.isBlank()) {
            throw new SolicitudInvalidaException("El investigador es obligatorio");
        }
        String nombre = nombreUsuario.trim();
        Equipo equipo = equipoExistente(equipoId);
        if (!equipo.isActivo()) {
            throw new SolicitudInvalidaException("El equipo " + equipo.getCodigo() + " no está activo");
        }
        UsuarioBasico investigador = usuarios.porNombre(nombre)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe el usuario " + nombre));
        if (!ROL_RESPONSABLE.equals(investigador.rol()) || !investigador.activo()) {
            throw new SolicitudInvalidaException("El usuario " + nombre + " no es un investigador activo");
        }

        Optional<AsignacionResponsable> vigente = asignaciones.findByEquipoIdAndHastaIsNull(equipoId);
        if (vigente.isPresent() && vigente.get().getUsuarioId().equals(investigador.id())) {
            throw new ConflictoException(nombre + " ya es el responsable del equipo " + equipo.getCodigo());
        }

        LocalDateTime ahora = LocalDateTime.now();
        vigente.ifPresent(anterior -> asignaciones.cerrar(anterior.getId(), ahora));
        try {
            AsignacionResponsable nueva = asignaciones.saveAndFlush(
                    AsignacionResponsable.nueva(equipoId, investigador.id(), asignadoPor, ahora));
            return vista(equipo, nueva, investigador.nombreUsuario());
        } catch (DataIntegrityViolationException e) {
            // dos asignaciones simultáneas del mismo equipo: el UNIQUE de la base de datos detiene la segunda
            throw new ConflictoException("El equipo " + equipo.getCodigo() + " acaba de ser asignado a otra persona");
        }
    }

    @Transactional(readOnly = true)
    public ResponsableVista consultar(Long equipoId) {
        Equipo equipo = equipoExistente(equipoId);
        return asignaciones.findByEquipoIdAndHastaIsNull(equipoId)
                .map(a -> vista(equipo, a, nombreDe(a.getUsuarioId())))
                .orElseGet(() -> new ResponsableVista(equipo.getId(), equipo.getCodigo(), null, null, null));
    }

    /** Los equipos que hoy tienen responsable. */
    @Transactional(readOnly = true)
    public List<ResponsableVista> listarVigentes() {
        return asignaciones.findByHastaIsNull().stream()
                .map(a -> equipos.findById(a.getEquipoId())
                        .map(e -> vista(e, a, nombreDe(a.getUsuarioId())))
                        .orElse(null))
                .filter(v -> v != null)
                .toList();
    }

    private Equipo equipoExistente(Long equipoId) {
        return equipos.findById(equipoId)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe el equipo " + equipoId));
    }

    private String nombreDe(Long usuarioId) {
        return usuarios.porId(usuarioId).map(UsuarioBasico::nombreUsuario).orElse(null);
    }

    private ResponsableVista vista(Equipo equipo, AsignacionResponsable a, String responsable) {
        return new ResponsableVista(equipo.getId(), equipo.getCodigo(), responsable, a.getAsignadoPor(), a.getAsignadoEn());
    }
}
