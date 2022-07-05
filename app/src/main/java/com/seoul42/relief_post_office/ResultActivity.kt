package com.seoul42.relief_post_office

import android.annotation.SuppressLint
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import com.seoul42.relief_post_office.databinding.ActivityResultBinding
import com.seoul42.relief_post_office.model.ResultDTO
import java.text.SimpleDateFormat

class ResultActivity : AppCompatActivity() {
    private val binding by lazy { ActivityResultBinding.inflate(layoutInflater) }
    private val database = Firebase.database
    private val storage = Firebase.storage
    private lateinit var date: String
    private var resultList = mutableListOf<ResultDTO>()
    private lateinit var adapter: ResultAdapter
    //intent로 넘어와야 할 정보들
    val wardId = "Aw9Pgjc0xXYJ7L25zQ4CtgofuTP2"
    //끝

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        setProfile("/profile/${wardId}.jpg")
        setWardName()
        setDate()
        resultListenSet()
    }

    private fun setProfile(path: String) {
        storage.getReference(path).downloadUrl.addOnSuccessListener { uri ->
            Glide.with(this).load(uri).into(binding.imgProfile)
        }.addOnFailureListener {
            Log.e("스토리지", "다운로드 에러=>${it.message}")
        }
    }

    private fun setWardName() {
        val usersRef = database.getReference("user")
        usersRef.child(wardId)
            .child("name")
            .get()
            .addOnSuccessListener {
                binding.textWardName.text = it.value.toString()
            }
    }

    private fun setDate() {
        val sdf = SimpleDateFormat("yyyy/MM/dd")
        date = sdf.format(System.currentTimeMillis())
        binding.textDate.text = date
    }

    private fun resultListenSet() {
        val resultListRef = database.getReference("ward").child(wardId).child("resultList")
        val resultsRef = database.getReference("result")

        adapter = ResultAdapter(resultList)
        with(binding) {
            recyclerView.adapter = adapter
            recyclerView.layoutManager = LinearLayoutManager(baseContext)
        }
        resultListRef.addValueEventListener(object : ValueEventListener {
            @SuppressLint("NotifyDataSetChanged")
            override fun onDataChange(snapshot: DataSnapshot) {
                resultList.clear()
                for(item in snapshot.children) {
                    item.getValue(String::class.java)?.let { resultId ->
                        resultsRef.child(resultId).get().addOnSuccessListener {
                            val result = it.getValue(ResultDTO::class.java)
                            if (result != null) {
                                if (result.date == date)
                                    resultList.add(result)
                                adapter.notifyDataSetChanged()
                            }
                        }
                    }
                }
            }
            override fun onCancelled(error: DatabaseError) {
                print(error.message)
            }
        })
    }
}