package com.seoul42.relief_post_office.adapter

import android.app.AlertDialog
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
import com.seoul42.relief_post_office.guardian.ProfileActivity
import com.seoul42.relief_post_office.model.ResultDTO
import com.seoul42.relief_post_office.model.UserDTO
import com.seoul42.relief_post_office.result.ResultActivity
import com.seoul42.relief_post_office.safety.WardSafetySettingActivity
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

/**
 * 보호자와 연결된 피보호자들을 RecyclerView 에 띄우기 위한 adapter 클래스
 *  - context : MainFragment's context
 *  - dataList : 피보호자들을 담은 리스트
 */
class GuardianAdapter(
    private val context: Context,
    private val dataList: ArrayList<Pair<String, UserDTO>>
    ) : RecyclerView.Adapter<GuardianAdapter.ItemViewHolder>() {

    // 데이터베이스 참조 변수
    private val wardDB = Firebase.database.reference.child("ward")
    private val resultDB = Firebase.database.reference.child("result")

    inner class ItemViewHolder(private val binding: ItemUserBinding) : RecyclerView.ViewHolder(binding.root) {
        /**
         * 각 피보호자의 정보를 받아와서 RecyclerView 에 각각 세팅해주는 메서드
         * - user : <피보호자 id, 피보호자 UserDTO>
         * - context : MainFragment's context
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
                startActivity(
                    context,
                    Intent(Intent.ACTION_VIEW, Uri.parse("tel:" + user.second.tel)),
                    null
                )
            }

            // 연결된 피보호자를 눌렀을 때 피보호자 다이얼로그를 설정
            binding.itemUserLayout.setOnClickListener {
                setWardDialog(user)
            }
        }
    }

    /**
     * 보호자가 특정 피보호자를 선택할 때 피보호자 다이얼로그를 세팅하는 메서드
     *  - user : <피보호자 id, 피보호자 UserDTO>
     */
    private fun setWardDialog(user : Pair<String, UserDTO>) {
        // 피보호자 다이얼로그 설정
        val dialog = android.app.AlertDialog.Builder(context).create()
        val eDialog : LayoutInflater = LayoutInflater.from(context)
        val mView : View = eDialog.inflate(R.layout.ward_profile_dialog,null)

        // 리사이클 뷰 설정
        val resultList: MutableList<Pair<String, ResultDTO>> = mutableListOf()
        val adapter = ResultDialogAdapter(resultList)

        dialog.setView(mView)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.create()

        setWard(dialog, adapter, resultList, user)
        setProfileButton(dialog, user)
        setEditSafety(dialog, user)
        setResult(dialog, user)

        // 피보호자 다이얼로그 띄우기
        dialog.show()
    }

    /**
     * 피보호자가 오늘 수행한 안부 결과를 확인하도록 세팅하는 메서드
     * - dialog : 해당 피보호자의 다이얼로그
     * - adapter : 피보호자의 adapter
     * - resultList : <오늘 안부 결과 id, 오늘 안부 결과 resultDTO> 에 대한 리스트
     * - user : <피보호자 id, 피보호자 userDTO>
     */
    private fun setWard(
        dialog : AlertDialog,
        adapter : ResultDialogAdapter,
        resultList : MutableList<Pair<String, ResultDTO>>,
        user : Pair<String, UserDTO>
    ) {
        val userName = user.second.name

        // 피보호자 이름 세팅
        dialog.findViewById<TextView>(R.id.guardian_dialog_name).setText(userName)

        // 피보호자 사진 세팅
        Glide.with(context)
            .load(user.second.photoUri)
            .circleCrop()
            .into(dialog.findViewById<ImageView>(R.id.guardian_dialog_photo))

        dialog.findViewById<RecyclerView>(R.id.result_dialog_recycler).adapter = adapter
        dialog.findViewById<RecyclerView>(R.id.result_dialog_recycler).layoutManager = LinearLayoutManager(context)

        // 피보호자가 보유한 결과들을 받고 오늘 수행한 안부 결과들을 추가하기 위한 과정을 걸침
        wardDB.child(user.first).child("resultIdList").get().addOnSuccessListener { resultListSnapshot ->
            val resultIdList = resultListSnapshot.getValue<MutableMap<String, String>>() ?: mutableMapOf()
            processWardResultList(adapter, resultList, resultIdList)
        }
    }

    /**
     * 피보호자의 프로필을 선택 시 확인하도록 하기 위한 메서드
     *  - dialog : 피보호자의 다이얼로그
     *  - user : <피보호자 id, 피보호자 userDTO>
     */
    private fun setProfileButton(
        dialog : AlertDialog,
        user : Pair<String, UserDTO>
    ) {
        dialog.findViewById<Button>(R.id.guardian_dialog_profile_button).setOnClickListener {
            val intent = Intent(context, ProfileActivity::class.java)

            intent.putExtra("userDTO", user.second)
            startActivity(context, intent, null)
        }
    }

    /**
     * 안부 설정 버튼을 선택 시 안부 설정 화면으로 이동하도록 하는 메서드
     *  - dialog : 피보호자 다이얼로그
     *  - user : <피보호자 id, 피보호자 userDTO>
     */
    private fun setEditSafety(
        dialog : AlertDialog,
        user : Pair<String, UserDTO>
    ) {
        dialog.findViewById<Button>(R.id.guardian_dialog_safety_setting_button).setOnClickListener { setButton ->
            val intent = Intent(context, WardSafetySettingActivity::class.java)

            setButton.isClickable = false // 여러번 클릭을 방지 (화면 겹침을 방지함)

            intent.putExtra("photoUri", user.second.photoUri)
            intent.putExtra("wardId", user.first)
            intent.putExtra("wardName", user.second.name)
            startActivity(context, intent, null)

            setButton.isClickable = true // 화면 이동시 클릭 방지를 해제함
        }
    }

    /**
     * 결과 보기 버튼을 선택 시 피보호자의 결과를 확인하는 화면으로 이동하도록 하는 메서드
     *  - dialog : 피보호자 다이얼로그
     *  - user : <피보호자 id, 피보호자 userDTO>
     */
    private fun setResult(
        dialog : AlertDialog,
        user : Pair<String, UserDTO>
    ) {
        dialog.findViewById<Button>(R.id.guardian_dialog_result_button).setOnClickListener { resultButton ->
            val intent = Intent(context, ResultActivity::class.java)

            resultButton.isClickable = false // 여러번 클릭을 방지 (화면 겹침 방지)

            intent.putExtra("wardId", user.first)
            startActivity(context, intent, null)

            resultButton.isClickable = true // 화면 이동시 클릭 방지를 해제함
        }
    }

    /**
     * 피보호자가 보유한 모든 결과를 하나씩 받고 결과 목록에 추가할 지를 결정하는 메서드
     *  - adapter : 피보호자의 adapter
     *  - resultList : <오늘 안부 결과 id, 오늘 안부 결과 resultDTO> 에 대한 리스트
     *  - resultIdList : <안부 결과 id, 안부 결과 resultDTO> 에 대한 리스트
     */
    private fun processWardResultList(
        adapter : ResultDialogAdapter,
        resultList : MutableList<Pair<String, ResultDTO>>,
        resultIdList : MutableMap<String, String>
    ) {
        for ((dummy, resultId) in resultIdList) {
            resultDB.child(resultId).get().addOnSuccessListener { resultSnapshot ->
                val resultKey = resultSnapshot.key
                    ?: throw IllegalArgumentException("resultKey required")
                val result = resultSnapshot.getValue<ResultDTO>()
                    ?: throw IllegalArgumentException("result required")

                setWardResultList(adapter, resultList, resultKey, result)
            }
        }
    }

    /**
     * 피보호자의 특정 결과가 오늘 수행한 안부일 때 결과 목록에 추가하는 메서드
     *  - adapter : 피보호자의 adapter
     *  - resultList : <오늘 안부 결과 id, 오늘 안부 결과 resultDTO> 에 대한 리스트
     *  - resultKey : 피보호자의 결과 id
     *  - result : 피보호자의 ResultDTO
     */
    private fun setWardResultList(
        adapter : ResultDialogAdapter,
        resultList : MutableList<Pair<String, ResultDTO>>,
        resultKey : String,
        result : ResultDTO
    ) {
        if (isTodayAndExecuteSafety(result)) {
            resultList.add(Pair(resultKey, result))
            resultList.sortBy{ it.second.safetyTime }
            adapter.notifyDataSetChanged()
        }
    }

    /**
     * 오늘 수행한 안부인지 확인하는 메서드
     *  - result : 피보호자의 ResultDTO
     */
    private fun isTodayAndExecuteSafety(
        result : ResultDTO
    ) : Boolean {
        val sdf = SimpleDateFormat("yyyy-MM-dd")
        val today = sdf.format(System.currentTimeMillis())
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm")
        val curTime = Calendar.getInstance()
        val safetyTime = dateFormat.parse(result.date + " " + result.safetyTime)
            ?: throw IllegalArgumentException("result required")

        return result.date == today && curTime.time.time - safetyTime.time >= 0
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