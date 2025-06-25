package com.example.controlsalasapp


import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.controlsalasapp.ui.theme.ControlSalasAppTheme
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.google.gson.Gson
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class MainActivity : ComponentActivity() {

    // Launcher para solicitar múltiples permisos en tiempo de ejecución
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->


        val bluetoothConnectGranted = permissions[Manifest.permission.BLUETOOTH_CONNECT] ?: false
        val bluetoothScanGranted = permissions[Manifest.permission.BLUETOOTH_SCAN] ?: false
        val fineLocationGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false

        var allPermissionsGranted = true

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) { // Android 12 (API 31) y versiones posteriores
            if (!bluetoothConnectGranted) {
                allPermissionsGranted = false
                Toast.makeText(this, "Permiso BLUETOOTH_CONNECT denegado. Algunas funciones podrían no estar disponibles.", Toast.LENGTH_LONG).show()
            }
            if (!bluetoothScanGranted) {
                allPermissionsGranted = false
                Toast.makeText(this, "Permiso BLUETOOTH_SCAN denegado. La búsqueda de dispositivos podría no funcionar.", Toast.LENGTH_LONG).show()
            }
        } else { // Android 11 (API 30) y versiones anteriores
            // En estas versiones, BLUETOOTH_ADMIN y BLUETOOTH son permisos normales (no de runtime),
            // se otorgan en la instalación. ACCESS_FINE_LOCATION sí es de runtime para escaneo.
            if (!fineLocationGranted) {
                allPermissionsGranted = false
                Toast.makeText(this, "Permiso de UBICACIÓN denegado. Necesario para el escaneo Bluetooth.", Toast.LENGTH_LONG).show()
            }
        }

        if (allPermissionsGranted) {
            // Si todos los permisos críticos fueron concedidos, procede a verificar el estado de Bluetooth.
            checkBluetoothEnabled()
        } else {
            // Algunos permisos fueron denegados. Notifica al usuario.
            // Considera guiar al usuario a la configuración si los permisos son vitales para la funcionalidad principal.
        }
    }

    // Launcher para solicitar la activación de Bluetooth
    private val enableBluetoothLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        // Aquí puedes manejar el resultado de la solicitud para encender Bluetooth
        if (result.resultCode == RESULT_OK) {
            Toast.makeText(this, "Bluetooth activado.", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Bluetooth no fue activado. Es necesario para la conexión.", Toast.LENGTH_LONG).show()
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Inicializar el servicio Bluetooth al inicio de la aplicación
        BluetoothService.initialize(applicationContext)

        // Solicitar permisos Bluetooth al iniciar la actividad
        requestBluetoothPermissions()

        setContent {
            ControlSalasAppTheme {
                val auth = Firebase.auth
                // Se utiliza `remember` con una clave para que el estado de autenticación se reinicie si el usuario cambia
                val isLoggedIn = remember(auth.currentUser) { mutableStateOf(auth.currentUser != null) }
                val navController = rememberNavController()

                // Define la pantalla de inicio basada en el estado de autenticación del usuario
                val startDestination = if (isLoggedIn.value) "salas" else "login"

                NavHost(
                    navController = navController,
                    startDestination = startDestination
                    // <<--- ELIMINADA: La línea 'isTestMode = false' de aquí
                ) {
                    composable("login") {
                        LoginScreen(
                            onLoginSuccess = {
                                navController.navigate("salas") {
                                    popUpTo("login") { inclusive = true } // Elimina la pantalla de login de la pila
                                }
                            },
                            onRegisterClick = {
                                navController.navigate("register") // Navega a la pantalla de registro
                            }
                        )
                    }

                    composable("register") {
                        RegisterScreen(
                            onRegisterSuccess = {
                                navController.navigate("salas") {
                                    popUpTo("login") { inclusive = true } // Después del registro, ve a salas y limpia la pila
                                }
                            },
                            onBackToLogin = {
                                navController.popBackStack() // Vuelve a la pantalla de login
                            }
                        )
                    }

                    composable("salas") {
                        PantallaSalas(
                            onSalaSeleccionada = { sala ->
                                // Serializa el objeto Sala a JSON para pasarlo como argumento a la siguiente pantalla
                                val salaJson = Gson().toJson(sala)
                                navController.navigate("control_sala/$salaJson")
                            },
                            onLogout = {
                                auth.signOut() // Cierra la sesión de Firebase
                                // Asegurarse de desconectar Bluetooth al cerrar sesión para liberar recursos
                                BluetoothService.disconnect()
                                navController.navigate("login") {
                                    popUpTo(0) // Limpia toda la pila de navegación y va a login
                                }
                            }
                        )
                    }

                    composable(
                        route = "control_sala/{salaJson}", // Define la ruta con un argumento JSON
                        arguments = listOf(navArgument("salaJson") { type = NavType.StringType }) // Especifica el tipo de argumento
                    ) { backStackEntry ->
                        val salaJson = backStackEntry.arguments?.getString("salaJson") // Recupera el JSON
                        // Deserializa el objeto Sala desde el JSON
                        val sala = Gson().fromJson(salaJson, Sala::class.java)
                        PantallaDispositivos(
                            sala = sala,
                            navController = navController,
                            isTestMode = false
                        )
                    }
                }
            }
        }
    }

    // Función auxiliar para verificar y solicitar la activación de Bluetooth
    private fun checkBluetoothEnabled() {
        val bluetoothManager: BluetoothManager? = getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
        val bluetoothAdapter: BluetoothAdapter? = bluetoothManager?.adapter
        if (bluetoothAdapter != null && !bluetoothAdapter.isEnabled) {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            enableBluetoothLauncher.launch(enableBtIntent)
        }
    }

    // Función principal para solicitar los permisos necesarios para Bluetooth
    private fun requestBluetoothPermissions() {
        val permissionsToRequest = mutableListOf<String>()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) { // Para Android 12 (API 31) y versiones posteriores
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.BLUETOOTH_CONNECT)
            }
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.BLUETOOTH_SCAN)
            }
            // Nota: BLUETOOTH_ADVERTISE también podría ser necesario si la app anuncia sus servicios BLE.
            // Por ahora, BLUETOOTH_CONNECT y BLUETOOTH_SCAN son los más comunes para cliente.
        } else { // Para Android 11 (API 30) y versiones anteriores
            // BLUETOOTH_ADMIN y BLUETOOTH son permisos normales (no de runtime) en estas versiones,
            // se otorgan al instalar la app.
            // ACCESS_FINE_LOCATION (o ACCESS_COARSE_LOCATION) es un permiso de runtime necesario para el escaneo.
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.ACCESS_FINE_LOCATION)
            }
        }

        if (permissionsToRequest.isNotEmpty()) {
            requestPermissionLauncher.launch(permissionsToRequest.toTypedArray())
        } else {
            // Todos los permisos ya están concedidos.
            // Procede a verificar si Bluetooth está encendido.
            checkBluetoothEnabled()
        }
    }
}


