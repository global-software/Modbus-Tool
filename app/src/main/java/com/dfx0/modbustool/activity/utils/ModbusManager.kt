package com.dfx0.modbustool.activity.utils

import android.util.Log
import com.serotonin.modbus4j.ModbusFactory
import com.serotonin.modbus4j.ip.IpParameters
import com.serotonin.modbus4j.ModbusMaster
import com.serotonin.modbus4j.code.DataType
import com.serotonin.modbus4j.locator.BaseLocator
import com.serotonin.modbus4j.msg.ReadHoldingRegistersRequest
import com.serotonin.modbus4j.msg.ReadHoldingRegistersResponse
import com.serotonin.modbus4j.msg.WriteRegistersRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object ModbusManager {
    private val TAG = "ModbusManager"
    private var master: ModbusMaster? = null
    private var ip = "192.168.1.88"
    private var port = "502"

    /**
     * 初始化 Modbus TCP 连接
     */
    suspend fun initTcp(ip:String = "192.168.1.88"): Boolean = withContext(Dispatchers.IO) {
        val params = IpParameters().apply {
            this.host = ip
            this.port = port
        }

        master = ModbusFactory().createTcpMaster(params, true) // true = keep-alive
        try {
            master?.init()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }


    /**
     * 读取保持寄存器
     */
    suspend fun readHoldingRegister(offset: Int,slaveId: Int = 1): Int? {
        return try {
            val locator = BaseLocator.holdingRegister(slaveId, offset, DataType.TWO_BYTE_INT_SIGNED)
            master?.getValue(locator) as? Int
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * 写入保持寄存器
     */
    suspend fun writeHoldingRegister(offset: Int, value: Int,slaveId: Int = 1): Boolean {
        return  withContext(Dispatchers.IO) {
            try {
                val locator =
                    BaseLocator.holdingRegister(slaveId, offset, DataType.TWO_BYTE_INT_SIGNED)
                master?.setValue(locator, value)
                true
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }
    }

    /**
     * 批量写入
     */
    suspend fun writeMultipleHoldingRegisters(offset: Int, values: List<Int>, slaveId: Int = 1): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                // 转换 Int 为 Short 数组，因为 modbus4j 要求写入 short[]
                val shortValues = values.map { it.toShort() }.toShortArray()
                val request = WriteRegistersRequest(slaveId, offset, shortValues)

                val response = master?.send(request)
                response != null && !response.isException
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }
    }


    /**
     * 批量读取
     */
    suspend fun readMultipleHoldingRegisters(
        startOffset: Int?,
        count: Int?,
        slaveId: Int = 1
    ): List<Int>? {
        return withContext(Dispatchers.IO) {
            try {
                val request = ReadHoldingRegistersRequest(slaveId, startOffset ?: 0, count ?: 15)
                val response = master?.send(request) as? ReadHoldingRegistersResponse

                if (response == null || response.isException) {
                    Log.e(TAG,"Modbus 响应错误: ${response?.exceptionMessage}")
                    null
                } else {
                    // 提取 short[] 数组并转为 Int
                    response.shortData.map { it.toInt() }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }


    fun close() {
        master?.destroy()
        master = null
    }
}
