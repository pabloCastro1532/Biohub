package co.biohub.cientifico.servicio;

import co.biohub.cientifico.repositorio.CredencialesJdbc;
import co.biohub.cientifico.repositorio.CredencialesJdbc.Credencial;
import co.biohub.cientifico.seguridad.JwtService;
import co.biohub.cientifico.seguridad.Rol;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** HU-22 y HU-23: el login valida contra la tabla de usuarios y el token lleva el rol. */
class AutenticacionServiceTest {

    private static final String SECRETO = "un-secreto-de-prueba-de-al-menos-32-caracteres";

    private CredencialesJdbc credenciales;
    private PasswordEncoder encoder;
    private JwtService jwt;
    private AutenticacionService servicio;

    @BeforeEach
    void preparar() {
        credenciales = mock(CredencialesJdbc.class);
        encoder = new BCryptPasswordEncoder(4);
        jwt = new JwtService(SECRETO, 5);
        servicio = new AutenticacionService(credenciales, encoder, jwt);
    }

    private void existeUsuario(String escrito, String guardado, String clave, Rol rol, boolean activo) {
        when(credenciales.porNombre(escrito)).thenReturn(Optional.of(
                new Credencial(1L, guardado, encoder.encode(clave), rol.name(), activo)));
    }

    @Test
    void investigadorConCredencialesValidasObtieneSesionConSuRol() {
        existeUsuario("investigador", "investigador", "biohub", Rol.INVESTIGADOR, true);

        var sesion = servicio.iniciarSesion("investigador", "biohub");

        assertTrue(sesion.isPresent());
        assertEquals(Rol.INVESTIGADOR, sesion.get().rol());
        Claims claims = jwt.validar(sesion.get().token());
        assertEquals("investigador", claims.getSubject());
        assertEquals("INVESTIGADOR", claims.get("rol", String.class));
    }

    @Test
    void directorConCredencialesValidasObtieneSesionConRolDirector() {
        existeUsuario("director", "director", "biohub", Rol.DIRECTOR, true);

        var sesion = servicio.iniciarSesion("director", "biohub");

        assertTrue(sesion.isPresent());
        assertEquals("DIRECTOR", jwt.validar(sesion.get().token()).get("rol", String.class));
    }

    @Test
    void claveIncorrectaNoDaSesion() {
        existeUsuario("director", "director", "biohub", Rol.DIRECTOR, true);

        assertTrue(servicio.iniciarSesion("director", "mala").isEmpty());
    }

    @Test
    void usuarioInexistenteNoDaSesion() {
        when(credenciales.porNombre("nadie")).thenReturn(Optional.empty());

        assertTrue(servicio.iniciarSesion("nadie", "biohub").isEmpty());
    }

    @Test
    void usuarioInactivoNoDaSesionAunqueLaClaveSeaCorrecta() {
        existeUsuario("director", "director", "biohub", Rol.DIRECTOR, false);

        assertTrue(servicio.iniciarSesion("director", "biohub").isEmpty());
    }

    @Test
    void elTokenLlevaElNombreGuardadoNoElEscrito() {
        existeUsuario("Director", "director", "biohub", Rol.DIRECTOR, true);

        var sesion = servicio.iniciarSesion("Director", "biohub");

        assertEquals("director", jwt.validar(sesion.get().token()).getSubject());
    }
}