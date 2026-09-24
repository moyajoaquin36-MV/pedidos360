import { APP_INITIALIZER, ApplicationConfig, provideZoneChangeDetection } from '@angular/core';
import { provideRouter, withInMemoryScrolling } from '@angular/router';
import { HTTP_INTERCEPTORS, provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';
import { IPublicClientApplication } from '@azure/msal-browser';
import {
  MSAL_GUARD_CONFIG,
  MSAL_INSTANCE,
  MSAL_INTERCEPTOR_CONFIG,
  MsalBroadcastService,
  MsalGuard,
  MsalInterceptor,
  MsalService
} from '@azure/msal-angular';

import { routes } from './app.routes';
import { MSALGuardConfigFactory, MSALInstanceFactory, MSALInterceptorConfigFactory } from './auth/msal.config';

/**
 * MSAL v3 exige inicializar la instancia (y resolver la promesa del
 * redirect de login) antes de que cualquier componente/guard la use.
 *
 * Ademas, MSAL nunca marca una cuenta como "activa" automaticamente: hay
 * que hacerlo a mano tras el login (o al recargar la pagina, tomando la
 * primera cuenta ya presente en cache). Sin esto, getActiveAccount()
 * siempre devuelve null y la pantalla de login nunca avanza a /home.
 */
function initializeMsal(msalInstance: IPublicClientApplication) {
  return async () => {
    await msalInstance.initialize();
    const result = await msalInstance.handleRedirectPromise();

    if (result?.account) {
      msalInstance.setActiveAccount(result.account);
    } else if (!msalInstance.getActiveAccount()) {
      const [firstAccount] = msalInstance.getAllAccounts();
      if (firstAccount) {
        msalInstance.setActiveAccount(firstAccount);
      }
    }
  };
}

export const appConfig: ApplicationConfig = {
  providers: [
    provideZoneChangeDetection({ eventCoalescing: true }),
    provideRouter(routes, withInMemoryScrolling({ anchorScrolling: 'enabled', scrollPositionRestoration: 'enabled' })),
    provideHttpClient(withInterceptorsFromDi()),
    {
      provide: HTTP_INTERCEPTORS,
      useClass: MsalInterceptor,
      multi: true
    },
    {
      provide: MSAL_INSTANCE,
      useFactory: MSALInstanceFactory
    },
    {
      provide: MSAL_GUARD_CONFIG,
      useFactory: MSALGuardConfigFactory
    },
    {
      provide: MSAL_INTERCEPTOR_CONFIG,
      useFactory: MSALInterceptorConfigFactory
    },
    MsalService,
    MsalGuard,
    MsalBroadcastService,
    {
      provide: APP_INITIALIZER,
      useFactory: initializeMsal,
      deps: [MSAL_INSTANCE],
      multi: true
    }
  ]
};
