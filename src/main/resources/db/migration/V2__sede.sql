-- Sede vecinal por comunidad: centro del mapa y punto de retiro de certificados.
ALTER TABLE comunidad ADD COLUMN IF NOT EXISTS sede_nombre    VARCHAR(160);
ALTER TABLE comunidad ADD COLUMN IF NOT EXISTS sede_direccion VARCHAR(255);

-- Backfill de las comunidades ya existentes.
UPDATE comunidad
   SET sede_nombre = 'Sede Junta de Vecinos Villa Las Flores',
       sede_direccion = 'Av. Lo Errázuriz 3940, Maipú'
 WHERE slug = 'villa_las_flores' AND sede_direccion IS NULL;

-- Para el resto, se centra en la comuna hasta que el admin ajuste la dirección.
UPDATE comunidad
   SET sede_nombre = COALESCE(sede_nombre, 'Sede ' || nombre),
       sede_direccion = COALESCE(sede_direccion, COALESCE(comuna, 'Chile'))
 WHERE sede_direccion IS NULL;
