package co.biohub.cientifico.mensajeria;

import co.biohub.cientifico.servicio.MonitorSeguridad;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/** HU-26: el evento acceso.coordenadas.registrado llega al monitor con el usuario y la hora del propio evento. */
class ConsumidorAccesosTest {

    private MonitorSeguridad monitor;
    private ConsumidorAccesos consumidor;

    @BeforeEach
    void preparar() {
        monitor = mock(MonitorSeguridad.class);
        consumidor = new ConsumidorAccesos(monitor);
    }

    private static Map<String, Object> mensaje(Object usuarioId, Object ocurridoEn) {
        Map<String, Object> m = new HashMap<>();
        m.put("id_evento", "6f1c1d0e-0000-4000-8000-000000000001");
        m.put("ocurrido_en", ocurridoEn);
        m.put("usuario_id", usuarioId);
        m.put("hallazgo_id", 1);
        m.put("resultado", "RECHAZADO");
        return m;
    }

    @Test
    void hu26_un_evento_valido_se_entrega_al_monitor_con_la_hora_del_evento() {
        consumidor.alRecibir(mensaje(3, "2026-11-23T10:00:00.250Z"));

        verify(monitor).evaluar(3L, LocalDateTime.of(2026, 11, 23, 10, 0, 0, 250_000_000));
    }

    @Test
    void hu26_acepta_la_hora_sin_segundos() {
        consumidor.alRecibir(mensaje(3, "2026-11-23T10:00Z"));

        verify(monitor).evaluar(3L, LocalDateTime.of(2026, 11, 23, 10, 0, 0));
    }

    @Test
    void hu26_un_evento_incompleto_se_descarta_sin_romper_el_consumidor() {
        assertDoesNotThrow(() -> consumidor.alRecibir(mensaje(null, "2026-11-23T10:00:00Z")));
        assertDoesNotThrow(() -> consumidor.alRecibir(mensaje(3, "no es una fecha")));

        verify(monitor, never()).evaluar(anyLong(), any(LocalDateTime.class));
    }

    @Test
    void hu26_si_el_monitor_falla_no_se_relanza_para_no_reintentar_para_siempre() {
        doThrow(new IllegalStateException("BD caída")).when(monitor).evaluar(anyLong(), any(LocalDateTime.class));

        assertDoesNotThrow(() -> consumidor.alRecibir(mensaje(3, "2026-11-23T10:00:00Z")));
    }
}
