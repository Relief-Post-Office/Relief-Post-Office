package com.seoul42.relief_post_office.ward

import android.media.MediaPlayer
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.seoul42.relief_post_office.R
import com.seoul42.relief_post_office.databinding.WardEndingBinding

class EndingActivity : AppCompatActivity() {

    private val binding: WardEndingBinding by lazy {
        WardEndingBinding.inflate(layoutInflater)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        // 보이스 재생 후 종료
        val endingGuideVoice = MediaPlayer.create(this, R.raw.safetyending)
        endingGuideVoice.setOnCompletionListener {
            endingGuideVoice.release()
            finish()
        }
        endingGuideVoice.start()
    }
}