package com.seoul42.relief_post_office.ward

import android.media.MediaPlayer
import android.os.Bundle
import android.os.Handler
import android.view.WindowManager
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.seoul42.relief_post_office.R
import com.seoul42.relief_post_office.databinding.WardEndingBinding
import com.seoul42.relief_post_office.model.*
import com.seoul42.relief_post_office.viewmodel.FirebaseViewModel

/**
 * 피보호자가 모든 응답을 완료한 경우 종료 음성이 끝나면 종료되도록 하는 클래스
 */
class EndingActivity : AppCompatActivity() {

    private val binding: WardEndingBinding by lazy {
        WardEndingBinding.inflate(layoutInflater)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        val endingGuideVoice = MediaPlayer.create(this, R.raw.safetyending)

        endingGuideVoice.start()
        setStatusBarTransparent()

        // 종료 음성이 끝나면 종료처리
        endingGuideVoice.setOnCompletionListener {
            endingGuideVoice.release()
            finish()
        }
    }

    private fun setStatusBarTransparent() {
        window.setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)
    }
}