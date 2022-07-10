package com.seoul42.relief_post_office.safety

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import com.bumptech.glide.Glide
import com.seoul42.relief_post_office.R

class WardSafetySettingActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ward_safety_setting)

        val wardId = intent.getStringExtra("wardId")
        val wardName = intent.getStringExtra("wardName")
        val photoUri = intent.getStringExtra("photoUri")

        // 액티비티 화면 세팅
        // 이름 세팅
        findViewById<TextView>(R.id.ward_safety_setting_text).setText("${wardName} 님의 안부")
        // 사진 세팅
        Glide.with(this)
            .load(photoUri)
            .circleCrop()
            .into(findViewById(R.id.ward_safety_setting_ward_photo))



        // 종료 버튼 이벤트
        findViewById<ImageView>(R.id.ward_safety_setting_exit_button).setOnClickListener {
            finish()
        }

        // 추가 버튼 이벤트
        findViewById<ImageView>(R.id.ward_safety_setting_add_button).setOnClickListener {
            startActivity(Intent(this, AddWardSafetyActivity::class.java))
        }

        // 리사이클러 뷰 설정
    }
}