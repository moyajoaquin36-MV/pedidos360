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

export interface ItemPedidoRequest {
  productoId: number;
  nombreProducto: string;
  cantidad: number;
  precioUnitario: number;
}

export interface CrearPedidoRequest {
  localId: string;
  modalidadEntrega: 'DELIVERY' | 'RETIRO_EN_TIENDA';
  items: ItemPedidoRequest[];
}

/**
 * Todas las llamadas van al API Gateway de AWS (no directo al microservicio).
 * El MsalInterceptor adjunta el access token automaticamente porque la URL
 * matchea el protectedResourceMap configurado en auth/msal.config.ts.
 */
@Injectable({ providedIn: 'root' })
export class PedidosService {
  private readonly baseUrl = environment.pedidosBaseUrl;

  constructor(private readonly http: HttpClient) {}

  listar(): Observable<Pedido[]> {
    return this.http.get<Pedido[]>(this.baseUrl);
  }

  crear(request: CrearPedidoRequest): Observable<Pedido> {
    return this.http.post<Pedido>(this.baseUrl, request);
  }

  cambiarEstado(id: number, estado: string): Observable<Pedido> {
    return this.http.patch<Pedido>(`${this.baseUrl}/${id}/estado`, { estado });
  }
}
