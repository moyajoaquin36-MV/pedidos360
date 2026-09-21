import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { map } from 'rxjs';
import { Rol } from './roles';
import { RolesService } from './roles.service';

/** Solo deja entrar a /admin a quien tenga el rol ADMIN_GENERAL; al resto lo devuelve a /home. */
export const adminGuard: CanActivateFn = () => {
  const router = inject(Router);
  return inject(RolesService)
    .cargar()
    .pipe(map((roles) => (roles.includes(Rol.AdminGeneral) ? true : router.createUrlTree(['/home']))));
};
