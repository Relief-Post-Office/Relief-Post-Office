package com.seoul42.relief_post_office.safety

import android.app.Activity
import android.app.TimePickerDialog
import android.content.Intent
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.*
import androidx.annotation.RequiresApi
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.seoul42.relief_post_office.R
import com.seoul42.relief_post_office.adapter.AddWardSafetyAdapter
import com.seoul42.relief_post_office.model.QuestionDTO
import com.seoul42.relief_post_office.model.SafetyDTO
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*

class EditWardSafetyActivity : AppCompatActivity() {

    private val database = Firebase.database
    private val QuestionRef = database.getReference("question")
    private var time : String? = null
    private var questionList = arrayListOf<Pair<String, QuestionDTO>>()
    private lateinit var owner : String
    private lateinit var wardId : String
    private lateinit var safetyId : String
    private lateinit var safety : SafetyDTO
    private lateinit var addWardSafetyAdapter : AddWardSafetyAdapter


    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_ward_safety)

        // 수정할 안부 데이터 가져오고 첫 세팅 하기
        safetyId = intent.getStringExtra("safetyId").toString()
        val safetyRef = database.getReference("safety").child(safetyId)
        owner = Firebase.auth.currentUser?.uid.toString()
        safetyRef.get().addOnSuccessListener {
            safety = it.getValue(SafetyDTO::class.java)!!
            wardId = safety.uid.toString()

            // 수정할 안부 데이터 세팅
            // 이름 세팅
            findViewById<EditText>(R.id.edit_ward_safety_name).setText(safety.name)
            // 요일 세팅
            var dayOfWeek = safety.dayOfWeek
            findViewById<CheckBox>(R.id.edit_ward_safety_monday).isChecked = dayOfWeek["월"]!!
            findViewById<CheckBox>(R.id.edit_ward_safety_tuesday).isChecked = dayOfWeek["화"]!!
            findViewById<CheckBox>(R.id.edit_ward_safety_wednesday).isChecked = dayOfWeek["수"]!!
            findViewById<CheckBox>(R.id.edit_ward_safety_thursday).isChecked = dayOfWeek["목"]!!
            findViewById<CheckBox>(R.id.edit_ward_safety_friday).isChecked = dayOfWeek["금"]!!
            findViewById<CheckBox>(R.id.edit_ward_safety_saturday).isChecked = dayOfWeek["토"]!!
            findViewById<CheckBox>(R.id.edit_ward_safety_sunday).isChecked = dayOfWeek["일"]!!
            // 시간 세팅
            findViewById<TextView>(R.id.edit_ward_safety_time).text =
                safety.time.toString().substring(0..1) + "시 " +
                        safety.time.toString().substring(3..4).toString() + "분"
            // questionList 초기 세팅
            for (q in safety.questionList.keys.toList()){
                QuestionRef.child(q).get().addOnSuccessListener {
                    questionList.add(Pair(q, it.getValue(QuestionDTO::class.java)) as Pair<String, QuestionDTO>)
                    addWardSafetyAdapter.notifyDataSetChanged()
                }
            }

            // 시간 설정 이벤트 / timepicker 다이얼로그
            val timeText = findViewById<TextView>(R.id.edit_ward_safety_time)
            time = safety.time
            timeText.setOnClickListener{
                var hour = time!!.substring(0..1).toInt()
                var minute = time!!.substring(3..4).toInt()

                var listener = TimePickerDialog.OnTimeSetListener{
                        _, i, i2 ->
                    var h = ""
                    var m = ""
                    if (i < 10){
                        h = "0" + i.toString()
                    }
                    else{
                        h = i.toString()
                    }
                    if (i2 < 10){
                        m = "0" + i2.toString()
                    }
                    else{
                        m = i2.toString()
                    }
                    timeText.text = "${h}시 ${m}분"

                    time = "${h}:${m}"
                }
                var picker = TimePickerDialog(this, android.R.style.Theme_Holo_Light_Dialog_NoActionBar ,listener, hour, minute, false)
                picker.setTitle("알람 시간")
                picker.window?.setBackgroundDrawableResource(android.R.color.transparent)
                picker.show()
            }
        }

        // 리사이클러 뷰 설정
        val rv = findViewById<RecyclerView>(R.id.edit_ward_safety_rv)
        addWardSafetyAdapter = AddWardSafetyAdapter(questionList)
        rv.adapter = addWardSafetyAdapter
        rv.layoutManager = LinearLayoutManager(this)
        rv.setHasFixedSize(true)

        // 질문 설정 이벤트
        findViewById<ImageView>(R.id.edit_ward_safety_question_setting).setOnClickListener{
            val tmpIntent = Intent(this, SafetyQuestionSettingActivity::class.java)
            tmpIntent.putExtra("questionList", questionList.toMap().keys.toCollection(ArrayList<String>()))
            questionList.clear()
            startActivityForResult(tmpIntent, 1)
        }

        // 뒤로 가기 버튼 이벤트
        findViewById<ImageView>(R.id.edit_ward_safety_backBtn).setOnClickListener{
            finish()
        }

        // 삭제 버튼 클릭 이벤트
        findViewById<Button>(R.id.edit_ward_safety_delete_button).setOnClickListener{
            // 질문 목록에 내 소유 질문들만 있는지 확인
            var canDelete = true
            for (q in questionList){
                // 내 소유가 아닌 질문을 찾으면
                if (q.second.owner.toString() != owner){
                    Toast.makeText(this, "안부에 다른 보호자의 질문이 있습니다", Toast.LENGTH_SHORT).show()
                    canDelete = false
                    break
                }
            }

            // 질문 목록에 내 소유 질문들만 있을 경우에 삭제
            if (canDelete) {
                // 해당 안부 id를 통해 데이터베이스에서 삭제
                database.getReference("safety").child(safetyId).setValue(null)
                // 피보호자의 안부 목록에서 해당 안부 삭제
                database.getReference("ward")
                    .child(wardId)
                    .child("safetyIdList")
                    .child(safetyId).setValue(null)

                // 액티비티 종료
                Toast.makeText(this, "안부 삭제 완료", Toast.LENGTH_SHORT).show()
                finish()
            }
        }


        // 저장 버튼 클릭 이벤트
        findViewById<Button>(R.id.edit_ward_safety_save_button).setOnClickListener {

            // 안부 이름 세팅
            var name = findViewById<EditText>(R.id.edit_ward_safety_name).text.toString()

            // 안부 요일 세팅
            val dayOfWeeks = mutableMapOf<String, Boolean>(
                Pair("월", findViewById<CheckBox>(R.id.edit_ward_safety_monday).isChecked),
                Pair("화", findViewById<CheckBox>(R.id.edit_ward_safety_tuesday).isChecked),
                Pair("수", findViewById<CheckBox>(R.id.edit_ward_safety_wednesday).isChecked),
                Pair("목", findViewById<CheckBox>(R.id.edit_ward_safety_thursday).isChecked),
                Pair("금", findViewById<CheckBox>(R.id.edit_ward_safety_friday).isChecked),
                Pair("토", findViewById<CheckBox>(R.id.edit_ward_safety_saturday).isChecked),
                Pair("일", findViewById<CheckBox>(R.id.edit_ward_safety_sunday).isChecked)
            )

            // 생성 날짜 세팅
            val date = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))

            // 시간을 설정한 경우에만 추가 가능
            if (time == null){
                Toast.makeText(this, "시간을 설정해 주세요", Toast.LENGTH_SHORT).show()
            }
            else if (questionList.isEmpty()){
                Toast.makeText(this, "질문을 설정해 주세요", Toast.LENGTH_SHORT).show()
            }
            else{
                // 이름을 설정하지 않은 경우에 "무제"로 통일
                if (name == "")
                    name = "무제"

                // safety 컬렉션에 추가할 safetyDTO 생성
                val newSafety = SafetyDTO(wardId, name, date, time,
                    questionList.map {it.first to it.second.date}.toMap() as MutableMap<String, String>, dayOfWeeks)

                // safety 컬렉션에 작성한 내용 수정
                val safetyRef = database.getReference("safety").child(safetyId)
                safetyRef.setValue(newSafety)

                // 선택한 피보호자의 안부 목록에 방금 수정한 안부 아이디 최종 변경 시간 변경
                val wardSafetyRef = database.getReference("ward").child(wardId).child("safetyIdList")
                wardSafetyRef.child(safetyId).setValue(date)

                // 액티비티 종료
                Toast.makeText(this, "안부 수정 완료", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == Activity.RESULT_OK){
            when (requestCode){
                1 -> {
                    addWardSafetyAdapter.notifyDataSetChanged()
                    val checkQuestions = data?.getStringArrayListExtra("returnQuestionList")
                    val QuestionRef = database.getReference("question")
                    for (q in checkQuestions!!){
                        QuestionRef.child(q).get().addOnSuccessListener {
                            questionList.add(Pair(q, it.getValue(QuestionDTO::class.java)) as Pair<String, QuestionDTO>)
                            addWardSafetyAdapter.notifyDataSetChanged()
                        }
                    }
                }
            }
        }
    }

}