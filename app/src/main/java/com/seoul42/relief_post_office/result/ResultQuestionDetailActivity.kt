package com.seoul42.relief_post_office.result

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
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
        setAnswerRecord(answer.second.questionRecord, answer.second.answerSrc)
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
            binding.resultQuestionRecordBtn.setOnClickListener {
                // 질문 녹음 재생 기능
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

    private fun setAnswerRecord(onRecord: Boolean, answerSrc: String) {
        // 답변 녹음 재생 기능
        if (onRecord) {
            binding.resultAnswerRecordBtnTitle.visibility = View.VISIBLE
            binding.resultAnswerRecordLayout.visibility = View.VISIBLE
            binding.resultAnswerRecordBtn.setOnClickListener {
                // 질문 녹음 재생 기능
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