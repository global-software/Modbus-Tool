package com.dfx0.modbustool.utils

import android.content.Context
import androidx.core.content.edit

object PrefsUtil {
    private const val PREF_NAME = "my_config"

    fun saveIpAddress(context: Context, ip: String) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE).edit {
            putString("ip_address", ip)
        }
    }

    fun getIpAddress(context: Context): String {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .getString("ip_address", "") ?: ""
    }
}
