package com.seoul42.relief_post_office.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.seoul42.relief_post_office.R
import com.seoul42.relief_post_office.model.RequestDTO
import java.text.SimpleDateFormat

class ResponseAdapter(private val context: Context, private val dataList: ArrayList<RequestDTO>) :
    RecyclerView.Adapter<ResponseAdapter.ItemViewHolder>() {

    private val checkList = ArrayList<String>()

    inner class ItemViewHolder(itemView : View) : RecyclerView.ViewHolder(itemView) {
        private val userPhoto = itemView.findViewById<ImageView>(R.id.item_guardian_img)
        private val userText = itemView.findViewById<TextView>(R.id.item_guardian_text)
        private val userCheck = itemView.findViewById<CheckBox>(R.id.item_guardian_check)

        fun bind(user : RequestDTO, context : Context) {
            val curYear = SimpleDateFormat("yyyy-MM-dd hh:mm")
                .format(System.currentTimeMillis())
                .split("-")[0].toInt()
            val userYear = user.birth!!.split("/")[0].toInt()
            val userAge = curYear - userYear + 1
            val userName = user.name

            Glide.with(context)
                .load(user.photoUri)
                .circleCrop()
                .into(userPhoto)
            userText.text = "$userName\n$userAge"
            userCheck.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) checkList.add(user.userId.toString())
                else checkList.remove(user.userId.toString())
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_guardian, parent, false)
        return ItemViewHolder(view)
    }

    override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
        holder.bind(dataList[position], context)
    }

    override fun getItemCount(): Int {
        return dataList.size
    }

    fun getCheckList() : ArrayList<String> {
        return checkList
    }
}