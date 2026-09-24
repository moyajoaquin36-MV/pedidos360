import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { catchError, of } from 'rxjs';
import { LOCALES } from '../../data/locales';
import { Pedido, PedidosService } from '../../services/pedidos.service';
import { Producto, ProductosService } from '../../services/productos.service';

/** Panel exclusivo del rol ADMIN_GENERAL (protegido por MsalGuard + adminGuard). */
@Component({
  selector: 'app-admin',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './admin.component.html'
})
export class AdminComponent implements OnInit {
  readonly locales = LOCALES;
  readonly estados = ['RECIBIDO', 'EN_PREPARACION', 'LISTO', 'ENTREGADO', 'CANCELADO'];
  readonly umbralStockBajo = 10;

  productos: Producto[] = [];
  pedidos: Pedido[] = [];

  filtroLocal = 'todos';
  nuevo = { localId: 'local-01', nombre: '', precio: 1000, stock: 10 };
  reposicion: Record<number, number> = {};

  mensaje: string | null = null;
  error: string | null = null;

  constructor(
    private readonly productosService: ProductosService,
    private readonly pedidosService: PedidosService
  ) {}

  ngOnInit(): void {
    this.cargarProductos();
    this.cargarPedidos();
  }

  // ---- Resumen ----
  get totalVentas(): number {
    return this.pedidos.filter((p) => p.estado !== 'CANCELADO').reduce((acc, p) => acc + p.total, 0);
  }

  get pedidosPorEstado(): { estado: string; cantidad: number }[] {
    return this.estados.map((estado) => ({
      estado,
      cantidad: this.pedidos.filter((p) => p.estado === estado).length
    }));
  }

  get productosStockBajo(): Producto[] {
    return this.productos.filter((p) => p.stock < this.umbralStockBajo);
  }

  get productosFiltrados(): Producto[] {
    return this.filtroLocal === 'todos'
      ? this.productos
      : this.productos.filter((p) => p.localId === this.filtroLocal);
  }

  nombreLocal(localId: string): string {
    return this.locales.find((l) => l.id === localId)?.nombre ?? localId;
  }

  // ---- Carga ----
  cargarProductos(): void {
    this.productosService
      .listar()
      .pipe(catchError(() => of([] as Producto[])))
      .subscribe((productos) => (this.productos = productos));
  }

  cargarPedidos(): void {
    this.pedidosService
      .listar()
      .pipe(catchError(() => of([] as Pedido[])))
      .subscribe((pedidos) => (this.pedidos = pedidos));
  }

  // ---- Acciones ----
  agregarProducto(): void {
    this.limpiarMensajes();
    this.productosService
      .crear({
        localId: this.nuevo.localId,
        sku: `${this.nuevo.localId}-${Date.now()}`,
        nombre: this.nuevo.nombre,
        precio: this.nuevo.precio,
        stock: this.nuevo.stock
      })
      .pipe(
        catchError((e) => {
          this.error = `No se pudo crear el producto (${e.status ?? 'sin conexion'}).`;
          return of(null);
        })
      )
      .subscribe((producto) => {
        if (producto) {
          this.mensaje = `Producto "${producto.nombre}" agregado a ${this.nombreLocal(producto.localId)}.`;
          this.nuevo.nombre = '';
          this.cargarProductos();
        }
      });
  }

  reponer(producto: Producto): void {
    const cantidad = this.reposicion[producto.id];
    if (!cantidad || cantidad < 1) {
      return;
    }
    this.limpiarMensajes();
    this.productosService
      .reponer(producto.id, cantidad)
      .pipe(
        catchError((e) => {
          this.error = `No se pudo reponer el stock (${e.status ?? 'sin conexion'}).`;
          return of(null);
        })
      )
      .subscribe((actualizado) => {
        if (actualizado) {
          this.mensaje = `Stock de "${actualizado.nombre}" actualizado: ${actualizado.stock} unidades.`;
          this.reposicion[producto.id] = 0;
          this.cargarProductos();
        }
      });
  }

  cambiarEstado(pedido: Pedido, estado: string): void {
    this.limpiarMensajes();
    this.pedidosService
      .cambiarEstado(pedido.id, estado)
      .pipe(
        catchError((e) => {
          this.error = `No se pudo cambiar el estado (${e.status ?? 'sin conexion'}).`;
          return of(null);
        })
      )
      .subscribe((actualizado) => {
        if (actualizado) {
          this.mensaje = `Pedido #${pedido.id} ahora esta ${estado}.`;
        }
        this.cargarPedidos();
      });
  }

  private limpiarMensajes(): void {
    this.mensaje = null;
    this.error = null;
  }
}
