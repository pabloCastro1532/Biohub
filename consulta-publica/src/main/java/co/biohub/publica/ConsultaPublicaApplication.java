package co.biohub.publica;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** Servicio de consulta pública: solo lectura, varias instancias, sin coordenadas exactas (ASR-1 y ASR-4). */
@SpringBootApplication
public class ConsultaPublicaApplication {

    public static void main(String[] args) {
        SpringApplication.run(ConsultaPublicaApplication.class, args);
    }
}
