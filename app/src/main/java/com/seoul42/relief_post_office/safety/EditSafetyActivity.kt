package com.seoul42.relief_post_office.safety

import android.app.Activity
import android.app.TimePickerDialog
import android.content.Intent
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.*
import androidx.annotation.RequiresApi
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.seoul42.relief_post_office.R
import com.seoul42.relief_post_office.adapter.AddWardSafetyAdapter
import com.seoul42.relief_post_office.model.QuestionDTO
import com.seoul42.relief_post_office.model.SafetyDTO
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.ArrayList

class EditSafetyActivity : AppCompatActivity() {

	private val database = Firebase.database
	private val QuestionRef = database.getReference("question")
	private var questionList = arrayListOf<Pair<String, QuestionDTO>>()
	private lateinit var owner : String
	private lateinit var safetyId : String
	private lateinit var safety : SafetyDTO
	private lateinit var editSafetyAdapter : AddWardSafetyAdapter

	@RequiresApi(Build.VERSION_CODES.O)
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(R.layout.activity_edit_safety)

		// 수정할 안부 데이터 가져오고 첫 세팅 하기
		safetyId = intent.getStringExtra("safetyId").toString()
		val safetyRef = database.getReference("safety").child(safetyId)
		owner = Firebase.auth.currentUser?.uid.toString()
		safetyRef.get().addOnSuccessListener {
			safety = it.getValue(SafetyDTO::class.java)!!

			// 수정할 안부 데이터 세팅
			// 이름 세팅
			findViewById<EditText>(R.id.edit_safety_name).setText(safety.name)

			// questionList 초기 세팅
			for (q in safety.questionList.keys.toList()){
				QuestionRef.child(q).get().addOnSuccessListener {
					questionList.add(Pair(q, it.getValue(QuestionDTO::class.java)) as Pair<String, QuestionDTO>)
					editSafetyAdapter.notifyDataSetChanged()
				}
			}
		}

		// 리사이클러 뷰 설정
		val rv = findViewById<RecyclerView>(R.id.edit_safety_rv)
		editSafetyAdapter = AddWardSafetyAdapter(questionList)
		rv.adapter = editSafetyAdapter
		rv.layoutManager = LinearLayoutManager(this)
		rv.setHasFixedSize(true)

		// 질문 설정 이벤트
		findViewById<ImageView>(R.id.edit_safety_setting).setOnClickListener{
			val tmpIntent = Intent(this, SafetyQuestionSettingActivity::class.java)
			tmpIntent.putExtra("questionList", questionList.toMap().keys.toCollection(ArrayList<String>()))
			questionList.clear()
			startActivityForResult(tmpIntent, 1)
		}

		// 뒤로 가기 버튼 이벤트
		findViewById<ImageView>(R.id.edit_safety_backBtn).setOnClickListener{
			finish()
		}

		// 삭제 버튼 클릭 이벤트
		findViewById<Button>(R.id.edit_safety_delete_button).setOnClickListener{
			// 해당 안부 id를 통해 데이터베이스에서 삭제
			database.getReference("safety").child(safetyId).setValue(null)
			// 로그인한 보호자의 안부 목록에서 해당 안부 삭제
			database.getReference("guardian")
				.child(owner)
				.child("safetyList")
				.child(safetyId).setValue(null)

			// 액티비티 종료
			Toast.makeText(this, "안부 삭제 완료", Toast.LENGTH_SHORT).show()
			finish()
		}

		// 저장 버튼 클릭 이벤트
		findViewById<Button>(R.id.edit_safety_save_button).setOnClickListener {

			// 안부 이름 세팅
			var name = findViewById<EditText>(R.id.edit_safety_name).text.toString()

			// 생성 날짜 세팅
			val date = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))

			// 질문을 설정한 경우에만 저장 가능
			if (questionList.isEmpty()){
				Toast.makeText(this, "질문을 설정해 주세요", Toast.LENGTH_SHORT).show()
			}
			else{
				// 이름을 설정하지 않은 경우에 "무제"로 통일
				if (name == "")
					name = "무제"

				// safety 컬렉션에 추가할 safetyDTO 생성
				val newSafety = SafetyDTO(owner, name, date, "",
					questionList.map {it.first to it.second.date}.toMap() as MutableMap<String, String>, mutableMapOf())

				// safety 컬렉션에 작성한 내용 수정
				val safetyRef = database.getReference("safety").child(safetyId)
				safetyRef.setValue(newSafety)

				// 로그인한 보호자의 안부 목록에 방금 수정한 안부 아이디 최종 변경 시간 변경
				val guardianSafetyRef = database.getReference("guardian").child(owner).child("safetyList")
				guardianSafetyRef.child(safetyId).setValue(date)

				// 액티비티 종료
				Toast.makeText(this, "안부 수정 완료", Toast.LENGTH_SHORT).show()
				finish()
			}
		}
	}

	override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
		super.onActivityResult(requestCode, resultCode, data)

		if (resultCode == Activity.RESULT_OK) {
			when (requestCode) {
				1 -> {
					editSafetyAdapter.notifyDataSetChanged()
					val checkQuestions = data?.getStringArrayListExtra("returnQuestionList")
					val QuestionRef = database.getReference("question")
					for (q in checkQuestions!!) {
						QuestionRef.child(q).get().addOnSuccessListener {
							questionList.add(
								Pair(
									q,
									it.getValue(QuestionDTO::class.java)
								) as Pair<String, QuestionDTO>
							)
							editSafetyAdapter.notifyDataSetChanged()
						}
					}
				}
			}
		}
	}
}