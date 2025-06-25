# ControlApp: Domótica Inteligente con ESP32, Android y Web 🏡✨

¡Bienvenido al monorepo de **ControlApp**! Este proyecto es tu solución integral para la automatización y el monitoreo de dispositivos domésticos. Conecta tu **ESP32** a la nube, contrólalo desde tu **App Móvil Android** y supervisa todo desde un **Dashboard Web en tiempo real**.

Este repositorio unifica el código fuente de todos los componentes, facilitando el desarrollo, versionado y colaboración.

## 🚀 Visión General de la Arquitectura

ControlApp se basa en una comunicación fluida y en tiempo real. Aquí un vistazo rápido:

* **App Móvil (Android):** Envía comandos al ESP32 vía Bluetooth Low Energy (BLE).
* **ESP32:** Recibe comandos, controla dispositivos físicos (IR), y actualiza el estado en Firebase.
* **Firebase Realtime Database:** Es el centro de la verdad, sincronizando el estado entre el ESP32, la App Móvil y el Dashboard Web en tiempo real.
* **Dashboard Web:** Escucha constantemente los cambios en Firebase para mostrar el estado actualizado de los dispositivos.


## 📦 Estructura del Repositorio

Aquí cómo está organizado este monorepo:
├── .github/                     # ⚙️ Configuraciones de GitHub Actions (CI/CD)
├── docs/                        # 📚 Documentación detallada del proyecto (arquitectura, DB, etc.)
├── web-dashboard/               # 🌐 Código fuente del Dashboard Web (React)
├── mobile-app/                  # 📱 Código fuente de la Aplicación Móvil Android
├── esp32-firmware/              # 🤖 Código del firmware para el ESP32 (Arduino)
├── .gitignore                   # 🚫 Archivos y carpetas a ignorar por Git
└── README.md                    # 📖 ¡Estás aquí!

## 🛠️ Manual de Entorno e Instalación

Para poner en marcha ControlApp, sigue estos pasos para cada componente. ¡Necesitarás hardware y software específicos!

### 📋 Requisitos Previos Generales

Asegúrate de tener lo siguiente:

**Hardware:**
* **Módulo(s) ESP32.**
* **Módulo(s) Transmisor Infrarrojo (IR).**
* **Cables USB** para el ESP32.

**Software:**
* **Arduino IDE:** Para programar el ESP32.
* **Acceso a una cuenta Firebase:** Con un proyecto configurado, **Firebase Realtime Database habilitada**, y las credenciales necesarias (archivo `google-services.json` para Android, y claves de API/URL de DB para la web y el ESP32).
* **Node.js y npm/Yarn:** Para el Dashboard Web.
* **Android Studio:** Para la Aplicación Móvil.

### 🤖 Configuración y Levantamiento del Firmware para ESP32

