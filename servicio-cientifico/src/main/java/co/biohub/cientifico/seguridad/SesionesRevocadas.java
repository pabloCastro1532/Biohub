package co.biohub.cientifico.seguridad;

import co.biohub.cientifico.repositorio.AccionSeguridadJdbc;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * HU-27 (ASR-4): una sesión está revocada si el token se emitió antes de la última revocación del usuario.
 * Se consulta en cada petición, así el usuario revocado recibe 401 en la siguiente.
 */
@Component
public class SesionesRevocadas {

    private final AccionSeguridadJdbc acciones;

    public SesionesRevocadas(AccionSeguridadJdbc acciones) {
        this.acciones = acciones;
    }

    public boolean estaRevocada(String usuario, Instant emitidoEn) {
        return acciones.ultimaRevocacion(usuario)
                .map(revocadaEn -> LocalDateTime.ofInstant(emitidoEn, ZoneId.systemDefault()).isBefore(revocadaEn))
                .orElse(false);
    }
}