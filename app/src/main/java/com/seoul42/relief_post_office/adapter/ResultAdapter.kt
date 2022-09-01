package com.seoul42.relief_post_office.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.seoul42.relief_post_office.R
import com.seoul42.relief_post_office.databinding.ItemResultBinding
import com.seoul42.relief_post_office.model.ResultDTO
import com.seoul42.relief_post_office.result.ResultDetailActivity
import java.text.SimpleDateFormat

class ResultAdapter(private val context : Context,
                    private val resultList: MutableList<Pair<String, ResultDTO>>,
                    private val wardId: String)
    : RecyclerView.Adapter<ResultAdapter.ResultHolder>() {
    inner class ResultHolder(private val binding: ItemResultBinding) : RecyclerView.ViewHolder(binding.root){
        @SuppressLint("NotifyDataSetChanged", "ResourceAsColor")
        fun setResult(result: Pair<String, ResultDTO>) {
            binding.itemResultSafetyName.text = result.second.safetyName
            binding.itemResultAlarmTime.text = result.second.safetyTime.replace(":", " : ")

            if (!isResponsed(result.second.responseTime)) {
                binding.itemResultResponseTime.text = result.second.responseTime
                binding.itemResultSafetyLayout.setBackgroundResource(R.drawable.result_disable_background)
                binding.itemResultResponseTime.setTextColor(R.color.red)
                binding.itemResultSafetyLayout.setOnClickListener {
                    Toast.makeText(context, "미응답 결과입니다.", Toast.LENGTH_SHORT).show()
                }
            }
            else {
                binding.itemResultSafetyLayout.setBackgroundResource(R.drawable.result_enable_background)
                binding.itemResultResponseTime.text = millisToTimeString(calTimeDiff(result))
                binding.itemResultSafetyLayout.setOnClickListener {
                    // 여러번 클릭 방지
                    it.isClickable = false
                    val intent = Intent(context, ResultDetailActivity::class.java)
                    intent.putExtra("wardId", wardId)
                    intent.putExtra("resultId", result.first)
                    intent.putExtra("result", result.second)
                    ContextCompat.startActivity(context, intent, null)
                    it.isClickable = true
                }
            }
        }
    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ResultHolder {
        val binding = ItemResultBinding.inflate(LayoutInflater.from(parent.context),
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

    private fun calTimeDiff(result: Pair<String, ResultDTO>): Long {
        val startTime = result.second.date + " " + result.second.safetyTime + ":00"
        val endTime = result.second.responseTime
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
        return dateFormat.parse(endTime).time - dateFormat.parse(startTime).time
    }

    private fun millisToTimeString(millisTime: Long): String{
        val timeSec = millisTime / 1000
        val hour = timeSec / 3600
        val minute = (timeSec % 3600) / 60
        val second = timeSec % 60
        return "${hour}시간 ${minute}분 ${second}초"
    }

    private fun isResponsed(responseTime: String): Boolean {
        return responseTime != "미응답"
    }
}