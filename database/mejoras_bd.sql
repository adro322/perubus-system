-- =====================================================================
-- PeruBus Cargo - MEJORAS DE LA BASE DE DATOS (PostgreSQL / Supabase)
--
-- Ejecutar en el editor SQL de Supabase, PASO POR PASO (cada bloque por separado).
-- Antes de empezar: avisar al equipo (la base es compartida) y desactivar el
-- traductor automático del navegador (traduce las palabras de SQL y lo rompe).
-- Si el editor avisa de RLS al crear tablas de respaldo: "Run and enable RLS".
-- =====================================================================


-- =====================================================================
-- PASO 0: respaldo dentro de la propia base (esquema "respaldo")
-- =====================================================================
create schema if not exists respaldo;

drop table if exists respaldo.cliente;              create table respaldo.cliente              as table public.cliente;
drop table if exists respaldo.usuario;              create table respaldo.usuario              as table public.usuario;
drop table if exists respaldo.encomienda;           create table respaldo.encomienda           as table public.encomienda;
drop table if exists respaldo.manifiesto;           create table respaldo.manifiesto           as table public.manifiesto;
drop table if exists respaldo.comprobante;          create table respaldo.comprobante          as table public.comprobante;
drop table if exists respaldo.incidencia;           create table respaldo.incidencia           as table public.incidencia;
drop table if exists respaldo.evento_tracking;      create table respaldo.evento_tracking      as table public.evento_tracking;
drop table if exists respaldo.solicitud_encomienda; create table respaldo.solicitud_encomienda as table public.solicitud_encomienda;
drop table if exists respaldo.bulto_encomienda;     create table respaldo.bulto_encomienda     as table public.bulto_encomienda;

-- Comprobar que cada respaldo tiene las mismas filas que su tabla original
select 'cliente' as tabla, (select count(*) from public.cliente) as original, (select count(*) from respaldo.cliente) as respaldo
union all select 'usuario',              (select count(*) from public.usuario),              (select count(*) from respaldo.usuario)
union all select 'encomienda',           (select count(*) from public.encomienda),           (select count(*) from respaldo.encomienda)
union all select 'manifiesto',           (select count(*) from public.manifiesto),           (select count(*) from respaldo.manifiesto)
union all select 'comprobante',          (select count(*) from public.comprobante),          (select count(*) from respaldo.comprobante)
union all select 'incidencia',           (select count(*) from public.incidencia),           (select count(*) from respaldo.incidencia)
union all select 'evento_tracking',      (select count(*) from public.evento_tracking),      (select count(*) from respaldo.evento_tracking)
union all select 'solicitud_encomienda', (select count(*) from public.solicitud_encomienda), (select count(*) from respaldo.solicitud_encomienda)
union all select 'bulto_encomienda',     (select count(*) from public.bulto_encomienda),     (select count(*) from respaldo.bulto_encomienda);


-- =====================================================================
-- PASO 1: diagnóstico (solo lectura). Si devuelve filas, corregirlas antes del PASO 3.
-- =====================================================================
select 'documento duplicado' as problema, num_documento::text as detalle, count(*) as cantidad
  from cliente group by num_documento having count(*) > 1
union all
select 'usuario duplicado', lower(username), count(*) from usuario group by lower(username) having count(*) > 1
union all
select 'usuario incompleto', id_usuario::text, 1 from usuario
  where username is null or password is null or rol is null or estado is null
union all
select 'rol fuera de lista', coalesce(rol,'(nulo)'), count(*) from usuario
  where upper(coalesce(rol,'')) not in ('ADMIN','SUPERVISOR','AGENTE','OPERARIO') group by rol
union all
select 'estado fuera de lista', coalesce(estado,'(nulo)'), count(*) from encomienda
  where estado is null or estado not in ('Pendiente de Pago','En origen','En tránsito','Llegó a agencia de destino',
        'Disponible para recojo','En reparto','Recogido','Entregado','Cancelado','Pendiente de pago adicional')
  group by estado
union all
select 'encomienda con datos nulos', id_encomienda::text, 1 from encomienda
  where peso is null or tarifa_base is null or id_remitente is null or id_destinatario is null or id_usuario is null
union all
select 'documento mal formado', coalesce(num_documento,'(nulo)'), 1 from cliente
  where num_documento is null or num_documento !~ '^([0-9]{8}|[0-9]{11})$';


-- =====================================================================
-- PASO 2: correcciones de datos antiguos
-- =====================================================================
-- El estado "Llegó a destino" ya no existe: ahora es "Llegó a agencia de destino"
update encomienda set estado = 'Llegó a agencia de destino' where estado = 'Llegó a destino';
-- Los roles se guardan en mayúsculas
update usuario set rol = upper(rol) where rol <> upper(rol);

