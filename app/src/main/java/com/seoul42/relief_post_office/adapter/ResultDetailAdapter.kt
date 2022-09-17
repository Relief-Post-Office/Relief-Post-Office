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
    inner class ResultDetailHolder(private val binding: ItemResultDetailBinding) :
        RecyclerView.ViewHolder(binding.root) {
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
            parent, false
        )
        return ResultDetailHolder(binding)
    }

    override fun onBindViewHolder(holder: ResultDetailHolder, position: Int) {
        val answer = answerList.get(position)
        holder.setQuestionAnswer(answer)
    }

    override fun getItemCount(): Int {
        return answerList.size
    }

    /**
     * 질문 내용을 표시하는 매서드
     */
    private fun setQuestionText(binding: ItemResultDetailBinding, answer: AnswerDTO) {
        binding.textResultQuetion.text = answer.questionText
    }

    /**
     * 답변을 표시하는 매서드
     */
    private fun setAnswerReply(binding: ItemResultDetailBinding, answer: AnswerDTO) {
        val replyImg = binding.imgResultAnswer
        if (answer.reply == true)
            // 긍정 답변
            replyImg.setBackgroundResource(R.drawable.answer_positive)
        else
            // 부정 답변
            replyImg.setBackgroundResource(R.drawable.answer_negative)
    }

    /**
     * 음성 답변 미디어를 설정하는 매서드
     *
     * - 음성 답변이 있을 경우에만 미디어와 재생버튼이 생성됩니다.
     */
    private fun setAnswerRecord(binding: ItemResultDetailBinding, answer: AnswerDTO) {
        if (answer.answerSrc != "") {
            // 재생 버튼 활성화
            val recordBtn = binding.btnResultQuetionPlay
            recordBtn.visibility = View.VISIBLE
            // 재생 버튼에 미디어 설정 객체 생성
            var playerBtn = PlayerButton(recordBtn, answer.answerSrc)
        }
    }

    /**
     * 입력 받은 버튼에 녹음 파일에 해당하는 미디어를 연결하고 클릭 이벤트를 설정하는 클래스
     *
     * - player : 미디어 객체
     * - isPlaying : 미디어를 재생 중인지 아닌지
     * - recordBtn : 미디어 재생 버튼
     * - recordSrc : 미디어 소스 위치
     */
    class PlayerButton(btn: Button, src: String) {
        var player: MediaPlayer?
        var isPlaying: Boolean
        var recordBtn: Button = btn
        var recordSrc: String = src

        init {
            // 미디어 초기화 상태 설정
            player = null
            isPlaying = false
            // 미디어 소스의 음성 파일로 미디어 객체 생성
            setPlayer()
            // 미디어 재생 버튼의 클릭 이벤트 설정
            setRecordBtnListener()
        }

        /**
         * 미디어 재생 버튼의 클릭 이벤트 설정 매서드
         */
        private fun setRecordBtnListener() {
            recordBtn.setOnClickListener {
                click()
            }
        }

        /**
         * 미디어 재생 버튼을 클릭 했을 때 처리 하는 매서드
         *
         * - 재생 중 : 미디어를 초기화 시켜 멈추고 미디어 객체를 다시 연결합니다.
         * - 멈춤 : 미디어를 객체를 생성하고 재생합니다.
         */
        private fun click() {
            // 재생 중일 때
            if (isPlaying) {
                //player 반납
                resetPlayer()
            } else {
                // 녹음 소스 불러와서 미디어 플레이어 세팅
                setPlayer()

                // 재생
                player?.start()
                // 버튼 이미지 변경
                recordBtn.setBackgroundResource(R.drawable.stopbtn)
                isPlaying = true
            }
        }

        /**
         * 미디어 객체 생성과 미디어 재생이 끝나면 미디어를 초기화 해주는 리스너 등록 하는 매서드
         *
         * - recordSrc 의 음성 녹음으로 미디어 객체를 생성합니다.
         */
        private fun setPlayer() {
            player = MediaPlayer().apply {
                setDataSource(recordSrc)
                prepare()
            }
            // 재생이 끝나면 player 초기화해주는 리스너 등록
            player?.setOnCompletionListener {
                resetPlayer()
            }
        }

        /**
         * 미디어를 초기화 시켜주는 매서드
         */
        private fun resetPlayer() {
            player?.release()
            // 미디어 재생 버튼 모양 변경
            recordBtn.setBackgroundResource(R.drawable.playbtn5)
            // 미디어 멈춤 상태 설정
            player = null
            isPlaying = false
        }
    }
}