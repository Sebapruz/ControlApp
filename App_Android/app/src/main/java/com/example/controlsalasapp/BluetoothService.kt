package com.example.controlsalasapp

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID

// Objeto singleton para gestionar toda la lógica Bluetooth de forma centralizada
object BluetoothService {

    private var bluetoothAdapter: BluetoothAdapter? = null
    private var bluetoothSocket: BluetoothSocket? = null
    private var outputStream: OutputStream? = null
    private var inputStream: InputStream? = null

    // CoroutineScope para gestionar las corrutinas de lectura de Bluetooth
    private val serviceScope = CoroutineScope(Dispatchers.IO + Job())
    private var readJob: Job? = null // Job para la corrutina de lectura continua

    // Usamos StateFlow para que el estado de conexión Bluetooth sea reactivo
    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    // NUEVO: StateFlow para los datos recibidos por Bluetooth
    private val _receivedData = MutableStateFlow<String?>(null)
    val receivedData: StateFlow<String?> = _receivedData.asStateFlow()

    // Se debe llamar a esta función una vez al inicio de la aplicación, por ejemplo, en MainActivity
    fun initialize(context: Context) {
        if (bluetoothAdapter == null) {
            val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
            bluetoothAdapter = bluetoothManager.adapter
        }
        Log.d("BluetoothService", "BluetoothService inicializado.")
    }

    // Intenta conectar a un dispositivo Bluetooth con la MAC y UUID proporcionadas
    @SuppressLint("MissingPermission")
    suspend fun connect(macAddress: String, uuidString: String): Boolean {
        if (bluetoothAdapter == null || !bluetoothAdapter!!.isEnabled) {
            Log.w("BluetoothService", "Bluetooth no disponible o no habilitado.")
            _isConnected.value = false
            return false
        }

        // Si ya estamos conectados al mismo dispositivo, no hacer nada
        if (bluetoothSocket != null && bluetoothSocket!!.isConnected && bluetoothSocket!!.remoteDevice.address == macAddress) {
            Log.d("BluetoothService", "Ya conectado al mismo dispositivo: $macAddress")
            _isConnected.value = true
            return true
        }

        // Desconectar cualquier conexión previa y cancelar la lectura si estaba activa
        disconnect()

        return withContext(Dispatchers.IO) {
            try {
                val device: BluetoothDevice = bluetoothAdapter!!.getRemoteDevice(macAddress)
                val uuid: UUID = UUID.fromString(uuidString)

                bluetoothSocket = device.createRfcommSocketToServiceRecord(uuid)
                bluetoothAdapter!!.cancelDiscovery() // Cancelar el descubrimiento para liberar recursos

                Log.d("BluetoothService", "Intentando conectar al socket...")
                bluetoothSocket!!.connect() // Esto bloquea hasta que se conecta o falla

                outputStream = bluetoothSocket!!.outputStream
                inputStream = bluetoothSocket!!.inputStream

                _isConnected.value = true // Actualiza el StateFlow a true
                Log.d("BluetoothService", "Conectado exitosamente a: ${device.name ?: device.address}")

                // NUEVO: Iniciar la lectura de datos después de una conexión exitosa
                startReading()
                true
            } catch (e: IOException) {
                Log.e("BluetoothService", "Error al conectar Bluetooth: ${e.message}", e)
                closeResources() // Asegurarse de cerrar recursos en caso de error
                _isConnected.value = false // Actualiza el StateFlow a false
                false
            } catch (e: SecurityException) {
                // Esto sucede si los permisos BLUETOOTH_CONNECT no están concedidos
                Log.e("BluetoothService", "Error de seguridad (permisos) al conectar Bluetooth: ${e.message}", e)
                closeResources()
                _isConnected.value = false
                false
            } catch (e: Exception) {
                Log.e("BluetoothService", "Error inesperado al conectar Bluetooth: ${e.message}", e)
                closeResources()
                _isConnected.value = false
                false
            }
        }
    }

