import {
  IPublicClientApplication,
  InteractionType,
  LogLevel,
  PublicClientApplication
} from '@azure/msal-browser';
import {
  MsalGuardConfiguration,
  MsalInterceptorConfiguration
} from '@azure/msal-angular';
import { environment } from '../../environments/environment';

/**
 * Instancia de MSAL apuntando al tenant de Azure AD (Entra ID) del proyecto.
 * authority = https://login.microsoftonline.com/{tenantId} restringe el
 * login a cuentas de ese tenant especifico (single-tenant), acorde al caso
 * (login corporativo con Azure AD).
 */
export function MSALInstanceFactory(): IPublicClientApplication {
  return new PublicClientApplication({
    auth: {
      clientId: environment.azureAd.clientId,
      authority: `https://login.microsoftonline.com/${environment.azureAd.tenantId}`,
      redirectUri: environment.azureAd.redirectUri,
      postLogoutRedirectUri: environment.azureAd.postLogoutRedirectUri
    },
    cache: {
      cacheLocation: 'sessionStorage',
      storeAuthStateInCookie: false
    },
    system: {
      loggerOptions: {
        loggerCallback: (level: LogLevel, message: string) => {
          if (!environment.production) {
            console.debug('[MSAL]', message);
          }
        },
        logLevel: LogLevel.Warning
      }
    }
  });
}

/** El MsalGuard exige login (redirect) para cualquier ruta que lo use. */
export function MSALGuardConfigFactory(): MsalGuardConfiguration {
  return {
    interactionType: InteractionType.Redirect,
    authRequest: {
      scopes: environment.azureAd.apiScopes
    }
  };
}

/**
 * Mapa de recursos protegidos: el MsalInterceptor adjunta automaticamente
 * el access token (con estos scopes) a cualquier request HTTP cuya URL
 * matchee una de estas entradas. Todo pasa por AWS API Gateway.
 */
export function MSALInterceptorConfigFactory(): MsalInterceptorConfiguration {
  const protectedResourceMap = new Map<string, Array<string> | null>();
  protectedResourceMap.set(`${environment.apiGatewayBaseUrl}/*`, environment.azureAd.apiScopes);

  return {
    interactionType: InteractionType.Redirect,
    protectedResourceMap
  };
}
