package com.seoul42.relief_post_office.adapter


import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat.startActivity
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.seoul42.relief_post_office.R
import com.seoul42.relief_post_office.model.SafetyDTO
import com.seoul42.relief_post_office.safety.EditWardSafetyActivity


class WardSafetyAdapter(private val context: Context, private val items: ArrayList<Pair<String, SafetyDTO>>,
                        private val wardName: String)
    : RecyclerView.Adapter<WardSafetyAdapter.ViewHolder>() {

    private val database = Firebase.database

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): WardSafetyAdapter.ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.ward_safety_item, parent, false)

        return ViewHolder(view)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onBindViewHolder(holder: WardSafetyAdapter.ViewHolder, position: Int) {
        holder.bindItems(items[position])
    }

    override fun getItemCount(): Int {
        return items.size
    }

    inner class ViewHolder(itemView : View) : RecyclerView.ViewHolder(itemView) {

        // 각 아이템마다 뷰 처리
        fun bindItems(item: Pair<String, SafetyDTO>){
            // 각 안부별 이름, 요일, 시간 세팅
            val rvName = itemView.findViewById<TextView>(R.id.ward_safety_item_name)
            val rvDayOfWeek = itemView.findViewById<TextView>(R.id.ward_safety_item_dayOfWeek)
            val rvTime = itemView.findViewById<TextView>(R.id.ward_safety_item_time)

            var itemDayOfWeek = ""
            var count = 0
            for (d in arrayListOf<String>("월", "화", "수", "목", "금", "토", "일",)){
                if (item.second.dayOfWeek[d]!!){
                    itemDayOfWeek += "${d} "
                    count++
                }
            }
            if (count == 7)
                itemDayOfWeek = "매일 "
            else if (count == 0){
                itemDayOfWeek = "비활성화 "
            }
            itemDayOfWeek = itemDayOfWeek.substring(0 until itemDayOfWeek.length - 1)

            rvName.text = item.second.name
            rvDayOfWeek.text = itemDayOfWeek
            rvTime.text = item.second.time

            // 안부 클릭시 안부 수정 액티비티로 이동
            itemView.setOnClickListener{
                // 여러번 클릭 방지
                it.isClickable = false

                // 선택한 안부를 누군가 수정중인지 확인
                val accessedGuardian = database.getReference("safety").child(item.first).child("Access")
                accessedGuardian.get().addOnSuccessListener {
                    Log.d("하하하", it.toString())
                    // 수정중인 사람이 없거나 나라면 수정 창으로 이동
                    if (it.getValue() == null || it.getValue() == Firebase.auth.currentUser!!.uid){
                        accessedGuardian.setValue(Firebase.auth.currentUser!!.uid).addOnSuccessListener {
                            val tmpIntent = Intent(context, EditWardSafetyActivity::class.java)
                            tmpIntent.putExtra("safetyId", item.first)
                            tmpIntent.putExtra("wardName", wardName)
                            startActivity(context, tmpIntent, null)
                        }
                    }
                    // 다른 누군가가 수정 중이라면 Toast로 누가 수정중인지 알려줌
                    else{
                        val accessedGuardianName = database.getReference("user").child(it.getValue().toString()).child("name")
                        accessedGuardianName.get().addOnSuccessListener {
                            Toast.makeText(context, it.getValue().toString() + "님이 수정중입니다", Toast.LENGTH_LONG).show()
                        }
                    }
                    itemView.isClickable = true
                }
            }
        }
    }
}