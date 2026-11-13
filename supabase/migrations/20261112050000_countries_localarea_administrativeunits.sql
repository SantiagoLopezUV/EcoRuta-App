DO $$
DECLARE
  _country_id uuid;
  _department_id uuid;
  _city_id uuid;
BEGIN
  -- 1. Insertar el País y capturar su UUID
  -- (Asegúrate de que los ISO codes sean de 2 y 3 caracteres exactos)
  INSERT INTO public.countries (name, iso_code_alpha_2, iso_code_alpha_3)
  VALUES ('Colombia', 'CO', 'COL')
  RETURNING id INTO _country_id;

  -- 2. Insertar el Departamento (Actúa como nodo raíz, parent_id es NULL)
  -- IMPORTANTE: El 'unit_type' debe ir obligatoriamente en minúsculas por tu constraint.
  INSERT INTO public.administrative_units (country_id, parent_id, name, unit_type)
  VALUES (_country_id, NULL, 'Valle del Cauca', 'department')
  RETURNING id INTO _department_id;

  -- 3. Insertar la Ciudad (Hija del departamento, usamos su UUID en parent_id)
  INSERT INTO public.administrative_units (country_id, parent_id, name, unit_type)
  VALUES (_country_id, _department_id, 'Tulua', 'municipality')
  RETURNING id INTO _city_id;

  -- 4. Insertar el listado de Barrios asociados a la ciudad
  -- IMPORTANTE: El 'area_type' también debe ir en minúsculas por tu constraint.
  INSERT INTO public.local_areas (administrative_unit_id, name, area_type)
  VALUES

    (_city_id, 'Alameda I y II', 'neighborhood'),
    (_city_id, 'Alvernia', 'neighborhood'),
    (_city_id, 'Asoagrin', 'neighborhood'),
    (_city_id, 'Avenida Cali', 'neighborhood'),
    (_city_id, 'Bello Horizonte', 'neighborhood'),
    (_city_id, 'Bolivar', 'neighborhood'),
    (_city_id, 'Bosques de Maracaibo', 'neighborhood'),
    (_city_id, 'Buenos Aires', 'neighborhood'),
    (_city_id, 'Casa Huertas', 'neighborhood'),
    (_city_id, 'Cespedes', 'neighborhood'),
    (_city_id, 'Chiminangos', 'neighborhood'),
    (_city_id, 'Ciudad Campestre', 'neighborhood'),
    (_city_id, 'Comfamiliar', 'neighborhood'),
    (_city_id, 'Departamental', 'neighborhood'),
    (_city_id, 'Diablos Rojos', 'neighborhood'),
    (_city_id, 'Doce de Octubre', 'neighborhood'),
    (_city_id, 'El Bosque', 'neighborhood'),
    (_city_id, 'El Centro', 'neighborhood'),
    (_city_id, 'El Condor I y II', 'neighborhood'),
    (_city_id, 'El Dorado', 'neighborhood'),
    (_city_id, 'El Jardin', 'neighborhood'),
    (_city_id, 'El Jazmin', 'neighborhood'),
    (_city_id, 'El Lago', 'neighborhood'),
    (_city_id, 'El Palmar', 'neighborhood'),
    (_city_id, 'El Porvenir', 'neighborhood'),
    (_city_id, 'El Principe', 'neighborhood'),
    (_city_id, 'El Retiro', 'neighborhood'),
    (_city_id, 'Entre Rios', 'neighborhood'),
    (_city_id, 'Escobar', 'neighborhood'),
    (_city_id, 'Estambul', 'neighborhood'),
    (_city_id, 'Farfan', 'neighborhood'),
    (_city_id, 'Franciscanos', 'neighborhood'),
    (_city_id, 'Fatima', 'neighborhood'),
    (_city_id, 'Guayacanes', 'neighborhood'),
    (_city_id, 'Horizonte', 'neighborhood'),
    (_city_id, 'Internacional', 'neighborhood'),
    (_city_id, 'Jorge Eliecer Gaitan', 'neighborhood'),
    (_city_id, 'La Bastilla', 'neighborhood'),
    (_city_id, 'La Campiña', 'neighborhood'),
    (_city_id, 'La Ceiba', 'neighborhood'),
    (_city_id, 'La Esperanza', 'neighborhood'),
    (_city_id, 'La Graciela', 'neighborhood'),
    (_city_id, 'La Independencia', 'neighborhood'),
    (_city_id, 'La Inmaculada', 'neighborhood'),
    (_city_id, 'La Quinta', 'neighborhood'),
    (_city_id, 'La Rivera', 'neighborhood'),
    (_city_id, 'La Santa Cruz', 'neighborhood'),
    (_city_id, 'La Trinidad', 'neighborhood'),
    (_city_id, 'La Villa', 'neighborhood'),
    (_city_id, 'Las Americas', 'neighborhood'),
    (_city_id, 'Las Brisas', 'neighborhood'),
    (_city_id, 'Las Delicias', 'neighborhood'),
    (_city_id, 'Las Nieves', 'neighborhood'),
    (_city_id, 'Las Veraneras', 'neighborhood'),
    (_city_id, 'Laureles I y II', 'neighborhood'),
    (_city_id, 'Lusitania', 'neighborhood'),
    (_city_id, 'Maracaibo', 'neighborhood'),
    (_city_id, 'Marandua', 'neighborhood'),
    (_city_id, 'Miraflores', 'neighborhood'),
    (_city_id, 'Municipal', 'neighborhood'),
    (_city_id, 'Palobonito', 'neighborhood'),
    (_city_id, 'Panamericano', 'neighborhood'),
    (_city_id, 'Popular', 'neighborhood'),
    (_city_id, 'Portales del Rio', 'neighborhood'),
    (_city_id, 'Prados del Norte', 'neighborhood'),
    (_city_id, 'Progresar', 'neighborhood'),
    (_city_id, 'Pueblo Nuevo', 'neighborhood'),
    (_city_id, 'Rojas', 'neighborhood'),
    (_city_id, 'Ruben Cruz Velez', 'neighborhood'),
    (_city_id, 'Sajonia', 'neighborhood'),
    (_city_id, 'Salesiano', 'neighborhood'),
    (_city_id, 'Saman del Norte', 'neighborhood'),
    (_city_id, 'San Antonio', 'neighborhood'),
    (_city_id, 'San Benito', 'neighborhood'),
    (_city_id, 'San Pedro Claver', 'neighborhood'),
    (_city_id, 'Santa Rita del Río', 'neighborhood'),
    (_city_id, 'Siete de Agosto', 'neighborhood'),
    (_city_id, 'Tercer Milenio', 'neighborhood'),
    (_city_id, 'Tomas Uribe Uribe', 'neighborhood'),
    (_city_id, 'Victoria', 'neighborhood'),
    (_city_id, 'Villa Campestre', 'neighborhood'),
    (_city_id, 'Villa Colombia', 'neighborhood'),
    (_city_id, 'Villa del Lago', 'neighborhood'),
    (_city_id, 'Villa del Sur', 'neighborhood'),
    (_city_id, 'Villa Liliana', 'neighborhood'),
    (_city_id, 'Villanueva', 'neighborhood');

END $$;