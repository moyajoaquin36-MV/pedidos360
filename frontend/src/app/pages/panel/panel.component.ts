import { CommonModule } from '@angular/common';
import { Component } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { RouterLink } from '@angular/router';
import { catchError, filter, of, switchMap, tap, timer } from 'rxjs';
import { Rol } from '../../auth/roles';
import { RolesService } from '../../auth/roles.service';
import { LOCALES } from '../../data/locales';
import { AuditoriaService, EventoAuditoria } from '../../services/auditoria.service';
import { EventoReciente, Metricas, ReportesService } from '../../services/reportes.service';

const INTERVALO_MS = 3000;
const ORDEN_ESTADOS = ['RECIBIDO', 'EN_PREPARACION', 'LISTO', 'EN_CAMINO', 'ENTREGADO'];

/**
 * Panel en tiempo real (ADMIN_GENERAL y OPERADOR_COCINA). ms-reportes calcula las metricas
 * consumiendo los topicos Kafka; aqui se consultan cada 3 s. Los timers se cancelan solos
 * al destruir el componente (takeUntilDestroyed).
 */
@Component({
  selector: 'app-panel',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './panel.component.html'
})
export class PanelComponent {
  metricas: Metricas | null = null;
  estados: { estado: string; cantidad: number; porcentaje: number }[] = [];
  ventasPorLocal: { localId: string; nombre: string; ventas: number }[] = [];
  eventos: EventoReciente[] = [];

  esAdmin = false;
  auditoria: EventoAuditoria[] = [];
  errorAuditoria = false;

  /** Date.now() de la ultima respuesta correcta de ms-reportes. */
  ultimaActualizacion: number | null = null;
  ultimoIntentoFallo = false;
  ahora = Date.now();

  constructor(reportes: ReportesService, auditoria: AuditoriaService, roles: RolesService) {
    // Reloj para el texto "actualizado hace Xs".
    timer(0, 1000)
      .pipe(takeUntilDestroyed())
      .subscribe(() => (this.ahora = Date.now()));

    timer(0, INTERVALO_MS)
      .pipe(
        switchMap(() => reportes.metricas().pipe(catchError(() => of(null)))),
        takeUntilDestroyed()
      )
      .subscribe((metricas) => {
        this.ultimoIntentoFallo = !metricas;
        if (metricas) {
          this.aplicar(metricas);
          this.ultimaActualizacion = Date.now();
        }
      });

    // La auditoria solo la puede leer ADMIN_GENERAL (el backend responde 403 al resto).
    roles
      .cargar()
      .pipe(
        filter((r) => r.includes(Rol.AdminGeneral)),
        tap(() => (this.esAdmin = true)),
        switchMap(() => timer(0, INTERVALO_MS)),
        switchMap(() => auditoria.eventos(20).pipe(catchError(() => of(null)))),
        takeUntilDestroyed()
      )
      .subscribe((eventos) => {
        this.errorAuditoria = !eventos;
        if (eventos) {
          this.auditoria = eventos;
        }
      });
  }

  get segundosDesdeActualizacion(): number | null {
    return this.ultimaActualizacion === null
      ? null
      : Math.max(0, Math.floor((this.ahora - this.ultimaActualizacion) / 1000));
  }

  /** Rojo si nunca llego respuesta, si fallo el ultimo intento o si el dato tiene mas de 10 s. */
  get desactualizado(): boolean {
    const s = this.segundosDesdeActualizacion;
    return s === null || this.ultimoIntentoFallo || s > 10;
  }

  nombreLocal(localId: string): string {
    return LOCALES.find((l) => l.id === localId)?.nombre ?? localId;
  }

  porEventId(_: number, evento: EventoReciente): string {
    return evento.eventId;
  }

  private aplicar(m: Metricas): void {
    this.metricas = { ...m, topProductos: m.topProductos ?? [], stockCritico: m.stockCritico ?? [] };

    const porEstado = m.pedidosPorEstado ?? {};
    // Los 5 estados del flujo siempre, en orden; cualquier otro (p. ej. CANCELADO) al final.
    const claves = [...ORDEN_ESTADOS, ...Object.keys(porEstado).filter((e) => !ORDEN_ESTADOS.includes(e))];
    const maximo = Math.max(1, ...claves.map((e) => porEstado[e] ?? 0));
    this.estados = claves.map((estado) => {
      const cantidad = porEstado[estado] ?? 0;
      return { estado, cantidad, porcentaje: (cantidad / maximo) * 100 };
    });

    this.ventasPorLocal = Object.entries(m.ventasPorLocal ?? {})
      .map(([localId, ventas]) => ({ localId, nombre: this.nombreLocal(localId), ventas }))
      .sort((a, b) => b.ventas - a.ventas);

    this.eventos = [...(m.ultimosEventos ?? [])].sort((a, b) => Date.parse(b.fecha) - Date.parse(a.fecha));
  }
}
