package co.biohub.cientifico.servicio;

import co.biohub.cientifico.dominio.Equipo;
import co.biohub.cientifico.dominio.Prestamo;
import co.biohub.cientifico.repositorio.EquipoRepository;
import co.biohub.cientifico.repositorio.PrestamoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** HU-24: el historial de préstamos de un equipo. */
class HistorialPrestamosServiceTest {

    private PrestamoRepository prestamos;
    private EquipoRepository equipos;
    private HistorialPrestamosService servicio;

    @BeforeEach
    void preparar() {
        prestamos = mock(PrestamoRepository.class);
        equipos = mock(EquipoRepository.class);
        servicio = new HistorialPrestamosService(prestamos, equipos);

        Equipo gps = Equipo.nuevo("GPS-001", "GPS Garmin", "Navegacion", null);
        ReflectionTestUtils.setField(gps, "id", 1L);
        when(equipos.findById(1L)).thenReturn(Optional.of(gps));
    }

    private Prestamo prestamo(long id, String grupo, boolean devuelto) {
        Prestamo p = Prestamo.nuevo(1L, grupo, "para el muestreo", "director");
        ReflectionTestUtils.setField(p, "id", id);
        if (devuelto) {
            ReflectionTestUtils.setField(p, "devueltoEn", LocalDateTime.now());
            ReflectionTestUtils.setField(p, "devueltoPor", "administrador");
        }
        return p;
    }

    @Test
    void hu24_lista_los_prestamos_del_equipo_con_su_codigo_en_el_orden_del_repositorio() {
        when(prestamos.findByEquipoIdOrderByPrestadoEnDesc(1L))
                .thenReturn(List.of(prestamo(2L, "Grupo Aves", false), prestamo(1L, "Grupo Felinos", true)));

        List<PrestamoVista> historial = servicio.deEquipo(1L);

        assertEquals(2, historial.size());
        assertEquals(2L, historial.get(0).id());
        assertEquals("Grupo Aves", historial.get(0).grupo());
        assertEquals("GPS-001", historial.get(0).equipoCodigo());
        assertEquals("Grupo Felinos", historial.get(1).grupo());
    }

    @Test
    void hu24_distingue_los_prestamos_devueltos_de_los_abiertos() {
        when(prestamos.findByEquipoIdOrderByPrestadoEnDesc(1L))
                .thenReturn(List.of(prestamo(2L, "Grupo Aves", false), prestamo(1L, "Grupo Felinos", true)));

        List<PrestamoVista> historial = servicio.deEquipo(1L);

        assertNull(historial.get(0).devueltoEn());
        assertNotNull(historial.get(1).devueltoEn());
        assertEquals("administrador", historial.get(1).devueltoPor());
    }

    @Test
    void hu24_un_equipo_que_nunca_se_presto_da_una_lista_vacia() {
        when(prestamos.findByEquipoIdOrderByPrestadoEnDesc(1L)).thenReturn(List.of());

        assertTrue(servicio.deEquipo(1L).isEmpty());
    }

    @Test
    void hu24_un_equipo_que_no_existe_da_404() {
        when(equipos.findById(99L)).thenReturn(Optional.empty());

        assertThrows(RecursoNoEncontradoException.class, () -> servicio.deEquipo(99L));
        verify(prestamos, never()).findByEquipoIdOrderByPrestadoEnDesc(any());
    }

    @Test
    void hu24_consultar_el_historial_no_modifica_nada() {
        when(prestamos.findByEquipoIdOrderByPrestadoEnDesc(1L)).thenReturn(List.of(prestamo(1L, "Grupo Aves", false)));

        servicio.deEquipo(1L);

        verify(prestamos, never()).save(any());
        verify(prestamos, never()).saveAndFlush(any());
    }
}
