package com.seoul42.relief_post_office.util

import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.seoul42.relief_post_office.model.UserDTO

class Ward(user : UserDTO) {

    private val userId : String by lazy {
        Firebase.auth.uid.toString()
    }

    companion object {
        lateinit var USER : UserDTO /* 현재 로그인한 유저 정보 */
        val CONNECT_GUARDIAN = mutableSetOf<String>()
        val REQUEST_GUARDIAN = mutableSetOf<String>()
    }

    init {
        USER = user
        setConnectedUser()
        setRequestedUser()
    }

    private fun setConnectedUser() {
        val connectedDB = Firebase.database.reference.child("ward").child(userId).child("connection")

        /* 연결된 보호자를 자동으로 추가하거나 제거 가능 */
        connectedDB.addChildEventListener(object : ChildEventListener {
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                val connectedUserId = snapshot.value.toString()
                CONNECT_GUARDIAN += connectedUserId
            }
            override fun onChildRemoved(snapshot: DataSnapshot) {
                val connectedUserId = snapshot.value.toString()
                CONNECT_GUARDIAN -= connectedUserId
            }
            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun setRequestedUser() {
        val requestedDB = Firebase.database.reference.child("ward").child(userId).child("request")

        /* 요청온 보호자를 자동으로 추가하거나 제거 가능 */
        requestedDB.addChildEventListener(object : ChildEventListener {
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                val requestedUserId = snapshot.value.toString()
                REQUEST_GUARDIAN += requestedUserId
            }
            override fun onChildRemoved(snapshot: DataSnapshot) {
                val requestedUserId = snapshot.value.toString()
                REQUEST_GUARDIAN -= requestedUserId
            }
            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onCancelled(error: DatabaseError) {}
        })
    }
}