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

/**
 * 피보호자와 연결된 보호자들을 RecyclerView 에 띄우기 위한 adapter 클래스
 *  - context : WardActivity's context
 *  - dataList : 보호자들을 담은 리스트
 */
class WardAdapter(
    private val context: Context,
    private val dataList: ArrayList<Pair<String, UserDTO>>
    ) : RecyclerView.Adapter<WardAdapter.ItemViewHolder>() {

    inner class ItemViewHolder(private val binding: ItemUserBinding) : RecyclerView.ViewHolder(binding.root) {
        /**
         * 각 보호자의 정보를 받아와서 RecyclerView 에 각각 세팅해주는 메서드
         * - user : <보호자 id, 보호자 UserDTO>
         * - context : WardActivity's context
         */
        fun bind(user : Pair<String, UserDTO>, context : Context) {
            val curYear = SimpleDateFormat("yyyy-MM-dd hh:mm")
                .format(System.currentTimeMillis())
                .split("-")[0].toInt()
            val userYear = user.second.birth.split("/")[0].toInt()
            val userAge = curYear - userYear + 1
            val userName = user.second.name

            Glide.with(context)
                .load(user.second.photoUri)
                .circleCrop()
                .into(binding.itemUserImg)

            binding.itemUserName.text = userName
            binding.itemUserAge.text = userAge.toString()

            // 통화 버튼 누를 시 전화번호가 키패드에 세팅되도록 설정
            binding.itemUserCall.setOnClickListener {
                ContextCompat.startActivity(
                    context,
                    Intent(Intent.ACTION_VIEW, Uri.parse("tel:" + user.second.tel)),
                    null
                )
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