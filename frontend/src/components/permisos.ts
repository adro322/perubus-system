// Permisos del personal por rol. Es solo para mostrar u ocultar opciones:
// la protección real está en el backend (@RequiereRol), que responde 403 si el rol no corresponde.
// ADMIN y SUPERVISOR tienen acceso total.

export type Rol = 'ADMIN' | 'SUPERVISOR' | 'AGENTE' | 'OPERARIO';

export const ROLES: Rol[] = ['ADMIN', 'SUPERVISOR', 'AGENTE', 'OPERARIO'];

export const ETIQUETAS_ROL: Record<Rol, string> = {
  ADMIN: 'Administrador',
  SUPERVISOR: 'Supervisor logístico',
  AGENTE: 'Agente de carga (ventanilla)',
  OPERARIO: 'Operario de almacén',
};

export type Accion =
  | 'registrar_encomienda' // RF-01 y cotizador RF-02
  | 'pagar'
  | 'notificaciones'       // solicitudes de clientes
  | 'registrar_cliente'
  | 'gestionar_acceso_portal' // generar acceso o restablecer la clave de un cliente
  | 'bloquear_cuentas'        // bloquear cuentas del portal: solo ADMIN y SUPERVISOR
  | 'ver_historial'
  | 'actualizar_estado'    // RF-06
  | 'ver_manifiestos'
  | 'asignar_manifiesto'
  | 'crear_manifiestos'    // RF-07: solo ADMIN y SUPERVISOR
  | 'gestionar_incidencias' // reporte y resolución de incidencias operativas
  | 'gestionar_clientes'   // CRUD de clientes
  | 'gestionar_personal'   // CRUD de personal
  | 'ver_estadisticas';    // Panel de estadísticas y reportes: solo ADMIN y SUPERVISOR

// Roles que pueden hacer cada acción, además de ADMIN y SUPERVISOR
const PERMISOS: Record<Accion, Rol[]> = {
  registrar_encomienda: ['AGENTE'],
  pagar: ['AGENTE'],
  notificaciones: ['AGENTE'],
  registrar_cliente: ['AGENTE'],
  gestionar_acceso_portal: ['AGENTE'],
  bloquear_cuentas: [],
  ver_historial: ['AGENTE', 'OPERARIO'],
  actualizar_estado: ['OPERARIO'],
  ver_manifiestos: ['OPERARIO'],
  asignar_manifiesto: ['OPERARIO'],
  crear_manifiestos: [],
  gestionar_incidencias: ['AGENTE', 'OPERARIO'],
  gestionar_clientes: [],
  gestionar_personal: [],
  ver_estadisticas: [],
};

export const puede = (rol: string, accion: Accion): boolean => {
  const actual = (rol || '').toUpperCase();
  if (actual === 'ADMIN' || actual === 'SUPERVISOR') return true;
  return (PERMISOS[accion] as string[]).includes(actual);
};

// Qué acción da acceso a cada pantalla del Dashboard (en orden de prioridad para la pantalla inicial)
const VISTAS: [string, Accion][] = [
  ['registro_encomienda', 'registrar_encomienda'],
  ['lista_encomiendas', 'ver_historial'],
  ['notificaciones', 'notificaciones'],
  ['registro_cliente', 'registrar_cliente'],
  ['accesos_portal', 'gestionar_acceso_portal'],
  ['lista_clientes', 'gestionar_clientes'],
  ['lista_manifiestos', 'ver_manifiestos'],
  ['incidencias', 'gestionar_incidencias'],
  ['personal', 'gestionar_personal'],
  ['estadisticas', 'ver_estadisticas'],
];

export const vistaPermitida = (rol: string, vista: string): boolean => {
  const entrada = VISTAS.find(([nombre]) => nombre === vista);
  return entrada ? puede(rol, entrada[1]) : false;
};

export const vistaInicial = (rol: string): string =>
  VISTAS.find(([, accion]) => puede(rol, accion))?.[0] ?? 'lista_encomiendas';
