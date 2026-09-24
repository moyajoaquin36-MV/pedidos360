import { Injectable } from '@angular/core';
import { MsalService } from '@azure/msal-angular';
import { BehaviorSubject, Observable, catchError, map, of, shareReplay, tap } from 'rxjs';
import { environment } from '../../environments/environment';
import { Rol, decodificarPayloadJwt, obtenerRoles } from './roles';

/**
 * Lee los roles del access token de la API (claim "roles") una sola vez y los
 * comparte con el header, la pagina Home y el guard de administrador.
 */
@Injectable({ providedIn: 'root' })
export class RolesService {
  private readonly rolesSubject = new BehaviorSubject<string[]>([]);
  readonly roles$ = this.rolesSubject.asObservable();
  readonly esAdmin$ = this.roles$.pipe(map((roles) => roles.includes(Rol.AdminGeneral)));

  private cargado?: Observable<string[]>;

  constructor(private readonly msalService: MsalService) {}

  cargar(): Observable<string[]> {
    const cuenta = this.msalService.instance.getActiveAccount();
    if (!cuenta) {
      return of([]);
    }
    if (!this.cargado) {
      this.cargado = this.msalService
        .acquireTokenSilent({ scopes: environment.azureAd.apiScopes, account: cuenta })
        .pipe(
          map((resultado) => obtenerRoles(decodificarPayloadJwt(resultado.accessToken))),
          // Un usuario autoregistrado no trae app roles: se le trata como cliente (igual que el backend).
          map((roles) => (roles.length > 0 ? roles : [Rol.Cliente])),
          catchError(() => of([] as string[])),
          tap((roles) => this.rolesSubject.next(roles)),
          shareReplay(1)
        );
    }
    return this.cargado;
  }
}
