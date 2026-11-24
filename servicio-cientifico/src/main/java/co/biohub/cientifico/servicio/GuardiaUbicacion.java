package co.biohub.cientifico.servicio;

import co.biohub.cientifico.dominio.ResultadoAcceso;
import co.biohub.cientifico.seguridad.Rol;
import org.springframework.stereotype.Service;

import java.util.EnumSet;
import java.util.Set;

/**
 * HU-16 y HU-17: único punto de decisión sobre quién ve coordenadas exactas (ASR-4).
 * Ninguna otra clase puede usar ProveedorUbicacion (lo verifica ArchUnit).
 *
 * Primero deja constancia del acceso, aceptado o rechazado (HU-25); si no se puede registrar, la
 * consulta falla y no sale ninguna coordenada. Después responde según el rol.
 */
@Service
public class GuardiaUbicacion {

    /** Personal científico de alto rango (HU-16): el director científico. */
    static final Set<Rol> ROLES_ALTO_RANGO = EnumSet.of(Rol.DIRECTOR);

    private final RegistroAccesoService accesos;
    private final ProveedorUbicacion proveedor;

    public GuardiaUbicacion(RegistroAccesoService accesos, ProveedorUbicacion proveedor) {
        this.accesos = accesos;
        this.proveedor = proveedor;
    }

    public UbicacionVista consultar(Long hallazgoId, String usuario, Rol rol) {
        boolean autorizado = rol != null && ROLES_ALTO_RANGO.contains(rol);

        accesos.registrar(usuario, hallazgoId, autorizado ? ResultadoAcceso.ACEPTADO : ResultadoAcceso.RECHAZADO);

        if (!autorizado) {
            return UbicacionVista.soloRegion(hallazgoId, proveedor.region(hallazgoId));
        }
        ProveedorUbicacion.Ubicacion u = proveedor.exacta(hallazgoId);
        return UbicacionVista.conCoordenadas(hallazgoId, u.region(), u.latitud(), u.longitud());
    }
}