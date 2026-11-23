package co.biohub.publica.mensajeria;

import co.biohub.publica.servicio.InvalidadorCache;
import co.biohub.publica.servicio.ProyeccionHallazgos;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** El evento actualiza la BD de lectura y solo entonces se invalida la caché. */
class ConsumidorHallazgosTest {

    private ProyeccionHallazgos proyeccion;
    private InvalidadorCache cache;
    private ConsumidorHallazgos consumidor;
    private final Map<String, Object> mensaje = Map.of("id_evento", "e-1");

    @BeforeEach
    void preparar() {
        proyeccion = mock(ProyeccionHallazgos.class);
        cache = mock(InvalidadorCache.class);
        consumidor = new ConsumidorHallazgos(proyeccion, cache);
    }

    @Test
    void cuando_el_evento_cambio_algo_se_invalida_la_cache_de_esa_especie() {
        when(proyeccion.aplicar(mensaje)).thenReturn(Optional.of(1L));

        consumidor.alRecibir(mensaje);

        verify(cache).invalidar(1L);
    }

    @Test
    void cuando_el_evento_no_cambio_nada_no_se_toca_la_cache() {
        when(proyeccion.aplicar(mensaje)).thenReturn(Optional.empty());

        consumidor.alRecibir(mensaje);

        verify(cache, never()).invalidar(anyLong());
    }

    @Test
    void si_la_bd_falla_la_excepcion_sale_para_que_rabbitmq_reintente_y_no_se_invalida_nada() {
        doThrow(new IllegalStateException("BD caída")).when(proyeccion).aplicar(mensaje);

        assertThrows(IllegalStateException.class, () -> consumidor.alRecibir(mensaje));
        verify(cache, never()).invalidar(anyLong());
    }
}
