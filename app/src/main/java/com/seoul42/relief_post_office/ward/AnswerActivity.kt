package com.seoul42.relief_post_office.ward

import android.content.Intent
import android.media.MediaPlayer
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.seoul42.relief_post_office.databinding.WardSafetyBinding
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.seoul42.relief_post_office.R
import com.seoul42.relief_post_office.model.AnswerDTO
import com.seoul42.relief_post_office.model.QuestionDTO
import java.text.SimpleDateFormat

class AnswerActivity : AppCompatActivity() {

    private val binding: WardSafetyBinding by lazy {
        WardSafetyBinding.inflate(layoutInflater)
    }

    private val database = Firebase.database
    private lateinit var questionList: ArrayList<Pair<String, QuestionDTO>>
    private lateinit var answerList: ArrayList<Pair<String, AnswerDTO>>
    private var listSize = 0
    private var currentIndex: Int = 0
    private lateinit var resultId : String
    private lateinit var questionPlayer : MediaPlayer

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        getId()
        setSize(questionList.size, answerList.size)
        setButton()
    }

    private fun getId() {
        resultId = intent.getStringExtra("resultId").toString()
        questionList = intent.getSerializableExtra("questionList") as ArrayList<Pair<String, QuestionDTO>>
        answerList = intent.getSerializableExtra("answerList") as ArrayList<Pair<String, AnswerDTO>>
    }

    private fun setSize(questionListSize: Int, answerListSize : Int) {
        if (questionListSize == answerListSize) {
            listSize = questionListSize
            currentIndex = 0
            // 녹음 플레이어 세팅
            questionPlayer = MediaPlayer()
            questionPlayer.setOnCompletionListener {
                questionPlayer.stop()
                questionPlayer.prepare()
            }
            setQuestion()
        }
    }

    private fun setButton() {
        binding.wardSafetyYes.buttonColor = resources.getColor(R.color.yes)
        binding.wardSafetyNo.buttonColor = resources.getColor(R.color.no)
        binding.wardSafetyRepeat.buttonColor = resources.getColor(R.color.gray)
        binding.wardSafetyNo.setOnClickListener {
            val reply: Boolean = false
            var recordSrc: String = ""
            if (!questionList[currentIndex].second.record) {
                // 녹음 시작
                // 녹음 끝
                recordSrc = "녹음이 끝난 주소"
            }
            sendAnswer(reply, recordSrc)
            nextQuestion()
        }
        binding.wardSafetyYes.setOnClickListener {
            val reply: Boolean = true
            var recordSrc: String = ""
            if (!questionList[currentIndex].second.record) {
                // 녹음 시작
                // 녹음 끝
                recordSrc = "녹음이 끝난 주소"
            }
            sendAnswer(reply, recordSrc)
            nextQuestion()
        }
    }

    private fun setQuestion() {
        binding.wardSafetyQuestion.text = questionList[currentIndex].second.text
        questionPlayer = MediaPlayer().apply {
            setDataSource(questionList[currentIndex].second.src)
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
            val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
            val date = sdf.format(System.currentTimeMillis())
            database.getReference("result")
                .child(resultId)
                .child("responseTime")
                .setValue(date)
            startActivity(Intent(this, EndingActivity::class.java))
            finish()
        }
    }
}