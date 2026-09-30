import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import axios from 'axios';

export default function Dashboard() {
  const navigate = useNavigate();
  const [usuarioId] = useState(() => Number(sessionStorage.getItem('idUsuario')));
  const [vistaActual, setVistaActual] = useState('registro_encomienda'); 
  const [mensaje, setMensaje] = useState('');

  // Estados Cliente
  const [clienteForm, setClienteForm] = useState({ numDocumento: '', nombres: '', apellidos: '', telefono: '', correo: '' });
  const [listaClientes, setListaClientes] = useState([]);
  
  // Estados Encomienda
  const [dniBuscado, setDniBuscado] = useState('');
  const [clienteEncontrado, setClienteEncontrado] = useState<any>(null);
  const [errorBusqueda, setErrorBusqueda] = useState('');
  const [peso, setPeso] = useState('');
  const [destino, setDestino] = useState('');
  const [tarifa, setTarifa] = useState<number | null>(null);
  const [listaEncomiendas, setListaEncomiendas] = useState<any[]>([]);
  const [pagoSeleccionado, setPagoSeleccionado] = useState<any>(null);
  const [pagoProcesando, setPagoProcesando] = useState(false);
  const [trackingBuscado, setTrackingBuscado] = useState('');
  const [encomiendaConsultada, setEncomiendaConsultada] = useState<any>(null);
  const [resumenRegistrado, setResumenRegistrado] = useState<any>(null);
  const [errorConsulta, setErrorConsulta] = useState('');
  const [destinatarioForm, setDestinatarioForm] = useState({ numDocumento: '', nombres: '', apellidos: '', telefono: '', correo: '' });
  const [descripcion, setDescripcion] = useState('');

  useEffect(() => {
    if (vistaActual === 'lista_clientes') cargarClientes();
    if (vistaActual === 'lista_encomiendas') cargarEncomiendas();
  }, [vistaActual]);

  const cargarClientes = async () => {
    try {
      const res = await axios.get('http://localhost:8080/api/clientes');
      setListaClientes(res.data);
    } catch (error) { console.error("Error al cargar clientes"); }
  };

  const cargarEncomiendas = async () => {
    try {
      const res = await axios.get('http://localhost:8080/api/encomiendas');
      setListaEncomiendas(res.data);
    } catch (error) { console.error("Error al cargar encomiendas"); }
  };

  const confirmarPago = async () => {
    if (!pagoSeleccionado || pagoProcesando) return;
    setPagoProcesando(true);
    try {
      const res = await axios.post(`http://localhost:8080/api/encomiendas/${encodeURIComponent(pagoSeleccionado.codigoTracking)}/simular-pago`);
      setListaEncomiendas((actual: any[]) => actual.map(encomienda =>
        encomienda.codigoTracking === res.data.codigoTracking
          ? { ...encomienda, estadoLogistico: res.data.estadoLogistico }
          : encomienda
      ));
      setPagoSeleccionado(null);
      mostrarMensaje(`Pago simulado registrado para ${res.data.codigoTracking}`);
    } catch (error: any) {
      alert(error.response?.data?.mensaje || "No se pudo registrar el pago simulado.");
    } finally {
      setPagoProcesando(false);
    }
  };

  const consultarEncomienda = async () => {
    const trackingNormalizado = trackingBuscado.trim().toUpperCase();
    setErrorConsulta('');
    setEncomiendaConsultada(null);

    if (!/^PERU-[A-Z0-9]{8}$/.test(trackingNormalizado)) {
      setErrorConsulta('Ingrese un código con el formato PERU-XXXXXXXX.');
      return;
    }

    try {
      const res = await axios.get(`http://localhost:8080/api/encomiendas/buscar/${trackingNormalizado}`);
      setEncomiendaConsultada(res.data);
    } catch (error: any) {
      setErrorConsulta(error.response?.status === 404
        ? 'No se encontró una encomienda con ese tracking.'
        : 'No se pudo consultar la encomienda.');
    }
  };

  const imprimirEncomienda = () => {
    if (encomiendaConsultada) {
      window.print();
    }
  };

  const limpiarConsulta = () => {
    setTrackingBuscado('');
    setEncomiendaConsultada(null);
    setErrorConsulta('');
  };

  const iniciarNuevaEncomienda = () => {
    setResumenRegistrado(null);
    setPeso('');
    setDniBuscado('');
    setClienteEncontrado(null);
    setTarifa(null);
    setDestino('');
    setDescripcion('');
    setDestinatarioForm({
      numDocumento: '',
      nombres: '',
      apellidos: '',
      telefono: '',
      correo: '',
    });
    setErrorBusqueda('');
    setMensaje('');
    setVistaActual('registro_encomienda');
  };

  const buscarCliente = async () => {
    setErrorBusqueda(''); 
    if (!dniBuscado) {
      setErrorBusqueda("Ingrese un DNI para buscar");
      return;
    }
    
    try {
      const res = await axios.get(`http://localhost:8080/api/clientes/buscar/${dniBuscado}`);
      if (res.data) {
        setClienteEncontrado(res.data);
      } else {
        setErrorBusqueda("Cliente no encontrado. Debe registrarlo primero.");
        setClienteEncontrado(null);
      }
    } catch (error) {
      setErrorBusqueda("Cliente no encontrado en la base de datos.");
      setClienteEncontrado(null);
    }
  };

  const registrarCliente = async (e: React.FormEvent) => {
    e.preventDefault();
    try {
      await axios.post('http://localhost:8080/api/clientes/registrar', clienteForm);
      mostrarMensaje('Cliente registrado exitosamente');
      setClienteForm({ numDocumento: '', nombres: '', apellidos: '', telefono: '', correo: '' });
    } catch (error) { alert("Error al guardar cliente"); }
  };

  const calcularTarifa = async () => {
    if (!peso || parseFloat(peso) <= 0) return alert("Ingrese un peso válido");
    const [origen, destinoRuta] = destino.split('-');
    if (!origen || !destinoRuta) return alert("Seleccione una ruta válida");
    try {
      const res = await axios.post('http://localhost:8080/api/encomiendas/cotizar', {
        origen,
        destino: destinoRuta,
        peso: parseFloat(peso),
      });
      setTarifa(res.data.costoEstimado);
    } catch (error) { alert("Error al cotizar"); }
  };

  const registrarEncomienda = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!clienteEncontrado) return alert("Primero busque y valide un remitente válido");
    if (tarifa === null) return alert("Debe calcular la tarifa antes de registrar");
    if (!Number.isInteger(usuarioId) || usuarioId <= 0) return alert("La sesión no contiene un usuario válido. Inicie sesión nuevamente.");

    try {
      const res = await axios.post('http://localhost:8080/api/encomiendas/registrar', {
        peso: parseFloat(peso),
        descripcion: descripcion,
        tarifaBase: tarifa,
        remitente: { numDocumento: clienteEncontrado.numDocumento },
        destinatario: { 
            numDocumento: destinatarioForm.numDocumento,
            nombres: destinatarioForm.nombres,
            apellidos: destinatarioForm.apellidos,
            telefono: destinatarioForm.telefono,
            correo: destinatarioForm.correo 
        },
        usuario: { idUsuario: usuarioId } 
      });
      setEncomiendaConsultada(null);
      setResumenRegistrado(res.data);
      mostrarMensaje(`Tracking generado: ${res.data.codigoTracking}`);
      
      setPeso(''); setDniBuscado(''); setClienteEncontrado(null); setTarifa(null); setDestino('');
      setDescripcion(''); setDestinatarioForm({ numDocumento: '', nombres: '', apellidos: '', telefono: '', correo: '' });
      
    } catch (error) { 
        alert("Error al registrar la encomienda. Verifica la consola."); 
        console.error(error);
    }
  };

  const mostrarMensaje = (texto: string) => {
    setMensaje(texto);
    setTimeout(() => setMensaje(''), 4000);
  };

  return (
    <>
    <div className="app-shell" style={{ display: 'flex', height: '100vh', backgroundColor: '#F4F7FA', fontFamily: "'Inter', 'Segoe UI', sans-serif" }}>
      
      <div style={{ width: '280px', backgroundColor: '#1E1E2D', color: '#A2A3B7', display: 'flex', flexDirection: 'column' }}>
        <div style={{  textAlign: 'center' }}>
            <img src="/src/assets/logo.png" alt="Logo PerúBus" style={{ width: '260px', maxWidth: '100%', padding: '20px' }} />
          </div>

        <div style={{ padding: '20px', display: 'flex', flexDirection: 'column', gap: '8px', flex: 1 }}>
          <div style={{ fontSize: '11px', textTransform: 'uppercase', letterSpacing: '1px', marginBottom: '10px', color: '#666' }}>Recepción</div>
          <button onClick={() => setVistaActual('registro_encomienda')} style={obtenerEstiloMenu(vistaActual === 'registro_encomienda')}>Nueva Encomienda</button>
          <button onClick={() => setVistaActual('lista_encomiendas')} style={obtenerEstiloMenu(vistaActual === 'lista_encomiendas')}>Historial Encomiendas</button>
          
          <div style={{ fontSize: '11px', textTransform: 'uppercase', letterSpacing: '1px', marginBottom: '10px', marginTop: '20px', color: '#666' }}>Directorio</div>
          <button onClick={() => setVistaActual('registro_cliente')} style={obtenerEstiloMenu(vistaActual === 'registro_cliente')}>Nuevo Cliente</button>
          <button onClick={() => setVistaActual('lista_clientes')} style={obtenerEstiloMenu(vistaActual === 'lista_clientes')}>Cartera de Clientes</button>
        </div>

        <button onClick={() => { sessionStorage.removeItem('idUsuario'); navigate('/'); }} style={{ padding: '20px', backgroundColor: '#1a1a27', color: '#ff4d4d', border: 'none', cursor: 'pointer', borderTop: '1px solid #2B2B40', textAlign: 'left', fontWeight: 'bold' }}>
          Cerrar Sesión
        </button>
      </div>

      <div style={{ flex: 1, padding: '40px', overflowY: 'auto' }}>
        
        {mensaje && (
          <div style={{ padding: '15px 20px', backgroundColor: '#E8F5E9', color: '#2E7D32', borderRadius: '8px', marginBottom: '20px', boxShadow: '0 2px 4px rgba(0,0,0,0.05)', borderLeft: '4px solid #4CAF50', fontWeight: '500' }}>
            {mensaje}
          </div>
        )}

        <div style={{ backgroundColor: 'white', padding: '40px', borderRadius: '16px', boxShadow: '0 4px 20px rgba(0,0,0,0.03)', maxWidth: '900px', margin: '0 auto' }}>
          
          {/* =========================================
              VISTA 1: REGISTRAR ENCOMIENDA (Principal) 
             ========================================= */}
          {vistaActual === 'registro_encomienda' && (
            <div>
              {resumenRegistrado && (
                <div style={{ padding: '20px', border: '1px solid #4CAF50', borderRadius: '8px', marginTop: '10px', marginBottom: '20px' }}>
                  <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '15px' }}>
                    <h3 style={{ color: '#1E1E2D' }}>Resumen de encomienda registrada</h3>
                    <div style={{ display: 'flex', gap: '8px' }}>
                      <button type="button" onClick={() => window.print()} style={btnSecondary}>Imprimir / PDF</button>
                      <button type="button" onClick={iniciarNuevaEncomienda} style={btnLight}>Nueva encomienda</button>
                    </div>
                  </div>
                  <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '10px', color: '#3F4254' }}>
                    <div><strong>Tracking:</strong> {resumenRegistrado.codigoTracking}</div>
                    <div><strong>Estado:</strong> {resumenRegistrado.estadoLogistico}</div>
                    <div><strong>Peso:</strong> {resumenRegistrado.peso} Kg</div>
                    <div><strong>Tarifa:</strong> S/ {Number(resumenRegistrado.tarifaBase).toFixed(2)}</div>
                    <div><strong>Descripción:</strong> {resumenRegistrado.descripcion}</div>
                  </div>
                </div>
              )}

              {!resumenRegistrado && (
                <>
              <h2 style={{ color: '#1E1E2D', marginBottom: '30px', fontSize: '24px' }}>Recepción de Encomienda</h2>
              
              <div style={{ padding: '20px', backgroundColor: '#F8F9FA', borderRadius: '8px', border: '1px solid #E4E6EF', marginBottom: '25px' }}>
                <label style={labelStyle}>Paso 1: Identificar Cliente (Remitente)</label>
                
                {/* RECUADRO ROJO DE ERROR (Igual al del Login) */}
                {errorBusqueda && (
                  <div style={{ padding: '12px', backgroundColor: '#ffebee', color: '#c62828', fontSize: '13px', borderRadius: '6px', marginTop: '10px', textAlign: 'center', fontWeight: '500' }}>
                    {errorBusqueda}
                  </div>
                )}

                <div style={{ display: 'flex', gap: '15px', marginTop: '10px' }}>
                  <input 
                    type="text" 
                    placeholder="Ingrese DNI del cliente" 
                    maxLength={8} /* <-- Límite de 8 caracteres */
                    value={dniBuscado} 
                    onChange={e => {
                      // Reemplaza cualquier cosa que NO sea un número por vacío
                      const soloNumeros = e.target.value.replace(/\D/g, ''); 
                      setDniBuscado(soloNumeros);
                    }} 
                    style={{...inputStyle, flex: 1}} 
                    disabled={!!clienteEncontrado} 
                  />
                  
                  {!clienteEncontrado && (
                    <button type="button" onClick={buscarCliente} style={btnSecondary}>Buscar</button>
                  )}

                  {(clienteEncontrado || dniBuscado.length > 0) && (
                    <button 
                      type="button" 
                      onClick={() => { setDniBuscado(''); setClienteEncontrado(null); setTarifa(null); setPeso(''); setErrorBusqueda(''); }} 
                      style={{ padding: '12px 18px', backgroundColor: '#F4F6F8', color: '#D32F2F', border: '1px solid #E4E6EF', borderRadius: '6px', cursor: 'pointer', fontWeight: 'bold', fontSize: '16px' }}
                      title="Limpiar búsqueda"
                    >
                      ✕
                    </button>
                  )}
                </div>
                
                {/* Mostrar datos del cliente como lectura */}
                {clienteEncontrado && (
                  <div style={{ marginTop: '15px', padding: '15px', backgroundColor: 'white', borderRadius: '6px', border: '1px solid #D32F2F', display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '10px' }}>
                    <div><span style={{color: '#888', fontSize: '12px'}}>Nombres:</span><br/><strong>{clienteEncontrado.nombres} {clienteEncontrado.apellidos}</strong></div>
                    <div><span style={{color: '#888', fontSize: '12px'}}>Contacto:</span><br/><strong>{clienteEncontrado.telefono} | {clienteEncontrado.correo}</strong></div>
                  </div>
                )}
              </div>

              {clienteEncontrado && (
                <form onSubmit={registrarEncomienda} style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '20px' }}>
                  <div style={{ gridColumn: 'span 2' }}><label style={labelStyle}>Ruta / Destino</label>
                    <select required value={destino} onChange={e => { setDestino(e.target.value); setTarifa(null); }} style={inputStyle}>
                      <option value="">Seleccione ruta de envío...</option>
                      <option value="Lima-Ica">Lima - Ica</option>
                      <option value="Lima-Nazca">Lima - Nazca</option>
                      <option value="Ica-Nazca">Ica - Nazca</option>
                      <option value="Lima-Pisco">Lima - Pisco</option>
                      <option value="Ica-Pisco">Ica - Pisco</option>
                    </select>
                  </div>
                  <div style={{ gridColumn: 'span 2', padding: '15px', backgroundColor: '#fff', border: '1px dashed #D32F2F', borderRadius: '6px' }}>
                    <label style={{...labelStyle, color: '#D32F2F'}}>Paso 2: Datos del Destinatario</label>
                    <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr 1fr', gap: '15px', marginTop: '10px' }}>
                      <input type="text" placeholder="DNI Destinatario" maxLength={8} required value={destinatarioForm.numDocumento} onChange={e => setDestinatarioForm({...destinatarioForm, numDocumento: e.target.value.replace(/\D/g, '')})} style={inputStyle} />
                      <input type="text" placeholder="Nombres" required value={destinatarioForm.nombres} onChange={e => setDestinatarioForm({...destinatarioForm, nombres: e.target.value})} style={inputStyle} />
                      <input type="text" placeholder="Apellidos" required value={destinatarioForm.apellidos} onChange={e => setDestinatarioForm({...destinatarioForm, apellidos: e.target.value})} style={inputStyle} />
                      <input type="text" placeholder="Teléfono" maxLength={9} required value={destinatarioForm.telefono} onChange={e => setDestinatarioForm({...destinatarioForm, telefono: e.target.value.replace(/\D/g, '')})} style={inputStyle} />
                      <input type="email" placeholder="Correo (Opcional)" value={destinatarioForm.correo} onChange={e => setDestinatarioForm({...destinatarioForm, correo: e.target.value})} style={inputStyle} />
                    </div>
                  </div>

                  {/* --- NUEVO BLOQUE: DESCRIPCIÓN --- */}
                  <div style={{ gridColumn: 'span 2' }}>
                    <label style={labelStyle}>Descripción del Paquete</label>
                    <input type="text" placeholder="Ej: Caja con repuestos, Documentos, etc." required value={descripcion} onChange={e => setDescripcion(e.target.value)} style={inputStyle} />
                  </div>

                  <div style={{ gridColumn: 'span 2' }}>
                    <label style={labelStyle}>Peso del Paquete (Kg)</label>
                    <div style={{ display: 'flex', gap: '15px' }}>
                      <input type="number" step="0.01" required value={peso} onChange={e => { setPeso(e.target.value); setTarifa(null); }} style={{...inputStyle, flex: 1}} />
                      <button type="button" onClick={calcularTarifa} style={btnSecondary}>Calcular Tarifa</button>
                    </div>
                  </div>

                  {tarifa !== null && (
                    <div style={{ gridColumn: 'span 2', padding: '20px', backgroundColor: '#F8F9FA', borderLeft: '5px solid #1E1E2D', fontSize: '20px', borderRadius: '4px' }}>
                      Tarifa Total a Cobrar: <strong style={{ color: '#D32F2F' }}>S/ {tarifa.toFixed(2)}</strong>
                    </div>
                  )}

                  <button type="submit" style={{ ...btnPrimary, gridColumn: 'span 2', marginTop: '10px' }}>Confirmar y Registrar Encomienda</button>

                </form>
              )}
                </>
              )}
            </div>
          )}

          {/* =========================================
              VISTA 2: REGISTRAR CLIENTE 
             ========================================= */}
          {vistaActual === 'registro_cliente' && (
            <form onSubmit={registrarCliente} style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '20px' }}>
              <h2 style={{ gridColumn: 'span 2', color: '#1E1E2D', marginBottom: '10px', fontSize: '24px' }}>Nuevo Cliente</h2>
              
              <div><label style={labelStyle}>Documento (DNI)</label><input type="text" maxLength={8} required value={clienteForm.numDocumento} onChange={e => setClienteForm({...clienteForm, numDocumento: e.target.value})} style={inputStyle} /></div>
              <div><label style={labelStyle}>Teléfono Móvil</label><input type="text" maxLength={9} required value={clienteForm.telefono} onChange={e => setClienteForm({...clienteForm, telefono: e.target.value})} style={inputStyle} /></div>
              <div><label style={labelStyle}>Nombres</label><input type="text" required value={clienteForm.nombres} onChange={e => setClienteForm({...clienteForm, nombres: e.target.value})} style={inputStyle} /></div>
              <div><label style={labelStyle}>Apellidos</label><input type="text" required value={clienteForm.apellidos} onChange={e => setClienteForm({...clienteForm, apellidos: e.target.value})} style={inputStyle} /></div>
              <div style={{ gridColumn: 'span 2' }}><label style={labelStyle}>Correo Electrónico</label><input type="email" value={clienteForm.correo} onChange={e => setClienteForm({...clienteForm, correo: e.target.value})} style={inputStyle} /></div>
              
              <button type="submit" style={{ ...btnPrimary, gridColumn: 'span 2' }}>Registrar Cliente</button>
            </form>
          )}

          {/* =========================================
              VISTA 3: LISTA DE CLIENTES 
             ========================================= */}
          {vistaActual === 'lista_clientes' && (
            <div>
              <h2 style={{ color: '#1E1E2D', marginBottom: '20px', fontSize: '24px' }}>Directorio de Clientes</h2>
              <table style={tableStyle}>
                <thead><tr><th style={thStyle}>DNI</th><th style={thStyle}>Cliente</th><th style={thStyle}>Teléfono</th><th style={thStyle}>Correo</th></tr></thead>
                <tbody>
                  {listaClientes.map((c: any) => (
                    <tr key={c.idCliente} style={trStyle}>
                      <td style={tdStyle}>{c.numDocumento}</td>
                      <td style={tdStyle}>{c.nombres} {c.apellidos}</td>
                      <td style={tdStyle}>{c.telefono}</td>
                      <td style={tdStyle}>{c.correo}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}

          {/* =========================================
              VISTA 4: LISTA DE ENCOMIENDAS 
             ========================================= */}
          {vistaActual === 'lista_encomiendas' && (
            <div>
              <h2 style={{ color: '#1E1E2D', marginBottom: '20px', fontSize: '24px' }}>Historial de Encomiendas</h2>
              <div style={{ display: 'flex', gap: '12px', marginBottom: '20px' }}>
                <input
                  type="text"
                  value={trackingBuscado}
                  maxLength={13}
                  placeholder="Consultar tracking: PERU-XXXXXXXX"
                  onChange={e => setTrackingBuscado(e.target.value.replace(/[^a-zA-Z0-9-]/g, '').toUpperCase())}
                  style={{ ...inputStyle, flex: 1 }}
                />
                <button type="button" onClick={consultarEncomienda} style={btnSecondary}>Consultar</button>
              </div>
              {errorConsulta && (
                <div style={{ padding: '12px', backgroundColor: '#ffebee', color: '#c62828', borderRadius: '6px', marginBottom: '20px' }}>
                  {errorConsulta}
                </div>
              )}
              {encomiendaConsultada && (
                <div style={{ padding: '20px', border: '1px solid #D32F2F', borderRadius: '8px', marginBottom: '25px' }}>
                  <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '15px' }}>
                    <h3 style={{ color: '#1E1E2D' }}>Detalle de {encomiendaConsultada.codigoTracking}</h3>
                    <div style={{ display: 'flex', gap: '8px' }}>
                      <button type="button" onClick={imprimirEncomienda} style={btnSecondary}>Imprimir / PDF</button>
                      <button type="button" onClick={limpiarConsulta} style={btnLight}>Limpiar consulta</button>
                    </div>
                  </div>
                  <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '10px', color: '#3F4254' }}>
                    <div><strong>Estado:</strong> {encomiendaConsultada.estadoLogistico}</div>
                    <div><strong>Peso:</strong> {encomiendaConsultada.peso} Kg</div>
                    <div><strong>Descripción:</strong> {encomiendaConsultada.descripcion}</div>
                    <div><strong>Tarifa:</strong> S/ {Number(encomiendaConsultada.tarifaBase).toFixed(2)}</div>
                    <div><strong>Remitente:</strong> {encomiendaConsultada.remitente?.nombres} {encomiendaConsultada.remitente?.apellidos}</div>
                    <div><strong>Destinatario:</strong> {encomiendaConsultada.destinatario?.nombres} {encomiendaConsultada.destinatario?.apellidos}</div>
                    {encomiendaConsultada.ruta && <div><strong>Ruta:</strong> {encomiendaConsultada.ruta}</div>}
                    {encomiendaConsultada.fechaViaje && <div><strong>Viaje:</strong> {encomiendaConsultada.fechaViaje}</div>}
                  </div>
                </div>
              )}
              <table style={tableStyle}>
                <thead>
                  <tr>
                    <th style={thStyle}>Tracking</th>
                    <th style={thStyle}>Remitente</th> 
                    <th style={thStyle}>Peso</th>
                    <th style={thStyle}>Estado</th>
                    <th style={thStyle}>Acciones</th>
                  </tr>
                </thead>
                <tbody>
                  {listaEncomiendas.map((enc: any) => (
                    <tr key={enc.codigoTracking} style={trStyle}>
                      <td style={{...tdStyle, fontWeight: 'bold', color: '#D32F2F'}}>{enc.codigoTracking}</td>
                      
                      <td style={tdStyle}>
                        {enc.remitente ? `${enc.remitente.nombres} ${enc.remitente.apellidos || ''}` : 'Sin datos'}
                      </td>
                      
                      <td style={tdStyle}>{enc.peso} Kg</td>
                      <td style={tdStyle}>
                        <span style={{ backgroundColor: '#E8F5E9', color: '#2E7D32', padding: '4px 8px', borderRadius: '4px', fontSize: '12px' }}>
                          {enc.estadoLogistico}
                        </span>
                      </td>
                      <td style={tdStyle}>
                        <div style={{ display: 'flex', gap: '8px', alignItems: 'center' }}>
                          {enc.estadoLogistico?.toLowerCase() === 'pendiente de pago' && (
                            <button type="button" onClick={() => setPagoSeleccionado(enc)} style={{ padding: '6px 12px', backgroundColor: '#D32F2F', color: 'white', border: '1px solid #D32F2F', borderRadius: '4px', fontSize: '12px', cursor: 'pointer' }}>
                              Pagar
                            </button>
                          )}
                          <button
                            type="button"
                            disabled={enc.estadoLogistico?.toLowerCase() !== 'en origen'}
                            onClick={() => mostrarMensaje(`La encomienda ${enc.codigoTracking} está lista para asignarle un manifiesto.`)}
                            style={{ padding: '6px 12px', backgroundColor: '#F3F6F9', color: enc.estadoLogistico?.toLowerCase() === 'en origen' ? '#3F4254' : '#B5B5C3', border: '1px solid #E4E6EF', borderRadius: '4px', fontSize: '12px', cursor: enc.estadoLogistico?.toLowerCase() === 'en origen' ? 'pointer' : 'not-allowed' }}
                          >
                            Asignar Manifiesto{enc.estadoLogistico?.toLowerCase() !== 'en origen' ? ' 🔒' : ''}
                          </button>
                        </div>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}

        </div>
      </div>
    </div>
    {pagoSeleccionado && (() => {
      const subtotal = Number(pagoSeleccionado.tarifaBase || 0);
      const igv = Math.round((subtotal * 0.18 + Number.EPSILON) * 100) / 100;
      const total = subtotal + igv;
      return (
        <div
          onClick={() => !pagoProcesando && setPagoSeleccionado(null)}
          style={{ position: 'fixed', inset: 0, zIndex: 1000, backgroundColor: 'rgba(20, 24, 32, 0.58)', display: 'grid', placeItems: 'center', padding: '20px' }}
        >
          <section
            role="dialog"
            aria-modal="true"
            aria-labelledby="pago-title"
            onClick={event => event.stopPropagation()}
            style={{ width: '100%', maxWidth: '460px', backgroundColor: 'white', borderRadius: '8px', padding: '26px', boxShadow: '0 16px 48px rgba(0,0,0,0.24)', color: '#3F4254' }}
          >
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'start', gap: '16px', marginBottom: '20px' }}>
              <div>
                <h2 id="pago-title" style={{ color: '#1E1E2D', margin: 0, fontSize: '20px' }}>Simular pago</h2>
                <p style={{ margin: '6px 0 0', color: '#777' }}>Encomienda {pagoSeleccionado.codigoTracking}</p>
              </div>
              <button type="button" aria-label="Cerrar" title="Cerrar" disabled={pagoProcesando} onClick={() => setPagoSeleccionado(null)} style={{ border: 0, background: 'transparent', fontSize: '22px', color: '#666', cursor: 'pointer' }}>×</button>
            </div>
            <div style={{ display: 'grid', gap: '12px', padding: '16px 0', borderTop: '1px solid #E4E6EF', borderBottom: '1px solid #E4E6EF' }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', gap: '12px' }}><span>Cliente</span><strong>{pagoSeleccionado.remitente?.nombres} {pagoSeleccionado.remitente?.apellidos}</strong></div>
              <div style={{ display: 'flex', justifyContent: 'space-between' }}><span>Descripción</span><strong>{pagoSeleccionado.descripcion || 'Encomienda'}</strong></div>
              <div style={{ display: 'flex', justifyContent: 'space-between' }}><span>Subtotal</span><strong>S/ {subtotal.toFixed(2)}</strong></div>
              <div style={{ display: 'flex', justifyContent: 'space-between' }}><span>IGV (18%)</span><strong>S/ {igv.toFixed(2)}</strong></div>
              <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '18px', color: '#1E1E2D' }}><strong>Total</strong><strong>S/ {total.toFixed(2)}</strong></div>
            </div>
            <p style={{ fontSize: '12px', color: '#777', lineHeight: 1.5 }}>Pago simulado. Se guardará el desglose y la fecha para la futura emisión del comprobante.</p>
            <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '10px', marginTop: '20px' }}>
              <button type="button" disabled={pagoProcesando} onClick={() => setPagoSeleccionado(null)} style={{ padding: '10px 14px', backgroundColor: 'white', color: '#3F4254', border: '1px solid #E4E6EF', borderRadius: '4px', cursor: pagoProcesando ? 'not-allowed' : 'pointer' }}>Cancelar</button>
              <button type="button" disabled={pagoProcesando} onClick={confirmarPago} style={{ padding: '10px 14px', backgroundColor: '#D32F2F', color: 'white', border: 0, borderRadius: '4px', fontWeight: 600, cursor: pagoProcesando ? 'wait' : 'pointer' }}>{pagoProcesando ? 'Procesando...' : 'Confirmar pago'}</button>
            </div>
          </section>
        </div>
      );
    })()}
    {(encomiendaConsultada || resumenRegistrado) && (() => {
      const reporte = resumenRegistrado || encomiendaConsultada;
      return (
        <div className="ticket-impresion">
          <h1>PeruBus Cargo</h1>
          <p className="ticket-titulo">Comprobante de encomienda</p>
          <div className="ticket-linea" />
          <p><strong>Tracking:</strong> {reporte.codigoTracking}</p>
          <p><strong>Estado:</strong> {reporte.estadoLogistico}</p>
          <p><strong>Peso:</strong> {reporte.peso} Kg</p>
          <p><strong>Tarifa:</strong> S/ {Number(reporte.tarifaBase).toFixed(2)}</p>
          <p><strong>Descripción:</strong> {reporte.descripcion}</p>
          {reporte.remitente && <p><strong>Remitente:</strong> {reporte.remitente.nombres} {reporte.remitente.apellidos}</p>}
          {reporte.destinatario && <p><strong>Destinatario:</strong> {reporte.destinatario.nombres} {reporte.destinatario.apellidos}</p>}
          {reporte.ruta && <p><strong>Ruta:</strong> {reporte.ruta}</p>}
          {reporte.fechaViaje && <p><strong>Viaje:</strong> {reporte.fechaViaje}</p>}
          <div className="ticket-linea" />
          <p className="ticket-pie">Conserve este comprobante para consultar el estado de su encomienda.</p>
        </div>
      );
    })()}
    </>
  );
}

