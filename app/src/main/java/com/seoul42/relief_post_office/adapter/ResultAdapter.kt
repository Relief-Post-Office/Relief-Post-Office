package com.seoul42.relief_post_office.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.seoul42.relief_post_office.R
import com.seoul42.relief_post_office.databinding.ItemResultBinding
import com.seoul42.relief_post_office.model.ResultDTO
import com.seoul42.relief_post_office.result.ResultDetailActivity
import java.text.SimpleDateFormat
import java.util.*

class ResultAdapter(private val context : Context,
                    private val resultList: MutableList<Pair<String, ResultDTO>>,
                    private val wardId: String)
    : RecyclerView.Adapter<ResultAdapter.ResultHolder>() {
    inner class ResultHolder(private val binding: ItemResultBinding) : RecyclerView.ViewHolder(binding.root){
        @SuppressLint("NotifyDataSetChanged", "ResourceAsColor")
        fun setResult(result: Pair<String, ResultDTO>, context: Context) {
            binding.itemResultSafetyName.text = result.second.safetyName
            binding.itemResultAlarmTime.text = result.second.safetyTime.replace(":", " : ")
            if (!isResponsed(result.second.responseTime)) {
                binding.itemResultResponseTime.text = result.second.responseTime
                binding.itemResultSafetyLayout.setBackgroundResource(R.drawable.result_disable_background)
                binding.itemResultResponseTime.setTextColor(R.color.red)
            }
            else {
                binding.itemResultSafetyLayout.setBackgroundResource(R.drawable.result_enable_background)
                val startTime = result.second.date + " " + result.second.safetyTime + ":00"
                val endTime = result.second.responseTime
                var dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
                val time = dateFormat.parse(endTime).time - dateFormat.parse(startTime).time

                binding.itemResultResponseTime.text = millisToTimeString(time)
                binding.itemResultSafetyLayout.setOnClickListener {
                    val intent = Intent(context, ResultDetailActivity::class.java)
                    intent.putExtra("wardId", wardId)
                    intent.putExtra("resultId", result.first)
                    intent.putExtra("result", result.second)
                    ContextCompat.startActivity(context, intent, null)
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
        holder.setResult(result, context)
    }

    override fun getItemCount(): Int {
        return resultList.size
    }

    private fun millisToTimeString(millisTime: Long): String{
        val timeSec = millisTime / 1000
        val hour = timeSec / 360
        val minute = (timeSec % 360) / 60
        val second = (timeSec % 3600) % 60
        return "${hour}시간 ${minute}분 ${second}초"
    }

    private fun isResponsed(responseTime: String): Boolean {
        return responseTime != "미응답"
    }
}