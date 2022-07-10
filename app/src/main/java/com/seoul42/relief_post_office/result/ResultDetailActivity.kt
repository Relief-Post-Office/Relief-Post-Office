package com.seoul42.relief_post_office.result

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.seoul42.relief_post_office.databinding.ActivityResultDetailBinding

class ResultDetailActivity : AppCompatActivity() {
    private val binding by lazy { ActivityResultDetailBinding.inflate(layoutInflater) }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
    }
}