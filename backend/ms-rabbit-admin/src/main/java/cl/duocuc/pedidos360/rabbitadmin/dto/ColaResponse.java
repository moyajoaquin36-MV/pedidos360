package cl.duocuc.pedidos360.rabbitadmin.dto;

public record ColaResponse(String nombre, long mensajes, int consumidores, String dlq) {
}
