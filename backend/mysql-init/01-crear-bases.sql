-- Cada microservicio tiene su propia base de datos (patron database-per-service).
-- MySQL solo ejecuta este script al crear el volumen mysql-data por primera vez. Si el
-- volumen ya existe, correrlo a mano (es idempotente) desde la carpeta backend:
--   docker compose -f docker-compose.aws.yml -f docker-compose.kafka.yml exec -T mysql sh -c 'mysql -uroot -p"$MYSQL_ROOT_PASSWORD"' < mysql-init/01-crear-bases.sql
CREATE DATABASE IF NOT EXISTS ms_pedidos_db;
CREATE DATABASE IF NOT EXISTS ms_productos_db;
CREATE DATABASE IF NOT EXISTS ms_auditoria_db;

GRANT ALL PRIVILEGES ON ms_pedidos_db.* TO 'pedidos360'@'%';
GRANT ALL PRIVILEGES ON ms_productos_db.* TO 'pedidos360'@'%';
GRANT ALL PRIVILEGES ON ms_auditoria_db.* TO 'pedidos360'@'%';
FLUSH PRIVILEGES;
