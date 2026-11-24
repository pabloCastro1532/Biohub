package co.biohub.cientifico.servicio;

import co.biohub.cientifico.dominio.Especie;
import co.biohub.cientifico.repositorio.EspecieRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class EspecieServiceTest {

    private EspecieRepository especies;
    private PlantillaService plantillas;
    private ApplicationEventPublisher eventos;
    private EspecieService servicio;

    @BeforeEach
    void preparar() {
        especies = mock(EspecieRepository.class);
        plantillas = mock(PlantillaService.class);
        eventos = mock(ApplicationEventPublisher.class);
        servicio = new EspecieService(especies, plantillas, eventos);
        when(especies.saveAndFlush(any(Especie.class))).thenAnswer(i -> i.getArgument(0));
    }

    @Test
    void guardaLaEspecieConLosNombresSinEspacios() {
        Especie e = servicio.crear("  Tapirus terrestris ", " Danta ");

        assertEquals("Tapirus terrestris", e.getNombreCientifico());
        assertEquals("Danta", e.getNombreComun());
    }

    @Test
    void elNombreComunVacioSeGuardaComoNulo() {
        Especie e = servicio.crear("Tapirus terrestris", "   ");

        assertNull(e.getNombreComun());
    }

    @Test
    void creaLaPlantillaInicialYPublicaElEvento() {
        servicio.crear("Tapirus terrestris", "Danta");

        verify(plantillas).crear(any(), any());
        ArgumentCaptor<EspecieRegistradaEvento> evento = ArgumentCaptor.forClass(EspecieRegistradaEvento.class);
        verify(eventos).publishEvent(evento.capture());
        assertEquals("Tapirus terrestris", evento.getValue().nombreCientifico());
        assertEquals("Danta", evento.getValue().nombreComun());
    }

    @Test
    void rechazaUnNombreCientificoVacioOMuyCorto() {
        assertThrows(SolicitudInvalidaException.class, () -> servicio.crear(null, "Danta"));
        assertThrows(SolicitudInvalidaException.class, () -> servicio.crear("   ", "Danta"));
        assertThrows(SolicitudInvalidaException.class, () -> servicio.crear("ab", "Danta"));
        verify(especies, never()).saveAndFlush(any());
    }

    @Test
    void rechazaNombresDemasiadoLargos() {
        String largo = "a".repeat(EspecieService.LARGO_MAXIMO + 1);

        assertThrows(SolicitudInvalidaException.class, () -> servicio.crear(largo, null));
        assertThrows(SolicitudInvalidaException.class, () -> servicio.crear("Tapirus terrestris", largo));
    }

    @Test
    void rechazaUnaEspecieRepetidaSinImportarLasMayusculas() {
        when(especies.existsByNombreCientificoIgnoreCase("tapirus terrestris")).thenReturn(true);

        assertThrows(ConflictoException.class, () -> servicio.crear("tapirus terrestris", null));
        verify(especies, never()).saveAndFlush(any());
        verify(eventos, never()).publishEvent(any(Object.class));
    }

    @Test
    void siLaBaseDetectaUnRepetidoEnUnaCarreraTambienEsConflicto() {
        when(especies.saveAndFlush(any(Especie.class))).thenThrow(new DataIntegrityViolationException("duplicado"));

        assertThrows(ConflictoException.class, () -> servicio.crear("Tapirus terrestris", null));
        verify(plantillas, never()).crear(any(), any());
    }
}
