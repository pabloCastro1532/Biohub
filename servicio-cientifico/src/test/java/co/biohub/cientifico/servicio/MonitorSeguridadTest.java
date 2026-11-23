package co.biohub.cientifico.servicio;

import co.biohub.cientifico.dominio.AlertaSeguridad;
import co.biohub.cientifico.dominio.ResultadoAcceso;
import co.biohub.cientifico.dominio.TipoAlerta;
import co.biohub.cientifico.notificacion.Notificador;
import co.biohub.cientifico.repositorio.AlertaSeguridadRepository;
import co.biohub.cientifico.repositorio.ConteoAccesosRepository;
import co.biohub.cientifico.repositorio.UsuarioConsulta;
import co.biohub.cientifico.repositorio.UsuarioConsulta.UsuarioBasico;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** HU-26: avisar cuando un usuario consulta demasiado (más de 50 en una hora) o acumula rechazos (5 en 10 minutos). */
class MonitorSeguridadTest {

    private static final LocalDateTime AHORA = LocalDateTime.of(2026, 11, 23, 10, 0, 0);

    private ConteoAccesosRepository conteo;
    private AlertaSeguridadRepository alertas;
    private UsuarioConsulta usuarios;
    private Notificador notificador;
    private MonitorSeguridad monitor;

    @BeforeEach
    void preparar() {
        conteo = mock(ConteoAccesosRepository.class);
        alertas = mock(AlertaSeguridadRepository.class);
        usuarios = mock(UsuarioConsulta.class);
        notificador = mock(Notificador.class);
        UmbralesSeguridad umbrales = new UmbralesSeguridad(50, 60, 5, 10, List.of("seguridad@biohub.local"));
        monitor = new MonitorSeguridad(conteo, alertas, usuarios, notificador, umbrales);

        when(usuarios.porId(3L)).thenReturn(Optional.of(new UsuarioBasico(3L, "visitante", "VISITANTE", true)));
        when(alertas.save(any(AlertaSeguridad.class))).thenAnswer(inv -> inv.getArgument(0));
        cuenta(0, 0);
    }

    /** Fija cuántas consultas (todas) y cuántos rechazos devuelve el conteo para el usuario 3. */
    private void cuenta(long consultas, long rechazos) {
        when(conteo.countByUsuarioIdAndOcurridoEnBetween(eq(3L), any(), any())).thenReturn(consultas);
        when(conteo.countByUsuarioIdAndResultadoAndOcurridoEnBetween(eq(3L), eq(ResultadoAcceso.RECHAZADO), any(), any()))
                .thenReturn(rechazos);
    }

    @Test
    void hu26_cincuenta_consultas_en_una_hora_no_generan_alerta() {
        cuenta(50, 0);

        List<AlertaSeguridad> nuevas = monitor.evaluar(3L, AHORA);

        assertTrue(nuevas.isEmpty());
        verify(notificador, never()).enviar(anyString(), anyString(), anyList());
        verify(alertas, never()).save(any(AlertaSeguridad.class));
    }

    @Test
    void hu26_la_consulta_51_en_una_hora_genera_alerta_y_envia_correo() {
        cuenta(51, 0);

        List<AlertaSeguridad> nuevas = monitor.evaluar(3L, AHORA);

        assertEquals(1, nuevas.size());
        AlertaSeguridad a = nuevas.get(0);
        assertEquals(TipoAlerta.CONSULTAS_EXCESIVAS, a.getTipo());
        assertEquals(3L, a.getUsuarioId());
        assertEquals(51, a.getCantidad());
        assertEquals(60, a.getVentanaMinutos());
        assertEquals(AHORA, a.getGeneradaEn());
        assertTrue(a.isEnviada());

        ArgumentCaptor<String> asunto = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> cuerpo = ArgumentCaptor.forClass(String.class);
        verify(notificador).enviar(asunto.capture(), cuerpo.capture(), eq(List.of("seguridad@biohub.local")));
        assertTrue(asunto.getValue().contains("visitante"));
        assertTrue(cuerpo.getValue().contains("51"));
    }

