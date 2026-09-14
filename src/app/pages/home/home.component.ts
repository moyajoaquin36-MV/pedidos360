import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { MsalService } from '@azure/msal-angular';
import { AccountInfo } from '@azure/msal-browser';
import { catchError, of } from 'rxjs';
import { obtenerRoles } from '../../auth/roles';
import { Pedido, PedidosService } from '../../services/pedidos.service';

@Component({
  selector: 'app-home',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './home.component.html'
})
export class HomeComponent implements OnInit {
  cuenta: AccountInfo | null = null;
  roles: string[] = [];
  pedidos: Pedido[] = [];
  errorPedidos: string | null = null;
  cargando = true;

  constructor(
    private readonly msalService: MsalService,
    private readonly pedidosService: PedidosService
  ) {}

  ngOnInit(): void {
    this.cuenta = this.msalService.instance.getActiveAccount();
    this.roles = obtenerRoles(this.cuenta?.idTokenClaims as Record<string, unknown>);

    this.pedidosService
      .listar()
      .pipe(
        catchError((error) => {
          this.errorPedidos = `No se pudo consultar ms-pedidos via API Gateway (${error.status ?? 'sin conexion'}). Es esperable hasta que el API Gateway y el tenant esten desplegados.`;
          return of([] as Pedido[]);
        })
      )
      .subscribe((pedidos) => {
        this.pedidos = pedidos;
        this.cargando = false;
      });
  }

  cerrarSesion(): void {
    this.msalService.logoutRedirect();
  }
}
