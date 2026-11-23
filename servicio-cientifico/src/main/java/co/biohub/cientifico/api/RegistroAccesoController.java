package co.biohub.cientifico.api;

import co.biohub.cientifico.dominio.ResultadoAcceso;
import co.biohub.cientifico.servicio.AccesoVista;
import co.biohub.cientifico.servicio.RegistroAccesoService;
import co.biohub.cientifico.servicio.SolicitudInvalidaException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * HU-25: consulta del registro de accesos a coordenadas exactas. Solo lectura: el registro lo escribe el servicio
 * cuando alguien consulta una ubicación exacta, nunca un cliente por HTTP. Responsable de seguridad y auditor.
 */
@RestController
@RequestMapping("/api/accesos")
public class RegistroAccesoController {

    private final RegistroAccesoService servicio;

    public RegistroAccesoController(RegistroAccesoService servicio) {
        this.servicio = servicio;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('SEGURIDAD','AUDITOR')")
    public List<AccesoVista> listar(@RequestParam(required = false) Long usuarioId,
                                    @RequestParam(required = false) String resultado) {
        ResultadoAcceso filtro = null;
        if (resultado != null && !resultado.isBlank()) {
            try {
                filtro = ResultadoAcceso.valueOf(resultado.trim().toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new SolicitudInvalidaException("El resultado debe ser ACEPTADO o RECHAZADO");
            }
        }
        return servicio.listar(usuarioId, filtro);
    }
}
