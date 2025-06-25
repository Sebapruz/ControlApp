import React, { useState, useEffect, useContext, createContext, useRef } from 'react';
import { initializeApp } from 'firebase/app';
import {
  getAuth,
  createUserWithEmailAndPassword,
  signInWithEmailAndPassword,
  signOut,
  onAuthStateChanged,
} from 'firebase/auth';
import { getDatabase, ref, onValue, update, serverTimestamp, push, get } from 'firebase/database';
import {
  LogIn,
  LogOut,
  Monitor,
  Fan,
  Thermometer,
  Home,
  ChevronLeft,
  XCircle,
  CheckCircle,
  WifiOff,
  User,
  Power,
  Zap,
} from 'lucide-react';

// --- Firebase Configuration ---
const firebaseConfig = {
  apiKey: "AIzaSyB8bvI6OUFyEsUA5nzAFa2P7rE_pH5TgCU",
  authDomain: "controlapp-1cae3.firebaseapp.com",
  projectId: "controlapp-1cae3",
  storageBucket: "controlapp-1cae3.firebasestorage.app",
  messagingSenderId: "278542934818",
  appId: "1:278542934818:web:494b12f16f341fd93f7b7b",
  measurementId: "G-DRZS9P591K"
};

// Initialize Firebase
const app = initializeApp(firebaseConfig);
const auth = getAuth(app);
const database = getDatabase(app);

// --- Auth Context ---
const AuthContext = createContext(null);

const AuthProvider = ({ children }) => {
  const [user, setUser] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    // Listen for authentication state changes
    const unsubscribe = onAuthStateChanged(auth, (currentUser) => {
      setUser(currentUser);
      setLoading(false);
    });
    return () => unsubscribe();
  }, []);

  return (
    <AuthContext.Provider value={{ user, loading, auth }}>
      {children}
    </AuthContext.Provider>
  );
};

// --- Notification Component ---
const Notification = ({ message, type, onClose }) => {
  if (!message) return null;
  const bgColor = type === 'success' ? 'bg-green-500' : 'bg-red-500';
  const Icon = type === 'success' ? CheckCircle : XCircle;

  return (
    <div className={`fixed bottom-4 right-4 ${bgColor} text-white p-4 rounded-lg shadow-lg flex items-center space-x-2 animate-fadeInOut z-50`}>
      <Icon size={20} />
      <span>{message}</span>
      <button onClick={onClose} className="ml-2 text-white hover:text-gray-200">
        <XCircle size={16} />
      </button>
    </div>
  );
};

// --- Login/Register Component ---
const LoginPage = ({ onLoginSuccess }) => {
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [isRegistering, setIsRegistering] = useState(false);
  const [notification, setNotification] = useState({ message: '', type: '' });

  const handleAuth = async (e) => {
    e.preventDefault();
    try {
      if (isRegistering) {
        await createUserWithEmailAndPassword(auth, email, password);
        setNotification({ message: 'Registro exitoso! Iniciando sesión...', type: 'success' });
      } else {
        await signInWithEmailAndPassword(auth, email, password);
        setNotification({ message: 'Sesión iniciada exitosamente!', type: 'success' });
      }
      onLoginSuccess();
    } catch (error) {
      console.error("Authentication error:", error.message);
      let errorMessage = "Ocurrió un error. Inténtalo de nuevo.";
      if (error.code === 'auth/invalid-email') errorMessage = 'Email inválido.';
      else if (error.code === 'auth/user-not-found' || error.code === 'auth/wrong-password') errorMessage = 'Credenciales incorrectas.';
      else if (error.code === 'auth/email-already-in-use') errorMessage = 'El email ya está registrado.';
      else if (error.code === 'auth/weak-password') errorMessage = 'Contraseña demasiado débil (mínimo 6 caracteres).';
      
      setNotification({ message: errorMessage, type: 'error' });
    }
  };

  return (
    <div className="min-h-screen flex items-center justify-center bg-white text-gray-900 p-4">
      <form onSubmit={handleAuth} className="bg-white p-8 rounded-lg shadow-lg w-full max-w-md border border-gray-200">
        <h2 className="text-3xl font-bold text-center mb-6 text-gray-900">
          {isRegistering ? 'Registro' : 'Iniciar Sesión'}
        </h2>
        <div className="mb-4">
          <label className="block text-sm font-medium mb-2" htmlFor="email">Email</label>
          <input
            type="email"
            id="email"
            className="w-full p-3 rounded-md bg-gray-50 border border-gray-300 focus:outline-none focus:ring-1 focus:ring-gray-500 text-gray-900"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            required
          />
        </div>
        <div className="mb-6">
          <label className="block text-sm font-medium mb-2" htmlFor="password">Contraseña</label>
          <input
            type="password"
            id="password"
            className="w-full p-3 rounded-md bg-gray-50 border border-gray-300 focus:outline-none focus:ring-1 focus:ring-gray-500 text-gray-900"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            required
          />
        </div>
        <button
          type="submit"
          className="w-full bg-gray-900 hover:bg-gray-700 text-white font-bold py-3 rounded-md transition duration-300 ease-in-out transform hover:scale-105"
        >
          {isRegistering ? 'Registrarse' : 'Iniciar Sesión'}
        </button>
        <button
          type="button"
          onClick={() => setIsRegistering(!isRegistering)}
          className="w-full mt-4 text-sm text-gray-600 hover:underline"
        >
          {isRegistering ? 'Ya tengo una cuenta' : 'Crear una cuenta nueva'}
        </button>
      </form>
      <Notification
        message={notification.message}
        type={notification.type}
        onClose={() => setNotification({ message: '', type: '' })}
      />
    </div>
  );
};

