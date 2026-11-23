#!/usr/bin/env bash
# Operaciones de Kafka por CLI (replicables) para Pedidos 360. Se ejecuta en la EC2,
# con el cluster arriba (docker-compose.aws.yml + docker-compose.kafka.yml):
#   ./deploy/kafka-topicos.sh crear       # crea los 4 topicos del contrato (idempotente)
#   ./deploy/kafka-topicos.sh describir   # particiones, lider, replicas, ISR y configs
#   ./deploy/kafka-topicos.sh grupos      # offsets y LAG de grupo-reporteria y grupo-auditoria
#   ./deploy/kafka-topicos.sh alterar [topico] [clave=valor]
#                                         # ejemplo de kafka-configs --alter
#                                         # (por defecto: pedidos-eventos retention.ms=604800000)
# Fuente de verdad de nombres y valores: docs/kafka-contrato.md
set -euo pipefail

# La CLI corre dentro del contenedor kafka-1 y habla con el listener interno.
CONTENEDOR="${CONTENEDOR:-kafka-1}"
BOOTSTRAP="${BOOTSTRAP:-kafka-1:29092}"

# Heap chico para cada comando de la CLI (si no, hereda los 256M del broker).
kafka_cli() {
  docker exec -e KAFKA_HEAP_OPTS="-Xms64M -Xmx128M" "$CONTENEDOR" "$@"
}

TOPICOS="pedidos-eventos inventario-movimientos pedidos-eventos.DLT inventario-movimientos.DLT"
GRUPOS="grupo-reporteria grupo-auditoria"

crear() {
  # pedidos-eventos: historial de negocio, se borra a los 7 dias.
  kafka_cli kafka-topics --bootstrap-server "$BOOTSTRAP" --create --if-not-exists \
    --topic pedidos-eventos --partitions 3 --replication-factor 3 \
    --config cleanup.policy=delete \
    --config retention.ms=604800000 \
    --config min.insync.replicas=2

  # inventario-movimientos: compactado, conserva el ultimo evento (stock) por productoId.
  kafka_cli kafka-topics --bootstrap-server "$BOOTSTRAP" --create --if-not-exists \
    --topic inventario-movimientos --partitions 3 --replication-factor 3 \
    --config cleanup.policy=compact \
    --config min.insync.replicas=2

  # Dead Letter Topics: mismas 3 particiones (el DLT publica en la misma particion), 14 dias.
  for dlt in pedidos-eventos.DLT inventario-movimientos.DLT; do
    kafka_cli kafka-topics --bootstrap-server "$BOOTSTRAP" --create --if-not-exists \
      --topic "$dlt" --partitions 3 --replication-factor 3 \
      --config cleanup.policy=delete \
      --config retention.ms=1209600000
  done

  kafka_cli kafka-topics --bootstrap-server "$BOOTSTRAP" --list
}

describir() {
  for topico in $TOPICOS; do
    echo "=== $topico ==="
    kafka_cli kafka-topics --bootstrap-server "$BOOTSTRAP" --describe --topic "$topico"
  done
}

grupos() {
  kafka_cli kafka-consumer-groups --bootstrap-server "$BOOTSTRAP" --list
  for grupo in $GRUPOS; do
    echo "=== $grupo ==="
    # CURRENT-OFFSET (ultimo confirmado), LOG-END-OFFSET (ultimo escrito) y LAG = diferencia.
    kafka_cli kafka-consumer-groups --bootstrap-server "$BOOTSTRAP" --describe --group "$grupo" \
      || echo "(el grupo $grupo aun no existe: se crea cuando su microservicio se conecta)"
  done
}

alterar() {
  local topico="${1:-pedidos-eventos}"
  local config="${2:-retention.ms=604800000}"
  # Cambia la configuracion en caliente, sin reiniciar brokers ni recrear el topico.
  kafka_cli kafka-configs --bootstrap-server "$BOOTSTRAP" --alter \
    --entity-type topics --entity-name "$topico" --add-config "$config"
  kafka_cli kafka-configs --bootstrap-server "$BOOTSTRAP" --describe \
    --entity-type topics --entity-name "$topico"
}

case "${1:-}" in
  crear) crear ;;
  describir) describir ;;
  grupos) grupos ;;
  alterar) shift; alterar "$@" ;;
  *)
    echo "Uso: $0 {crear|describir|grupos|alterar [topico] [clave=valor]}" >&2
    exit 1
    ;;
esac