    // NUEVO: Función para iniciar la lectura continua de datos Bluetooth
    private fun startReading() {
        readJob?.cancel() // Cancelar cualquier trabajo de lectura anterior
        readJob = serviceScope.launch {
            val buffer = ByteArray(1024) // Buffer para los bytes leídos
            var bytes: Int // Bytes devueltos por read()
            val stringBuilder = StringBuilder() // Para construir la cadena JSON
            Log.d("BluetoothService", "Iniciando lectura de datos Bluetooth...")

            while (_isConnected.value) { // Continuar leyendo mientras estemos conectados
                try {
                    bytes = inputStream?.read(buffer) ?: -1
                    if (bytes != -1) {
                        val receivedPart = String(buffer, 0, bytes)
                        stringBuilder.append(receivedPart)

                        // Buscar el terminador de línea (\n) para saber cuándo un mensaje JSON ha terminado
                        val terminatorIndex = stringBuilder.indexOf('\n')
                        if (terminatorIndex != -1) {
                            val fullMessage = stringBuilder.substring(0, terminatorIndex).trim()
                            stringBuilder.delete(0, terminatorIndex + 1) // Eliminar el mensaje procesado

                            // Emitir el mensaje completo
                            withContext(Dispatchers.Main) {
                                _receivedData.value = fullMessage
                                Log.d("BluetoothService", "Datos recibidos: $fullMessage")
                            }
                        }
                    }
                } catch (e: IOException) {
                    Log.e("BluetoothService", "Error de lectura Bluetooth: ${e.message}", e)
                    disconnect() // Desconectar si hay un error de lectura
                    break // Salir del bucle de lectura
                } catch (e: Exception) {
                    Log.e("BluetoothService", "Error inesperado durante la lectura Bluetooth: ${e.message}", e)
                    disconnect()
                    break
                }
            }
            Log.d("BluetoothService", "Lectura de datos Bluetooth terminada.")
        }
    }

    // Envía datos al dispositivo Bluetooth
    suspend fun write(data: String): Boolean {
        return withContext(Dispatchers.IO) {
            if (_isConnected.value && outputStream != null) {
                try {
                    outputStream!!.write(data.toByteArray())
                    // Añadir un pequeño delay para asegurar que el ESP32 tenga tiempo de procesar
                    // y para evitar enviar comandos demasiado rápido.
                    // Esto es una práctica común en comunicaciones seriales.
                    Thread.sleep(50)
                    Log.d("BluetoothService", "Datos enviados: $data")
                    true
                } catch (e: IOException) {
                    Log.e("BluetoothService", "Error al escribir en Bluetooth: ${e.message}", e)
                    disconnect() // Desconectar si hay un error de escritura
                    false
                } catch (e: Exception) {
                    Log.e("BluetoothService", "Error inesperado al escribir en Bluetooth: ${e.message}", e)
                    disconnect()
                    false
                }
            } else {
                Log.w("BluetoothService", "No conectado para escribir datos.")
                false
            }
        }
    }

    // Desconecta el dispositivo Bluetooth y cierra todos los sockets/streams
    fun disconnect() {
        Log.d("BluetoothService", "Desconectando Bluetooth...")
        readJob?.cancel() // Cancelar la lectura al desconectar
        closeResources()
        _isConnected.value = false // Actualiza el StateFlow a false
        // También limpiamos los datos recibidos al desconectar
        _receivedData.value = null
        Log.d("BluetoothService", "Bluetooth desconectado.")
    }

    // Cierra todos los recursos de conexión (sockets, streams)
    private fun closeResources() {
        try {
            outputStream?.close()
            inputStream?.close()
            bluetoothSocket?.close()
        } catch (e: IOException) {
            Log.e("BluetoothService", "Error al cerrar recursos Bluetooth: ${e.message}", e)
        } finally {
            outputStream = null
            inputStream = null
            bluetoothSocket = null
        }
    }
}
