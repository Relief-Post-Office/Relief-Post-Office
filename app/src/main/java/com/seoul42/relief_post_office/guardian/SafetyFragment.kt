package com.seoul42.relief_post_office.guardian

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.seoul42.relief_post_office.R
import com.seoul42.relief_post_office.adapter.SafetyAdapter
import com.seoul42.relief_post_office.model.SafetyDTO
import com.seoul42.relief_post_office.safety.SafetyMake

class SafetyFragment : Fragment(R.layout.fragment_safety) {

	private val safetyList = ArrayList<SafetyDTO>()
	private lateinit var auth : FirebaseAuth
	private lateinit var safetyAdapter : SafetyAdapter

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)

		val makeSafetyBtn = view.findViewById<Button>(R.id.make_safety_btn)

		makeSafetyBtn.setOnClickListener {
			val intent = Intent(context, SafetyMake::class.java)
			startActivity(intent)
		}
		getSafetyList()
		setRecyclerView(view)
	}

	private fun getSafetyList() {
		auth = Firebase.auth

		val guardianSafetyDB = Firebase.database.getReference("guardian").child(auth.currentUser?.uid.toString()).child("safetyList")

		guardianSafetyDB.addChildEventListener(object : ChildEventListener {
			override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {

				val safetyID = snapshot.value.toString()
				setSafetyList(safetyID)
			}

			override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
			}

			override fun onChildRemoved(snapshot: DataSnapshot) {
			}

			override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {
			}

			override fun onCancelled(error: DatabaseError) {
			}
		})
	}

	private fun setSafetyList(safetyID : String) {
		val safetyDB = Firebase.database.getReference("safety").child(safetyID)

		/*safetyDB.addChildEventListener(object : ChildEventListener {
			override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
				val value = snapshot.getValue(SafetyDTO.SafetyData::class.java) as SafetyDTO.SafetyData

				safetyList.add(SafetyDTO(safetyID, value))
				safetyList.sortByDescending { it.data!!.date }
				safetyAdapter.notifyDataSetChanged()
			}

			override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
				val value = snapshot.getValue(SafetyDTO.SafetyData::class.java) as SafetyDTO.SafetyData

				for (tmp in safetyList)
				{
					if (tmp.key == safetyID)
					{
						safetyList.remove(tmp)
						break
					}
				}
				safetyList.add(SafetyDTO(safetyID, value))
				safetyList.sortByDescending { it.data!!.date }
				safetyAdapter.notifyDataSetChanged()
			}

			override fun onChildRemoved(snapshot: DataSnapshot) {
				val value = snapshot.getValue(SafetyDTO.SafetyData::class.java) as SafetyDTO.SafetyData
				val data = SafetyDTO(safetyID, value)
				safetyList.remove(data)
				safetyList.sortByDescending { it.data!!.date }
				safetyAdapter.notifyDataSetChanged()
			}

			override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {
				TODO("Not yet implemented")
			}

			override fun onCancelled(error: DatabaseError) {
				TODO("Not yet implemented")
			}

		})*/
	}

	private fun setRecyclerView(view : View) {
		val recyclerView = view.findViewById<RecyclerView>(R.id.safety_rv)
		val layout = LinearLayoutManager(context)
		safetyAdapter = SafetyAdapter(view.context, safetyList)

		recyclerView.adapter = safetyAdapter
		recyclerView.layoutManager = layout
		recyclerView.setHasFixedSize(true)
	}
}
