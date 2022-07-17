package com.seoul42.relief_post_office.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.*
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.seoul42.relief_post_office.R
import com.seoul42.relief_post_office.databinding.ItemGuardianBinding
import com.seoul42.relief_post_office.model.UserDTO
import java.text.SimpleDateFormat

class ResponseAdapter(private val context: Context, private val dataList: ArrayList<Pair<String, UserDTO>>) :
    RecyclerView.Adapter<ResponseAdapter.ItemViewHolder>() {

    private val checkList = ArrayList<String>()

    inner class ItemViewHolder(private val binding: ItemGuardianBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(user : Pair<String, UserDTO>, context : Context) {
            val curYear = SimpleDateFormat("yyyy-MM-dd hh:mm")
                .format(System.currentTimeMillis())
                .split("-")[0].toInt()
            val userYear = user.second.birth!!.split("/")[0].toInt()
            val userAge = curYear - userYear + 1
            val userName = user.second.name

            Glide.with(context)
                .load(user.second.photoUri)
                .circleCrop()
                .into(binding.itemGuardianImg)
            binding.itemGuardianName.text = userName
            binding.itemGuardianAge.text = userAge.toString()
            binding.itemGuardianCheck.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) checkList.add(user.first)
                else checkList.remove(user.first)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
        return ItemViewHolder(
            DataBindingUtil.inflate(
                LayoutInflater.from(context), R.layout.item_guardian, parent, false)
        )
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