import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';

export interface Producto {
  id: number;
  localId: string;
  sku: string;
  nombre: string;
  precio: number;
  stock: number;
}

export interface CrearProductoRequest {
  localId: string;
  sku: string;
  nombre: string;
  precio: number;
  stock: number;
}

@Injectable({ providedIn: 'root' })
export class ProductosService {
  private readonly baseUrl = environment.productosBaseUrl;

  constructor(private readonly http: HttpClient) {}

  listar(): Observable<Producto[]> {
    return this.http.get<Producto[]>(this.baseUrl);
  }

  crear(request: CrearProductoRequest): Observable<Producto> {
    return this.http.post<Producto>(this.baseUrl, request);
  }
}
