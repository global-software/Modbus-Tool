package com.dfx0.modbustool.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ColumnInfo

@Entity(tableName = "modbus_tag")
data class VarTag(
    @PrimaryKey
    @ColumnInfo(name = "tag")
    val tag: String,

    @ColumnInfo(name = "data_type")
    val dataType: String? = null,

    @ColumnInfo(name = "describe")
    val describe: String? = null,

    @ColumnInfo(name = "true_text")
    val trueText: String? = null,

    @ColumnInfo(name = "false_text")
    val falseText: String? = null,

    @ColumnInfo(name = "modbus_address")
    val modBusAddress: Int? = -1,

    @ColumnInfo(name = "unit")
    val unit: String? = ""
)
