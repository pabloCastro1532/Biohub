package co.biohub.cientifico.servicio;

import co.biohub.cientifico.dominio.ResultadoAcceso;
import co.biohub.cientifico.seguridad.Rol;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** HU-15, HU-16 y HU-17: solo el director ve coordenadas, y todo acceso queda registrado antes de responder. */
class GuardiaUbicacionTest {

    private RegistroAccesoService accesos;
    private ProveedorUbicacion proveedor;
    private GuardiaUbicacion guardia;

    @BeforeEach
    void preparar() {
        accesos = mock(RegistroAccesoService.class);
        proveedor = mock(ProveedorUbicacion.class);
        guardia = new GuardiaUbicacion(accesos, proveedor);

        when(proveedor.region(10L)).thenReturn("Meta");
        when(proveedor.exacta(10L)).thenReturn(new ProveedorUbicacion.Ubicacion(
                "Meta", new BigDecimal("4.100000"), new BigDecimal("-73.600000")));
    }

    @Test
    void elDirectorRecibeLasCoordenadasExactasYQuedaRegistradoComoAceptado() {
        UbicacionVista vista = guardia.consultar(10L, "director", Rol.DIRECTOR);

        assertTrue(vista.exacta());
        assertEquals(new BigDecimal("4.100000"), vista.latitud());
        assertEquals(new BigDecimal("-73.600000"), vista.longitud());
        verify(accesos).registrar("director", 10L, ResultadoAcceso.ACEPTADO);
    }

    @Test
    void cualquierOtroRolRecibeSoloLaRegionYQuedaRegistradoComoRechazado() {
        int otrosRoles = 0;
        for (Rol rol : Rol.values()) {
            if (rol == Rol.DIRECTOR) {
                continue;
            }
            otrosRoles++;
            UbicacionVista vista = guardia.consultar(10L, "usuario", rol);

            assertFalse(vista.exacta(), "El rol " + rol + " no debe ver coordenadas exactas");
            assertNull(vista.latitud());
            assertNull(vista.longitud());
            assertEquals("Meta", vista.region());
        }
        verify(accesos, times(otrosRoles)).registrar(eq("usuario"), eq(10L), eq(ResultadoAcceso.RECHAZADO));
        verify(proveedor, never()).exacta(any());
    }

    @Test
    void sinRolNoSeVenCoordenadas() {
        UbicacionVista vista = guardia.consultar(10L, "usuario", null);

        assertFalse(vista.exacta());
        verify(accesos).registrar("usuario", 10L, ResultadoAcceso.RECHAZADO);
        verify(proveedor, never()).exacta(any());
    }

    @Test
    void primeroSeRegistraElAccesoYDespuesSeLeenLasCoordenadas() {
        guardia.consultar(10L, "director", Rol.DIRECTOR);

        InOrder orden = inOrder(accesos, proveedor);
        orden.verify(accesos).registrar("director", 10L, ResultadoAcceso.ACEPTADO);
        orden.verify(proveedor).exacta(10L);
    }

    @Test
    void siNoSePuedeRegistrarElAccesoNoSaleNingunaCoordenada() {
        when(accesos.registrar("director", 10L, ResultadoAcceso.ACEPTADO))
                .thenThrow(new IllegalStateException("BD caída"));

        assertThrows(IllegalStateException.class, () -> guardia.consultar(10L, "director", Rol.DIRECTOR));
        verify(proveedor, never()).exacta(any());
    }

    @Test
    void unHallazgoInexistenteSePropagaComoNoEncontradoSinLeerUbicacion() {
        when(accesos.registrar("director", 99L, ResultadoAcceso.ACEPTADO))
                .thenThrow(new RecursoNoEncontradoException("No existe el hallazgo 99"));

        assertThrows(RecursoNoEncontradoException.class, () -> guardia.consultar(99L, "director", Rol.DIRECTOR));
        verify(proveedor, never()).exacta(any());
        verify(proveedor, never()).region(any());
    }
}