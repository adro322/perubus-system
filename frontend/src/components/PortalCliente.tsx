import { useEffect, useState } from 'react';
import axios from 'axios';
import './PortalCliente.css';
import { etiquetaEstadoEncomienda } from './estadoEncomienda';
import ConfirmacionModal from './ConfirmacionModal';

type ClientePortal = {
  nombres: string;
  apellidos: string;
  numDocumento?: string;
};

type EventoTracking = {
  estado: string;
  detalle: string;
  ubicacion: string | null;
  fecha: string | null;
};

type BultoPortal = {
  descripcion: string | null;
  peso: number | null;
  original: boolean;
};

type EncomiendaPortal = {
  codigoTracking: string;
  estadoActual: string;
  descripcion: string | null;
  pesoTotal: number | null;
  tarifaTotal: number | null;
  bultos: BultoPortal[];
  direccionDestinatario: string | null;
  ruta: string | null;
  rolCliente: string;
  contraparte: string;
  fechaViaje: string | null;
  numeroBus: string | null;
  fechaEstimadaLlegada: string | null;
  progreso: number;
  estimacionVencida: boolean;
  historial: EventoTracking[];
};

type SolicitudPortal = {
  idSolicitud: number;
  codigoTracking: string;
  tipo: 'CANCELACION' | 'CAMBIO_DIRECCION' | 'AGREGAR_BULTO';
  direccionActual: string | null;
  direccionSolicitada: string | null;
  descripcionBulto: string | null;
  pesoBulto: number | null;
  montoAdicional: number | null;
  fechaPagoAdicional: string | null;
  estado: 'PENDIENTE' | 'PENDIENTE_PAGO' | 'APLICADA' | 'APROBADA' | 'RECHAZADA';
  respuesta: string | null;
  fechaSolicitud: string;
};

type SolicitudAConfirmar = {
  envio: EncomiendaPortal;
  tipo: 'CANCELACION' | 'CAMBIO_DIRECCION' | 'AGREGAR_BULTO';
  direccion: string;
  descripcionBulto: string;
  pesoBulto: string;
};

type RegistroForm = {
  numDocumento: string;
  nombres: string;
  apellidos: string;
  telefono: string;
  correo: string;
  password: string;
};

const api = axios.create({
  baseURL: 'http://localhost:8080/api/portal',
  withCredentials: true,
});

const formularioVacio: RegistroForm = {
  numDocumento: '',
  nombres: '',
  apellidos: '',
  telefono: '',
  correo: '',
  password: '',
};

