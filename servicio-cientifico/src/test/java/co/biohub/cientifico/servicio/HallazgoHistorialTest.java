package co.biohub.cientifico.servicio;

import co.biohub.cientifico.dominio.VersionHallazgo;
import co.biohub.cientifico.repositorio.EspecieRepository;
import co.biohub.cientifico.repositorio.HallazgoRepository;
import co.biohub.cientifico.repositorio.UsuarioConsulta;
import co.biohub.cientifico.repositorio.UsuarioConsulta.UsuarioBasico;
import co.biohub.cientifico.repositorio.VersionHallazgoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

import java.lang.reflect.RecordComponent;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** HU-10, HU-11 y HU-12: el historial conserva cada versión con su autor, motivo y cambios, sin coordenadas. */
class HallazgoHistorialTest {

    private HallazgoRepository hallazgos;
    private VersionHallazgoRepository versiones;
    private HallazgoService servicio;

    @BeforeEach
    void preparar() {
        hallazgos = mock(HallazgoRepository.class);
        versiones = mock(VersionHallazgoRepository.class);
        UsuarioConsulta usuarios = mock(UsuarioConsulta.class);
        servicio = new HallazgoService(hallazgos, versiones, mock(EspecieRepository.class),
                mock(PlantillaService.class), usuarios, mock(ApplicationEventPublisher.class));

        when(usuarios.porId(7L)).thenReturn(Optional.of(new UsuarioBasico(7L, "investigador", "INVESTIGADOR", true)));
        when(usuarios.porId(8L)).thenReturn(Optional.of(new UsuarioBasico(8L, "director", "DIRECTOR", true)));

        LocalDate fecha = LocalDate.now().minusDays(5);
        VersionHallazgo v1 = VersionHallazgo.nueva(50L, 1, 1L, "Amazonas", new BigDecimal("-4.215000"),
                new BigDecimal("-69.940000"), fecha, Map.of("sexo", "macho", "peso_kg", 80), 7L, null);
        VersionHallazgo v2 = VersionHallazgo.nueva(50L, 2, 1L, "Casanare", new BigDecimal("-4.300000"),
                new BigDecimal("-69.940000"), fecha, Map.of("sexo", "macho", "peso_kg", 85), 8L,
                "Corrección de medición");

        when(hallazgos.existsById(50L)).thenReturn(true);
        when(versiones.findByHallazgoIdOrderByNumeroAsc(50L)).thenReturn(List.of(v1, v2));
    }

    @Test
    void hu10_conserva_todas_las_versiones_en_orden() {
        List<VersionVista> historial = servicio.historial(50L);

        assertEquals(2, historial.size());
        assertEquals(1, historial.get(0).numero());
        assertEquals("Amazonas", historial.get(0).region());
        assertEquals(2, historial.get(1).numero());
        assertEquals("Casanare", historial.get(1).region());
    }

    @Test
    void hu11_cada_version_lleva_autor_y_motivo() {
        List<VersionVista> historial = servicio.historial(50L);

        assertEquals("investigador", historial.get(0).autor());
        assertNull(historial.get(0).motivo());
        assertEquals("director", historial.get(1).autor());
        assertEquals("Corrección de medición", historial.get(1).motivo());
    }

    @Test
    void hu12_indica_que_cambio_en_cada_version() {
        List<VersionVista> historial = servicio.historial(50L);

        assertTrue(historial.get(0).cambios().isEmpty(), "la versión 1 es el registro inicial");
        assertEquals(List.of("región", "ubicación", "atributo peso_kg"), historial.get(1).cambios());
    }

    @Test
    void hu10_consulta_una_version_puntual_y_da_404_si_no_existe() {
        assertEquals("Amazonas", servicio.version(50L, 1).region());
        assertEquals("Casanare", servicio.version(50L, 2).region());
        assertThrows(RecursoNoEncontradoException.class, () -> servicio.version(50L, 3));
    }

    @Test
    void hu12_un_hallazgo_que_no_existe_da_404() {
        assertThrows(RecursoNoEncontradoException.class, () -> servicio.historial(99L));
    }

    @Test
    void asr4_el_historial_no_tiene_coordenadas() {
        List<String> campos = Arrays.stream(VersionVista.class.getRecordComponents())
                .map(RecordComponent::getName).toList();

        assertFalse(campos.contains("latitud"));
        assertFalse(campos.contains("longitud"));
    }

    @Test
    void consultar_el_historial_no_modifica_nada() {
        servicio.historial(50L);
        servicio.version(50L, 1);

        verify(versiones, never()).save(any());
        verify(versiones, never()).delete(any());
        verify(hallazgos, never()).save(any());
    }
}
