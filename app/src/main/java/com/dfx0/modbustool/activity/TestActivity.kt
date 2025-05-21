package com.dfx0.modbustool.activity

import com.dfx0.modbustool.R
import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import com.dfx0.modbustool.activity.utils.ModbusManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class TestActivity : Activity() {
    var editTextRead: EditText? = null;
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main) // 设置 XML 布局
        initModbusConnect()
        val editTextAddress = findViewById<EditText>(R.id.editTextAddress)
        val editTextValue = findViewById<EditText>(R.id.editTextValue)
        val connectButton = findViewById<Button>(R.id.buttonSubmit)
        editTextRead = findViewById<EditText>(R.id.editTextRead)
        readThread()
        connectButton.setOnClickListener {
            CoroutineScope(Dispatchers.IO).launch {
                val value = editTextValue.text.toString()
                val address = editTextAddress.text.toString()
                val r = ModbusManager.writeHoldingRegister(address.toInt() ,value.toInt())
                if(r)
                    ToastInContext(this@TestActivity, "写入成功")
                else
                    ToastInContext(this@TestActivity, "写入失败")
            }
        }
    }

    private fun initModbusConnect(){
        CoroutineScope(Dispatchers.IO).launch {
            val result = ModbusManager.initTcp()
            if (result) {
                ToastInContext(this@TestActivity, "ModBus 连接成功")
            } else {
                ToastInContext(this@TestActivity, "ModBus 连接失败")
            }
        }
    }

    private fun ToastInContext(context:Context, message:String){
        CoroutineScope(Dispatchers.Main).launch {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

    private fun readThread(){
        CoroutineScope(Dispatchers.IO).launch {
            while (true){
                val r = ModbusManager.readMultipleHoldingRegisters(0,10)
                if(r != null && editTextRead?.text.toString()!=r.toString())
                    CoroutineScope(Dispatchers.Main).launch {
                        editTextRead?.setText(r.toString())
                    }
                delay(100)
            }
        }
    }
}
