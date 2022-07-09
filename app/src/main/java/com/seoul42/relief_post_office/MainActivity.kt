package com.seoul42.relief_post_office

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.seoul42.relief_post_office.util.Alarm.isIgnoringBatteryOptimizations


class MainActivity : AppCompatActivity() {

    private val loadButton: Button by lazy {
        findViewById<Button>(R.id.main_button)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main)

        setBatteryOptimization()
        setLogin()
    }

    private fun setBatteryOptimization() {
        /* 최초 앱 실행시 배터리 최적화 작업 수행 */
        if (!isIgnoringBatteryOptimizations(this)) {
            val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)

            intent.data = Uri.parse("package:$packageName")
            startActivity(intent)
        }
    }

    private fun setLogin() {
        loadButton.setOnClickListener {
            startActivity(Intent(this, PhoneActivity::class.java))
        }
    }
}