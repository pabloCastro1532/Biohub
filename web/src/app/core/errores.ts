import { HttpErrorResponse } from '@angular/common/http';

/** Texto para mostrar al usuario a partir de un error HTTP. El backend manda {"error": "mensaje"} en 400, 404 y 409. */
export function mensajeDeError(e: unknown): string {
  if (e instanceof HttpErrorResponse) {
    const cuerpo = e.error as { error?: unknown } | null;
    if (cuerpo && typeof cuerpo === 'object' && typeof cuerpo.error === 'string') {
      return cuerpo.error;
    }
    switch (e.status) {
      case 0:
        return 'No se pudo conectar con el servidor.';
      case 400:
        return 'Datos incompletos o mal formados.';
      case 403:
        return 'Tu rol no tiene permiso para esta acción.';
      case 404:
        return 'No se encontró lo que buscabas.';
      case 409:
        return 'Esa acción choca con el estado actual.';
    }
  }
  return 'Ocurrió un error inesperado.';
}
