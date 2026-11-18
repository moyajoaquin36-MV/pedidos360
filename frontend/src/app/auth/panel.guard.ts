import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { map } from 'rxjs';
import { puedeVerPanel } from './roles';
import { RolesService } from './roles.service';

/** Solo deja entrar a /panel a ADMIN_GENERAL u OPERADOR_COCINA; al resto lo devuelve a /home. */
export const panelGuard: CanActivateFn = () => {
  const router = inject(Router);
  return inject(RolesService)
    .cargar()
    .pipe(map((roles) => (puedeVerPanel(roles) ? true : router.createUrlTree(['/home']))));
};
