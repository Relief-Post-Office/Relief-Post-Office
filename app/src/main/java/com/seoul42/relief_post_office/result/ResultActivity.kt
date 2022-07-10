package com.seoul42.relief_post_office.result

import android.annotation.SuppressLint
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.database.ktx.getValue
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import com.seoul42.relief_post_office.ResultAdapter
import com.seoul42.relief_post_office.databinding.ActivityResultBinding
import com.seoul42.relief_post_office.model.ResultDTO
import java.text.SimpleDateFormat
import java.util.*

class ResultActivity : AppCompatActivity() {
    private val binding by lazy { ActivityResultBinding.inflate(layoutInflater) }
    private val database = Firebase.database
    private val storage = Firebase.storage
    private lateinit var date: String
    private var resultList = mutableListOf<Pair<String, ResultDTO>>()
    private lateinit var adapter: ResultAdapter
    //intent로 넘어와야 할 정보들
    private val wardId = "jNmigty6iAST8GNZHtkbasmfINy1"
    //끝

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        setProfile("/profile/${wardId}.jpg")
        setWardName()
        setDate()
        setAdapter()
        setDateBtn()
        resultListenSet()
    }

    private fun setDateBtn() {
        binding.btnResultSetDate.setOnClickListener {
            showDatePickerDialog(binding.btnResultSetDate)
        }
        binding.btnResultSetDate.addTextChangedListener(object : TextWatcher{
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
            }

            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
            }

            override fun afterTextChanged(p0: Editable?) {
                resetResultList()
            }
        })
    }

    fun showDatePickerDialog(v: View) {
        val newFragment = DatePickerFragment()
        newFragment.show(supportFragmentManager, "datePicker")
    }

    @SuppressLint("SimpleDateFormat")
    fun processDatePickerResult(year: Int, month: Int, day: Int) {
        val calendar:Calendar = Calendar.getInstance()
        calendar.set(year,month,day)
        val sdf = SimpleDateFormat("yyyy/MM/dd")
        val dateMessage = sdf.format(calendar.timeInMillis)
        binding.btnResultSetDate.text = dateMessage
    }

    private fun setProfile(path: String) {
        storage.getReference(path).downloadUrl.addOnSuccessListener { uri ->
            Glide.with(this).load(uri).into(binding.imgResultProfile)
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
                binding.textResultWardName.text = it.value.toString()
            }
    }

    @SuppressLint("SimpleDateFormat")
    private fun setDate() {
        val sdf = SimpleDateFormat("yyyy/MM/dd")
        date = sdf.format(System.currentTimeMillis())
        binding.btnResultSetDate.text = date

    }

    private fun setAdapter() {
        adapter = ResultAdapter(this, resultList, wardId)
        with(binding) {
            resultRecyclerView.adapter = adapter
            resultRecyclerView.layoutManager = LinearLayoutManager(baseContext)
        }
    }

    private fun resultListenSet() {
        val resultListRef = database.getReference("ward").child(wardId).child("resultIdList")

        resultListRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                resetResultList()
            }
            override fun onCancelled(error: DatabaseError) {
                print(error.message)
            }
        })
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun resetResultList() {
        val resultListRef = database.getReference("ward").child(wardId).child("resultIdList")
        val resultsRef = database.getReference("result")

        resultList.clear()
        resultListRef.get().addOnSuccessListener {
            if (it.value != null) {
                val resultIdList = it.getValue<MutableMap<String, String>>() as MutableMap<String, String>
                for ((dummy, resultId) in resultIdList) {
                    resultsRef.child(resultId).get().addOnSuccessListener {
                        if (it.value != null) {
                            val result = it.getValue(ResultDTO::class.java) as ResultDTO
                            if (result.date == binding.btnResultSetDate.text.toString()) {
                                resultList.add(Pair(it.key.toString(), result))
                                adapter.notifyDataSetChanged()
                            }
                        }
                    }
                }
            }
        }
    }
}