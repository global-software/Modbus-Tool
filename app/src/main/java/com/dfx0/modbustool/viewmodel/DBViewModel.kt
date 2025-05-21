package com.dfx0.modbustool.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.room.Room
import com.dfx0.modbustool.db.AppDatabase

class DBViewModel (application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getInstance(application)
    private val dao = db.varTagDao()

    fun getVarTagDao() = dao
}