1.  **Instalar Arduino IDE:**
    * Si aún no lo tienes, descárgalo e instálalo desde [arduino.cc](https://www.arduino.cc/en/software).
2.  **Configurar Placa ESP32 en Arduino IDE:**
    * Ve a `Archivo > Preferencias`. En "URLs adicionales para el gestor de tarjetas", añade: `https://raw.githubusercontent.com/espressif/arduino-esp32/gh-pages/package_esp32_index.json`
    * Luego, ve a `Herramientas > Placa > Gestor de Tarjetas...` (Boards Manager).
    * Busca "esp32" e instala el paquete **"esp32 by Espressif Systems"**.
3.  **Instalar Librerías Necesarias:**
    * Ve a `Sketch > Incluir Librería > Gestionar Librerías...` (Manage Libraries...).
    * Busca e instala:
        * `IRremoteESP8266` (para control IR).
        * `Firebase-ESP32` (para comunicación con Firebase).
4.  **Configurar Credenciales de Wi-Fi y Firebase:**
    * Abre el proyecto del firmware del ESP32 en Arduino IDE: `esp32-firmware/`
    * Dentro del código, busca las secciones donde se definen las credenciales de Wi-Fi (`SSID` y `contraseña`) y las credenciales de Firebase (`clave de API`, `URL de la base de datos`). ¡El ESP32 necesita Wi-Fi para llegar a Firebase!
    * **Introduce tus credenciales.** ⚠️ Para producción, considera usar métodos más seguros de gestión de secretos.
5.  **Conectar Módulo IR:**
    * Asegúrate de que el transmisor infrarrojo esté correctamente conectado al ESP32 según los pines definidos en tu código.
6.  **Cargar Código al ESP32:**
    * Conecta tu ESP32 a la computadora vía USB.
    * En Arduino IDE, selecciona la placa ESP32 correcta (`Herramientas > Placa`) y el puerto serie correcto (`Herramientas > Puerto`).
    * Haz clic en "Subir" (Upload) para compilar y cargar el firmware.
    * Verifica la consola para confirmar una carga exitosa.

### 📱 Configuración y Levantamiento de la Aplicación Móvil (Android)

1.  **Abrir Proyecto en Android Studio:**
    * Abre el directorio `mobile-app/` en Android Studio.
2.  **Configurar Firebase para Android:**
    * Necesitarás el archivo `google-services.json` de tu proyecto Firebase. Colócalo en el directorio `mobile-app/app/`.
    * Sigue la guía oficial de Firebase para añadir Android a tu proyecto si aún no lo has hecho, asegurándote de que los ID de paquete coincidan.
3.  **Instalar Dependencias:**
    * Android Studio debería sincronizar automáticamente las dependencias de Gradle. Si hay problemas, haz clic en `File > Sync Project with Gradle Files`.
4.  **Ejecutar en Emulador/Dispositivo:**
    * Conecta tu dispositivo Android (con depuración USB activada) o inicia un emulador.
    * Haz clic en el botón "Run" (el triángulo verde ▶️) en Android Studio para compilar e instalar la app.
5.  **Instalación Directa del APK:**
    * Para una instalación rápida sin compilar:
        * **Descarga el último APK estable** desde la sección de [GitHub Releases](https://github.com/tu_usuario/tu_repositorio/releases) de este repositorio.
        * Transfiere el archivo `.apk` a tu dispositivo Android (vía USB, email, etc.).
        * Asegúrate de tener habilitada la opción "Instalar apps desconocidas" en la configuración de seguridad de tu dispositivo (si no es desde la Play Store).
        * Abre el archivo `.apk` en tu dispositivo para instalar la aplicación.

### 🌐 Configuración y Despliegue del Dashboard Web

1.  **Navegar al Directorio:**
    ```bash
    cd web-dashboard/
    ```
2.  **Instalar Dependencias:**
    ```bash
    npm install # o yarn install
    ```
3.  **Configurar Variables de Entorno (Firebase Web Config):**
    * Crea un archivo `.env` en la raíz de `web-dashboard/`.
    * Copia tus credenciales de configuración de Firebase para la Web aquí:
        ```
        REACT_APP_FIREBASE_API_KEY=AIzaSy...
        REACT_APP_FIREBASE_AUTH_DOMAIN=tu-proyecto.firebaseapp.com
        REACT_APP_FIREBASE_PROJECT_ID=tu-proyecto
        REACT_APP_FIREBASE_STORAGE_BUCKET=tu-proyecto.appspot.com
        REACT_APP_FIREBASE_MESSAGING_SENDER_ID=...
        REACT_APP_FIREBASE_APP_ID=1:..
        REACT_APP_FIREBASE_DATABASE_URL=[https://tu-proyecto-default-rtdb.firebaseio.com](https://tu-proyecto-default-rtdb.firebaseio.com)
        ```
4.  **Iniciar en Desarrollo:**
    ```bash
    npm start # o yarn start
    ```
    El dashboard estará disponible generalmente en `http://localhost:3000`.
5.  **Despliegue a Producción (Ej. Firebase Hosting):**
    * **Instala las Firebase CLI tools:** `npm install -g firebase-tools`
    * **Inicia sesión en Firebase:** `firebase login`
    * **Inicializa tu proyecto (si no lo has hecho):** `firebase init` (selecciona Hosting y conecta a tu proyecto Firebase).
    * **Compila el Dashboard para producción:** `npm run build # o yarn build` (esto creará una carpeta `build/`).
    * **Despliega:** `firebase deploy --only hosting`
    * Tu dashboard estará accesible en la URL de Firebase Hosting (ej. `tu-proyecto.web.app`).

---

## 📚 Documentación Completa

Para una inmersión profunda en la arquitectura, el esquema de la base de datos, la definición de la API y más detalles técnicos, consulta nuestra [Documentación Detallada del Proyecto](./docs/DOCUMENTACION.md).


## 📄 Licencia

Este proyecto está bajo la Licencia [Nombre de tu Licencia, ej. MIT License]. Consulta el archivo [LICENSE](./LICENSE) para más detalles.
