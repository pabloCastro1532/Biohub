package co.biohub.cientifico.seguridad;

import org.springframework.stereotype.Component;

/**
 * Punto de enganche de HU-27 (revocación de sesión, ASR-4: usuario revocado recibe 401).
 * Hoy nunca revoca; Yonnatan lo reemplaza con la lista real (tabla o Redis) sin tocar el filtro.
 */
@Component
public class SesionesRevocadas {

    public boolean estaRevocada(String jti, String usuario) {
        return false;
    }
}
