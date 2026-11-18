# Demo RabbitMQ (presentacion individual)

## 0. Antes de empezar
1. Learner Lab encendido (circulo verde) y esperar 2 minutos a que arranquen los contenedores.
2. Si presentas desde otra red: agregar tu IP (https://checkip.amazonaws.com) a la regla de los puertos
   15672-15673 del security group `sg-0782a8487bdcac91d`.
3. Abrir en pestanas: la app, la consola RabbitMQ (nodo 1 y nodo 2), Postman y la terminal de la EC2.

## 1. Abrir la terminal de la EC2 (desde el navegador)
Consola AWS → **EC2 → Instancias** → `pedidos360` → **Conectar** → pestana **EC2 Instance Connect** →
usuario `ec2-user` → **Conectar**. Despues:

```bash
cd ~/pedidos360
```

## 2. docker compose levantando RabbitMQ en la nube
```bash
docker compose -f docker-compose.aws.yml ps
```
Se ven `rabbitmq-1` y `rabbitmq-2` **(healthy)** junto a los microservicios, MySQL y el frontend.

## 3. Cluster de 2 nodos
```bash
docker exec rabbitmq-1 rabbitmqctl cluster_status
```
En **Running Nodes** aparecen `rabbit@rabbitmq-1` y `rabbit@rabbitmq-2`.
En la consola web: **Overview → Nodes** (los dos en verde) y abrir tambien el nodo 2 (puerto 15673): muestra lo mismo.

## 4. Colas quorum con su DLQ
```bash
docker exec rabbitmq-1 rabbitmqctl list_queues name type members messages consumers
```
6 colas `quorum` replicadas en ambos nodos; las 3 principales con 1 consumidor.
En la consola: **Queues and Streams** (`quorum`, `rabbit@rabbitmq-1 +1`, features `DLX`).

## 5. Exchanges direct y topic
Consola → **Exchanges**: `pedidos360.direct` (direct), `pedidos360.topic` (topic) y los 3 `*.dlx` (fanout).
Clic en `pedidos360.topic` → **Bindings**: `pedido.estado.*` y `producto.stock.*`.

## 6. Flujo real: pedido → ticket y notificacion
Dejar los logs abiertos en vivo:
```bash
docker logs -f --since 1m pedidos360-ms-pedidos-1 | grep -E "Publicado|TICKET|NOTIFICACION|ALERTA DLQ"
```
1. En la app: crear un pedido → aparece `Publicado en pedidos360.direct [pedido.creado]` y `TICKET Pedido #N ...`.
2. Cambiar su estado → `Publicado en pedidos360.topic [pedido.estado.LISTO]` y `NOTIFICACION para cliente ...`.
Salir con `Ctrl+C`.

## 7. DLQ funcionando (mensaje invalido → NACK → DLQ)
Consola → **Exchanges** → `pedidos360.direct` → **Publish message**:
- Routing key: `pedido.creado`
- Properties: `content_type` = `application/json`
- Payload: `esto no es json`
- **Publish message**

En la terminal:
```bash
docker logs --since 1m pedidos360-ms-pedidos-1 | grep "ALERTA DLQ"
```
Sale `[ALERTA DLQ] [pedidos.tickets] Mensaje invalido, enviado a la DLQ ...`.
En la consola: **Queues** → `pedidos.tickets.dlq` tiene 1 mensaje mas → **Get messages** muestra el mensaje rechazado.

Lo mismo funciona en `pedidos360.topic` con `pedido.estado.LISTO` (cola `notificaciones.pedidos`)
y `producto.stock.bajo` (cola `inventario.alertas`, logs de `pedidos360-ms-productos-1`).

## 8. Control plane (ms-rabbit-admin)
Postman → carpeta **RabbitMQ admin (ADMIN_GENERAL)**:
1. Enviar solo `POST topology?dominio=demo` → **201**. En la consola aparecen `demo.queue` y `demo.dlq` (quorum).
2. **Run folder** con el resto → validaciones con **400** y limpieza con **204**.

## 9. Si algo falla
| Sintoma | Causa | Solucion |
|---|---|---|
| La app responde 503 o error | Contenedores arrancando | Esperar 1-2 min y recargar |
| La consola RabbitMQ no abre | Tu IP cambio | Actualizar la regla 15672-15673 del security group |
| Postman da 401 con token | Token vencido (dura ~1 h) | Copiar uno nuevo desde la app (F12 → Network) |
| EC2 Instance Connect no conecta | Falta la regla SSH de AWS | Regla SSH desde `18.206.107.24/29` en el security group |
