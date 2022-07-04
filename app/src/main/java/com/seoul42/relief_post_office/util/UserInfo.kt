package com.seoul42.relief_post_office.util

import android.util.Log
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.seoul42.relief_post_office.model.UserDTO

class UserInfo {

    companion object {
        val ALL_USER = mutableMapOf<String, UserDTO>() /* 모든 유저 정보 */
    }

    init {
        setAllUser()
    }

    private fun setAllUser() {
        val userDB = Firebase.database.reference.child("user")

        /* 유저 정보가 삭제되지 않는다는 가정하에 구현 */
        userDB.addChildEventListener(object : ChildEventListener {
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                val userId = snapshot.key.toString()
                val userValue = snapshot.getValue(UserDTO::class.java) as UserDTO
                ALL_USER[userId] = userValue
            }
            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
                val userId = snapshot.key.toString()
                val userValue = snapshot.getValue(UserDTO::class.java) as UserDTO
                ALL_USER[userId] = userValue
            }
            override fun onChildRemoved(snapshot: DataSnapshot) {}
            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onCancelled(error: DatabaseError) {}
        })
    }
}