package com.seoul42.relief_post_office.ward

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.*
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
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
import com.seoul42.relief_post_office.adapter.ResponseAdapter
import com.seoul42.relief_post_office.adapter.WardAdapter
import com.seoul42.relief_post_office.alarm.WardReceiver
import com.seoul42.relief_post_office.databinding.WardBinding
import com.seoul42.relief_post_office.model.ListenerDTO
import com.seoul42.relief_post_office.model.NotificationDTO
import com.seoul42.relief_post_office.model.UserDTO
import com.seoul42.relief_post_office.service.CheckLoginService
import com.seoul42.relief_post_office.util.Alarm.isIgnoringBatteryOptimizations
import com.seoul42.relief_post_office.util.Ward
import com.seoul42.relief_post_office.viewmodel.FirebaseViewModel

class WardActivity : AppCompatActivity() {

    private val auth: FirebaseAuth by lazy {
        Firebase.auth
    }
    private val myUserId: String by lazy {
        Firebase.auth.uid.toString()
    }
    private val binding: WardBinding by lazy {
        WardBinding.inflate(layoutInflater)
    }

    private lateinit var wardAdapter : WardAdapter
    private lateinit var responseAdapter : ResponseAdapter
    private val connectedGuardianList = ArrayList<Pair<String, UserDTO>>()
    private val listenerList = ArrayList<ListenerDTO>()
    private val firebaseViewModel : FirebaseViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

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
        binding.wardLogout.setOnClickListener {
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
            .into(binding.wardPhoto)
        binding.wardPhoto.setOnClickListener {
            startActivity(Intent(this, WardProfileActivity::class.java))
        }

        /* 프로필 편집이 완료될 경우 업데이트된 사진을 적용하도록 리스너 설정 */
        val userListener = userDB.addChildEventListener(object : ChildEventListener {
            @SuppressLint("NotifyDataSetChanged")
            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
                Glide.with(this@WardActivity)
                    .load(USER.photoUri)
                    .circleCrop()
                    .into(binding.wardPhoto)
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
        val connectDB = Firebase.database.reference.child("ward").child(myUserId).child("connectList")

        setConnectedGuardianList()
        wardAdapter = WardAdapter(this, connectedGuardianList)

        binding.wardRecyclerView.adapter = wardAdapter
        binding.wardRecyclerView.layoutManager = wardLayout
        binding.wardRecyclerView.setHasFixedSize(true)

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
        val requestDB = Firebase.database.reference.child("ward").child(myUserId).child("requestList")

        binding.wardAddGuardian.setOnClickListener {
            if (REQUEST_LIST.isEmpty()) {
                Toast.makeText(this, "추가하실 보호자 정보가 없습니다.", Toast.LENGTH_SHORT).show()
            } else {
                setAddDialog()
            }
        }

        /* 요청온 보호자의 수를 실시간으로 반영 */
        val requestListener = requestDB.addChildEventListener(object : ChildEventListener {
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                binding.wardAddGuardian.text = REQUEST_LIST.size.toString()
            }
            override fun onChildRemoved(snapshot: DataSnapshot) {
                binding.wardAddGuardian.text = REQUEST_LIST.size.toString()
            }
            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onCancelled(error: DatabaseError) {}
        })
        listenerList.add(ListenerDTO(requestDB, requestListener))
    }

    private fun setAddDialog() {
        val responseDialog = ResponseDialog(this)
        val responseLayout = LinearLayoutManager(this)

        responseAdapter = ResponseAdapter(this, getRequestedGuardianList())
        responseDialog.show(responseAdapter, responseLayout)
        responseDialog.setOnAddClickedListener {
            if (responseAdapter.getCheckList().isNotEmpty()) {
                Toast.makeText(this, "보호자가 추가되었습니다!", Toast.LENGTH_SHORT).show()
                userConnection(responseAdapter.getCheckList())
            }
            binding.wardAddGuardian.text = "0"
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun userConnection(checkList : ArrayList<String>) {
        var wardDB : DatabaseReference
        var guardianDB : DatabaseReference
        var userDB : DatabaseReference

        /* 피보호자의 요청 목록을 제거 */
        wardDB = Firebase.database.reference.child("ward").child(myUserId).child("requestList")
        wardDB.removeValue()

        for (checkId in checkList) {
            /* 피보호자는 선택한 보호자와 연결 */
            wardDB = Firebase.database.reference.child("ward").child(myUserId).child("connectList")
            wardDB.push().setValue(checkId)
            addConnectedGuardianList(connectedGuardianList, checkId)
            /* 선택된 보호자는 피보호자와 연결 */
            guardianDB = Firebase.database.reference.child("guardian").child(checkId).child("connectList")
            guardianDB.push().setValue(myUserId)
            /* 보호자에게 FCM 보내기 */
            userDB = Firebase.database.reference.child("user").child(checkId)
            userDB.get().addOnSuccessListener {
                val userDTO = it.getValue(UserDTO::class.java) as UserDTO
                val notificationData = NotificationDTO.NotificationData("안심 집배원"
                    , USER.name!!, USER.name + "님이 요청을 수락했습니다.")
                val notificationDTO = NotificationDTO(userDTO.token!!, notificationData)
                firebaseViewModel.sendNotification(notificationDTO) /* FCM 전송하기 */
            }
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