    @Test
    void hu26_cuatro_rechazos_en_diez_minutos_no_generan_alerta() {
        cuenta(4, 4);

        assertTrue(monitor.evaluar(3L, AHORA).isEmpty());
        verify(notificador, never()).enviar(anyString(), anyString(), anyList());
    }

    @Test
    void hu26_cinco_rechazos_en_diez_minutos_generan_alerta() {
        cuenta(5, 5);

        List<AlertaSeguridad> nuevas = monitor.evaluar(3L, AHORA);

        assertEquals(1, nuevas.size());
        assertEquals(TipoAlerta.RECHAZOS_REPETIDOS, nuevas.get(0).getTipo());
        assertEquals(5, nuevas.get(0).getCantidad());
        assertEquals(10, nuevas.get(0).getVentanaMinutos());
        verify(notificador).enviar(anyString(), anyString(), anyList());
    }

    @Test
    void hu26_si_se_cumplen_los_dos_patrones_hay_dos_alertas() {
        cuenta(60, 6);

        List<AlertaSeguridad> nuevas = monitor.evaluar(3L, AHORA);

        assertEquals(2, nuevas.size());
        assertEquals(TipoAlerta.CONSULTAS_EXCESIVAS, nuevas.get(0).getTipo());
        assertEquals(TipoAlerta.RECHAZOS_REPETIDOS, nuevas.get(1).getTipo());
    }

    @Test
    void hu26_no_repite_la_alerta_si_ya_se_aviso_en_la_misma_ventana() {
        cuenta(55, 0);
        when(alertas.existsByUsuarioIdAndTipoAndGeneradaEnBetween(eq(3L), eq(TipoAlerta.CONSULTAS_EXCESIVAS), any(), any()))
                .thenReturn(true);

        assertTrue(monitor.evaluar(3L, AHORA).isEmpty());
        verify(notificador, never()).enviar(anyString(), anyString(), anyList());
        verify(alertas, never()).save(any(AlertaSeguridad.class));
    }

    @Test
    void hu26_las_ventanas_terminan_en_la_hora_del_evento() {
        monitor.evaluar(3L, AHORA);

        verify(conteo).countByUsuarioIdAndOcurridoEnBetween(3L, AHORA.minusMinutes(60), AHORA);
        verify(conteo).countByUsuarioIdAndResultadoAndOcurridoEnBetween(
                3L, ResultadoAcceso.RECHAZADO, AHORA.minusMinutes(10), AHORA);
    }

    @Test
    void hu26_si_el_correo_falla_la_alerta_se_guarda_igual_como_no_enviada() {
        cuenta(51, 0);
        doThrow(new IllegalStateException("SMTP caído")).when(notificador).enviar(anyString(), anyString(), anyList());

        List<AlertaSeguridad> nuevas = monitor.evaluar(3L, AHORA);

        assertEquals(1, nuevas.size());
        assertFalse(nuevas.get(0).isEnviada());
        verify(alertas).save(any(AlertaSeguridad.class));
    }

    @Test
    void hu26_los_umbrales_se_pueden_cambiar_sin_tocar_el_codigo() {
        UmbralesSeguridad estrictos = new UmbralesSeguridad(5, 30, 2, 5, List.of("jefe@biohub.local"));
        MonitorSeguridad estricto = new MonitorSeguridad(conteo, alertas, usuarios, notificador, estrictos);
        cuenta(6, 2);

        List<AlertaSeguridad> nuevas = estricto.evaluar(3L, AHORA);

        assertEquals(2, nuevas.size());
        verify(conteo).countByUsuarioIdAndOcurridoEnBetween(3L, AHORA.minusMinutes(30), AHORA);
        verify(notificador, org.mockito.Mockito.times(2)).enviar(anyString(), anyString(), eq(List.of("jefe@biohub.local")));
    }
}
