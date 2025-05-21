package com.dfx0.modbustool.viewmodel

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class SharedViewModel : ViewModel() {
    private val _isConnectedPLC = MutableStateFlow(false)
    val isConnectedPLC: StateFlow<Boolean> = _isConnectedPLC

    fun updateConnectedPLC(newValue: Boolean) {
        _isConnectedPLC.value = newValue
    }
}