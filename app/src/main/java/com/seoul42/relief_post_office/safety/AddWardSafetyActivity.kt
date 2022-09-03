package com.seoul42.relief_post_office.safety

import android.app.Activity
import android.app.TimePickerDialog
import android.content.Intent
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.view.View
import android.view.WindowManager
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

/**
 * 피보호자의 안부를 추가하는 화면을 띄우도록 돕는 클래스
 */
class AddWardSafetyActivity : AppCompatActivity() {

    // FCM을 보내기 위한 객체
    private val database = Firebase.database
    private val firebaseViewModel : FirebaseViewModel by viewModels()
    // 안부가 설정된 시간을 담는 변수
    private var time : String? = null
    // 선택한 안부에 설정된 질문들을 담는 리스트
    private var questionList = arrayListOf<Pair<String, QuestionDTO>>()
    // RecyclerView 세팅을 돕는 adapter 객체
    private lateinit var addWardSafetyAdapter : AddWardSafetyAdapter
    // 피보호자의 ID
    private lateinit var wardId : String
    // 피보호자의 이름
    private lateinit var wardName : String

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_ward_safety)

        /* 초기 세팅 */
        setData()

        /* 리사이클러 뷰 세팅 */
        setRecyclerView()

        /* 보호자 안부 가져오기 버튼 세팅 */
        setGetSafetyButton()

        /* 질문 설정 버튼 세팅 */
        setEditSafetyQuestionButton()

        /* 저장 버튼 세팅 */
        setSaveButton()

        // 뒤로 가기 버튼 세팅
        setBackButton()
    }

    /**
     * "안부 추가" 화면의 초기 세팅을 하는 메서드
     */
    private fun setData() {
        // 선택한 피보호자 id, 이름 찾아오기
        wardId = intent.getStringExtra("wardId").toString()
        wardName = intent.getStringExtra("wardName").toString()

        // 시간 설정 이벤트 / timepicker 다이얼로그
        val timeText = findViewById<TextView>(R.id.add_ward_safety_time)
        timeText.setOnClickListener{
            var calender = Calendar.getInstance()
            var hour = calender.get(Calendar.HOUR_OF_DAY)
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
    }

    /**
     *  안부 저장(추가) 버튼을 세팅해주는 메서드
     *   - 입력한 데이터와 설정값을 데이터베이스에 업로드
     *   - 안부 저장 최소 조건
     *    1. 시간 설정
     *    2. 질문 1개 이상 설정
     */
    @RequiresApi(Build.VERSION_CODES.O)
    private fun setSaveButton() {
        // 저장 버튼 이벤트
        findViewById<Button>(R.id.add_ward_safety_add_button).setOnClickListener {
            // 프로그레스바 처리
            val saveBtnView = it
            saveBtnView.isClickable = false
            window!!.addFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
            val progressBar = findViewById<ProgressBar>(R.id.add_ward_safety_progress)
            progressBar.visibility = View.VISIBLE

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
                saveBtnView.isClickable = true
                progressBar.visibility = View.INVISIBLE
                window.clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
                Toast.makeText(this, "시간을 설정해 주세요", Toast.LENGTH_SHORT).show()
            }
            // 질문을 설정한 경우에만 추가 가능
            else if (questionList.isEmpty()){
                saveBtnView.isClickable = true
                progressBar.visibility = View.INVISIBLE
                window.clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
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
                connectedSafetyListSync(date, key)

                // 선택한 피보호자의 안부 목록에 방금 등록한 안부 아이디 추가
                val wardSafetyRef = database.getReference("ward").child(wardId).child("safetyIdList")
                wardSafetyRef.child(key).setValue(date)

                // 피보호자에게 동기화 FCM 보내기
                wardSafetySync()
            }
        }
    }

    /**
     * "뒤로가기" 버튼을 세팅해주는 메서드
     *  - "WardSafetySettingActivity"로 돌아감
     */
    private fun setBackButton() {
        findViewById<ImageView>(R.id.add_ward_safety_backBtn).setOnClickListener{
            finish()
        }
    }

    /**
     * RecyclerView를 세팅하기 위해 adapter클래스에 연결하는 메서드
     */
    private fun setRecyclerView() {
        val rv = findViewById<RecyclerView>(R.id.add_ward_safety_rv)
        addWardSafetyAdapter = AddWardSafetyAdapter(questionList)
        rv.adapter = addWardSafetyAdapter
        rv.layoutManager = LinearLayoutManager(this)
        rv.setHasFixedSize(true)
    }

    /**
     * "질문 설정" 버튼을 세팅해주는 메서드
     *  - "SafetyQuestionSettingActivity"로 이동
     *  - questionList를 함께 전달
     */
    private fun setEditSafetyQuestionButton() {
        findViewById<ImageView>(R.id.add_ward_safety_question_setting).setOnClickListener{
            val tmpIntent = Intent(this, SafetyQuestionSettingActivity::class.java)
            tmpIntent.putExtra("questionList", questionList.toMap().keys.toCollection(ArrayList<String>()))
            startActivityForResult(tmpIntent, 1)
        }
    }

    /**
     * 데이터베이스에서 안부에 연결 및 연결 해제된 질문들 connectedSafetyList 동기화하는 메서드
     */
    private fun connectedSafetyListSync(date : String, key : String) {
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
    }

    /**
     * 피보호자에게 안부 동기화 FCM 보내는 메서드
     *  - 안부 추가 후 동작
     */
    private fun wardSafetySync() {
        val wardRef = Firebase.database.reference.child("user").child(wardId)
        wardRef.get().addOnSuccessListener {
            // 피보호자에게 보내기
            val wardDTO = it.getValue(UserDTO::class.java) as UserDTO
            val notificationData = NotificationDTO.NotificationData("SafetyWard",
                "안심우체국", "누군가 새로운 안부를 추가하였습니다")
            val notificationDTO = NotificationDTO(wardDTO.token, "high", notificationData)
            firebaseViewModel.sendNotification(notificationDTO) /* FCM 전송하기 */

            // 피보호자와 연결된 보호자에게 동기화 FCM 보내기
            guardianSafetySync()
        }
    }

    /**
     * 피보호자와 연결된 보호자들에게 안부 동기화 FCM 보내는 메서드
     *  - wardSafetySync()에서 동작
     */
    private fun guardianSafetySync() {
        val guardianListRef = database.getReference("ward").child(wardId).child("connectList")
        guardianListRef.get().addOnSuccessListener {
            val guardianList = (it.getValue() as java.util.HashMap<String, String>).values.toList()
            val UserRef = database.getReference("user")
            for (guardianId in guardianList){
                UserRef.child(guardianId).child("token").get().addOnSuccessListener {
                    val notificationData = NotificationDTO.NotificationData("SafetyGuardian",
                        "안심우체국", "${wardName}님에게 새로운 안부가 추가되었습니다")
                    val notificationDTO = NotificationDTO(it.getValue().toString(), "high", notificationData)
                    firebaseViewModel.sendNotification(notificationDTO) /* FCM 전송하기 */
                }
            }
        }
        Handler().postDelayed({
            // 액티비티 종료
            Toast.makeText(this, "안부 추가 완료", Toast.LENGTH_SHORT).show()
            finish()
        }, 1500)
    }

    /**
     * "안부 가져오기" 버튼 세팅해주는 메서드
     *  - "GetGuardianSafetyActivity"로 이동
     */
    private fun setGetSafetyButton() {
        findViewById<Button>(R.id.add_ward_safety_get_button).setOnClickListener{
            val tmpIntent = Intent(this, GetGuardianSafetyActivity::class.java)
            startActivityForResult(tmpIntent, 2)
        }
    }

    /* 질문 설정, 안부 가져오기 작업 결과 가져오기 */
    /**
     * "질문 설정", "안부 가져오기" 페이지에서 작업한 결과 가져오는 메서드
     *  - "질문 설정"
     *      - 수정된 질문 할당 여부들을 가져와서 동기화
     *  - "안부 가져오기"
     *      - 선택한 보호자 안부(질문 모음)에 포함된 질문들을 questionList에 추가
     */
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == Activity.RESULT_OK){
            when (requestCode){
                1 -> {  // 질문 설정이 변경되었을 경우
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
                2 -> {// 보호자 안부가 적용되었을 경우
                    val QuestionsFromSafety = data?.getStringArrayListExtra("questionsFromSafety")
                    val QuestionRef = database.getReference("question")
                    val qIdList = questionList.toMap().keys
                    for (q in QuestionsFromSafety!!){
                        if (!qIdList.contains(q)){
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
}