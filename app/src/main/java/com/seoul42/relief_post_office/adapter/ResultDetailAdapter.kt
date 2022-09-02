package com.seoul42.relief_post_office.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.core.content.ContextCompat.startActivity
import androidx.recyclerview.widget.RecyclerView
import com.seoul42.relief_post_office.R
import com.seoul42.relief_post_office.databinding.ItemResultDetailBinding
import com.seoul42.relief_post_office.model.AnswerDTO
import com.seoul42.relief_post_office.result.ResultQuestionDetailActivity

class ResultDetailAdapter (private val context : Context,
                           private val answerList: MutableList<Pair<String, AnswerDTO>>,
                           private val safetyName: String,
                           private val answerDate: String)
    : RecyclerView.Adapter<ResultDetailAdapter.ResultDetailHolder>() {
    inner class ResultDetailHolder(private val binding: ItemResultDetailBinding) : RecyclerView.ViewHolder(binding.root){
        @SuppressLint("NotifyDataSetChanged", "ResourceAsColor")
        fun setQuestionAnswer(answer: Pair<String, AnswerDTO>) {
            setQuestionText(binding, answer.second)
            setAnswerRecord(binding, answer.second)
            setAnswerReply(binding, answer.second)
            binding.textResultQuetion.setOnClickListener { textResultQuestionView ->
                // 여러번 클릭 방지
                textResultQuestionView.isClickable = false
                val intent = Intent(context, ResultQuestionDetailActivity::class.java)
                intent.putExtra("safetyName", safetyName)
                intent.putExtra("answer", answer)
                intent.putExtra("answerDate", answerDate)
                startActivity(context, intent, null)
                textResultQuestionView.isClickable = true
            }
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
        holder.setQuestionAnswer(answer)
    }

    override fun getItemCount(): Int {
        return answerList.size
    }

    private fun setQuestionText(binding: ItemResultDetailBinding, answer: AnswerDTO) {
        binding.textResultQuetion.text = answer.questionText
    }

    private fun setAnswerRecord(binding: ItemResultDetailBinding, answer: AnswerDTO) {
        if (answer.answerSrc != "") {
            val recordBtn = binding.btnResultQuetionPlay
            recordBtn.visibility = View.VISIBLE
            setRecordPlayer(recordBtn, answer.answerSrc)
        }
    }

    class PlayerState {
        var player: MediaPlayer? = null
        var isPlaying: Boolean = false
    }

    private fun setRecordPlayer(recordBtn: Button, recordSrc: String) {
        val playerState = PlayerState()
        recordBtn.setOnClickListener {
            // 질문 녹음 재생 기능
            setRecordPlayerListener(recordBtn, recordSrc, playerState)
        }
    }

    private fun setRecordPlayerListener(
        recordBtn: Button,
        recordSrc: String,
        playerState: PlayerState,
    ) {
        // 재생 중일 때
        if (playerState.isPlaying){
            //player 반납
            resetPlayer(playerState, recordBtn)
        } else {
            // 재생 중이 아니면 중지 버튼으로 이미지 변경
            // 재생 중이 아닐때 맨 처음
            // 녹음 소스 불러와서 미디어 플레이어 세팅
            setPlayer(playerState, recordSrc)

            // 재생이 끝나면 player 초기화해주는 리스너 등록
            setPlayerListener(playerState, recordBtn)

            // 재생
            playerState.player?.start()
            recordBtn.setBackgroundResource(R.drawable.stopbtn)
            playerState.isPlaying = true
        }
    }

    private fun setPlayer(playerState: PlayerState, recordSrc: String) {
        playerState.player = MediaPlayer().apply {
            setDataSource(recordSrc)
            prepare()
        }
    }

    private fun setPlayerListener(playerState: PlayerState, recordBtn: Button) {
        playerState.player?.setOnCompletionListener {
            resetPlayer(playerState, recordBtn)
        }
    }

    private fun resetPlayer(playerState: PlayerState, recordBtn: Button) {
        playerState.player?.release()
        recordBtn.setBackgroundResource(R.drawable.playbtn5)
        playerState.player = null
        playerState.isPlaying = false
    }


    private fun setAnswerReply(binding: ItemResultDetailBinding, answer: AnswerDTO) {
        val replyImg = binding.imgResultAnswer
        if (answer.reply == true)
            replyImg.setBackgroundResource(R.drawable.answer_positive)
        else
            replyImg.setBackgroundResource(R.drawable.answer_negative)
    }

}