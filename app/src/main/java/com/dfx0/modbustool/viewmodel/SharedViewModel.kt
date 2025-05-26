package com.dfx0.modbustool.viewmodel

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.lifecycle.ViewModel
import com.dfx0.modbustool.model.VarTag
import com.dfx0.modbustool.model.enums.VarType
import com.dfx0.modbustool.utils.ModbusManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class SharedViewModel : ViewModel() {
    private val _isConnectedPLC = MutableStateFlow(false)
    private val _logText = MutableStateFlow("")
    private val _varTags = MutableStateFlow<List<VarTag>>(emptyList())
    private val _varTagValueDic = MutableStateFlow<Map<String,String>>(emptyMap())
    val isConnectedPLC: StateFlow<Boolean> = _isConnectedPLC
    val getVarTag: StateFlow<List<VarTag>> = _varTags
    val getTagValueDic: StateFlow<Map<String,String>> = _varTagValueDic
    val getLogText : StateFlow<String> = _logText



    @RequiresApi(Build.VERSION_CODES.O)
    fun log(log: String){
        val now = LocalDateTime.now()
        val formatted = now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
        _logText.value = "[${formatted}]" + log + "\n\r" + _logText.value
    }

    fun clearLog(){
        _logText.value = ""
    }


    fun updateVarTagList(varTags : List<VarTag>){
        _varTags.value = ModbusManager.updateModbusAddresses(varTags)
    }

    fun updateConnectedPLC(newValue: Boolean) {
        _isConnectedPLC.value = newValue
    }

    fun updateVarTag(tag: VarTag) {
        _varTags.value = _varTags.value.map {
            if (it.tag == tag.tag) tag else it
        }
    }

    /**
     * Update the varTag value
     */
    fun updateVarTagDic(tag:String,value:String) {
        _varTagValueDic.value = _varTagValueDic.value.toMutableMap().apply {
            this[tag] = value
        }
    }

    fun initializeVarTagValuesIfNeeded() {
        val currentTags = _varTags.value
        val currentValues = _varTagValueDic.value.toMutableMap()
        currentTags.forEach { tag ->
            if (tag.tag !in currentValues) {
                currentValues[tag.tag] = when (tag.dataType) {
                    VarType.BOOL -> "0"
                    VarType.JoyBOOL -> "0"
                    else -> "null"
                }
            }
        }
        _varTagValueDic.value = currentValues
    }

    private var readJob: Job? = null
    suspend fun readVarTag(){
        if (readJob?.isActive == true) return
        readJob = CoroutineScope(Dispatchers.IO).launch {
            while (true){
                val currentValues = _varTagValueDic.value.toMutableMap()
                _varTags.value.forEach {
                    if(it.modBusAddress != null && it.dataType!=null) {
                        var v = ModbusManager.readModbusValue(it.modBusAddress, it.dataType)
                        if(it.dataType == VarType.BOOL || it.dataType == VarType.JoyBOOL)
                            if(v.toString() == "1" || v.toString() == "true")
                                currentValues[it.tag] = "true"
                            else
                                currentValues[it.tag] = "false"
                        else
                            currentValues[it.tag] = v.toString()
                    }
                }
                _varTagValueDic.value = currentValues
                delay(1)
            }
        }
    }
}