package com.seoul42.relief_post_office.safety

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import com.seoul42.relief_post_office.R

class WardSafetySettingActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ward_safety_setting)

        // 액티비티 화면 세팅


        // 종료 버튼 이벤트
        findViewById<ImageView>(R.id.ward_safety_setting_exit_button).setOnClickListener {
            finish()
        }

        // 추가 버튼 이벤트
        findViewById<ImageView>(R.id.ward_safety_setting_add_button).setOnClickListener {
        }

        // 리사이클러 뷰 설정
    }
}