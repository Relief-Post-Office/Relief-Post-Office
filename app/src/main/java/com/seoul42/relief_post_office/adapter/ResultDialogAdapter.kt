package com.seoul42.relief_post_office.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.seoul42.relief_post_office.R
import com.seoul42.relief_post_office.databinding.ItemResultDialogBinding
import com.seoul42.relief_post_office.model.ResultDTO

class ResultDialogAdapter(private val resultList: MutableList<Pair<String, ResultDTO>>)
    : RecyclerView.Adapter<ResultDialogAdapter.ResultHolder>() {
    inner class ResultHolder(private val binding: ItemResultDialogBinding) : RecyclerView.ViewHolder(binding.root){
        fun setResult(result: Pair<String, ResultDTO>) {
            setSafetyName(binding, result.second.safetyName)
            setIsAnswer(binding, result.second.responseTime)
        }
    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ResultHolder {
        val binding = ItemResultDialogBinding.inflate(LayoutInflater.from(parent.context),
            parent, false)
        return ResultHolder(binding)
    }

    override fun onBindViewHolder(holder: ResultHolder, position: Int) {
        val result = resultList.get(position)
        holder.setResult(result)
    }

    override fun getItemCount(): Int {
        return resultList.size
    }

    private fun setSafetyName(binding: ItemResultDialogBinding, safetyName: String) {
        binding.resultSafetyNameText.text = safetyName
    }

    private fun setIsAnswer(binding: ItemResultDialogBinding, responseTime: String) {
        if (isResponsed(responseTime))
        {
            binding.resultDialogIsAnswerText.text = "응답"
            binding.resultDialogIsAnswerText.setTextColor(R.color.green)
        }
        else {
            binding.resultDialogIsAnswerText.text = "미응답"
            binding.resultDialogIsAnswerText.setTextColor(R.color.red)
        }
    }

    private fun isResponsed(responseTime: String): Boolean {
        return responseTime != "미응답"
    }
}