package cl.duocuc.pedidos360.rabbitadmin.controller;

import cl.duocuc.pedidos360.rabbitadmin.dto.BindingRequest;
import cl.duocuc.pedidos360.rabbitadmin.dto.ColaResponse;
import cl.duocuc.pedidos360.rabbitadmin.dto.CrearColaRequest;
import cl.duocuc.pedidos360.rabbitadmin.dto.CrearExchangeRequest;
import cl.duocuc.pedidos360.rabbitadmin.dto.Nombres;
import cl.duocuc.pedidos360.rabbitadmin.dto.PurgaResponse;
import cl.duocuc.pedidos360.rabbitadmin.dto.TopologiaResponse;
import cl.duocuc.pedidos360.rabbitadmin.service.RabbitAdminService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * API REST del control plane de RabbitMQ (solo ADMIN_GENERAL).
 *
 *  POST   /api/rabbitmq/queues                 crea una cola (con DLX/DLQ por defecto)
 *  GET    /api/rabbitmq/queues/{nombre}        mensajes y consumidores de una cola
 *  DELETE /api/rabbitmq/queues/{nombre}        elimina una cola
 *  POST   /api/rabbitmq/queues/{nombre}/purge  vacia una cola sin borrarla
 *  POST   /api/rabbitmq/exchanges              crea un exchange (DIRECT, TOPIC o FANOUT)
 *  DELETE /api/rabbitmq/exchanges/{nombre}
 *  POST   /api/rabbitmq/bindings               enlaza exchange -> cola con una routing key
 *  DELETE /api/rabbitmq/bindings               elimina ese enlace (mismo cuerpo)
 *  POST   /api/rabbitmq/topology?dominio=x     crea x.exchange, x.queue, x.dlx y x.dlq de una vez
 */
@RestController
@RequestMapping("/api/rabbitmq")
public class RabbitAdminController {

    private final RabbitAdminService service;

    public RabbitAdminController(RabbitAdminService service) {
        this.service = service;
    }

    @PostMapping("/queues")
    @ResponseStatus(HttpStatus.CREATED)
    public ColaResponse crearCola(@Valid @RequestBody CrearColaRequest request) {
        return service.crearCola(request);
    }

    @GetMapping("/queues/{nombre}")
    public ColaResponse obtenerCola(@PathVariable @Pattern(regexp = Nombres.PATRON, message = Nombres.MENSAJE) String nombre) {
        return service.obtenerCola(nombre);
    }

    @DeleteMapping("/queues/{nombre}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminarCola(@PathVariable @Pattern(regexp = Nombres.PATRON, message = Nombres.MENSAJE) String nombre) {
        service.eliminarCola(nombre);
    }

    @PostMapping("/queues/{nombre}/purge")
    public PurgaResponse purgarCola(@PathVariable @Pattern(regexp = Nombres.PATRON, message = Nombres.MENSAJE) String nombre) {
        return service.purgarCola(nombre);
    }

    @PostMapping("/exchanges")
    @ResponseStatus(HttpStatus.CREATED)
    public CrearExchangeRequest crearExchange(@Valid @RequestBody CrearExchangeRequest request) {
        service.crearExchange(request);
        return request;
    }

    @DeleteMapping("/exchanges/{nombre}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminarExchange(@PathVariable @Pattern(regexp = Nombres.PATRON, message = Nombres.MENSAJE) String nombre) {
        service.eliminarExchange(nombre);
    }

    @PostMapping("/bindings")
    @ResponseStatus(HttpStatus.CREATED)
    public BindingRequest crearBinding(@Valid @RequestBody BindingRequest request) {
        service.crearBinding(request);
        return request;
    }

    @DeleteMapping("/bindings")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminarBinding(@Valid @RequestBody BindingRequest request) {
        service.eliminarBinding(request);
    }

    @PostMapping("/topology")
    @ResponseStatus(HttpStatus.CREATED)
    public TopologiaResponse crearTopologia(
            @RequestParam @Pattern(regexp = Nombres.PATRON_DOMINIO, message = Nombres.MENSAJE_DOMINIO) String dominio) {
        return service.crearTopologia(dominio);
    }
}
