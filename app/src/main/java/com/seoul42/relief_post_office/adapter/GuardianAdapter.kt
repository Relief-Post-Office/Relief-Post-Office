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
import androidx.core.content.ContextCompat.startActivity
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.seoul42.relief_post_office.R
import com.seoul42.relief_post_office.model.UserDTO
import com.seoul42.relief_post_office.result.ResultActivity
import com.seoul42.relief_post_office.safety.WardSafetySettingActivity
import java.text.SimpleDateFormat

class GuardianAdapter(private val context: Context, private val dataList: ArrayList<Pair<String, UserDTO>>) :
    RecyclerView.Adapter<GuardianAdapter.ItemViewHolder>() {
        inner class ItemViewHolder(itemView : View) : RecyclerView.ViewHolder(itemView) {
            private val userPhoto = itemView.findViewById<ImageView>(R.id.item_user_img)
            private val userText = itemView.findViewById<TextView>(R.id.item_user_text)
            private val userCall = itemView.findViewById<Button>(R.id.item_user_call)
            private val userLayout = itemView.findViewById<LinearLayout>(R.id.item_user_layout)

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
                    .into(userPhoto)
                userText.text = "$userName\n$userAge"
                userCall.setOnClickListener {
                    /* 통화 바로 가능하도록 */
                    ContextCompat.startActivity(
                        context,
                        Intent(Intent.ACTION_VIEW, Uri.parse("tel:" + user.second.tel)),
                        null
                    )
                }

                // 연결된 피보호자를 눌렀을 때 이벤트 처리
                userLayout.setOnClickListener {

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
        val view = LayoutInflater.from(context).inflate(R.layout.item_user, parent, false)
        return ItemViewHolder(view)
    }

    override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
        holder.bind(dataList[position], context)
    }

    override fun getItemCount(): Int {
        return dataList.size
    }
}