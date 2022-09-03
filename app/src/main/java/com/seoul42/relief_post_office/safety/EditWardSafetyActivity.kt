package com.seoul42.relief_post_office.safety

import android.app.Activity
import android.app.TimePickerDialog
import android.content.Intent
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.widget.*
import androidx.activity.viewModels
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
import com.seoul42.relief_post_office.model.NotificationDTO
import com.seoul42.relief_post_office.model.QuestionDTO
import com.seoul42.relief_post_office.model.SafetyDTO
import com.seoul42.relief_post_office.model.UserDTO
import com.seoul42.relief_post_office.viewmodel.FirebaseViewModel
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*

/**
 * 피보호자의 안부를 수정하는 화면을 띄우도록 돕는 클래스
 */
class EditWardSafetyActivity : AppCompatActivity() {

    private val database = Firebase.database
    private val QuestionRef = database.getReference("question")
    // 안부가 설정된 시간을 담는 변수
    private var time : String? = null
    // 안부에 설정된 질문들을 담는 리스트
    private var questionList = arrayListOf<Pair<String, QuestionDTO>>()
    // 안부에 포함되었다가 삭제된 질문들을 담는 리스트
    private var deletedQuestionList = arrayListOf<String>()
    // FCM을 보내기 위한 객체
    private val firebaseViewModel : FirebaseViewModel by viewModels()
    // 로그인한 보호자 id
    private lateinit var owner : String
    private lateinit var wardId : String
    private lateinit var wardName : String
    private lateinit var safetyId : String
    private lateinit var safety : SafetyDTO
    // RecyclerView 세팅을 돕는 adapter 객체
    private lateinit var editWardSafetyAdapter : AddWardSafetyAdapter

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_ward_safety)

        // 안부 데이터 가져오기 및 초기 세팅
        setData()

        // 리사이클러 뷰 세팅
        setRecyclerView()

        // 보호자 안부 가져오기 버튼 이벤트 세팅
        setGetSafetyButton()

        // 질문 설정 버튼 세팅
        setEditSafetyQuestionButton()

        // 저장 버튼 세팅
        setSaveButton()

        // 삭제 버튼 세팅
        setDeleteButton()

        // 뒤로 가기 버튼 세팅
        setBackButton()
    }

    /**
     * 데이터베이스에서 정보를 불러와 "피보호자 안부 수정" 화면의 초기 세팅을 해주는 메서드
     */
    private fun setData() {
        safetyId = intent.getStringExtra("safetyId").toString()
        wardName = intent.getStringExtra("wardName").toString()
        owner = Firebase.auth.currentUser?.uid.toString()

        val safetyRef = database.getReference("safety").child(safetyId)
        safetyRef.get().addOnSuccessListener {
            safety = it.getValue(SafetyDTO::class.java)!!
            wardId = safety.uid.toString()

            /* 수정할 안부 데이터 세팅 */
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
                    editWardSafetyAdapter.notifyDataSetChanged()
                }
            }

            // 시간 설정 버튼 (timepicker 다이얼로그)
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
    }

    /**
     * RecyclerView를 세팅하기 위해 adapter클래스에 연결하는 메서드
     */
    private fun setRecyclerView() {
        val rv = findViewById<RecyclerView>(R.id.edit_ward_safety_rv)
        editWardSafetyAdapter = AddWardSafetyAdapter(questionList)
        rv.adapter = editWardSafetyAdapter
        rv.layoutManager = LinearLayoutManager(this)
        rv.setHasFixedSize(true)
    }

    /**
     * "질문 설정" 버튼을 세팅해주는 메서드
     *  - "SafetyQuestionSettingActivity"으로 이동
     *  - questionList를 함께 전달
     */
    private fun setEditSafetyQuestionButton() {
        findViewById<ImageView>(R.id.edit_ward_safety_question_setting).setOnClickListener{
            val tmpIntent = Intent(this, SafetyQuestionSettingActivity::class.java)
            tmpIntent.putExtra("questionList", questionList.toMap().keys.toCollection(ArrayList<String>()))
            startActivityForResult(tmpIntent, 1)
        }
    }

    /**
     * "뒤로가기" 버튼 세팅해주는 메서드
     *  - "WardSafetySettingActivity"로 돌아감
     */
    private fun setBackButton() {
        findViewById<ImageView>(R.id.edit_ward_safety_backBtn).setOnClickListener{
            finish()
        }
    }

    /* 삭제 버튼 세팅 */
    /**
     * "삭제" 버튼 세팅해주는 메서드
     *  - 삭제 조건
     *   1. 안부에 할당된 질문들이 모두 로그인한 보호자의 소유
     *  - 삭제 과정
     *   1. 안부 삭제
     *   2. 피보호자 안부 목록에서 삭제
     *   3. 안부에 연결되어 있던 질문들 connectedSafetyList에서 삭제
     *   4. 피보호자에게 안부 삭제 동기화 FCM 전송
     */
    @RequiresApi(Build.VERSION_CODES.O)
    private fun setDeleteButton() {
        findViewById<Button>(R.id.edit_ward_safety_delete_button).setOnClickListener{
            // 질문 목록에 내 소유 질문들만 있을 경우에 삭제
            if (canDelete()) {
                // 프로그레스바 처리
                it.isClickable = false
                window!!.addFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
                val progressBar = findViewById<ProgressBar>(R.id.edit_ward_safety_progress)
                progressBar.visibility = View.VISIBLE

                // 해당 안부 id를 통해 데이터베이스에서 삭제
                database.getReference("safety").child(safetyId).setValue(null)

                // 피보호자의 안부 목록에서 해당 안부 삭제
                database.getReference("ward")
                    .child(wardId)
                    .child("safetyIdList")
                    .child(safetyId).setValue(null)

                /* 안부에 연결된 질문들 connectedSafetyList 동기화 */
                val date = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
                connectedSafetyListSync(date, 2)

                // 피보호자에게 동기화 FCM 보내기
                wardSafetySync("삭제 되었습니다")
            }
        }
    }

    /* 저장 버튼 세팅 */
    /**
     * "저장" 버튼 세팅해주는 메서드
     *  - 저장 조건
     *   1. 시간 설정
     *   2. 안부에 할당된 질문이 1개 이상
     *  - 변경 후 "피보호자" 및 "피보호자와 연결된 보호자"들에게 동기화 FCM 전송
     */
    @RequiresApi(Build.VERSION_CODES.O)
    private fun setSaveButton() {
        findViewById<Button>(R.id.edit_ward_safety_save_button).setOnClickListener {
            // 프로그레스바 처리
            val saveBtnView = it
            saveBtnView.isClickable = false
            window!!.addFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
            val progressBar = findViewById<ProgressBar>(R.id.edit_ward_safety_progress)
            progressBar.visibility = View.VISIBLE

            // 안부 이름 가져오기
            var name = findViewById<EditText>(R.id.edit_ward_safety_name).text.toString()

            // 안부 요일 가져오기
            val dayOfWeeks = mutableMapOf<String, Boolean>(
                Pair("월", findViewById<CheckBox>(R.id.edit_ward_safety_monday).isChecked),
                Pair("화", findViewById<CheckBox>(R.id.edit_ward_safety_tuesday).isChecked),
                Pair("수", findViewById<CheckBox>(R.id.edit_ward_safety_wednesday).isChecked),
                Pair("목", findViewById<CheckBox>(R.id.edit_ward_safety_thursday).isChecked),
                Pair("금", findViewById<CheckBox>(R.id.edit_ward_safety_friday).isChecked),
                Pair("토", findViewById<CheckBox>(R.id.edit_ward_safety_saturday).isChecked),
                Pair("일", findViewById<CheckBox>(R.id.edit_ward_safety_sunday).isChecked)
            )

            // 생성 날짜 가져오기
            val date = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))

            // 시간과 질문을 설정한 경우에만 추가 가능
            if (time == null){
                saveBtnView.isClickable = true
                progressBar.visibility = View.INVISIBLE
                window.clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
                Toast.makeText(this, "시간을 설정해 주세요", Toast.LENGTH_SHORT).show()
            }
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

                // safety 컬렉션에 수정된 safetyDTO 적용
                val safetyRef = database.getReference("safety").child(safetyId)
                safetyRef.setValue(newSafety)

                /* 안부에 연결 및 연결 해제된 질문들 connectedSafetyList 동기화 */
                connectedSafetyListSync(date, 1)

                // 선택한 피보호자의 안부 목록에 방금 수정한 안부 아이디 최종 변경 시간 변경
                val wardSafetyRef = database.getReference("ward").child(wardId).child("safetyIdList")
                wardSafetyRef.child(safetyId).setValue(date)

                // 피보호자에게 동기화 FCM 보내기
                wardSafetySync("변경 되었습니다")
            }
        }
    }

    /**
     * 안부에 연결 및 연결 해제된 질문들을 connectedSafetyList에 동기화 해주는 함수
     *  - flag -> 1 : 안부 수정 시
     *  - flag -> 2 : 안부 삭제 시
     */
    private fun connectedSafetyListSync(date : String, flag : Int) {
        when(flag){
            1-> { for (q in questionList){  // 최종 저장되는 질문들 connectedSafetyList 동기화
                    val qRef = database.getReference("question").child(q.first)
                        .child("connectedSafetyList")
                        .child(safetyId)
                    qRef.setValue(date)

                    if (q.second.owner == owner) {
                        // 해당하는 질문들 보호자 질문 목록에서 최종 수정일 변경하기
                        database.getReference("guardian").child(Firebase.auth.currentUser!!.uid)
                            .child("questionList").child(q.first).setValue(date)
                    }
                }

                // 기존에 설정되었다가 이번 수정에서 빠진 질문들 connectedSafetyList 동기화
                for (q in deletedQuestionList){
                    val qRef = database.getReference("question").child(q)
                        .child("connectedSafetyList")
                        .child(safetyId)
                    qRef.setValue(null)

                    // 해당하는 질문들 보호자 질문 목록에서 최종 수정일 변경하기
                    database.getReference("guardian").child(Firebase.auth.currentUser!!.uid)
                        .child("questionList").child(q).setValue(date)
                }
            }
            2-> {
                // 삭제한 안부에 포함되어있던 질문들의 connectedSafetyList에서 삭제한 안부 삭제
                for (q in questionList){
                    database.getReference("question")
                        .child(q.first)
                        .child("connectedSafetyList")
                        .child(safetyId).setValue(null)

                    // 해당하는 질문들 보호자 질문 목록에서 최종 수정일 변경하기
                    database.getReference("guardian").child(Firebase.auth.currentUser!!.uid)
                        .child("questionList").child(q.first).setValue(date)
                }
            }
        }
    }

    /**
     * 해당 안부가 삭제 가능한지 확인해주는 메서드
     *  - 안부에 설정된 질문들이 모두 로그인한 보호자의 소유일 경우 : true 반환
     *  - 안부에 설정된 질문들 중 로그인한 보호자의 소유가 아닌 질문이 있는 경우 : false 반환
     */
    private fun canDelete() : Boolean {
        for (q in questionList){
            // 내 소유가 아닌 질문을 찾으면
            if (q.second.owner.toString() != owner){
                Toast.makeText(this, "안부에 다른 보호자의 질문이 있습니다", Toast.LENGTH_SHORT).show()
                return false
            }
        }
        return true
    }

    /**
     * "안부 가져오기" 버튼 세팅해주는 함수
     */
    private fun setGetSafetyButton() {
        findViewById<Button>(R.id.edit_ward_safety_get_button).setOnClickListener{
            val tmpIntent = Intent(this, GetGuardianSafetyActivity::class.java)
            startActivityForResult(tmpIntent, 2)
        }
    }

    /**
     * 피보호자에게 안부 동기화 FCM 보내는 메서드
     *  - message : FCM에 담을 텍스트 메세지
     */
    private fun wardSafetySync(message : String) {
        val wardRef = Firebase.database.reference.child("user").child(wardId)
        wardRef.get().addOnSuccessListener {
            // 피보호자에게 보내기
            val wardDTO = it.getValue(UserDTO::class.java) as UserDTO
            val notificationData = NotificationDTO.NotificationData("SafetyWard"
                , "안심우체국", "안부에 변경사항이 있습니다")
            val notificationDTO = NotificationDTO(wardDTO.token, "high", notificationData)
            firebaseViewModel.sendNotification(notificationDTO) /* FCM 전송하기 */

            // 피보호자와 연결된 보호자에게 동기화 FCM 보내기
            guardianSafetySync(message)
        }
    }

    /**
     * 피보호자와 연결된 보호자들에게 안부 동기화 FCM 보내는 메서드
     *  - 전송 후 액티비티 종료
     *  - message : FCM에 담을 텍스트 메세지
     */
    private fun guardianSafetySync(message : String) {
        val guardianListRef = database.getReference("ward").child(wardId).child("connectList")
        guardianListRef.get().addOnSuccessListener {
            val guardianList = (it.getValue() as HashMap<String, String>).values.toList()
            val UserRef = database.getReference("user")
            for (guardianId in guardianList){
                UserRef.child(guardianId).child("token").get().addOnSuccessListener {
                    val notificationData = NotificationDTO.NotificationData("SafetyGuardian",
                        "안심우체국", "${wardName}님의 안부가 $message")
                    val notificationDTO = NotificationDTO(it.getValue().toString(),"high", notificationData)
                    firebaseViewModel.sendNotification(notificationDTO) /* FCM 전송하기 */
                }
            }
        }
        Handler().postDelayed({
            // 액티비티 종료
            Toast.makeText(this, "안부가 $message", Toast.LENGTH_SHORT).show()
            finish()
        }, 1500)
    }

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
                    editWardSafetyAdapter.notifyDataSetChanged()
                    deletedQuestionList = data?.getStringArrayListExtra("deletedQuestionList")!!
                    val checkQuestions = data?.getStringArrayListExtra("returnQuestionList")
                    val QuestionRef = database.getReference("question")
                    for (q in checkQuestions!!){
                        QuestionRef.child(q).get().addOnSuccessListener {
                            questionList.add(Pair(q, it.getValue(QuestionDTO::class.java)) as Pair<String, QuestionDTO>)
                            editWardSafetyAdapter.notifyDataSetChanged()
                        }
                    }
                }
                2 -> {  // 보호자 안부가 적용되었을 경우
                    val QuestionsFromSafety = data?.getStringArrayListExtra("questionsFromSafety")
                    val QuestionRef = database.getReference("question")
                    val qIdList = questionList.toMap().keys
                    for (q in QuestionsFromSafety!!){
                        if (!qIdList.contains(q)){
                            QuestionRef.child(q).get().addOnSuccessListener {
                                questionList.add(Pair(q, it.getValue(QuestionDTO::class.java)) as Pair<String, QuestionDTO>)
                                editWardSafetyAdapter.notifyDataSetChanged()
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * 액티비티 종료 시 뮤텍스를 반환하는 메서드
     *  - 안부 수정 후 다른 보호자가 수정 화면에 접근할 수 있게하기 위함
     *  - 뮤텍스 획득 시기 : "WardSafetySettingActivity"의 RecyclerView에서 해당 안부를 클릭 했을 경우
     *      - 클릭 시 뮤텍스가 없다면 접근 불가
     */
    override fun onDestroy() {
        super.onDestroy()
        database.getReference("safety").child(safetyId).child("Access")
            .setValue(null)
    }

}