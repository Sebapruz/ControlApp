package com.example.controlsalasapp

// data class para representar una sala, con macAddress y uuidString para Bluetooth
data class Sala(
    val nombre: String,
    val id: Int,
    val macAddress: String, // Dirección MAC del ESP32 para Bluetooth
    val uuidString: String  // UUID del servicio SPP de Bluetooth del ESP32
)
