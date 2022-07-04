package com.Seoul42.relief_post_office

import Results
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import com.seoul42.relief_post_office.databinding.ActivityResultBinding
import java.text.SimpleDateFormat

class ResultActivity : AppCompatActivity() {
    val binding by lazy { ActivityResultBinding.inflate(layoutInflater) }
    val database = Firebase.database
    val storage = Firebase.storage

    //intent로 넘어와야 할 정보들
    val wardId = "userid-6"
    //끝

    lateinit var date: String
    lateinit var resultsRef: DatabaseReference
    lateinit var resultListRef: DatabaseReference

    var resultList = mutableListOf<Results>()
    lateinit var adapter: ResultAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        setProfile("/profile/${wardId}.png")
        setWardName()
        setDate()

        resultListRef = database.getReference("wards").child(wardId).child("resultList")
        adapter = ResultAdapter(resultList)
        with(binding) {
            recyclerView.adapter = adapter
            recyclerView.layoutManager = LinearLayoutManager(baseContext)
        }
        loadResult()
    }

    fun setProfile(path: String) {
        storage.getReference(path).downloadUrl.addOnSuccessListener { uri ->
            Glide.with(this).load(uri).into(binding.imgProfile)
        }.addOnFailureListener {
            Log.e("스토리지", "다운로드 에러=>${it.message}")
        }
    }

    fun setWardName() {
        val usersRef = database.getReference("users")
        usersRef.child(wardId)
            .child("name")
            .get()
            .addOnSuccessListener {
                binding.textWardName.text = it.value.toString()
            }
    }

    fun setDate() {
        val sdf = SimpleDateFormat("yyyy/MM/dd")
        date = sdf.format(System.currentTimeMillis())
        binding.textDate.text = date
    }

    fun loadResult() {
        resultListRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                resultList.clear()
                for(item in snapshot.children) {
                    item.getValue(String::class.java)?.let { resultId ->
                        resultsRef = database.getReference("results")
                        resultsRef.child(resultId).get().addOnSuccessListener {
                            val result = it.getValue(Results::class.java)
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