package com.seoul42.relief_post_office.result

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
import com.google.firebase.database.*
import com.google.firebase.database.ktx.database
import com.google.firebase.database.ktx.getValue
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import com.seoul42.relief_post_office.adapter.ResultAdapter
import com.seoul42.relief_post_office.databinding.ActivityResultBinding
import com.seoul42.relief_post_office.model.ResultDTO
import java.text.SimpleDateFormat
import java.util.*

/**
 * 결과 페이지의 첫번째 화면을 동작시키는 액티비티 클래스
 *
 */
class ResultActivity : AppCompatActivity() {
    // 레이아웃 연결
    private val binding by lazy { ActivityResultBinding.inflate(layoutInflater) }

    // 데이터베이스
    private val database = Firebase.database
    private val storage = Firebase.storage
    private val userDB = database.getReference("user")
    private val wardDB = database.getReference("ward")
    private val resultsDB = database.getReference("result")

    // 결과
    private lateinit var date: String
    private var resultList = mutableListOf<Pair<String, ResultDTO>>()
    private lateinit var adapter: ResultAdapter

    // 리스너 관리
    private lateinit var listenerDTO : Pair<DatabaseReference, ValueEventListener>

    /**
     * 액티비티가 생성되었을 때 호출 되는 매서드
     */
    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 피보호자의 id 정보
        val wardId = intent.getSerializableExtra("wardId") as String
        // 전체 레이아웃 표시
        setContentView(binding.root)
        // 피보호자 프로필 사진 설정
        setProfile("/profile/${wardId}.jpg")
        // 피보호자 이름 설정
        setWardName(wardId)
        // 날짜 선택기의 오늘 날짜 설정
        setDate()
        // 해당 날짜에 대한 안부 결과 표시를 위한 어뎁터 설정
        setAdapter(wardId)
        // 날짜 선택기 클릭 리스너 설정
        setDateBtn(wardId)
        // 데이터베이스 동기화를 위한 리스너 설정
        resultListenSet(wardId)
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
     * 피보호자의 사진을 표시하는 매서드
     *
     * - firebase 의 storage 의 "/profile/${wardId}.jpg" 형식의 이미지를 불러 옵니다.
     */
    private fun setProfile(path: String) {
        storage.getReference(path).downloadUrl.addOnSuccessListener { uri ->
            Glide.with(this).load(uri).into(binding.imgResultProfile)
        }.addOnFailureListener { profileErrorSnapshot ->
            Log.e("스토리지", "다운로드 에러=>${profileErrorSnapshot.message}")
        }
    }

    /**
     * 피보호자의 이름을 표시하는 매서드
     *
     * - user 콜렉션에서 참조합니다.
     */
    private fun setWardName(wardId: String) {
        userDB.child(wardId)
            .child("name")
            .get()
            .addOnSuccessListener { wardNameSnapshot ->
                binding.textResultWardName.text = wardNameSnapshot.value.toString()
            }
    }

    /**
     * 날짜 선택기에서 기본 날짜를 오늘로 설정하는 매서드
     */
    @SuppressLint("SimpleDateFormat")
    private fun setDate() {
        // 날짜 형식
        val sdf = SimpleDateFormat("yyyy/MM/dd")
        // 오늘 날짜를 형식에 맞게 변환
        date = sdf.format(System.currentTimeMillis())
        // 뷰에 셋팅
        binding.btnResultSetDate.text = date
    }

    /**
     * 날짜의 맞는 결과를 보여주기 위한 어뎁터 매서드
     *
     * - adapter/ResultAdapter 파일과 관련이 있습니다.
     */
    private fun setAdapter(wardId: String) {
        // 어뎁터 객체 생성
        adapter = ResultAdapter(this, resultList, wardId)
        // 리사이클 뷰에 어뎁터 설정
        with(binding) {
            resultRecyclerView.adapter = adapter
            resultRecyclerView.layoutManager = LinearLayoutManager(baseContext)
        }
    }

