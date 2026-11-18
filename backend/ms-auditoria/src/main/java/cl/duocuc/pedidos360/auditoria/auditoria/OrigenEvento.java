package cl.duocuc.pedidos360.auditoria.auditoria;

/** De donde vino el evento: permite ubicar el mensaje original en Kafka. */
public record OrigenEvento(String topico, int particion, long offset, String clave) {
}
