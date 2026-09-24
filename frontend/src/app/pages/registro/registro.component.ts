import { Component, OnInit } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { MsalService } from '@azure/msal-angular';
import { environment } from '../../../environments/environment';

/**
 * Pantalla de registro: el usuario crea su propia cuenta en el tenant de
 * Azure AD mediante el flujo de usuario de autoregistro. prompt=create abre
 * directamente la pantalla "Crear cuenta" de Microsoft (OIDC Authorization
 * Code con PKCE, igual que el login). Al terminar vuelve autenticado y entra
 * como CLIENTE.
 */
@Component({
  selector: 'app-registro',
  standalone: true,
  imports: [RouterLink],
  templateUrl: './registro.component.html'
})
export class RegistroComponent implements OnInit {
  constructor(
    private readonly msalService: MsalService,
    private readonly router: Router
  ) {}

  ngOnInit(): void {
    if (this.msalService.instance.getActiveAccount()) {
      this.router.navigateByUrl('/home');
    }
  }

  crearCuenta(): void {
    this.msalService.loginRedirect({
      scopes: environment.azureAd.apiScopes,
      prompt: 'create'
    });
  }
}
