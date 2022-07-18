package com.seoul42.relief_post_office.adapter

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.seoul42.relief_post_office.R
import com.seoul42.relief_post_office.model.SafetyDTO
import com.seoul42.relief_post_office.safety.EditSafetyActivity
import com.seoul42.relief_post_office.safety.EditWardSafetyActivity

class SafetyAdapter(private val context : Context, private val items : ArrayList<Pair<String, SafetyDTO>>)
	: RecyclerView.Adapter<SafetyAdapter.ViewHolder>() {

	override fun onCreateViewHolder(
		parent: ViewGroup,
		viewType: Int
	): SafetyAdapter.ViewHolder {
		val view = LayoutInflater.from(parent.context).inflate(R.layout.safety_fragment_rv_item, parent, false)

		return ViewHolder(view)
	}

	override fun onBindViewHolder(holder: SafetyAdapter.ViewHolder, position: Int) {
		holder.bindItems(items[position])
	}

	// 전체 리사이클러 뷰의 아이템 개수
	override fun getItemCount(): Int {
		return items.size
	}

	inner class ViewHolder(itemView : View) : RecyclerView.ViewHolder(itemView) {

		// 데이터 매핑 해주기
		fun bindItems(item : Pair<String, SafetyDTO>){
			// 안부 이름 매핑
			val rvText = itemView.findViewById<TextView>(R.id.safety_fragment_rv_item_text)
			rvText.text = item.second.name

			// 아이템 클릭 시 수정 액티비티로 넘어가기
			itemView.setOnClickListener{
				// 아이템 여러번 클릭 방지
				it.isClickable = false
				val tmpIntent = Intent(context, EditSafetyActivity::class.java)
				tmpIntent.putExtra("safetyId", item.first)
				ContextCompat.startActivity(context, tmpIntent, null)
				it.isClickable = true
			}
		}
	}

}