export default function PortalCliente() {
  const [cliente, setCliente] = useState<ClientePortal | null>(null);
  const [encomiendas, setEncomiendas] = useState<EncomiendaPortal[]>([]);
  const [solicitudes, setSolicitudes] = useState<SolicitudPortal[]>([]);
  const [direccionEdicion, setDireccionEdicion] = useState<Record<string, string>>({});
  const [descripcionBultoEdicion, setDescripcionBultoEdicion] = useState<Record<string, string>>({});
  const [pesoBultoEdicion, setPesoBultoEdicion] = useState<Record<string, string>>({});
  const [solicitudEnviando, setSolicitudEnviando] = useState('');
  const [solicitudAConfirmar, setSolicitudAConfirmar] = useState<SolicitudAConfirmar | null>(null);
  const [pagoBultoAConfirmar, setPagoBultoAConfirmar] = useState<SolicitudPortal | null>(null);
  const [pagoEnviando, setPagoEnviando] = useState<number | null>(null);
  const [modoRegistro, setModoRegistro] = useState(false);
  const [formulario, setFormulario] = useState<RegistroForm>(formularioVacio);
  const [documento, setDocumento] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [aviso, setAviso] = useState('');
  const [cargando, setCargando] = useState(true);
  const [enviando, setEnviando] = useState(false);

  const cargarSesion = async () => {
    const respuesta = await api.get<ClientePortal>('/sesion');
    setCliente(respuesta.data);
    const envios = await api.get<EncomiendaPortal[]>('/mis-encomiendas');
    setEncomiendas(envios.data);
    const solicitudesCliente = await api.get<SolicitudPortal[]>('/solicitudes');
    setSolicitudes(solicitudesCliente.data);
  };

  useEffect(() => {
    let vigente = true;
    const verificarSesion = async () => {
      try {
        const respuesta = await api.get<ClientePortal>('/sesion');
        if (!vigente) return;
        setCliente(respuesta.data);
        const envios = await api.get<EncomiendaPortal[]>('/mis-encomiendas');
        if (vigente) setEncomiendas(envios.data);
        const solicitudesCliente = await api.get<SolicitudPortal[]>('/solicitudes');
        if (vigente) setSolicitudes(solicitudesCliente.data);
      } catch {
        if (vigente) setCliente(null);
      } finally {
        if (vigente) setCargando(false);
      }
    };
    void verificarSesion();
    return () => { vigente = false; };
  }, []);

  useEffect(() => {
    if (!cliente) return;
    const intervalo = window.setInterval(() => {
      void Promise.all([
        api.get<EncomiendaPortal[]>('/mis-encomiendas'),
        api.get<SolicitudPortal[]>('/solicitudes'),
      ])
        .then(([envios, solicitudesCliente]) => {
          setEncomiendas(envios.data);
          setSolicitudes(solicitudesCliente.data);
          setError('');
        })
        .catch(() => setError('No se pudo actualizar automáticamente el seguimiento.'));
    }, 60_000);
    return () => window.clearInterval(intervalo);
  }, [cliente]);

  const actualizarSeguimiento = async () => {
    setError('');
    try {
      const respuesta = await api.get<EncomiendaPortal[]>('/mis-encomiendas');
      setEncomiendas(respuesta.data);
      const solicitudesCliente = await api.get<SolicitudPortal[]>('/solicitudes');
      setSolicitudes(solicitudesCliente.data);
    } catch {
      setError('No se pudo actualizar el seguimiento. Inténtalo nuevamente.');
    }
  };

  const prepararSolicitud = (
    envio: EncomiendaPortal,
    tipo: 'CANCELACION' | 'CAMBIO_DIRECCION' | 'AGREGAR_BULTO'
  ) => {
    const direccion = direccionEdicion[envio.codigoTracking] ?? envio.direccionDestinatario ?? '';
    if (tipo === 'CAMBIO_DIRECCION' && !direccion.trim()) {
      setError('Ingresa la nueva dirección del destinatario.');
      return;
    }
    const descripcionBulto = descripcionBultoEdicion[envio.codigoTracking] ?? '';
    const pesoBulto = pesoBultoEdicion[envio.codigoTracking] ?? '';
    if (tipo === 'AGREGAR_BULTO'
      && (!descripcionBulto.trim() || !Number.isFinite(Number(pesoBulto)) || Number(pesoBulto) <= 0)) {
      setError('Ingresa una descripción y un peso mayor que cero para el bulto.');
      return;
    }
    setSolicitudAConfirmar({
      envio,
      tipo,
      direccion: direccion.trim(),
      descripcionBulto: descripcionBulto.trim(),
      pesoBulto,
    });
  };

  const enviarSolicitud = async (solicitud: SolicitudAConfirmar) => {
    setError('');
    setAviso('');
    setSolicitudEnviando(solicitud.envio.codigoTracking);
    try {
      const respuesta = await api.post<SolicitudPortal>(
        `/encomiendas/${encodeURIComponent(solicitud.envio.codigoTracking)}/solicitudes`,
        {
          tipo: solicitud.tipo,
          direccionDestinatario: solicitud.tipo === 'CAMBIO_DIRECCION' ? solicitud.direccion : null,
          descripcionBulto: solicitud.tipo === 'AGREGAR_BULTO' ? solicitud.descripcionBulto : null,
          pesoBulto: solicitud.tipo === 'AGREGAR_BULTO' ? Number(solicitud.pesoBulto) : null,
        }
      );
      setSolicitudes(actual => [respuesta.data, ...actual]);
      setAviso(respuesta.data.estado === 'APLICADA'
        ? respuesta.data.respuesta || 'La solicitud se aplicó.'
        : 'Tu solicitud fue enviada y está pendiente de revisión del personal.');
      setSolicitudAConfirmar(null);
      try {
        const [envios, solicitudesCliente] = await Promise.all([
          api.get<EncomiendaPortal[]>('/mis-encomiendas'),
          api.get<SolicitudPortal[]>('/solicitudes'),
        ]);
        setEncomiendas(envios.data);
        setSolicitudes(solicitudesCliente.data);
      } catch {
        setError('La solicitud se envió, pero no se pudo actualizar la vista. Inténtalo nuevamente.');
      }
    } catch (e: unknown) {
      setError(mensajeError(e, 'No se pudo enviar la solicitud.'));
    } finally {
      setSolicitudEnviando('');
    }
  };

  const pagarBulto = async (solicitud: SolicitudPortal) => {
    setError('');
    setAviso('');
    setPagoEnviando(solicitud.idSolicitud);
    try {
      const respuesta = await api.post<SolicitudPortal>(`/solicitudes/${solicitud.idSolicitud}/pagar`);
      setSolicitudes(actual => actual.map(item => item.idSolicitud === respuesta.data.idSolicitud
        ? respuesta.data
        : item));
      setPagoBultoAConfirmar(null);
      setAviso('El pago adicional fue confirmado. El envío ya puede asignarse a un manifiesto.');
      await actualizarSeguimiento();
    } catch (e: unknown) {
      setError(mensajeError(e, 'No se pudo confirmar el pago adicional.'));
    } finally {
      setPagoEnviando(null);
    }
  };

  const iniciarSesion = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');
    setEnviando(true);
    try {
      await api.post('/login', { numDocumento: documento, password });
      await cargarSesion();
    } catch (e: unknown) {
      setError(mensajeInicioSesion(e));
    } finally {
      setEnviando(false);
    }
  };

  const registrar = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');
    setAviso('');
    setEnviando(true);
    try {
      const respuesta = await api.post<{ mensaje: string }>('/registro', formulario);
      setAviso(respuesta.data.mensaje);
      setDocumento(formulario.numDocumento);
      setFormulario(formularioVacio);
      setModoRegistro(false);
    } catch (e: unknown) {
      setError(mensajeRegistro(e));
    } finally {
      setEnviando(false);
    }
  };

  const cerrarSesion = async () => {
    try {
      await api.post('/salir');
      setCliente(null);
      setEncomiendas([]);
      setPassword('');
      setAviso('');
    } catch {
      setError('No se pudo cerrar la sesión. Inténtalo nuevamente.');
    }
  };

  const actualizarRegistro = (campo: keyof RegistroForm, valor: string) => {
    setFormulario((actual) => ({ ...actual, [campo]: valor }));
  };

  if (cargando) {
    return <main className="portal-page"><p className="portal-loading">Cargando portal...</p></main>;
  }

  return (
    <main className="portal-page">
      <header className="portal-header">
        <a className="portal-brand" href="/">
          <span className="portal-brand-mark">PB</span>
          <span>PeruBus <strong>CARGO</strong></span>
        </a>
        {cliente && (
          <div className="portal-user">
            <span>Hola, {cliente.nombres}</span>
            <button type="button" className="portal-logout" onClick={() => void cerrarSesion()}>
              Cerrar sesión
            </button>
          </div>
        )}
      </header>

      {cliente ? (
        <section className="portal-content">
          <div className="portal-heading">
            <span className="portal-eyebrow">PORTAL DE CLIENTES</span>
            <h1>Seguimiento de tus envíos</h1>
            <p>Consulta los envíos en los que apareces como remitente o destinatario.</p>
            <button type="button" className="portal-refresh" onClick={() => void actualizarSeguimiento()}>
              Actualizar seguimiento
            </button>
          </div>

          {error && <div className="portal-alert portal-alert-error" role="alert">{error}</div>}
          {aviso && <div className="portal-alert portal-alert-success" role="status">{aviso}</div>}
          {encomiendas.length === 0 ? (
            <div className="portal-empty">
              <span className="portal-empty-icon">⌕</span>
              <h2>Aún no tienes envíos asociados</h2>
              <p>Cuando registren una encomienda con tu documento, podrás verla aquí.</p>
            </div>
          ) : (
            <div className="portal-shipment-list">
              {encomiendas.map((envio) => (
                <article className="portal-shipment" key={envio.codigoTracking}>
                  <div className="shipment-heading">
                    <div>
                      <span className="shipment-label">CÓDIGO DE TRACKING</span>
                      <h2>{envio.codigoTracking}</h2>
                    </div>
                    <span className={envio.estimacionVencida ? 'shipment-status shipment-status-late' : 'shipment-status'}>
                      {envio.estimacionVencida
                        ? `Retrasado · ${etiquetaEstadoEncomienda(envio.estadoActual)}`
                        : etiquetaEstadoEncomienda(envio.estadoActual)}
                    </span>
                  </div>
                  <div className="shipment-details">
                    <p><span>Tu rol</span>{envio.rolCliente}</p>
                    <p><span>{envio.rolCliente === 'Remitente' ? 'Destinatario' : 'Remitente'}</span>{envio.contraparte || 'No disponible'}</p>
                    <p><span>Ruta</span>{envio.ruta || 'Por confirmar'}</p>
                    <p><span>Detalle</span>{envio.descripcion || 'Encomienda'}</p>
                    <p><span>Peso total</span>{envio.pesoTotal != null ? `${envio.pesoTotal} kg` : 'No registrado'}</p>
                    <p><span>Tarifa total</span>{envio.tarifaTotal != null ? `S/ ${envio.tarifaTotal.toFixed(2)}` : 'Por calcular'}</p>
                    <p><span>Dirección de destino</span>{envio.direccionDestinatario || 'No registrada'}</p>
                    <p><span>Viaje</span>{envio.fechaViaje || 'Por asignar'}</p>
                    <p><span>Bus</span>{envio.numeroBus || 'Por asignar'}</p>
                  </div>
                  {envio.bultos?.length > 0 && (
                    <div className="shipment-parcels">
                      <h3>Bultos del envío</h3>
                      <ul>
                        {envio.bultos.map((bulto, index) => (
                          <li key={`${envio.codigoTracking}-bulto-${index}`}>
                            <span>{bulto.original ? 'Bulto original' : `Bulto adicional ${index}`}</span>
                            <strong>{bulto.descripcion || 'Sin descripción'} · {bulto.peso != null ? `${bulto.peso} kg` : 'Peso no registrado'}</strong>
                          </li>
                        ))}
                      </ul>
                    </div>
                  )}
                  {(() => {
                    const solicitudesEnvio = solicitudes.filter(
                      (solicitud) => solicitud.codigoTracking === envio.codigoTracking
                    );
                    const solicitudPendiente = solicitudesEnvio.some(
                      (solicitud) => solicitud.estado === 'PENDIENTE' || solicitud.estado === 'PENDIENTE_PAGO'
                    );
                    const solicitudEnRevision = solicitudesEnvio.some(
                      (solicitud) => solicitud.estado === 'PENDIENTE'
                    );
                    const solicitudConPagoPendiente = solicitudesEnvio.some(
                      (solicitud) => solicitud.estado === 'PENDIENTE_PAGO'
                    );
                    const terminal = ['Cancelado', 'Entregado', 'Recogido']
                      .includes(envio.estadoActual);
                    const permiteSolicitud = ['Pendiente de Pago', 'En origen']
                      .includes(envio.estadoActual);
                    return (
                      <section className="shipment-requests">
                        <h3>Solicitudes del envío</h3>
                        {solicitudesEnvio.length > 0 && (
                          <ul>
                            {solicitudesEnvio.map((solicitud) => (
                              <li key={solicitud.idSolicitud}>
                                <strong>
                                  {solicitud.tipo === 'CANCELACION'
                                    ? 'Cancelación'
                                    : solicitud.tipo === 'CAMBIO_DIRECCION' ? 'Cambio de dirección' : 'Bulto adicional'}
                                  {' · '}{etiquetaEstadoSolicitud(solicitud.estado)}
                                </strong>
                                {solicitud.tipo === 'CAMBIO_DIRECCION' && solicitud.direccionSolicitada && (
                                  <span> Dirección solicitada: {solicitud.direccionSolicitada}</span>
                                )}
                                {solicitud.tipo === 'AGREGAR_BULTO' && (
                                  <span>
                                    {' '}{solicitud.descripcionBulto || 'Bulto'} · {solicitud.pesoBulto ?? '—'} kg
                                    {solicitud.montoAdicional != null && ` · Importe adicional: S/ ${solicitud.montoAdicional.toFixed(2)}`}
                                  </span>
                                )}
                                {solicitud.respuesta && <p>{solicitud.respuesta}</p>}
                                {envio.rolCliente === 'Remitente'
                                  && solicitud.tipo === 'AGREGAR_BULTO'
                                  && solicitud.estado === 'PENDIENTE_PAGO' && (
                                  <button
                                    type="button"
                                    disabled={pagoEnviando === solicitud.idSolicitud}
                                    onClick={() => setPagoBultoAConfirmar(solicitud)}
                                  >
                                    {pagoEnviando === solicitud.idSolicitud ? 'Procesando...' : 'Pagar importe adicional'}
                                  </button>
                                )}
                              </li>
                            ))}
                          </ul>
                        )}
                        {envio.rolCliente === 'Remitente' && !terminal && permiteSolicitud && !solicitudPendiente && (
                          <div className="shipment-request-actions">
                            <label>
                              Nueva dirección del destinatario
                              <input
                                maxLength={500}
                                value={direccionEdicion[envio.codigoTracking] ?? ''}
                                placeholder={envio.direccionDestinatario || 'Ingresa la nueva dirección'}
                                onChange={(e) => setDireccionEdicion(actual => ({
                                  ...actual,
                                  [envio.codigoTracking]: e.target.value,
                                }))}
                              />
                            </label>
                            <label>
                              Descripción del bulto adicional
                              <input
                                maxLength={200}
                                value={descripcionBultoEdicion[envio.codigoTracking] ?? ''}
                                placeholder="Ej. Caja con libros"
                                onChange={(e) => setDescripcionBultoEdicion(actual => ({
                                  ...actual,
                                  [envio.codigoTracking]: e.target.value,
                                }))}
                              />
                            </label>
                            <label>
                              Peso del bulto adicional (kg)
                              <input
                                type="number"
                                min="0.01"
                                max="1000"
                                step="0.01"
                                value={pesoBultoEdicion[envio.codigoTracking] ?? ''}
                                placeholder="Ej. 2.5"
                                onChange={(e) => setPesoBultoEdicion(actual => ({
                                  ...actual,
                                  [envio.codigoTracking]: e.target.value,
                                }))}
                              />
                            </label>
                            <button
                              type="button"
                              disabled={solicitudEnviando === envio.codigoTracking
                                || !(descripcionBultoEdicion[envio.codigoTracking] ?? '').trim()
                                || !Number.isFinite(Number(pesoBultoEdicion[envio.codigoTracking]))
                                || Number(pesoBultoEdicion[envio.codigoTracking]) <= 0}
                              onClick={() => prepararSolicitud(envio, 'AGREGAR_BULTO')}
                            >
                              Solicitar agregar bulto
                            </button>
                            <button
                              type="button"
                              disabled={solicitudEnviando === envio.codigoTracking
                                || !(direccionEdicion[envio.codigoTracking] ?? '').trim()
                                || (direccionEdicion[envio.codigoTracking] ?? '').trim().toLowerCase()
                                  === (envio.direccionDestinatario ?? '').trim().toLowerCase()}
                              onClick={() => prepararSolicitud(envio, 'CAMBIO_DIRECCION')}
                            >
                              Solicitar cambio de dirección
                            </button>
                            <button
                              type="button"
                              className="request-cancel"
                              disabled={solicitudEnviando === envio.codigoTracking}
                              onClick={() => prepararSolicitud(envio, 'CANCELACION')}
                            >
                              Solicitar cancelación
                            </button>
                          </div>
                        )}
                        {solicitudEnRevision && <p className="request-pending-note">Hay una solicitud pendiente de revisión. Espera la respuesta del personal antes de enviar otra.</p>}
                        {solicitudConPagoPendiente && <p className="request-pending-note">El personal aprobó el bulto adicional. Completa el pago para continuar con el despacho del envío.</p>}
                        {envio.rolCliente !== 'Remitente' && <p>Solo el remitente puede solicitar cambios o cancelación.</p>}
                        {terminal && <p>Este envío ya está finalizado y no admite nuevas solicitudes.</p>}
                        {envio.rolCliente === 'Remitente' && !terminal && !permiteSolicitud && (
                          <p className="request-pending-note">
                            Las solicitudes de cambio o cancelación solo están disponibles antes del despacho. Contacta con la agencia para consultar otras opciones.
                          </p>
                        )}
                      </section>
                    );
                  })()}
                  <section className="shipment-progress" aria-label={`Progreso del envío: ${envio.progreso}%`}>
                    <div className="shipment-progress-heading">
                      <h3>Progreso del envío</h3>
                      <strong>{envio.progreso}%</strong>
                    </div>
                    <div className="progress-track">
                      <div className="progress-track-fill" style={{ width: `${envio.progreso}%` }} />
                      <span className="progress-truck" style={{ left: `${envio.progreso}%` }} aria-hidden="true">🚚</span>
                      <div className="progress-stops">
                        {[
                          'Registrado',
                          'En origen',
                          'En tránsito',
                          'Llegó a agencia destino',
                          envio.estadoActual === 'Disponible para recojo'
                            ? 'Disponible para recojo'
                            : envio.estadoActual === 'En reparto'
                              ? 'En reparto'
                              : 'Recojo / reparto',
                          envio.estadoActual === 'Recogido'
                            ? 'Recogido'
                            : envio.estadoActual === 'Entregado'
                              ? 'Entregado'
                              : 'Entrega / recojo',
                        ].map((hito, indice) => (
                          <span
                            className={envio.progreso >= [0, 20, 40, 60, 80, 100][indice] ? 'progress-stop active' : 'progress-stop'}
                            style={{
                              left: `${[0, 20, 40, 60, 80, 100][indice]}%`,
                              transform: indice === 0 ? 'none' : indice === 5 ? 'translateX(-100%)' : 'translateX(-50%)',
                            }}
                            key={hito}
                          >
                            <i />
                            <small>{hito}</small>
                          </span>
                        ))}
                      </div>
                    </div>
                  </section>
                  <div className={envio.estimacionVencida ? 'shipment-eta shipment-eta-late' : 'shipment-eta'}>
                    <span className="eta-icon" aria-hidden="true">◷</span>
                    <div>
                      <strong>
                        {envio.fechaEstimadaLlegada
                          ? `Llegada estimada: ${formatearFechaSimple(envio.fechaEstimadaLlegada)}`
                          : 'Fecha estimada pendiente'}
                      </strong>
                      <p>
                        {envio.estimacionVencida
                          ? 'La fecha estimada ya pasó. El estado real no cambia hasta que el personal registre el siguiente hito.'
                          : envio.fechaEstimadaLlegada
                            ? 'Fecha referencial calculada con el plazo configurado para la ruta; puede variar.'
                            : 'Se calculará cuando la encomienda tenga un viaje programado.'}
                      </p>
                    </div>
                  </div>
                  <div className="shipment-timeline">
                    <h3>Historial del envío</h3>
                    <ol>
                      {envio.historial.map((evento, indice) => (
                        <li key={`${envio.codigoTracking}-${evento.estado}-${evento.fecha ?? indice}`}>
                          <span className="timeline-dot" />
                          <div>
                            <strong>{etiquetaEstadoEncomienda(evento.estado)}</strong>
                            <p>{evento.detalle}</p>
                            {evento.ubicacion && <span>{evento.ubicacion}</span>}
                            {evento.fecha && <time dateTime={evento.fecha}>{formatearFecha(evento.fecha)}</time>}
                          </div>
                        </li>
                      ))}
                    </ol>
                  </div>
                </article>
              ))}
            </div>
          )}
        </section>
      ) : (
        <section className="portal-auth">
          <div className="portal-auth-intro">
            <span className="portal-eyebrow">PORTAL DE CLIENTES</span>
            <h1>Tu envío, siempre a la vista.</h1>
            <p>Accede al tracking de tus encomiendas como remitente o destinatario.</p>
            <div className="portal-assurance">
              <span>✓</span>
              <p>Usa tu DNI o RUC y la contraseña de tu cuenta.</p>
            </div>
          </div>
          <div className="portal-auth-card">
            <div className="portal-auth-tabs">
              <button type="button" className={!modoRegistro ? 'active' : ''} onClick={() => { setModoRegistro(false); setError(''); }}>
                Iniciar sesión
              </button>
              <button type="button" className={modoRegistro ? 'active' : ''} onClick={() => { setModoRegistro(true); setError(''); }}>
                Crear cuenta
              </button>
            </div>
            <h2>{modoRegistro ? 'Regístrate en el portal' : 'Bienvenido de nuevo'}</h2>
            <p className="portal-form-subtitle">
              {modoRegistro ? 'Crea tus credenciales para consultar tus envíos.' : 'Ingresa para consultar el estado de tus envíos.'}
            </p>
            {aviso && <div className="portal-alert portal-alert-success" role="status">{aviso}</div>}
            {error && <div className="portal-alert portal-alert-error" role="alert">{error}</div>}
            {modoRegistro ? (
              <form className="portal-form" onSubmit={(e) => void registrar(e)}>
                <label>
                  DNI o RUC
                  <input value={formulario.numDocumento} onChange={(e) => actualizarRegistro('numDocumento', e.target.value)} inputMode="numeric" minLength={8} maxLength={11} required />
                </label>
                <label>
                  Nombres o razón social
                  <input value={formulario.nombres} onChange={(e) => actualizarRegistro('nombres', e.target.value)} required />
                </label>
                <label>
                  Apellidos <span className="field-note">(no necesario para RUC)</span>
                  <input value={formulario.apellidos} onChange={(e) => actualizarRegistro('apellidos', e.target.value)} />
                </label>
                <label>
                  Correo electrónico
                  <input type="email" autoComplete="email" value={formulario.correo} onChange={(e) => actualizarRegistro('correo', e.target.value)} required />
                </label>
                <label>
                  Teléfono
                  <input type="tel" autoComplete="tel" value={formulario.telefono} onChange={(e) => actualizarRegistro('telefono', e.target.value)} maxLength={9} />
                </label>
                <label>
                  Contraseña <span className="field-note">(mínimo 8 caracteres)</span>
                  <input type="password" autoComplete="new-password" value={formulario.password} onChange={(e) => actualizarRegistro('password', e.target.value)} minLength={8} required />
                </label>
                <button className="portal-submit" type="submit" disabled={enviando}>{enviando ? 'Creando cuenta...' : 'Crear cuenta'}</button>
                <p className="portal-privacy-note">Si ya registraste un envío, usa el mismo correo que proporcionaste en la agencia.</p>
              </form>
            ) : (
              <form className="portal-form" onSubmit={(e) => void iniciarSesion(e)}>
                <label>
                  DNI o RUC
                  <input value={documento} onChange={(e) => setDocumento(e.target.value)} autoComplete="username" inputMode="numeric" minLength={8} maxLength={11} required />
                </label>
                <label>
                  Contraseña
                  <input type="password" value={password} onChange={(e) => setPassword(e.target.value)} autoComplete="current-password" required />
                </label>
                <button className="portal-submit" type="submit" disabled={enviando}>{enviando ? 'Ingresando...' : 'Ingresar al portal'}</button>
              </form>
            )}
            <a className="portal-back-link" href="/">Volver al acceso administrativo</a>
          </div>
        </section>
      )}
      {solicitudAConfirmar && (
        <ConfirmacionModal
          titulo={solicitudAConfirmar.tipo === 'CANCELACION'
            ? 'Confirmar solicitud de cancelación'
            : solicitudAConfirmar.tipo === 'CAMBIO_DIRECCION'
              ? 'Confirmar cambio de dirección'
              : 'Confirmar bulto adicional'}
          mensaje={solicitudAConfirmar.tipo === 'CANCELACION'
            ? `¿Deseas solicitar la cancelación de ${solicitudAConfirmar.envio.codigoTracking}? El envío no se cancelará hasta que el personal revise y apruebe la solicitud.`
            : solicitudAConfirmar.tipo === 'CAMBIO_DIRECCION'
              ? `¿Deseas solicitar el cambio de dirección para ${solicitudAConfirmar.envio.codigoTracking}? ${solicitudAConfirmar.envio.estadoActual === 'Pendiente de Pago' ? 'Se aplicará antes del pago.' : 'El personal debe aprobarlo porque el envío ya fue pagado.'}`
              : `¿Deseas agregar un bulto a ${solicitudAConfirmar.envio.codigoTracking}? ${solicitudAConfirmar.envio.estadoActual === 'Pendiente de Pago' ? 'Se agregará y recalculará la tarifa antes del pago.' : 'El personal deberá aprobarlo y se generará un importe adicional antes de despachar el envío.'}`}
          textoConfirmar={solicitudAConfirmar.tipo === 'CANCELACION'
            ? 'Enviar solicitud'
            : solicitudAConfirmar.tipo === 'CAMBIO_DIRECCION' ? 'Confirmar cambio' : 'Solicitar bulto'}
          peligro={solicitudAConfirmar.tipo === 'CANCELACION'}
          procesando={solicitudEnviando === solicitudAConfirmar.envio.codigoTracking}
          onCancelar={() => setSolicitudAConfirmar(null)}
          onConfirmar={() => void enviarSolicitud(solicitudAConfirmar)}
        >
          {solicitudAConfirmar.tipo === 'CAMBIO_DIRECCION' && (
            <p className="confirmation-request-detail">
              <strong>Nueva dirección:</strong> {solicitudAConfirmar.direccion}
            </p>
          )}
          {solicitudAConfirmar.tipo === 'AGREGAR_BULTO' && (
            <p className="confirmation-request-detail">
              <strong>Bulto:</strong> {solicitudAConfirmar.descripcionBulto} · {solicitudAConfirmar.pesoBulto} kg
            </p>
          )}
        </ConfirmacionModal>
      )}
      {pagoBultoAConfirmar && (
        <ConfirmacionModal
          titulo="Confirmar pago adicional"
          mensaje={`Se registrará el pago simulado de S/ ${(pagoBultoAConfirmar.montoAdicional ?? 0).toFixed(2)} por el bulto adicional. Luego el envío podrá asignarse a un manifiesto.`}
          textoConfirmar="Confirmar pago"
          procesando={pagoEnviando === pagoBultoAConfirmar.idSolicitud}
          onCancelar={() => setPagoBultoAConfirmar(null)}
          onConfirmar={() => void pagarBulto(pagoBultoAConfirmar)}
        />
      )}
    </main>
  );
}

