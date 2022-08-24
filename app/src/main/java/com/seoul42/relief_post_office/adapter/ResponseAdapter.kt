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

/**
 * 피보호자에게 요청온 보호자의 정보를 선택한 체크리스트를 반환하도록 돕는 클래스
 *  - context : WardActivity's context
 *  - dataList : <요청온 보호자 id, 요청온 보호자 UserDTO> 의 목록
 */
class ResponseAdapter(
    private val context: Context,
    private val dataList: ArrayList<Pair<String, UserDTO>>
    ) : RecyclerView.Adapter<ResponseAdapter.ItemViewHolder>() {

    // 피보호자가 선택한 보호자 목록
    private val checkList = ArrayList<String>()

    inner class ItemViewHolder(private val binding: ItemGuardianBinding) : RecyclerView.ViewHolder(binding.root) {
        /**
         * 요청온 보호자 정보들을 받아와서 다이얼로그에 각각 세팅해주는 메서드
         * - user : <요청온 보호자 id, 요청온 보호자 UserDTO>
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
                .into(binding.itemGuardianImg)

            binding.itemGuardianName.text = userName
            binding.itemGuardianAge.text = userAge.toString()

            // 보호자를 선택하면 피보호자가 선택한 보호자 목록에 추가됨
            // 선택된 상태에서 다시 선택할 경우 보호자 목록에서 제거됨
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