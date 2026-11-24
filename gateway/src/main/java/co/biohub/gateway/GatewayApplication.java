package co.biohub.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * API Gateway minimo de BioHub (ASR-5, alcance de esta entrega): unico punto de entrada que enruta.
 * La API key, el limite de 60 por minuto y el contrato v1 (HU-20 y HU-21) quedan como diseno futuro.
 * Todo el comportamiento esta en application.yml: no hay logica de negocio aqui.
 */
@SpringBootApplication
public class GatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(GatewayApplication.class, args);
    }
}
