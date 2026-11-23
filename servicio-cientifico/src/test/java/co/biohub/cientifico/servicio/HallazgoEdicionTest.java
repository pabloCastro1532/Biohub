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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** HU-08: editar un hallazgo inserta la versión n+1 con motivo y nunca toca la anterior (ASR-3). */
class HallazgoEdicionTest {

    private HallazgoRepository hallazgos;
    private VersionHallazgoRepository versiones;
    private PlantillaService plantillas;
    private UsuarioConsulta usuarios;
    private ApplicationEventPublisher eventos;
    private HallazgoService servicio;

    @BeforeEach
    void preparar() {
        hallazgos = mock(HallazgoRepository.class);
        versiones = mock(VersionHallazgoRepository.class);
        EspecieRepository especies = mock(EspecieRepository.class);
        plantillas = mock(PlantillaService.class);
        usuarios = mock(UsuarioConsulta.class);
        eventos = mock(ApplicationEventPublisher.class);
        servicio = new HallazgoService(hallazgos, versiones, especies, plantillas, usuarios, eventos);

        Hallazgo hallazgo = Hallazgo.nuevo(7L);
        ReflectionTestUtils.setField(hallazgo, "id", 50L);
        VersionHallazgo actual = VersionHallazgo.nueva(50L, 1, 1L, "Meta", new BigDecimal("4.710989"),
                new BigDecimal("-74.072092"), LocalDate.now().minusDays(5), Map.of("sexo", "hembra"), 7L, null);

        when(hallazgos.findById(50L)).thenReturn(Optional.of(hallazgo));
        when(versiones.findFirstByHallazgoIdOrderByNumeroDesc(50L)).thenReturn(Optional.of(actual));
        when(plantillas.validar(eq(1L), anyMap())).thenReturn(List.of());
        when(usuarios.porNombre("investigador"))
                .thenReturn(Optional.of(new UsuarioBasico(7L, "investigador", "INVESTIGADOR", true)));
        when(usuarios.porNombre("director"))
                .thenReturn(Optional.of(new UsuarioBasico(8L, "director", "DIRECTOR", true)));
        when(versiones.save(any(VersionHallazgo.class))).thenAnswer(i -> i.getArgument(0));
    }

    /** Cambia la región respecto a la versión actual. */
    private EdicionHallazgo edicion(String motivo) {
        return new EdicionHallazgo("  Casanare ", new BigDecimal("4.710989"), new BigDecimal("-74.072092"),
                LocalDate.now().minusDays(5), Map.of("sexo", "hembra"), motivo);
    }

    @Test
    void hu08_crea_la_version_siguiente_con_motivo_y_editor() {
        HallazgoVista v = servicio.editar(50L, edicion("  Corrección de la región  "), "director");

        assertEquals(50L, v.id());
        assertEquals(2, v.version());
        assertEquals("Casanare", v.region());
        assertEquals("director", v.autor());

        ArgumentCaptor<VersionHallazgo> guardada = ArgumentCaptor.forClass(VersionHallazgo.class);
        verify(versiones, times(1)).save(guardada.capture());
        VersionHallazgo nueva = guardada.getValue();
        assertEquals(2, nueva.getNumero());
        assertEquals(8L, nueva.getAutorId());
        assertEquals("Corrección de la región", nueva.getMotivo());
        assertEquals(1L, nueva.getEspecieId());
    }

    @Test
    void hu08_no_toca_la_version_anterior_ni_borra_nada() {
        servicio.editar(50L, edicion("Corrección"), "investigador");

        // Solo se agrega una versión: no hay llamadas de borrado ni se vuelve a guardar la identidad del hallazgo.
        verify(versiones, times(1)).save(any(VersionHallazgo.class));
        verify(versiones, never()).delete(any());
        verify(versiones, never()).deleteById(any());
        verify(hallazgos, never()).save(any());
    }

    @Test
    void hu08_publica_el_evento_con_la_version_nueva() {
        servicio.editar(50L, edicion("Corrección"), "investigador");

        ArgumentCaptor<Object> evento = ArgumentCaptor.forClass(Object.class);
        verify(eventos).publishEvent(evento.capture());
        HallazgoRegistradoEvento e = (HallazgoRegistradoEvento) evento.getValue();
        assertEquals(50L, e.hallazgoId());
        assertEquals(2, e.version());
        assertEquals("Casanare", e.region());
    }

    @Test
    void hu08_exige_un_motivo() {
        assertThrows(SolicitudInvalidaException.class, () -> servicio.editar(50L, edicion(null), "investigador"));
        assertThrows(SolicitudInvalidaException.class, () -> servicio.editar(50L, edicion("   "), "investigador"));
        assertThrows(SolicitudInvalidaException.class,
                () -> servicio.editar(50L, edicion("x".repeat(501)), "investigador"));
        verify(versiones, never()).save(any());
        verify(eventos, never()).publishEvent(any(Object.class));
    }

    @Test
    void hu08_rechaza_una_edicion_que_no_cambia_nada() {
        EdicionHallazgo igual = new EdicionHallazgo("Meta", new BigDecimal("4.710989"), new BigDecimal("-74.072092"),
                LocalDate.now().minusDays(5), Map.of("sexo", "hembra"), "Sin cambios");

        SolicitudInvalidaException e = assertThrows(SolicitudInvalidaException.class,
                () -> servicio.editar(50L, igual, "investigador"));

        assertEquals("No hay cambios respecto a la versión actual", e.getMessage());
        verify(versiones, never()).save(any());
    }

    @Test
    void hu08_rechaza_atributos_que_no_cumplen_la_plantilla() {
        when(plantillas.validar(eq(1L), anyMap())).thenReturn(List.of("$.sexo: es obligatorio"));

        assertThrows(SolicitudInvalidaException.class, () -> servicio.editar(50L, edicion("Corrección"), "investigador"));
        verify(versiones, never()).save(any());
    }

    @Test
    void hu08_un_hallazgo_que_no_existe_da_404() {
        when(hallazgos.findById(99L)).thenReturn(Optional.empty());

        assertThrows(RecursoNoEncontradoException.class, () -> servicio.editar(99L, edicion("Corrección"), "investigador"));
        verify(versiones, never()).save(any());
    }

    @Test
    void hu08_dos_ediciones_a_la_vez_dan_conflicto_y_no_publican_evento() {
        when(versiones.save(any(VersionHallazgo.class))).thenThrow(new DataIntegrityViolationException("duplicado"));

        assertThrows(ConflictoException.class, () -> servicio.editar(50L, edicion("Corrección"), "investigador"));
        verify(eventos, never()).publishEvent(any(Object.class));
    }
}
