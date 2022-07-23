package com.seoul42.relief_post_office.result

import android.annotation.SuppressLint
import android.os.Bundle
import android.os.Handler
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.google.firebase.database.*
import com.google.firebase.database.ktx.database
import com.google.firebase.database.ktx.getValue
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import com.seoul42.relief_post_office.adapter.ResultAdapter
import com.seoul42.relief_post_office.databinding.ActivityResultBinding
import com.seoul42.relief_post_office.model.ListenerDTO
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
    private lateinit var listenerDTO : Pair<DatabaseReference, ValueEventListener>

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val wardId = intent.getSerializableExtra("wardId") as String
        setContentView(binding.root)
        setProfile("/profile/${wardId}.jpg")
        setWardName(wardId)
        setDate()
        setAdapter(wardId)
        setDateBtn(wardId)
        resultListenSet(wardId)
    }

    override fun onDestroy() {
        super.onDestroy()

        val reference : DatabaseReference = listenerDTO.first
        val listener : ValueEventListener = listenerDTO.second

        reference.removeEventListener(listener)
    }

    private fun setDateBtn(wardId: String) {
        binding.btnResultSetDate.setOnClickListener {
            showDatePickerDialog(binding.btnResultSetDate)
        }
        binding.btnResultSetDate.addTextChangedListener(object : TextWatcher{
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
            }

            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
            }

            override fun afterTextChanged(p0: Editable?) {
                resetResultList(wardId)
            }
        })
    }

    private fun isEnableDate(year: Int, month: Int, day: Int): Boolean {
        val calendar:Calendar = Calendar.getInstance()
        calendar.set(year,month,day)
        val sdf = SimpleDateFormat("yyyyMMdd")
        val today = sdf.format(System.currentTimeMillis()).toLong()
        val pickerDate = sdf.format(calendar.timeInMillis).toLong()
        return today >= pickerDate
    }

    fun showDatePickerDialog(v: View) {
        val newFragment = DatePickerFragment(binding.btnResultSetDate.text.toString())
        newFragment.show(supportFragmentManager, "datePicker")
    }

    @SuppressLint("SimpleDateFormat")
    fun processDatePickerResult(year: Int, month: Int, day: Int) {
        if (isEnableDate(year, month, day)) {
            val calendar:Calendar = Calendar.getInstance()
            calendar.set(year,month,day)
            val sdf = SimpleDateFormat("yyyy/MM/dd")
            val dateMessage = sdf.format(calendar.timeInMillis)
            binding.btnResultSetDate.text = dateMessage
        }
        else {
            Toast.makeText(this, "데이터가 없는 날입니다!", Toast.LENGTH_SHORT)
        }
    }

    private fun setProfile(path: String) {
        storage.getReference(path).downloadUrl.addOnSuccessListener { uri ->
            Glide.with(this).load(uri).into(binding.imgResultProfile)
        }.addOnFailureListener {
            Log.e("스토리지", "다운로드 에러=>${it.message}")
        }
    }

    private fun setWardName(wardId: String) {
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

    private fun setAdapter(wardId: String) {
        adapter = ResultAdapter(this, resultList, wardId)
        with(binding) {
            resultRecyclerView.adapter = adapter
            resultRecyclerView.layoutManager = LinearLayoutManager(baseContext)
        }
    }

    private fun resultListenSet(wardId: String) {
        val resultListRef = database.getReference("ward").child(wardId).child("resultIdList")

        val resultListener = resultListRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                resetResultList(wardId)
            }
            override fun onCancelled(error: DatabaseError) {
                print(error.message)
            }
        })

        listenerDTO = Pair(resultListRef, resultListener)
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun resetResultList(wardId: String) {
        val resultListRef = database.getReference("ward").child(wardId).child("resultIdList")
        val resultsRef = database.getReference("result")
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm")
        val curTime = Calendar.getInstance()

        resultList.clear()
        adapter.notifyDataSetChanged()
        resultListRef.get().addOnSuccessListener {
            if (it.value != null) {
                val resultIdList = it.getValue<MutableMap<String, String>>() as MutableMap<String, String>
                for ((dummy, resultId) in resultIdList) {
                    resultsRef.child(resultId).get().addOnSuccessListener {
                        if (it.value != null) {
                            val result = it.getValue(ResultDTO::class.java) as ResultDTO
                            if (result.date.replace("-", "/") == binding.btnResultSetDate.text.toString()) {
                                val safetyTime = dateFormat.parse(result.date + " " + result.safetyTime)

                                if (curTime.time.time - safetyTime.time >= 0) {
                                    resultList.add(Pair(resultId, result))
                                    resultList.sortBy { it.second.safetyTime }
                                    adapter.notifyDataSetChanged()
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}