# Base de datos de EcoRuta

## Esquema remoto actual

El archivo `remote_schema.sql` contiene una exportación del esquema SQL actual de la base de datos remota de EcoRuta en Supabase.

Este es el archivo de referencia para consultar la estructura vigente de la base de datos.

### ¿Qué contiene?

- Tablas y columnas.
- Funciones y procedimientos exportables.
- Vistas.
- Índices.
- Restricciones.
- Políticas de seguridad RLS.
- Otras definiciones SQL exportables.

**Importante:** este archivo contiene principalmente la estructura de la base de datos. No es un respaldo completo de los datos, usuarios de autenticación, archivos de Storage ni configuraciones del proyecto Supabase.

## ¿Cómo actualizarlo?

Cuando se realicen cambios en la base de datos remota y se quiera actualizar esta fotografía:

1. Abrir una terminal en la raíz del proyecto.
2. Verificar que Supabase esté vinculado al proyecto correcto.
3. Ejecutar:

   `supabase db dump --linked -f database/remote_schema.sql`

4. Revisar el archivo generado.
5. Subir el cambio a GitHub.

El comando reemplaza el contenido del archivo existente.

## Diferencia entre este archivo y las migraciones

- `database/remote_schema.sql`: representa el esquema actual de referencia.
- `supabase/migrations/`: conserva el historial de cambios de la base de datos.

**No eliminar ni reemplazar las migraciones por este archivo.**

## Regla para el equipo de desarrollo

Consultar `remote_schema.sql` para conocer la estructura actual de la base de datos.

Los cambios estructurales deben gestionarse mediante migraciones y coordinarse con el equipo antes de modificar la base de datos compartida.