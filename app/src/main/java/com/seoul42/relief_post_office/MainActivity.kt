package com.seoul42.relief_post_office

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.seoul42.relief_post_office.databinding.MainBinding

class MainActivity : AppCompatActivity() {

    private val binding by lazy {
        MainBinding.inflate(layoutInflater)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        setLogin()
    }

    private fun setLogin() {
        binding.mainButton.setOnClickListener {
            startActivity(Intent(this, PhoneActivity::class.java))
        }
    }
}