import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, throwError } from 'rxjs';
import { AuthService } from './auth.service';

/** Agrega el token a las llamadas a /api y cierra la sesión si el backend responde 401. */
export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const auth = inject(AuthService);
  const token = auth.token();
  const esApi = req.url.startsWith('/api');
  const esLogin = req.url.startsWith('/api/auth/login');

  const peticion = token && esApi ? req.clone({ setHeaders: { Authorization: `Bearer ${token}` } }) : req;

  return next(peticion).pipe(
    catchError((error: HttpErrorResponse) => {
      // 401 = sin token, vencido o sesión revocada (HU-27). 403 = rol sin permiso: no se cierra la sesión.
      if (error.status === 401 && esApi && !esLogin) {
        auth.logout();
      }
      return throwError(() => error);
    })
  );
};
