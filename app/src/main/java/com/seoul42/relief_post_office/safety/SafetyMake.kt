package com.seoul42.relief_post_office.safety

import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import androidx.annotation.RequiresApi
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.seoul42.relief_post_office.R
import com.seoul42.relief_post_office.model.SafetyDTO
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class SafetyMake : AppCompatActivity() {

	private lateinit var auth : FirebaseAuth
	private val database = Firebase.database
	private val safetyRef = database.getReference("safety")

	@RequiresApi(Build.VERSION_CODES.O)
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(R.layout.activity_safety_make)

		val safetyTitle = findViewById<EditText>(R.id.safety_title)
		val writeSafetyBtn = findViewById<Button>(R.id.write_btn)

		writeSafetyBtn.setOnClickListener {

			val current = LocalDateTime.now()
			val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
			val formatted = current.format(formatter)

			auth = Firebase.auth
			val guardianSafetyRef = database.getReference("guardian").child(auth.currentUser?.uid.toString()).child("safetyList")

			val push = safetyRef.push()
			val key = push.key

			// 데이터 추가 arrayList 형태로 넣기
			push.child("safetyBody").setValue(
				SafetyDTO.SafetyData(auth.currentUser?.uid.toString(), safetyTitle.text.toString(), formatted.toString())
			)

			guardianSafetyRef.push().setValue(key.toString())

			finish()
		}
	}
}