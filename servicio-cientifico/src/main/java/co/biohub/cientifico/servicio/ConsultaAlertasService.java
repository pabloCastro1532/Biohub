package co.biohub.cientifico.servicio;

import co.biohub.cientifico.dominio.AlertaSeguridad;
import co.biohub.cientifico.repositorio.AlertaSeguridadRepository;
import co.biohub.cientifico.repositorio.UsuarioConsulta;
import co.biohub.cientifico.repositorio.UsuarioConsulta.UsuarioBasico;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** HU-26: lista las alertas más recientes para el responsable de seguridad. */
@Service
public class ConsultaAlertasService {

    private final AlertaSeguridadRepository alertas;
    private final UsuarioConsulta usuarios;

    public ConsultaAlertasService(AlertaSeguridadRepository alertas, UsuarioConsulta usuarios) {
        this.alertas = alertas;
        this.usuarios = usuarios;
    }

    @Transactional(readOnly = true)
    public List<AlertaVista> listar() {
        Map<Long, String> nombres = new HashMap<>();
        return alertas.findTop200ByOrderByGeneradaEnDescIdDesc().stream()
                .map(a -> vista(a, nombres.computeIfAbsent(a.getUsuarioId(), id ->
                        usuarios.porId(id).map(UsuarioBasico::nombreUsuario).orElse("usuario " + id))))
                .toList();
    }

    private static AlertaVista vista(AlertaSeguridad a, String usuario) {
        return new AlertaVista(a.getId(), a.getUsuarioId(), usuario, a.getTipo(), a.getCantidad(),
                a.getVentanaMinutos(), a.getGeneradaEn(), a.isEnviada());
    }
}
