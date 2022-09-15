package com.seoul42.relief_post_office.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.seoul42.relief_post_office.R
import com.seoul42.relief_post_office.databinding.ItemResultDialogBinding
import com.seoul42.relief_post_office.model.ResultDTO

/**
 * 당일 안부 응답여부를 에서 보여주기 위해 아이템에 값을 넣어줄 어뎁터 클래스
 *
 * - 표시 내용 : 당일 안부 이름, 당일 안부 응답 결과
 */
class ResultDialogAdapter(private val resultList: MutableList<Pair<String, ResultDTO>>)
    : RecyclerView.Adapter<ResultDialogAdapter.ResultHolder>() {
    inner class ResultHolder(private val binding: ItemResultDialogBinding) : RecyclerView.ViewHolder(binding.root){
        fun setResult(result: Pair<String, ResultDTO>) {
            // 당일 안부 이름 설정
            setSafetyName(binding, result)
            // 당일 안부 응답 결과
            setIsAnswer(binding, result)
        }
    }

    /**
     * 리사이클 뷰를 연결하는 매서드
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ResultHolder {
        val binding = ItemResultDialogBinding.inflate(LayoutInflater.from(parent.context),
            parent, false)
        return ResultHolder(binding)
    }

    /**
     * 순서에 해당하는 아이템에 값을 설정하는 매서드
     */
    override fun onBindViewHolder(holder: ResultHolder, position: Int) {
        val result = resultList.get(position)
        holder.setResult(result)
    }

    /**
     * 아이템 뷰의 개수
     */
    override fun getItemCount(): Int {
        return resultList.size
    }

    /**
     * 아이템에 안부이름을 설정하는 매서드
     */
    private fun setSafetyName(binding: ItemResultDialogBinding, result: Pair<String, ResultDTO>) {
        binding.resultSafetyNameText.text = result.second.safetyName
    }

    /**
     * 아이템에 응답 여부를 설정하는 매서드
     *
     * <표시 내용>
     * - 응답 : 응답 종료 시간이 있을 때
     * - 미응답 : 응답 종료 시간이 없을 때
     */
    private fun setIsAnswer(binding: ItemResultDialogBinding, result : Pair<String, ResultDTO>) {
        if (isResponsed(result.second.responseTime))
        {
            binding.resultDialogIsAnswerText.text = "응답"
            binding.resultDialogIsAnswerText.setTextColor(R.color.green)
        }
        else {
            binding.resultDialogIsAnswerText.text = "미응답"
            binding.resultDialogIsAnswerText.setTextColor(R.color.red)
        }
    }

    /**
     * 응답 여부를 판단하는 매서드
     *
     * 데이터베이스의 결과 콜렉션의 responseTime 필드의 도메인과 관련이 있습니다.
     */
    private fun isResponsed(responseTime: String): Boolean {
        return responseTime != "미응답"
    }
}