package com.dfx0.modbustool.viewmodel

import androidx.lifecycle.ViewModel
import com.dfx0.modbustool.model.VarTag
import com.dfx0.modbustool.model.enums.VarType
import com.dfx0.modbustool.utils.ModbusManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class SharedViewModel : ViewModel() {
    private val _isConnectedPLC = MutableStateFlow(false)
    private val _varTags = MutableStateFlow<List<VarTag>>(emptyList())
    private val _varTagValueDic = MutableStateFlow<Map<String,String>>(emptyMap())
    val isConnectedPLC: StateFlow<Boolean> = _isConnectedPLC
    val getVarTag: StateFlow<List<VarTag>> = _varTags
    val getTagValueDic: StateFlow<Map<String,String>> = _varTagValueDic
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
}