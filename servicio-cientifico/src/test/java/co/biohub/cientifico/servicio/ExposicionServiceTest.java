package co.biohub.cientifico.servicio;

import co.biohub.cientifico.dominio.RegistroAcceso;
import co.biohub.cientifico.dominio.ResultadoAcceso;
import co.biohub.cientifico.dominio.TipoAccionSeguridad;
import co.biohub.cientifico.repositorio.AccionSeguridadJdbc;
import co.biohub.cientifico.repositorio.RegistroAccesoRepository;
import co.biohub.cientifico.repositorio.UsuarioConsulta;
import co.biohub.cientifico.repositorio.UsuarioConsulta.UsuarioBasico;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ExposicionServiceTest {

    private final RegistroAccesoRepository accesos = mock(RegistroAccesoRepository.class);
    private final UsuarioConsulta usuarios = mock(UsuarioConsulta.class);
    private final AccionSeguridadJdbc acciones = mock(AccionSeguridadJdbc.class);
    private final ExposicionService servicio = new ExposicionService(accesos, usuarios, acciones);

    @BeforeEach
    void preparar() {
        when(usuarios.porId(7L)).thenReturn(Optional.of(new UsuarioBasico(7L, "ana", "INVESTIGADOR", true)));
        when(usuarios.porNombre("seguridad")).thenReturn(Optional.of(new UsuarioBasico(5L, "seguridad", "SEGURIDAD", true)));
    }

    @Test
    void agrupaLosAccesosAceptadosPorHallazgoYContabilizaLasVeces() {
        when(accesos.findByUsuarioIdAndResultadoAndOcurridoEnBetweenOrderByOcurridoEnDesc(
                any(), any(), any(), any()))
                .thenReturn(List.of(
                        RegistroAcceso.nuevo(7L, 10L, ResultadoAcceso.ACEPTADO),
                        RegistroAcceso.nuevo(7L, 11L, ResultadoAcceso.ACEPTADO),
                        RegistroAcceso.nuevo(7L, 10L, ResultadoAcceso.ACEPTADO)));

        List<ExposicionVista> resultado = servicio.coordenadasExpuestas("seguridad", 7L, null, null);

        assertEquals(2, resultado.size());
        assertEquals(10L, resultado.get(0).hallazgoId());
        assertEquals(2, resultado.get(0).consultas());
        assertEquals(11L, resultado.get(1).hallazgoId());
        assertEquals(1, resultado.get(1).consultas());
    }

    @Test
    void soloPideLosAccesosAceptadosYDejaRegistradaLaConsulta() {
        when(accesos.findByUsuarioIdAndResultadoAndOcurridoEnBetweenOrderByOcurridoEnDesc(
                any(), any(), any(), any())).thenReturn(List.of());

        servicio.coordenadasExpuestas("seguridad", 7L, null, null);

        verify(accesos).findByUsuarioIdAndResultadoAndOcurridoEnBetweenOrderByOcurridoEnDesc(
                eq(7L), eq(ResultadoAcceso.ACEPTADO), any(), any());
        verify(acciones).registrar(TipoAccionSeguridad.CONSULTA_EXPOSICION, 5L, 7L);
    }

    @Test
    void unPeriodoInvertidoSeRechazaSinRegistrarNada() {
        LocalDateTime ahora = LocalDateTime.now();

        assertThrows(SolicitudInvalidaException.class,
                () -> servicio.coordenadasExpuestas("seguridad", 7L, ahora, ahora.minusDays(1)));

        verify(acciones, never()).registrar(any(), any(), any());
    }

    @Test
    void siElUsuarioNoExisteNoSeRegistraNada() {
        when(usuarios.porId(99L)).thenReturn(Optional.empty());

        assertThrows(RecursoNoEncontradoException.class,
                () -> servicio.coordenadasExpuestas("seguridad", 99L, null, null));

        verify(acciones, never()).registrar(any(), any(), any());
    }
}