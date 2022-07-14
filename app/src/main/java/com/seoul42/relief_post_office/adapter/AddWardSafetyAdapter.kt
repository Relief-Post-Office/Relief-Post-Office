package com.seoul42.relief_post_office.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.seoul42.relief_post_office.R
import com.seoul42.relief_post_office.model.QuestionDTO
import com.seoul42.relief_post_office.model.SafetyDTO

class AddWardSafetyAdapter(private val items: ArrayList<Pair<String, QuestionDTO>>)
    : RecyclerView.Adapter<AddWardSafetyAdapter.ViewHolder>(){
    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): AddWardSafetyAdapter.ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.question_rv_item, parent, false)

        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: AddWardSafetyAdapter.ViewHolder, position: Int) {
        holder.bindItems(items[position])
    }

    override fun getItemCount(): Int {
        return items.size
    }

    inner class ViewHolder(itemView : View) : RecyclerView.ViewHolder(itemView) {

        // 각 아이템마다 뷰 처리
        fun bindItems(item: Pair<String, QuestionDTO>){
            // 각 질문 별 세팅
            // text 세팅
            val rvText = itemView.findViewById<TextView>(R.id.question_rv_item_text)
            rvText.text = item.second.text

            // 녹음 소스 매핑 필요
        }
    }

}