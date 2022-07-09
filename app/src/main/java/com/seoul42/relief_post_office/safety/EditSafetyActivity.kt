package com.seoul42.relief_post_office.safety

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.seoul42.relief_post_office.R
import com.seoul42.relief_post_office.model.SafetyDTO
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

// recylcerview 설정
//


class EditSafetyActivity : AppCompatActivity() {

	private lateinit var auth : FirebaseAuth
	val database = Firebase.database

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(R.layout.activity_edit_safety)

		val safetyEditText = findViewById<EditText>(R.id.safety_edit_text)
		val safetyEditBtn = findViewById<Button>(R.id.safety_edit_btn)
		val safetyDelBtn = findViewById<Button>(R.id.safety_del_btn)

		val safetyDTO = intent.getSerializableExtra("safetyDTO") as SafetyDTO?

		//safetyEditText.setText(safetyDTO!!.data!!.content)

		safetyEditBtn.setOnClickListener {
			val current = LocalDateTime.now()
			val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
			val formatted = current.format(formatter)

			//val safetyDB = database.getReference("safety").child(safetyDTO.key).child("safetyBody")

			auth = Firebase.auth

			/*safetyDB.setValue(
				SafetyDTO.SafetyData(safetyDTO!!.data!!.uid, safetyEditText.text.toString(), formatted.toString())
			)*/
			finish()
		}


		safetyDelBtn.setOnClickListener {
			//val safetyRef = database.getReference("safety").child(safetyDTO.key)
			//val guardianSafetyRef = database.getReference("guardian").child(safetyDTO!!.data!!.uid).child("safetyList")

			/*guardianSafetyRef.get().addOnSuccessListener {
				for (child in it.children) {
					if (child.value == safetyDTO.key)
					{
						val tempRef = guardianSafetyRef.child(child.key.toString())

						tempRef.removeValue()
						break
					}
				}
				safetyRef.removeValue()
				finish()
			}*/
		}
	}
}