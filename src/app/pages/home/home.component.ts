import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MsalService } from '@azure/msal-angular';
import { AccountInfo } from '@azure/msal-browser';
import { catchError, of } from 'rxjs';
import { Rol } from '../../auth/roles';
import { RolesService } from '../../auth/roles.service';
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
    private readonly rolesService: RolesService,
    readonly carrito: CartService,
    readonly localSeleccionado: LocalSeleccionadoService
  ) {}

  ngOnInit(): void {
    this.cuenta = this.msalService.instance.getActiveAccount();

    this.rolesService.cargar().subscribe((roles) => (this.roles = roles));

    this.cargarProductos();
    this.cargarPedidos();
  }

  // --- Utilidad de cada rol ---
  get esAdmin(): boolean {
    return this.roles.includes(Rol.AdminGeneral);
  }

  get esOperador(): boolean {
    return this.roles.includes(Rol.OperadorCocina);
  }

  /** Puede comprar: cliente o admin. */
  get puedeComprar(): boolean {
    return this.roles.includes(Rol.Cliente) || this.esAdmin;
  }

  /** Puede cambiar el estado de los pedidos: operador o admin. */
  get puedeGestionarPedidos(): boolean {
    return this.esOperador || this.esAdmin;
  }

  get tituloPedidos(): string {
    return this.puedeGestionarPedidos ? 'Pedidos de la red' : 'Mis pedidos';
  }

  siguienteEstado(estado: string): string | null {
    const flujo: Record<string, string> = {
      RECIBIDO: 'EN_PREPARACION',
      EN_PREPARACION: 'LISTO',
      LISTO: 'ENTREGADO'
    };
    return flujo[estado] ?? null;
  }

  avanzarPedido(pedido: Pedido): void {
    const siguiente = this.siguienteEstado(pedido.estado);
    if (siguiente) {
      this.cambiarEstado(pedido, siguiente);
    }
  }

  cambiarEstado(pedido: Pedido, estado: string): void {
    this.pedidosService
      .cambiarEstado(pedido.id, estado)
      .pipe(
        catchError((error) => {
          this.errorPedidos = `No se pudo cambiar el estado (${error.status ?? 'sin conexion'}).`;
          return of(null);
        })
      )
      .subscribe((actualizado) => {
        if (actualizado) {
          this.cargarPedidos();
        }
      });
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
