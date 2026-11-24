package co.biohub.publica.servicio;

import co.biohub.publica.repositorio.LecturaRepository;
import co.biohub.publica.repositorio.LecturaRepository.Agregado;
import co.biohub.publica.repositorio.LecturaRepository.EspecieBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** HU-18 (búsqueda) y HU-19 (ficha de especie). */
class ConsultaEspeciesTest {

    private LecturaRepository lectura;
    private ConsultaEspecies consulta;

    @BeforeEach
    void preparar() {
        lectura = mock(LecturaRepository.class);
        consulta = new ConsultaEspecies(lectura);
    }

    @Test
    void hu18_la_busqueda_entrega_lo_que_devuelve_la_bd_de_lectura() {
        List<EspecieResumen> resultado = List.of(
                new EspecieResumen(1L, "Panthera onca", "Jaguar", 3, LocalDate.of(2026, 11, 15)));
        when(lectura.buscar("%jag%", "Meta", 20)).thenReturn(resultado);

        assertEquals(resultado, consulta.buscar("%jag%", "Meta", 20));
        verify(lectura).buscar("%jag%", "Meta", 20);
    }

    @Test
    void hu19_la_ficha_junta_nombres_totales_regiones_y_hallazgos_recientes() {
        when(lectura.especie(1L)).thenReturn(Optional.of(new EspecieBase(1L, "Panthera onca", "Jaguar")));
        when(lectura.agregado(1L)).thenReturn(new Agregado(3, LocalDate.of(2026, 11, 15)));
        when(lectura.regiones(1L)).thenReturn(List.of(new RegionConteo("Meta", 2), new RegionConteo("Amazonas", 1)));
        when(lectura.recientes(1L, ConsultaEspecies.HALLAZGOS_RECIENTES)).thenReturn(List.of(
                new HallazgoReciente(7L, "Meta", LocalDate.of(2026, 11, 15), Map.of("sexo", "hembra"))));

        FichaEspecie ficha = consulta.ficha(1L);

        assertEquals("Panthera onca", ficha.nombreCientifico());
        assertEquals("Jaguar", ficha.nombreComun());
        assertEquals(3, ficha.totalHallazgos());
        assertEquals(LocalDate.of(2026, 11, 15), ficha.ultimaObservacion());
        assertEquals(2, ficha.regiones().size());
        assertEquals("Meta", ficha.regiones().get(0).region());
        assertEquals(1, ficha.recientes().size());
        assertEquals("hembra", ficha.recientes().get(0).atributos().get("sexo"));
    }

    @Test
    void hu19_una_especie_sin_hallazgos_tiene_ficha_con_ceros() {
        when(lectura.especie(2L)).thenReturn(Optional.of(new EspecieBase(2L, "Ara macao", "Guacamaya bandera")));
        when(lectura.agregado(2L)).thenReturn(new Agregado(0, null));
        when(lectura.regiones(2L)).thenReturn(List.of());
        when(lectura.recientes(2L, ConsultaEspecies.HALLAZGOS_RECIENTES)).thenReturn(List.of());

        FichaEspecie ficha = consulta.ficha(2L);

        assertEquals(0, ficha.totalHallazgos());
        assertEquals(List.of(), ficha.regiones());
        assertEquals(List.of(), ficha.recientes());
    }

    @Test
    void hu19_una_especie_que_no_existe_da_error_de_no_encontrada() {
        when(lectura.especie(99L)).thenReturn(Optional.empty());

        assertThrows(EspecieNoEncontradaException.class, () -> consulta.ficha(99L));
    }
}
