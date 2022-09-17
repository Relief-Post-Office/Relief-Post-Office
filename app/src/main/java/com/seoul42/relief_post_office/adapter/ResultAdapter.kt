package com.seoul42.relief_post_office.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
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
/**
 * 해당 날짜의 결과를 리사이클뷰에서 보여주기 위해 아이템에 값을 넣어줄 어뎁터 클래스
 *
 * - resultList : 연결된 노드는 모두 아이템으로 표시됩니다.
 * - 표시할 내용 : 안부 이름, 안부 시작 시각, 응답 여부 또는 응답 하는 시간, 클릭 가능
 * - 아이템 클릭 시 : 결과 상세 페이지로 넘어값니다. 미응답시 클릭되지 않습니다.
*/
class ResultAdapter(private val context : Context,
                    private val resultList: MutableList<Pair<String, ResultDTO>>,
                    private val wardId: String)
    : RecyclerView.Adapter<ResultAdapter.ResultHolder>() {
    inner class ResultHolder(private val binding: ItemResultBinding) : RecyclerView.ViewHolder(binding.root){
        @SuppressLint("NotifyDataSetChanged", "ResourceAsColor")
        fun setResult(result: Pair<String, ResultDTO>) {
            // 안부 이름 표시
            binding.itemResultSafetyName.text = result.second.safetyName
            // 안부 시작 시간 표시
            binding.itemResultAlarmTime.text = result.second.safetyTime.replace(":", " : ")
            // 응답 여부에 따라 응답 또는 응답 하는 시간, 클릭 가능, 클릭 리스너 표시 및 설정
            if (!isResponsed(result.second.responseTime)) {
                setUnClickableResult(binding, result)
            } else {
                setClickableResult(binding, result)
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

    /**
     * 안부에 응답을 했을 때 작동 하는 매서드
     *
     * 표시될 내용 : 응답 여부, 클릭 가능 여부(색깔)
     * 아이템이 클릭 할 수 없습니다.
     */
    private fun setUnClickableResult(binding: ItemResultBinding, result: Pair<String, ResultDTO>) {
        // 미응답 텍스트 표시
        binding.itemResultResponseTime.text = result.second.responseTime
        binding.itemResultResponseTime.setTextColor(R.color.red)
        // 클릭 할 수 없는 색깔 표시
        binding.itemResultSafetyLayout.setBackgroundResource(R.drawable.result_disable_background)
        // 클릭 하면 간단한 토스트 메시지가 나오며 아무 일도 일어나지 않음
        binding.itemResultSafetyLayout.setOnClickListener {
            Toast.makeText(context, "미응답 결과입니다.", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * 안부에 응답하지 않았을 작동 하는 매서드
     *
     * 표시될 내용 : 응답 시간, 클릭 가능 여부(색깔)
     * 아이템을 클릭 하면 결과 상세 페이지로 넘어갑니다.
     */
    private fun setClickableResult(binding: ItemResultBinding, result: Pair<String, ResultDTO>) {
        // 응답 하는 시간 표시
        binding.itemResultResponseTime.text = millisToTimeString(calTimeDiff(result))
        // 클릭 할 수 있는 색깔 표시
        binding.itemResultSafetyLayout.setBackgroundResource(R.drawable.result_enable_background)
        // 클릭 시 결과 상세 페이지로 넘어감
        binding.itemResultSafetyLayout.setOnClickListener { itemResultSafetyView ->
            // 여러번 클릭 방지
            // 아이템 클릭 불가능
            itemResultSafetyView.isClickable = false
            val intent = Intent(context, ResultDetailActivity::class.java)
            intent.putExtra("wardId", wardId)
            intent.putExtra("resultId", result.first)
            intent.putExtra("result", result.second)
            ContextCompat.startActivity(context, intent, null)
            // 아이템 클릭 가능
            itemResultSafetyView.isClickable = true
        }
    }

    /**
     * 응답 하는 시간 구하는 매서드
     *
     * - 응답 하는 시간(ms) = 결과 종료 시각 - 안부 설정 시작 시각
     */
    private fun calTimeDiff(result: Pair<String, ResultDTO>): Long {
        val startTime = result.second.date + " " + result.second.safetyTime + ":00"
        val endTime = result.second.responseTime
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
        return dateFormat.parse(endTime).time - dateFormat.parse(startTime).time
    }

    /**
     * 밀리 세컨드를 시간, 분, 초로 바꾸는 메서드
     */
    private fun millisToTimeString(millisTime: Long): String{
        val timeSec = millisTime / 1000
        val hour = timeSec / 3600
        val minute = (timeSec % 3600) / 60
        val second = timeSec % 60
        return "${hour}시간 ${minute}분 ${second}초"
    }

    /**
     * 응답을 했는 지 안했는지 확인하는 메서드드
    */
    private fun isResponsed(responseTime: String): Boolean {
        return responseTime != "미응답"
    }
}