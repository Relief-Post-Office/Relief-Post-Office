package com.seoul42.relief_post_office.guardian

import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.Window
import android.widget.*
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.seoul42.relief_post_office.R
import com.seoul42.relief_post_office.adapter.SafetyAdapter
import com.seoul42.relief_post_office.model.ListenerDTO
import com.seoul42.relief_post_office.model.QuestionDTO
import com.seoul42.relief_post_office.model.SafetyDTO
import com.seoul42.relief_post_office.safety.SafetyMake
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatter.*

/**
 * 보호자의 안부(질문 모음)탭을 띄우도록 돕는 클래스
 */
class SafetyFragment : Fragment(R.layout.fragment_safety) {

	private val database = Firebase.database
	// RecyclerView에 띄울 안부들을 담는 리스트
	private var safetyList = arrayListOf<Pair<String, SafetyDTO>>()
	// RecyclerView 세팅을 돕는 adapter 객체
	private lateinit var safetyAdapter : SafetyAdapter
	private lateinit var owner : String
	private lateinit var listenerDTO : ListenerDTO

	/**
	 * 화면이 시작될때 초기 세팅을 해주는 메서드
	 */
	@RequiresApi(Build.VERSION_CODES.O)
	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)

		// 로그인 한 사람 uid 가져오기
		owner = Firebase.auth.currentUser?.uid.toString()

		// safetyList 세팅
		setSafetyList()

		// 리사이클러 뷰 세팅
		setRecyclerView(view)

		// 안부 추가 버튼 이벤트
		val safetyAddBtn = view.findViewById<ImageView>(R.id.safety_fragment_add_button)
		safetyAddBtn.setOnClickListener{
			startActivity(Intent(context, SafetyMake::class.java))
		}
	}

	/**
	 * 화면 종료시 사용하고 있던 리스너들을 반환하는 메서드
	 */
	override fun onDestroy() {
		super.onDestroy()

		val reference : DatabaseReference = listenerDTO.reference
		val listener : ChildEventListener = listenerDTO.listener

		reference.removeEventListener(listener)
	}

	/**
	 * RecyclerView에 띄워질 safetyList를 데이터베이스에 따라 실시간으로 세팅하는 메서드
	 *  - 초기화 / 추가 / 수정 / 삭제 시 적용
	 */
	private fun setSafetyList() {
		// 로그인한 유저의 안부 목록
		val userSafetyRef = database.getReference("guardian").child(owner).child("safetyList")

		// safetyList에 로그인한 유저의 안부들 넣기
		val safetyListener = userSafetyRef.addChildEventListener(object : ChildEventListener{
			override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
				// 로그인한 유저의 안부 하나씩 참조
				val safetyId = snapshot.key.toString()
				val safetyToAdd = database.getReference("safety").child(safetyId)

				// 안부 컬렉션에서 각 안부 불러와서 safetyList에 넣기
				safetyToAdd.get().addOnSuccessListener {
					safetyList.add(Pair(safetyId, it.getValue(SafetyDTO::class.java) as SafetyDTO))
					// 리사이클러 뷰 어댑터에 알려주기
					safetyAdapter.notifyDataSetChanged()
				}
			}

			override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
				// safetyId 찾기
				val safetyId = snapshot.key.toString()
				val safetyInDB = database.getReference("safety").child(safetyId)

				// safetyList에서 safetyId에 해당하는 안부 찾아 수정해주기
				var idx = -1
				for (s in safetyList){
					idx++
					if (s.first == safetyId){
						safetyInDB.get().addOnSuccessListener {
							safetyList.remove(s)
							safetyList.add(idx, Pair(safetyId, it.getValue(SafetyDTO::class.java) as SafetyDTO))

							// 리스트가 수정되었다고 어댑터에게 알려주기
							safetyAdapter.notifyDataSetChanged()
						}
						break
					}
				}
			}

			override fun onChildRemoved(snapshot: DataSnapshot) {
				// safetyId 찾기
				val safetyId = snapshot.key.toString()

				// safetyList에서 safetyId에 해당하는 안부 찾아 삭제하기
				for (s in safetyList){
					if (s.first == safetyId) {
						safetyList.remove(s)
						// 리스트가 수정되었다고 어댑터에게 알려주기
						safetyAdapter.notifyDataSetChanged()
						break
					}
				}
			}

			override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {
			}

			override fun onCancelled(error: DatabaseError) {
			}

		})

		listenerDTO = ListenerDTO(userSafetyRef, safetyListener)
	}

	/**
	 * RecyclerView를 세팅하기 위해 adapter클래스에 연결하는 메서드
	 *  - view : "SafetyFragment"의 View
	 */
	private fun setRecyclerView(view : View) {
		val recyclerView = view.findViewById<RecyclerView>(R.id.safety_fragment_rv)
		val layout = LinearLayoutManager(context)
		safetyAdapter = SafetyAdapter(view.context, safetyList)

		recyclerView.adapter = safetyAdapter
		recyclerView.layoutManager = layout
		recyclerView.setHasFixedSize(true)
	}
}
