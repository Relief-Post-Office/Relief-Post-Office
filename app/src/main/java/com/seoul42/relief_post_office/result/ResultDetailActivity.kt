package com.seoul42.relief_post_office.result

import android.annotation.SuppressLint
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
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

class ResultDetailActivity : AppCompatActivity() {
    private val binding by lazy { ActivityResultDetailBinding.inflate(layoutInflater) }
    private val auth : FirebaseAuth by lazy { Firebase.auth }
    private val database = Firebase.database
    private val userDB = database.getReference("user")
    private val resultDB = database.getReference("result")
    private val answerDB = database.getReference("answer")
    private var answerList = mutableListOf<Pair<String, AnswerDTO>>()
    private lateinit var adapter: ResultDetailAdapter
    private lateinit var listenerDTO : Pair<DatabaseReference, ValueEventListener>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        val wardId = intent.getSerializableExtra("wardId") as String
        val resultId = intent.getSerializableExtra("resultId") as String
        val result = intent.getSerializableExtra("result") as ResultDTO
        Log.d("확인", wardId+" / " + resultId+" / " + result.toString())
        setSafetyName(result.safetyName)
        setWardName(wardId)
        setDate(result.date)
        setAdapter(result.safetyName, result.date)
        setQuestionAnswerList(resultId)
    }

    override fun onDestroy() {
        super.onDestroy()

        val reference : DatabaseReference = listenerDTO.first
        val listener : ValueEventListener = listenerDTO.second

        reference.removeEventListener(listener)
    }

    private fun setSafetyName(safetyName: String) {
        binding.textResultDetailSafetyName.text = safetyName
    }

    private fun setWardName(wardId: String) {
        userDB.child(wardId).child("name")
            .get().addOnSuccessListener { wardNameSnapshot ->
            val wardName = (wardNameSnapshot.value ?: throw IllegalArgumentException("corresponding resultID not exists"))
                .toString()
            binding.textResultDetailWardName.text = wardName
        }
    }

    private fun setDate(date: String) {
        binding.textResultDetailDate.text = date.replace("-", "/")

    }

    private fun setAdapter(safetyName: String, answerDate: String) {
        adapter = ResultDetailAdapter(this, answerList, safetyName, answerDate)
        with(binding) {
            resultDetailRecyclerView.adapter = adapter
            resultDetailRecyclerView.layoutManager = LinearLayoutManager(baseContext)
        }
    }

    private fun setQuestionAnswerList(resultId: String) {
        val answerListRef = database.getReference("result").child(resultId).child("answerList")

        val answerListener = answerListRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                updateQuestionAnswerList(resultId)
            }

            override fun onCancelled(error: DatabaseError) {
                print(error.message)
            }
        })
        listenerDTO = Pair(answerListRef, answerListener)
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun updateQuestionAnswerList(resultId: String) {
        answerList.clear()
        adapter.notifyDataSetChanged()
        val answerListRef = resultDB.child(resultId).child("answerIdList")
        answerListRef.get().addOnSuccessListener { answerListSnapshot ->
            val answerIdList = answerListSnapshot.getValue<MutableMap<String, String>>() ?: mutableMapOf()
            addAnswers(answerIdList)
        }
    }

    private fun addAnswers(answerIdList: MutableMap<String, String>) {
        for ((dummy, answerId) in answerIdList) {
            addAnswer(answerId)
        }
    }

    private fun addAnswer(answerId: String) {
        answerDB.child(answerId).get().addOnSuccessListener { answerSnapshot ->
            val answer = answerSnapshot.getValue(AnswerDTO::class.java) ?: throw IllegalArgumentException("corresponding answer not exists")
            val userId = auth.uid.toString()
            if (!answer.questionSecret) {
                answerList.add(Pair(answerId, answer))
                adapter.notifyDataSetChanged()
            }
            else if ((answer.questionSecret) and (answer.questionOwner == userId)) {
                answerList.add(Pair(answerId, answer))
                adapter.notifyDataSetChanged()
            }
        }
    }
}