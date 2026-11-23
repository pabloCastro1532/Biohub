package co.biohub.cientifico.servicio;

import co.biohub.cientifico.dominio.Equipo;
import co.biohub.cientifico.repositorio.EquipoRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

/** HU-03: registrar y consultar equipos de campo. */
@Service
public class EquipoService {

    private final EquipoRepository equipos;

    public EquipoService(EquipoRepository equipos) {
        this.equipos = equipos;
    }

    @Transactional
    public Equipo registrar(String codigo, String nombre, String tipo, String descripcion) {
        String codigoLimpio = limpiar(codigo, "El código").toUpperCase(Locale.ROOT);
        String nombreLimpio = limpiar(nombre, "El nombre");
        String tipoLimpio = limpiar(tipo, "El tipo");
        String descripcionLimpia = descripcion == null || descripcion.isBlank() ? null : descripcion.trim();

        if (equipos.existsByCodigo(codigoLimpio)) {
            throw new ConflictoException("Ya existe un equipo con el código " + codigoLimpio);
        }
        try {
            return equipos.saveAndFlush(Equipo.nuevo(codigoLimpio, nombreLimpio, tipoLimpio, descripcionLimpia));
        } catch (DataIntegrityViolationException e) {
            // dos registros simultáneos con el mismo código: la restricción UNIQUE de la base lo detiene
            throw new ConflictoException("Ya existe un equipo con el código " + codigoLimpio);
        }
    }

    @Transactional(readOnly = true)
    public List<Equipo> listar() {
        return equipos.findAllByOrderByNombreAsc();
    }

    @Transactional(readOnly = true)
    public Equipo obtener(Long id) {
        return equipos.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe el equipo " + id));
    }

    private static String limpiar(String valor, String campo) {
        if (valor == null || valor.isBlank()) {
            throw new SolicitudInvalidaException(campo + " es obligatorio");
        }
        return valor.trim();
    }
}