// --- Dashboard Component (List of Rooms) ---
const DashboardPage = ({ onLogout, onSelectRoom }) => {
  const { user } = useContext(AuthContext);
  const [room201Status, setRoom201Status] = useState(null);

  useEffect(() => {
    // Listen for Sala 201 status to update icons
    const roomStatusRef = ref(database, 'rooms/201/current_status');
    const unsubscribe = onValue(roomStatusRef, (snapshot) => {
      const data = snapshot.val();
      if (data) {
        setRoom201Status(data);
      } else {
        setRoom201Status({ projector_status: 'OFF', ac_status: 'OFF' });
      }
    }, (error) => {
      console.error("Error reading room 201 status:", error);
    });
    return () => unsubscribe();
  }, []);

  const rooms = [
    { id: '201', name: 'Sala 201 (ESP32)', description: 'Estado de Proyector y Aire Acondicionado' },
    { id: '302', name: 'Sala 302', description: 'Próximamente...' },
    { id: '405', name: 'Sala 405', description: 'Próximamente...' },
  ];

  return (
    <div className="min-h-screen bg-white text-gray-900 flex flex-col">
      <header className="bg-white p-4 shadow-md border-b border-gray-200 flex justify-between items-center">
        <h1 className="text-2xl font-bold text-gray-900">Control de Salas</h1>
        <div className="flex items-center space-x-4">
          {user && (
            <span className="text-gray-700 flex items-center">
              <User size={18} className="mr-2" />
              {user.email}
            </span>
          )}
          <button
            onClick={onLogout}
            className="flex items-center bg-red-600 hover:bg-red-700 text-white py-2 px-4 rounded-md transition duration-300 ease-in-out"
          >
            <LogOut size={18} className="mr-2" />
            Cerrar Sesión
          </button>
        </div>
      </header>
      <main className="flex-grow p-8">
        <h2 className="text-xl font-semibold mb-6 text-gray-800">Selecciona una Sala:</h2>
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
          {rooms.map((room) => (
            <button
              key={room.id}
              onClick={() => room.id === '201' && onSelectRoom(room.id)}
              className={`bg-white p-6 rounded-lg shadow-lg border ${room.id === '201' ? 'border-gray-300 cursor-pointer hover:bg-gray-50 transform hover:scale-105' : 'border-gray-200 cursor-not-allowed opacity-50'} transition duration-300 ease-in-out`}
              disabled={room.id !== '201'}
            >
              <h3 className="text-xl font-bold mb-2 text-gray-900">{room.name}</h3>
              <p className="text-gray-600">{room.description}</p>
              {room.id === '201' && room201Status && (
                <div className="mt-4 flex items-center space-x-4">
                  <div className="flex items-center">
                    <Monitor size={20} className={`${room201Status.projector_status === 'ON' ? 'text-green-500' : 'text-red-500'} mr-1`} />
                    <span className={`text-sm ${room201Status.projector_status === 'ON' ? 'text-green-500' : 'text-red-500'}`}>
                      Proyector {room201Status.projector_status}
                    </span>
                  </div>
                  <div className="flex items-center">
                    <Fan size={20} className={`${room201Status.ac_status === 'ON' ? 'text-green-500' : 'text-red-500'} mr-1`} />
                    <span className={`text-sm ${room201Status.ac_status === 'ON' ? 'text-green-500' : 'text-red-500'}`}>
                      AC {room201Status.ac_status}
                    </span>
                  </div>
                </div>
              )}
            </button>
          ))}
        </div>
      </main>
    </div>
  );
};

