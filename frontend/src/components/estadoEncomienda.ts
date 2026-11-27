export function etiquetaEstadoEncomienda(estado: string | null | undefined): string {
  if (!estado) return 'Estado no disponible';
  if (estado.trim().toLowerCase() === 'pendiente de pago') {
    return 'Registrado · pendiente de pago';
  }
  return estado;
}
