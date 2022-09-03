package com.seoul42.relief_post_office.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat.startActivity
import androidx.recyclerview.widget.RecyclerView
import com.seoul42.relief_post_office.R
import com.seoul42.relief_post_office.databinding.ItemResultDetailBinding
import com.seoul42.relief_post_office.model.AnswerDTO
import com.seoul42.relief_post_office.result.ResultQuestionDetailActivity

/**
 * 해당 결과의 답변들을 리사이클뷰에서 보여주기 위해 아이템에 값을 넣어줄 어뎁터 클래스
 *
 * - 결과 id에 해당하는 모든 답변을 아이템으로 표시합니다.
 * - 단, 비밀 옵션이 켜져 있을 경우 질문의 생성자가 아니면 아이템으로 표시 하지 않습니다.
 * - 표시할 내용 : 질문 내용, 답변, 음성 답변 녹음 미디어 버튼
 * - 음성 답변 녹음 미디어 버튼 클릭 시 : 음성 답변 미디어가 재생되거나 초기화 됩니다.
 * - 답변 아이템 클릭 시 : 답변 상세 페이지로 넘어 갑니다.
 */
class ResultDetailAdapter (private val context : Context,
                           private val answerList: MutableList<Pair<String, AnswerDTO>>,
                           private val safetyName: String,
                           private val answerDate: String)
    : RecyclerView.Adapter<ResultDetailAdapter.ResultDetailHolder>() {
    inner class ResultDetailHolder(private val binding: ItemResultDetailBinding) : RecyclerView.ViewHolder(binding.root){
        @SuppressLint("NotifyDataSetChanged", "ResourceAsColor")
        fun setQuestionAnswer(answer: Pair<String, AnswerDTO>) {
            // 질문 내용 표시
            setQuestionText(binding, answer.second)
            // 음성 답변 미디어 설정
            setAnswerRecord(binding, answer.second)
            // 답변 표시
            setAnswerReply(binding, answer.second)
            // 답변 아이템 클릭시 답변 상세 페이지로 넘김
            binding.textResultQuetion.setOnClickListener { textResultQuestionView ->
                // 여러번 클릭 방지
                // 답변 아이템 클릭 불가능
                textResultQuestionView.isClickable = false
                val intent = Intent(context, ResultQuestionDetailActivity::class.java)
                intent.putExtra("safetyName", safetyName)
                intent.putExtra("answer", answer)
                intent.putExtra("answerDate", answerDate)
                startActivity(context, intent, null)
                // 답변 아이템 클릭 가능
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
            var playing = false
            var player: MediaPlayer? = null
            recordBtn.setOnClickListener {
                // 질문 녹음 재생 기능
                if (playing){
                    player?.release()
                    player = null

                    recordBtn.setBackgroundResource(R.drawable.playbtn5)
                    playing = false
                }
                // 재생 중이 아니면 중지 버튼으로 이미지 변경
                else{
                    // 녹음 소스 불러와서 미디어 플레이어 세팅
                    player = MediaPlayer().apply {
                        setDataSource(answer.answerSrc)
                        prepare()
                    }

                    player?.setOnCompletionListener {
                        player?.release()
                        player = null

                        recordBtn.setBackgroundResource(R.drawable.playbtn5)
                        playing = false
                    }

                    // 재생
                    player?.start()

                    recordBtn.setBackgroundResource(R.drawable.stopbtn)
                    playing = true
                }
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