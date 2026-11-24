package co.biohub.publica.config;

import org.junit.jupiter.api.Test;
import org.springframework.cache.Cache;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** ASR-1, degradación elegante: los errores de Redis no llegan al usuario. */
class CacheToleranteTest {

    @Test
    void ningun_error_de_la_cache_se_propaga() {
        Cache cache = mock(Cache.class);
        when(cache.getName()).thenReturn("fichas");
        CacheTolerante manejador = new CacheTolerante();
        RuntimeException error = new IllegalStateException("Redis caído");

        assertDoesNotThrow(() -> manejador.handleCacheGetError(error, cache, "k"));
        assertDoesNotThrow(() -> manejador.handleCachePutError(error, cache, "k", "v"));
        assertDoesNotThrow(() -> manejador.handleCacheEvictError(error, cache, "k"));
        assertDoesNotThrow(() -> manejador.handleCacheClearError(error, cache));
    }
}
