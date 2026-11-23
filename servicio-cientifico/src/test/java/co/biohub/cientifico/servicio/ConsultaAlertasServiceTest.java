package co.biohub.cientifico.servicio;

import co.biohub.cientifico.dominio.AlertaSeguridad;
import co.biohub.cientifico.dominio.TipoAlerta;
import co.biohub.cientifico.repositorio.AlertaSeguridadRepository;
import co.biohub.cientifico.repositorio.UsuarioConsulta;
import co.biohub.cientifico.repositorio.UsuarioConsulta.UsuarioBasico;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** HU-26: el responsable de seguridad ve las alertas con el nombre del usuario. */
class ConsultaAlertasServiceTest {

    private static final LocalDateTime AHORA = LocalDateTime.of(2026, 11, 23, 10, 0, 0);

    @Test
    void hu26_lista_las_alertas_con_el_nombre_del_usuario() {
        AlertaSeguridadRepository alertas = mock(AlertaSeguridadRepository.class);
        UsuarioConsulta usuarios = mock(UsuarioConsulta.class);
        when(alertas.findTop200ByOrderByGeneradaEnDescIdDesc()).thenReturn(List.of(
                AlertaSeguridad.nueva(3L, TipoAlerta.RECHAZOS_REPETIDOS, 5, 10, AHORA, true),
                AlertaSeguridad.nueva(3L, TipoAlerta.CONSULTAS_EXCESIVAS, 51, 60, AHORA.minusHours(2), false)));
        when(usuarios.porId(3L)).thenReturn(Optional.of(new UsuarioBasico(3L, "visitante", "VISITANTE", true)));

        List<AlertaVista> lista = new ConsultaAlertasService(alertas, usuarios).listar();

        assertEquals(2, lista.size());
        assertEquals("visitante", lista.get(0).usuario());
        assertEquals(TipoAlerta.RECHAZOS_REPETIDOS, lista.get(0).tipo());
        assertTrue(lista.get(0).enviada());
        assertEquals(51, lista.get(1).cantidad());
        verify(usuarios, times(1)).porId(3L);   // el nombre del mismo usuario se busca una sola vez
    }

    @Test
    void hu26_si_no_hay_alertas_devuelve_lista_vacia() {
        AlertaSeguridadRepository alertas = mock(AlertaSeguridadRepository.class);
        when(alertas.findTop200ByOrderByGeneradaEnDescIdDesc()).thenReturn(List.of());

        assertTrue(new ConsultaAlertasService(alertas, mock(UsuarioConsulta.class)).listar().isEmpty());
    }
}
