package com.seoul42.relief_post_office.adapter

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.Button
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.seoul42.relief_post_office.R
import com.seoul42.relief_post_office.model.SafetyDTO
import com.seoul42.relief_post_office.safety.EditSafetyActivity
import com.seoul42.relief_post_office.safety.GetGuardianSafetyActivity

class GetGuardianSafetyRVAdapter(private val context : Context, private val items : ArrayList<Pair<String, SafetyDTO>>)
    : RecyclerView.Adapter<GetGuardianSafetyRVAdapter.ViewHolder>() {

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): GetGuardianSafetyRVAdapter.ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.safety_fragment_rv_item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bindItems(items[position])
    }

    // 전체 리사이클러 뷰의 아이템 개수
    override fun getItemCount(): Int {
        return items.size
    }

    inner class ViewHolder(itemView : View) : RecyclerView.ViewHolder(itemView) {

        // 데이터 매핑 해주기
        fun bindItems(item : Pair<String, SafetyDTO>){
            // 안부 이름 매핑
            val rvText = itemView.findViewById<TextView>(R.id.safety_fragment_rv_item_text)
            rvText.text = item.second.name

            // 아이템 클릭 시 적용 확인 다이얼로그 띄우기
            itemView.setOnClickListener{
                // 아이템 여러번 클릭 방지
                it.isClickable = false

                // 확인 다이얼로그 세팅
                val dialog = android.app.AlertDialog.Builder(context).create()
                val eDialog : LayoutInflater = LayoutInflater.from(context)
                val mView : View = eDialog.inflate(R.layout.get_guardian_safety_dialog,null)

                dialog.setView(mView)
                dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
                dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
                dialog.create()

                val dialogText = item.second.name + "를\n적용하시겠습니까?"
                dialog.findViewById<TextView>(R.id.get_guardian_safety_dialog_text)
                    .setText(dialogText)

                // 다이얼로그 띄우기
                dialog.show()

                // 다이얼로그 종료 시 이벤트
                dialog.setOnDismissListener {
                    // 아이템 클릭 방지 해제
                    itemView.isClickable = true
                }

                // "예" 버튼 눌렀을 때 이벤트
                dialog.findViewById<Button>(R.id.get_guardian_safety_dialog_yes).setOnClickListener {
                    it.isClickable = false
                    // 안부에 포함된 질문 리스트 전달
                    val returnIntent = Intent()
                    returnIntent.putExtra("questionsFromSafety", item.second.questionList.keys.toCollection(java.util.ArrayList<String>()))
                    (context as GetGuardianSafetyActivity).setResult(Activity.RESULT_OK, returnIntent)
                    dialog.dismiss()
                    context.finish()
                }

                // "아니오 버튼 눌렀을 때 이벤트
                dialog.findViewById<Button>(R.id.get_guardian_safety_dialog_no).setOnClickListener {
                    dialog.dismiss()
                }
            }
        }
    }

}