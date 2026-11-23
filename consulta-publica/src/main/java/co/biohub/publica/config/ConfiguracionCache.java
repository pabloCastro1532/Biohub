package co.biohub.publica.config;

import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.context.annotation.Configuration;

/** Activa la caché (Redis, TTL corto en application.yml) y hace que sus fallos no tumben las consultas. */
@Configuration
@EnableCaching
public class ConfiguracionCache implements CachingConfigurer {

    @Override
    public CacheErrorHandler errorHandler() {
        return new CacheTolerante();
    }
}
