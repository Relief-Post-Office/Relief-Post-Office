package com.seoul42.relief_post_office.adapter

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.seoul42.relief_post_office.R
import com.seoul42.relief_post_office.model.QuestionDTO
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class QuestionFragmentRVAdapter(private val context : Context, private val items : ArrayList<QuestionDTO>)
    : RecyclerView.Adapter<QuestionFragmentRVAdapter.ViewHolder>() {

    val database = Firebase.database
    private lateinit var QuestionAdapter : QuestionFragmentRVAdapter

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int)
    : QuestionFragmentRVAdapter.ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.question_rv_item, parent, false)

        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: QuestionFragmentRVAdapter.ViewHolder, position: Int) {
        holder.bindItems(items[position])
    }

    // 전체 리사이클러 뷰의 아이템 개수
    override fun getItemCount(): Int {
        return items.size
    }

    inner class ViewHolder(itemView : View) : RecyclerView.ViewHolder(itemView){

        // 데이터 매핑 해주기
        fun bindItems(item : QuestionDTO){
            // 각 질문 별 텍스트
            val rvText = itemView.findViewById<TextView>(R.id.question_rv_item_text)

            // text 매핑
            if (item.body!!.text != null){
                rvText.text = item.body.text
            }

            // 아이템 눌렀을 때 이벤트
            rvText.setOnClickListener{
                // 질문 수정 다이얼로그 세팅
//                val date = item.body.date
                val questionText = item.body.text
                val secret = item.body.secret
                val record = item.body.record

                val mDialogView = LayoutInflater.from(context).inflate(R.layout.setting_question_dialog2, null)
                val mBuilder = android.app.AlertDialog.Builder(context)
                    .setView(mDialogView)

                mDialogView.findViewById<EditText>(R.id.question_text2).setText(questionText) // 텍스트 세팅
                mDialogView.findViewById<Switch>(R.id.secret_switch2).isChecked = secret   // 비밀 스위치 세팅
                mDialogView.findViewById<Switch>(R.id.record_switch2).isChecked = record   // 녹음 스위치 세팅


                // 질문 수정 다이얼로그 띄우기
                val mAlertDialog = mBuilder.show()



                // 질문 수정 다이얼로그의 "저장" 버튼을 눌렀을 때 이벤트 처리
                mAlertDialog.findViewById<Button>(R.id.save_question_btn).setOnClickListener {
                    // 텍스트, 비밀 옵션, 녹음 옵션
                    val editedQuestionText = mAlertDialog.findViewById<EditText>(R.id.question_text2).text.toString()
                    val editedSecret = mAlertDialog.findViewById<Switch>(R.id.secret_switch2).isChecked
                    val editedRecord = mAlertDialog.findViewById<Switch>(R.id.record_switch2).isChecked

                    // question 컬렉션에 수정된 질문 내용 수정
                    val questionBody = database.getReference("question").child(item.key!!).child("body")
                    questionBody.child("text").setValue(editedQuestionText)
                    questionBody.child("secret").setValue(editedSecret)
                    questionBody.child("record").setValue(editedRecord)

                    // 다이얼로그 종료
                    Toast.makeText(context, "질문 수정 완료", Toast.LENGTH_SHORT).show()
                    mAlertDialog.dismiss()
                }

                // 질문 수정 다이얼로그의 "삭제" 버튼을 눌렀을 때 이벤트 처리
                mAlertDialog.findViewById<Button>(R.id.delete_question_btn).setOnClickListener {
                    // 해당 질문 id를 통해 데이터베이스에서 삭제
                    database.getReference("question").child(item.key!!).setValue(null)

                    // 다이얼로그 종료
                    Toast.makeText(context, "질문 삭제 완료", Toast.LENGTH_SHORT).show()
                    mAlertDialog.dismiss()
                }
            }

            // 녹음 소스 매핑 필요
        }
    }
}