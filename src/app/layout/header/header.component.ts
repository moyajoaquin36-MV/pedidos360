import { CommonModule } from '@angular/common';
import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';
import { MsalService } from '@azure/msal-angular';
import { AccountInfo } from '@azure/msal-browser';
import { RolesService } from '../../auth/roles.service';
import { LOCALES } from '../../data/locales';
import { CartService } from '../../services/cart.service';
import { LocalSeleccionadoService } from '../../services/local-seleccionado.service';

@Component({
  selector: 'app-header',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './header.component.html'
})
export class HeaderComponent {
  readonly locales = LOCALES;
  menuCuentaAbierto = false;
  menuCarritoAbierto = false;

  constructor(
    private readonly msalService: MsalService,
    readonly localSeleccionado: LocalSeleccionadoService,
    readonly carrito: CartService,
    readonly roles: RolesService
  ) {
    this.roles.cargar().subscribe();
  }

  get cuenta(): AccountInfo | null {
    return this.msalService.instance.getActiveAccount();
  }

  seleccionarLocal(localId: string): void {
    this.localSeleccionado.seleccionar(localId);
  }

  iniciarSesion(): void {
    this.msalService.loginRedirect();
  }

  cerrarSesion(): void {
    this.msalService.logoutRedirect();
  }
}
