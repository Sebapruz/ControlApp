package com.example.controlsalasapp

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaSalas(
    onSalaSeleccionada: (Sala) -> Unit,
    onLogout: () -> Unit
) {
    val salas = remember {
        listOf(
            // !IMPORTANTE! Reemplaza con la MAC y UUID REALES de tu ESP32
            // Obtén la MAC Bluetooth del Monitor Serial de tu ESP32.
            // El UUID es el estándar para el Serial Port Profile (SPP) de Bluetooth.
            Sala("MALI-201", 201, "94:54:C5:A9:9C:72", "00001101-0000-1000-8000-00805F9B34FB"), // Reemplaza "00:00:00:00:00:00" y verifica el UUID
            Sala("MALI-202", 202, "00:00:00:00:00:00", "00001101-0000-1000-8000-008055554444"), // Reemplaza
            Sala("MALI-203", 203, "00:00:00:00:00:00", "00001101-0000-1000-8000-008055554444")  // Reemplaza
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Selecciona una Sala") },
                actions = {
                    IconButton(onClick = onLogout) {
                        Icon(Icons.Filled.ExitToApp, contentDescription = "Cerrar Sesión")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Salas",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )

            Spacer(modifier = Modifier.height(24.dp))

            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(salas) { sala ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onSalaSeleccionada(sala)
                            }
                            .padding(vertical = 4.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = sala.nombre, style = MaterialTheme.typography.titleMedium)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
