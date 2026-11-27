# Base de datos de PeruBus Cargo

Base de datos PostgreSQL alojada en Supabase. El backend (Spring Boot) se conecta con
las variables `DB_URL`, `DB_USERNAME` y `DB_PASSWORD` del archivo `backend/.env`
(ese archivo no se sube al repositorio).

## Tablas

| Tabla | Para qué sirve |
|---|---|
| `cliente` | Personas (DNI, 8 dígitos) y empresas (RUC, 11 dígitos). También guarda la cuenta del portal del cliente |
| `usuario` | Personal de PeruBus. Rol: `ADMIN`, `SUPERVISOR`, `AGENTE` u `OPERARIO` |
| `encomienda` | Cada envío: ruta, peso, tarifa, estado, remitente, destinatario y manifiesto |
| `manifiesto` | Bus, ruta, fecha de viaje y capacidad. Agrupa las encomiendas de un viaje (RF-07) |
| `comprobante` | Pago de una encomienda (boleta o factura, con IGV 18%) |
| `evento_tracking` | Historial de estados de cada encomienda (RF-05) |
| `solicitud_encomienda` | Solicitudes del cliente desde el portal: cambio de dirección, agregar bulto o cancelar |
| `bulto_encomienda` | Bultos adicionales agregados a una encomienda |
| `incidencia` | Reclamos y problemas de una encomienda (RF-08) |

## Reglas que la base hace cumplir (`mejoras_bd.sql`, paso 3)

- `cliente.num_documento`: único y con 8 u 11 dígitos.
- `usuario.username`: único (sin distinguir mayúsculas). Usuario, contraseña, rol y estado son obligatorios.
- `usuario.rol`: solo `ADMIN`, `SUPERVISOR`, `AGENTE` u `OPERARIO`.
- `encomienda.estado`: solo los estados del flujo (pendiente de pago, en origen, en tránsito,
  llegó a agencia de destino, disponible para recojo, en reparto, recogido, entregado, cancelado
  y pendiente de pago adicional). Peso mayor a 0 y tarifa no negativa.
- `solicitud_encomienda`: tipo y estado dentro de los valores válidos.
- Índices en las claves foráneas para acelerar las consultas.

Si se agregan estados o tipos nuevos en el código, hay que ampliar el `CHECK` correspondiente.

## Cómo aplicar los cambios

1. Avisar al equipo: la base es compartida.
2. Abrir `mejoras_bd.sql` y ejecutarlo en el editor SQL de Supabase, **un paso a la vez**
   (el paso 0 es el respaldo; el paso 1 es solo lectura).
3. No ejecutar el paso 3 hasta que el diagnóstico del paso 1 salga vacío.
4. Desactivar el traductor automático del navegador en la pestaña de Supabase.

## Pendiente

- Activar RLS en las tablas (paso 4, comentado en el script).
- Tabla `ruta`: hoy las rutas y sus tarifas están fijas en el código.
- Contraseñas del personal cifradas con BCrypt (RNF-04).
- Migraciones versionadas (Flyway) en lugar de `ddl-auto=update`.
