package co.biohub.cientifico.servicio;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.util.List;

/**
 * Umbrales del monitor de seguridad (HU-26). Configurables en application.yml (biohub.seguridad.*) o con variables
 * de entorno; no están fijos en el código porque el Instituto puede ajustarlos.
 *
 * @param maximoConsultasPorHora   se alerta cuando un usuario SUPERA este número de consultas en la ventana (50: la 51 dispara)
 * @param ventanaConsultasMinutos  tamaño de la ventana de consultas
 * @param rechazosParaAlerta       se alerta cuando un usuario LLEGA a este número de rechazos en la ventana (5: el quinto dispara)
 * @param ventanaRechazosMinutos   tamaño de la ventana de rechazos
 * @param destinatarios            correos que reciben el aviso
 */
@ConfigurationProperties(prefix = "biohub.seguridad")
public record UmbralesSeguridad(
        @DefaultValue("50") int maximoConsultasPorHora,
        @DefaultValue("60") int ventanaConsultasMinutos,
        @DefaultValue("5") int rechazosParaAlerta,
        @DefaultValue("10") int ventanaRechazosMinutos,
        @DefaultValue("seguridad@biohub.local") List<String> destinatarios) {}
