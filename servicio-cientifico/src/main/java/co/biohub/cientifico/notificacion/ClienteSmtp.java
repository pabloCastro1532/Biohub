package co.biohub.cientifico.notificacion;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Properties;

/**
 * Cliente SMTP "del proveedor": hoy apunta a Mailpit (servidor de pruebas, nada sale a internet).
 * Host y puerto: BIOHUB_SMTP_HOST y BIOHUB_SMTP_PORT (por defecto localhost:1025).
 */
@Component
public class ClienteSmtp {

    private final JavaMailSenderImpl remitente = new JavaMailSenderImpl();

    public ClienteSmtp(@Value("${biohub.smtp.host:localhost}") String host,
                       @Value("${biohub.smtp.port:1025}") int puerto) {
        remitente.setHost(host);
        remitente.setPort(puerto);
        Properties p = remitente.getJavaMailProperties();
        // Sin esperas largas: si el servidor de correo no responde, falla rápido y el monitor sigue.
        p.put("mail.smtp.connectiontimeout", "3000");
        p.put("mail.smtp.timeout", "5000");
        p.put("mail.smtp.writetimeout", "5000");
    }

    public void enviar(String desde, List<String> para, String asunto, String texto) {
        SimpleMailMessage mensaje = new SimpleMailMessage();
        mensaje.setFrom(desde);
        mensaje.setTo(para.toArray(new String[0]));
        mensaje.setSubject(asunto);
        mensaje.setText(texto);
        remitente.send(mensaje);
    }
}
