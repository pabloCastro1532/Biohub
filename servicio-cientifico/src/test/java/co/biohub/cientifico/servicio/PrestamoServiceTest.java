package co.biohub.cientifico.servicio;

import co.biohub.cientifico.dominio.Equipo;
import co.biohub.cientifico.dominio.Prestamo;
import co.biohub.cientifico.repositorio.EquipoRepository;
import co.biohub.cientifico.repositorio.PrestamoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PrestamoServiceTest {

    private PrestamoRepository prestamos;
    private EquipoRepository equipos;
    private PrestamoService servicio;
    private Equipo gps;

    @BeforeEach
    void preparar() {
        prestamos = mock(PrestamoRepository.class);
        equipos = mock(EquipoRepository.class);
        servicio = new PrestamoService(prestamos, equipos);

        gps = Equipo.nuevo("GPS-001", "GPS Garmin", "Navegacion", null);
        ReflectionTestUtils.setField(gps, "id", 1L);
        when(equipos.findById(1L)).thenReturn(Optional.of(gps));
        when(prestamos.saveAndFlush(any(Prestamo.class))).thenAnswer(i -> i.getArgument(0));
    }

    @Test
    void presta_un_equipo_libre_a_un_grupo() {
        PrestamoVista v = servicio.prestar(1L, "  Grupo Aves ", " para el muestreo ", "director");

        assertEquals("GPS-001", v.equipoCodigo());
        assertEquals("Grupo Aves", v.grupo());
        assertEquals("para el muestreo", v.observacion());
        assertEquals("director", v.prestadoPor());
        assertNotNull(v.prestadoEn());
        assertNull(v.devueltoEn());
    }

    @Test
    void hu05_no_presta_un_equipo_que_ya_esta_prestado() {
        Prestamo abierto = Prestamo.nuevo(1L, "Grupo Mamiferos", null, "director");
        when(prestamos.findByEquipoIdAndDevueltoEnIsNull(1L)).thenReturn(Optional.of(abierto));

        ConflictoException e = assertThrows(ConflictoException.class,
                () -> servicio.prestar(1L, "Grupo Aves", null, "director"));

        assertEquals("El equipo GPS-001 ya está prestado al grupo Grupo Mamiferos", e.getMessage());
        verify(prestamos, never()).saveAndFlush(any());
    }

    @Test
    void hu05_dos_prestamos_simultaneos_los_detiene_la_base_de_datos() {
        when(prestamos.saveAndFlush(any(Prestamo.class))).thenThrow(new DataIntegrityViolationException("uq_prestamo_abierto"));

        assertThrows(ConflictoException.class, () -> servicio.prestar(1L, "Grupo Aves", null, "director"));
    }

    @Test
    void no_presta_un_equipo_que_no_existe() {
        when(equipos.findById(99L)).thenReturn(Optional.empty());

        assertThrows(RecursoNoEncontradoException.class, () -> servicio.prestar(99L, "Grupo Aves", null, "director"));
    }

    @Test
    void no_presta_un_equipo_inactivo() {
        ReflectionTestUtils.setField(gps, "activo", false);

        assertThrows(SolicitudInvalidaException.class, () -> servicio.prestar(1L, "Grupo Aves", null, "director"));
        verify(prestamos, never()).saveAndFlush(any());
    }

    @Test
    void exige_el_grupo() {
        assertThrows(SolicitudInvalidaException.class, () -> servicio.prestar(1L, "  ", null, "director"));
        assertThrows(SolicitudInvalidaException.class, () -> servicio.prestar(1L, null, null, "director"));
    }

    @Test
    void devuelve_un_prestamo_abierto() {
        Prestamo p = Prestamo.nuevo(1L, "Grupo Aves", null, "director");
        ReflectionTestUtils.setField(p, "id", 7L);
        when(prestamos.findById(7L)).thenReturn(Optional.of(p));
        when(prestamos.cerrar(eq(7L), any(LocalDateTime.class), eq("director"))).thenReturn(1);

        PrestamoVista v = servicio.devolver(7L, "director");

        assertEquals("GPS-001", v.equipoCodigo());
        verify(prestamos).cerrar(eq(7L), any(LocalDateTime.class), eq("director"));
    }

    @Test
    void no_devuelve_dos_veces_el_mismo_prestamo() {
        Prestamo p = Prestamo.nuevo(1L, "Grupo Aves", null, "director");
        when(prestamos.findById(7L)).thenReturn(Optional.of(p));
        when(prestamos.cerrar(anyLong(), any(LocalDateTime.class), anyString())).thenReturn(0);

        assertThrows(ConflictoException.class, () -> servicio.devolver(7L, "director"));
    }
}
