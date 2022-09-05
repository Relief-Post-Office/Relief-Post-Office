package com.seoul42.relief_post_office.ward

import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.widget.Button
import android.widget.Toast
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.seoul42.relief_post_office.databinding.WardSafetyBinding
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.seoul42.relief_post_office.R
import com.seoul42.relief_post_office.model.*
import com.seoul42.relief_post_office.record.AnswerRecordActivity
import com.seoul42.relief_post_office.viewmodel.FirebaseViewModel
import java.io.File
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*
import kotlin.collections.ArrayList

class AnswerActivity : AppCompatActivity() {

    private val binding: WardSafetyBinding by lazy {
        WardSafetyBinding.inflate(layoutInflater)
    }

    private val storage: FirebaseStorage by lazy {
        FirebaseStorage.getInstance()
    }

    // STT 기능을 위한 객체
    private var speechRecognizer: SpeechRecognizer? = null
    private var auth : FirebaseAuth = Firebase.auth
    private val database = Firebase.database
    private lateinit var answerList: ArrayList<Pair<String, AnswerDTO>>
    private var listSize = 0
    private var currentIndex: Int = 0
    private lateinit var resultId : String
    private lateinit var answerBell : MediaPlayer
    private lateinit var questionPlayer : MediaPlayer

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        getId()
        setSize(answerList.size)
        setButton()

