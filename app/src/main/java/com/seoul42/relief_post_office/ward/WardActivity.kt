package com.seoul42.relief_post_office.ward

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.Window
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.gms.common.internal.ServiceSpecificExtraArgs.CastExtraArgs.LISTENER
import com.google.firebase.auth.FirebaseAuth
import com.seoul42.relief_post_office.util.Ward.Companion.CONNECT_LIST
import com.seoul42.relief_post_office.util.Ward.Companion.REQUEST_LIST
import com.seoul42.relief_post_office.util.Ward.Companion.USER
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.seoul42.relief_post_office.R
import com.seoul42.relief_post_office.adapter.ResponseAdapter
import com.seoul42.relief_post_office.adapter.WardAdapter
import com.seoul42.relief_post_office.alarm.WardReceiver
import com.seoul42.relief_post_office.model.ListenerDTO
import com.seoul42.relief_post_office.model.UserDTO
import com.seoul42.relief_post_office.service.CheckLoginService
import com.seoul42.relief_post_office.util.Alarm.isIgnoringBatteryOptimizations
import com.seoul42.relief_post_office.util.Guardian
import com.seoul42.relief_post_office.util.Ward

class WardActivity : AppCompatActivity() {

    private val auth: FirebaseAuth by lazy {
        Firebase.auth
    }
    private val myUserId: String by lazy {
        Firebase.auth.uid.toString()
    }
    private val wardPhoto : ImageView by lazy {
        findViewById<ImageView>(R.id.ward_photo)
    }
    private val recyclerView : RecyclerView by lazy {
        findViewById<RecyclerView>(R.id.ward_recyclerView)
    }
    private val guardianAddButton : Button by lazy {
        findViewById<Button>(R.id.ward_add)
    }
    private val logoutButton : Button by lazy {
        findViewById<Button>(R.id.ward_logout)
    }

