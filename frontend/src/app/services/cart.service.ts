import { Injectable } from '@angular/core';
import { BehaviorSubject } from 'rxjs';
import { Producto } from './productos.service';

export interface ItemCarrito {
  producto: Producto;
  cantidad: number;
}

/**
 * Carrito en memoria (no persiste entre recargas). Un pedido pertenece a un
 * solo local, asi que si se agrega un producto de un local distinto al que
 * ya hay en el carrito, este se vacia primero (igual que en la mayoria de
 * apps de pedidos de comida).
 */
@Injectable({ providedIn: 'root' })
export class CartService {
  private readonly itemsSubject = new BehaviorSubject<ItemCarrito[]>([]);
  readonly items$ = this.itemsSubject.asObservable();

  private readonly cambioLocalSubject = new BehaviorSubject<boolean>(false);
  /** Emite true una vez cuando agregar un producto obligo a vaciar el carrito anterior. */
  readonly cambioLocal$ = this.cambioLocalSubject.asObservable();

  get items(): ItemCarrito[] {
    return this.itemsSubject.value;
  }

  get localIdActual(): string | null {
    return this.itemsSubject.value[0]?.producto.localId ?? null;
  }

  agregar(producto: Producto): void {
    const actuales = this.itemsSubject.value;
    const localActual = this.localIdActual;

    if (localActual && localActual !== producto.localId) {
      this.cambioLocalSubject.next(true);
      this.itemsSubject.next([{ producto, cantidad: 1 }]);
      return;
    }

    const existente = actuales.find((item) => item.producto.id === producto.id);
    if (existente) {
      existente.cantidad += 1;
      this.itemsSubject.next([...actuales]);
    } else {
      this.itemsSubject.next([...actuales, { producto, cantidad: 1 }]);
    }
  }

  quitar(productoId: number): void {
    this.itemsSubject.next(this.itemsSubject.value.filter((item) => item.producto.id !== productoId));
  }

  actualizarCantidad(productoId: number, cantidad: number): void {
    if (cantidad < 1) {
      this.quitar(productoId);
      return;
    }
    this.itemsSubject.next(
      this.itemsSubject.value.map((item) => (item.producto.id === productoId ? { ...item, cantidad } : item))
    );
  }

  limpiar(): void {
    this.itemsSubject.next([]);
  }

  get totalItems(): number {
    return this.itemsSubject.value.reduce((acc, item) => acc + item.cantidad, 0);
  }

  get totalMonto(): number {
    return this.itemsSubject.value.reduce((acc, item) => acc + item.cantidad * item.producto.precio, 0);
  }
}