        // Test
        startStt()
    }

    private fun getId() {
        answerBell = MediaPlayer.create(this, R.raw.bell)
        resultId = intent.getStringExtra("resultId").toString()
        answerList = intent.getSerializableExtra("answerList") as ArrayList<Pair<String, AnswerDTO>>
    }

    private fun setSize(answerListSize : Int) {
        listSize = answerListSize
        currentIndex = 0
        // 녹음 플레이어 세팅
        questionPlayer = MediaPlayer()
        questionPlayer.setOnCompletionListener {
            questionPlayer.stop()
            questionPlayer.prepare()
        }
        setQuestion()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun setButton() {
        binding.wardSafetyYes.buttonColor = resources.getColor(R.color.yes)
        binding.wardSafetyNo.buttonColor = resources.getColor(R.color.no)
        binding.wardSafetyRepeat.buttonColor = resources.getColor(R.color.gray)
        binding.wardSafetyNo.setOnClickListener {
            window.addFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
            answerBell.start()
            val reply: Boolean = false
            var recordSrc: String = ""

            sendAnswer(reply, recordSrc)
            Handler().postDelayed({
                nextQuestion()
            }, 1500)
        }

        binding.wardSafetyYes.setOnClickListener {
            window.addFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
            answerBell.start()
            val reply: Boolean = true
            var recordSrc: String = ""
            if (answerList[currentIndex].second.questionRecord) {
                // 질문 녹음 재생 중지
                questionPlayer.stop()

                // 녹음 안내 가이드 보이스
                val recordGuide = MediaPlayer.create(this, R.raw.recordguide)
                Handler().postDelayed({
                    recordGuide.start()
                }, 600)

                val dialog = android.app.AlertDialog.Builder(binding.root.context).create()
                val eDialog : LayoutInflater = LayoutInflater.from(binding.root.context)
                val mView : View = eDialog.inflate(R.layout.answer_record_dialog, null)

                dialog.setView(mView)
                dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
                dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
                dialog.create()

                Handler().postDelayed({
                    // 녹음기능
                    val answerRecordActivity = AnswerRecordActivity(mView)
                    answerRecordActivity.startRecoding()

                    dialog.show()

                    var answerRecordFile = Uri.fromFile(File(answerRecordActivity.returnRecordingFile()))
                    val answerRecordRef =
                        storage.reference.child("answerRecord/${auth.currentUser?.uid + LocalDateTime.now().format(
                            DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))}")

                    // 다이얼로그 종료 시 이벤트
                    dialog.setOnDismissListener {
                        // 녹음 중이라면 중단 후 저장
                        answerRecordActivity.stopRecording()
                        var uploadAnswerRecord = answerRecordRef.putFile(answerRecordFile)
                        uploadAnswerRecord.addOnSuccessListener {
                            answerRecordRef.downloadUrl.addOnCompleteListener { task ->
                                if (task.isSuccessful) {
                                    recordSrc = task.result.toString()
                                }
                                sendAnswer(reply, recordSrc)
                            }
                            dialog.dismiss()
                        }
                        recordGuide.release()
                        Handler().postDelayed({
                            nextQuestion()
                        }, 1500)
                    }

                    dialog.findViewById<Button>(R.id.record_stop_btn).setOnClickListener {
                        dialog.dismiss()
                    }
                }, 13000)
            }
            else {
                sendAnswer(reply, recordSrc)
                Handler().postDelayed({
                    nextQuestion()
                }, 1500)
            }
        }
    }

    private fun setQuestion() {
        binding.wardSafetyQuestion.text = answerList[currentIndex].second.questionText
        questionPlayer = MediaPlayer().apply {
            setDataSource(answerList[currentIndex].second.questionSrc)
        }
        questionPlayer.setOnCompletionListener {
            questionPlayer.stop()
            questionPlayer.prepare()
        }
        questionPlayer.prepare()
        questionPlayer.start()
        binding.wardSafetyRepeat.setOnClickListener {
            // 오디오 다시 듣기 경로 셋팅
            questionPlayer.stop()
            questionPlayer.prepare()
            questionPlayer.start()
        }
        window.clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
    }

    private fun sendAnswer(reply: Boolean, recordSrc: String) {
        database.getReference("answer")
            .child(answerList[currentIndex].first)
            .child("reply")
            .setValue(reply)
        database.getReference("answer")
            .child(answerList[currentIndex].first)
            .child("answerSrc")
            .setValue(recordSrc)
    }

    private fun nextQuestion() {
        if (currentIndex < listSize - 1) {
            currentIndex += 1
            questionPlayer.release()
            setQuestion()
        }
        else {
            setFCM()
            Handler().postDelayed({
                val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
                val date = sdf.format(System.currentTimeMillis())
                val intent = Intent(this, EndingActivity::class.java)

                answerBell.release()
                database.getReference("result")
                    .child(resultId)
                    .child("responseTime")
                    .setValue(date)
                intent.putExtra("resultId", resultId)
                startActivity(intent)
                finish()
            }, 800)
        }
    }

    private fun setFCM() {
        val uid = Firebase.auth.uid.toString()

        setUser(uid)
    }

    private fun setUser(uid : String) {
        val userDB = Firebase.database.getReference("user")

        userDB.child(uid).get().addOnSuccessListener { user ->
            if (user.getValue(UserDTO::class.java) != null) {
                val userDTO = user.getValue(UserDTO::class.java) as UserDTO
                setWard(uid, userDTO)
            }
        }
    }

    private fun setWard(uid : String, userDTO : UserDTO) {
        val wardDB = Firebase.database.getReference("ward")

        wardDB.child(uid).get().addOnSuccessListener { ward ->
            if (ward.getValue(WardDTO::class.java) != null) {
                val wardDTO = ward.getValue(WardDTO::class.java) as WardDTO
                setResult(userDTO, wardDTO)
            }
        }
    }

    private fun setResult(userDTO : UserDTO, wardDTO : WardDTO) {
        val resultDB = Firebase.database.getReference("result")
        val resultId = intent.getStringExtra("resultId").toString()

        resultDB.child(resultId).get().addOnSuccessListener { result ->
            if (result.getValue(ResultDTO::class.java) != null) {
                val resultDTO = result.getValue(ResultDTO::class.java)
                sendFCM(userDTO.name, wardDTO.connectList, resultDTO!!.safetyName)
            }
        }
    }

    private fun sendFCM(myName : String, connectList : MutableMap<String, String>, safety : String) {
        val userDB = Firebase.database.getReference("user")
        val firebaseViewModel : FirebaseViewModel by viewModels()

        for (connect in connectList) {
            val uid = connect.key
            userDB.child(uid).get().addOnSuccessListener {
                if (it.getValue(UserDTO::class.java) != null) {
                    val userDTO = it.getValue(UserDTO::class.java) as UserDTO
                    val notificationData = NotificationDTO.NotificationData("안심우체국",
                        myName, "$myName 님이 $safety 안부를 완료했습니다.")
                    val notificationDTO = NotificationDTO(userDTO.token,"high", notificationData)
                    firebaseViewModel.sendNotification(notificationDTO) /* FCM 전송하기 */
                }
            }
        }
    }

    /**
     * 아래는 STT 기능 구현을 위한 코드
     */

    /**
     * SpeechToText 설정 및 동작
     */
    private fun startStt() {
        val speechRecognizerintent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, packageName)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ko-KR")
        }

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this).apply {
            setRecognitionListener(recognitionListener())
            startListening(speechRecognizerintent)
        }
    }

    /**
     * SpeechToText 기능 세팅
     */
    private fun recognitionListener() = object : RecognitionListener {
        override fun onReadyForSpeech(p0: Bundle?) { }

        override fun onBeginningOfSpeech() { }

        override fun onRmsChanged(p0: Float) { }

        override fun onBufferReceived(p0: ByteArray?) { }

        override fun onEndOfSpeech() { }

        override fun onError(error: Int) {
            var message : String

            when (error) {
                SpeechRecognizer.ERROR_AUDIO ->
                    message = "오디오 에러"
                SpeechRecognizer.ERROR_CLIENT ->
                    message = "클라이언트 에러"
                SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS ->
                    message = "퍼미션 없음"
                SpeechRecognizer.ERROR_NETWORK ->
                    message = "네트워크 에러"
                SpeechRecognizer.ERROR_NETWORK_TIMEOUT ->
                    message = "네트워크 타임아웃"
                SpeechRecognizer.ERROR_NO_MATCH ->
                    message = "찾을 수 없음"
                SpeechRecognizer.ERROR_RECOGNIZER_BUSY ->
                    message = "RECOGNIZER가 바쁨"
                SpeechRecognizer.ERROR_SERVER ->
                    message = "서버가 이상함"
                SpeechRecognizer.ERROR_SPEECH_TIMEOUT ->
                    message = "말하는 시간초과"
                else ->
                    message = "알 수 없는 오류"
            }
            Toast.makeText(applicationContext, "에러 발생 $message", Toast.LENGTH_SHORT).show()
        }

        override fun onResults(results: Bundle?) {
            Toast.makeText(applicationContext, results.toString(), Toast.LENGTH_SHORT).show()
        }

        override fun onPartialResults(p0: Bundle?) { }

        override fun onEvent(p0: Int, p1: Bundle?) { }
    }
}