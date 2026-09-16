import { CommonModule } from '@angular/common';
import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';
import { MsalService } from '@azure/msal-angular';

@Component({
  selector: 'app-footer',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './footer.component.html'
})
export class FooterComponent {
  constructor(private readonly msalService: MsalService) {}

  get cuenta() {
    return this.msalService.instance.getActiveAccount();
  }

  iniciarSesion(): void {
    this.msalService.loginRedirect();
  }

  cerrarSesion(): void {
    this.msalService.logoutRedirect();
  }
}
