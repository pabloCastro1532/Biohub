package co.biohub.cientifico.servicio;

import co.biohub.cientifico.dominio.Hallazgo;
import co.biohub.cientifico.dominio.VersionHallazgo;
import co.biohub.cientifico.repositorio.EspecieRepository;
import co.biohub.cientifico.repositorio.HallazgoRepository;
import co.biohub.cientifico.repositorio.UsuarioConsulta;
import co.biohub.cientifico.repositorio.UsuarioConsulta.UsuarioBasico;
import co.biohub.cientifico.repositorio.VersionHallazgoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** HU-09: reclasificar un hallazgo crea la versión n+1 con otra especie y un motivo, sin tocar la anterior (ASR-3). */
class HallazgoReclasificacionTest {

    private HallazgoRepository hallazgos;
    private VersionHallazgoRepository versiones;
    private EspecieRepository especies;
    private PlantillaService plantillas;
    private ApplicationEventPublisher eventos;
    private HallazgoService servicio;

    @BeforeEach
    void preparar() {
        hallazgos = mock(HallazgoRepository.class);
        versiones = mock(VersionHallazgoRepository.class);
        especies = mock(EspecieRepository.class);
        plantillas = mock(PlantillaService.class);
        UsuarioConsulta usuarios = mock(UsuarioConsulta.class);
        eventos = mock(ApplicationEventPublisher.class);
        servicio = new HallazgoService(hallazgos, versiones, especies, plantillas, usuarios, eventos);

        Hallazgo hallazgo = Hallazgo.nuevo(7L);
        ReflectionTestUtils.setField(hallazgo, "id", 50L);
        VersionHallazgo actual = VersionHallazgo.nueva(50L, 1, 1L, "Meta", new BigDecimal("4.710989"),
                new BigDecimal("-74.072092"), LocalDate.now().minusDays(5), Map.of("sexo", "hembra"), 7L, null);

        when(hallazgos.findById(50L)).thenReturn(Optional.of(hallazgo));
        when(versiones.findFirstByHallazgoIdOrderByNumeroDesc(50L)).thenReturn(Optional.of(actual));
        when(especies.existsById(2L)).thenReturn(true);
        when(plantillas.validar(eq(2L), anyMap())).thenReturn(List.of());
        when(usuarios.porNombre("director"))
                .thenReturn(Optional.of(new UsuarioBasico(8L, "director", "DIRECTOR", true)));
        when(versiones.save(any(VersionHallazgo.class))).thenAnswer(i -> i.getArgument(0));
    }

    private ReclasificacionHallazgo datos(Long especie, String motivo) {
        return new ReclasificacionHallazgo(especie, Map.of("tamano", "grande"), motivo);
    }

    @Test
    void hu09_crea_la_version_siguiente_con_la_especie_nueva_y_conserva_lugar_y_fecha() {
        HallazgoVista v = servicio.reclasificar(50L, datos(2L, "  Se confundió con otra especie  "), "director");

        assertEquals(50L, v.id());
        assertEquals(2, v.version());
        assertEquals(2L, v.especieId());
        assertEquals("Meta", v.region());
        assertEquals("director", v.autor());

        ArgumentCaptor<VersionHallazgo> guardada = ArgumentCaptor.forClass(VersionHallazgo.class);
        verify(versiones, times(1)).save(guardada.capture());
        VersionHallazgo nueva = guardada.getValue();
        assertEquals(2, nueva.getNumero());
        assertEquals(2L, nueva.getEspecieId());
        assertEquals(8L, nueva.getAutorId());
        assertEquals("Se confundió con otra especie", nueva.getMotivo());
        assertEquals(0, new BigDecimal("4.710989").compareTo(nueva.getLatitud()));
        assertEquals(0, new BigDecimal("-74.072092").compareTo(nueva.getLongitud()));
        assertEquals(Map.of("tamano", "grande"), nueva.getAtributos());
    }

    @Test
    void hu09_no_toca_la_version_anterior_ni_borra_nada() {
        servicio.reclasificar(50L, datos(2L, "Corrección"), "director");

        verify(versiones, times(1)).save(any(VersionHallazgo.class));
        verify(versiones, never()).delete(any());
        verify(versiones, never()).deleteById(any());
        verify(hallazgos, never()).save(any());
    }

    @Test
    void hu09_valida_los_atributos_contra_la_plantilla_de_la_especie_nueva() {
        servicio.reclasificar(50L, datos(2L, "Corrección"), "director");

        verify(plantillas).validar(eq(2L), anyMap());
        verify(plantillas, never()).validar(eq(1L), anyMap());
    }

