package com.seoul42.relief_post_office.safety

import android.app.Activity
import android.app.TimePickerDialog
import android.content.Intent
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.*
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.seoul42.relief_post_office.R
import com.seoul42.relief_post_office.adapter.AddWardSafetyAdapter
import com.seoul42.relief_post_office.model.NotificationDTO
import com.seoul42.relief_post_office.model.QuestionDTO
import com.seoul42.relief_post_office.model.SafetyDTO
import com.seoul42.relief_post_office.model.UserDTO
import com.seoul42.relief_post_office.viewmodel.FirebaseViewModel
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

class AddWardSafetyActivity : AppCompatActivity() {

    private val database = Firebase.database
    private val firebaseViewModel : FirebaseViewModel by viewModels()
    private var questionList = arrayListOf<Pair<String, QuestionDTO>>()
    private lateinit var addWardSafetyAdapter : AddWardSafetyAdapter
    private lateinit var wardId : String
    private lateinit var wardName : String

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_ward_safety)

        // 선택한 피보호자 id, 이름 찾아오기
        wardId = intent.getStringExtra("wardId").toString()
        wardName = intent.getStringExtra("wardName").toString()

        // 뒤로 가기 버튼 이벤트
        findViewById<ImageView>(R.id.add_ward_safety_backBtn).setOnClickListener{
            finish()
        }

        // 시간 설정 이벤트 / timepicker 다이얼로그
        val timeText = findViewById<TextView>(R.id.add_ward_safety_time)
        var time : String? = null
        timeText.setOnClickListener{
            var calender = Calendar.getInstance()
            var hour = calender.get(Calendar.HOUR)
            var minute = calender.get(Calendar.MINUTE)

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

        // 리사이클러 뷰 세팅
        val rv = findViewById<RecyclerView>(R.id.add_ward_safety_rv)
        addWardSafetyAdapter = AddWardSafetyAdapter(questionList)
        rv.adapter = addWardSafetyAdapter
        rv.layoutManager = LinearLayoutManager(this)
        rv.setHasFixedSize(true)

        // 질문 설정 이벤트
        findViewById<ImageView>(R.id.add_ward_safety_question_setting).setOnClickListener{
            val tmpIntent = Intent(this, SafetyQuestionSettingActivity::class.java)
            tmpIntent.putExtra("questionList", questionList.toMap().keys.toCollection(ArrayList<String>()))
            startActivityForResult(tmpIntent, 1)
        }

        // 저장 버튼 이벤트
        findViewById<Button>(R.id.add_ward_safety_add_button).setOnClickListener {

            // 안부 이름 세팅
            var name = findViewById<EditText>(R.id.add_ward_safety_name).text.toString()

            // 안부 요일 세팅
            val dayOfWeeks = mutableMapOf<String, Boolean>(
                Pair("월", findViewById<CheckBox>(R.id.add_ward_safety_monday).isChecked),
                Pair("화", findViewById<CheckBox>(R.id.add_ward_safety_tuesday).isChecked),
                Pair("수", findViewById<CheckBox>(R.id.add_ward_safety_wednesday).isChecked),
                Pair("목", findViewById<CheckBox>(R.id.add_ward_safety_thursday).isChecked),
                Pair("금", findViewById<CheckBox>(R.id.add_ward_safety_friday).isChecked),
                Pair("토", findViewById<CheckBox>(R.id.add_ward_safety_saturday).isChecked),
                Pair("일", findViewById<CheckBox>(R.id.add_ward_safety_sunday).isChecked)
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

                // safety 컬렉션에 작성한 내용 추가
                val safetyRef = database.getReference("safety")
                val newPush = safetyRef.push()
                val key = newPush.key.toString()
                newPush.setValue(newSafety)

                // 설정한 질문들의 connedSafetyList에 현재 안부 아이디 추가
                for (q in questionList){
                    val qRef = database.getReference("question").child(q.first)
                        .child("connectedSafetyList")
                        .child(key)
                    qRef.setValue(date)

                    // 해당하는 질문들 보호자 질문 목록에서 최종 수정일 변경하기
                    database.getReference("guardian").child(Firebase.auth.currentUser!!.uid)
                        .child("questionList").child(q.first).setValue(date)
                }

                // 선택한 피보호자의 안부 목록에 방금 등록한 안부 아이디 추가
                val wardSafetyRef = database.getReference("ward").child(wardId).child("safetyIdList")
                wardSafetyRef.child(key).setValue(date)

                /* 피보호자에게 안부 동기화 FCM 보내기 */
                val wardRef = Firebase.database.reference.child("user").child(wardId)
                wardRef.get().addOnSuccessListener {
                    // 피보호자에게 보내기
                    val wardDTO = it.getValue(UserDTO::class.java) as UserDTO
                    val notificationData = NotificationDTO.NotificationData("SafetyWard"
                        , "안심우체국", "누군가 새로운 안부를 추가하였습니다")
                    val notificationDTO = NotificationDTO(wardDTO.token!!, notificationData)
                    firebaseViewModel.sendNotification(notificationDTO) /* FCM 전송하기 */
                }

                /* 피보호자와 연결된 보호자들에게 안부 동기화 FCM 보내기 */
                val guardianListRef = database.getReference("ward").child(wardId).child("connectList")
                guardianListRef.get().addOnSuccessListener {
                    val guardianList = (it.getValue() as HashMap<String, String>).values.toList()
                    val UserRef = database.getReference("user")
                    for (guardianId in guardianList){
                        UserRef.child(guardianId).child("token").get().addOnSuccessListener {
                            val notificationData = NotificationDTO.NotificationData("SafetyGuardian",
                            "안심우체국", "${wardName}님에게 새로운 안부가 추가되었습니다")
                            val notificationDTO = NotificationDTO(it.getValue().toString()!!, notificationData)
                            firebaseViewModel.sendNotification(notificationDTO) /* FCM 전송하기 */
                        }
                    }
                }

                // 액티비티 종료
                Toast.makeText(this, "안부 추가 완료", Toast.LENGTH_SHORT).show()
                finish()
            }
        }

    }

    // 질문 설정 후 받은 체크된 질문들 리스트에 추가
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == Activity.RESULT_OK){
            when (requestCode){
                1 -> {
                    questionList.clear()
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