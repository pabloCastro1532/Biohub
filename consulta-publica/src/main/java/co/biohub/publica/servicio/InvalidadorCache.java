package co.biohub.publica.servicio;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;

/**
 * Invalida la caché cuando un evento cambia una especie: su ficha, y todas las búsquedas (pueden incluirla).
 * Si Redis no responde no pasa nada grave: los datos viejos desaparecen solos cuando vence el TTL.
 */
@Component
public class InvalidadorCache {

    private static final Logger LOG = LoggerFactory.getLogger(InvalidadorCache.class);

    private final CacheManager caches;

    public InvalidadorCache(CacheManager caches) {
        this.caches = caches;
    }

    public void invalidar(long especieId) {
        try {
            Cache fichas = caches.getCache("fichas");
            if (fichas != null) {
                fichas.evict(especieId);
            }
            Cache busquedas = caches.getCache("busquedas");
            if (busquedas != null) {
                busquedas.clear();
            }
        } catch (RuntimeException e) {
            LOG.warn("No se pudo invalidar la caché de la especie {}: {}", especieId, e.getMessage());
        }
    }
}
