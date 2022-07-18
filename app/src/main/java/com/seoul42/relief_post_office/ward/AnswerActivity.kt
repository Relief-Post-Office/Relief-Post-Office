package com.seoul42.relief_post_office.ward

import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.Window
import android.widget.Button
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.seoul42.relief_post_office.databinding.WardSafetyBinding
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.seoul42.relief_post_office.R
import com.seoul42.relief_post_office.model.AnswerDTO
import com.seoul42.relief_post_office.model.QuestionDTO
import com.seoul42.relief_post_office.record.AnswerRecordActivity
import com.seoul42.relief_post_office.record.RecordActivity
import java.io.File
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class AnswerActivity : AppCompatActivity() {

    private val binding: WardSafetyBinding by lazy {
        WardSafetyBinding.inflate(layoutInflater)
    }

    private val storage: FirebaseStorage by lazy {
        FirebaseStorage.getInstance()
    }

    private var auth : FirebaseAuth = Firebase.auth

    private val database = Firebase.database
    private lateinit var questionList: ArrayList<Pair<String, QuestionDTO>>
    private lateinit var answerList: ArrayList<Pair<String, AnswerDTO>>
    private var listSize = 0
    private var currentIndex: Int = 0
    private lateinit var resultId : String
    private lateinit var questionPlayer : MediaPlayer

    @RequiresApi(Build.VERSION_CODES.O)
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

    @RequiresApi(Build.VERSION_CODES.O)
    private fun setButton() {
        binding.wardSafetyYes.buttonColor = resources.getColor(R.color.yes)
        binding.wardSafetyNo.buttonColor = resources.getColor(R.color.no)
        binding.wardSafetyRepeat.buttonColor = resources.getColor(R.color.gray)
        binding.wardSafetyNo.setOnClickListener {
            val reply: Boolean = false
            var recordSrc: String = ""

            sendAnswer(reply, recordSrc)
            nextQuestion()
        }

        binding.wardSafetyYes.setOnClickListener {
            val reply: Boolean = true
            var recordSrc: String = ""
            if (questionList[currentIndex].second.record) {
                val dialog = android.app.AlertDialog.Builder(binding.root.context).create()
                val eDialog : LayoutInflater = LayoutInflater.from(binding.root.context)
                val mView : View = eDialog.inflate(R.layout.answer_record_dialog, null)

                dialog.setView(mView)
                dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
                dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
                dialog.create()

                dialog.show()

                // 녹음기능
                val answerRecordActivity = AnswerRecordActivity(mView)

                answerRecordActivity.startRecoding()

                var answerRecordFile = Uri.fromFile(File(answerRecordActivity.returnRecordingFile()))
                val answerRecordRef =
                    storage.reference.child("answerRecord/${auth.currentUser?.uid + LocalDateTime.now().format(
                        DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))}")
                var uploadAnswerRecord = answerRecordRef.putFile(answerRecordFile)

                // 다이얼로그 종료 시 이벤트
                dialog.setOnDismissListener {
                    answerRecordActivity.stopRecording()
                }

                dialog.findViewById<Button>(R.id.record_stop_btn).setOnClickListener {
                    // 녹음 중이라면 중단 후 저장
                    answerRecordActivity.stopRecording()
                    Log.d("record", answerRecordRef.toString())
                    uploadAnswerRecord.addOnSuccessListener {
                        answerRecordRef.downloadUrl.addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                recordSrc = task.result.toString()
                            }
                            sendAnswer(reply, recordSrc)
                        }
                        nextQuestion()
                        dialog.dismiss()
                    }
                }
            }
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