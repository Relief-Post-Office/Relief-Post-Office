package com.seoul42.relief_post_office.safety

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.seoul42.relief_post_office.R
import com.seoul42.relief_post_office.adapter.QuestionFragmentRVAdapter
import com.seoul42.relief_post_office.adapter.WardSafetyAdapter
import com.seoul42.relief_post_office.model.SafetyDTO

class WardSafetySettingActivity : AppCompatActivity() {

    private val database = Firebase.database
    private lateinit var wardId : String
    private lateinit var wardName : String
    private lateinit var photoUri : String
    // 피보호자 안부 리스트
    private lateinit var WardSafetyList : ArrayList<Pair<String, SafetyDTO>>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ward_safety_setting)

        wardId = intent.getStringExtra("wardId").toString()
        wardName = intent.getStringExtra("wardName").toString()
        photoUri = intent.getStringExtra("photoUri").toString()

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

        // WardSafetyList 세팅 및 업데이트 하기
        // 현재 선택한 피보호자의 안부 목록
        val wardSafetyRef = database.getReference("ward").child(wardId).child("safetyIdList")

        // questionList에 로그인한 유저의 질문들 넣기
        wardSafetyRef.addChildEventListener(object : ChildEventListener{
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
            }

            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
            }

            override fun onChildRemoved(snapshot: DataSnapshot) {
            }

            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {
            }

            override fun onCancelled(error: DatabaseError) {
            }

        })

        // 리사이클러 뷰 설정
        // 리사이클러 뷰 가져오기
        val rv = findViewById<RecyclerView>(R.id.ward_safety_setting_rv)
        // 리사이클러 뷰 아답터에 리스트 넘긴 후 아답터 가져오기
        val rvAdapter = WardSafetyAdapter(WardSafetyList)
        // 리사이클러 뷰에 아답터 연결하기
        rv.adapter = rvAdapter
        rv.layoutManager = LinearLayoutManager(this)
        rv.setHasFixedSize(true)
    }
}