// --- Bar Chart Component for Usage Hours ---
const BarChart = ({ data, title, color }) => {
  if (!data || data.length === 0) return <p className="text-gray-600">No hay datos de uso para mostrar.</p>;

  const width = 600; // Increased width for better display
  const height = 300;
  const margin = { top: 20, right: 30, bottom: 60, left: 50 }; // Increased bottom margin for labels

  // Calculate max value for y-axis
  const maxHours = Math.max(...data.map(d => d.hours)) * 1.2; // Add some padding to the max value
  const yAxisScale = maxHours > 0 ? height - margin.top - margin.bottom : 1;

  return (
    <div className="p-4 bg-white rounded-lg shadow-inner border border-gray-100">
      <h4 className="text-lg font-semibold mb-4 text-gray-800 text-center">{title}</h4>
      <svg viewBox={`0 0 ${width} ${height}`} className="w-full h-auto" preserveAspectRatio="xMidYMid meet">
        {/* Y-axis (left side) */}
        <line x1={margin.left} y1={margin.top} x2={margin.left} y2={height - margin.bottom} stroke="gray" strokeWidth="1" />
        {/* X-axis (bottom) */}
        <line x1={margin.left} y1={height - margin.bottom} x2={width - margin.right} y2={height - margin.bottom} stroke="gray" strokeWidth="1" />

        {/* Y-axis labels */}
        {Array.from({ length: 5 }).map((_, i) => {
          const value = (maxHours / 4) * i;
          const y = height - margin.bottom - (value / maxHours) * yAxisScale;
          return (
            <text key={`y-label-${i}`} x={margin.left - 10} y={y} textAnchor="end" alignmentBaseline="middle" fontSize="12" fill="gray">
              {value.toFixed(1)}h
            </text>
          );
        })}

        {data.map((d, i) => {
          const barWidth = (width - margin.left - margin.right) / data.length;
          const x = margin.left + i * barWidth + barWidth * 0.1; // Add padding between bars
          const barHeight = (d.hours / maxHours) * yAxisScale;
          const y = height - margin.bottom - barHeight;

          return (
            <g key={d.date}>
              <rect
                x={x}
                y={y}
                width={barWidth * 0.8} // Adjust bar width for spacing
                height={barHeight}
                fill={color}
                className="transition-all duration-300 ease-out"
                rx="4" ry="4" // Rounded corners for bars
              />
              <text
                x={x + barWidth * 0.4} // Center text above the bar
                y={y - 5}
                textAnchor="middle"
                fontSize="12"
                fill={color}
              >
                {d.hours.toFixed(1)}
              </text>
              <text
                x={x + barWidth * 0.4} // Center date label below the bar
                y={height - margin.bottom + 20} // Position below X-axis
                textAnchor="middle"
                fontSize="12"
                fill="gray"
                transform={`rotate(45 ${x + barWidth * 0.4}, ${height - margin.bottom + 20})`} // Rotate labels
              >
                {d.date}
              </text>
            </g>
          );
        })}
      </svg>
    </div>
  );
};

