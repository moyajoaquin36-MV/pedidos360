import { Injectable } from '@angular/core';
import { BehaviorSubject } from 'rxjs';
import { LOCALES } from '../data/locales';

/** Local que el usuario esta explorando/comprando en este momento (para filtrar el catalogo). */
@Injectable({ providedIn: 'root' })
export class LocalSeleccionadoService {
  private readonly localSubject = new BehaviorSubject<string>(LOCALES[0].id);
  readonly local$ = this.localSubject.asObservable();

  get actual(): string {
    return this.localSubject.value;
  }

  seleccionar(localId: string): void {
    this.localSubject.next(localId);
  }
}
