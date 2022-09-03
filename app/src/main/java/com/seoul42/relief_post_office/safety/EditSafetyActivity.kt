package com.seoul42.relief_post_office.safety

import android.app.Activity
import android.app.TimePickerDialog
import android.content.Intent
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.view.View
import android.view.WindowManager
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

/**
 * "보호자 안부 수정" 화면을 띄오도록 돕는 클래스
 */
class EditSafetyActivity : AppCompatActivity() {

	private val database = Firebase.database
	private val QuestionRef = database.getReference("question")
	// 안부에 포함되었다가 삭제된 질문들을 담는 리스트
	private var deletedQuestionList = arrayListOf<String>()
	// 안부에 포함된 질문들을 담는 리스트
	private var questionList = arrayListOf<Pair<String, QuestionDTO>>()
	// 로그인한 보호자의 id
	private lateinit var owner : String
	// 선택한 안부의 id
	private lateinit var safetyId : String
	private lateinit var safety : SafetyDTO
	// RecyclerView 세팅을 돕는 adapter 객체
	private lateinit var editSafetyAdapter : AddWardSafetyAdapter

	@RequiresApi(Build.VERSION_CODES.O)
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(R.layout.activity_edit_safety)

		/* 초기 세팅 */
		setData()

		/* 리사이클러 뷰 세팅 */
		setRecyclerView()

		/* 질문 설정 버튼 세팅 */
		setEditSafetyQuestionButton()

		/* 삭제 버튼 세팅 */
		setDeleteButton()

		/* 저장 버튼 세팅  */
		setSaveButton()

