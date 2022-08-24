package com.seoul42.relief_post_office.ward

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.graphics.drawable.AnimationDrawable
import android.media.MediaPlayer
import android.os.Bundle
import android.os.Handler
import android.view.WindowManager
import android.widget.ImageButton
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.app.ActivityCompat
import androidx.core.view.isVisible
import com.google.firebase.database.ktx.database
import com.google.firebase.database.ktx.getValue
import com.google.firebase.ktx.Firebase
import com.seoul42.relief_post_office.R
import com.seoul42.relief_post_office.databinding.ActivityAlarmBinding
import com.seoul42.relief_post_office.model.*
import java.text.SimpleDateFormat
import java.util.*

class AlarmActivity : AppCompatActivity() {

    private val binding: ActivityAlarmBinding by lazy {
        ActivityAlarmBinding.inflate(layoutInflater)
    }

    private val resultDB = Firebase.database.reference.child("result")
    private val answerDB = Firebase.database.reference.child("answer")
    private val answerList: ArrayList<Pair<String, AnswerDTO>> = arrayListOf()

    private lateinit var resultId : String
    private lateinit var imageView : ImageButton
    private lateinit var animationDrawable: AnimationDrawable
    private var mediaPlayer: MediaPlayer? = null

    @Override
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        setStatusBarTransparent()
        removeNotification()
        checkResponse()
    }

    /* MediaPlayer release */
    override fun onDestroy() {
        super.onDestroy()

        if (mediaPlayer != null) {
            mediaPlayer!!.release()
            mediaPlayer = null
        }
    }

    private fun checkResponse() {
        val recommendDTO =
            intent.getSerializableExtra("recommendDTO") as WardRecommendDTO

        resultDB.child(recommendDTO.resultId!!).get().addOnSuccessListener { result ->
            val resultDTO = result.getValue(ResultDTO::class.java)
                ?: throw IllegalArgumentException("result required")

            if (resultDTO.responseTime != "미응답") {
                finish()
            } else {
                imageView = binding.alarmButton
                resultId = recommendDTO.resultId!!
                animationDrawable = imageView.background as AnimationDrawable
                animationDrawable.start()
                setAlarm(resultDTO)
                setButton()
            }
        }
    }

    /* 알람 설정 */
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

        /* 5분 뒤에 알람 종료 */
        Handler().postDelayed({
            if (mediaPlayer != null) {
                alarmClose()
            }
            finish()
        }, finishTime)
    }

    /* 버튼 text = 피보호자가 진행해야 할 "안부" 이름 */
    private fun setButton() {
        binding.alarmButton.setOnClickListener {
            setAnswerStart()
            // voice
            // 안부 시작 안내 보이스
            Handler().postDelayed({
                setGuideAndStartSafety()
            }, 500)
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

    private fun setGuideAndStartSafety() {
        val startGuideVoice = MediaPlayer.create(this, R.raw.startingsafety)

        startGuideVoice.start()
        startGuideVoice.setOnCompletionListener {
            val intent = Intent(this, AnswerActivity::class.java)

            startGuideVoice.release()
            ActivityCompat.finishAffinity(this)
            intent.putExtra("resultId", resultId)
            intent.putExtra("answerList", answerList)
            startActivity(intent)
        }
    }

    private fun setStatusBarTransparent() {
        window.setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)
    }

    /* 상단 notification 을 없애기 */
    private fun removeNotification() {
        val notificationManager = this.getSystemService(
            Context.NOTIFICATION_SERVICE) as NotificationManager

        notificationManager.cancelAll()
    }

    /* 알람 종료 */
    private fun alarmClose() {
        if (mediaPlayer!!.isPlaying) {
            mediaPlayer!!.stop()
            mediaPlayer!!.release()
            mediaPlayer = null
        }
    }
}

