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
import org.springframework.test.util.ReflectionTestUtils;

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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class HallazgoServiceTest {

    private HallazgoRepository hallazgos;
    private VersionHallazgoRepository versiones;
    private EspecieRepository especies;
    private PlantillaService plantillas;
    private UsuarioConsulta usuarios;
    private ApplicationEventPublisher eventos;
    private HallazgoService servicio;

    @BeforeEach
    void preparar() {
        hallazgos = mock(HallazgoRepository.class);
        versiones = mock(VersionHallazgoRepository.class);
        especies = mock(EspecieRepository.class);
        plantillas = mock(PlantillaService.class);
        usuarios = mock(UsuarioConsulta.class);
        eventos = mock(ApplicationEventPublisher.class);
        servicio = new HallazgoService(hallazgos, versiones, especies, plantillas, usuarios, eventos);

        when(especies.existsById(1L)).thenReturn(true);
        when(plantillas.validar(eq(1L), anyMap())).thenReturn(List.of());
        when(usuarios.porNombre("investigador"))
                .thenReturn(Optional.of(new UsuarioBasico(7L, "investigador", "INVESTIGADOR", true)));
        when(hallazgos.save(any(Hallazgo.class))).thenAnswer(i -> {
            Hallazgo h = i.getArgument(0);
            ReflectionTestUtils.setField(h, "id", 50L);
            return h;
        });
        when(versiones.save(any(VersionHallazgo.class))).thenAnswer(i -> i.getArgument(0));
    }

    private NuevoHallazgo valido() {
        return new NuevoHallazgo(1L, "  Meta  ", new BigDecimal("4.710989"), new BigDecimal("-74.072092"),
                LocalDate.now().minusDays(1), Map.of("sexo", "hembra"));
    }

    @Test
    void hu01_registra_la_version_1_con_el_autor_y_sin_motivo() {
        HallazgoVista v = servicio.registrar(valido(), "investigador");

        assertEquals(50L, v.id());
        assertEquals(1, v.version());
        assertEquals("Meta", v.region());
        assertEquals("investigador", v.autor());

        ArgumentCaptor<VersionHallazgo> guardada = ArgumentCaptor.forClass(VersionHallazgo.class);
        verify(versiones).save(guardada.capture());
        assertEquals(1, guardada.getValue().getNumero());
        assertEquals(7L, guardada.getValue().getAutorId());
        assertNull(guardada.getValue().getMotivo());
    }

    @Test
    void hu01_publica_el_evento_con_region_y_sin_coordenadas() {
        servicio.registrar(valido(), "investigador");

        ArgumentCaptor<Object> evento = ArgumentCaptor.forClass(Object.class);
        verify(eventos).publishEvent(evento.capture());
        HallazgoRegistradoEvento e = (HallazgoRegistradoEvento) evento.getValue();
        assertEquals(50L, e.hallazgoId());
        assertEquals("Meta", e.region());
        assertEquals(1, e.version());
        assertFalse(nombresDeCampos(HallazgoRegistradoEvento.class).contains("latitud"));
        assertFalse(nombresDeCampos(HallazgoRegistradoEvento.class).contains("longitud"));
    }

    @Test
    void asr4_la_respuesta_del_hallazgo_no_tiene_coordenadas() {
        assertFalse(nombresDeCampos(HallazgoVista.class).contains("latitud"));
        assertFalse(nombresDeCampos(HallazgoVista.class).contains("longitud"));
    }

    @Test
    void hu01_guarda_las_coordenadas_con_6_decimales() {
        NuevoHallazgo datos = new NuevoHallazgo(1L, "Meta", new BigDecimal("4.7109891234"), new BigDecimal("-74.07"),
                LocalDate.now(), Map.of("sexo", "macho"));

        servicio.registrar(datos, "investigador");

        ArgumentCaptor<VersionHallazgo> guardada = ArgumentCaptor.forClass(VersionHallazgo.class);
        verify(versiones).save(guardada.capture());
        assertEquals(new BigDecimal("4.710989"), guardada.getValue().getLatitud());
        assertEquals(new BigDecimal("-74.070000"), guardada.getValue().getLongitud());
    }

    @Test
    void hu01_rechaza_atributos_que_no_cumplen_la_plantilla_y_no_guarda_nada() {
        when(plantillas.validar(eq(1L), anyMap())).thenReturn(List.of("$.sexo: es obligatorio"));

        SolicitudInvalidaException e = assertThrows(SolicitudInvalidaException.class,
                () -> servicio.registrar(valido(), "investigador"));

        assertEquals("Los atributos no cumplen la plantilla: $.sexo: es obligatorio", e.getMessage());
        verify(hallazgos, never()).save(any());
        verify(versiones, never()).save(any());
        verify(eventos, never()).publishEvent(any(Object.class));
    }

    @Test
    void hu01_rechaza_una_especie_que_no_existe() {
        NuevoHallazgo datos = new NuevoHallazgo(99L, "Meta", BigDecimal.ONE, BigDecimal.ONE, LocalDate.now(), Map.of());

        assertThrows(RecursoNoEncontradoException.class, () -> servicio.registrar(datos, "investigador"));
        verify(hallazgos, never()).save(any());
    }

    @Test
    void hu01_rechaza_fecha_futura_region_vacia_y_coordenadas_fuera_de_rango() {
        LocalDate manana = LocalDate.now().plusDays(1);
        assertThrows(SolicitudInvalidaException.class, () -> servicio.registrar(
                new NuevoHallazgo(1L, "Meta", BigDecimal.ONE, BigDecimal.ONE, manana, Map.of()), "investigador"));
        assertThrows(SolicitudInvalidaException.class, () -> servicio.registrar(
                new NuevoHallazgo(1L, "   ", BigDecimal.ONE, BigDecimal.ONE, LocalDate.now(), Map.of()), "investigador"));
        assertThrows(SolicitudInvalidaException.class, () -> servicio.registrar(
                new NuevoHallazgo(1L, "Meta", new BigDecimal("91"), BigDecimal.ONE, LocalDate.now(), Map.of()), "investigador"));
        assertThrows(SolicitudInvalidaException.class, () -> servicio.registrar(
                new NuevoHallazgo(1L, "Meta", BigDecimal.ONE, new BigDecimal("-181"), LocalDate.now(), Map.of()), "investigador"));
        verify(hallazgos, never()).save(any());
    }

    @Test
    void hu01_rechaza_si_el_autor_no_existe_en_la_tabla_de_usuarios() {
        when(usuarios.porNombre("fantasma")).thenReturn(Optional.empty());

        assertThrows(SolicitudInvalidaException.class, () -> servicio.registrar(valido(), "fantasma"));
        verify(hallazgos, never()).save(any());
    }

    @Test
    void consulta_de_un_hallazgo_que_no_existe_da_404() {
        when(hallazgos.findById(77L)).thenReturn(Optional.empty());

        assertThrows(RecursoNoEncontradoException.class, () -> servicio.obtener(77L));
    }

    private static List<String> nombresDeCampos(Class<?> registro) {
        return Arrays.stream(registro.getRecordComponents()).map(RecordComponent::getName).toList();
    }
}
