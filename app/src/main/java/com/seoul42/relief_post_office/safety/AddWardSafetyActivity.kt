package com.seoul42.relief_post_office.safety

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.EditText
import android.widget.ImageView
import com.seoul42.relief_post_office.R

class AddWardSafetyActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_ward_safety)

        // 뒤로 가기 버튼 이벤트
        findViewById<ImageView>(R.id.add_ward_safety_backBtn).setOnClickListener{
            finish()
        }

        // 안부 이름 설정 이벤트

        // 요일 설정 이벤트

        // 시간 설정 이벤트

        // 질문 설정 이벤트

        // 저장 버튼 이벤트

        // 리사이클러 뷰 세팅

    }
}