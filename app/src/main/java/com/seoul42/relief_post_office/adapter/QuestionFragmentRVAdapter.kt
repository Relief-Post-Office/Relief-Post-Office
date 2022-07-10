package com.seoul42.relief_post_office.adapter

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.*
import androidx.annotation.RequiresApi
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.seoul42.relief_post_office.R
import com.seoul42.relief_post_office.model.QuestionDTO
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class QuestionFragmentRVAdapter(private val context: Context, private val items: List<Pair<String, QuestionDTO>>)
    : RecyclerView.Adapter<QuestionFragmentRVAdapter.ViewHolder>() {

    val database = Firebase.database
    private lateinit var QuestionAdapter : QuestionFragmentRVAdapter

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int)
    : QuestionFragmentRVAdapter.ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.question_rv_item, parent, false)

        return ViewHolder(view)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onBindViewHolder(holder: QuestionFragmentRVAdapter.ViewHolder, position: Int) {
        holder.bindItems(items[position])
    }

    // 전체 리사이클러 뷰의 아이템 개수
    override fun getItemCount(): Int {
        return items.size
    }

    inner class ViewHolder(itemView : View) : RecyclerView.ViewHolder(itemView){

        // 데이터 매핑 해주기
        @RequiresApi(Build.VERSION_CODES.O)
        fun bindItems(item: Pair<String, QuestionDTO>){
            Log.d("하하하", "123123")
            // 각 질문 별 텍스트
            val rvText = itemView.findViewById<TextView>(R.id.question_rv_item_text)

            // text 매핑
            rvText.text = item.second.text

            // 아이템 눌렀을 때 이벤트
            rvText.setOnClickListener{
                // 질문 수정 다이얼로그 세팅
//                val date = item.body.date
                val questionText = item.second.text
                val secret = item.second.secret
                val record = item.second.record

                val dialog = android.app.AlertDialog.Builder(context).create()
                val eDialog : LayoutInflater = LayoutInflater.from(context)
                val mView : View = eDialog.inflate(R.layout.setting_question_dialog2,null)

                dialog.setView(mView)
                dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
                dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
                dialog.create()

                dialog.findViewById<EditText>(R.id.question_text2).setText(questionText) // 텍스트 세팅
                dialog.findViewById<Switch>(R.id.secret_switch2).isChecked = secret   // 비밀 스위치 세팅
                dialog.findViewById<Switch>(R.id.record_switch2).isChecked = record   // 녹음 스위치 세팅


                // 질문 수정 다이얼로그 띄우기
                dialog.show()


                // 질문 수정 다이얼로그의 "저장" 버튼을 눌렀을 때 이벤트 처리
                dialog.findViewById<Button>(R.id.save_question_btn).setOnClickListener {
                    // 텍스트, 비밀 옵션, 녹음 옵션
                    val editedQuestionText = dialog.findViewById<EditText>(R.id.question_text2).text.toString()
                    val editedSecret = dialog.findViewById<Switch>(R.id.secret_switch2).isChecked
                    val editedRecord = dialog.findViewById<Switch>(R.id.record_switch2).isChecked

                    // question 컬렉션에 수정된 질문 내용 수정
                    val question = database.getReference("question").child(item.first)
                    question.child("text").setValue(editedQuestionText)
                    question.child("secret").setValue(editedSecret)
                    question.child("record").setValue(editedRecord)
                    // 로그인한 보호자의 questionList와 question 컬렉션의 수정된 질문의 최종 수정날짜 수정
                    val date = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
                    question.child("date").setValue(date)
                    database.getReference("guardian").child(item.second.owner.toString())
                        .child("questionList")
                        .child(item.first).setValue(date)

                    // 다이얼로그 종료
                    Toast.makeText(context, "질문 수정 완료", Toast.LENGTH_SHORT).show()
                    dialog.dismiss()
                }

                // 질문 수정 다이얼로그의 "삭제" 버튼을 눌렀을 때 이벤트 처리
                dialog.findViewById<Button>(R.id.delete_question_btn).setOnClickListener {
                    // 해당 질문 id를 통해 데이터베이스에서 삭제
                    database.getReference("question").child(item.first).setValue(null)
                    // 로그인한 보호자의 질문 목록에서 해당하는 질문id 삭제하기
                    database.getReference("guardian")
                        .child(item.second.owner.toString())
                        .child("questionList")
                        .child(item.first).setValue(null)

                    // 다이얼로그 종료
                    Toast.makeText(context, "질문 삭제 완료", Toast.LENGTH_SHORT).show()
                    dialog.dismiss()
                }
            }

            // 녹음 소스 매핑 필요
        }
    }
}