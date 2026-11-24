package co.biohub.cientifico.servicio;

import co.biohub.cientifico.repositorio.UsuarioAdminJdbc;
import co.biohub.cientifico.repositorio.UsuarioAdminJdbc.Fila;
import co.biohub.cientifico.repositorio.UsuarioConsulta;
import co.biohub.cientifico.repositorio.UsuarioConsulta.UsuarioBasico;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UsuarioAdminServiceTest {

    private UsuarioAdminJdbc cuentas;
    private UsuarioConsulta usuarios;
    private PasswordEncoder encoder;
    private UsuarioAdminService servicio;

    @BeforeEach
    void preparar() {
        cuentas = mock(UsuarioAdminJdbc.class);
        usuarios = mock(UsuarioConsulta.class);
        encoder = mock(PasswordEncoder.class);
        servicio = new UsuarioAdminService(cuentas, usuarios, encoder);
        when(encoder.encode(anyString())).thenAnswer(i -> "hash:" + i.getArgument(0));
        when(cuentas.crear(anyString(), anyString(), anyString())).thenAnswer(i ->
                new Fila(10L, i.getArgument(0), i.getArgument(2), true, LocalDateTime.now()));
    }

    @Test
    void creaLaCuentaConElNombreEnMinusculaYLaClaveCifrada() {
        UsuarioVista v = servicio.crear("  Maria.Perez ", "investigador", "clave-segura", "administrador");

        assertEquals("maria.perez", v.nombreUsuario());
        assertEquals("INVESTIGADOR", v.rol());
        assertEquals(true, v.activo());
        verify(cuentas).crear("maria.perez", "hash:clave-segura", "INVESTIGADOR");
    }

    @Test
    void elNombreEsObligatorio() {
        assertEquals("El nombre de usuario es obligatorio",
                assertThrows(SolicitudInvalidaException.class,
                        () -> servicio.crear("   ", "DIRECTOR", "clave-segura", "administrador")).getMessage());
        assertEquals("El nombre de usuario es obligatorio",
                assertThrows(SolicitudInvalidaException.class,
                        () -> servicio.crear(null, "DIRECTOR", "clave-segura", "administrador")).getMessage());
    }

    @Test
    void rechazaUnNombreMuyCortoOMuyLargo() {
        assertThrows(SolicitudInvalidaException.class,
                () -> servicio.crear("ab", "DIRECTOR", "clave-segura", "administrador"));
        assertThrows(SolicitudInvalidaException.class,
                () -> servicio.crear("a".repeat(41), "DIRECTOR", "clave-segura", "administrador"));
        verify(cuentas, never()).crear(anyString(), anyString(), anyString());
    }

    @Test
    void rechazaNombresConEspaciosTildesOSimbolos() {
        for (String malo : List.of("maria perez", "josé", "ana@correo", "-ana", "ñandú")) {
            assertThrows(SolicitudInvalidaException.class,
                    () -> servicio.crear(malo, "DIRECTOR", "clave-segura", "administrador"), malo);
        }
    }

    @Test
    void elRolEsObligatorioYDebeExistir() {
        assertEquals("Elige un rol", assertThrows(SolicitudInvalidaException.class,
                () -> servicio.crear("maria", "", "clave-segura", "administrador")).getMessage());
        assertEquals("El rol no es válido", assertThrows(SolicitudInvalidaException.class,
                () -> servicio.crear("maria", "JEFE", "clave-segura", "administrador")).getMessage());
    }

    @Test
    void laClaveDebeTenerAlMenosOchoCaracteres() {
        assertEquals("La clave debe tener al menos 8 caracteres", assertThrows(SolicitudInvalidaException.class,
                () -> servicio.crear("maria", "DIRECTOR", "corta12", "administrador")).getMessage());
        assertEquals("La clave es obligatoria", assertThrows(SolicitudInvalidaException.class,
                () -> servicio.crear("maria", "DIRECTOR", null, "administrador")).getMessage());
    }

    @Test
    void laClaveNoPuedeSerIgualAlNombre() {
        assertThrows(SolicitudInvalidaException.class,
                () -> servicio.crear("mariaperez", "DIRECTOR", "MariaPerez", "administrador"));
    }

    @Test
    void laClaveNoPuedePasarDeLosSetentaYDosBytes() {
        assertThrows(SolicitudInvalidaException.class,
                () -> servicio.crear("maria", "DIRECTOR", "x".repeat(73), "administrador"));
    }

    @Test
    void noCreaUnNombreQueYaExiste() {
        when(usuarios.porNombre("maria")).thenReturn(Optional.of(new UsuarioBasico(1L, "maria", "DIRECTOR", true)));

        ConflictoException e = assertThrows(ConflictoException.class,
                () -> servicio.crear("Maria", "DIRECTOR", "clave-segura", "administrador"));

        assertEquals("Ya existe un usuario llamado maria", e.getMessage());
        verify(cuentas, never()).crear(anyString(), anyString(), anyString());
    }

    @Test
    void siLaBaseDetectaElDuplicadoDevuelveConflicto() {
        when(cuentas.crear(anyString(), anyString(), anyString())).thenThrow(new DuplicateKeyException("dup"));

        assertThrows(ConflictoException.class,
                () -> servicio.crear("maria", "DIRECTOR", "clave-segura", "administrador"));
    }

    @Test
    void listaLasCuentasSinNingunaClave() {
        when(cuentas.listar()).thenReturn(List.of(
                new Fila(1L, "director", "DIRECTOR", true, LocalDateTime.now()),
                new Fila(2L, "visitante", "VISITANTE", false, LocalDateTime.now())));

        List<UsuarioVista> lista = servicio.listar();

        assertEquals(2, lista.size());
        assertEquals("director", lista.get(0).nombreUsuario());
        assertFalse(lista.get(1).activo());
    }

    @Test
    void reactivaUnaCuentaRevocada() {
        when(usuarios.porId(5L)).thenReturn(Optional.of(new UsuarioBasico(5L, "investigador", "INVESTIGADOR", false)));

        servicio.reactivar(5L, "administrador");

        verify(cuentas).activar(5L);
    }

    @Test
    void noReactivaUnaCuentaQueYaTieneAcceso() {
        when(usuarios.porId(5L)).thenReturn(Optional.of(new UsuarioBasico(5L, "investigador", "INVESTIGADOR", true)));

        assertThrows(ConflictoException.class, () -> servicio.reactivar(5L, "administrador"));
        verify(cuentas, never()).activar(any());
    }

    @Test
    void reactivarUnaCuentaQueNoExisteDa404() {
        assertThrows(RecursoNoEncontradoException.class, () -> servicio.reactivar(99L, "administrador"));
    }
}
