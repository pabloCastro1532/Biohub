package co.biohub.cientifico.servicio;

import co.biohub.cientifico.dominio.AlertaSeguridad;
import co.biohub.cientifico.dominio.ResultadoAcceso;
import co.biohub.cientifico.dominio.TipoAlerta;
import co.biohub.cientifico.notificacion.Notificador;
import co.biohub.cientifico.repositorio.AlertaSeguridadRepository;
import co.biohub.cientifico.repositorio.ConteoAccesosRepository;
import co.biohub.cientifico.repositorio.UsuarioConsulta;
import co.biohub.cientifico.repositorio.UsuarioConsulta.UsuarioBasico;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * HU-26: vigila los accesos a coordenadas exactas y avisa por correo cuando un usuario tiene un patrón anómalo (ASR-4):
 * demasiadas consultas en una hora o varios rechazos en pocos minutos.
 *
 * Se ejecuta cada vez que llega el evento acceso.coordenadas.registrado. Cuenta las filas de registro_acceso
 * (la fuente de verdad) en la ventana que termina en el momento del evento, así que procesar dos veces el mismo
 * evento da el mismo resultado, y no se repite la alerta mientras el usuario siga dentro de la misma ventana.
 */
@Service
public class MonitorSeguridad {

    private static final Logger LOG = LoggerFactory.getLogger(MonitorSeguridad.class);

    private final ConteoAccesosRepository conteo;
    private final AlertaSeguridadRepository alertas;
    private final UsuarioConsulta usuarios;
    private final Notificador notificador;
    private final UmbralesSeguridad umbrales;

    public MonitorSeguridad(ConteoAccesosRepository conteo, AlertaSeguridadRepository alertas,
                            UsuarioConsulta usuarios, Notificador notificador, UmbralesSeguridad umbrales) {
        this.conteo = conteo;
        this.alertas = alertas;
        this.usuarios = usuarios;
        this.notificador = notificador;
        this.umbrales = umbrales;
    }

    /** Revisa al usuario hasta el instante del evento y devuelve las alertas nuevas (puede ser ninguna, una o dos). */
    @Transactional
    public List<AlertaSeguridad> evaluar(Long usuarioId, LocalDateTime hasta) {
        List<AlertaSeguridad> nuevas = new ArrayList<>();

        long consultas = conteo.countByUsuarioIdAndOcurridoEnBetween(
                usuarioId, hasta.minusMinutes(umbrales.ventanaConsultasMinutos()), hasta);
        if (consultas > umbrales.maximoConsultasPorHora()) {
            alertar(usuarioId, TipoAlerta.CONSULTAS_EXCESIVAS, consultas, umbrales.ventanaConsultasMinutos(), hasta)
                    .ifPresent(nuevas::add);
        }

        long rechazos = conteo.countByUsuarioIdAndResultadoAndOcurridoEnBetween(
                usuarioId, ResultadoAcceso.RECHAZADO, hasta.minusMinutes(umbrales.ventanaRechazosMinutos()), hasta);
        if (rechazos >= umbrales.rechazosParaAlerta()) {
            alertar(usuarioId, TipoAlerta.RECHAZOS_REPETIDOS, rechazos, umbrales.ventanaRechazosMinutos(), hasta)
                    .ifPresent(nuevas::add);
        }
        return nuevas;
    }

    private Optional<AlertaSeguridad> alertar(Long usuarioId, TipoAlerta tipo, long cantidad,
                                                         int ventanaMinutos, LocalDateTime hasta) {
        if (alertas.existsByUsuarioIdAndTipoAndGeneradaEnBetween(
                usuarioId, tipo, hasta.minusMinutes(ventanaMinutos), hasta)) {
            return Optional.empty();   // ya se avisó de este patrón en esta ventana
        }
        String nombre = usuarios.porId(usuarioId).map(UsuarioBasico::nombreUsuario).orElse("usuario " + usuarioId);
        boolean enviada = enviarCorreo(nombre, tipo, cantidad, ventanaMinutos, hasta);
        AlertaSeguridad alerta = alertas.save(
                AlertaSeguridad.nueva(usuarioId, tipo, (int) cantidad, ventanaMinutos, hasta, enviada));
        LOG.warn("ALERTA DE SEGURIDAD {}: {} con {} accesos en {} minutos (correo {})",
                tipo, nombre, cantidad, ventanaMinutos, enviada ? "enviado" : "NO enviado");
        return Optional.of(alerta);
    }

    private boolean enviarCorreo(String nombre, TipoAlerta tipo, long cantidad, int ventanaMinutos,
                                 LocalDateTime hasta) {
        String que = tipo == TipoAlerta.CONSULTAS_EXCESIVAS
                ? "consultó ubicaciones exactas " + cantidad + " veces"
                : "tuvo " + cantidad + " consultas de ubicación exacta rechazadas";
        String asunto = "[BioHub] Alerta de seguridad: " + nombre + " en ubicaciones exactas";
        String cuerpo = "El usuario " + nombre + " " + que + " en los últimos " + ventanaMinutos + " minutos "
                + "(hasta " + hasta + ").\n\n"
                + "Revise el detalle en Registro de accesos y, si corresponde, revoque su sesión.";
        try {
            notificador.enviar(asunto, cuerpo, umbrales.destinatarios());
            return true;
        } catch (RuntimeException e) {
            // La alerta se guarda igual: perder el correo no debe perder el aviso.
            LOG.warn("No se pudo enviar el correo de alerta: {}", e.getMessage());
            return false;
        }
    }
}