    @Test
    void hu09_rechaza_atributos_que_no_cumplen_la_plantilla_nueva() {
        when(plantillas.validar(eq(2L), anyMap())).thenReturn(List.of("$.tamano: es obligatorio"));

        SolicitudInvalidaException e = assertThrows(SolicitudInvalidaException.class,
                () -> servicio.reclasificar(50L, datos(2L, "Corrección"), "director"));

        assertTrue(e.getMessage().contains("plantilla de la especie nueva"));
        assertTrue(e.getMessage().contains("tamano"));
        verify(versiones, never()).save(any());
        verify(eventos, never()).publishEvent(any(Object.class));
    }

    @Test
    void hu09_exige_un_motivo() {
        assertThrows(SolicitudInvalidaException.class, () -> servicio.reclasificar(50L, datos(2L, null), "director"));
        assertThrows(SolicitudInvalidaException.class, () -> servicio.reclasificar(50L, datos(2L, "   "), "director"));
        assertThrows(SolicitudInvalidaException.class,
                () -> servicio.reclasificar(50L, datos(2L, "x".repeat(501)), "director"));
        verify(versiones, never()).save(any());
        verify(eventos, never()).publishEvent(any(Object.class));
    }

    @Test
    void hu09_rechaza_la_misma_especie() {
        SolicitudInvalidaException e = assertThrows(SolicitudInvalidaException.class,
                () -> servicio.reclasificar(50L, datos(1L, "Corrección"), "director"));

        assertEquals("El hallazgo ya está clasificado como esa especie", e.getMessage());
        verify(versiones, never()).save(any());
    }

    @Test
    void hu09_exige_elegir_la_especie_nueva() {
        assertThrows(SolicitudInvalidaException.class,
                () -> servicio.reclasificar(50L, datos(null, "Corrección"), "director"));
        verify(versiones, never()).save(any());
    }

    @Test
    void hu09_rechaza_una_especie_que_no_existe() {
        assertThrows(RecursoNoEncontradoException.class,
                () -> servicio.reclasificar(50L, datos(99L, "Corrección"), "director"));
        verify(versiones, never()).save(any());
    }

    @Test
    void hu09_rechaza_un_hallazgo_que_no_existe() {
        assertThrows(RecursoNoEncontradoException.class,
                () -> servicio.reclasificar(404L, datos(2L, "Corrección"), "director"));
    }

    @Test
    void hu09_publica_el_estado_nuevo_y_la_reclasificacion() {
        servicio.reclasificar(50L, datos(2L, "Corrección"), "director");

        ArgumentCaptor<Object> capturados = ArgumentCaptor.forClass(Object.class);
        verify(eventos, times(2)).publishEvent(capturados.capture());
        HallazgoRegistradoEvento registrado = (HallazgoRegistradoEvento) capturados.getAllValues().get(0);
        assertEquals(50L, registrado.hallazgoId());
        assertEquals(2L, registrado.especieId());
        assertEquals(2, registrado.version());
        assertEquals("Meta", registrado.region());
        HallazgoReclasificadoEvento reclasificado = (HallazgoReclasificadoEvento) capturados.getAllValues().get(1);
        assertEquals(50L, reclasificado.hallazgoId());
        assertEquals(1L, reclasificado.especieAnterior());
        assertEquals(2L, reclasificado.especieNueva());
        assertEquals("director", reclasificado.autor());
        assertEquals("Corrección", reclasificado.motivo());
    }

    @Test
    void hu09_si_dos_personas_cambian_a_la_vez_avisa_del_conflicto() {
        when(versiones.save(any(VersionHallazgo.class))).thenThrow(new DataIntegrityViolationException("duplicado"));

        assertThrows(ConflictoException.class, () -> servicio.reclasificar(50L, datos(2L, "Corrección"), "director"));
        verify(eventos, never()).publishEvent(any(Object.class));
    }

    @Test
    void hu09_el_historial_muestra_que_cambio_la_especie() {
        VersionHallazgo v1 = VersionHallazgo.nueva(50L, 1, 1L, "Meta", new BigDecimal("4.710989"),
                new BigDecimal("-74.072092"), LocalDate.now().minusDays(5), Map.of("sexo", "hembra"), 7L, null);
        VersionHallazgo v2 = VersionHallazgo.nueva(50L, 2, 2L, "Meta", new BigDecimal("4.710989"),
                new BigDecimal("-74.072092"), LocalDate.now().minusDays(5), Map.of("tamano", "grande"), 8L, "Corrección");
        when(hallazgos.existsById(50L)).thenReturn(true);
        when(versiones.findByHallazgoIdOrderByNumeroAsc(50L)).thenReturn(List.of(v1, v2));

        // El historial usa UsuarioConsulta para los nombres; sin datos devuelve autor desconocido, que aquí no importa.
        List<VersionVista> historial = servicio.historial(50L);

        assertTrue(historial.get(1).cambios().contains("especie"));
        assertEquals("Corrección", historial.get(1).motivo());
        verify(versiones, atLeastOnce()).findByHallazgoIdOrderByNumeroAsc(50L);
    }
}
