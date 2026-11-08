
import { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import axios from 'axios';
import './RegisterAdmin.css';

export default function RegisterAdmin() {
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [rol, setRol] = useState('ADMIN');
  const [error, setError] = useState('');
  const [cargando, setCargando] = useState(false);

  const navigate = useNavigate();

  const handleRegister = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');
    setCargando(true);

    try {
      await axios.post('http://localhost:8080/api/usuarios/registrar', {
        username,
        password,
        rol,
        estado: true,
      });

      navigate('/');
    } catch (error: any) {
      setError(
        error.response?.data?.mensaje ||
        'No se pudo registrar al empleado. Verifica los datos e inténtalo nuevamente.'
      );
    } finally {
      setCargando(false);
    }
  };

  return (
    <main className="register-page">
      <section className="register-visual">
        <div className="register-visual-content">
          <div className="register-eyebrow">
            Gestión de personal
          </div>

          <h1>
            PeruBus
            <br />
            <span>Cargo.</span>
          </h1>

          <p>
            Gestiona los accesos de tu equipo y mantén las operaciones
            de transporte organizadas, seguras y eficientes.
          </p>

          <div className="register-benefits">
            <div className="register-benefit">
              <span className="register-benefit-icon">✓</span>
              <span>Administración centralizada de usuarios</span>
            </div>

            <div className="register-benefit">
              <span className="register-benefit-icon">↗</span>
              <span>Accesos según el rol del personal</span>
            </div>

            <div className="register-benefit">
              <span className="register-benefit-icon">◆</span>
              <span>Una operación logística más organizada</span>
            </div>
          </div>
        </div>
      </section>

      <section className="register-form-panel">
        <div className="register-form-container">
          <div className="register-logo">
            <img
              src="/src/assets/logo.png"
              alt="Logo PerúBus"
            />
          </div>

          <div className="register-heading-label">
            Nuevo acceso
          </div>

          <h2>Registrar personal</h2>

          <p className="register-description">
            Completa los datos para crear una cuenta de acceso
            al sistema PeruBus Cargo.
          </p>

          {error && (
            <div
              role="alert"
              style={{
                padding: '12px 14px',
                marginBottom: '20px',
                border: '1px solid #f3c4cb',
                borderRadius: '9px',
                background: '#fff1f2',
                color: '#b42336',
                fontSize: '12px',
                lineHeight: 1.6,
              }}
            >
              {error}
            </div>
          )}

          <form className="register-form" onSubmit={handleRegister}>
            <div className="register-field">
              <label htmlFor="register-username">
                Nombre de usuario
              </label>

              <input
                id="register-username"
                type="text"
                placeholder="Ej. juan.perez"
                value={username}
                onChange={(e) => setUsername(e.target.value)}
                autoComplete="username"
                required
              />
            </div>

            <div className="register-field">
              <label htmlFor="register-password">
                Contraseña
              </label>

              <input
                id="register-password"
                type="password"
                placeholder="Crea una contraseña"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                autoComplete="new-password"
                required
              />
            </div>

            <div className="register-field">
              <label htmlFor="register-role">
                Nivel de acceso
              </label>

              <select
                id="register-role"
                value={rol}
                onChange={(e) => setRol(e.target.value)}
              >
                <option value="ADMIN">
                  Administrador General
                </option>
                <option value="AGENTE">
                  Agente de Recepción
                </option>
              </select>
            </div>

            <button
              className="register-submit"
              type="submit"
              disabled={cargando}
            >
              {cargando ? 'Registrando empleado...' : 'Registrar empleado'}
              {!cargando && <span aria-hidden="true">→</span>}
            </button>
          </form>

          <Link to="/" className="register-back">
            ← Volver al inicio de sesión
          </Link>

          <div className="register-security-note">
            <span aria-hidden="true">🔒</span>
            <span>
              <strong>Acceso al sistema</strong>
              <br />
              Asigna el rol correspondiente según las responsabilidades
              de cada empleado.
            </span>
          </div>
        </div>
      </section>
    </main>
  );
}
