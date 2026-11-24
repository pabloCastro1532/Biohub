package co.biohub.cientifico.servicio;

import co.biohub.cientifico.repositorio.HallazgoRepository;
import co.biohub.cientifico.repositorio.ImagenHallazgoJdbc;
import co.biohub.cientifico.repositorio.UsuarioConsulta;
import co.biohub.cientifico.repositorio.UsuarioConsulta.UsuarioBasico;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;

/**
 * Imágenes de un hallazgo. Solo JPG y PNG, hasta 2 MB cada una y 5 por hallazgo. Una foto de campo suele traer la
 * posición GPS en sus metadatos; para no saltarse la protección de la ubicación (ASR-4) la imagen se vuelve a
 * codificar al guardarla, y con eso se pierden todos los metadatos.
 */
@Service
public class ImagenHallazgoService {

    static final int TAMANO_MAXIMO = 2 * 1024 * 1024;
    static final int MAXIMO_POR_HALLAZGO = 5;
    static final int LADO_MAXIMO = 8000;
    static final long PIXELES_MAXIMOS = 36_000_000L;

    private final HallazgoRepository hallazgos;
    private final ImagenHallazgoJdbc imagenes;
    private final UsuarioConsulta usuarios;

    public ImagenHallazgoService(HallazgoRepository hallazgos, ImagenHallazgoJdbc imagenes, UsuarioConsulta usuarios) {
        this.hallazgos = hallazgos;
        this.imagenes = imagenes;
        this.usuarios = usuarios;
    }

    public void subir(Long hallazgoId, String nombreOriginal, byte[] archivo, String nombreAutor) {
        existe(hallazgoId);
        if (archivo == null || archivo.length == 0) {
            throw new SolicitudInvalidaException("Elige una imagen");
        }
        if (archivo.length > TAMANO_MAXIMO) {
            throw new SolicitudInvalidaException("La imagen no puede pasar de 2 MB");
        }
        if (imagenes.contar(hallazgoId) >= MAXIMO_POR_HALLAZGO) {
            throw new SolicitudInvalidaException("Un hallazgo puede tener hasta " + MAXIMO_POR_HALLAZGO + " imágenes");
        }
        String tipo = tipoDe(archivo);
        byte[] limpia = sinMetadatos(archivo, tipo);
        UsuarioBasico autor = usuarios.porNombre(nombreAutor)
                .orElseThrow(() -> new SolicitudInvalidaException("No se encontró el usuario " + nombreAutor));
        imagenes.guardar(hallazgoId, nombreSeguro(nombreOriginal, tipo), tipo, limpia, autor.id());
    }

    public List<ImagenVista> listar(Long hallazgoId) {
        existe(hallazgoId);
        return imagenes.listar(hallazgoId);
    }

    public ContenidoImagen contenido(Long hallazgoId, Long imagenId) {
        existe(hallazgoId);
        return imagenes.contenido(hallazgoId, imagenId)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "El hallazgo " + hallazgoId + " no tiene la imagen " + imagenId));
    }

    private void existe(Long hallazgoId) {
        if (hallazgoId == null || !hallazgos.existsById(hallazgoId)) {
            throw new RecursoNoEncontradoException("No existe el hallazgo " + hallazgoId);
        }
    }

    /** El tipo se decide por el contenido real del archivo, no por la extensión ni por lo que diga el navegador. */
    static String tipoDe(byte[] b) {
        if (b.length >= 3 && (b[0] & 0xFF) == 0xFF && (b[1] & 0xFF) == 0xD8 && (b[2] & 0xFF) == 0xFF) {
            return "image/jpeg";
        }
        if (b.length >= 8 && (b[0] & 0xFF) == 0x89 && b[1] == 'P' && b[2] == 'N' && b[3] == 'G') {
            return "image/png";
        }
        throw new SolicitudInvalidaException("Solo se aceptan imágenes JPG o PNG");
    }

    /** Lee la imagen y la escribe de nuevo: así sale sin EXIF ni GPS, y se comprueba que de verdad es una imagen. */
    static byte[] sinMetadatos(byte[] archivo, String tipo) {
        try (ImageInputStream entrada = ImageIO.createImageInputStream(new ByteArrayInputStream(archivo))) {
            Iterator<ImageReader> lectores = ImageIO.getImageReaders(entrada);
            if (!lectores.hasNext()) {
                throw new SolicitudInvalidaException("El archivo no es una imagen válida");
            }
            ImageReader lector = lectores.next();
            try {
                lector.setInput(entrada, true, true);
                int ancho = lector.getWidth(0);
                int alto = lector.getHeight(0);
                if (ancho > LADO_MAXIMO || alto > LADO_MAXIMO || (long) ancho * alto > PIXELES_MAXIMOS) {
                    throw new SolicitudInvalidaException("La imagen es demasiado grande en píxeles");
                }
                BufferedImage imagen = lector.read(0);
                boolean jpeg = tipo.equals("image/jpeg");
                if (jpeg && imagen.getType() != BufferedImage.TYPE_INT_RGB) {
                    BufferedImage rgb = new BufferedImage(imagen.getWidth(), imagen.getHeight(), BufferedImage.TYPE_INT_RGB);
                    Graphics2D g = rgb.createGraphics();
                    g.drawImage(imagen, 0, 0, java.awt.Color.WHITE, null);
                    g.dispose();
                    imagen = rgb;
                }
                ByteArrayOutputStream salida = new ByteArrayOutputStream();
                if (!ImageIO.write(imagen, jpeg ? "jpg" : "png", salida)) {
                    throw new SolicitudInvalidaException("No se pudo procesar la imagen");
                }
                return salida.toByteArray();
            } finally {
                lector.dispose();
            }
        } catch (IOException | RuntimeException e) {
            if (e instanceof SolicitudInvalidaException s) {
                throw s;
            }
            throw new SolicitudInvalidaException("El archivo no es una imagen válida");
        }
    }

    /** Un nombre corto y sin rutas ni caracteres raros, con la extensión que corresponde al tipo real. */
    static String nombreSeguro(String original, String tipo) {
        String base = original == null ? "" : original.replace('\\', '/');
        base = base.substring(base.lastIndexOf('/') + 1);
        int punto = base.lastIndexOf('.');
        if (punto > 0) {
            base = base.substring(0, punto);
        }
        base = base.replaceAll("[^A-Za-z0-9 _-]", "_").trim();
        if (base.isEmpty()) {
            base = "imagen";
        }
        if (base.length() > 90) {
            base = base.substring(0, 90);
        }
        return base + (tipo.equals("image/png") ? ".png" : ".jpg");
    }
}
