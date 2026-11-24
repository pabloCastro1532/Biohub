package co.biohub.cientifico.notificacion;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;

/** Adapter: traduce la interfaz Notificador de BioHub a las llamadas de ClienteSmtp. */
@Component
public class NotificadorSmtp implements Notificador {

    private final ClienteSmtp cliente;
    private final String remitente;

    public NotificadorSmtp(ClienteSmtp cliente,
                           @Value("${biohub.smtp.remitente:alertas@biohub.local}") String remitente) {
        this.cliente = cliente;
        this.remitente = remitente;
    }

    @Override
    public void enviar(String asunto, String cuerpo, List<String> destinatarios) {
        cliente.enviar(remitente, destinatarios, asunto, cuerpo);
    }
}
