import { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import axios from 'axios';

export default function Login() {
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const navigate = useNavigate();

  const handleLogin = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');
    try {
      await axios.post('http://localhost:8080/api/usuarios/login', {
        username, 
        password 
      });
      navigate('/dashboard'); 
    } catch (error) {
      setError("Credenciales incorrectas o acceso denegado.");
    }
  };

  return (
    <div style={{ display: 'flex', height: '100vh', width: '100vw', fontFamily: "'Inter', 'Segoe UI', sans-serif" }}>
      
      {/* MITAD IZQUIERDA: IMAGEN DE FONDO COMPLETA */}
      <div style={{ 
        flex: 1, 
        backgroundImage: 'url("https://img.magnific.com/foto-gratis/estanterias-almacenamiento-almacen-cajas-carton_23-2152001531.jpg?semt=ais_hybrid&w=740&q=80")', 
        backgroundSize: 'cover', 
        backgroundPosition: 'center', 
        display: 'flex', 
        flexDirection: 'column', 
        justifyContent: 'center', 
        alignItems: 'center', 
        position: 'relative'
      }}>
        {/* Filtro oscuro sobre la imagen para que el texto resalte */}
        <div style={{ position: 'absolute', top: 0, left: 0, right: 0, bottom: 0, backgroundColor: 'rgba(30, 30, 45, 0.75)' }}></div>
        
        <div style={{ position: 'relative', zIndex: 1, padding: '40px', textAlign: 'center', color: 'white', maxWidth: '80%' }}>
          <h1 style={{ fontSize: '42px', fontWeight: '800', marginBottom: '15px', letterSpacing: '1px' }}>PeruBus Cargo</h1>
          <p style={{ color: '#E4E6EF', fontSize: '16px', lineHeight: '1.6' }}>La tranquilidad de saber que tu carga viaja segura hacia su destino.</p>
        </div>
      </div>

      {/* MITAD DERECHA: FORMULARIO */}
      <div style={{ flex: 1, backgroundColor: 'white', display: 'flex', flexDirection: 'column', justifyContent: 'center', alignItems: 'center', padding: '40px' }}>
        
        <div style={{ width: '100%', maxWidth: '420px' }}>
          {/* AQUÍ VA TU LOGO PEQUEÑO */}
          <div style={{ marginBottom: '40px', textAlign: 'left' }}>
            <img src="/src/assets/logo.png" alt="Logo PerúBus" style={{ width: '260px', maxWidth: '100%' }} />
          </div>

          <h3 style={{ color: '#1E1E2D', fontSize: '26px', marginBottom: '8px' }}>¡Hola de nuevo!</h3>
          <p style={{ color: '#888', fontSize: '14px', marginBottom: '35px' }}>Ingresa tus credenciales para acceder al panel administrativo.</p>

          {error && <div style={{ padding: '12px', backgroundColor: '#ffebee', color: '#c62828', fontSize: '13px', borderRadius: '6px', marginBottom: '20px', textAlign: 'center', fontWeight: '500' }}>{error}</div>}

          <form onSubmit={handleLogin} style={{ display: 'flex', flexDirection: 'column', gap: '20px' }}>
            <div>
              <label style={labelStyle}>Usuario</label>
              <input type="text" placeholder="Nombre de usuario" value={username} onChange={(e) => setUsername(e.target.value)} required style={inputStyle} />
            </div>
            <div>
              <label style={labelStyle}>Contraseña</label>
              <input type="password" placeholder="••••••••" value={password} onChange={(e) => setPassword(e.target.value)} required style={inputStyle} />
            </div>
            <button type="submit" style={btnPrimary}>Ingresar al Sistema</button>
          </form>

          <div style={{ textAlign: 'center', marginTop: '30px', fontSize: '14px', color: '#888' }}>
            ¿Nuevo personal? <Link to="/register" style={{ color: '#D32F2F', fontWeight: '600', textDecoration: 'none' }}>Solicitar acceso</Link>
          </div>
        </div>
        
      </div>
    </div>
  );
}

const labelStyle = { display: 'block', marginBottom: '8px', fontSize: '12px', fontWeight: '700', color: '#3F4254', textTransform: 'uppercase' as const, letterSpacing: '0.5px' };
const inputStyle = { width: '100%', padding: '14px 15px', border: '1px solid #E4E6EF', borderRadius: '8px', boxSizing: 'border-box' as const, fontSize: '15px', backgroundColor: '#FAFAFA', outline: 'none', transition: '0.2s' };
const btnPrimary = { padding: '16px', backgroundColor: '#D32F2F', color: 'white', border: 'none', borderRadius: '8px', cursor: 'pointer', fontWeight: '700', fontSize: '16px', marginTop: '10px', transition: '0.3s' };