-- La tabla "usuarios" (plural) es un duplicado sin uso: la aplicación usa "usuario".
-- Comprobar primero que esté vacía:  select count(*) from usuarios;
-- Si da 0:
drop table if exists public.usuarios;


-- =====================================================================
-- PASO 3: reglas de integridad e índices (todo o nada).
-- Si algún dato no cumple una regla, falla y no cambia nada.
-- Cuando se agreguen estados o tipos nuevos (v4.0), ampliar los CHECK correspondientes.
-- =====================================================================
begin;

-- Clientes
alter table cliente drop constraint if exists cliente_num_documento_uk;
alter table cliente drop constraint if exists cliente_num_documento_ck;
alter table cliente add constraint cliente_num_documento_uk unique (num_documento);
alter table cliente alter column num_documento set not null;
alter table cliente add constraint cliente_num_documento_ck check (num_documento ~ '^([0-9]{8}|[0-9]{11})$');

-- Personal
drop index if exists usuario_username_uk;
alter table usuario drop constraint if exists usuario_rol_ck;
create unique index usuario_username_uk on usuario (lower(username));
alter table usuario alter column username set not null;
alter table usuario alter column password set not null;
alter table usuario alter column rol set not null;
alter table usuario alter column estado set not null;
alter table usuario add constraint usuario_rol_ck check (rol in ('ADMIN','SUPERVISOR','AGENTE','OPERARIO'));

-- Encomiendas
alter table encomienda drop constraint if exists encomienda_peso_ck;
alter table encomienda drop constraint if exists encomienda_tarifa_ck;
alter table encomienda drop constraint if exists encomienda_estado_ck;
alter table encomienda alter column estado set not null;
alter table encomienda alter column peso set not null;
alter table encomienda alter column tarifa_base set not null;
alter table encomienda alter column id_remitente set not null;
alter table encomienda alter column id_destinatario set not null;
alter table encomienda alter column id_usuario set not null;
alter table encomienda add constraint encomienda_peso_ck check (peso > 0);
alter table encomienda add constraint encomienda_tarifa_ck check (tarifa_base >= 0);
alter table encomienda add constraint encomienda_estado_ck check (estado in
  ('Pendiente de Pago','En origen','En tránsito','Llegó a agencia de destino','Disponible para recojo',
   'En reparto','Recogido','Entregado','Cancelado','Pendiente de pago adicional'));

-- Manifiestos y solicitudes
alter table manifiesto drop constraint if exists manifiesto_capacidad_ck;
alter table solicitud_encomienda drop constraint if exists solicitud_estado_ck;
alter table solicitud_encomienda drop constraint if exists solicitud_tipo_ck;
alter table manifiesto add constraint manifiesto_capacidad_ck check (capacidad_kg is null or capacidad_kg > 0);
alter table solicitud_encomienda add constraint solicitud_estado_ck
  check (estado in ('PENDIENTE','APLICADA','APROBADA','RECHAZADA','PENDIENTE_PAGO'));
alter table solicitud_encomienda add constraint solicitud_tipo_ck
  check (tipo in ('CANCELACION','CAMBIO_DIRECCION','AGREGAR_BULTO'));

-- Índices en claves foráneas (Postgres no los crea solo)
create index if not exists idx_encomienda_remitente     on encomienda (id_remitente);
create index if not exists idx_encomienda_destinatario  on encomienda (id_destinatario);
create index if not exists idx_encomienda_manifiesto    on encomienda (id_manifiesto);
create index if not exists idx_encomienda_usuario       on encomienda (id_usuario);
create index if not exists idx_encomienda_estado        on encomienda (estado);
create index if not exists idx_evento_encomienda_fecha  on evento_tracking (id_encomienda, fecha);
create index if not exists idx_bulto_encomienda         on bulto_encomienda (id_encomienda);
create index if not exists idx_solicitud_encomienda     on solicitud_encomienda (id_encomienda);
create index if not exists idx_solicitud_cliente_estado on solicitud_encomienda (id_cliente, estado);
create index if not exists idx_incidencia_encomienda    on incidencia (id_encomienda);

commit;


-- =====================================================================
-- PASO 4 (opcional, PENDIENTE): RLS. Evita que la API pública de Supabase
-- (claves anon/authenticated) lea estas tablas. El backend conecta como
-- "postgres" y no se ve afectado. Probar el inicio de sesión después.
-- =====================================================================
-- alter table public.cliente              enable row level security;
-- alter table public.usuario              enable row level security;
-- alter table public.encomienda           enable row level security;
-- alter table public.manifiesto           enable row level security;
-- alter table public.comprobante          enable row level security;
-- alter table public.incidencia           enable row level security;
-- alter table public.evento_tracking      enable row level security;
-- alter table public.solicitud_encomienda enable row level security;
-- alter table public.bulto_encomienda     enable row level security;