// --- Room Dashboard Component ---
const RoomDashboardPage = ({ roomId, onBack }) => {
  const [roomState, setRoomState] = useState({
    projector_status: 'OFF',
    ac_status: 'OFF',
    ac_temperature: 24,
    wifi_connected: false,
  });
  const [projectorUsageLogs, setProjectorUsageLogs] = useState([]);
  const [acUsageLogs, setAcUsageLogs] = useState([]);
  const [notification, setNotification] = useState({ message: '', type: '' });

  // Refs to store on-time for ongoing sessions
  const projectorOnTimeRef = useRef(null);
  const acOnTimeRef = useRef(null);

  useEffect(() => {
    // Listen for current status
    const roomStatusRef = ref(database, `rooms/${roomId}/current_status`);
    const unsubscribeStatus = onValue(roomStatusRef, (snapshot) => {
      const data = snapshot.val();
      if (data) {
        setRoomState({
          projector_status: data.projector_status || 'OFF',
          ac_status: data.ac_status || 'OFF',
          ac_temperature: data.ac_temperature || 24,
          wifi_connected: data.wifi_connected || false,
        });
      } else {
        setRoomState({ projector_status: 'OFF', ac_status: 'OFF', ac_temperature: 24, wifi_connected: false });
      }
    }, (error) => {
      console.error("Error reading room status:", error);
      setNotification({ message: 'Error al cargar el estado de la sala.', type: 'error' });
    });

    // Listen for projector usage logs
    const projectorLogsRef = ref(database, `rooms/${roomId}/projector_logs`);
    const unsubscribeProjectorLogs = onValue(projectorLogsRef, (snapshot) => {
      const logs = [];
      snapshot.forEach((childSnapshot) => {
        logs.push(childSnapshot.val());
      });
      setProjectorUsageLogs(logs.sort((a, b) => a.timestamp - b.timestamp));
    });

    // Listen for AC usage logs
    const acLogsRef = ref(database, `rooms/${roomId}/ac_logs`);
    const unsubscribeAcLogs = onValue(acLogsRef, (snapshot) => {
      const logs = [];
      snapshot.forEach((childSnapshot) => {
        logs.push(childSnapshot.val());
      });
      setAcUsageLogs(logs.sort((a, b) => a.timestamp - b.timestamp));
    });

    return () => {
      unsubscribeStatus();
      unsubscribeProjectorLogs();
      unsubscribeAcLogs();
    };
  }, [roomId]);

  // Effect to update on-time refs based on current status
  useEffect(() => {
    // Projector
    if (roomState.projector_status === 'ON' && !projectorOnTimeRef.current) {
      // If projector just turned ON, or was ON when loaded, set current time
      projectorOnTimeRef.current = Date.now();
    } else if (roomState.projector_status === 'OFF') {
      projectorOnTimeRef.current = null; // Reset when OFF
    }

    // AC
    if (roomState.ac_status === 'ON' && !acOnTimeRef.current) {
      // If AC just turned ON, or was ON when loaded, set current time
      acOnTimeRef.current = Date.now();
    } else if (roomState.ac_status === 'OFF') {
      acOnTimeRef.current = null; // Reset when OFF
    }
  }, [roomState.projector_status, roomState.ac_status]);


  // Helper function to calculate total usage hours from logs
  const calculateTotalUsageHours = (logs, currentStatus, currentOnTimeRef) => {
    let totalHours = 0;
    let lastOnTime = null;

    logs.forEach(log => {
      if (log.status === 'ON') {
        lastOnTime = log.timestamp;
      } else if (log.status === 'OFF' && lastOnTime) {
        totalHours += (log.timestamp - lastOnTime) / (1000 * 60 * 60); // Convert ms to hours
        lastOnTime = null;
      }
    });

    // If device is currently ON, add duration from last ON event to now
    if (currentStatus === 'ON' && currentOnTimeRef.current) {
      const lastRelevantOnTime = logs.length > 0 && logs[logs.length - 1].status === 'ON'
                               ? logs[logs.length - 1].timestamp
                               : currentOnTimeRef.current;
      totalHours += (Date.now() - lastRelevantOnTime) / (1000 * 60 * 60);
    }
    return totalHours;
  };

  // Helper function to calculate daily usage for charts
  const calculateDailyUsageHours = (logs, currentStatus, currentOnTimeRef) => {
    const dailyData = {};
    const today = new Date();
    today.setHours(0, 0, 0, 0); // Normalize to start of today

    // Initialize dailyData for the last 7 days
    for (let i = 0; i < 7; i++) {
      const date = new Date(today);
      date.setDate(today.getDate() - i);
      const dateString = date.toISOString().split('T')[0];
      dailyData[dateString] = 0;
    }

    let lastOnTime = null;

    logs.forEach(log => {
      if (log.status === 'ON') {
        lastOnTime = log.timestamp;
      } else if (log.status === 'OFF' && lastOnTime) {
        const duration = log.timestamp - lastOnTime;
        const onDate = new Date(lastOnTime);
        const offDate = new Date(log.timestamp);

        // Distribute duration across days if it spans midnight
        let currentIterDate = new Date(onDate);
        currentIterDate.setHours(0, 0, 0, 0); // Normalize to start of day

        while (currentIterDate.getTime() < offDate.getTime()) {
          const nextDay = new Date(currentIterDate);
          nextDay.setDate(currentIterDate.getDate() + 1);

          const intersectionStart = Math.max(onDate.getTime(), currentIterDate.getTime());
          const intersectionEnd = Math.min(offDate.getTime(), nextDay.getTime());
          
          if (intersectionEnd > intersectionStart) {
            const partialDuration = intersectionEnd - intersectionStart;
            const dateKey = currentIterDate.toISOString().split('T')[0];
            if (dailyData[dateKey] !== undefined) {
              dailyData[dateKey] += partialDuration / (1000 * 60 * 60);
            }
          }
          currentIterDate = nextDay;
        }
        lastOnTime = null;
      }
    });

    // Handle currently ON device
    if (currentStatus === 'ON' && currentOnTimeRef.current) {
      const lastRelevantOnTime = logs.length > 0 && logs[logs.length - 1].status === 'ON'
                               ? logs[logs.length - 1].timestamp
                               : currentOnTimeRef.current;
      const now = Date.now();
      const onDate = new Date(lastRelevantOnTime);
      const offDate = new Date(now);

      let currentIterDate = new Date(onDate);
      currentIterDate.setHours(0, 0, 0, 0);

      while (currentIterDate.getTime() < offDate.getTime()) {
        const nextDay = new Date(currentIterDate);
        nextDay.setDate(currentIterDate.getDate() + 1);

        const intersectionStart = Math.max(onDate.getTime(), currentIterDate.getTime());
        const intersectionEnd = Math.min(offDate.getTime(), nextDay.getTime());
        
        if (intersectionEnd > intersectionStart) {
          const partialDuration = intersectionEnd - intersectionStart;
          const dateKey = currentIterDate.toISOString().split('T')[0];
          if (dailyData[dateKey] !== undefined) {
            dailyData[dateKey] += partialDuration / (1000 * 60 * 60);
          }
        }
        currentIterDate = nextDay;
      }
    }

    // Format for chart: array of { date: 'MM/DD', hours: X }
    return Object.keys(dailyData).map(dateKey => ({
      date: new Date(dateKey).toLocaleDateString('es-ES', { month: '2-digit', day: '2-digit' }),
      hours: dailyData[dateKey],
    })).sort((a, b) => {
        const [aMonth, aDay] = a.date.split('/').map(Number);
        const [bMonth, bDay] = b.date.split('/').map(Number);
        const dateA = new Date(today.getFullYear(), aMonth - 1, aDay);
        const dateB = new Date(today.getFullYear(), bMonth - 1, bDay);
        return dateA - dateB;
    });
  };

  const totalProjectorHours = calculateTotalUsageHours(projectorUsageLogs, roomState.projector_status, projectorOnTimeRef);
  const totalAcHours = calculateTotalUsageHours(acUsageLogs, roomState.ac_status, acOnTimeRef);

  const dailyProjectorHours = calculateDailyUsageHours(projectorUsageLogs, roomState.projector_status, projectorOnTimeRef);
  const dailyAcHours = calculateDailyUsageHours(acUsageLogs, roomState.ac_status, acOnTimeRef);

  return (
    <div className="min-h-screen bg-white text-gray-900 p-8 font-sans">
      <header className="flex items-center mb-8 pb-4 border-b border-gray-200">
        <button onClick={onBack} className="flex items-center text-gray-600 hover:text-gray-900 transition duration-300">
          <ChevronLeft size={24} className="mr-2" />
          <span className="text-lg">Volver a Salas</span>
        </button>
        <h1 className="text-4xl font-extrabold text-center flex-grow text-gray-900">
          Sala {roomId} - Dashboard
        </h1>
        <div className="flex items-center space-x-2">
          <div className={`w-3 h-3 rounded-full ${roomState.wifi_connected ? 'bg-green-500' : 'bg-red-500'}`}></div>
          <span className="text-sm text-gray-600">Estado ESP32</span>
        </div>
      </header>

      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-8 mb-8">
        {/* Projector Status Card */}
        <div className="bg-white p-6 rounded-lg shadow-md border border-gray-200 flex flex-col items-center">
          <Monitor size={48} className={`mb-4 ${roomState.projector_status === 'ON' ? 'text-green-500' : 'text-red-500'}`} />
          <h3 className="text-xl font-bold mb-2 text-gray-800">Proyector</h3>
          <span className={`text-2xl font-semibold ${roomState.projector_status === 'ON' ? 'text-green-500' : 'text-red-500'}`}>
            {roomState.projector_status}
          </span>
          <p className="text-gray-600 mt-2">Horas ON acumuladas: {totalProjectorHours.toFixed(2)}h</p>
        </div>

        {/* AC Status Card */}
        <div className="bg-white p-6 rounded-lg shadow-md border border-gray-200 flex flex-col items-center">
          <Fan size={48} className={`mb-4 ${roomState.ac_status === 'ON' ? 'text-green-500' : 'text-red-500'}`} />
          <h3 className="text-xl font-bold mb-2 text-gray-800">Aire Acondicionado</h3>
          <span className={`text-2xl font-semibold ${roomState.ac_status === 'ON' ? 'text-green-500' : 'text-red-500'}`}>
            {roomState.ac_status}
          </span>
          <p className="text-gray-600 mt-2">Horas ON acumuladas: {totalAcHours.toFixed(2)}h</p>
        </div>

        {/* Temperature Card */}
        <div className="bg-white p-6 rounded-lg shadow-md border border-gray-200 flex flex-col items-center">
          <Thermometer size={48} className="mb-4 text-gray-700" />
          <h3 className="text-xl font-bold mb-2 text-gray-800">Temperatura Actual</h3>
          <span className="text-4xl font-bold text-gray-900">
            {roomState.ac_temperature}°C
          </span>
          <p className="text-gray-600 mt-2">Desde el último reporte del AC</p>
        </div>
      </div>

      {/* Usage Charts */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-8">
        <BarChart data={dailyProjectorHours} title="Horas de Uso Diario del Proyector (Últimos 7 días)" color="rgb(34, 197, 94)" />
        <BarChart data={dailyAcHours} title="Horas de Uso Diario del Aire Acondicionado (Últimos 7 días)" color="rgb(34, 197, 94)" />
      </div>

      <Notification
        message={notification.message}
        type={notification.type}
        onClose={() => setNotification({ message: '', type: '' })}
      />
    </div>
  );
};

// --- Main Application Component ---
const App = () => {
  const { user, loading } = useContext(AuthContext);
  const [currentPage, setCurrentPage] = useState('/');

  useEffect(() => {
    if (!loading) {
      if (user) {
        setCurrentPage('/dashboard');
      } else {
        setCurrentPage('/');
      }
    }
  }, [user, loading]);

  const handleLoginSuccess = () => {
    setCurrentPage('/dashboard');
  };

  const handleLogout = async () => {
    try {
      await signOut(auth);
      setCurrentPage('/');
    } catch (error) {
      console.error("Error closing session:", error.message);
    }
  };

  const handleSelectRoom = (roomId) => {
    setCurrentPage(`/room/${roomId}`);
  };

  const handleBackToDashboard = () => {
    setCurrentPage('/dashboard');
  };

  if (loading) {
    return (
      <div className="min-h-screen flex items-center justify-center bg-white text-gray-900 text-2xl">
        Cargando...
      </div>
    );
  }

  // If user is not logged in and not on the login page, redirect to login
  if (!user && currentPage !== '/') {
    return <LoginPage onLoginSuccess={handleLoginSuccess} />;
  }

  switch (currentPage.split('/')[1]) {
    case 'dashboard':
      return <DashboardPage onLogout={handleLogout} onSelectRoom={handleSelectRoom} />;
    case 'room':
      const roomId = currentPage.split('/')[2];
      // Updated to RoomDashboardPage
      return <RoomDashboardPage roomId={roomId} onBack={handleBackToDashboard} />;
    case '':
    default:
      return <LoginPage onLoginSuccess={handleLoginSuccess} />;
  }
};

const RootApp = () => (
  <AuthProvider>
    <App />
  </AuthProvider>
);

export default RootApp;

