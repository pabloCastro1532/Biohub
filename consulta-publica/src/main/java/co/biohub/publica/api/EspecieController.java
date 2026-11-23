package co.biohub.publica.api;

import co.biohub.publica.servicio.ConsultaEspecies;
import co.biohub.publica.servicio.EspecieResumen;
import co.biohub.publica.servicio.FichaEspecie;
import co.biohub.publica.servicio.ParametrosBusqueda;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Consulta pública (HU-18 y HU-19). Abierta a cualquiera: nada de lo que devuelve incluye ubicación exacta. */
@RestController
@RequestMapping("/api/publico/especies")
public class EspecieController {

    private final ConsultaEspecies consulta;

    public EspecieController(ConsultaEspecies consulta) {
        this.consulta = consulta;
    }

    /** HU-18: GET /api/publico/especies?q=jaguar&region=Meta&limite=20 */
    @GetMapping
    public List<EspecieResumen> buscar(@RequestParam(required = false) String q,
                                       @RequestParam(required = false) String region,
                                       @RequestParam(required = false) Integer limite) {
        return consulta.buscar(ParametrosBusqueda.patron(q), ParametrosBusqueda.region(region),
                ParametrosBusqueda.limite(limite));
    }

    /** HU-19: ficha de una especie. */
    @GetMapping("/{id}")
    public FichaEspecie ficha(@PathVariable long id) {
        return consulta.ficha(id);
    }
}
