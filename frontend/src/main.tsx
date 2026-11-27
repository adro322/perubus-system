import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import axios from 'axios'
import './index.css'
import './App.css'
import App from './App.tsx'

// El backend identifica al personal por su sesión (cookie): hay que enviarla en todas las peticiones
axios.defaults.withCredentials = true

// Si la sesión del personal expira, se vuelve al login. El login y el registro de personal
// se excluyen para que muestren su propio mensaje de error.
axios.interceptors.response.use(
  (respuesta) => respuesta,
  (error) => {
    const url = String(error.config?.url ?? '')
    const esAutenticacion = url.includes('/api/usuarios/login') || url.includes('/api/usuarios/registrar')
    if (error.response?.status === 401 && !esAutenticacion && window.location.pathname === '/dashboard') {
      sessionStorage.clear()
      window.location.href = '/'
    }
    return Promise.reject(error)
  },
)

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <App />
  </StrictMode>,
)
