-- Cada microservicio tiene su propia base de datos (patron database-per-service).
CREATE DATABASE IF NOT EXISTS ms_pedidos_db;
CREATE DATABASE IF NOT EXISTS ms_productos_db;

GRANT ALL PRIVILEGES ON ms_pedidos_db.* TO 'pedidos360'@'%';
GRANT ALL PRIVILEGES ON ms_productos_db.* TO 'pedidos360'@'%';
FLUSH PRIVILEGES;
