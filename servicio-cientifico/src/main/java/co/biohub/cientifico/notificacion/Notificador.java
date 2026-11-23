package co.biohub.cientifico.notificacion;

import java.util.List;

/**
 * Interfaz propia de BioHub para avisar por correo. El resto del sistema solo conoce esta interfaz, así cambiar de
 * proveedor de correo toca una sola clase (patrón Adapter, ver NotificadorSmtp).
 */
public interface Notificador {

    void enviar(String asunto, String cuerpo, List<String> destinatarios);
}
