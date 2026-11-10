import { useEffect, useState } from 'react';
import axios from 'axios';
import './PortalCliente.css';

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

type EncomiendaPortal = {
  codigoTracking: string;
  estadoActual: string;
  descripcion: string | null;
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
      void api.get<EncomiendaPortal[]>('/mis-encomiendas')
        .then((respuesta) => {
          setEncomiendas(respuesta.data);
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
    } catch {
      setError('No se pudo actualizar el seguimiento. Inténtalo nuevamente.');
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
                      {envio.estimacionVencida ? `Retrasado · ${envio.estadoActual}` : envio.estadoActual}
                    </span>
                  </div>
                  <div className="shipment-details">
                    <p><span>Tu rol</span>{envio.rolCliente}</p>
                    <p><span>{envio.rolCliente === 'Remitente' ? 'Destinatario' : 'Remitente'}</span>{envio.contraparte || 'No disponible'}</p>
                    <p><span>Ruta</span>{envio.ruta || 'Por confirmar'}</p>
                    <p><span>Detalle</span>{envio.descripcion || 'Encomienda'}</p>
                    <p><span>Viaje</span>{envio.fechaViaje || 'Por asignar'}</p>
                    <p><span>Bus</span>{envio.numeroBus || 'Por asignar'}</p>
                  </div>
                  <section className="shipment-progress" aria-label={`Progreso del envío: ${envio.progreso}%`}>
                    <div className="shipment-progress-heading">
                      <h3>Progreso del envío</h3>
                      <strong>{envio.progreso}%</strong>
                    </div>
                    <div className="progress-track">
                      <div className="progress-track-fill" style={{ width: `${envio.progreso}%` }} />
                      <span className="progress-truck" style={{ left: `${envio.progreso}%` }} aria-hidden="true">🚚</span>
                      <div className="progress-stops">
                        {['Registrado', 'En origen', 'En tránsito', 'En agencia destino', 'Entregado / recogido'].map((hito, indice) => (
                          <span
                            className={envio.progreso >= [0, 15, 50, 75, 100][indice] ? 'progress-stop active' : 'progress-stop'}
                            style={{
                              left: `${[0, 15, 50, 75, 100][indice]}%`,
                              transform: indice === 0 ? 'none' : indice === 4 ? 'translateX(-100%)' : 'translateX(-50%)',
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
                            <strong>{evento.estado}</strong>
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
    </main>
  );
}

function mensajeError(error: unknown, alternativo: string) {
  if (axios.isAxiosError<{ mensaje?: string }>(error)) {
    return error.response?.data?.mensaje || alternativo;
  }
  return alternativo;
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
