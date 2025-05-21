package com.dfx0.modbustool.dao

import androidx.room.*
import com.dfx0.modbustool.model.VarTag

@Dao
interface VarTagDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(tag: VarTag)

    @Update
    suspend fun update(tag: VarTag)

    @Delete
    suspend fun delete(tag: VarTag)

    @Query("SELECT * FROM modbus_tag WHERE tag = :tag LIMIT 1")
    suspend fun getByTag(tag: String): VarTag?

    @Query("SELECT * FROM modbus_tag order by tag")
    suspend fun getAll(): List<VarTag>
}