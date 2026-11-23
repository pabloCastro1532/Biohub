package co.biohub.publica.api;

import co.biohub.publica.servicio.ConsultaEspecies;
import co.biohub.publica.servicio.EspecieResumen;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** HU-18: el controlador limpia los parámetros antes de consultar. */
class EspecieControllerTest {

    @Test
    void hu18_los_parametros_se_limpian_antes_de_consultar() {
        ConsultaEspecies consulta = mock(ConsultaEspecies.class);
        List<EspecieResumen> esperado = List.of(
                new EspecieResumen(1L, "Panthera onca", "Jaguar", 3, LocalDate.of(2026, 11, 15)));
        when(consulta.buscar("%jaguar%", "Meta", 20)).thenReturn(esperado);

        List<EspecieResumen> resultado = new EspecieController(consulta).buscar("  jaguar ", " Meta ", null);

        assertEquals(esperado, resultado);
    }

    @Test
    void hu18_sin_parametros_busca_todas_las_especies_con_el_limite_por_defecto() {
        ConsultaEspecies consulta = mock(ConsultaEspecies.class);

        new EspecieController(consulta).buscar(null, "", null);

        verify(consulta).buscar(null, null, 20);
    }

    @Test
    void hu19_la_ficha_se_pide_por_id() {
        ConsultaEspecies consulta = mock(ConsultaEspecies.class);

        new EspecieController(consulta).ficha(1L);

        verify(consulta).ficha(1L);
    }
}
