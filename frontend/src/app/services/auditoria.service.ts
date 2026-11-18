import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';

/** Un evento Kafka guardado por ms-auditoria (GET /api/auditoria/eventos, solo ADMIN_GENERAL). */
export interface EventoAuditoria {
  id: number;
  eventId: string;
  topico: string;
  particion: number;
  offset: number;
  clave: string;
  tipo: string;
  entidadId: string | number;
  localId: string;
  usuario: string;
  rol: string;
  /** ISO-8601 */
  fechaEvento: string;
  /** ISO-8601 */
  recibidoEn: string;
}

@Injectable({ providedIn: 'root' })
export class AuditoriaService {
  private readonly baseUrl = environment.auditoriaBaseUrl;

  constructor(private readonly http: HttpClient) {}

  eventos(limite = 20): Observable<EventoAuditoria[]> {
    return this.http.get<EventoAuditoria[]>(`${this.baseUrl}/eventos`, { params: { limite } });
  }
}
