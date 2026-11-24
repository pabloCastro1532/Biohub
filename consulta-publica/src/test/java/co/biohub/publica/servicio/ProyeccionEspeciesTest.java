package co.biohub.publica.servicio;

import co.biohub.publica.repositorio.LecturaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/** Una especie nueva llega por evento y queda con su nombre real en la BD de lectura. */
class ProyeccionEspeciesTest {

    private LecturaRepository lectura;
    private ProyeccionEspecies proyeccion;

    @BeforeEach
    void preparar() {
        lectura = mock(LecturaRepository.class);
        proyeccion = new ProyeccionEspecies(lectura);
    }

    private static Map<String, Object> evento(Object id, Object cientifico, Object comun) {
        Map<String, Object> m = new HashMap<>();
        m.put("id_evento", "e-1");
        m.put("especie_id", id);
        m.put("nombre_cientifico", cientifico);
        m.put("nombre_comun", comun);
        return m;
    }

    @Test
    void guardaLaEspecieYDevuelveSuIdParaInvalidarLaCache() {
        Optional<Long> especie = proyeccion.aplicar(evento(7, " Tapirus terrestris ", "Danta"));

        assertEquals(Optional.of(7L), especie);
        verify(lectura).guardarEspecie(7L, "Tapirus terrestris", "Danta");
    }

    @Test
    void elNombreComunVacioSeGuardaComoNulo() {
        proyeccion.aplicar(evento(7, "Tapirus terrestris", "  "));

        verify(lectura).guardarEspecie(7L, "Tapirus terrestris", null);
    }

    @Test
    void descartaUnMensajeSinIdOSinNombreCientifico() {
        assertTrue(proyeccion.aplicar(evento(null, "Tapirus terrestris", "Danta")).isEmpty());
        assertTrue(proyeccion.aplicar(evento(7, " ", "Danta")).isEmpty());
        assertTrue(proyeccion.aplicar(evento(7, null, "Danta")).isEmpty());
        verify(lectura, never()).guardarEspecie(anyLong(), anyString(), any());
    }
}
