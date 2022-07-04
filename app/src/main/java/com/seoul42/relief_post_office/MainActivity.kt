package com.seoul42.relief_post_office

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity


class MainActivity : AppCompatActivity() {

    private val loadButton: Button by lazy {
        findViewById<Button>(R.id.main_button)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main)

        processLogout()
    }

    private fun processLogout() {
        loadButton.setOnClickListener {
            startActivity(Intent(this, PhoneActivity::class.java))
        }
    }
}