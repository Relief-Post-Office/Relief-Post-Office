package com.seoul42.relief_post_office

import android.annotation.SuppressLint
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
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
import com.seoul42.relief_post_office.model.WardDTO
import java.text.SimpleDateFormat
import java.util.*

class ResultActivity : AppCompatActivity() {
    private val binding by lazy { ActivityResultBinding.inflate(layoutInflater) }
    private val database = Firebase.database
    private val storage = Firebase.storage
    private lateinit var date: String
    private var resultList = mutableListOf<ResultDTO>()
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
        setDateBtn()
        setDate()
        resultListenSet()
    }

    private fun setDateBtn() {
        binding.btnSetDate.setOnClickListener {
            showDatePickerDialog(binding.btnSetDate)
        }
        binding.btnSetDate.addTextChangedListener(object : TextWatcher{
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
            }

            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
            }

            @SuppressLint("NotifyDataSetChanged")
            override fun afterTextChanged(p0: Editable?) {
                resultList.clear()
                database.getReference("ward").child(wardId).get().addOnSuccessListener {
                    val resultIdList = it.getValue(WardDTO.ResultIdList::class.java) as WardDTO.ResultIdList
                    for (resultId in resultIdList.resultIdList) {
                        database.getReference("result").child(resultId).get().addOnSuccessListener {
                            val resultData = it.getValue(ResultDTO.ResultData::class.java) as ResultDTO.ResultData
                            if (resultData.date == binding.btnSetDate.text)
                                resultList.add(it.getValue(ResultDTO::class.java) as ResultDTO)
                            adapter.notifyDataSetChanged()
                        }
                    }
                }
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
        binding.btnSetDate.text = dateMessage
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

    @SuppressLint("SimpleDateFormat")
    private fun setDate() {
        val sdf = SimpleDateFormat("yyyy/MM/dd")
        date = sdf.format(System.currentTimeMillis())
        binding.btnSetDate.text = date

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
                            val resultData = it.getValue(ResultDTO.ResultData::class.java) as ResultDTO.ResultData
                            if (resultData.date == binding.btnSetDate.text)
                                resultList.add(it.getValue(ResultDTO::class.java) as ResultDTO)
                            adapter.notifyDataSetChanged()
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