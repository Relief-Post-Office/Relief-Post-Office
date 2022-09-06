package com.seoul42.relief_post_office.ward

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.graphics.drawable.AnimationDrawable
import android.media.MediaPlayer
import android.os.Bundle
import android.os.Handler
import android.text.BoringLayout
import android.view.WindowManager
import android.widget.ImageButton
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.app.ActivityCompat
import androidx.core.view.isVisible
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.ktx.database
import com.google.firebase.database.ktx.getValue
import com.google.firebase.ktx.Firebase
import com.seoul42.relief_post_office.R
import com.seoul42.relief_post_office.databinding.ActivityAlarmBinding
import com.seoul42.relief_post_office.model.*
import java.text.SimpleDateFormat
import java.util.*

/**
 * 피보호자의 안부에 해당하는 강제알람을 띄우도록 돕는 클래스
 * 강제 알람이 5분 동안 지속되고 그 이후에는 종료
 */
class AlarmActivity : AppCompatActivity() {

    private val binding: ActivityAlarmBinding by lazy {
        ActivityAlarmBinding.inflate(layoutInflater)
    }

    // 데이터베이스 참조 변수
    private val resultDB = Firebase.database.reference.child("result")
    private val answerDB = Firebase.database.reference.child("answer")
    private val wardDB = Firebase.database.reference.child("ward")

    // 안부의 여러 질문들 중, 질문에 대한 응답 아이디 및 응답 데이터를 리스트 형태로 담음
    private val answerList: ArrayList<Pair<String, AnswerDTO>> = arrayListOf()

    // 알람 소리를 울릴 수 있도록 하는 미디어 플레이어 변수
    private var mediaPlayer: MediaPlayer? = null

    // 음성 답변 기능(STT) 기능이 켜져있는지 확인하는 변수
    private var sttIsOn : Boolean = false

    private lateinit var myUserId : String
    private lateinit var resultId : String
    private lateinit var imageView : ImageButton
    private lateinit var animationDrawable: AnimationDrawable

    @Override
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        setStatusBarTransparent()
        removeNotification()
        checkResponse()
    }

    /**
     * MediaPlayer release
     */
    override fun onDestroy() {
        super.onDestroy()

        if (mediaPlayer != null) {
            mediaPlayer!!.release()
            mediaPlayer = null
        }
    }

    /**
     * - 미응답인 경우 : 강제 알람을 수행하도록 세팅
     * - 응답한 경우 : 강제 알람을 바로 종료
     */
    private fun checkResponse() {
        val recommendDTO =
            intent.getSerializableExtra("recommendDTO") as WardRecommendDTO
        myUserId = Firebase.auth.currentUser?.uid.toString()

        resultDB.child(recommendDTO.resultId!!).get().addOnSuccessListener { result ->
            val resultDTO = result.getValue(ResultDTO::class.java)
                ?: throw IllegalArgumentException("result required")

            if (resultDTO.responseTime != "미응답") {
                finish()
            } else {
                wardDB.child(myUserId).child("stt").get().addOnSuccessListener{ snapshot ->
                    sttIsOn = snapshot.getValue() as Boolean
                }
                imageView = binding.alarmButton
                resultId = recommendDTO.resultId!!
                animationDrawable = imageView.background as AnimationDrawable
                animationDrawable.start()
                setAlarm(resultDTO)
                setButton()
            }
        }
    }

    /**
     * 강제 알람에서 받아온 안부에 맞게 텍스트 변경 및 알람 소리를 on
     * 알람 시작으로부터 5분 지날시 강제종료
     */
    private fun setAlarm(resultDTO : ResultDTO) {
        val date = SimpleDateFormat("MM월 dd일 E요일 HH : mm")
            .format(Date(System.currentTimeMillis()))
        val curDay = date.substring(0, 11)
        val curTime = date.substring(12, 19)
        val finishTime : Long = 300000

        mediaPlayer = MediaPlayer.create(this, R.raw.alarmbell)
        mediaPlayer!!.start()
        mediaPlayer!!.isLooping = true

        binding.alarmDay.text = curDay
        binding.alarmTime.text = curTime
        binding.alarmText.text = resultDTO.safetyName

        // 5분 뒤에 알람 종료
        Handler().postDelayed({
            if (mediaPlayer != null) {
                alarmClose()
            }
            finish()
        }, finishTime)
    }

    /**
     * 피보호자가 안부 시작 버튼을 누를 시
     *  1. 안부에 대응하는 answerList 를 세팅
     *  2. 울리고 있는 알람을 close
     *  3. 1, 2 작업 완료시 안내 음성이 뜨고 안부 시작
     */
    private fun setButton() {
        binding.alarmButton.setOnClickListener {
            setAnswerStart()
            Handler().postDelayed({
                setGuideAndStartSafety()
            }, 500) // 비동기적 데이터 통신을 고려하여 500ms 딜레이를 설정
        }
    }

    private fun setAnswerStart() {
        binding.alarmProgressbar.isVisible = true
        binding.alarmTransformText.text = "답변 준비중..."
        window.addFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
        checkAndSetAnswerList()
        alarmClose()
    }

    private fun checkAndSetAnswerList() {
        resultDB.child(resultId).child("answerIdList").get()
            .addOnSuccessListener { resultSnapshot ->
                val answerIdList = resultSnapshot.getValue<MutableMap<String, String>>()
                    ?: throw IllegalArgumentException("answerIdList required")
                setAnswerList(answerIdList)
            }
    }

    /**
     * 응답 목록에 담겨있는 모든 응답 아이디 및 응답 데이터를 answerList 에 담음
     *  - answerList => (응답 아이디, 응답 데이터)
     */
    private fun setAnswerList(answerIdList : MutableMap<String, String>) {
        answerIdList.map { questionAndAnswer ->
            val answerId = questionAndAnswer.value
            answerDB.child(answerId).get().addOnSuccessListener { answerSnapshot ->
                val answerDTO = answerSnapshot.getValue<AnswerDTO>()
                    ?: throw IllegalArgumentException("answer required")
                answerList.add(Pair(answerId, answerDTO))
            }
        }
    }

    /**
     * 안내 음성이 종료되면 응답 화면(AnswerActivity)으로 이동
     */
    private fun setGuideAndStartSafety() {
        val startGuideVoice = MediaPlayer.create(this, R.raw.startingsafety)

        startGuideVoice.start()
        startGuideVoice.setOnCompletionListener {
            val intent = Intent(this, AnswerActivity::class.java)

            startGuideVoice.release()
            ActivityCompat.finishAffinity(this)
            intent.putExtra("resultId", resultId)
            intent.putExtra("answerList", answerList)
            intent.putExtra("sttIsOn", sttIsOn)
            startActivity(intent)
        }
    }

    private fun setStatusBarTransparent() {
        window.setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)
    }

    /**
     * 상단 notification 을 없애는 메서드
     */
    private fun removeNotification() {
        val notificationManager = this.getSystemService(
            Context.NOTIFICATION_SERVICE) as NotificationManager

        notificationManager.cancelAll()
    }

    private fun alarmClose() {
        if (mediaPlayer!!.isPlaying) {
            mediaPlayer!!.stop()
            mediaPlayer!!.release()
            mediaPlayer = null
        }
    }
}

