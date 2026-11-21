package co.biohub.cientifico.seguridad;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JwtServiceTest {

    private static final String SECRETO = "un-secreto-de-prueba-de-al-menos-32-caracteres";

    @Test
    void emiteYValidaTokenConRol() {
        JwtService jwt = new JwtService(SECRETO, 5);
        Claims claims = jwt.validar(jwt.emitir("director", Rol.DIRECTOR));

        assertEquals("director", claims.getSubject());
        assertEquals("DIRECTOR", claims.get("rol", String.class));
        assertNotNull(claims.getId());
    }

    @Test
    void rechazaTokenFirmadoConOtroSecreto() {
        String token = new JwtService(SECRETO, 5).emitir("director", Rol.DIRECTOR);
        JwtService otro = new JwtService("otro-secreto-distinto-de-al-menos-32-caracteres", 5);

        assertThrows(JwtException.class, () -> otro.validar(token));
    }

    @Test
    void rechazaTokenAlterado() {
        JwtService jwt = new JwtService(SECRETO, 5);
        String token = jwt.emitir("visitante", Rol.VISITANTE);

        assertThrows(JwtException.class, () -> jwt.validar(token + "x"));
    }
}
