package co.biohub.cientifico.seguridad;

import co.biohub.cientifico.repositorio.AccionSeguridadJdbc;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SesionesRevocadasTest {

    private final AccionSeguridadJdbc acciones = mock(AccionSeguridadJdbc.class);
    private final SesionesRevocadas revocadas = new SesionesRevocadas(acciones);
    private final LocalDateTime revocacion = LocalDateTime.of(2026, 11, 24, 12, 0, 0);

    @Test
    void sinRevocacionLaSesionSigueValida() {
        when(acciones.ultimaRevocacion("ana")).thenReturn(Optional.empty());
        assertFalse(revocadas.estaRevocada("ana", revocacion.atZone(ZoneId.systemDefault()).toInstant()));
    }

    @Test
    void unTokenEmitidoAntesDeLaRevocacionQuedaRevocado() {
        when(acciones.ultimaRevocacion("ana")).thenReturn(Optional.of(revocacion));
        var antes = revocacion.minusMinutes(10).atZone(ZoneId.systemDefault()).toInstant();
        assertTrue(revocadas.estaRevocada("ana", antes));
    }

    @Test
    void unTokenEmitidoDespuesDeLaRevocacionEsValido() {
        when(acciones.ultimaRevocacion("ana")).thenReturn(Optional.of(revocacion));
        var despues = revocacion.plusMinutes(10).atZone(ZoneId.systemDefault()).toInstant();
        assertFalse(revocadas.estaRevocada("ana", despues));
    }
}