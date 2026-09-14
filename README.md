# pedidos360-backend

Backend del sistema **Pedidos 360** (DSY1107 - Evaluacion Parcial N°1): 2 microservicios Spring
Boot, cada uno protegido con JWT emitido por Azure AD y pensado para ser consumido a traves de AWS
API Gateway.

## Modulos

| Modulo | Responsabilidad | Puerto por defecto |
|---|---|---|
| [`ms-pedidos`](./ms-pedidos) | Ciclo de vida del pedido (crear, listar, cambiar estado) | 8081 |
| [`ms-productos`](./ms-productos) | Catalogo de productos y stock basico por local | 8082 |

Cada modulo es un proyecto Spring Boot **independiente** (su propio `spring-boot-starter-parent`,
su propio jar desplegable). Este repo solo los agrupa para facilitar el desarrollo en pareja; en
produccion cada uno corre en su propia instancia EC2. El detalle de configuracion (variables de
entorno pendientes, seguridad, endpoints) esta en el `README.md` de cada modulo.

## Compilar y testear todo junto

```bash
./mvnw test          # corre los tests de ms-pedidos y ms-productos en un solo comando
```

Tambien se puede compilar/testear cada modulo por separado entrando a su carpeta y usando su propio
`./mvnw`.

## Correr localmente

```bash
docker compose -f ../docker-compose.yml up -d     # MySQL en localhost:3306

cd ms-pedidos && ./mvnw spring-boot:run            # puerto 8081
cd ms-productos && ./mvnw spring-boot:run          # puerto 8082
```