function mensajeError(error: unknown, alternativo: string) {
  if (axios.isAxiosError<{ mensaje?: string }>(error)) {
    return error.response?.data?.mensaje || alternativo;
  }
  return alternativo;
}

function etiquetaEstadoSolicitud(estado: SolicitudPortal['estado']) {
  switch (estado) {
    case 'PENDIENTE':
      return 'Pendiente de aprobación';
    case 'PENDIENTE_PAGO':
      return 'Pendiente de pago adicional';
    case 'APLICADA':
      return 'Aplicada';
    case 'APROBADA':
      return 'Aprobada';
    case 'RECHAZADA':
      return 'Rechazada';
  }
}

function mensajeInicioSesion(error: unknown) {
  if (axios.isAxiosError<{ mensaje?: string }>(error) && error.response?.status === 401) {
    return 'Documento o contraseña incorrectos. Si aún no tienes cuenta, selecciona “Crear cuenta”. Si ya registraste una encomienda, usa el mismo correo que diste en la agencia.';
  }
  return mensajeError(error, 'No se pudo iniciar sesión. Revisa tus datos e inténtalo nuevamente.');
}

function mensajeRegistro(error: unknown) {
  if (axios.isAxiosError<{ mensaje?: string }>(error)) {
    const mensajeServidor = error.response?.data?.mensaje;
    if (error.response?.status === 409 && mensajeServidor?.toLowerCase().includes('correo no coincide')) {
      return 'El cliente está registrado, pero su ficha no tiene este correo asociado. Aún no tiene una cuenta del portal. Solicita a la agencia actualizar y verificar el correo antes de crearla.';
    }
    if (error.response?.status === 409 && mensajeServidor?.toLowerCase().includes('ya existe una cuenta')) {
      return 'Ya existe una cuenta para este documento. Vuelve a “Iniciar sesión” y usa la contraseña que registraste.';
    }
    if (mensajeServidor) {
      return mensajeServidor;
    }
  }
  return mensajeError(error, 'No se pudo crear la cuenta.');
}

function formatearFecha(fecha: string) {
  return new Intl.DateTimeFormat('es-PE', {
    dateStyle: 'medium',
    timeStyle: 'short',
  }).format(new Date(fecha));
}

function formatearFechaSimple(fecha: string) {
  const [anio, mes, dia] = fecha.split('-').map(Number);
  return new Intl.DateTimeFormat('es-PE', {
    day: 'numeric',
    month: 'long',
    year: 'numeric',
  }).format(new Date(anio, mes - 1, dia));
}
