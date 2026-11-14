import type { ReactNode } from 'react';
import './ConfirmacionModal.css';

type ConfirmacionModalProps = {
  titulo: string;
  mensaje: string;
  textoConfirmar: string;
  peligro?: boolean;
  procesando?: boolean;
  children?: ReactNode;
  onConfirmar: () => void;
  onCancelar: () => void;
};

export default function ConfirmacionModal({
  titulo,
  mensaje,
  textoConfirmar,
  peligro = false,
  procesando = false,
  children,
  onConfirmar,
  onCancelar,
}: ConfirmacionModalProps) {
  return (
    <div
      className="confirmation-backdrop"
      onMouseDown={(event) => {
        if (event.target === event.currentTarget && !procesando) onCancelar();
      }}
    >
      <section
        className="confirmation-modal"
        role="dialog"
        aria-modal="true"
        aria-labelledby="confirmation-title"
        aria-describedby="confirmation-message"
      >
        <h2 id="confirmation-title">{titulo}</h2>
        <p id="confirmation-message">{mensaje}</p>
        {children}
        <div className="confirmation-actions">
          <button type="button" className="confirmation-cancel" disabled={procesando} onClick={onCancelar}>
            Volver
          </button>
          <button
            type="button"
            className={peligro ? 'confirmation-submit confirmation-submit-danger' : 'confirmation-submit'}
            disabled={procesando}
            onClick={onConfirmar}
          >
            {procesando ? 'Procesando...' : textoConfirmar}
          </button>
        </div>
      </section>
    </div>
  );
}
