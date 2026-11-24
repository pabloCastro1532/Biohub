package co.biohub.publica.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.Cache;
import org.springframework.cache.interceptor.CacheErrorHandler;

/**
 * Degradación elegante (ASR-1): si Redis falla, la petición se responde desde la BD de lectura en vez de dar error.
 * Solo se anota el problema.
 */
public class CacheTolerante implements CacheErrorHandler {

    private static final Logger LOG = LoggerFactory.getLogger(CacheTolerante.class);

    @Override
    public void handleCacheGetError(RuntimeException e, Cache cache, Object key) {
        LOG.warn("Caché '{}' no disponible al leer: {}", cache.getName(), e.getMessage());
    }

    @Override
    public void handleCachePutError(RuntimeException e, Cache cache, Object key, Object value) {
        LOG.warn("Caché '{}' no disponible al guardar: {}", cache.getName(), e.getMessage());
    }

    @Override
    public void handleCacheEvictError(RuntimeException e, Cache cache, Object key) {
        LOG.warn("Caché '{}' no disponible al borrar: {}", cache.getName(), e.getMessage());
    }

    @Override
    public void handleCacheClearError(RuntimeException e, Cache cache) {
        LOG.warn("Caché '{}' no disponible al vaciar: {}", cache.getName(), e.getMessage());
    }
}
