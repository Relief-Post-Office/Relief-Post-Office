package com.seoul42.relief_post_office.safety

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
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
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.seoul42.relief_post_office.R
import com.seoul42.relief_post_office.adapter.QuestionFragmentRVAdapter
import com.seoul42.relief_post_office.adapter.WardSafetyAdapter
import com.seoul42.relief_post_office.model.ListenerDTO
import com.seoul42.relief_post_office.model.SafetyDTO

/**
 * "피보호자 안부 설정" 화면을 띄우도록 돕는 클래스
 */
class WardSafetySettingActivity : AppCompatActivity() {

    private val database = Firebase.database
    // RecyclerView에 띄울 피보호자 안부를 담는 리스트
    private var wardSafetyList = arrayListOf<Pair<String, SafetyDTO>>()
    // RecyclerView 세팅을 돕는 adapter 객체
    private lateinit var wardSafetyAdapter : WardSafetyAdapter
    private lateinit var wardId : String
    private lateinit var wardName : String
    // 피보호자의 프로필 사진 경로
    private lateinit var photoUri : String
    private lateinit var listenerDTO : ListenerDTO

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ward_safety_setting)

        /* 데이터 세팅 */
        setData()

        /* 리사이클러 뷰 세팅 */
        setRecyclerView()

        /* 피보호자 안부 목록 리스너 설정 */
        setWardSafetyListener()

        /* 종료 버튼 세팅 */
        setExitButton()

        /* 추가 버튼 세팅 */
        setAddButton()
    }

    /**
     * "피보호자 안부 설정" 화면의 초기 세팅을 해주는 메서드
     */
    private fun setData() {
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
    }

    /**
     * "종료" 버튼을 세팅해주는 메서드
     *  - "MainFragment"에서 선택한 피보호자의 Dialog가 켜진 화면으로 돌아감
     */
    private fun setExitButton() {
        findViewById<ImageView>(R.id.ward_safety_setting_exit_button).setOnClickListener {
            finish()
        }
    }

    /**
     * "피보호자 안부 추가" 버튼을 세팅해주는 메서드
     *  - "AddWardSafetyActivity"로 이동
     *  - wardName, wardId를 함게 전달
     */
    private fun setAddButton() {
        findViewById<ImageView>(R.id.ward_safety_setting_add_button).setOnClickListener {
            val tmpIntent = Intent(this, AddWardSafetyActivity::class.java)
            tmpIntent.putExtra("wardName", wardName)
            tmpIntent.putExtra("wardId", wardId)
            startActivity(tmpIntent)
        }
    }

    /* 피보호자 안부 목록 리스너 설정 */
    /**
     * RecyclerView에 띄워질 wardSafetyList를 데이터베이스에 따라 실시간으로 세팅하는 메서드
     *  - 초기화 / 추가 / 수정 / 삭제 시 적용
     */
    private fun setWardSafetyListener() {
        // wardSafetyList 세팅 및 업데이트 하기
        // 현재 선택한 피보호자의 안부 목록
        val wardSafetyRef = database.getReference("ward").child(wardId).child("safetyIdList")

        // wardSafetyList에 선택한 피보호자의 안부들 넣기
        val safetyListener = wardSafetyRef.addChildEventListener(object : ChildEventListener{
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                // 선택한 피보호자의 안부 하나씩 참조
                val safetyId = snapshot.key.toString()
                val safetyToAdd = database.getReference("safety").child(safetyId)

                // 안부 컬렉션에서 각 안부 불러와서 WardSafetyList에 넣기
                safetyToAdd.get().addOnSuccessListener {
                    wardSafetyList.add(Pair(safetyId, it.getValue(SafetyDTO::class.java) as SafetyDTO))
                    // 내림차순으로 정렬
                    wardSafetyList.sortedByDescending { it.second.date }
                    // 리사이클러 뷰 어답터에 알려주기
                    wardSafetyAdapter.notifyDataSetChanged()
                }
            }
            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
                // safetyId 찾기
                val safetyId = snapshot.key.toString()
                val safetyInDB = database.getReference("safety").child(safetyId)

                // wardSafetyList에서 safetyId에 해당하는 안부 찾아 수정해주기
                var idx = -1
                for (s in wardSafetyList){
                    idx++
                    if (s.first == safetyId){
                        safetyInDB.get().addOnSuccessListener {
                            wardSafetyList.remove(s)
                            wardSafetyList.add(idx, Pair(safetyId, it.getValue(SafetyDTO::class.java) as SafetyDTO))

                            // 리스트가 수정되었다고 어댑터에게 알려주기
                            wardSafetyAdapter.notifyDataSetChanged()
                        }
                        break
                    }
                }
            }
            override fun onChildRemoved(snapshot: DataSnapshot) {
                // safetyId 찾기
                val safetyId = snapshot.key.toString()

                // wardSafetyList에서 safetyId에 해당하는 안부 찾아 삭제하기
                for (s in wardSafetyList){
                    if (s.first == safetyId) {
                        wardSafetyList.remove(s)
                        // 리스트가 수정되었다고 어댑터에게 알려주기
                        wardSafetyAdapter.notifyDataSetChanged()
                        break
                    }
                }
            }
            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {
            }
            override fun onCancelled(error: DatabaseError) {
            }
        })
        listenerDTO = ListenerDTO(wardSafetyRef, safetyListener)
    }

    /**
     * RecyclerView를 세팅하기 위해 adapter클래스에 연결하는 메서드
     */
    private fun setRecyclerView() {
        val rv = findViewById<RecyclerView>(R.id.ward_safety_setting_rv)
        wardSafetyAdapter = WardSafetyAdapter(this, wardSafetyList, wardName)
        rv.adapter = wardSafetyAdapter
        rv.layoutManager = LinearLayoutManager(this)
        rv.setHasFixedSize(true)
    }

    /**
     * 화면 종료시 사용하고 있던 리스너들을 반환하는 메서드
     */
    override fun onDestroy() {
        super.onDestroy()

        val reference : DatabaseReference = listenerDTO.reference
        val listener : ChildEventListener = listenerDTO.listener

        reference.removeEventListener(listener)
    }
}