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

/**
 * 피보호자가 안부에 응답하는 클래스
 * AlarmActivity 에서 intent 로 넘어오는 AnswerList 의 응답지에 응답내용을 작성합니다.
 *
 * UI에 표시될 내용 : 질문 내용,
 * 기능 : 질문이 순서대로 나오고 응답하면 자동으로 넘어갑니다. 아래 루틴을 따라갑니다.
 *  1. 질문과 stt 가 시작됩니다.
 *  2. 답변
 *      2-1. O, stt(긍정 답변)
 *          2-1-1. 음성 답변 옵션 답변있다면 자동으로 음성 녹음이 시작됩니다.
 *      2-2. X, stt(부정 답변)
 *  3. answer 을 데이터베이스와 동기화 시킵니다.
 *  모든 질문이 끝나면 FCM 을 보냅니다.
 */
class AnswerActivity : AppCompatActivity() {
    // 레이아웃과 연결
    private val binding: WardSafetyBinding by lazy {
        WardSafetyBinding.inflate(layoutInflater)
    }

    // 데이터베이스
    private val storage: FirebaseStorage by lazy {
        FirebaseStorage.getInstance()
    }

    // STT 기능을 위한 객체
    private var auth : FirebaseAuth = Firebase.auth
    private val database = Firebase.database
    private val answerDB = database.getReference("answer")
    private val resultDB = database.getReference("result")
    private val userDB = database.getReference("user")
    private val wardDB = Firebase.database.getReference("ward")

    // STT 기능을 위한 객체
    private var speechRecognizer: SpeechRecognizer? = null
    private var sttIsOn: Boolean = false

    // 응답해야하는 응답지
    private lateinit var answerList: ArrayList<Pair<String, AnswerDTO>>
    private var listSize = 0
    private lateinit var resultId : String

    // 응답 카운터
    private var currentIndex: Int = 0

    // 버튼 클릭 소리
    private lateinit var answerBell : MediaPlayer

    // 질문 미디어
    private lateinit var questionPlayer : MediaPlayer

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        // AlarmActivity 에서 넘어오는 값 셋팅
        getId()
        // 반복적으로 응답해야 하는 횟수 셋팅
        setListSize(answerList)
        // 버튼 이벤트 셋팅
        setButton()

