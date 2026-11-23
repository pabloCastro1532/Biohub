import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService, Rol } from './auth.service';

/** Exige sesión iniciada; si no, manda al login. */
export const authGuard: CanActivateFn = () => {
  const auth = inject(AuthService);
  const router = inject(Router);
  return auth.estaAutenticado() ? true : router.createUrlTree(['/login']);
};

/** Exige uno de los roles dados. Se usa en rutas: canActivate: [rolGuard('DIRECTOR', 'ADMINISTRADOR')] */
export const rolGuard =
  (...roles: Rol[]): CanActivateFn =>
  () => {
    const auth = inject(AuthService);
    const router = inject(Router);
    if (!auth.estaAutenticado()) {
      return router.createUrlTree(['/login']);
    }
    return auth.tieneRol(...roles) ? true : router.createUrlTree(['/sin-permiso']);
  };
