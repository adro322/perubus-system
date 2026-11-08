
import { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import axios from 'axios';
import './Login.css';
import logo from '../assets/logo.png';

export default function Login() {
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);
  const navigate = useNavigate();

  const handleLogin = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');
    setLoading(true);

    try {
      const res = await axios.post(
        'http://localhost:8080/api/usuarios/login',
        { username, password }
      );

      sessionStorage.setItem(
        'idUsuario',
        String(res.data.idUsuario)
      );

      navigate('/dashboard');
    } catch {
      setError('Credenciales incorrectas o acceso denegado.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <main className="login-page">
      <section className="login-visual">
        <div className="visual-overlay" />

        <div className="visual-content">
          <div className="visual-brand">
            <span className="brand-mark">PB</span>
            <div>
              <strong>Peru<span>Bus</span></strong>
              <small>CARGO</small>
            </div>
          </div>

          <div className="visual-eyebrow">
            LOGÍSTICA QUE CONECTA PERÚ
          </div>

          <h1>
            Tu carga.
            <br />
            Nuestro <span>compromiso.</span>
          </h1>

          <p className="visual-description">
            Gestiona tus operaciones de transporte y mantén
            el control de cada envío desde un solo lugar.
          </p>

          <div className="feature-list">
            <div className="feature-item">
              <div className="feature-icon">✓</div>
              <div>
                <strong>Seguridad</strong>
                <p>Tu carga siempre protegida</p>
              </div>
            </div>

            <div className="feature-item">
              <div className="feature-icon">◷</div>
              <div>
                <strong>Trazabilidad</strong>
                <p>Seguimiento de tus operaciones</p>
              </div>
            </div>

            <div className="feature-item">
              <div className="feature-icon">⇢</div>
              <div>
                <strong>Eficiencia</strong>
                <p>Control en cada etapa del envío</p>
              </div>
            </div>

            <div className="feature-item">
              <div className="feature-icon">◇</div>
              <div>
                <strong>Confianza</strong>
                <p>Comprometidos con tu destino</p>
              </div>
            </div>
          </div>

          <div className="visual-bottom">
            <span className="status-dot" />
            Tu operación, siempre en movimiento
          </div>
        </div>

        <div className="visual-red-stripe" />
      </section>

      <section className="login-panel">
        <div className="login-card">
          <div className="login-brand">
            <img src={logo} alt="Logo PeruBus Cargo" />
            <span>PORTAL ADMINISTRATIVO</span>
          </div>

          <div className="login-heading">
            <span className="welcome-label">
              <span /> BIENVENIDO
            </span>
            <h2>
              Inicia <span>sesión</span>
            </h2>
            <p>
              Ingresa tus credenciales para acceder
              <br className="desktop-break" /> al panel de administración.
            </p>
          </div>

          {error && (
            <div className="login-error" role="alert">
              <span className="error-symbol">!</span>
              {error}
            </div>
          )}

          <form className="login-form" onSubmit={handleLogin}>
            <div className="login-field">
              <label htmlFor="username">Usuario</label>
              <input
                id="username"
                type="text"
                placeholder="Ingresa tu usuario"
                value={username}
                onChange={(e) => setUsername(e.target.value)}
                autoComplete="username"
                required
              />
            </div>

            <div className="login-field">
              <label htmlFor="password">Contraseña</label>
              <input
                id="password"
                type="password"
                placeholder="Ingresa tu contraseña"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                autoComplete="current-password"
                required
              />
            </div>

            <button
              className="login-submit"
              type="submit"
              disabled={loading}
            >
              <span className="submit-arrow">
                {loading ? '···' : '➜'}
              </span>
              {loading ? 'Verificando acceso...' : 'Ingresar al sistema'}
            </button>
          </form>

          <div className="login-register">
            <div className="register-divider">
              <span />
              <small>¿Nuevo personal?</small>
              <span />
            </div>

            <Link to="/register" className="register-link">
              <span>＋</span> Solicitar acceso
            </Link>
          </div>

          <div className="login-security">
            <span className="security-icon">✓</span>
            <span>Acceso exclusivo para personal autorizado</span>
          </div>
        </div>

        <footer className="login-copyright">
          <span>© {new Date().getFullYear()} PeruBus Cargo.</span>
          <span>Juntos llegamos más lejos.</span>
        </footer>
      </section>
    </main>
  );
}
