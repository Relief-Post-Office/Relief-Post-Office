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

class WardSafetySettingActivity : AppCompatActivity() {

    private val database = Firebase.database
    private var wardSafetyList = arrayListOf<Pair<String, SafetyDTO>>()
    private lateinit var wardSafetyAdapter : WardSafetyAdapter
    private lateinit var wardId : String
    private lateinit var wardName : String
    private lateinit var photoUri : String
    private lateinit var listenerDTO : ListenerDTO

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
            val tmpIntent = Intent(this, AddWardSafetyActivity::class.java)
            tmpIntent.putExtra("wardName", wardName)
            tmpIntent.putExtra("wardId", wardId)
            startActivity(tmpIntent)
        }

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

        // 리사이클러 뷰 설정
        // 리사이클러 뷰 가져오기
        val rv = findViewById<RecyclerView>(R.id.ward_safety_setting_rv)
        // 리사이클러 뷰 아답터에 리스트 넘긴 후 아답터 가져오기
        wardSafetyAdapter = WardSafetyAdapter(this, wardSafetyList, wardName)
        // 리사이클러 뷰에 아답터 연결하기
        rv.adapter = wardSafetyAdapter
        rv.layoutManager = LinearLayoutManager(this)
        rv.setHasFixedSize(true)
    }

    override fun onDestroy() {
        super.onDestroy()

        val reference : DatabaseReference = listenerDTO.reference
        val listener : ChildEventListener = listenerDTO.listener

        reference.removeEventListener(listener)
    }
}