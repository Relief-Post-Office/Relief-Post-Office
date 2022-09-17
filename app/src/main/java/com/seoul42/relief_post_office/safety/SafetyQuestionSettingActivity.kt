package com.seoul42.relief_post_office.safety

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.util.SparseBooleanArray
import android.view.LayoutInflater
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.widget.*
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.seoul42.relief_post_office.R
import com.seoul42.relief_post_office.adapter.QuestionFragmentRVAdapter
import com.seoul42.relief_post_office.adapter.SafetyQuestionSettingAdapter
import com.seoul42.relief_post_office.adapter.WardSafetyAdapter
import com.seoul42.relief_post_office.model.ListenerDTO
import com.seoul42.relief_post_office.model.QuestionDTO
import com.seoul42.relief_post_office.record.RecordActivity
import com.seoul42.relief_post_office.tts.TextToSpeech
import com.seoul42.relief_post_office.viewmodel.FirebaseViewModel
import kotlinx.android.synthetic.main.activity_safety_question_setting.*
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class SafetyQuestionSettingActivity : AppCompatActivity() {

    private val storage: FirebaseStorage by lazy {
        FirebaseStorage.getInstance()
    }

    private val firebaseViewModel : FirebaseViewModel by viewModels()
    private val database = Firebase.database
    private var questionList = arrayListOf<Pair<String, QuestionDTO>>()
    private var deletedQuestionList = arrayListOf<String>()
    private lateinit var checkedQuestions : ArrayList<String>
    private lateinit var safetyQuestionSettingAdapter: SafetyQuestionSettingAdapter
    private lateinit var owner : String
    private lateinit var listenerDTO : ListenerDTO

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_safety_question_setting)

        // 전달 받은 question들 questionList에 넣기
        checkedQuestions = intent.getStringArrayListExtra("questionList") as ArrayList<String>

        // 로그인 한 사람 uid 가져오기
        owner = Firebase.auth.currentUser?.uid.toString()

        /* questionList 세팅 */
        setQuestionList()

        /* recycler 뷰 세팅 */
        setRecyclerView()

        /* 질문 추가 버튼 세팅 */
        setAddQuestionButton()

        /* 저장 버튼 세팅 */
        setSaveButton()
    }

    /* 저장 버튼 세팅 */
    private fun setSaveButton() {
        findViewById<Button>(R.id.safety_question_setting_save_button).setOnClickListener {
            it.isClickable = false
            val returnIntent = Intent()
            returnIntent.putExtra("returnQuestionList", safetyQuestionSettingAdapter.checkedQuestions)
            returnIntent.putExtra("deletedQuestionList", safetyQuestionSettingAdapter.deletedQuestions)
            setResult(Activity.RESULT_OK, returnIntent)
            finish()
        }
    }

    /* 질문 추가 버튼 세팅 */
    @RequiresApi(Build.VERSION_CODES.O)
    private fun setAddQuestionButton() {
        val questionPlusBtn = findViewById<ImageView>(R.id.safety_question_setting_add_question)
        questionPlusBtn.setOnClickListener{
            // 질문 추가 다이얼로그 띄우기
            val dialog = android.app.AlertDialog.Builder(this).create()
            val eDialog : LayoutInflater = LayoutInflater.from(this)
            val mView : View = eDialog.inflate(R.layout.setting_question_dialog,null)

            dialog.setView(mView)
            dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
            dialog.create()
            dialog.show()

            // 녹음기능
            val recordActivity = RecordActivity(mView)

            recordActivity.initViews()
            recordActivity.bindViews()
            recordActivity.initVariables()

            // tts 기능
            val textToSpeech = TextToSpeech(mView, dialog.context)

            textToSpeech.initTTS()

            // 녹음 활성를 할 것인지에 대한 이벤트 처리
            val recordLayout = dialog.findViewById<LinearLayout>(R.id.question_add_record_layout)
            var ttsFlag = true

            dialog.findViewById<Switch>(R.id.question_add_voice_record).setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) {
                    recordLayout.visibility = View.VISIBLE
                    ttsFlag = false
                } else {
                    recordLayout.visibility = View.GONE
                    ttsFlag = true
                }
            }

            // 질문 추가 다이얼로그의 "저장"버튼을 눌렀을 때 이벤트 처리
            dialog.findViewById<Button>(R.id.add_question_btn).setOnClickListener {
                it.isClickable = false
                // 프로그레스바 처리
                val progressBar = dialog.findViewById<ProgressBar>(R.id.setting_question_progressbar)
                lateinit var recordFile : Uri

                progressBar.visibility = View.VISIBLE
                dialog.window!!.addFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)

                // 생성 날짜, 텍스트, 비밀 옵션, 녹음 옵션, 녹음 파일 주소
                val date = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
                val questionText = dialog.findViewById<EditText>(R.id.question_text).text.toString()
                val secret = dialog.findViewById<Switch>(R.id.secret_switch).isChecked
                val record = dialog.findViewById<Switch>(R.id.record_switch).isChecked
                var src: String? = null

                // question 컬렉션에 추가할 QuestoinDTO 생성
                val newQuestion = QuestionDTO(secret, record, ttsFlag, owner, date, questionText, src, mutableMapOf())

                // 녹음 중이라면 중단 후 저장
                recordActivity.stopRecording()
                // 재생 중이라면 재생 중단
                recordActivity.stopPlaying()

                if (ttsFlag) {
                    textToSpeech.synthesizeToFile(newQuestion.text!!)
                    Handler().postDelayed({
                        // 녹음 파일 생성 및 스토리지 저장
                        recordFile = Uri.fromFile(File(textToSpeech.returnRecordingFile()))
                        addRecordToStorage(recordFile, newQuestion, date, dialog, progressBar)
                    }, 2000)
                } else {
                    // 녹음 파일 생성 및 스토리지 저장
                    Handler().postDelayed({
                        // 녹음 파일 생성 및 스토리지 저장
                        recordFile = Uri.fromFile(File(recordActivity.returnRecordingFile()))
                        addRecordToStorage(recordFile, newQuestion, date, dialog, progressBar)
                    }, 2000)
                }
            }
        }
    }

    private fun addRecordToStorage(
        recordFile : Uri,
        newQuestion : QuestionDTO,
        date : String,
        dialog : AlertDialog,
        progressBar : ProgressBar
    ) {
        val recordRef = storage.reference
            .child("questionRecord/${owner}/${owner + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))}")

        // 녹음 업로드에 성공한 경우(녹음이 있는 경우)
        recordRef.putFile(recordFile).addOnSuccessListener {
            recordRef.downloadUrl.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    // question 컬렉션에 작성한 내용 추가
                    val questionRef = database.getReference("question")
                    val newPush = questionRef.push()
                    val key = newPush.key.toString()

                    newQuestion.src = task.result.toString()
                    newPush.setValue(newQuestion)

                    // 지금 로그인한 사람 질문 목록에 방금 등록한 질문 아이디 추가
                    val userQuestionRef = database.getReference("guardian").child(owner).child("questionList")
                    userQuestionRef.child(key).setValue(date)

                    // 다이얼로그 종료
                    Toast.makeText(this, "질문 추가 완료", Toast.LENGTH_SHORT).show()
                    dialog.dismiss()
                }
            }
            // 녹음 업로드에 실패한 경우(녹음이 없는 경우 + @) 질문 추가 불가능
        }.addOnFailureListener{
            progressBar.visibility = View.INVISIBLE
            dialog.window!!.clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
            Toast.makeText(this, "녹음 파일을 생성해 주세요", Toast.LENGTH_SHORT).show()
            dialog.findViewById<Button>(R.id.add_question_btn).isClickable = true
        }
    }

    /* questionList 실시간 업데이트 (수정 및 변경 적용 포함) */
    private fun setQuestionList(){
        // 로그인한 유저의 질문 목록
        val userQuestionRef = database.getReference("guardian").child(owner).child("questionList")

        // questionList에 로그인한 유저의 질문들 넣기
        val questionListener = userQuestionRef.addChildEventListener(object : ChildEventListener {
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                // 로그인한 유저의 질문 하나씩 참조
                val questionId = snapshot.key.toString()
                val questionToAdd = database.getReference("question").child(questionId)

                // 질문 컬렉션에서 각 질문 불러와서 questionList에 넣기
                questionToAdd.get().addOnSuccessListener {
                    questionList.add(Pair(questionId, it.getValue(QuestionDTO::class.java) as QuestionDTO))
                    // 내림차순으로 정렬
                    questionList.sortedByDescending { it.second.date }
                    // 리사이클러 뷰 어댑터에 알려주기
                    safetyQuestionSettingAdapter.notifyDataSetChanged()
                }
            }
            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
                // questionID 찾기
                val questionId = snapshot.key.toString()
                val questionInDB = database.getReference("question").child(questionId)

                // questionList에서 questionID에 해당하는 질문 찾아 수정해주기
                for (q in questionList) {
                    if (q.first == questionId) {
                        questionInDB.get().addOnSuccessListener {
                            q.second.text = it.child("text").getValue().toString()
                            q.second.record = it.child("record").getValue() as Boolean
                            q.second.secret = it.child("secret").getValue() as Boolean
                            q.second.date = it.child("date").getValue().toString()
                            q.second.src = it.child("src").getValue().toString()
                            q.second.ttsFlag = it.child("ttsFlag").getValue() as Boolean

                            // 가장 최근에 수정된 것이 리스트 상단으로 가게 하기
                            // 내림차순으로 정렬(map -> list.sort -> map)
                            questionList.sortedByDescending { it.second.date }

                            // 리스트가 수정되었다고 어댑터에게 알려주기
                            safetyQuestionSettingAdapter.notifyDataSetChanged()
                        }
                        break
                    }
                }
            }
            override fun onChildRemoved(snapshot: DataSnapshot) {
                // questionID 찾기
                val questionId = snapshot.key.toString()

                // questionList에서 questionID에 해당하는 질문 찾아 삭제하기
                for (q in questionList){
                    if (q.first == questionId){
                        questionList.remove(q)
                        break
                    }
                }
                // 리스트가 수정되었다고 어댑터에게 알려주기
                safetyQuestionSettingAdapter.notifyDataSetChanged()
            }
            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {
            }
            override fun onCancelled(error: DatabaseError) {
            }
        })
        listenerDTO = ListenerDTO(userQuestionRef, questionListener)
    }

    /* 리사이클러 뷰 세팅 */
    private fun setRecyclerView(){
        val rv = findViewById<RecyclerView>(R.id.safety_question_setting_rv)
        // 리사이클러 뷰 아답터에 리스트 넘긴 후 아답터 가져오기
        safetyQuestionSettingAdapter = SafetyQuestionSettingAdapter(
            this, checkedQuestions, questionList, firebaseViewModel)
        // 리사이클러 뷰에 아답터 연결하기
        rv.adapter = safetyQuestionSettingAdapter
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