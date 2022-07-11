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

class SafetyAdapter(private val context : Context, private val dataList : ArrayList<SafetyDTO>) : RecyclerView.Adapter<SafetyAdapter.ItemViewHolder>() {
	class ItemViewHolder(itemView : View) : RecyclerView.ViewHolder(itemView) {
		private val text = itemView.findViewById<TextView>(R.id.safety_rv_item)

		fun bind(data : SafetyDTO, context: Context) {
			//text.text = data.data!!.content
			text.setOnClickListener {
				val intent = Intent(context, EditSafetyActivity::class.java)

				intent.putExtra("safetyDTO", data)

				ContextCompat.startActivity(context, intent, null)
			}
		}
	}

	override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
		val view = LayoutInflater.from(context).inflate(R.layout.item_safety, parent,false)
		return ItemViewHolder(view)
	}

	override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
		holder.bind(dataList[position], context)
	}

	override fun getItemCount(): Int {
		return dataList.size
	}
}