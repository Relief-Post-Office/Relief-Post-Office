package com.seoul42.relief_post_office.adapter

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.seoul42.relief_post_office.R
import com.seoul42.relief_post_office.databinding.DialogResponseBinding
import com.seoul42.relief_post_office.databinding.ItemUserBinding
import com.seoul42.relief_post_office.model.UserDTO
import java.text.SimpleDateFormat

class WardAdapter(private val context: Context, private val dataList: ArrayList<Pair<String, UserDTO>>) :
    RecyclerView.Adapter<WardAdapter.ItemViewHolder>() {
    inner class ItemViewHolder(private val binding: ItemUserBinding) : RecyclerView.ViewHolder(binding.root) {
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
                .into(binding.itemUserImg)
            binding.itemUserName.text = userName
            binding.itemUserAge.text = userAge.toString()
            binding.itemUserCall.setOnClickListener {
                /* 통화 바로 가능하도록 */
                ContextCompat.startActivity(
                    context,
                    Intent(Intent.ACTION_VIEW, Uri.parse("tel:" + user.second.tel)),
                    null
                )
            }
            binding.itemUserLayout.setOnClickListener {
                /* 보호자 정보 확인 */
                Toast.makeText(context, "안부 확인", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
        return ItemViewHolder(
            DataBindingUtil.inflate(
                LayoutInflater.from(context), R.layout.item_user, parent, false)
        )
    }

    override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
        holder.bind(dataList[position], context)
    }

    override fun getItemCount(): Int {
        return dataList.size
    }
}