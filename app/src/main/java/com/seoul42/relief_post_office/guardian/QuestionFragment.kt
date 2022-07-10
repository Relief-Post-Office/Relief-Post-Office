package com.seoul42.relief_post_office.guardian

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.Window
import android.widget.*
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.seoul42.relief_post_office.R
import com.seoul42.relief_post_office.adapter.QuestionFragmentRVAdapter
import com.seoul42.relief_post_office.model.QuestionDTO
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class QuestionFragment : Fragment(R.layout.fragment_question) {


    private val database = Firebase.database
    // 유저가 가지고 있는 질문들의 객체를 담은 리스트 선언
    private var questionList = arrayListOf<Pair<String, QuestionDTO>>()
    // 리스트를 가진 아답터를 담은 변수 초기화
    private lateinit var QuestionAdapter : QuestionFragmentRVAdapter
    private lateinit var auth : FirebaseAuth
    private lateinit var owner : String

    // 프래그먼트 실행시 동작
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 로그인 한 사람 uid 가져오기
        auth = Firebase.auth
        owner = auth.currentUser?.uid.toString()

        // questionList 세팅
        setQuestionList()

        // recycler 뷰 세팅
        setRecyclerView(view)

        // recycler 뷰 자동 업데이트
        updateRV()

        // 질문 추가 버튼 이벤트
        val questionPlusBtn = view.findViewById<ImageView>(R.id.question_rv_item_plusBtn)
        questionPlusBtn.setOnClickListener{

            // 질문 추가 다이얼로그 띄우기
            val dialog = android.app.AlertDialog.Builder(context).create()
            val eDialog : LayoutInflater = LayoutInflater.from(context)
            val mView : View = eDialog.inflate(R.layout.setting_question_dialog,null)

            dialog.setView(mView)
            dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
            dialog.create()
            dialog.show()

            // 질문 추가 다이얼로그의 "저장"버튼을 눌렀을 때 이벤트 처리
            dialog.findViewById<Button>(R.id.add_question_btn).setOnClickListener {

                // 생성 날짜, 텍스트, 비밀 옵션, 녹음 옵션, 녹음 파일 주소
                val date = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
                val questionText = dialog.findViewById<EditText>(R.id.question_text).text.toString()
                val secret = dialog.findViewById<Switch>(R.id.secret_switch).isChecked
                val record = dialog.findViewById<Switch>(R.id.record_switch).isChecked
                val src = null

                // question 컬렉션에 추가할 QuestoinBody 생성
                val newQuestion = QuestionDTO(owner, date, questionText, secret, record, src)

                // question 컬렉션에 작성한 내용 추가
                val questionRef = database.getReference("question")
                val newPush = questionRef.push()
                val key = newPush.key.toString()
                newPush.setValue(newQuestion)

                // 지금 로그인한 사람 질문 목록에 방금 등록한 질문 아이디 추가
                val userQuestionRef = database.getReference("guardian").child(owner).child("questionList")
                userQuestionRef.child(key).setValue(date)

                // 다이얼로그 종료
                Toast.makeText(context, "질문 추가 완료", Toast.LENGTH_SHORT).show()
                dialog.dismiss()

            }
        }
    }

    // questionList 실시간 세팅해주기
    private fun setQuestionList(){
        // 로그인한 유저의 질문 목록
        val userQuestionRef = database.getReference("guardian").child(owner).child("questionList")

        // questionList에 로그인한 유저의 질문들 넣기
        userQuestionRef.addChildEventListener(object : ChildEventListener{
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                // 로그인한 유저의 질문 하나씩 참조
                val questionId = snapshot.key.toString()
                val questionToAdd = database.getReference("question").child(questionId)

                Log.d("하하하", owner)

                // 질문 컬렉션에서 각 질문 불러와서 questionList에 넣기
                questionToAdd.get().addOnSuccessListener {
                    questionList.add(Pair(questionId, it.getValue(QuestionDTO::class.java) as QuestionDTO))
                    // 내림차순으로 정렬(map -> list.sort -> map)
                    questionList.sortedByDescending { it.second.date }
                    // 리사이클러 뷰 어댑터에 알려주기
                    QuestionAdapter.notifyDataSetChanged()
                }
            }

            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
            }

            override fun onChildRemoved(snapshot: DataSnapshot) {
            }

            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {
            }

            override fun onCancelled(error: DatabaseError) {
            }
        })
    }

    // 질문 수정시 리사이클러 뷰 업데이트
    private fun updateRV() {
        val QuestionRef = database.getReference("guardian").child(owner).child("questionList")

        QuestionRef.addChildEventListener(object : ChildEventListener{
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
            }

            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
                // questionID 찾기
                val questionId = snapshot.key.toString()
                val questionInDB = database.getReference("question").child(questionId)

                Log.d("하하하2", snapshot.toString())

                // questionList에서 questionID에 해당하는 질문 찾아 수정해주기
                for (q in questionList){
                    if (q.first == questionId){
                        questionInDB.get().addOnSuccessListener {
                            Log.d("하하하3", questionList.toString())
                            q.second.text = it.child("text").getValue().toString()
                            q.second.record = it.child("record").getValue() as Boolean
                            q.second.secret = it.child("secret").getValue() as Boolean
                            q.second.date = it.child("date").getValue().toString()
                            Log.d("하하하4", questionList.toString())

                            Log.d("하하하5", questionList.toString())
                            // 가장 최근에 수정된 것이 리스트 상단으로 가게 하기
                            // 내림차순으로 정렬(map -> list.sort -> map)
                            questionList.sortedByDescending { it.second.date }

                            // 리스트가 수정되었다고 어댑터에게 알려주기
                            QuestionAdapter.notifyDataSetChanged()
                        }
                        break
                    }
                }
            }

            override fun onChildRemoved(snapshot: DataSnapshot) {
                // questionID 찾기
                val questionId = snapshot.key.toString()

                // questionList에서 questionID에 해당하는 질문 찾아 삭제하기
                for (q in questionList){
                    if (q.first == questionId){
                        questionList.remove(q)
                    }
                }
                // 리스트가 수정되었다고 어댑터에게 알려주기
                QuestionAdapter.notifyDataSetChanged()
            }

            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {
            }

            override fun onCancelled(error: DatabaseError) {
            }

        })
    }

    // 리사이클러 뷰 세팅함수
    private fun setRecyclerView(view : View){
        val recyclerView = view.findViewById<RecyclerView>(R.id.question_rv)

        QuestionAdapter = QuestionFragmentRVAdapter(view.context, questionList)
        recyclerView.adapter = QuestionAdapter
        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.setHasFixedSize(true)
    }

}