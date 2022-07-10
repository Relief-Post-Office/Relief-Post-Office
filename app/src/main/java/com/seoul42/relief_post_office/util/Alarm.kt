package com.seoul42.relief_post_office.util

import android.content.Context
import android.os.Build
import android.os.PowerManager

internal object Alarm {
    /* 배터리 최적화 */
    fun isIgnoringBatteryOptimizations(context: Context): Boolean {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            powerManager.isIgnoringBatteryOptimizations(context.packageName)
        } else true
    }
}