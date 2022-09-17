package com.seoul42.relief_post_office.result

import android.media.MediaPlayer
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import com.seoul42.relief_post_office.R
import com.seoul42.relief_post_office.databinding.ActivityResultQuestionDetailBinding
import com.seoul42.relief_post_office.model.AnswerDTO

/**
 * 결과 페이지의 세번째 화면을 동작시키는 액티비티 클래스
 *
 *  주요 UI : 결과의 질문들의 상세 내용 ( 질문 텍스트, 질문 음성, 답변, 녹음 답변 )
 */
class ResultQuestionDetailActivity : AppCompatActivity() {
    // 레이아웃 연결
    private val binding by lazy { ActivityResultQuestionDetailBinding.inflate(layoutInflater) }

    /**
     * 액티비티가 생성되었을 때 호출 되는 매서드
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 안부 이름
        val safetyName = intent.getSerializableExtra("safetyName") as String
        // 응답
        val answer = intent.getSerializableExtra("answer") as Pair<String, AnswerDTO>
        // 응답 날짜
        val answerDate = intent.getSerializableExtra("answerDate") as String
        // 전체 레이아웃 표시
        setContentView(binding.root)
        // 안부 이름 설정
        setSafetyName(safetyName)
        // 응답 날짜 설정
        setDate(answerDate)
        // 질문 텍스트 설정
        setQuestionText(answer.second.questionText)
        // 질문 녹음 설정
        setQuestionRecord(answer.second.questionSrc)
        // 응답 결과 설정
        setAnswerReply(answer.second.reply)
        // 녹음 응답 설정
        setAnswerRecord(answer.second, answer.second.answerSrc)
        // 질문의 옵션 설정
        setQuestionOption(answer.second.questionRecord, answer.second.questionSecret)
    }

    /**
     * 안부의 이름을 표시하는 매서드
     */
    private fun setSafetyName(safetyName: String) {
        binding.textResultSafetyName.text = safetyName
    }

    /**
     * 결과의 날짜를 표시하는 매서드
     */
    private fun setDate(date: String) {
        binding.resultQuestionDetailDate.text = date.replace("-", "/")
    }

    /**
     * 질문의 텍스트를 표시하는 매서드
     */
    private fun setQuestionText(questionText: String) {
        binding.resultQuestionText.text = questionText
    }

    /**
     * 질문의 녹음 미디어를 셋팅하는 매서드
     */
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

    /**
     * 응답의 결과를 표시하는 매서드
     *
     * 응답의 결과에는 O와 X가 있습니다. 이를 이미지로 보여줍니다.
     */
    private fun setAnswerReply(reply: Boolean?) {
        if (reply != null)
            if (reply)
                // 긍정 답변
                binding.resultAnswerImg.setBackgroundResource(R.drawable.answer_positive)
            else
                // 부정 답변
                binding.resultAnswerImg.setBackgroundResource(R.drawable.answer_negative)
        else
            // 답변이 없다면 관련된 뷰를 표시하지 않음
            binding.resultAnswerImg.visibility = View.GONE
    }

    /**
     * 녹음 응답의 미디어를 셋팅하는 매서드
     *
     * 응답는 질문의 종류와 마찬가지로 녹음요청질문에 해당하는 녹음 응답이 있습니다.
     * 녹음 응답이 있을 경우 미디어를 생성합니다.
     */
    private fun setAnswerRecord(answer: AnswerDTO, answerSrc: String) {
        // 답변 녹음 재생 기능
        if (onRecord(answer)) {
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
            // 음성 답변이 없다면 관련된 뷰를 표시하지 않음
            binding.resultAnswerRecordBtnTitle.visibility = View.GONE
            binding.resultAnswerRecordLayout.visibility = View.GONE
        }
    }

    /**
     * 응답의 질문이 녹음요청 질문인지 확인하는 매서드
     */
    private fun onRecord(answer: AnswerDTO): Boolean {
        return (answer.answerSrc != "")
    }

    /**
     * 질문의 옵션을 표시하는 매서드
     */
    private fun setQuestionOption(onRecord: Boolean, onSecret: Boolean) {
        binding.resultRecordOption.isChecked = onRecord
        binding.resultSecretOption.isChecked = onSecret
    }
}