package co.biohub.publica.servicio;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** HU-18: el texto de búsqueda se limpia antes de consultar. */
class ParametrosBusquedaTest {

    @Test
    void hu18_un_texto_se_convierte_en_patron_sin_espacios_sobrantes() {
        assertEquals("%jaguar%", ParametrosBusqueda.patron("  jaguar "));
    }

    @Test
    void hu18_texto_vacio_o_en_blanco_significa_sin_filtro() {
        assertNull(ParametrosBusqueda.patron(null));
        assertNull(ParametrosBusqueda.patron(""));
        assertNull(ParametrosBusqueda.patron("   "));
        assertNull(ParametrosBusqueda.region(" "));
    }

    @Test
    void hu18_los_comodines_que_escribe_el_usuario_se_escapan() {
        assertEquals("%100\\%\\_x\\\\y%", ParametrosBusqueda.patron("100%_x\\y"));
    }

    @Test
    void hu18_un_texto_demasiado_largo_se_rechaza() {
        String largo = "a".repeat(ParametrosBusqueda.LARGO_MAXIMO_TEXTO + 1);
        assertThrows(SolicitudInvalidaException.class, () -> ParametrosBusqueda.patron(largo));
        assertThrows(SolicitudInvalidaException.class, () -> ParametrosBusqueda.region(largo));
    }

    @Test
    void hu18_la_region_se_recorta() {
        assertEquals("Meta", ParametrosBusqueda.region(" Meta "));
    }

    @Test
    void hu18_el_limite_tiene_valor_por_defecto_y_tope() {
        assertEquals(20, ParametrosBusqueda.limite(null));
        assertEquals(1, ParametrosBusqueda.limite(0));
        assertEquals(1, ParametrosBusqueda.limite(-5));
        assertEquals(30, ParametrosBusqueda.limite(30));
        assertEquals(50, ParametrosBusqueda.limite(5000));
    }
}
