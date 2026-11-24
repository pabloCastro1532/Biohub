package co.biohub.cientifico.servicio;

import co.biohub.cientifico.repositorio.HallazgoRepository;
import co.biohub.cientifico.repositorio.ImagenHallazgoJdbc;
import co.biohub.cientifico.repositorio.UsuarioConsulta;
import co.biohub.cientifico.repositorio.UsuarioConsulta.UsuarioBasico;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Imágenes de un hallazgo: solo JPG y PNG reales, con límites, y sin metadatos (no pueden llevar el GPS de la foto). */
class ImagenHallazgoServiceTest {

    private HallazgoRepository hallazgos;
    private ImagenHallazgoJdbc imagenes;
    private ImagenHallazgoService servicio;

    @BeforeEach
    void preparar() {
        hallazgos = mock(HallazgoRepository.class);
        imagenes = mock(ImagenHallazgoJdbc.class);
        UsuarioConsulta usuarios = mock(UsuarioConsulta.class);
        servicio = new ImagenHallazgoService(hallazgos, imagenes, usuarios);

        when(usuarios.porNombre("investigador"))
                .thenReturn(Optional.of(new UsuarioBasico(7L, "investigador", "INVESTIGADOR", true)));
        when(hallazgos.existsById(50L)).thenReturn(true);
        when(imagenes.contar(50L)).thenReturn(0);
    }

