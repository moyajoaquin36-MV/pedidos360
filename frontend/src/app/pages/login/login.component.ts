import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { MsalService } from '@azure/msal-angular';

@Component({
  selector: 'app-login',
  standalone: true,
  templateUrl: './login.component.html'
})
export class LoginComponent implements OnInit {
  constructor(
    private readonly msalService: MsalService,
    private readonly router: Router
  ) {}

  ngOnInit(): void {
    if (this.msalService.instance.getActiveAccount()) {
      this.router.navigateByUrl('/home');
    }
  }

  iniciarSesion(): void {
    this.msalService.loginRedirect();
  }
}