// === ESTILOS REUTILIZABLES (Diseño Clean) ===
const obtenerEstiloMenu = (activo: boolean) => ({
  padding: '12px 15px', backgroundColor: activo ? '#2B2B40' : 'transparent', color: activo ? 'white' : '#A2A3B7', border: 'none', textAlign: 'left' as const, cursor: 'pointer', borderRadius: '6px', fontSize: '14px', transition: '0.2s', fontWeight: activo ? '600' : '400'
});
const labelStyle = { display: 'block', marginBottom: '8px', fontSize: '13px', fontWeight: '600', color: '#3F4254' };
const inputStyle = { width: '100%', padding: '12px 15px', border: '1px solid #E4E6EF', borderRadius: '6px', boxSizing: 'border-box' as const, fontSize: '14px', backgroundColor: '#ffffff', color: '#3F4254', outline: 'none', transition: 'border-color 0.15s ease-in-out' };
const btnPrimary = { padding: '14px', backgroundColor: '#D32F2F', color: 'white', border: 'none', borderRadius: '6px', cursor: 'pointer', fontWeight: '600', fontSize: '15px', transition: 'background-color 0.3s' };
const btnSecondary = { padding: '12px 20px', backgroundColor: '#1E1E2D', color: 'white', border: 'none', borderRadius: '6px', cursor: 'pointer', fontWeight: '600' };
const btnLight = { padding: '12px 20px', backgroundColor: '#F4F6F8', color: '#D32F2F', border: '1px solid #E4E6EF', borderRadius: '6px', cursor: 'pointer', fontWeight: '600' };
const tableStyle = { width: '100%', borderCollapse: 'collapse' as const, marginTop: '10px' };
const thStyle = { textAlign: 'left' as const, padding: '15px', backgroundColor: '#F8F9FA', color: '#B5B5C3', fontSize: '12px', textTransform: 'uppercase' as const, letterSpacing: '1px', borderBottom: '1px solid #E4E6EF' };
const tdStyle = { padding: '15px', borderBottom: '1px dashed #E4E6EF', color: '#3F4254', fontSize: '14px' };
const trStyle = { transition: 'background-color 0.2s' };