    private lateinit var wardAdapter : WardAdapter
    private lateinit var responseAdapter : ResponseAdapter
    private val connectedGuardianList = ArrayList<Pair<String, UserDTO>>()
    private val listenerList = ArrayList<ListenerDTO>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.ward)

        setAlarm()
        setLogout()
        setWardPhoto()
        setRecyclerView()
        setAddButton()
    }

    override fun onDestroy() {
        super.onDestroy()

        var reference : DatabaseReference
        var listener : ChildEventListener

        for (listenerInfo in listenerList) {
            reference = listenerInfo.reference
            listener = listenerInfo.listener
            reference.removeEventListener(listener)
        }
    }

    /*
     * 주기적 작업을 수행할 수 있도록 설정
     * 단, 배터리 최적화 무시를 안할 경우 피보호자 측은 강제 알람을 띄울 수 없음
     *
     */
    private fun setAlarm() {
        if (!isIgnoringBatteryOptimizations(this)) {
            val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)

            intent.data = Uri.parse("package:$packageName")
            startActivity(intent)
        } else {
            val start = Intent(WardReceiver.REPEAT_START)

            start.setClass(this, WardReceiver::class.java)
            sendBroadcast(start, WardReceiver.PERMISSION_REPEAT)
        }
    }

    private fun setLogout() {
        logoutButton.setOnClickListener {
            Ward.setLogout()
            auth.signOut()
            ActivityCompat.finishAffinity(this)
            startActivity(Intent(this, CheckLoginService::class.java))
        }
    }

    private fun setWardPhoto() {
        val userDB = Firebase.database.reference.child("user").child(myUserId)

        Glide.with(this)
            .load(USER.photoUri) /* ★★★ USER is in class of Ward ★★★ */
            .circleCrop()
            .into(wardPhoto)
        wardPhoto.setOnClickListener {
            startActivity(Intent(this, WardProfileActivity::class.java))
        }

        /* 프로필 편집이 완료될 경우 업데이트된 사진을 적용하도록 리스너 설정 */
        val userListener = userDB.addChildEventListener(object : ChildEventListener {
            @SuppressLint("NotifyDataSetChanged")
            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
                Glide.with(this@WardActivity)
                    .load(USER.photoUri)
                    .circleCrop()
                    .into(wardPhoto)
            }
            override fun onChildRemoved(snapshot: DataSnapshot) {}
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onCancelled(error: DatabaseError) {}
        })
        listenerList.add(ListenerDTO(userDB, userListener))
    }

    private fun setRecyclerView() {
        val wardLayout = LinearLayoutManager(this)
        val connectDB = Firebase.database.reference.child("ward").child(myUserId).child("connection")

        setConnectedGuardianList()
        wardAdapter = WardAdapter(this, connectedGuardianList)

        recyclerView.adapter = wardAdapter
        recyclerView.layoutManager = wardLayout
        recyclerView.setHasFixedSize(true)

        /* 연결된 피보호자가 실시간으로 recyclerView 에 적용하도록 리스너 설정 */
        val connectListener = connectDB.addChildEventListener(object : ChildEventListener {
            @SuppressLint("NotifyDataSetChanged")
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                val connectedUserId = snapshot.value.toString()
                addConnectedGuardianList(connectedGuardianList, connectedUserId)
            }
            @SuppressLint("NotifyDataSetChanged")
            override fun onChildRemoved(snapshot: DataSnapshot) {
                val connectedUserId = snapshot.value.toString()
                connectedGuardianList.removeIf {
                    it.first == connectedUserId
                }
                wardAdapter.notifyDataSetChanged()
            }
            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onCancelled(error: DatabaseError) {}
        })
        listenerList.add(ListenerDTO(connectDB, connectListener))
    }

    private fun setAddButton() {
        val requestDB = Firebase.database.reference.child("ward").child(myUserId).child("request")

        guardianAddButton.setOnClickListener {
            if (REQUEST_LIST.isEmpty()) {
                Toast.makeText(this, "추가하실 보호자 정보가 없습니다.", Toast.LENGTH_SHORT).show()
            } else {
                setAddDialog()
            }
        }

        /* 요청온 보호자의 수를 실시간으로 반영 */
        val requestListener = requestDB.addChildEventListener(object : ChildEventListener {
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                guardianAddButton.text = REQUEST_LIST.size.toString()
            }
            override fun onChildRemoved(snapshot: DataSnapshot) {
                guardianAddButton.text = REQUEST_LIST.size.toString()
            }
            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onCancelled(error: DatabaseError) {}
        })
        listenerList.add(ListenerDTO(requestDB, requestListener))
    }

    private fun setAddDialog() {
        val dialog = AlertDialog.Builder(this).create()
        val eDialog : LayoutInflater = LayoutInflater.from(this)
        val mView : View = eDialog.inflate(R.layout.dialog_response,null)
        val dialogRecyclerView : RecyclerView = mView.findViewById(R.id.response_recyclerView)
        val responseButton : Button = mView.findViewById(R.id.response_button)
        val responseLayout = LinearLayoutManager(this)

        responseAdapter = ResponseAdapter(this, getRequestedGuardianList())

        dialogRecyclerView.adapter = responseAdapter
        dialogRecyclerView.layoutManager = responseLayout
        dialogRecyclerView.setHasFixedSize(true)

        responseButton.setOnClickListener {
            Toast.makeText(this, "보호자가 추가되었습니다!", Toast.LENGTH_SHORT).show()
            userConnection(responseAdapter.getCheckList())
            guardianAddButton.text = "0"
            dialog.dismiss()
            dialog.cancel()
        }

        dialog.setView(mView)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.create()
        dialog.show()
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun userConnection(checkList : ArrayList<String>) {
        var wardDB : DatabaseReference
        var guardianDB : DatabaseReference

        /* 피보호자의 요청 목록을 제거 */
        wardDB = Firebase.database.reference.child("ward").child(myUserId).child("request")
        wardDB.removeValue()

        for (checkId in checkList) {
            /* 피보호자는 선택한 보호자와 연결 */
            wardDB = Firebase.database.reference.child("ward").child(myUserId).child("connectList")
            wardDB.push().setValue(checkId)
            addConnectedGuardianList(connectedGuardianList, checkId)
            /* 선택된 보호자는 피보호자와 연결 */
            guardianDB = Firebase.database.reference.child("guardian").child(checkId).child("connectList")
            guardianDB.push().setValue(myUserId)
        }
    }

    private fun setConnectedGuardianList() {
        for (guardian in CONNECT_LIST) {
            addConnectedGuardianList(connectedGuardianList, guardian.value)
        }
    }

    private fun getRequestedGuardianList() : ArrayList<Pair<String, UserDTO>> {
        val requestedGuardianList = ArrayList<Pair<String, UserDTO>>()

        for (requestGuardian in REQUEST_LIST) {
            addRequestedGuardianList(requestedGuardianList, requestGuardian.value)
        }
        return requestedGuardianList
    }

    private fun addRequestedGuardianList(requestedGuardianList : ArrayList<Pair<String, UserDTO>>, requestedUserId : String) {
        val userDB = Firebase.database.reference.child("user").child(requestedUserId)

        userDB.get().addOnSuccessListener {
            if (!requestedGuardianList.contains(Pair(requestedUserId, it.getValue(UserDTO::class.java) as UserDTO))) {
                requestedGuardianList.add(Pair(requestedUserId, it.getValue(UserDTO::class.java) as UserDTO))
                /* 실시간으로 요청온 유저들을 추가 반영 */
                responseAdapter.notifyDataSetChanged()
            }
        }
    }

    private fun addConnectedGuardianList(connectedGuardianList : ArrayList<Pair<String, UserDTO>>, connectedUserId : String) {
        val userDB = Firebase.database.reference.child("user").child(connectedUserId)

        userDB.get().addOnSuccessListener {
            if (!connectedGuardianList.contains(Pair(connectedUserId, it.getValue(UserDTO::class.java) as UserDTO))) {
                connectedGuardianList.add(Pair(connectedUserId, it.getValue(UserDTO::class.java) as UserDTO))
                /* 실시간으로 선택한 유저들을 추가 반영 */
                wardAdapter.notifyDataSetChanged()
            }
        }
    }
}