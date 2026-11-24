package co.biohub.cientifico.servicio;

/** El archivo de una imagen y su tipo (image/jpeg o image/png), listo para devolverse al navegador. */
public record ContenidoImagen(String tipoContenido, byte[] bytes) {}