@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    onRegisterClick: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val scope = rememberCoroutineScope()
    val auth = Firebase.auth

    // Valida que el email termine en @umayor.cl y sea un formato de email válido
    val isEmailValid = email.endsWith("@umayor.cl") &&
            android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
    val isFormValid = isEmailValid && password.length >= 6 // Contraseña de al menos 6 caracteres

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("ControlApp", style = MaterialTheme.typography.headlineLarge)

        Spacer(modifier = Modifier.height(24.dp))

        Text("Iniciar Sesión", style = MaterialTheme.typography.titleMedium)

        // Muestra mensajes de error si existen
        errorMessage?.let {
            Text(
                text = it,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(bottom = 8.dp))
        }

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Correo institucional (@umayor.cl)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            isError = email.isNotEmpty() && !isEmailValid // Indica error si el email no es válido
        )

        if (email.isNotEmpty() && !isEmailValid) {
            Text(
                text = "Debe ser un correo @umayor.cl válido",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.align(Alignment.Start)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Contraseña") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            visualTransformation = PasswordVisualTransformation(), // Oculta la contraseña
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            isError = password.isNotEmpty() && password.length < 6 // Indica error si la contraseña es corta
        )

        if (password.isNotEmpty() && password.length < 6) {
            Text(
                text = "La contraseña debe tener al menos 6 caracteres",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.align(Alignment.Start)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                scope.launch {
                    isLoading = true
                    errorMessage = null
                    try {
                        auth.signInWithEmailAndPassword(email, password).await()
                        onLoginSuccess() // Navega si el login es exitoso
                    } catch (e: Exception) {
                        errorMessage = when (e) {
                            is FirebaseAuthInvalidUserException -> "Usuario no registrado"
                            is FirebaseAuthInvalidCredentialsException -> "Credenciales incorrectas"
                            else -> "Error al iniciar sesión: ${e.localizedMessage}"
                        }
                    } finally {
                        isLoading = false
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            enabled = isFormValid && !isLoading // Botón habilitado solo si el formulario es válido y no está cargando
        ) {
            if (isLoading) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary) // Muestra indicador de carga
            } else {
                Text("Iniciar Sesión")
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = onRegisterClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            enabled = !isLoading
        ) {
            Text("Registrarse")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Al hacer clic en continuar, aceptas nuestros Términos de Servicio y Política de Privacidad.",
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(top = 8.dp),
            maxLines = 2
        )
    }
}

@Composable
fun RegisterScreen(
    onRegisterSuccess: () -> Unit,
    onBackToLogin: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val scope = rememberCoroutineScope()
    val auth = Firebase.auth

    val isEmailValid = email.endsWith("@umayor.cl") &&
            android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
    val isPasswordValid = password.length >= 6
    val passwordsMatch = password == confirmPassword
    val isFormValid = isEmailValid && isPasswordValid && passwordsMatch

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Registro", style = MaterialTheme.typography.headlineMedium)

        Spacer(modifier = Modifier.height(24.dp))

        errorMessage?.let {
            Text(
                text = it,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(bottom = 8.dp))
        }

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Correo institucional (@umayor.cl)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            isError = email.isNotEmpty() && !isEmailValid
        )

        if (email.isNotEmpty() && !isEmailValid) {
            Text(
                text = "Debe ser un correo @umayor.cl válido",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.align(Alignment.Start)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Contraseña (mínimo 6 caracteres)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            isError = password.isNotEmpty() && !isPasswordValid
        )

        if (password.isNotEmpty() && !isPasswordValid) {
            Text(
                text = "La contraseña debe tener al menos 6 caracteres",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.align(Alignment.Start)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = confirmPassword,
            onValueChange = { confirmPassword = it },
            label = { Text("Confirmar Contraseña") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            isError = confirmPassword.isNotEmpty() && !passwordsMatch
        )

        if (confirmPassword.isNotEmpty() && !passwordsMatch) {
            Text(
                text = "Las contraseñas no coinciden",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.align(Alignment.Start)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                scope.launch {
                    isLoading = true
                    errorMessage = null
                    try {
                        auth.createUserWithEmailAndPassword(email, password).await()
                        onRegisterSuccess()
                    } catch (e: Exception) {
                        errorMessage = when (e) {
                            is FirebaseAuthInvalidCredentialsException -> "Correo electrónico inválido"
                            else -> "Error al registrar: ${e.localizedMessage}"
                        }
                    } finally {
                        isLoading = false
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            enabled = isFormValid && !isLoading
        ) {
            if (isLoading) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary)
            } else {
                Text("Registrarse")
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = onBackToLogin,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            enabled = !isLoading
        ) {
            Text("Volver a Iniciar Sesión")
        }
    }
}
