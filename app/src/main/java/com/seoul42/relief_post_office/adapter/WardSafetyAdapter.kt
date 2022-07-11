package com.seoul42.relief_post_office.adapter


import android.os.Build
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.recyclerview.widget.RecyclerView
import com.seoul42.relief_post_office.R
import com.seoul42.relief_post_office.model.SafetyDTO


class WardSafetyAdapter(private val items: ArrayList<Pair<String, SafetyDTO>>)
    : RecyclerView.Adapter<WardSafetyAdapter.ViewHolder>() {

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): WardSafetyAdapter.ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.ward_safety_item, parent, false)

        return ViewHolder(view)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onBindViewHolder(holder: WardSafetyAdapter.ViewHolder, position: Int) {
        holder.bindItems(items[position])
    }

    override fun getItemCount(): Int {
        return items.size
    }

    inner class ViewHolder(itemView : View) : RecyclerView.ViewHolder(itemView) {

        // 각 아이템마다 뷰 처리
        fun bindItems(item: Pair<String, SafetyDTO>){
            // 각 안부별 이름, 요일, 시간 세팅
            val rvName = itemView.findViewById<TextView>(R.id.ward_safety_item_name)
            val rvDayOfWeek = itemView.findViewById<TextView>(R.id.ward_safety_item_dayOfWeek)
            val rvTime = itemView.findViewById<TextView>(R.id.ward_safety_item_time)

            var itemDayOfWeek = ""
            var count = 0
            for (d in arrayListOf<String>("월", "화", "수", "목", "금", "토", "일",)){
                if (item.second.dayOfWeek[d]!!){
                    itemDayOfWeek += "${d} "
                    count++
                }
            }
            if (count == 7)
                itemDayOfWeek = "매일 "
            else if (count == 0){
                itemDayOfWeek = "비활성화 "
            }
            itemDayOfWeek = itemDayOfWeek.substring(0 until itemDayOfWeek.length - 1)

            rvName.text = item.second.name
            rvDayOfWeek.text = itemDayOfWeek
            rvTime.text = item.second.time

        }
    }
}