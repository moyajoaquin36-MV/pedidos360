# Demo Kafka (presentacion individual EP6)

## 0. Antes de empezar
1. Learner Lab encendido y esperar 2-3 minutos (17 contenedores).
2. Si presentas desde otra red: agregar tu IP (https://checkip.amazonaws.com) a la regla del puerto 8080 (Kafka UI) del SG `sg-0782a8487bdcac91d`.
3. Reponer stock en la app (Admin → Pan amasado de local-01 tiene 0): asi el pedido de la demo no da 409.
4. Pestanas: la app (`localhost:4200`), Kafka UI (`http://174.129.161.62:8080`), Postman y la terminal de la EC2.

## 1. Terminal de la EC2
Consola AWS → EC2 → `pedidos360` → Conectar → EC2 Instance Connect → `ec2-user`. Despues `cd ~/pedidos360`.

## 2. Cluster: 3 Zookeeper + 3 brokers
```bash
docker ps --format "table {{.Names}}\t{{.Status}}" | grep -E "zookeeper|kafka"
```
Los 3 Zookeeper (votan quien es el controlador; con 3 aguanta 1 caido) y los 3 brokers (healthy).

## 3. Topicos 3 particiones x 3 replicas
```bash
./deploy/kafka-topicos.sh describir
```
Cada particion tiene un lider distinto y `Isr: 1,2,3` (las 3 copias al dia). Kafka UI → **Topics** muestra lo mismo.

## 4. Flujo productor → consumidores en tiempo real
1. En la app crear un pedido y cambiarle el estado.
2. `/panel` se actualiza en menos de 3 s (ms-reportes, `grupo-reporteria`).
3. Kafka UI → Topics → `pedidos-eventos` → **Messages**: el JSON del evento, con clave = pedidoId.
4. Postman → `GET /api/auditoria/eventos`: el mismo evento guardado con usuario, rol, topico y offset (`grupo-auditoria`).

## 5. Lag (consumer groups)
```bash
docker stop pedidos360-ms-auditoria-1
```
Crear 2-3 pedidos → Kafka UI → **Consumers** → `grupo-auditoria`: la columna **Lag** sube (`grupo-reporteria` sigue en 0).
```bash
docker start pedidos360-ms-auditoria-1
```
En unos segundos el lag vuelve a 0: Kafka guardo los eventos y el consumidor los leyo al volver (en RabbitMQ eso no se puede releer).

## 6. Tolerancia a fallos
```bash
docker stop kafka-2 && ./deploy/kafka-topicos.sh describir
```
El ISR baja a 2 y otro broker asume el liderazgo; crear un pedido sigue funcionando (`min.insync.replicas=2`).
`docker start kafka-2` y el ISR vuelve a 3.

## 7. Errores → DLT
Kafka UI → Topics → `pedidos-eventos` → **Produce Message**: clave `999`, valor `esto no es json`.
```bash
docker logs --since 1m pedidos360-ms-reportes-1 | grep "ALERTA DLT"
```
El mensaje queda en `pedidos-eventos.DLT` (misma particion) y el consumidor sigue funcionando.

## 8. Administracion (ms-kafka-admin)
Postman → carpeta **Kafka admin (ADMIN_GENERAL)** → **Run folder**: crea `demo-postman` (3x3), cambia la retencion,
sube a 6 particiones, muestra grupos y lag, valida errores (400/409) y borra el topico.

## 9. Si algo falla
| Sintoma | Causa | Solucion |
|---|---|---|
| Kafka UI no abre | Tu IP cambio | Actualizar la regla 8080 del SG |
| Pedido da 409 | Sin stock | Reponer en Admin |
| Postman 401 | Token vencido (~1 h) | Copiar uno nuevo desde la app (F12 → Network) |
| Panel vacio al arrancar | ms-reportes releyendo el topico | Esperar 10-20 s |
