import React from 'react';
import ReactDOM from 'react-dom/client';
import './index.css'; // Importa tus estilos Tailwind
import App from './App'; // Importa tu componente principal App
import reportWebVitals from './reportWebVitals'; // Para medir el rendimiento

const root = ReactDOM.createRoot(document.getElementById('root'));
root.render(
  <React.StrictMode>
    <App />
  </React.StrictMode>
);

// Si quieres medir el rendimiento de tu app, pasa una función
// para registrar resultados (por ejemplo: reportWebVitals(console.log))
// o envíalos a un endpoint de análisis. Aprende más: https://bit.ly/CRA-vitals
reportWebVitals();
