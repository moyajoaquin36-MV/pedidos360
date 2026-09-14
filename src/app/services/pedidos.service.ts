import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';

export interface Pedido {
  id: number;
  localId: string;
  clienteId: string;
  estado: string;
  modalidadEntrega: string;
  total: number;
  fechaCreacion: string;
}

/**
 * Todas las llamadas van al API Gateway de AWS (no directo al microservicio).
 * El MsalInterceptor adjunta el access token automaticamente porque la URL
 * matchea el protectedResourceMap configurado en auth/msal.config.ts.
 */
@Injectable({ providedIn: 'root' })
export class PedidosService {
  private readonly baseUrl = `${environment.apiGatewayBaseUrl}${environment.api.pedidos}`;

  constructor(private readonly http: HttpClient) {}

  listar(): Observable<Pedido[]> {
    return this.http.get<Pedido[]>(this.baseUrl);
  }
}
