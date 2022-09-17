package com.seoul42.relief_post_office.login

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.seoul42.relief_post_office.R
import com.seoul42.relief_post_office.databinding.MainBinding
import com.seoul42.relief_post_office.record.RecordActivity

/**
 * 유저가 로그아웃 또는 최초 앱 실행 시 이동되는 클래스
 * 오디오 퍼미션 설정을 수락해야만 앱 수행이 가능하도록 설정
 */
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

        setLogin()
        requestAudioPermission()
    }

    private fun setLogin() {
        binding.mainButton.setOnClickListener {
            startActivity(Intent(this, PhoneActivity::class.java))
        }
    }

    /**
     * 요청 권한에 대한 결과가 파라메터로 받아옴
     *  - requestCode : 오디오 퍼미션인지 아닌지 확인
     *  - grantResults : 퍼미션 허가가 되었는지 아닌지 확인
     */
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        // 요청한 권한에 대한 결과
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

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