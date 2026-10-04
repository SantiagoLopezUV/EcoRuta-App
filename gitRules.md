# Reglas de Git del proyecto

## Tipos de cambio

Estos prefijos se usan tanto para nombrar ramas como para los commits.

| Tipo       | Uso en el proyecto                |
|------------|-----------------------------------|
| `feat`     | Nueva funcionalidad               |
| `fix`      | Corrección de errores             |
| `docs`     | Documentación                     |
| `refactor` | Reorganización o mejora del código|
| `test`     | Pruebas                           |
| `chore`    | Dependencias y librerías          |
| `ci`       | Despliegue e integración continua |

## Nombres de ramas

Formato: `tipo/descripcion-corta`

- Usar uno de los tipos de la tabla anterior.
- Escribir la descripción en minúsculas y separar las palabras con guiones.
- Mantener el nombre conciso pero claro respecto al trabajo realizado.

Ejemplos:

```
feat/payment-integration
fix/login-validation
docs/update-readme
```

## Mensajes de commit

Formato: `tipo(alcance): descripción del trabajo realizado`

- Todos los commits deben estar escritos en **inglés**.
- Iniciar con el tipo de cambio, seguido del alcance entre paréntesis y dos puntos.
- Mantener el mensaje conciso pero claro respecto al trabajo realizado.
- Usar el verbo en imperativo (`add`, `fix`, `update`), no en pasado.

Ejemplos:

```
feat(payment): add credit card processing integration
fix(auth): handle expired token on login
docs(readme): add installation instructions
```