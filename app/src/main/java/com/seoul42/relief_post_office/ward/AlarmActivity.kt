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
    private val database = Firebase.database
    private val questionList: ArrayList<Pair<String, QuestionDTO>> = arrayListOf()
    private val answerList: ArrayList<Pair<String, AnswerDTO>> = arrayListOf()

    private lateinit var safetyId : String
    private lateinit var resultId : String

    private lateinit var imageView : ImageButton
    private lateinit var animationDrawable: AnimationDrawable

    private var mediaPlayer: MediaPlayer? = null

    @Override
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        imageView = binding.alarmButton
        animationDrawable = imageView.background as AnimationDrawable

        animationDrawable.start()

        setAlarm()
        setButton()
        setStatusBarTransparent()
        removeNotification()
    }

    /* MediaPlayer release */
    override fun onDestroy() {
        super.onDestroy()

        if (mediaPlayer != null) {
            mediaPlayer!!.release()
            mediaPlayer = null
        }
    }

    /* 알람 설정 */
    private fun setAlarm() {
        val date = SimpleDateFormat("MM월 dd일 E요일 HH : mm")
            .format(Date(System.currentTimeMillis()))
        val curDay = date.substring(0, 11)
        val curTime = date.substring(12, 19)
        val finishTime : Long = 300000
        val recommendDTO = intent.getSerializableExtra("recommendDTO") as WardRecommendDTO
        val safetyDB = Firebase.database.reference.child("safety")

        safetyId = recommendDTO.safetyId
        resultId = recommendDTO.resultId!!

        mediaPlayer = MediaPlayer.create(this, R.raw.alarm)
        mediaPlayer!!.start()
        mediaPlayer!!.isLooping = true

        binding.alarmDay.text = curDay
        binding.alarmTime.text = curTime

        safetyDB.child(recommendDTO.safetyId).get().addOnSuccessListener {
            if (it.getValue(SafetyDTO::class.java) != null) {
                val safetyDTO = it.getValue(SafetyDTO::class.java) as SafetyDTO

                binding.alarmText.text = safetyDTO.name
            }
        }

        /* 5분 뒤에 알람 종료 */
        Handler().postDelayed({
            if (mediaPlayer != null) {
                close()
            }
        }, finishTime)
    }

    /* 버튼 text = 피보호자가 진행해야 할 "안부" 이름 */
    private fun setButton() {
        binding.alarmButton.setOnClickListener{
            binding.alarmProgressbar.isVisible = true
            binding.alarmTransformText.text = "답변 준비중..."
            window.addFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
            setList()
            close()
            // voice
            // 안부 시작 안내 보이스
            val startGuideVoice = MediaPlayer.create(this, R.raw.startingsafety)
            startGuideVoice.start()
            startGuideVoice.setOnCompletionListener {
                startGuideVoice.release()
                val intent = Intent(this, AnswerActivity::class.java)

                ActivityCompat.finishAffinity(this)

                intent.putExtra("resultId", resultId)
                intent.putExtra("questionList", questionList)
                intent.putExtra("answerList", answerList)
                startActivity(intent)
            }
        }
    }

    private fun setList() {
        database.getReference("result")
            .child(resultId)
            .child("answerIdList")
            .get().addOnSuccessListener { snapshot ->
                val answerIdList = snapshot.getValue<MutableMap<String, String>>()

                if (answerIdList != null) {
                    for ((questionId, answerId) in answerIdList) {
                        database.getReference("answer")
                            .child(answerId).get().addOnSuccessListener {
                                val answer = it.getValue<AnswerDTO>()
                                if (answer != null) {
                                    answerList.add(Pair(answerId, answer))
                                }
                            }
                        database.getReference("question")
                            .child(questionId).get().addOnSuccessListener {
                                val question = it.getValue<QuestionDTO>()
                                if (question != null) {
                                    questionList.add(Pair(questionId, question))
                                }
                            }
                    }
                }
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
    private fun close() {
        if (mediaPlayer!!.isPlaying) {
            mediaPlayer!!.stop()
            mediaPlayer!!.release()
            mediaPlayer = null
        }
    }
}

