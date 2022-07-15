package com.seoul42.relief_post_office.ward

import android.media.MediaPlayer
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.seoul42.relief_post_office.databinding.WardSafetyBinding
import com.seoul42.relief_post_office.model.WardRecommendDTO

class QuestionActivity : AppCompatActivity() {

    private val binding: WardSafetyBinding by lazy {
        WardSafetyBinding.inflate(layoutInflater)
    }
    private lateinit var safetyId : String
    private lateinit var resultId : String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        setQuestion()
    }

    private fun setQuestion() {
        safetyId = intent.getStringExtra("safetyId").toString()
        resultId = intent.getStringExtra("resultId").toString()
    }
}