		/* 뒤로가기 버튼 세팅 */
		setBackButton()
	}

	/**
	 * 데이터베이스에서 정보를 불러와서 "보호자 안부 수정" 화면의 초기 세팅을 하는 메서드
	 */
	private fun setData() {
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
	}

	/**
	 * "질문 설정" 버튼을 세팅해주는 메서드
	 *  - "SafetyQuestionSettingActivity"으로 이동
	 *  - questionList를 함께 전달
	 */
	private fun setEditSafetyQuestionButton() {
		findViewById<ImageView>(R.id.edit_safety_setting).setOnClickListener{
			val tmpIntent = Intent(this, SafetyQuestionSettingActivity::class.java)
			tmpIntent.putExtra("questionList", questionList.toMap().keys.toCollection(ArrayList<String>()))
			startActivityForResult(tmpIntent, 1)
		}
	}

	/**
	 * RecyclerView를 세팅하기 위해 adapter클래스에 연결하는 메서드
	 */
	private fun setRecyclerView() {
		val rv = findViewById<RecyclerView>(R.id.edit_safety_rv)
		editSafetyAdapter = AddWardSafetyAdapter(questionList)
		rv.adapter = editSafetyAdapter
		rv.layoutManager = LinearLayoutManager(this)
		rv.setHasFixedSize(true)
	}

	/**
	 * "뒤로가기" 버튼 세팅해주는 메서드
	 *  - "SafetyFragment"로 돌아감
	 */
	private fun setBackButton() {
		findViewById<ImageView>(R.id.edit_safety_backBtn).setOnClickListener{
			finish()
		}
	}

	/**
	 * "삭제" 버튼 세팅해주는 메서드
	 *  - 삭제 과정
	 *   1. 안부 삭제
	 *   2. 보호자의 안부 목록에서 삭제
	 *   3. 안부에 포함되어 있던 질문들의 "connectedSafetyList"에서 삭제
	 */
	@RequiresApi(Build.VERSION_CODES.O)
	private fun setDeleteButton() {
		findViewById<Button>(R.id.edit_safety_delete_button).setOnClickListener{
			// 프로그레스바 처리
			it.isClickable = false
			window!!.addFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
			val progressBar = findViewById<ProgressBar>(R.id.edit_safety_progress)
			progressBar.visibility = View.VISIBLE

			// 해당 안부 id를 통해 데이터베이스에서 삭제
			database.getReference("safety").child(safetyId).setValue(null)
			// 로그인한 보호자의 안부 목록에서 해당 안부 삭제
			database.getReference("guardian")
				.child(owner)
				.child("safetyList")
				.child(safetyId).setValue(null)

			// 삭제한 안부에 포함되어있던 질문들의 connectedSafetyList에서 삭제한 안부 삭제
			for (q in questionList){
				database.getReference("question")
					.child(q.first)
					.child("connectedSafetyList")
					.child(safetyId).setValue(null)

				// 해당하는 질문들 보호자 질문 목록에서 최종 수정일 변경하기
				val date = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
				database.getReference("guardian").child(Firebase.auth.currentUser!!.uid)
					.child("questionList").child(q.first).setValue(date)
			}

			Handler().postDelayed({
				// 액티비티 종료
				Toast.makeText(this, "안부 삭제 완료", Toast.LENGTH_SHORT).show()
				finish()
			}, 100)
		}
	}

	/* 저장 버튼 세팅 */
	/**
	 * "저장" 버튼 세팅해주는 메서드
	 *  - 저장 조건
	 *   1. 안부에 할당된 질문이 1개 이상
	 */
	@RequiresApi(Build.VERSION_CODES.O)
	private fun setSaveButton() {
		findViewById<Button>(R.id.edit_safety_save_button).setOnClickListener {
			// 프로그레스바 처리
			it.isClickable = false
			window!!.addFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
			val progressBar = findViewById<ProgressBar>(R.id.edit_safety_progress)
			progressBar.visibility = View.VISIBLE

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

				/* 안부에 연결 및 연결 해제된 질문들 connectedSafetyList 동기화 */
				connectedSafetyListSync(date, 1)

				// 로그인한 보호자의 안부 목록에 방금 수정한 안부 아이디 최종 변경 시간 변경
				val guardianSafetyRef = database.getReference("guardian").child(owner).child("safetyList")
				guardianSafetyRef.child(safetyId).setValue(date)

				Handler().postDelayed({
					// 액티비티 종료
					Toast.makeText(this, "안부 수정 완료", Toast.LENGTH_SHORT).show()
					finish()
				}, 200)
			}
		}
	}

	/**
	 * 안부에 연결 및 연결 해제된 질문들을 connectedSafetyList에 동기화 해주는 함수
	 *  - flag -> 1 : 안부 수정 시
	 *  - flag -> 2 : 안부 삭제 시
	 */
	private fun connectedSafetyListSync(date : String, flag : Int) {
		when(flag){
			1-> { for (q in questionList){  // 최종 저장되는 질문들 connectedSafetyList 동기화
				val qRef = database.getReference("question").child(q.first)
					.child("connectedSafetyList")
					.child(safetyId)
				qRef.setValue(date)

				if (q.second.owner == owner) {
					// 해당하는 질문들 보호자 질문 목록에서 최종 수정일 변경하기
					database.getReference("guardian").child(Firebase.auth.currentUser!!.uid)
						.child("questionList").child(q.first).setValue(date)
				}
			}

				// 기존에 설정되었다가 이번 수정에서 빠진 질문들 connectedSafetyList 동기화
				for (q in deletedQuestionList){
					val qRef = database.getReference("question").child(q)
						.child("connectedSafetyList")
						.child(safetyId)
					qRef.setValue(null)

					// 해당하는 질문들 보호자 질문 목록에서 최종 수정일 변경하기
					database.getReference("guardian").child(Firebase.auth.currentUser!!.uid)
						.child("questionList").child(q).setValue(date)
				}
			}
			2-> {
				// 삭제한 안부에 포함되어있던 질문들의 connectedSafetyList에서 삭제한 안부 삭제
				for (q in questionList){
					database.getReference("question")
						.child(q.first)
						.child("connectedSafetyList")
						.child(safetyId).setValue(null)

					// 해당하는 질문들 보호자 질문 목록에서 최종 수정일 변경하기
					database.getReference("guardian").child(Firebase.auth.currentUser!!.uid)
						.child("questionList").child(q.first).setValue(date)
				}
			}
		}
	}

	/**
	 * 질문 설정 작업 결과를 가져오는 메서드
	 *  - "질문 설정"
	 *  	- 수정된 질문 할당 여부들을 가져와서 동기화
	 */
	override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
		super.onActivityResult(requestCode, resultCode, data)

		if (resultCode == Activity.RESULT_OK) {
			when (requestCode) {
				1 -> { // 질문 설정이 변경되었을 경우
					questionList.clear()
					editSafetyAdapter.notifyDataSetChanged()
					val checkQuestions = data?.getStringArrayListExtra("returnQuestionList")
					val QuestionRef = database.getReference("question")
					for (q in checkQuestions!!) {
						QuestionRef.child(q).get().addOnSuccessListener {
							questionList.add(Pair(q, it.getValue(QuestionDTO::class.java)) as Pair<String, QuestionDTO>)
							editSafetyAdapter.notifyDataSetChanged()
						}
					}
					deletedQuestionList = data?.getStringArrayListExtra("deletedQuestionList")!!
				}
			}
		}
	}
}