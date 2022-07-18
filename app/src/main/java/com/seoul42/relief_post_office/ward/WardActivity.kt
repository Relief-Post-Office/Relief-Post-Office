package com.seoul42.relief_post_office.ward

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import android.widget.*
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.registerForActivityResult
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.recyclerview.widget.LinearLayoutManager
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
import com.seoul42.relief_post_office.adapter.ResponseAdapter
import com.seoul42.relief_post_office.adapter.WardAdapter
import com.seoul42.relief_post_office.alarm.WardReceiver
import com.seoul42.relief_post_office.service.CheckLoginService
import com.seoul42.relief_post_office.viewmodel.FirebaseViewModel
import com.seoul42.relief_post_office.databinding.WardBinding
import com.seoul42.relief_post_office.model.*

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

    private lateinit var userDTO : UserDTO
    private lateinit var connectList : MutableMap<String, String>
    private lateinit var requestList : MutableMap<String, String>
    private lateinit var wardAdapter : WardAdapter
    private lateinit var responseAdapter : ResponseAdapter
    private lateinit var activityResultLauncher: ActivityResultLauncher<Intent>

    private val connectedGuardianList = ArrayList<Pair<String, UserDTO>>()
    private val listenerList = ArrayList<ListenerDTO>()
    private val firebaseViewModel : FirebaseViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        activityResultLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult())
            {
                if (it.resultCode == RESULT_OK) {
                    userDTO = it.data?.getSerializableExtra("userDTO") as UserDTO
                    Glide.with(this)
                        .load(userDTO.photoUri)
                        .circleCrop()
                        .into(binding.wardPhoto)
                }
            }
        getWard()
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

    private fun getWard() {
        val wardDB = Firebase.database.reference.child("ward").child(myUserId)

        userDTO = intent.getSerializableExtra("userDTO") as UserDTO
        wardDB.get().addOnSuccessListener {
            if (it.getValue(WardDTO::class.java) != null) {
                val wardDTO = it.getValue(WardDTO::class.java) as WardDTO

                connectList = wardDTO.connectList
                requestList = wardDTO.requestList
                setAlarm()
                setLogout()
                setWardPhoto()
                setRecyclerView()
                setAddButton()
            } else {
                connectList = mutableMapOf()
                requestList = mutableMapOf()
                setAlarm()
                setLogout()
                setWardPhoto()
                setRecyclerView()
                setAddButton()
            }
        }
    }

    /*
     * 알람 요청 작업을 수행할 수 있도록 설정
     */
    private fun setAlarm() {
        val start = Intent(WardReceiver.REPEAT_START)

        start.setClass(this, WardReceiver::class.java)
        sendBroadcast(start, WardReceiver.PERMISSION_REPEAT)
    }

    private fun setLogout() {
        binding.wardLogout.buttonColor = resources.getColor(R.color.pink)
        binding.wardLogout.cornerRadius = 30
        binding.wardLogout.setOnClickListener {
            auth.signOut()
            ActivityCompat.finishAffinity(this)
            startActivity(Intent(this, CheckLoginService::class.java))
        }
    }

    private fun setWardPhoto() {
        Glide.with(this)
            .load(userDTO.photoUri)
            .circleCrop()
            .into(binding.wardPhoto)
        binding.wardPhoto.setOnClickListener {
            val intent = Intent(this, WardProfileActivity::class.java)

            intent.putExtra("userDTO", userDTO)
            activityResultLauncher.launch(intent)
        }
    }

    private fun setRecyclerView() {
        val wardLayout = LinearLayoutManager(this)
        val connectDB = Firebase.database.reference.child("ward").child(myUserId).child("connectList")

        wardAdapter = WardAdapter(this, connectedGuardianList)

        binding.wardRecyclerView.adapter = wardAdapter
        binding.wardRecyclerView.layoutManager = wardLayout
        binding.wardRecyclerView.setHasFixedSize(true)

        /* 연결된 피보호자가 실시간으로 recyclerView 에 적용하도록 리스너 설정 */
        val connectListener = connectDB.addChildEventListener(object : ChildEventListener {
            @SuppressLint("NotifyDataSetChanged")
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                val connectedUserId = snapshot.key.toString()

                connectList[connectedUserId] = connectedUserId
                addConnectedGuardianList(connectedGuardianList, connectedUserId)
            }
            @SuppressLint("NotifyDataSetChanged")
            override fun onChildRemoved(snapshot: DataSnapshot) {
                val connectedUserId = snapshot.key.toString()

                connectList.remove(connectedUserId)
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

        binding.wardAddGuardian.buttonColor = resources.getColor(R.color.pink)
        binding.wardAddGuardian.cornerRadius = 30
        binding.wardAddGuardian.setOnClickListener {
            if (requestList.isEmpty()) {
                Toast.makeText(this, "추가하실 보호자 정보가 없습니다.", Toast.LENGTH_SHORT).show()
            } else {
                setAddDialog()
            }
        }

        /* 요청온 보호자의 수를 실시간으로 반영 */
        val requestListener = requestDB.addChildEventListener(object : ChildEventListener {
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                val key = snapshot.key.toString()
                val uid = snapshot.value.toString()

                requestList[key] = uid
                binding.wardAddGuardian.text = requestList.size.toString()
            }
            override fun onChildRemoved(snapshot: DataSnapshot) {
                val key = snapshot.key.toString()

                requestList.remove(key)
                binding.wardAddGuardian.text = requestList.size.toString()
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
        responseDialog.show(responseAdapter, responseLayout, resources)
        responseDialog.setOnAddClickedListener {
            if (responseAdapter.getCheckList().isNotEmpty()) {
                Toast.makeText(this, "보호자가 추가되었습니다!", Toast.LENGTH_SHORT).show()
                userConnection(responseAdapter.getCheckList())
                binding.wardAddGuardian.text = "0"
            }
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
            wardDB.child(checkId).setValue(checkId)
            addConnectedGuardianList(connectedGuardianList, checkId)
            /* 선택된 보호자는 피보호자와 연결 */
            guardianDB = Firebase.database.reference.child("guardian").child(checkId).child("connectList")
            guardianDB.child(myUserId).setValue(myUserId)
            /* 보호자에게 FCM 보내기 */
            userDB = Firebase.database.reference.child("user").child(checkId)
            userDB.get().addOnSuccessListener {
                val guardian = it.getValue(UserDTO::class.java) as UserDTO
                val notificationData = NotificationDTO.NotificationData("안심 집배원"
                    , userDTO.name, userDTO.name + "님이 요청을 수락했습니다.")
                val notificationDTO = NotificationDTO(guardian.token, notificationData)
                firebaseViewModel.sendNotification(notificationDTO) /* FCM 전송하기 */
            }
        }
    }

    private fun getRequestedGuardianList() : ArrayList<Pair<String, UserDTO>> {
        val requestedGuardianList = ArrayList<Pair<String, UserDTO>>()

        for (requestGuardian in requestList) {
            addRequestedGuardianList(requestedGuardianList, requestGuardian.key)
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