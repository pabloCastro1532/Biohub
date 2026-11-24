package co.biohub.cientifico.api;

import co.biohub.cientifico.servicio.ContenidoImagen;
import co.biohub.cientifico.servicio.ImagenHallazgoService;
import co.biohub.cientifico.servicio.ImagenVista;
import co.biohub.cientifico.servicio.SolicitudInvalidaException;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Imágenes de un hallazgo. Subir: investigador y director. Ver: investigador, director y auditor.
 * Las imágenes se guardan sin metadatos (no llevan GPS) y no salen en la consulta pública.
 */
@RestController
@RequestMapping("/api/hallazgos/{id}/imagenes")
public class ImagenHallazgoController {

    private final ImagenHallazgoService servicio;

    public ImagenHallazgoController(ImagenHallazgoService servicio) {
        this.servicio = servicio;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('INVESTIGADOR','DIRECTOR')")
    public List<ImagenVista> subir(@PathVariable Long id, @RequestParam("archivo") MultipartFile archivo,
                                   Authentication usuario) {
        try {
            servicio.subir(id, archivo.getOriginalFilename(), archivo.getBytes(), usuario.getName());
        } catch (IOException e) {
            throw new SolicitudInvalidaException("No se pudo leer el archivo");
        }
        return servicio.listar(id);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('INVESTIGADOR','DIRECTOR','AUDITOR')")
    public List<ImagenVista> listar(@PathVariable Long id) {
        return servicio.listar(id);
    }

    @GetMapping("/{imagenId}")
    @PreAuthorize("hasAnyRole('INVESTIGADOR','DIRECTOR','AUDITOR')")
    public ResponseEntity<byte[]> ver(@PathVariable Long id, @PathVariable Long imagenId) {
        ContenidoImagen imagen = servicio.contenido(id, imagenId);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(imagen.tipoContenido()))
                .cacheControl(CacheControl.maxAge(1, TimeUnit.HOURS).cachePrivate())
                .header("X-Content-Type-Options", "nosniff")
                .body(imagen.bytes());
    }
}