    /**
     * 날짜 선택기의 버튼의 이벤트를 설정하는 매서드
     *
     * 1. 버튼 클릭 : 날짜 선택기 다이얼로그 실행
     * 2. 날짜 변경 : 결과 리사이클 뷰 초기화 시켜 동기화
     */
    private fun setDateBtn(wardId: String) {
        // 1. 버튼 클릭 이벤트 리스너 설정
        binding.btnResultSetDate.setOnClickListener {
            // 날짜 다이얼로그 실행
            showDatePickerDialog(binding.btnResultSetDate)
        }
        // 2. 날짜 텍스트 변경 이벤트 리스너 설정
        binding.btnResultSetDate.addTextChangedListener(object : TextWatcher{
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
            }
            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
            }
            /**
             * 날짜가 바뀌고 나면 실행되는 매서드
             */
            override fun afterTextChanged(p0: Editable?) {
                // 결과 리사이클뷰 초기화
                resetResultList(wardId)
            }
        })
    }

    /**
     * 날짜 선택기 다이얼로그를 불러오는 매서드
     */
    fun showDatePickerDialog(v: View) {
        // 날짜 선택기 프래그먼트 객체를 이미 골라져 있는 날짜로 생성
        val newFragment = DatePickerFragment(binding.btnResultSetDate.text.toString())
        // 프래그먼트 레이아웃 그리기
        newFragment.show(supportFragmentManager, "datePicker")
    }

    /**
     * 날짜 선택기에서 날짜가 선택되면 실행되는 매서드
     *
     * 날짜는 오늘 날짜까지 고를 수 있습니다.
     * 데이터베이스에서 가까운 미래 결과도 미응답으로 저장되어 오늘 날짜로 제한합니다.
     */
    @SuppressLint("SimpleDateFormat")
    fun processDatePickerResult(year: Int, month: Int, day: Int) {
        // 데이터가 있는 날만 날짜 선택가능
        if (isEnableDate(year, month, day)) {
            // 날짜 형식
            val sdf = SimpleDateFormat("yyyy/MM/dd")
            // 숫자를 날짜 형식으로 변환
            val calendar:Calendar = Calendar.getInstance()
            calendar.set(year,month,day)
            val dateMessage = sdf.format(calendar.timeInMillis)
            // 결과 UI에 표시
            binding.btnResultSetDate.text = dateMessage
        }
        else {
            Toast.makeText(this, "데이터가 없는 날입니다!", Toast.LENGTH_SHORT)
        }
    }

    /**
     * 허용가능한 날짜인지를 판별하는 매서드
     *
     * 데이터베이스에서 가까운 미래 결과도 미응답으로 저장되어 오늘 날짜로 제한합니다.
     */
    private fun isEnableDate(year: Int, month: Int, day: Int): Boolean {
        // 날짜 형식
        val sdf = SimpleDateFormat("yyyyMMdd")
        // 날짜 형식으로 변환
        val calendar:Calendar = Calendar.getInstance()
        calendar.set(year,month,day)
        // 오늘 날짜
        val today = sdf.format(System.currentTimeMillis()).toLong()
        // 선택한 날짜
        val pickerDate = sdf.format(calendar.timeInMillis).toLong()
        return today >= pickerDate
    }

    /**
     * 리사이클뷰에서 표시되어야 할 결과 리스트를 세팅하는 매서드
     *
     * 데이터베이스와 실시간 동기화를 지원합니다.
     * 결과가 변경, 추가되면 결과 리스트를 초기화하여 동기화 시킵니다.
     */
    private fun resultListenSet(wardId: String) {
        // 해당 피보호자의 결과 id 리스트
        val resultListRef = wardDB.child(wardId).child("resultIdList")
        // 결과 id 리스트의 값이 추가되면 호출되는 리스너 설정
        val resultListener = resultListRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                // 결과 리스트 초기화
                resetResultList(wardId)
            }
            override fun onCancelled(error: DatabaseError) {
                print(error.message)
            }
        })
        // 리스너 관리
        listenerDTO = Pair(resultListRef, resultListener)
    }

    /**
     * 결과 리사이클뷰의 표시될 결과들을 초기화하는 매서드
     *
     * 리스트를 비우고 데이터베이스에서 모든 결과를 가져와 날짜에 맞는 결과만 리스트에 셋팅합니다.
     */
    @SuppressLint("NotifyDataSetChanged")
    private fun resetResultList(wardId: String) {
        // 해당 피보호자의 결과 id 리스트
        val resultListRef = wardDB.child(wardId).child("resultIdList")

        // 리스트 초기화
        resultList.clear()
        adapter.notifyDataSetChanged()
        // 결과 리스트 셋팅
        resultListRef.get().addOnSuccessListener { resultListSnapshot ->
            val resultIdList = resultListSnapshot.getValue<MutableMap<String, String>>() ?: mutableMapOf()
            for ((dummy, resultId) in resultIdList) {
                // 해당 날짜의 결과만 리스트에 추가
                addResult(resultId)
            }
        }
    }

    /**
     * 데이터베이스에서 결과 id에 해당하는 결과를 가져와 해당하는 결과만 리스트에 추가하는 매서드
     */
    private fun addResult(resultId: String) {
        resultsDB.child(resultId).get().addOnSuccessListener { resultSnapshot ->
            val result = resultSnapshot.getValue(ResultDTO::class.java) ?: throw IllegalArgumentException("corresponding resultID not exists")
            // 해당 날짜의 결과만 리스트에 추가
            sortResultList(result, resultId)
        }
    }

    /**
     * 가져온 결과가 해당하는 날짜, 시간의 결과이면 리스트에 추가하는 매서드
     *
     * 데이터베이스 설계상 가까운 안부의 결과가 미리생성되어 현재 시간을 제한
     */
    private fun sortResultList(result: ResultDTO, resultId: String) {
        // 날짜, 시간 형식
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm")
        // 현재 시간
        val curTime = Calendar.getInstance()
        // 해당하는 날짜 인지 확인
        if (isAccessibleDate(result)) {
            // 결과의 날짜와 시간 형식
            val safetyTime = dateFormat.parse(result.date + " " + result.safetyTime)

            // 현재 시간 기준 제한
            if (curTime.time.time - safetyTime.time >= 0) {
                // 추가
                resultList.add(Pair(resultId, result))
                // 정렬
                resultList.sortBy { it.second.safetyTime }
                // 어뎁터 동기화
                adapter.notifyDataSetChanged()
            }
        }
    }

    /**
     * 기준 날짜인지 확인하는 매서드
     *
     * 날짜 선택기에서 선택한 날짜에 해당하는 확인합니다.
     */
    private fun isAccessibleDate(result: ResultDTO) : Boolean{
        return result.date.replace("-", "/") == binding.btnResultSetDate.text.toString()
    }
}