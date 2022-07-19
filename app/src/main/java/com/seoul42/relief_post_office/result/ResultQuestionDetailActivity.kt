package com.seoul42.relief_post_office.result

import android.media.MediaPlayer
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import com.seoul42.relief_post_office.R
import com.seoul42.relief_post_office.databinding.ActivityResultQuestionDetailBinding
import com.seoul42.relief_post_office.model.AnswerDTO

class ResultQuestionDetailActivity : AppCompatActivity() {
    private val binding by lazy { ActivityResultQuestionDetailBinding.inflate(layoutInflater) }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        val safetyName = intent.getSerializableExtra("safetyName") as String
        val answer = intent.getSerializableExtra("answer") as Pair<String, AnswerDTO>
        val answerDate = intent.getSerializableExtra("answerDate") as String

        setSafetyName(safetyName)
        setDate(answerDate)
        setQuestionText(answer.second.questionText)
        setQuestionRecord(answer.second.questionSrc)
        setAnswerReply(answer.second.reply)
        setAnswerRecord(onRecord(answer.second), answer.second.answerSrc)
        setQuestionOption(answer.second.questionRecord, answer.second.questionSecret)
    }

    private fun setSafetyName(safetyName: String) {
        binding.textResultSafetyName.text = safetyName
    }

    private fun setDate(date: String) {
        binding.resultQuestionDetailDate.text = date.replace("-", "/")
    }

    private fun setQuestionText(questionText: String) {
        binding.resultQuestionText.text = questionText
    }

    private fun setQuestionRecord(questionSrc: String) {
        if (questionSrc != "") {
            var playing = false
            var player: MediaPlayer? = null
            binding.resultQuestionRecordBtn.setOnClickListener {
                // 질문 녹음 재생 기능
                if (playing){
                    player?.release()
                    player = null

                    binding.resultQuestionRecordBtn.setBackgroundResource(R.drawable.playbtn5)
                    playing = false
                }
                // 재생 중이 아니면 중지 버튼으로 이미지 변경
                else{
                    // 녹음 소스 불러와서 미디어 플레이어 세팅
                    player = MediaPlayer().apply {
                        setDataSource(questionSrc)
                        prepare()
                    }

                    player?.setOnCompletionListener {
                        player?.release()
                        player = null

                        binding.resultQuestionRecordBtn.setBackgroundResource(R.drawable.playbtn5)
                        playing = false
                    }

                    // 재생
                    player?.start()

                    binding.resultQuestionRecordBtn.setBackgroundResource(R.drawable.stopbtn)
                    playing = true
                }
            }
        }
    }

    private fun setAnswerReply(reply: Boolean?) {
        if (reply != null)
            if (reply)
                binding.resultAnswerImg.setBackgroundResource(R.drawable.answer_positive)
            else
                binding.resultAnswerImg.setBackgroundResource(R.drawable.answer_negative)
        else
            binding.resultAnswerImg.visibility = View.GONE
    }

    private fun onRecord(answer: AnswerDTO): Boolean {
        return (answer.answerSrc != "")
    }

    private fun setAnswerRecord(onRecord: Boolean, answerSrc: String) {
        // 답변 녹음 재생 기능
        if (onRecord) {
            binding.resultAnswerRecordBtnTitle.visibility = View.VISIBLE
            binding.resultAnswerRecordLayout.visibility = View.VISIBLE
            var playing = false
            var player: MediaPlayer? = null
            binding.resultAnswerRecordBtn.setOnClickListener {
                // 질문 녹음 재생 기능
                if (playing){
                    player?.release()
                    player = null

                    binding.resultAnswerRecordBtn.setBackgroundResource(R.drawable.playbtn5)
                    playing = false
                }
                // 재생 중이 아니면 중지 버튼으로 이미지 변경
                else{
                    // 녹음 소스 불러와서 미디어 플레이어 세팅
                    player = MediaPlayer().apply {
                        setDataSource(answerSrc)
                        prepare()
                    }

                    player?.setOnCompletionListener {
                        player?.release()
                        player = null

                        binding.resultAnswerRecordBtn.setBackgroundResource(R.drawable.playbtn5)
                        playing = false
                    }

                    // 재생
                    player?.start()

                    binding.resultAnswerRecordBtn.setBackgroundResource(R.drawable.stopbtn)
                    playing = true
                }
            }
        }
        else {
            binding.resultAnswerRecordBtnTitle.visibility = View.GONE
            binding.resultAnswerRecordLayout.visibility = View.GONE
        }
    }

    private fun setQuestionOption(onRecord: Boolean, onSecret: Boolean) {
        binding.resultRecordOption.isChecked = onRecord
        binding.resultSecretOption.isChecked = onSecret
    }
}