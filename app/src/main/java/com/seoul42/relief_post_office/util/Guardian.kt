package com.seoul42.relief_post_office.util

import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.seoul42.relief_post_office.model.ListenerDTO
import com.seoul42.relief_post_office.model.UserDTO

class Guardian(user : UserDTO) {

    private val userId : String by lazy {
        Firebase.auth.uid.toString()
    }

    companion object {
        lateinit var USER : UserDTO /* 현재 로그인한 유저 정보 */
        val CONNECT_LIST : MutableMap<String, String> = mutableMapOf()
        private val LISTENER = ArrayList<ListenerDTO>()

        /* 로그아웃 시 등록된 리스너 및 Collection 초기화 작업 */
        fun setLogout() {
            var reference : DatabaseReference
            var listener : ChildEventListener

            for (listenerInfo in LISTENER) {
                reference = listenerInfo.reference
                listener = listenerInfo.listener
                reference.removeEventListener(listener)
            }
            CONNECT_LIST.clear()
        }
    }

    init {
        USER = user
        setConnectedUser()
    }

    private fun setConnectedUser() {
        val connectedDB = Firebase.database.reference.child("guardian").child(userId).child("connectList")

        /* 연결된 피보호자를 자동으로 추가하거나 제거 가능 */
        val connectedListener = connectedDB.addChildEventListener(object : ChildEventListener {
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                val key = snapshot.key.toString()
                val connectedUserId = snapshot.value.toString()
                CONNECT_LIST[key] = connectedUserId
            }
            override fun onChildRemoved(snapshot: DataSnapshot) {
                val key = snapshot.key.toString()
                CONNECT_LIST.remove(key)
            }
            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onCancelled(error: DatabaseError) {}
        })
        LISTENER.add(ListenerDTO(connectedDB, connectedListener))
    }
}