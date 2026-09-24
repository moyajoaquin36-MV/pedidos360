export interface Local {
  id: string;
  nombre: string;
  comuna: string;
  tipo: 'Panaderia' | 'Pasteleria' | 'Cafeteria';
}

/**
 * Los 20 locales asociados a la red Pedidos 360. Es informacion de
 * referencia en el frontend (no hay un microservicio de "locales" en el
 * alcance de esta entrega); local-01/02/03 son los que tienen productos
 * de ejemplo cargados en ms-productos.
 */
export const LOCALES: Local[] = [
  { id: 'local-01', nombre: 'Panaderia Santa Marta', comuna: 'Providencia', tipo: 'Panaderia' },
  { id: 'local-02', nombre: 'Cafe Central', comuna: 'Nunoa', tipo: 'Cafeteria' },
  { id: 'local-03', nombre: 'Pasteleria Dulce Hogar', comuna: 'Las Condes', tipo: 'Pasteleria' },
  { id: 'local-04', nombre: 'Panaderia El Trigal', comuna: 'Maipu', tipo: 'Panaderia' },
  { id: 'local-05', nombre: 'Cafeteria Aroma', comuna: 'La Florida', tipo: 'Cafeteria' },
  { id: 'local-06', nombre: 'Pasteleria Los Almendros', comuna: 'San Miguel', tipo: 'Pasteleria' },
  { id: 'local-07', nombre: 'Panaderia La Espiga', comuna: 'Independencia', tipo: 'Panaderia' },
  { id: 'local-08', nombre: 'Cafe del Parque', comuna: 'Providencia', tipo: 'Cafeteria' },
  { id: 'local-09', nombre: 'Pasteleria Rincon Dulce', comuna: 'Nunoa', tipo: 'Pasteleria' },
  { id: 'local-10', nombre: 'Panaderia San Jose', comuna: 'Recoleta', tipo: 'Panaderia' },
  { id: 'local-11', nombre: 'Cafeteria Buen Dia', comuna: 'Vitacura', tipo: 'Cafeteria' },
  { id: 'local-12', nombre: 'Pasteleria Tres Leches', comuna: 'La Reina', tipo: 'Pasteleria' },
  { id: 'local-13', nombre: 'Panaderia El Horno', comuna: 'Quinta Normal', tipo: 'Panaderia' },
  { id: 'local-14', nombre: 'Cafe Andino', comuna: 'Santiago Centro', tipo: 'Cafeteria' },
  { id: 'local-15', nombre: 'Pasteleria Merengue', comuna: 'Penalolen', tipo: 'Pasteleria' },
  { id: 'local-16', nombre: 'Panaderia Campo Lindo', comuna: 'Pudahuel', tipo: 'Panaderia' },
  { id: 'local-17', nombre: 'Cafeteria Molienda', comuna: 'San Bernardo', tipo: 'Cafeteria' },
  { id: 'local-18', nombre: 'Pasteleria Delicia', comuna: 'La Cisterna', tipo: 'Pasteleria' },
  { id: 'local-19', nombre: 'Panaderia Nueva Era', comuna: 'Estacion Central', tipo: 'Panaderia' },
  { id: 'local-20', nombre: 'Cafe Sur', comuna: 'Puente Alto', tipo: 'Cafeteria' }
];
