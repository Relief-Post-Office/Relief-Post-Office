package com.seoul42.relief_post_office.safety

import android.app.Activity
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
import com.seoul42.relief_post_office.adapter.SafetyQuestionSettingAdapter
import com.seoul42.relief_post_office.model.QuestionDTO
import com.seoul42.relief_post_office.model.SafetyDTO
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * "보호자 안부 추가" 화면을 띄오도록 돕는 클래스
 */
class SafetyMake : AppCompatActivity() {

	private val database = Firebase.database
	private val safetyRef = database.getReference("safety")
	// 안부에 할당된 질문들을 담는 리스트
	private var questionList = arrayListOf<Pair<String, QuestionDTO>>()
	// RecyclerView 세팅을 돕는 adapter 객체
	private lateinit var safetyMakeAdapter: AddWardSafetyAdapter
	// 로그인한 보호자 id
	private lateinit var owner : String

	@RequiresApi(Build.VERSION_CODES.O)
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(R.layout.activity_safety_make)

		// 로그인 한 보호자 아이디 가져오기
		owner = Firebase.auth.currentUser?.uid.toString()

		/* 질문 설정 버튼 세팅 */
		setEditSafetyQuestionButton()

		/* 리사이클러 뷰 세팅 */
		setRecyclerView()

		/* 저장 버튼 세팅*/
		setSaveButton()

		/* 뒤로 가기 버튼 세팅 */
		setBackButton()
	}

	/**
	 * "뒤로가기" 버튼 세팅해주는 메서드
	 *  - "SafetyFragment"로 돌아감
	 */
	private fun setBackButton(){
		findViewById<ImageView>(R.id.safety_make_backBtn).setOnClickListener{
			finish()
		}
	}

	/**
	 * "질문 설정" 버튼을 세팅해주는 메서드
	 *  - "SafetyQuestionSettingActivity"로 이동
	 *  - questionList를 함께 전달
	 */
	private fun setEditSafetyQuestionButton(){
		findViewById<ImageView>(R.id.safety_make_question_setting).setOnClickListener{
			val tmpIntent = Intent(this, SafetyQuestionSettingActivity::class.java)
			tmpIntent.putExtra("questionList", questionList.toMap().keys.toCollection(ArrayList<String>()))
			startActivityForResult(tmpIntent, 1)
		}
	}

	/**
	 * RecyclerView를 세팅하기 위해 adapter클래스에 연결하는 메서드
	 */
	private fun setRecyclerView() {
		val rv = findViewById<RecyclerView>(R.id.safety_make_rv)
		safetyMakeAdapter = AddWardSafetyAdapter(questionList)
		rv.adapter = safetyMakeAdapter
		rv.layoutManager = LinearLayoutManager(this)
		rv.setHasFixedSize(true)
	}

	/**
	 *  안부 저장 버튼을 세팅해주는 메서드
	 *   - 수정 데이터와 설정값을 데이터베이스에 업로드
	 *   - 안부 저장 최소 조건
	 *    2. 질문 1개 이상 설정
	 */
	@RequiresApi(Build.VERSION_CODES.O)
	private fun setSaveButton() {
		findViewById<Button>(R.id.safety_make_save_button).setOnClickListener {
			// 프로그레스 바 처리
			it.isClickable = false
			window!!.addFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
			val progressBar = findViewById<ProgressBar>(R.id.safety_make_progress)
			progressBar.visibility = View.VISIBLE

			// 안부 이름 세팅
			var name = findViewById<EditText>(R.id.safety_make_name).text.toString()

			// 생성 날짜 세팅
			val date = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))


			// 질문을 설정한 경우에만 추가 가능
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

				// safety 컬렉션에 작성한 내용 추가
				val newPush = safetyRef.push()
				val key = newPush.key.toString()
				newPush.setValue(newSafety)

				// 선택한 질문들의 connectedSafetyList에 안부 추가
				for (q in questionList){
					val qRef = database.getReference("question").child(q.first)
						.child("connectedSafetyList")
						.child(key)
					qRef.setValue(date)

					// 해당하는 질문들 보호자 질문 목록에서 최종 수정일 변경하기
					database.getReference("guardian").child(owner)
						.child("questionList").child(q.first).setValue(date)
				}

				// 로그인한 보호자의 안부 목록에 방금 등록한 안부 아이디 추가
				val wardSafetyRef = database.getReference("guardian").child(owner).child("safetyList")
				wardSafetyRef.child(key).setValue(date)

				Handler().postDelayed({
					// 액티비티 종료
					Toast.makeText(this, "안부 추가 완료", Toast.LENGTH_SHORT).show()
					finish()
				}, 200)
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

		if (resultCode == Activity.RESULT_OK){
			when (requestCode){
				1 -> {
					questionList.clear()
					safetyMakeAdapter.notifyDataSetChanged()
					val checkQuestions = data?.getStringArrayListExtra("returnQuestionList")
					val QuestionRef = database.getReference("question")
					for (q in checkQuestions!!){
						QuestionRef.child(q).get().addOnSuccessListener {
							questionList.add(Pair(q, it.getValue(QuestionDTO::class.java)) as Pair<String, QuestionDTO>)
							safetyMakeAdapter.notifyDataSetChanged()
						}
					}
				}
			}
		}
	}

}