        // stt 시작
        startStt()
    }

    /**
     * AlarmActivity 에서 intent 로 넘어오는 값을 변수에 대입하는 매서드
     */
    private fun getId() {
        answerBell = MediaPlayer.create(this, R.raw.bell)
        resultId = intent.getStringExtra("resultId").toString()
        answerList = intent.getSerializableExtra("answerList") as ArrayList<Pair<String, AnswerDTO>>
        sttIsOn = intent.getBooleanExtra("sttIsOn", false)
    }

    /**
     * 반복하는 횟수를 세팅하고 첫번째 질문 세팅하는 매서드
     */
    private fun setListSize(answerList: ArrayList<Pair<String, AnswerDTO>>) {
        listSize = answerList.size
        currentIndex = 0
        // 질문 플레이어 세팅
        questionPlayer = MediaPlayer()
        questionPlayer.setOnCompletionListener {
            questionPlayer.stop()
            questionPlayer.prepare()
        }
        setQuestion()
    }

    /**
     * 버튼 이벤트를 세팅하는 매서드
     *
     * - o버튼 클릭
     * - x버튼 클릭
     */
    @RequiresApi(Build.VERSION_CODES.O)
    private fun setButton() {
        // 버튼 색깔 설정
        binding.wardSafetyYes.buttonColor = resources.getColor(R.color.yes)
        binding.wardSafetyNo.buttonColor = resources.getColor(R.color.no)
        binding.wardSafetyRepeat.buttonColor = resources.getColor(R.color.gray)

        // X 버튼 클릭
        binding.wardSafetyNo.setOnClickListener {
            window.addFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
            // 버튼 클릭 소리 재생
            answerBell.start()
            // 부정 답변 세팅
            val reply: Boolean = false
            val recordSrc: String = ""
            // 부정 답변 데이터베이스 동기화
            sendAnswer(reply, recordSrc)
            // 다음 질문으로 넘어감
            Handler().postDelayed({
                nextQuestion()
            }, 1500)
        }

        // O 버튼 클릭
        binding.wardSafetyYes.setOnClickListener {
            window.addFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
            // 버튼 클릭 소리 재생
            answerBell.start()
            // 긍정 답변 세팅
            val reply: Boolean = true
            var recordSrc: String = ""
            // 긍정 답변 + 음성 답변 세팅 후 데이터베이스 동기회
            if (answerList[currentIndex].second.questionRecord) {
                // 음성 답변 옵션이 활성화 된 경우

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
                // 음성 답변 옵션이 아닐 경우
                sendAnswer(reply, recordSrc)
                Handler().postDelayed({
                    nextQuestion()
                }, 1500)
            }
        }
    }

    /**
     * currentIndex 번재 질문을 셋팅하는 메서드
     *
     * - 질문의 텍스트 셋팅
     * - 질문 미디어 객체 생성
     * - 다시듣기 버튼 이벤트 셋팅
     * - stt 셋팅
     */
    private fun setQuestion() {
        // 질문 텍스트 셋팅
        binding.wardSafetyQuestion.text = answerList[currentIndex].second.questionText
        // 질문 녹음 파일 미디어 셋팅
        questionPlayer = MediaPlayer().apply {
            setDataSource(answerList[currentIndex].second.questionSrc)
        }
        // 질문 녹음 파일 재생이 끝나면 발생하는 이벤트 셋팅
        questionPlayer.setOnCompletionListener {
            questionPlayer.stop()
            questionPlayer.prepare()
        }
        // 질문 녹음 파일 재생
        questionPlayer.prepare()
        questionPlayer.start()
        // 다시 듣기 버튼을 누르면 발생하는 이벤트 셋팅 - 미디어 초기화
        binding.wardSafetyRepeat.setOnClickListener {
            // 오디오 다시 듣기 경로 셋팅
            questionPlayer.stop()
            questionPlayer.prepare()
            questionPlayer.start()
        }
        startStt()
        window.clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
    }

    /**
     * 응답 결과를 데이터베스에 보낸 매서드
     */
    private fun sendAnswer(reply: Boolean, recordSrc: String) {
        answerDB
            .child(answerList[currentIndex].first)
            .child("reply")
            .setValue(reply)
        answerDB
            .child(answerList[currentIndex].first)
            .child("answerSrc")
            .setValue(recordSrc)
    }

    /**
     * 다음 질문으로 넘어가믐 매서드
     *
     * - 다음 질문이 있을 경우 인덱스 변경과 세팅
     * - 마지막 질문일 경우 EndingActivity 로 넘어갑니다.
     */
    private fun nextQuestion() {
        // 음성 인식 종료
        speechRecognizer?.stopListening()
        speechRecognizer?.destroy()

        if (currentIndex < listSize - 1) {
            // 다음 질문이 있을 경우
            currentIndex += 1
            questionPlayer.release()
            // 다음 질문 세팅
            setQuestion()
        }
        else {
            // 마지막 질문일 경우
            // 안부 응답 완료 FCM 보내기
            setFCM()
            Handler().postDelayed({
                // 응답 종료 시간 측정
                val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
                val date = sdf.format(System.currentTimeMillis())
                // EndingActivity 를 위한 intent
                val intent = Intent(this, EndingActivity::class.java)

                // 종료 벨소리
                answerBell.release()
                // 결과 데이터베이스 동기화
                resultDB
                    .child(resultId)
                    .child("responseTime")
                    .setValue(date)
                intent.putExtra("resultId", resultId)
                startActivity(intent)
                finish()
            }, 800)
        }
    }

    /**
     * 결과에 대한 FCM 을 보내는 메서드
     *
     * 로그인 유저 -> 피보호자 -> 결과 -> FCM
     */
    private fun setFCM() {
        val uid = Firebase.auth.uid.toString()
        setUser(uid)
    }

    /**
     * FCM 의 user를 찾는 매서드
     * userId -> userDTO
     */
    private fun setUser(uid : String) {
        userDB.child(uid).get().addOnSuccessListener { user ->
            if (user.getValue(UserDTO::class.java) != null) {
                val userDTO = user.getValue(UserDTO::class.java) as UserDTO
                setWard(uid, userDTO)
            }
        }
    }

    /**
     * userid 를 통해 wardDTO 를 찾는 매서드
     */
    private fun setWard(uid : String, userDTO : UserDTO) {
        wardDB.child(uid).get().addOnSuccessListener { ward ->
            if (ward.getValue(WardDTO::class.java) != null) {
                val wardDTO = ward.getValue(WardDTO::class.java) as WardDTO
                setResult(userDTO, wardDTO)
            }
        }
    }

    /**
     * intent 의 resultId 를 통해 resultDTO 를 가져오고 FCM을 보내는 매서드
     */
    private fun setResult(userDTO : UserDTO, wardDTO : WardDTO) {
        val resultId = intent.getStringExtra("resultId").toString()

        resultDB.child(resultId).get().addOnSuccessListener { result ->
            if (result.getValue(ResultDTO::class.java) != null) {
                val resultDTO = result.getValue(ResultDTO::class.java)
                sendFCM(userDTO.name, wardDTO.connectList, resultDTO!!.safetyName)
            }
        }
    }

    /**
     * 피보호자와 연결된 모든 보호자에게
     * "<피보호자 이름> 님이 <안부 이름> 안부를 완료했습니다"
     * 라는 FCM 를 보내는 매서드
     */
    private fun sendFCM(myName : String, connectList : MutableMap<String, String>, safety : String) {
        val firebaseViewModel : FirebaseViewModel by viewModels()

        // 연결된 모든 보호자 찾기
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
     * 이하는 음성 대답 기능(STT) 구현을 위한 메서드
     */

    /**
     * SpeechToText 설정 및 동작
     */
    private fun startStt() {
        // STT 옵션이 켜져있고 질문이 녹음 회신 옵션이 꺼진 경우에만 실행
        if (sttIsOn && !answerList[currentIndex].second.questionRecord) {
            val speechRecognizerintent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, packageName)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ko-KR")
            }

            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this).apply {
                setRecognitionListener(recognitionListener())
                startListening(speechRecognizerintent)
            }
        }
    }

    /**
     * SpeechToText 기능 세팅
     *  - onResults
     *      - 해석 결과가 긍정 / 부정 / 다시 듣기에 해당하면 알맞은 기능 수행
     *      - 위에 해당하지 않는다면 음성인식 다시 수행
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
                SpeechRecognizer.ERROR_NO_MATCH -> {
                    message = "음성 인식 실패"
                    startStt()
                }
                SpeechRecognizer.ERROR_RECOGNIZER_BUSY ->
                    message = "RECOGNIZER가 바쁨"
                SpeechRecognizer.ERROR_SERVER ->
                    message = "서버가 이상함"
                SpeechRecognizer.ERROR_SPEECH_TIMEOUT ->
                    message = "말하는 시간초과"
                else ->
                    message = "알 수 없는 오류"
            }
            Toast.makeText(applicationContext, "에러 발생 $message", Toast.LENGTH_SHORT)
        }

        override fun onResults(results: Bundle?) {
            var str = results!!.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)!![0]
            var positiveWordList = arrayListOf("그래", "으응", "응", "엉", "어")
            var negativeWordList = arrayListOf("아니", "않이", "안이")
            var replayWordList = arrayListOf("다시", "다시 듣기")

            when(checkResults(str, positiveWordList, negativeWordList, replayWordList)) {
                1 -> {
                    window.addFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
                    answerBell.start()
                    val reply = true
                    var recordSrc = ""
                    sendAnswer(reply, recordSrc)
                    Handler().postDelayed({
                        nextQuestion()
                    }, 1500)
                }

                2 -> {
                    window.addFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
                    answerBell.start()
                    val reply = false
                    var recordSrc = ""

                    sendAnswer(reply, recordSrc)
                    Handler().postDelayed({
                        nextQuestion()
                    }, 1500)
                }

                3 -> {
                    questionPlayer.stop()
                    questionPlayer.prepare()
                    questionPlayer.start()
                    startStt()
                }

                else -> startStt()
            }
        }

        override fun onPartialResults(p0: Bundle?) { }

        override fun onEvent(p0: Int, p1: Bundle?) { }
    }

    /**
     * 문자열에 찾는 단어가 있는지 확인하여 3가지 시그널을 반환함
     *  - str : 검색할 문자열
     *  - positiveWordList : 긍정 단어들을 담은 리스트
     *  - negativeWordList : 부정 단어들을 담은 리스트
     *  - replayWordList : 다시 듣기 단어들을 담은 리스트
     *  - 시그널 종류
     *      - 1 : 긍정
     *      - 2 : 부정
     *      - 3 : 다시 듣기
     *      - -1 : 일치하는 단어 없음
     */
    private fun checkResults(
        str : String,
        positiveWordList : ArrayList<String>,
        negativeWordList : ArrayList<String>,
        replayWordList : ArrayList<String>) : Int {

        if (positiveWordList.contains(str))
            return 1
        else if (negativeWordList.contains(str))
            return 2
        else if (replayWordList.contains(str))
            return 3
        return -1
    }
}