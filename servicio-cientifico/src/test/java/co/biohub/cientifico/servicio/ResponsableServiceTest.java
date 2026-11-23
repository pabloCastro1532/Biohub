package co.biohub.cientifico.servicio;

import co.biohub.cientifico.dominio.AsignacionResponsable;
import co.biohub.cientifico.dominio.Equipo;
import co.biohub.cientifico.repositorio.AsignacionResponsableRepository;
import co.biohub.cientifico.repositorio.EquipoRepository;
import co.biohub.cientifico.repositorio.UsuarioConsulta;
import co.biohub.cientifico.repositorio.UsuarioConsulta.UsuarioBasico;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ResponsableServiceTest {

    private AsignacionResponsableRepository asignaciones;
    private EquipoRepository equipos;
    private UsuarioConsulta usuarios;
    private ResponsableService servicio;
    private Equipo gps;

    @BeforeEach
    void preparar() {
        asignaciones = mock(AsignacionResponsableRepository.class);
        equipos = mock(EquipoRepository.class);
        usuarios = mock(UsuarioConsulta.class);
        servicio = new ResponsableService(asignaciones, equipos, usuarios);

        gps = Equipo.nuevo("GPS-001", "GPS Garmin", "Navegacion", null);
        ReflectionTestUtils.setField(gps, "id", 1L);
        when(equipos.findById(1L)).thenReturn(Optional.of(gps));
        when(asignaciones.saveAndFlush(any(AsignacionResponsable.class))).thenAnswer(i -> i.getArgument(0));
        when(asignaciones.findByEquipoIdAndHastaIsNull(1L)).thenReturn(Optional.empty());

        when(usuarios.porNombre("ana")).thenReturn(Optional.of(new UsuarioBasico(10L, "ana", "INVESTIGADOR", true)));
        when(usuarios.porId(10L)).thenReturn(Optional.of(new UsuarioBasico(10L, "ana", "INVESTIGADOR", true)));
        when(usuarios.porNombre("luis")).thenReturn(Optional.of(new UsuarioBasico(11L, "luis", "INVESTIGADOR", true)));
    }

    @Test
    void hu06_asigna_un_equipo_sin_responsable_a_un_investigador() {
        ResponsableVista v = servicio.asignar(1L, " ana ", "administrador");

        assertEquals("GPS-001", v.equipoCodigo());
        assertEquals("ana", v.responsable());
        assertEquals("administrador", v.asignadoPor());
        assertNotNull(v.asignadoEn());
        verify(asignaciones, never()).cerrar(anyLong(), any());
    }

    @Test
    void hu06_reasignar_cierra_la_asignacion_anterior_y_la_conserva() {
        AsignacionResponsable deLuis = AsignacionResponsable.nueva(1L, 11L, "administrador", LocalDateTime.now().minusDays(2));
        ReflectionTestUtils.setField(deLuis, "id", 99L);
        when(asignaciones.findByEquipoIdAndHastaIsNull(1L)).thenReturn(Optional.of(deLuis));

        ResponsableVista v = servicio.asignar(1L, "ana", "director");

        assertEquals("ana", v.responsable());
        verify(asignaciones).cerrar(eq(99L), any(LocalDateTime.class));
        verify(asignaciones).saveAndFlush(any(AsignacionResponsable.class));
    }

    @Test
    void hu06_no_reasigna_al_mismo_responsable() {
        AsignacionResponsable deAna = AsignacionResponsable.nueva(1L, 10L, "administrador", LocalDateTime.now());
        ReflectionTestUtils.setField(deAna, "id", 5L);
        when(asignaciones.findByEquipoIdAndHastaIsNull(1L)).thenReturn(Optional.of(deAna));

        assertThrows(ConflictoException.class, () -> servicio.asignar(1L, "ana", "administrador"));
        verify(asignaciones, never()).saveAndFlush(any());
    }

    @Test
    void hu06_solo_se_asigna_a_investigadores_activos() {
        when(usuarios.porNombre("director")).thenReturn(Optional.of(new UsuarioBasico(2L, "director", "DIRECTOR", true)));
        when(usuarios.porNombre("inactivo")).thenReturn(Optional.of(new UsuarioBasico(3L, "inactivo", "INVESTIGADOR", false)));

        assertThrows(SolicitudInvalidaException.class, () -> servicio.asignar(1L, "director", "administrador"));
        assertThrows(SolicitudInvalidaException.class, () -> servicio.asignar(1L, "inactivo", "administrador"));
        verify(asignaciones, never()).saveAndFlush(any());
    }

    @Test
    void hu06_rechaza_usuario_o_equipo_inexistente_y_datos_vacios() {
        when(usuarios.porNombre("nadie")).thenReturn(Optional.empty());

        assertThrows(RecursoNoEncontradoException.class, () -> servicio.asignar(1L, "nadie", "administrador"));
        assertThrows(RecursoNoEncontradoException.class, () -> servicio.asignar(77L, "ana", "administrador"));
        assertThrows(SolicitudInvalidaException.class, () -> servicio.asignar(1L, "  ", "administrador"));
    }

    @Test
    void hu06_asignaciones_simultaneas_el_unique_de_la_base_responde_409() {
        when(asignaciones.saveAndFlush(any(AsignacionResponsable.class))).thenThrow(new DataIntegrityViolationException("uq"));

        assertThrows(ConflictoException.class, () -> servicio.asignar(1L, "ana", "administrador"));
    }

    @Test
    void hu07_consulta_el_responsable_actual() {
        AsignacionResponsable deAna = AsignacionResponsable.nueva(1L, 10L, "administrador", LocalDateTime.now());
        when(asignaciones.findByEquipoIdAndHastaIsNull(1L)).thenReturn(Optional.of(deAna));

        ResponsableVista v = servicio.consultar(1L);

        assertEquals("GPS-001", v.equipoCodigo());
        assertEquals("ana", v.responsable());
        assertEquals("administrador", v.asignadoPor());
    }

    @Test
    void hu07_un_equipo_sin_responsable_se_informa_con_campos_nulos() {
        ResponsableVista v = servicio.consultar(1L);

        assertEquals("GPS-001", v.equipoCodigo());
        assertNull(v.responsable());
        assertNull(v.asignadoEn());
        assertThrows(RecursoNoEncontradoException.class, () -> servicio.consultar(77L));
    }

    @Test
    void hu07_lista_los_equipos_que_hoy_tienen_responsable() {
        AsignacionResponsable deAna = AsignacionResponsable.nueva(1L, 10L, "administrador", LocalDateTime.now());
        when(asignaciones.findByHastaIsNull()).thenReturn(List.of(deAna));

        List<ResponsableVista> lista = servicio.listarVigentes();

        assertEquals(1, lista.size());
        assertEquals("ana", lista.get(0).responsable());
    }
}
