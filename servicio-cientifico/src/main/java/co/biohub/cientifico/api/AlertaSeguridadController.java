package co.biohub.cientifico.api;

import co.biohub.cientifico.servicio.AlertaVista;
import co.biohub.cientifico.servicio.ConsultaAlertasService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** HU-26: alertas del monitor de seguridad. Solo lectura: las alertas las genera el monitor, nunca un cliente. */
@RestController
@RequestMapping("/api/alertas")
public class AlertaSeguridadController {

    private final ConsultaAlertasService servicio;

    public AlertaSeguridadController(ConsultaAlertasService servicio) {
        this.servicio = servicio;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('SEGURIDAD','AUDITOR')")
    public List<AlertaVista> listar() {
        return servicio.listar();
    }
}
