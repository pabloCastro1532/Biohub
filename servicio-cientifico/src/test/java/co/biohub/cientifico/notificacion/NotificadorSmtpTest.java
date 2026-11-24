package co.biohub.cientifico.notificacion;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/** Adapter: el Notificador de BioHub se traduce a una llamada al cliente SMTP con el remitente configurado. */
class NotificadorSmtpTest {

    @Test
    void envia_por_el_cliente_smtp_con_el_remitente_configurado() {
        ClienteSmtp cliente = mock(ClienteSmtp.class);
        NotificadorSmtp notificador = new NotificadorSmtp(cliente, "alertas@biohub.local");

        notificador.enviar("Asunto", "Cuerpo", List.of("a@biohub.local", "b@biohub.local"));

        verify(cliente).enviar("alertas@biohub.local", List.of("a@biohub.local", "b@biohub.local"), "Asunto", "Cuerpo");
    }
}
