import { Injectable } from '@angular/core';
import { MsalBroadcastService, MsalService } from '@azure/msal-angular';
import { InteractionStatus } from '@azure/msal-browser';
import { BehaviorSubject, Observable, catchError, filter, map, of, shareReplay, tap } from 'rxjs';
import { environment } from '../../environments/environment';
import { Rol, decodificarPayloadJwt, obtenerRoles } from './roles';

/**
 * Lee los roles del access token de la API (claim "roles") y los comparte con
 * el header, la pagina Home y el guard de administrador.
 */
@Injectable({ providedIn: 'root' })
export class RolesService {
  private readonly rolesSubject = new BehaviorSubject<string[]>([]);
  readonly roles$ = this.rolesSubject.asObservable();
  readonly esAdmin$ = this.roles$.pipe(map((roles) => roles.includes(Rol.AdminGeneral)));

  private cargado?: Observable<string[]>;
  private cuentaCargada?: string;

  constructor(
    private readonly msalService: MsalService,
    msalBroadcast: MsalBroadcastService
  ) {
    // Cuando MSAL termina un login (o cualquier interaccion), se vuelven a leer
    // los roles: asi el header se actualiza sin tener que recargar la pagina.
    msalBroadcast.inProgress$
      .pipe(filter((estado) => estado === InteractionStatus.None))
      .subscribe(() => this.cargar().subscribe());
  }

  cargar(): Observable<string[]> {
    const cuenta = this.msalService.instance.getActiveAccount();
    if (!cuenta) {
      return of([]);
    }
    if (!this.cargado || this.cuentaCargada !== cuenta.homeAccountId) {
      this.cuentaCargada = cuenta.homeAccountId;
      this.cargado = this.msalService
        .acquireTokenSilent({ scopes: environment.azureAd.apiScopes, account: cuenta })
        .pipe(
          map((resultado) => obtenerRoles(decodificarPayloadJwt(resultado.accessToken))),
          // Un usuario autoregistrado no trae app roles: se le trata como cliente (igual que el backend).
          map((roles) => (roles.length > 0 ? roles : [Rol.Cliente])),
          catchError(() => {
            // No se guarda el fallo: el siguiente intento vuelve a pedir el token.
            this.cargado = undefined;
            return of([] as string[]);
          }),
          tap((roles) => this.rolesSubject.next(roles)),
          shareReplay(1)
        );
    }
    return this.cargado;
  }
}
