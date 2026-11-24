package co.biohub.cientifico.servicio;

import co.biohub.cientifico.dominio.TipoAccionSeguridad;
import co.biohub.cientifico.repositorio.AccionSeguridadJdbc;
import co.biohub.cientifico.repositorio.UsuarioConsulta;
import co.biohub.cientifico.repositorio.UsuarioConsulta.UsuarioBasico;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RevocacionServiceTest {

    private final UsuarioConsulta usuarios = mock(UsuarioConsulta.class);
    private final AccionSeguridadJdbc acciones = mock(AccionSeguridadJdbc.class);
    private final RevocacionService servicio = new RevocacionService(usuarios, acciones);

    @Test
    void revocarRegistraQuienLoHizoYDesactivaAlUsuario() {
        when(usuarios.porId(7L)).thenReturn(Optional.of(new UsuarioBasico(7L, "ana", "INVESTIGADOR", true)));
        when(usuarios.porNombre("seguridad")).thenReturn(Optional.of(new UsuarioBasico(5L, "seguridad", "SEGURIDAD", true)));

        servicio.revocar("seguridad", 7L);

        verify(acciones).registrar(TipoAccionSeguridad.REVOCACION, 5L, 7L);
        verify(acciones).desactivarUsuario(7L);
    }

    @Test
    void siElUsuarioNoExisteNoSeEscribeNada() {
        when(usuarios.porId(99L)).thenReturn(Optional.empty());

        assertThrows(RecursoNoEncontradoException.class, () -> servicio.revocar("seguridad", 99L));

        verify(acciones, never()).registrar(any(), any(), any());
        verify(acciones, never()).desactivarUsuario(any());
    }
}