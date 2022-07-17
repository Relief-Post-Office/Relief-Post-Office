package com.seoul42.relief_post_office.login

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.seoul42.relief_post_office.R
import com.seoul42.relief_post_office.databinding.MainBinding
import com.seoul42.relief_post_office.record.RecordActivity

class MainActivity : AppCompatActivity() {

    private val binding by lazy {
        MainBinding.inflate(layoutInflater)
    }

    private val requiredPermissions = arrayOf(
        Manifest.permission.RECORD_AUDIO
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        requestAudioPermission()
        setLogin()
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