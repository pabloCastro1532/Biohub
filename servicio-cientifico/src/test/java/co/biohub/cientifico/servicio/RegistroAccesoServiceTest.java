package co.biohub.cientifico.servicio;

import co.biohub.cientifico.dominio.RegistroAcceso;
import co.biohub.cientifico.dominio.ResultadoAcceso;
import co.biohub.cientifico.repositorio.HallazgoRepository;
import co.biohub.cientifico.repositorio.RegistroAccesoRepository;
import co.biohub.cientifico.repositorio.UsuarioConsulta;
import co.biohub.cientifico.repositorio.UsuarioConsulta.UsuarioBasico;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** HU-25: cada consulta de ubicación exacta, aceptada o rechazada, deja una fila y un evento sin coordenadas (ASR-4). */
class RegistroAccesoServiceTest {

    private RegistroAccesoRepository accesos;
    private HallazgoRepository hallazgos;
    private UsuarioConsulta usuarios;
    private ApplicationEventPublisher eventos;
    private RegistroAccesoService servicio;

    @BeforeEach
    void preparar() {
        accesos = mock(RegistroAccesoRepository.class);
        hallazgos = mock(HallazgoRepository.class);
        usuarios = mock(UsuarioConsulta.class);
        eventos = mock(ApplicationEventPublisher.class);
        servicio = new RegistroAccesoService(accesos, hallazgos, usuarios, eventos);

        when(usuarios.porNombre("director")).thenReturn(Optional.of(new UsuarioBasico(2L, "director", "DIRECTOR", true)));
        when(usuarios.porId(2L)).thenReturn(Optional.of(new UsuarioBasico(2L, "director", "DIRECTOR", true)));
        when(usuarios.porId(3L)).thenReturn(Optional.of(new UsuarioBasico(3L, "visitante", "VISITANTE", true)));
        when(hallazgos.existsById(10L)).thenReturn(true);
        when(accesos.save(any(RegistroAcceso.class))).thenAnswer(inv -> {
            RegistroAcceso r = inv.getArgument(0);
            ReflectionTestUtils.setField(r, "id", 99L);
            return r;
        });
    }

    @Test
    void hu25_un_acceso_aceptado_guarda_usuario_hallazgo_y_resultado() {
        AccesoVista vista = servicio.registrar("director", 10L, ResultadoAcceso.ACEPTADO);

        ArgumentCaptor<RegistroAcceso> captor = ArgumentCaptor.forClass(RegistroAcceso.class);
        verify(accesos).save(captor.capture());
        assertEquals(2L, captor.getValue().getUsuarioId());
        assertEquals(10L, captor.getValue().getHallazgoId());
        assertEquals(ResultadoAcceso.ACEPTADO, captor.getValue().getResultado());
        assertEquals(99L, vista.id());
        assertEquals("director", vista.usuario());
    }

    @Test
    void hu25_un_acceso_rechazado_tambien_queda_registrado() {
        AccesoVista vista = servicio.registrar("director", 10L, ResultadoAcceso.RECHAZADO);

        ArgumentCaptor<RegistroAcceso> captor = ArgumentCaptor.forClass(RegistroAcceso.class);
        verify(accesos).save(captor.capture());
        assertEquals(ResultadoAcceso.RECHAZADO, captor.getValue().getResultado());
        assertEquals(ResultadoAcceso.RECHAZADO, vista.resultado());
    }

    @Test
    void hu25_el_evento_lleva_usuario_hallazgo_y_resultado() {
        servicio.registrar("director", 10L, ResultadoAcceso.RECHAZADO);

        ArgumentCaptor<AccesoCoordenadasEvento> captor = ArgumentCaptor.forClass(AccesoCoordenadasEvento.class);
        verify(eventos).publishEvent(captor.capture());
        assertEquals(2L, captor.getValue().usuarioId());
        assertEquals(10L, captor.getValue().hallazgoId());
        assertEquals(ResultadoAcceso.RECHAZADO, captor.getValue().resultado());
    }

    @Test
    void hu25_la_vista_y_el_evento_no_tienen_campos_de_coordenadas() {
        for (Class<?> tipo : List.of(AccesoVista.class, AccesoCoordenadasEvento.class)) {
            for (var componente : tipo.getRecordComponents()) {
                String nombre = componente.getName().toLowerCase();
                assertEquals(false, nombre.contains("latitud") || nombre.contains("longitud"),
                        tipo.getSimpleName() + " no debe exponer coordenadas: " + nombre);
            }
        }
    }

    @Test
    void hu25_un_usuario_desconocido_no_se_registra() {
        when(usuarios.porNombre("fantasma")).thenReturn(Optional.empty());

        assertThrows(RecursoNoEncontradoException.class,
                () -> servicio.registrar("fantasma", 10L, ResultadoAcceso.ACEPTADO));
        verify(accesos, never()).save(any());
        verify(eventos, never()).publishEvent(any(Object.class));
    }

    @Test
    void hu25_un_hallazgo_inexistente_no_se_registra() {
        assertThrows(RecursoNoEncontradoException.class,
                () -> servicio.registrar("director", 404L, ResultadoAcceso.ACEPTADO));
        verify(accesos, never()).save(any());
    }

    @Test
    void hu25_el_resultado_es_obligatorio() {
        assertThrows(SolicitudInvalidaException.class, () -> servicio.registrar("director", 10L, null));
        verify(accesos, never()).save(any());
    }

    @Test
    void hu25_listar_sin_filtros_trae_los_recientes_con_el_nombre_del_usuario() {
        when(accesos.findTop200ByOrderByOcurridoEnDescIdDesc()).thenReturn(List.of(
                RegistroAcceso.nuevo(2L, 10L, ResultadoAcceso.ACEPTADO),
                RegistroAcceso.nuevo(3L, 10L, ResultadoAcceso.RECHAZADO)));

        List<AccesoVista> lista = servicio.listar(null, null);

        assertEquals(2, lista.size());
        assertEquals("director", lista.get(0).usuario());
        assertEquals("visitante", lista.get(1).usuario());
    }

    @Test
    void hu25_listar_filtra_por_usuario_y_resultado() {
        when(accesos.findTop200ByUsuarioIdAndResultadoOrderByOcurridoEnDescIdDesc(3L, ResultadoAcceso.RECHAZADO))
                .thenReturn(List.of(RegistroAcceso.nuevo(3L, 10L, ResultadoAcceso.RECHAZADO)));

        List<AccesoVista> lista = servicio.listar(3L, ResultadoAcceso.RECHAZADO);

        assertEquals(1, lista.size());
        assertEquals(ResultadoAcceso.RECHAZADO, lista.get(0).resultado());
    }

    @Test
    void hu25_listar_solo_por_resultado() {
        when(accesos.findTop200ByResultadoOrderByOcurridoEnDescIdDesc(ResultadoAcceso.ACEPTADO))
                .thenReturn(List.of(RegistroAcceso.nuevo(2L, 10L, ResultadoAcceso.ACEPTADO)));

        assertEquals(1, servicio.listar(null, ResultadoAcceso.ACEPTADO).size());
    }
}
