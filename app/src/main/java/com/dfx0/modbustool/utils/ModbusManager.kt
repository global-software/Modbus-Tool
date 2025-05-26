package com.dfx0.modbustool.utils

import android.util.Log
import com.dfx0.modbustool.model.VarTag
import com.dfx0.modbustool.model.enums.VarType
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
import java.nio.ByteBuffer
import java.nio.ByteOrder

object ModbusManager {
    private val TAG = "ModbusManager"
    private var master: ModbusMaster? = null
    private var ip = "192.168.1.88"
    private var port = "502"
    private val slaveId = 1

    /**
     * Initial The modbus Tcp
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
     * Read Holding Register
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
     * Write Holding Register
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
     * Batch of write to the Modbus
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
     * Batch of read to the Modbus
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

    /**
     * Compute the VarTag modbus address
     */
    fun updateModbusAddresses(varTags: List<VarTag>): List<VarTag> {
        return varTags.map { tag ->
            val updatedAddress = try {
                when {
                    tag.tag.startsWith("%QX") -> {
                        val r = tag.tag.replace("%QX", "").split('.')
                        40960 + r[0].toInt() * 8 + r[1].toInt()
                    }
                    tag.tag.startsWith("%MW") -> {
                        tag.tag.replace("%MW", "").toInt()
                    }
                    tag.tag.startsWith("%MD") -> {
                        tag.tag.replace("%MD", "").toInt() * 2
                    }
                    tag.tag.startsWith("%IX") -> {
                        val r = tag.tag.replace("%IX", "").split('.')
                        24576 + r[0].toInt() * 8 + r[1].toInt()
                    }
                    tag.tag.startsWith("%MX") -> {
                        val r = tag.tag.replace("%MX", "").split('.')
                        r[0].toInt() / 2
                    }
                    else -> tag.modBusAddress ?: -1
                }
            } catch (e: Exception) {
                println("解析 ${tag.tag} 时出错: ${e.message}")
                tag.modBusAddress ?: -1
            }

            // 返回更新后的 VarTag
            tag.copy(modBusAddress = updatedAddress)
        }
    }


    fun readModbusValue(
        address: Int,
        type: VarType,
        isHolding: Boolean = true,
        byteOrder: ByteOrder = ByteOrder.BIG_ENDIAN
    ): Any? {
        return try {
            val locator = when (type) {
                VarType.BOOL,VarType.JoyBOOL -> {
                    if(address>= 24576)
                        return BaseLocator.coilStatus(slaveId, address)
                    else {
                        //if the modbus address doesn't come from %IX or %QX,read it from %MW
                        return createLocator(slaveId, address, DataType.TWO_BYTE_INT_SIGNED, isHolding)
                    }
                }
                VarType.INT16 -> createLocator(slaveId, address, DataType.TWO_BYTE_INT_SIGNED, isHolding)
                VarType.UINT16 -> createLocator(slaveId, address, DataType.TWO_BYTE_INT_UNSIGNED, isHolding)
                VarType.INT32 -> createLocator(slaveId, address, DataType.FOUR_BYTE_INT_SIGNED, isHolding)
                VarType.UINT32 -> createLocator(slaveId, address, DataType.FOUR_BYTE_INT_UNSIGNED, isHolding)
                VarType.REAL -> createLocator(slaveId, address, DataType.FOUR_BYTE_FLOAT, isHolding)
                else -> throw IllegalArgumentException("不支持的数据类型: $type")
            }

            val rawValue = master?.getValue(locator)

            // 对 byteOrder 的支持：Modbus4j 不自动转换字节序，如果需要处理小端序，自行转换
            if (rawValue is Number && byteOrder == ByteOrder.LITTLE_ENDIAN && (type== VarType.INT32|| type == VarType.REAL)) {
                val bytes = ByteBuffer.allocate(4).order(ByteOrder.BIG_ENDIAN).putInt(rawValue.toInt()).array()
                val reversed = ByteBuffer.wrap(bytes.reversedArray()).order(ByteOrder.BIG_ENDIAN)
                return when (type) {
                    VarType.REAL -> reversed.float
                    VarType.INT32 -> reversed.int
                    else -> rawValue
                }
            }

            rawValue
        } catch (e: Exception) {
            println("读取失败: ${e.message}")
            null
        }
    }

    private fun createLocator(slaveId: Int, address: Int, dataType: Int, isHolding: Boolean): BaseLocator<*> {
        return if (isHolding) {
            BaseLocator.holdingRegister(slaveId, address, dataType)
        } else {
            BaseLocator.inputRegister(slaveId, address, dataType)
        }
    }



    fun close() {
        master?.destroy()
        master = null
    }
}
