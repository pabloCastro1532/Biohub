package co.biohub.cientifico.servicio;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/** Activa la lectura de los umbrales del monitor de seguridad. */
@Configuration
@EnableConfigurationProperties(UmbralesSeguridad.class)
public class ConfiguracionMonitor {
}
