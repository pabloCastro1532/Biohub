package co.biohub.publica.servicio;

import co.biohub.publica.repositorio.LecturaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** HU-18/19 y ASR-1: la BD de lectura se mantiene al día con los eventos, sin repetirlos y sin coordenadas. */
class ProyeccionHallazgosTest {

    private LecturaRepository lectura;
    private ProyeccionHallazgos proyeccion;

    @BeforeEach
    void preparar() {
        lectura = mock(LecturaRepository.class);
        proyeccion = new ProyeccionHallazgos(lectura);
        when(lectura.registrarEvento(anyString())).thenReturn(true);
        when(lectura.versionDe(anyLong())).thenReturn(Optional.empty());
    }

    private static Map<String, Object> evento(String id, int version) {
        Map<String, Object> m = new HashMap<>();
        m.put("id_evento", id);
        m.put("ocurrido_en", "2026-11-23T10:00:00Z");
        m.put("hallazgo_id", 7);
        m.put("especie_id", 1);
        m.put("version", version);
        m.put("region", "Meta");
        m.put("fecha", "2026-11-15");
        m.put("atributos_publicos", Map.of("sexo", "hembra"));
        return m;
    }

    @Test
    void un_hallazgo_nuevo_se_guarda_con_region_fecha_y_atributos() {
        Optional<Long> especie = proyeccion.aplicar(evento("e-1", 1));

        assertEquals(Optional.of(1L), especie);
        verify(lectura).asegurarEspecie(1L);
        ArgumentCaptor<HallazgoPublico> captor = ArgumentCaptor.forClass(HallazgoPublico.class);
        verify(lectura).insertarHallazgo(captor.capture());
        HallazgoPublico h = captor.getValue();
        assertEquals(7L, h.hallazgoId());
        assertEquals(1L, h.especieId());
        assertEquals(1, h.version());
        assertEquals("Meta", h.region());
        assertEquals(LocalDate.of(2026, 11, 15), h.fecha());
        assertEquals("hembra", h.atributos().get("sexo"));
    }

    @Test
    void un_evento_repetido_no_cambia_nada() {
        when(lectura.registrarEvento("e-1")).thenReturn(false);

        Optional<Long> especie = proyeccion.aplicar(evento("e-1", 1));

        assertTrue(especie.isEmpty());
        verify(lectura, never()).insertarHallazgo(any(HallazgoPublico.class));
        verify(lectura, never()).actualizarHallazgo(any(HallazgoPublico.class));
    }

    @Test
    void una_version_mas_nueva_actualiza_el_hallazgo() {
        when(lectura.versionDe(7L)).thenReturn(Optional.of(1));

        Optional<Long> especie = proyeccion.aplicar(evento("e-2", 2));

        assertEquals(Optional.of(1L), especie);
        verify(lectura).actualizarHallazgo(any(HallazgoPublico.class));
        verify(lectura, never()).insertarHallazgo(any(HallazgoPublico.class));
    }

    @Test
    void una_version_igual_o_mas_vieja_se_ignora() {
        when(lectura.versionDe(7L)).thenReturn(Optional.of(3));

        assertTrue(proyeccion.aplicar(evento("e-3", 2)).isEmpty());
        assertTrue(proyeccion.aplicar(evento("e-4", 3)).isEmpty());

        verify(lectura, never()).actualizarHallazgo(any(HallazgoPublico.class));
        verify(lectura, never()).insertarHallazgo(any(HallazgoPublico.class));
    }

    @Test
    void un_evento_incompleto_se_descarta_sin_marcarlo_como_procesado() {
        Map<String, Object> sinRegion = evento("e-5", 1);
        sinRegion.remove("region");
        Map<String, Object> sinHallazgo = evento("e-6", 1);
        sinHallazgo.put("hallazgo_id", null);
        Map<String, Object> versionCero = evento("e-7", 0);

        assertTrue(proyeccion.aplicar(sinRegion).isEmpty());
        assertTrue(proyeccion.aplicar(sinHallazgo).isEmpty());
        assertTrue(proyeccion.aplicar(versionCero).isEmpty());

        verify(lectura, never()).registrarEvento(anyString());
        verify(lectura, never()).insertarHallazgo(any(HallazgoPublico.class));
    }

    @Test
    void una_fecha_invalida_o_de_otro_tipo_se_descarta() {
        Map<String, Object> mala = evento("e-8", 1);
        mala.put("fecha", "15/11/2026");
        Map<String, Object> numerica = evento("e-9", 1);
        numerica.put("fecha", 20261115);

        assertTrue(proyeccion.aplicar(mala).isEmpty());
        assertTrue(proyeccion.aplicar(numerica).isEmpty());
        verify(lectura, never()).insertarHallazgo(any(HallazgoPublico.class));
    }

    @Test
    void sin_atributos_publicos_se_guarda_un_mapa_vacio() {
        Map<String, Object> m = evento("e-10", 1);
        m.remove("atributos_publicos");

        proyeccion.aplicar(m);

        ArgumentCaptor<HallazgoPublico> captor = ArgumentCaptor.forClass(HallazgoPublico.class);
        verify(lectura).insertarHallazgo(captor.capture());
        assertTrue(captor.getValue().atributos().isEmpty());
    }

    @Test
    void asr4_lo_que_se_guarda_no_tiene_ningun_campo_de_ubicacion_exacta() {
        // Aunque alguien enviara latitud y longitud por error, la proyección no tiene dónde guardarlas.
        Map<String, Object> m = evento("e-11", 1);
        m.put("latitud", 4.71);
        m.put("longitud", -74.07);

        proyeccion.aplicar(m);

        ArgumentCaptor<HallazgoPublico> captor = ArgumentCaptor.forClass(HallazgoPublico.class);
        verify(lectura).insertarHallazgo(captor.capture());
        for (var campo : HallazgoPublico.class.getRecordComponents()) {
            String nombre = campo.getName().toLowerCase();
            assertFalse(nombre.contains("lat") || nombre.contains("lon"), "campo de ubicación: " + nombre);
        }
        assertFalse(captor.getValue().atributos().containsKey("latitud"));
    }
}
