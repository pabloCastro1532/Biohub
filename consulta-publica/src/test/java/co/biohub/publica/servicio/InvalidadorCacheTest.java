package co.biohub.publica.servicio;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** ASR-1: un evento invalida la ficha de su especie y todas las búsquedas; si Redis falla no se rompe nada. */
class InvalidadorCacheTest {

    private CacheManager caches;
    private Cache fichas;
    private Cache busquedas;
    private InvalidadorCache invalidador;

    @BeforeEach
    void preparar() {
        caches = mock(CacheManager.class);
        fichas = mock(Cache.class);
        busquedas = mock(Cache.class);
        when(caches.getCache("fichas")).thenReturn(fichas);
        when(caches.getCache("busquedas")).thenReturn(busquedas);
        invalidador = new InvalidadorCache(caches);
    }

    @Test
    void borra_la_ficha_de_la_especie_y_vacia_las_busquedas() {
        invalidador.invalidar(1L);

        verify(fichas).evict(1L);
        verify(busquedas).clear();
    }

    @Test
    void si_redis_falla_no_lanza_excepcion() {
        doThrow(new IllegalStateException("Redis caído")).when(fichas).evict(1L);

        assertDoesNotThrow(() -> invalidador.invalidar(1L));
    }

    @Test
    void si_no_existe_alguna_cache_no_lanza_excepcion() {
        when(caches.getCache("fichas")).thenReturn(null);

        assertDoesNotThrow(() -> invalidador.invalidar(1L));
        verify(busquedas).clear();
    }
}
