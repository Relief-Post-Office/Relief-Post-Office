package com.seoul42.relief_post_office.adapter

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.*
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.core.content.ContextCompat.startActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.firebase.database.ktx.database
import com.google.firebase.database.ktx.getValue
import com.google.firebase.ktx.Firebase
import com.seoul42.relief_post_office.R
import com.seoul42.relief_post_office.databinding.ItemUserBinding
import com.seoul42.relief_post_office.model.ResultDTO
import com.seoul42.relief_post_office.model.UserDTO
import com.seoul42.relief_post_office.result.ResultActivity
import com.seoul42.relief_post_office.safety.WardSafetySettingActivity
import java.text.SimpleDateFormat

class GuardianAdapter(private val context: Context, private val dataList: ArrayList<Pair<String, UserDTO>>) :
    RecyclerView.Adapter<GuardianAdapter.ItemViewHolder>() {
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
                // 연결된 피보호자를 눌렀을 때 이벤트 처리
                binding.itemUserLayout.setOnClickListener {

                    // 피보호자 다이얼로그 세팅
                    val dialog = android.app.AlertDialog.Builder(context).create()
                    val eDialog : LayoutInflater = LayoutInflater.from(context)
                    val mView : View = eDialog.inflate(R.layout.ward_profile_dialog,null)

                    dialog.setView(mView)
                    dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
                    dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
                    dialog.create()

                    // 피보호자 이름 세팅
                    dialog.findViewById<TextView>(R.id.guardian_dialog_name).setText(userName)

                    // 피보호자 사진 세팅
                    Glide.with(context)
                        .load(user.second.photoUri)
                        .circleCrop()
                        .into(dialog.findViewById<ImageView>(R.id.guardian_dialog_photo))

                    // 리사이클 뷰 설정
                    var resultList: MutableList<Pair<String, ResultDTO>> = mutableListOf()
                    val adapter = ResultDialogAdapter(resultList)
                    dialog.findViewById<RecyclerView>(R.id.result_dialog_recycler).adapter = adapter
                    dialog.findViewById<RecyclerView>(R.id.result_dialog_recycler).layoutManager = LinearLayoutManager(context)

                    // 리사이클 뷰 리스트 설정
                    val database = Firebase.database
                    val sdf = SimpleDateFormat("yyyy-MM-dd")
                    val today = sdf.format(System.currentTimeMillis())

                    database.getReference("ward")
                        .child(user.first)
                        .child("resultIdList").get().addOnSuccessListener {
                            if (it.value != null) {
                                val resultIdList =
                                    it.getValue<MutableMap<String, String>>() as MutableMap<String, String>
                                for ((dummy, resultId) in resultIdList) {
                                    database.getReference("result")
                                        .child(resultId).get().addOnSuccessListener {
                                            val result = it.getValue<ResultDTO>()
                                            if (result != null) {
                                                if (result.date == today) {
                                                    resultList.add(Pair(it.key as String, result))
                                                    resultList.sortBy{ it.second.safetyTime }
                                                    adapter.notifyDataSetChanged()
                                                }
                                            }
                                        }
                                }
                            }
                        }


                    // 피보호자 다이얼로그 띄우기
                    dialog.show()

                    // 프로필 보기 버튼 이벤트 처리
                    dialog.findViewById<Button>(R.id.guardian_dialog_profile_button).setOnClickListener {

                    }

                    // 안부 설정 버튼 이벤트 처리
                    dialog.findViewById<Button>(R.id.guardian_dialog_safety_setting_button).setOnClickListener {
                        val intent = Intent(context, WardSafetySettingActivity::class.java)
                        intent.putExtra("photoUri", user.second.photoUri)
                        intent.putExtra("wardId", user.first)
                        intent.putExtra("wardName", user.second.name)
                        startActivity(context, intent, null)
                    }

                    // 결과 보기 버튼 이벤트 처리
                    dialog.findViewById<Button>(R.id.guardian_dialog_result_button).setOnClickListener {
                        val intent = Intent(context, ResultActivity::class.java)
                        intent.putExtra("wardId", user.first)
                        startActivity(context, intent, null)
                    }

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