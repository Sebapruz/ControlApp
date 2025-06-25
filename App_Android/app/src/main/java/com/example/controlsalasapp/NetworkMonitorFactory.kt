package com.example.controlsalasapp

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class NetworkMonitorFactory(private val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(NetworkMonitor::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return NetworkMonitor(context) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
