package co.biohub.cientifico.servicio;

import co.biohub.cientifico.dominio.Equipo;
import co.biohub.cientifico.repositorio.EquipoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DataIntegrityViolationException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class EquipoServiceTest {

    private EquipoRepository repositorio;
    private EquipoService servicio;

    @BeforeEach
    void preparar() {
        repositorio = mock(EquipoRepository.class);
        servicio = new EquipoService(repositorio);
        when(repositorio.saveAndFlush(any(Equipo.class))).thenAnswer(i -> i.getArgument(0));
    }

    @Test
    void registraConElCodigoEnMayusculasYSinEspacios() {
        Equipo e = servicio.registrar("  gps-001 ", " GPS Garmin ", " Navegación ", "  ");

        ArgumentCaptor<Equipo> guardado = ArgumentCaptor.forClass(Equipo.class);
        verify(repositorio).saveAndFlush(guardado.capture());
        assertEquals("GPS-001", guardado.getValue().getCodigo());
        assertEquals("GPS Garmin", e.getNombre());
        assertEquals("Navegación", e.getTipo());
        assertNull(e.getDescripcion());
        assertTrue(e.isActivo());
    }

    @Test
    void rechazaUnCodigoRepetidoSinImportarLasMayusculas() {
        when(repositorio.existsByCodigo("GPS-001")).thenReturn(true);

        assertThrows(ConflictoException.class, () -> servicio.registrar("gps-001", "GPS", "Navegación", null));
        verify(repositorio, never()).saveAndFlush(any());
    }

    @Test
    void laRestriccionUniqueDeLaBaseTambienSeTraduceAConflicto() {
        when(repositorio.saveAndFlush(any(Equipo.class))).thenThrow(new DataIntegrityViolationException("duplicado"));

        assertThrows(ConflictoException.class, () -> servicio.registrar("GPS-002", "GPS", "Navegación", null));
    }

    @Test
    void exigeCodigoNombreYTipo() {
        assertThrows(SolicitudInvalidaException.class, () -> servicio.registrar(" ", "GPS", "Navegación", null));
        assertThrows(SolicitudInvalidaException.class, () -> servicio.registrar("GPS-003", null, "Navegación", null));
        assertThrows(SolicitudInvalidaException.class, () -> servicio.registrar("GPS-003", "GPS", "", null));
        verify(repositorio, never()).saveAndFlush(any());
    }

    @Test
    void consultarUnEquipoQueNoExisteDa404() {
        when(repositorio.findById(99L)).thenReturn(java.util.Optional.empty());

        assertThrows(RecursoNoEncontradoException.class, () -> servicio.obtener(99L));
    }
}
