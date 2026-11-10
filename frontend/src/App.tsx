import { BrowserRouter as Router, Routes, Route } from 'react-router-dom';
import Login from './components/Login';
import Dashboard from './components/Dashboard';
import RegisterAdmin from './components/RegisterAdmin';
import PortalCliente from './components/PortalCliente';


function App() {
  return (
    <Router>
      <Routes>
        <Route path="/" element={<Login />} />
        <Route path="/register" element={<RegisterAdmin />} />
        <Route path="/dashboard" element={<Dashboard />} />
        <Route path="/portal" element={<PortalCliente />} />
      </Routes>
    </Router>
  );
}

export default App;