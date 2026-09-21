import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MsalService } from '@azure/msal-angular';
import { AccountInfo } from '@azure/msal-browser';
import { catchError, of } from 'rxjs';
import { environment } from '../../../environments/environment';
import { decodificarPayloadJwt, obtenerRoles } from '../../auth/roles';
import { LOCALES } from '../../data/locales';
import { CartService } from '../../services/cart.service';
import { LocalSeleccionadoService } from '../../services/local-seleccionado.service';
import { Pedido, PedidosService } from '../../services/pedidos.service';
import { Producto, ProductosService } from '../../services/productos.service';

@Component({
  selector: 'app-home',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './home.component.html'
})
export class HomeComponent implements OnInit {
  readonly locales = LOCALES;

  cuenta: AccountInfo | null = null;
  roles: string[] = [];

  productos: Producto[] = [];
  cargandoProductos = true;
  errorProductos: string | null = null;

  pedidos: Pedido[] = [];
  cargandoPedidos = true;
  errorPedidos: string | null = null;

  modalidadEntrega: 'DELIVERY' | 'RETIRO_EN_TIENDA' = 'RETIRO_EN_TIENDA';
  creandoPedido = false;
  errorCrearPedido: string | null = null;

  constructor(
    private readonly msalService: MsalService,
    private readonly pedidosService: PedidosService,
    private readonly productosService: ProductosService,
    readonly carrito: CartService,
    readonly localSeleccionado: LocalSeleccionadoService
  ) {}

  ngOnInit(): void {
    this.cuenta = this.msalService.instance.getActiveAccount();

    // Los roles se leen del access token de la API (su audience es la API,
    // que es donde estan definidos y asignados los app roles), no del ID
    // token (cuya audience es la propia SPA).
    this.msalService
      .acquireTokenSilent({ scopes: environment.azureAd.apiScopes, account: this.cuenta ?? undefined })
      .subscribe({
        next: (result) => {
          const claims = decodificarPayloadJwt(result.accessToken);
          this.roles = obtenerRoles(claims);
        },
        error: () => {
          this.roles = [];
        }
      });

    this.cargarProductos();
    this.cargarPedidos();
  }

  get nombreLocalSeleccionado(): string {
    return this.locales.find((local) => local.id === this.localSeleccionado.actual)?.nombre ?? '';
  }

  get productosDelLocal(): Producto[] {
    return this.productos.filter((producto) => producto.localId === this.localSeleccionado.actual);
  }

  cargarProductos(): void {
    this.cargandoProductos = true;
    this.productosService
      .listar()
      .pipe(
        catchError((error) => {
          this.errorProductos = `No se pudo consultar ms-productos (${error.status ?? 'sin conexion'}).`;
          return of([] as Producto[]);
        })
      )
      .subscribe((productos) => {
        this.productos = productos;
        this.cargandoProductos = false;
      });
  }

  cargarPedidos(): void {
    this.cargandoPedidos = true;
    this.pedidosService
      .listar()
      .pipe(
        catchError((error) => {
          this.errorPedidos = `No se pudo consultar ms-pedidos (${error.status ?? 'sin conexion'}).`;
          return of([] as Pedido[]);
        })
      )
      .subscribe((pedidos) => {
        this.pedidos = pedidos;
        this.cargandoPedidos = false;
      });
  }

  agregarAlCarrito(producto: Producto): void {
    this.carrito.agregar(producto);
  }

  confirmarPedido(): void {
    const itemsCarrito = this.carrito.items;
    if (itemsCarrito.length === 0) {
      return;
    }

    this.creandoPedido = true;
    this.errorCrearPedido = null;

    this.pedidosService
      .crear({
        localId: this.carrito.localIdActual ?? this.localSeleccionado.actual,
        modalidadEntrega: this.modalidadEntrega,
        items: itemsCarrito.map((item) => ({
          productoId: item.producto.id,
          nombreProducto: item.producto.nombre,
          cantidad: item.cantidad,
          precioUnitario: item.producto.precio
        }))
      })
      .pipe(
        catchError((error) => {
          this.errorCrearPedido = `No se pudo crear el pedido (${error.status ?? 'sin conexion'}): ${error.error?.mensaje ?? ''}`;
          return of(null);
        })
      )
      .subscribe((pedido) => {
        this.creandoPedido = false;
        if (pedido) {
          this.carrito.limpiar();
          this.cargarPedidos();
        }
        // Tras un pedido (o un rechazo por stock) se refresca el catalogo para ver el stock real.
        this.cargarProductos();
      });
  }
}
