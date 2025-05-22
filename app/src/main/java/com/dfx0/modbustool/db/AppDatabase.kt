package com.dfx0.modbustool.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.dfx0.modbustool.dao.VarTagDao
import com.dfx0.modbustool.model.VarTag
import com.dfx0.modbustool.model.converts.EnumConvert

@Database(entities = [VarTag::class], version = 1)
@TypeConverters(EnumConvert::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun varTagDao(): VarTagDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "modbus-db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
