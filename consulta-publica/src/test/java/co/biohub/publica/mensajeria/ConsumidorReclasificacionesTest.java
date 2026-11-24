package co.biohub.publica.mensajeria;

import co.biohub.publica.servicio.InvalidadorCache;
import co.biohub.publica.servicio.ProyeccionHallazgos;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** HU-09: la reclasificación se guarda primero y solo entonces se limpia la caché de las dos especies. */
class ConsumidorReclasificacionesTest {

    private ProyeccionHallazgos proyeccion;
    private InvalidadorCache cache;
    private ConsumidorReclasificaciones consumidor;
    private final Map<String, Object> mensaje = Map.of("id_evento", "r-1");

    @BeforeEach
    void preparar() {
        proyeccion = mock(ProyeccionHallazgos.class);
        cache = mock(InvalidadorCache.class);
        consumidor = new ConsumidorReclasificaciones(proyeccion, cache);
    }

    @Test
    void se_invalida_la_cache_de_la_especie_anterior_y_de_la_nueva() {
        when(proyeccion.reclasificar(mensaje)).thenReturn(List.of(1L, 2L));

        consumidor.alRecibir(mensaje);

        verify(cache).invalidar(1L);
        verify(cache).invalidar(2L);
    }

    @Test
    void si_el_evento_no_cambio_nada_no_se_toca_la_cache() {
        when(proyeccion.reclasificar(mensaje)).thenReturn(List.of());

        consumidor.alRecibir(mensaje);

        verify(cache, never()).invalidar(anyLong());
    }

    @Test
    void si_la_bd_falla_la_excepcion_sale_para_que_rabbitmq_reintente_y_no_se_invalida_nada() {
        doThrow(new IllegalStateException("BD caída")).when(proyeccion).reclasificar(mensaje);

        assertThrows(IllegalStateException.class, () -> consumidor.alRecibir(mensaje));
        verify(cache, never()).invalidar(anyLong());
    }
}
