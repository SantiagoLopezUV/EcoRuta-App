--Asignación de vehículo recolector al chofer
ALTER TABLE public.operators
  ADD COLUMN IF NOT EXISTS vehicle_plate text;
