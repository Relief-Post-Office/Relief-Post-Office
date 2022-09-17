package com.seoul42.relief_post_office.result

import android.annotation.SuppressLint
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.database.ktx.getValue
import com.google.firebase.ktx.Firebase
import com.seoul42.relief_post_office.adapter.ResultDetailAdapter
import com.seoul42.relief_post_office.databinding.ActivityResultDetailBinding
import com.seoul42.relief_post_office.model.AnswerDTO
import com.seoul42.relief_post_office.model.ResultDTO

/**
 * 결과 페이지의 두번째 화면을 동작시키는 액티비티 클래스
 *
 *  주요 UI : 해당 결과의 질문, 그 질문에 해당하는 응답(O, X, 녹음 응답)
 */
class ResultDetailActivity : AppCompatActivity() {
    // 레이아웃 연결
    private val binding by lazy { ActivityResultDetailBinding.inflate(layoutInflater) }

    // 데이터베이스
    private val auth : FirebaseAuth by lazy { Firebase.auth }
    private val database = Firebase.database
    private val userDB = database.getReference("user")
    private val resultDB = database.getReference("result")
    private val answerDB = database.getReference("answer")

    // 결과 상세 페이지
    private var answerList = mutableListOf<Pair<String, AnswerDTO>>()
    private lateinit var adapter: ResultDetailAdapter

    // 리스너 관리
    private lateinit var listenerDTO : Pair<DatabaseReference, ValueEventListener>

    /**
     * 액티비티가 생성되었을 때 호출 되는 매서드
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 피보호자 ID
        val wardId = intent.getSerializableExtra("wardId") as String
        // 결과 ID
        val resultId = intent.getSerializableExtra("resultId") as String
        // 결과
        val result = intent.getSerializableExtra("result") as ResultDTO
        // 전체 레이아웃 표시
        setContentView(binding.root)
        // 안부 이름 설정
        setSafetyName(result.safetyName)
        // 피보호자 이름 설정
        setWardName(wardId)
        // 선택한 날짜 설정
        setDate(result.date)
        // 질문 리사이클 뷰 어뎁터 설정
        setAdapter(result.safetyName, result.date)
        // 질문 리사이클 뷰 List 설정
        setQuestionAnswerList(resultId)
    }

    /**
     * 액티비티가 종료되었을 때 호출되는 매서드
     *
     * - 해당 액티비티가 가지고 있던 리스너를 반환합니다.
     */
    override fun onDestroy() {
        super.onDestroy()

        val reference : DatabaseReference = listenerDTO.first
        val listener : ValueEventListener = listenerDTO.second

        reference.removeEventListener(listener)
    }

    /**
     * 결과에 해당하는 안부의 이름을 표시하는 매서드
     */
    private fun setSafetyName(safetyName: String) {
        binding.textResultDetailSafetyName.text = safetyName
    }

    /**
     * 피보호자의 이름을 표시하는 매서드
     */
    private fun setWardName(wardId: String) {
        userDB.child(wardId).child("name")
            .get().addOnSuccessListener { wardNameSnapshot ->
            val wardName = (wardNameSnapshot.value ?: throw IllegalArgumentException("corresponding resultID not exists"))
                .toString()
            binding.textResultDetailWardName.text = wardName
        }
    }

    /**
     * 결과 날짜를 표시하는 매서드
     */
    private fun setDate(date: String) {
        binding.textResultDetailDate.text = date.replace("-", "/")

    }

    /**
     * 결과의 질문을 보여주는 리사이클 뷰의 어뎁터를 셋팅하는 매서드
     */
    private fun setAdapter(safetyName: String, answerDate: String) {
        // 어뎁터 객체 생성
        adapter = ResultDetailAdapter(this, answerList, safetyName, answerDate)
        // 리사이클 뷰 어뎁터, 매니저 설정
        with(binding) {
            resultDetailRecyclerView.adapter = adapter
            resultDetailRecyclerView.layoutManager = LinearLayoutManager(baseContext)
        }
    }

    /**
     * 결과의 응답내용이 변하는 이벤트를 처리하는 리스너를 등록하는 매서드
     */
    private fun setQuestionAnswerList(resultId: String) {
        // 결과의 응답 iD 리스트 가져오기
        val answerListRef = resultDB.child(resultId).child("answerList")
        // 결과의 응답이 추가 되었을 때 발생하는 이벤트 리스너
        val answerListener = answerListRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                // 리스트 새로 세팅
                updateQuestionAnswerList(resultId)
            }

            override fun onCancelled(error: DatabaseError) {
                print(error.message)
            }
        })
        // 생성된 리스너 등록
        listenerDTO = Pair(answerListRef, answerListener)
    }

    /**
     * 결과의 질문을 보여주는 리사이클 뷰의 내용에 해당하는 리스트를 세팅하는 매서드
     */
    @SuppressLint("NotifyDataSetChanged")
    private fun updateQuestionAnswerList(resultId: String) {
        // 리스트 초기화
        answerList.clear()
        // 어뎁터 동기화
        adapter.notifyDataSetChanged()
        // 응답 ID들 가져오기
        val answerListRef = resultDB.child(resultId).child("answerIdList")
        answerListRef.get().addOnSuccessListener { answerListSnapshot ->
            val answerIdList = answerListSnapshot.getValue<MutableMap<String, String>>() ?: mutableMapOf()
            // ID 들에 해당하는 응답를 리스트에 추가
            addAnswers(answerIdList)
        }
    }

    /**
     * ID 들에 해당하는 응답를 리스트에 추가하는 매서드
     */
    private fun addAnswers(answerIdList: MutableMap<String, String>) {
        for ((dummy, answerId) in answerIdList) {
            // 응답 ID에 해당하는 응답 리스트에 추가
            addAnswer(answerId)
        }
    }

    /**
     * 응답 ID에 해당하는 응답 리스트에 추가하는 매서드
     *
     * 응답는 질문의 종류와 마찬가지로 비밀질문에 해당하는 비밀 응답이 있습니다.
     * 비밀 응답의 경우 해당 질문의 소유자만 확인 할 수 있습니다.
     */
    private fun addAnswer(answerId: String) {
        // ID에 해당하는 응답 데이터 가져오기
        answerDB.child(answerId).get().addOnSuccessListener { answerSnapshot ->
            val answer = answerSnapshot.getValue(AnswerDTO::class.java) ?: throw IllegalArgumentException("corresponding answer not exists")
            val userId = auth.uid.toString()
            if (!answer.questionSecret) {
                // 응답이 비밀 응답이 아닐 경우
                // 응답 추가
                answerList.add(Pair(answerId, answer))
                adapter.notifyDataSetChanged()
            }
            else if ((answer.questionSecret) and (answer.questionOwner == userId)) {
                // 응답이 비밀 응답이고 해당 질문의 주인일 경우
                // 응답 추가
                answerList.add(Pair(answerId, answer))
                adapter.notifyDataSetChanged()
            }
        }
    }
}