package com.example.controlsalasapp

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ServerValue
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import org.json.JSONObject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaDispositivos(
    sala: Sala,
    navController: NavHostController,
    isTestMode: Boolean
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val networkMonitor: NetworkMonitor = viewModel(factory = NetworkMonitorFactory(context))
    val isOnline by networkMonitor.isOnline.observeAsState(initial = false)

    // ESTADO PARA MENSAJES DE CONEXIÓN AL USUARIO
    var connectionStatusMessage by remember { mutableStateOf("Iniciando conexión...") }
    // Estado interno para saber qué tipo de conexión está activa para el CONTROL
    var activeControlConnection by remember { mutableStateOf<String>("none") }

    // Estados locales para los dispositivos. Se actualizarán desde BT (si conectados) o Firebase (si online y BT no).
    var acTemperature by remember { mutableStateOf(24) }
    var acPower by remember { mutableStateOf(false) }
    var projectorPower by remember { mutableStateOf(false) }

    var commandMessage by remember { mutableStateOf<String?>(null) } // Mensaje de estado del último comando

    // Estado WiFi del ESP32 (reportado por Firebase cuando el ESP32 está online)
    var esp32WifiConnected by remember { mutableStateOf(false) }
    // Estado de la conexión Bluetooth de la APP al ESP32 (reactivo desde BluetoothService)
    val bluetoothAppConnected by BluetoothService.isConnected.collectAsState()
    // NUEVO: Datos recibidos por Bluetooth (reactivo desde BluetoothService)
    val bluetoothReceivedData by BluetoothService.receivedData.collectAsState()

    // Referencia a la base de datos de Firebase
    val database = FirebaseDatabase.getInstance()
    val roomStatusRef = remember(sala.id) {
        database.getReference("rooms/${sala.id}/current_status")
    }

    // Listener de Firebase para SINCRONIZAR la UI de la APP con el estado del ESP32
    // Este listener ahora SOLO actualiza la UI si la app NO está conectada por Bluetooth
    val firebaseListener = remember {
        object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {

                if (isOnline == true && !isTestMode && !bluetoothAppConnected) {
                    val data = snapshot.value as? Map<String, Any>
                    data?.let {
                        acPower = (it["ac_status"] as? String == "ON")
                        acTemperature = (it["ac_temperature"] as? Long)?.toInt() ?: 24
                        projectorPower = (it["projector_status"] as? String == "ON")
                        esp32WifiConnected = (it["wifi_connected"] as? Boolean ?: false)
                        Log.d("FirebaseSync", "UI actualizada desde Firebase: $it, ESP32 WiFi conectado: $esp32WifiConnected")
                    } ?: run {
                        acPower = false
                        acTemperature = 24
                        projectorPower = false
                        esp32WifiConnected = false
                        Log.d("FirebaseSync", "No hay datos en Firebase para sala ${sala.id}, restableciendo UI.")
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("FirebaseSync", "Error al leer el estado de la sala desde Firebase: ${error.message}")
                if (isOnline == true) {
                    Toast.makeText(context, "Error al cargar estado (Firebase): ${error.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    // Función para activar/desactivar el listener de Firebase
    val toggleFirebaseListener = remember<(Boolean) -> Unit> {
        { enable ->
            if (enable) {
                roomStatusRef.addValueEventListener(firebaseListener)
                Log.d("Control", "Listener de Firebase para sala ${sala.id} ACTIVADO.")
            } else {
                roomStatusRef.removeEventListener(firebaseListener)
                Log.d("Control", "Listener de Firebase para sala ${sala.id} DESACTIVADO.")
            }
        }
    }

    // NUEVO: LaunchedEffect para procesar datos recibidos por Bluetooth
    LaunchedEffect(bluetoothReceivedData) {
        bluetoothReceivedData?.let { jsonString ->
            if (activeControlConnection == "bluetooth") { // Solo procesar si BT es la conexión activa para control
                try {
                    val jsonObject = JSONObject(jsonString)
                    // Actualiza los estados de la UI con los datos recibidos por Bluetooth
                    if (jsonObject.has("ac_status")) {
                        acPower = (jsonObject.getString("ac_status") == "ON")
                    }
                    if (jsonObject.has("ac_temperature")) {
                        acTemperature = jsonObject.getInt("ac_temperature")
                    }
                    if (jsonObject.has("projector_status")) {
                        projectorPower = (jsonObject.getString("projector_status") == "ON")
                    }
                    if (jsonObject.has("wifi_connected")) {
                        esp32WifiConnected = jsonObject.getBoolean("wifi_connected")
                    }
                    Log.d("BluetoothSync", "UI actualizada desde Bluetooth: $jsonString")
                } catch (e: Exception) {
                    Log.e("BluetoothSync", "Error al parsear JSON recibido por Bluetooth: ${e.message}", e)
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Error al leer datos Bluetooth: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }


    // --- LOGICA PRINCIPAL DE CONEXIÓN Y ESTADO ---
    LaunchedEffect(sala.id, isOnline, bluetoothAppConnected) {
        if (isTestMode) {
            withContext(Dispatchers.Main) {
                connectionStatusMessage = "Modo de prueba activado."
                activeControlConnection = "test"
            }
            toggleFirebaseListener(false) // Desactiva Firebase en modo de prueba
            BluetoothService.disconnect() // Asegura desconexión de BT
            Log.d("Control", "Modo de prueba activado.")
            return@LaunchedEffect
        }

        // Siempre manejar el listener de Firebase basado en la conectividad a Internet de la APP
        // Firebase es para la SINCRONIZACIÓN general cuando la app tiene internet.
        // La lectura prioritaria via BT es una capa adicional.
        toggleFirebaseListener(isOnline)

        // Determinar la conexión de CONTROL PRIORITARIA y el mensaje de estado para el usuario
        if (bluetoothAppConnected) { // Si ya estamos conectados por Bluetooth (o acaba de conectar)
            withContext(Dispatchers.Main) {
                connectionStatusMessage = "Conectado vía Bluetooth."
                activeControlConnection = "bluetooth"
            }
            Log.d("Control", "Estado: Conectado vía Bluetooth. No intentando Internet para control.")

            // NUEVO: Cuando BT está conectado, solicitar inmediatamente el estado actual al ESP32
            coroutineScope.launch(Dispatchers.IO) {
                val requestJson = JSONObject().apply { put("action", "requestStatus") }.toString() + "\n"
                val success = BluetoothService.write(requestJson)
                if (!success) {
                    Log.e("BluetoothRequest", "Fallo al solicitar estado por Bluetooth.")
                    // Considerar un fallback si la solicitud inicial falla, pero la conexión BT sigue activa
                } else {
                    Log.d("BluetoothRequest", "Solicitud de estado enviada por Bluetooth.")
                }
            }

        } else {
            // Si no estamos conectados por Bluetooth (o se acaba de desconectar), intentar conectar BT primero.
            withContext(Dispatchers.Main) {
                connectionStatusMessage = "Conectando vía Bluetooth..."
            }
            val btConnectAttemptSuccess = BluetoothService.connect(sala.macAddress, sala.uuidString)

            if (btConnectAttemptSuccess) {
                withContext(Dispatchers.Main) {
                    connectionStatusMessage = "Conectado vía Bluetooth."
                    activeControlConnection = "bluetooth"
                    Toast.makeText(context, "Bluetooth conectado a ${sala.nombre}", Toast.LENGTH_SHORT).show()
                }
                Log.d("Control", "Conexión Bluetooth exitosa. Mensaje actualizado.")

                // NUEVO: Inmediatamente después de una NUEVA conexión BT exitosa, solicitar el estado
                coroutineScope.launch(Dispatchers.IO) {
                    val requestJson = JSONObject().apply { put("action", "requestStatus") }.toString() + "\n"
                    val success = BluetoothService.write(requestJson)
                    if (!success) {
                        Log.e("BluetoothRequest", "Fallo al solicitar estado por Bluetooth después de nueva conexión.")
                    } else {
                        Log.d("BluetoothRequest", "Solicitud de estado enviada por Bluetooth después de nueva conexión.")
                    }
                }

            } else {
                // Bluetooth falló o no está disponible/habilitado, intentar Internet si la app está online
                if (isOnline) {
                    withContext(Dispatchers.Main) {
                        connectionStatusMessage = "Conectado a Internet."
                        activeControlConnection = "internet"
                        Toast.makeText(context, "Bluetooth falló. Conectado vía Internet.", Toast.LENGTH_LONG).show()
                    }
                    Log.d("Control", "Bluetooth falló. Usando Internet para control.")
                } else {
                    // Ninguna conexión disponible
                    withContext(Dispatchers.Main) {
                        connectionStatusMessage = "No hay conexión activa (Bluetooth o Internet)."
                        activeControlConnection = "none"
                        Toast.makeText(context, "No se pudo conectar (Bluetooth y sin Internet).", Toast.LENGTH_LONG).show()
                    }
                    Log.d("Control", "No hay conexión disponible. Mensaje actualizado.")
                }
            }
        }
    }


    // Efecto de limpieza al salir de la pantalla
    DisposableEffect(Unit) {
        onDispose {
            Log.d("Control", "Limpiando recursos para sala ${sala.id}.")
            toggleFirebaseListener(false) // Desactiva el listener de Firebase
            BluetoothService.disconnect() // Desconecta Bluetooth
        }
    }

    // Función para enviar comandos vía Firebase (extraída para reutilización)
    val sendCommandViaFirebase = remember<(String, String, Int?) -> Unit> {
        { device, command, value ->
            coroutineScope.launch(Dispatchers.IO) {
                try {
                    val updates = mutableMapOf<String, Any>()
                    when (device) {
                        "PROJECTOR" -> updates["projector_status"] = command // "ON" o "OFF"
                        "AC" -> {
                            updates["ac_status"] = command // "ON" o "OFF"
                            if (value != null) updates["ac_temperature"] = value // Si hay cambio de temp
                        }
                    }
                    updates["control_source"] = "ANDROID_APP_ONLINE" // Identifica la fuente del comando
                    updates["last_command"] = "${device}_${command}" + if (value != null) "_$value" else ""
                    updates["timestamp"] = ServerValue.TIMESTAMP // Marca de tiempo del servidor

                    roomStatusRef.updateChildren(updates).await() // Usamos .await() para esperar la operación
                    Log.d("Control", "Comando Firebase enviado: ${device} ${command}")
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Comando ${device} ${command} enviado (ONLINE)", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Log.e("Control", "Error al enviar comando a Firebase: ${e.message}", e)
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Error ONLINE: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }


    // Función unificada para enviar comandos (decide entre Modo de Prueba, Bluetooth o Firebase)
    val sendControlCommand = remember<(String, String, Int?) -> Unit> {
        { device, command, value ->
            coroutineScope.launch(Dispatchers.IO) { // Se lanza en un hilo de IO
                when (activeControlConnection) {
                    "test" -> {
                        // --- MODO DE PRUEBA (SIMULACIÓN) ---
                        delay(500) // Simular retardo
                        withContext(Dispatchers.Main) {
                            val cmdName = "${device}_${command}" + if (value != null) "_$value" else ""
                            commandMessage = "$cmdName (simulado) enviado a ${sala.nombre}"
                            // Actualizar estados locales en modo de prueba
                            when (device) {
                                "AC" -> {
                                    if (command == "ON") acPower = true
                                    else if (command == "OFF") acPower = false
                                    if (value != null) acTemperature = value
                                }
                                "PROJECTOR" -> {
                                    if (command == "ON") projectorPower = true
                                    else if (command == "OFF") projectorPower = false
                                }
                            }
                            Toast.makeText(context, "Comando ${device} ${command} enviado (SIMULADO)", Toast.LENGTH_SHORT).show()
                        }
                        Log.d("TestMode", commandMessage ?: "Comando simulado.")
                    }
                    "bluetooth" -> { // Si la conexión de control activa es Bluetooth
                        // --- VÍA BLUETOOTH (OFFLINE) ---
                        try {
                            // Formato JSON para enviar por Bluetooth al ESP32 (el ESP32 espera este formato)
                            val jsonCommand = JSONObject().apply {
                                put("action", "sendIR")
                                put("device", device)
                                put("command", command)
                                if (value != null) put("value", value)
                            }.toString() + "\n" // Añadir newline para que el ESP32 lea hasta el final

                            val success = BluetoothService.write(jsonCommand)
                            withContext(Dispatchers.Main) {
                                if (success) {
                                    Toast.makeText(context, "Comando ${device} ${command} enviado (BLUETOOTH)", Toast.LENGTH_SHORT).show()
                                    // Cuando el comando se envía por Bluetooth, ESP32 actualizará su estado y lo reportará vía Bluetooth.
                                    // La UI se actualizará automáticamente a través del `LaunchedEffect(bluetoothReceivedData)`.
                                    commandMessage = "${device} ${command} enviado BLUETOOTH."
                                } else {
                                    commandMessage = "Fallo al enviar ${device} ${command} BLUETOOTH."
                                    Toast.makeText(context, "Error BLUETOOTH: Fallo al enviar por Bluetooth", Toast.LENGTH_LONG).show()
                                    // Si BT falla en el envío y hay Internet, intentar Firebase como fallback
                                    if (isOnline) {
                                        Log.w("Control", "Bluetooth falló en el envío, intentando Firebase como fallback.")
                                        sendCommandViaFirebase(device, command, value)
                                    }
                                }
                            }
                        } catch (e: Exception) {
                            Log.e("Control", "Excepción al enviar comando Bluetooth: ${e.message}", e)
                            withContext(Dispatchers.Main) {
                                Toast.makeText(context, "Excepción BLUETOOTH: ${e.message}", Toast.LENGTH_LONG).show()
                                if (isOnline) {
                                    Log.w("Control", "Excepción Bluetooth en el envío, intentando Firebase como fallback.")
                                    sendCommandViaFirebase(device, command, value)
                                }
                            }
                        }
                    }
                    "internet" -> { // Si la conexión de control activa es Internet (Firebase)
                        // --- VÍA FIREBASE (ONLINE) ---
                        sendCommandViaFirebase(device, command, value)
                    }
                    else -> { // No hay conexión activa para el control
                        withContext(Dispatchers.Main) {
                            commandMessage = "No hay conexión disponible."
                            Toast.makeText(context, "No hay conexión para enviar el comando.", Toast.LENGTH_LONG).show()
                        }
                    }
                }
            }
        }
    }


    Scaffold(
        containerColor = Color.White
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = sala.nombre,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            // MENSAJE DE ESTADO DE CONEXIÓN GLOBAL (ÚNICO Y CONSOLIDADO)
            Text(
                text = connectionStatusMessage,
                style = MaterialTheme.typography.bodyLarge, // Texto más grande para mayor visibilidad
                color = when {
                    connectionStatusMessage.contains("Conectado") -> MaterialTheme.colorScheme.primary
                    connectionStatusMessage.contains("Fallo") || connectionStatusMessage.contains("No hay conexión") -> MaterialTheme.colorScheme.error
                    else -> MaterialTheme.colorScheme.onSurfaceVariant // Color predeterminado para "Iniciando..." o "Conectando..."
                },
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // La sección de indicadores individuales se ha eliminado.

            Spacer(modifier = Modifier.height(16.dp))

            // Aire Acondicionado
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, Color.LightGray),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Botón encendido/apagado AC
                    IconButton(
                        onClick = {
                            val newStatus = if (acPower) "OFF" else "ON"
                            sendControlCommand("AC", newStatus, null)
                        },
                        modifier = Modifier
                            .size(56.dp)
                            .border(1.dp, Color.LightGray, CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PowerSettingsNew,
                            contentDescription = "Encender/Apagar Aire",
                            tint = if (acPower) Color.Green else Color.Red,
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    // Temperatura y flechas
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "$acTemperature°",
                            fontSize = 40.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            // Flecha arriba (Subir temperatura)
                            IconButton(
                                onClick = {
                                    if (acTemperature < 30) {
                                        sendControlCommand("AC", "TEMP", acTemperature + 1)
                                    } else {
                                        Toast.makeText(context, "Temperatura máxima alcanzada", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                modifier = Modifier
                                    .size(48.dp)
                                    .border(1.dp, Color.LightGray, CircleShape)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.KeyboardArrowUp,
                                    contentDescription = "Subir temperatura",
                                    tint = Color.Black,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                            // Flecha abajo (Bajar temperatura)
                            IconButton(
                                onClick = {
                                    if (acTemperature > 16) {
                                        sendControlCommand("AC", "TEMP", acTemperature - 1)
                                    } else {
                                        Toast.makeText(context, "Temperatura mínima alcanzada", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                modifier = Modifier
                                    .size(48.dp)
                                    .border(1.dp, Color.LightGray, CircleShape)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.KeyboardArrowDown,
                                    contentDescription = "Bajar temperatura",
                                    tint = Color.Black,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        }
                    }

                    // Icono de aire
                    Icon(
                        imageVector = Icons.Default.Air,
                        contentDescription = "Modo Aire",
                        tint = Color.Black,
                        modifier = Modifier.size(36.dp)
                    )
                }
            }

            // Proyector
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, Color.LightGray),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Proyector",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    IconButton(
                        onClick = {
                            val newStatus = if (projectorPower) "OFF" else "ON"
                            sendControlCommand("PROJECTOR", newStatus, null)
                        },
                        modifier = Modifier
                            .size(56.dp)
                            .border(1.dp, Color.LightGray, CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PowerSettingsNew,
                            contentDescription = "Encender/Apagar Proyector",
                            tint = if (projectorPower) Color.Green else Color.Red,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
            }

            // Mostrar el mensaje de estado del comando
            commandMessage?.let { msg ->
                Text(
                    text = msg,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 16.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Botón de desconexión
            Button(
                onClick = {
                    navController.popBackStack() // Volver a la lista de salas
                    Toast.makeText(context, "Volviendo a Salas", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Volver a Salas")
            }
        }
    }
}
