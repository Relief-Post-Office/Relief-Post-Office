package com.seoul42.relief_post_office.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.seoul42.relief_post_office.R
import com.seoul42.relief_post_office.databinding.ItemResultDetailBinding
import com.seoul42.relief_post_office.model.AnswerDTO

class ResultDetailAdapter (private val context : Context,
                           private val answerList: MutableList<Pair<String, AnswerDTO>>)
    : RecyclerView.Adapter<ResultDetailAdapter.ResultDetailHolder>() {
    inner class ResultDetailHolder(private val binding: ItemResultDetailBinding) : RecyclerView.ViewHolder(binding.root){
        @SuppressLint("NotifyDataSetChanged", "ResourceAsColor")
        fun setQuestionAnswer(answer: Pair<String, AnswerDTO>, context: Context) {
            setQuestionText(binding, answer.second)
            setAnswerRecord(binding, answer.second)
            setAnswerReply(binding, answer.second)
        }
    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ResultDetailHolder {
        val binding = ItemResultDetailBinding.inflate(
            LayoutInflater.from(parent.context),
            parent, false)
        return ResultDetailHolder(binding)
    }

    override fun onBindViewHolder(holder: ResultDetailHolder, position: Int) {
        val answer = answerList.get(position)
        holder.setQuestionAnswer(answer, context)
    }

    override fun getItemCount(): Int {
        return answerList.size
    }

    private fun setQuestionText(binding: ItemResultDetailBinding, answer: AnswerDTO) {
        binding.textResultQuetion.text = answer.questionText
    }

    private fun setAnswerRecord(binding: ItemResultDetailBinding, answer: AnswerDTO) {
        if (answer.questionRecord) {
            val recordBtn = binding.btnResultQuetionPlay
            recordBtn.visibility = View.VISIBLE
            recordBtn.setOnClickListener {
                // 녹음 재생
            }
        }
    }

    private fun setAnswerReply(binding: ItemResultDetailBinding, answer: AnswerDTO) {
        val replyImg = binding.imgResultAnswer
        if (answer.reply == true)
            replyImg.setBackgroundResource(R.drawable.answer_positive)
        else
            replyImg.setBackgroundResource(R.drawable.answer_negative)
    }

}