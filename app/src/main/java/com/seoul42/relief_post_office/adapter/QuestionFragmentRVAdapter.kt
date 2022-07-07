package com.seoul42.relief_post_office.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.seoul42.relief_post_office.R
import com.seoul42.relief_post_office.model.QuestionDTO

class QuestionFragmentRVAdapter(private val context : Context, private val items : ArrayList<QuestionDTO>)
    : RecyclerView.Adapter<QuestionFragmentRVAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int)
    : QuestionFragmentRVAdapter.ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.question_rv_item, parent, false)

        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: QuestionFragmentRVAdapter.ViewHolder, position: Int) {
        holder.bindItems(items[position])
    }

    // 전체 리사이클러 뷰의 아이템 개수
    override fun getItemCount(): Int {
        return items.size
    }

    inner class ViewHolder(itemView : View) : RecyclerView.ViewHolder(itemView){

        // 데이터 매핑 해주기
        fun bindItems(item : QuestionDTO){
            // 각 질문 별 텍스트
            val rvText = itemView.findViewById<TextView>(R.id.question_rv_item_text)

            // text 매핑
            if (item.body!!.text != null){
                rvText.text = item.body!!.text
            }
            // 녹음 소스 매핑 필요
        }
    }
}