package cl.duocuc.pedidos360.rabbitadmin.dto;

import org.springframework.amqp.core.ExchangeTypes;

public enum TipoExchange {
    DIRECT(ExchangeTypes.DIRECT),
    TOPIC(ExchangeTypes.TOPIC),
    FANOUT(ExchangeTypes.FANOUT);

    private final String valorRabbit;

    TipoExchange(String valorRabbit) {
        this.valorRabbit = valorRabbit;
    }

    public String valorRabbit() {
        return valorRabbit;
    }
}
