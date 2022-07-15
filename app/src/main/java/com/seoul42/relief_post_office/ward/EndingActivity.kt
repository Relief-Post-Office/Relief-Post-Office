package com.seoul42.relief_post_office.ward

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.seoul42.relief_post_office.databinding.WardEndingBinding

class EndingActivity : AppCompatActivity() {

    private val binding: WardEndingBinding by lazy {
        WardEndingBinding.inflate(layoutInflater)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
    }
}