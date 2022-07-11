package com.seoul42.relief_post_office

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.seoul42.relief_post_office.util.Alarm.isIgnoringBatteryOptimizations
import com.seoul42.relief_post_office.databinding.MainBinding
import com.seoul42.relief_post_office.record.RecordActivity

class MainActivity : AppCompatActivity() {

    private val binding by lazy {
        MainBinding.inflate(layoutInflater)
    }

    private val requiredPermissions = arrayOf(
        android.Manifest.permission.RECORD_AUDIO,
        Manifest.permission.READ_EXTERNAL_STORAGE
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        setBatteryOptimization()
        requestAudioPermission()
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
        binding.mainButton.setOnClickListener {
            startActivity(Intent(this, PhoneActivity::class.java))
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        // 요청한 권한에 대한 결과

        val audioRecordPermissionGranted =
            requestCode == RecordActivity.REQUEST_RECORD_AUDIO_PERMISSION &&
                    grantResults.firstOrNull() == PackageManager.PERMISSION_GRANTED

        if (!audioRecordPermissionGranted) {
            finish()
        }
    }

    private fun requestAudioPermission() {
        requestPermissions(requiredPermissions, RecordActivity.REQUEST_RECORD_AUDIO_PERMISSION)
    }
}