    private static byte[] imagen(String formato) throws Exception {
        BufferedImage img = new BufferedImage(40, 30, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream salida = new ByteArrayOutputStream();
        ImageIO.write(img, formato, salida);
        return salida.toByteArray();
    }

    /** Un JPG al que se le agrega un bloque Exif, como el que dejan las cámaras con la posición GPS. */
    private static byte[] jpgConExif() throws Exception {
        byte[] jpg = imagen("jpg");
        byte[] exif = "Exif\0\0GPS-4.6N-74.1W".getBytes(StandardCharsets.ISO_8859_1);
        int largo = exif.length + 2;
        ByteArrayOutputStream salida = new ByteArrayOutputStream();
        salida.write(jpg, 0, 2);                       // SOI
        salida.write(new byte[] {(byte) 0xFF, (byte) 0xE1, (byte) (largo >> 8), (byte) largo});
        salida.write(exif);
        salida.write(jpg, 2, jpg.length - 2);
        return salida.toByteArray();
    }

    private static boolean contiene(byte[] datos, String texto) {
        return new String(datos, StandardCharsets.ISO_8859_1).contains(texto);
    }

    @Test
    void guarda_un_jpg_y_un_png_con_su_tipo_y_su_autor() throws Exception {
        servicio.subir(50L, "ave.jpg", imagen("jpg"), "investigador");
        servicio.subir(50L, "huella.png", imagen("png"), "investigador");

        ArgumentCaptor<String> tipo = ArgumentCaptor.forClass(String.class);
        verify(imagenes, org.mockito.Mockito.times(2))
                .guardar(any(), anyString(), tipo.capture(), any(byte[].class), any());
        assertEquals(List.of("image/jpeg", "image/png"), tipo.getAllValues());
    }

    @Test
    void asr4_la_imagen_se_guarda_sin_exif_ni_gps() throws Exception {
        byte[] original = jpgConExif();
        assertTrue(contiene(original, "GPS-4.6N"), "la imagen de prueba debe traer la posición");

        servicio.subir(50L, "foto.jpg", original, "investigador");

        ArgumentCaptor<byte[]> guardado = ArgumentCaptor.forClass(byte[].class);
        verify(imagenes).guardar(any(), anyString(), anyString(), guardado.capture(), any());
        assertFalse(contiene(guardado.getValue(), "Exif"));
        assertFalse(contiene(guardado.getValue(), "GPS-4.6N"));
        assertNotNull(ImageIO.read(new ByteArrayInputStream(guardado.getValue())), "sigue siendo una imagen");
    }

    @Test
    void el_tipo_sale_del_contenido_y_no_del_nombre() {
        byte[] texto = "esto es un texto".getBytes(StandardCharsets.UTF_8);
        SolicitudInvalidaException e = assertThrows(SolicitudInvalidaException.class,
                () -> servicio.subir(50L, "falsa.jpg", texto, "investigador"));
        assertTrue(e.getMessage().contains("JPG o PNG"));
        verify(imagenes, never()).guardar(any(), anyString(), anyString(), any(byte[].class), any());
    }

    @Test
    void rechaza_un_archivo_que_empieza_como_jpg_pero_no_es_una_imagen() {
        byte[] falso = new byte[300];
        falso[0] = (byte) 0xFF;
        falso[1] = (byte) 0xD8;
        falso[2] = (byte) 0xFF;
        SolicitudInvalidaException e = assertThrows(SolicitudInvalidaException.class,
                () -> servicio.subir(50L, "rota.jpg", falso, "investigador"));
        assertTrue(e.getMessage().contains("no es una imagen válida"));
        verify(imagenes, never()).guardar(any(), anyString(), anyString(), any(byte[].class), any());
    }

    @Test
    void rechaza_un_archivo_vacio() {
        assertThrows(SolicitudInvalidaException.class, () -> servicio.subir(50L, "a.jpg", new byte[0], "investigador"));
    }

    @Test
    void rechaza_mas_de_2_mb() {
        byte[] grande = new byte[ImagenHallazgoService.TAMANO_MAXIMO + 1];
        SolicitudInvalidaException e = assertThrows(SolicitudInvalidaException.class,
                () -> servicio.subir(50L, "grande.jpg", grande, "investigador"));
        assertTrue(e.getMessage().contains("2 MB"));
    }

    @Test
    void permite_hasta_5_imagenes_por_hallazgo() throws Exception {
        when(imagenes.contar(50L)).thenReturn(5);
        byte[] jpg = imagen("jpg");
        SolicitudInvalidaException e = assertThrows(SolicitudInvalidaException.class,
                () -> servicio.subir(50L, "sexta.jpg", jpg, "investigador"));
        assertTrue(e.getMessage().contains("5 imágenes"));
        verify(imagenes, never()).guardar(any(), anyString(), anyString(), any(byte[].class), any());
    }

    @Test
    void un_hallazgo_que_no_existe_da_404() throws Exception {
        byte[] jpg = imagen("jpg");
        assertThrows(RecursoNoEncontradoException.class, () -> servicio.subir(99L, "a.jpg", jpg, "investigador"));
        assertThrows(RecursoNoEncontradoException.class, () -> servicio.listar(99L));
        assertThrows(RecursoNoEncontradoException.class, () -> servicio.contenido(99L, 1L));
    }

    @Test
    void una_imagen_que_no_es_de_ese_hallazgo_da_404() {
        when(imagenes.contenido(50L, 3L)).thenReturn(Optional.empty());
        assertThrows(RecursoNoEncontradoException.class, () -> servicio.contenido(50L, 3L));
    }

    @Test
    void rechaza_si_el_autor_no_existe() throws Exception {
        byte[] jpg = imagen("jpg");
        assertThrows(SolicitudInvalidaException.class, () -> servicio.subir(50L, "a.jpg", jpg, "fantasma"));
        verify(imagenes, never()).guardar(anyLong(), anyString(), anyString(), any(byte[].class), anyLong());
    }

    @Test
    void el_nombre_del_archivo_no_lleva_rutas_ni_caracteres_raros() {
        assertEquals("mi ave_.png", ImagenHallazgoService.nombreSeguro("C:\\fotos\\mi ave*.PNG", "image/png"));
        assertEquals("passwd.jpg", ImagenHallazgoService.nombreSeguro("../../etc/passwd", "image/jpeg"));
        assertEquals("imagen.jpg", ImagenHallazgoService.nombreSeguro(null, "image/jpeg"));
    }
}
