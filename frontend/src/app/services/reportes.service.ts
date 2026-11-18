import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';

export interface ProductoTop {
  productoId: number;
  nombre: string;
  unidades: number;
}

export interface StockCritico {
  productoId: number;
  nombre: string;
  localId: string;
  stock: number;
}

export interface EventoReciente {
  eventId: string;
  tipo: string;
  /** ISO-8601 */
  fecha: string;
  detalle: string;
}

/** Respuesta de GET /api/reportes/metricas (ms-reportes, calculada desde los topicos Kafka). */
export interface Metricas {
  totalPedidos: number;
  ventasTotales: number;
  /** estado del pedido -> cantidad */
  pedidosPorEstado: Record<string, number>;
  /** localId -> ventas (CLP) */
  ventasPorLocal: Record<string, number>;
  topProductos: ProductoTop[];
  stockCritico: StockCritico[];
  ultimosEventos: EventoReciente[];
  eventosProcesados: number;
  /** ISO-8601 */
  actualizado: string;
}

/** Va por el API Gateway; el MsalInterceptor adjunta el token (protectedResourceMap). */
@Injectable({ providedIn: 'root' })
export class ReportesService {
  private readonly baseUrl = environment.reportesBaseUrl;

  constructor(private readonly http: HttpClient) {}

  metricas(): Observable<Metricas> {
    return this.http.get<Metricas>(`${this.baseUrl}/metricas`);
  }
}
