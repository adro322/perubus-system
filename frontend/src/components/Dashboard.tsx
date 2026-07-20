import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import axios from 'axios';

export default function Dashboard() {
  const navigate = useNavigate();
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
  const [listaEncomiendas, setListaEncomiendas] = useState([]);

  // Cargar listas al cambiar de vista
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

  const buscarCliente = async () => {
    setErrorBusqueda(''); // Limpia errores anteriores
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
    try {
      const res = await axios.post('http://localhost:8080/api/encomiendas/cotizar', { peso: parseFloat(peso) });
      setTarifa(res.data.costoEstimado);
    } catch (error) { alert("Error al cotizar"); }
  };

  const registrarEncomienda = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!clienteEncontrado) return alert("Primero busque y valide un cliente válido");
    try {
      const res = await axios.post('http://localhost:8080/api/encomiendas/registrar', {
        peso: parseFloat(peso),
        cliente: { numDocumento: clienteEncontrado.numDocumento } 
      });
      mostrarMensaje(`Tracking generado: ${res.data.codigoTracking}`);
      setPeso(''); setDniBuscado(''); setClienteEncontrado(null); setTarifa(null); setDestino('');
    } catch (error) { alert("Error al registrar la encomienda"); }
  };

  const mostrarMensaje = (texto: string) => {
    setMensaje(texto);
    setTimeout(() => setMensaje(''), 4000);
  };

  return (
    <div style={{ display: 'flex', height: '100vh', backgroundColor: '#F4F7FA', fontFamily: "'Inter', 'Segoe UI', sans-serif" }}>
      
      {/* SIDEBAR PROFESIONAL */}
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

        <button onClick={() => navigate('/')} style={{ padding: '20px', backgroundColor: '#1a1a27', color: '#ff4d4d', border: 'none', cursor: 'pointer', borderTop: '1px solid #2B2B40', textAlign: 'left', fontWeight: 'bold' }}>
          Cerrar Sesión
        </button>
      </div>

      {/* ÁREA DE CONTENIDO */}
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
                    <select required value={destino} onChange={e => setDestino(e.target.value)} style={inputStyle}>
                      <option value="">Seleccione ruta de envío...</option>
                      <option value="Lima-Ica">Lima - Ica</option>
                      <option value="Lima-Arequipa">Lima - Arequipa</option>
                    </select>
                  </div>
                  
                  <div style={{ gridColumn: 'span 2' }}>
                    <label style={labelStyle}>Peso del Paquete (Kg)</label>
                    <div style={{ display: 'flex', gap: '15px' }}>
                      <input type="number" step="0.01" required value={peso} onChange={e => setPeso(e.target.value)} style={{...inputStyle, flex: 1}} />
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
              <table style={tableStyle}>
                <thead>
                  <tr>
                    <th style={thStyle}>Tracking</th>
                    <th style={thStyle}>Peso</th>
                    <th style={thStyle}>Estado</th>
                    <th style={thStyle}>Acciones</th> {/* Nueva columna */}
                  </tr>
                </thead>
                <tbody>
                  {listaEncomiendas.map((enc: any) => (
                    <tr key={enc.idEncomienda} style={trStyle}>
                      <td style={{...tdStyle, fontWeight: 'bold', color: '#D32F2F'}}>{enc.codigoTracking}</td>
                      <td style={tdStyle}>{enc.peso} Kg</td>
                      <td style={tdStyle}>
                        <span style={{ backgroundColor: '#E8F5E9', color: '#2E7D32', padding: '4px 8px', borderRadius: '4px', fontSize: '12px' }}>
                          {enc.estado}
                        </span>
                      </td>
                      <td style={tdStyle}>
                        {/* Botón preparado para el futuro módulo */}
                        <button style={{ padding: '6px 12px', backgroundColor: '#F3F6F9', color: '#B5B5C3', border: '1px solid #E4E6EF', borderRadius: '4px', fontSize: '12px', cursor: 'not-allowed' }} disabled>
                          Asignar Manifiesto 🔒
                        </button>
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
const tableStyle = { width: '100%', borderCollapse: 'collapse' as const, marginTop: '10px' };
const thStyle = { textAlign: 'left' as const, padding: '15px', backgroundColor: '#F8F9FA', color: '#B5B5C3', fontSize: '12px', textTransform: 'uppercase' as const, letterSpacing: '1px', borderBottom: '1px solid #E4E6EF' };
const tdStyle = { padding: '15px', borderBottom: '1px dashed #E4E6EF', color: '#3F4254', fontSize: '14px' };
const trStyle = { transition: 'background-color 0.2s' };