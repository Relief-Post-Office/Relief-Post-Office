package com.seoul42.relief_post_office.ward

import android.annotation.SuppressLint
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.util.Log
import android.widget.*
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.browser.customtabs.CustomTabsClient.getPackageName
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
import com.seoul42.relief_post_office.databinding.WardBinding
import com.seoul42.relief_post_office.model.*
import com.seoul42.relief_post_office.service.CheckLoginService
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

    private lateinit var userDTO : UserDTO
    private lateinit var connectList : MutableMap<String, String>
    private lateinit var requestList : MutableMap<String, String>
    private lateinit var wardAdapter : WardAdapter
    private lateinit var responseAdapter : ResponseAdapter
    private lateinit var activityResultLauncher: ActivityResultLauncher<Intent>

    private val userDB = Firebase.database.reference.child("user")
    private val wardDB = Firebase.database.reference.child("ward")
    private val guardianDB = Firebase.database.reference.child("guardian")
    private val connectedGuardianList = ArrayList<Pair<String, UserDTO>>()
    private val listenerList = ArrayList<ListenerDTO>()
    private val firebaseViewModel : FirebaseViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        ignoreBatteryOptimization()
        activityResultLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { profileActivity ->
            if (profileActivity.resultCode == RESULT_OK) {
                userDTO = profileActivity.data?.getSerializableExtra("userDTO") as UserDTO
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

    private fun ignoreBatteryOptimization() {
        val intent = Intent()
        val packageName = packageName
        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager

        if (!pm.isIgnoringBatteryOptimizations(packageName)) {
            intent.action = Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
            intent.data = Uri.parse("package:$packageName")
            startActivity(intent)
        }
    }

    private fun getWard() {
        userDTO = intent.getSerializableExtra("userDTO") as UserDTO
        wardDB.child(myUserId).get().addOnSuccessListener { wardSnapshot ->
            if (wardSnapshot.getValue(WardDTO::class.java) != null) {
                val wardDTO = wardSnapshot.getValue(WardDTO::class.java) as WardDTO

                connectList = wardDTO.connectList
                requestList = wardDTO.requestList
                setUp()
            } else {
                connectList = mutableMapOf()
                requestList = mutableMapOf()
                setUp()
            }
        }
    }

    private fun setUp() {
        setAlarm()
        setWardPhoto()
        setRecyclerView()
        setAddButton()
    }

    /*
     * 알람 요청 작업을 수행할 수 있도록 설정
     */
    private fun setAlarm() {
        val start = Intent(WardReceiver.REPEAT_START)

        start.setClass(this, WardReceiver::class.java)
        sendBroadcast(start, WardReceiver.PERMISSION_REPEAT)
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

        wardAdapter = WardAdapter(this, connectedGuardianList)
        binding.wardRecyclerView.adapter = wardAdapter
        binding.wardRecyclerView.layoutManager = wardLayout
        binding.wardRecyclerView.setHasFixedSize(true)

        /* 연결된 피보호자가 실시간으로 recyclerView 에 적용하도록 리스너 설정 */
        val connectDB = wardDB.child(myUserId).child("connectList")
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
                connectedGuardianList.removeIf { connectedGuardian ->
                    connectedGuardian.first == connectedUserId
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
        val requestDB = wardDB.child(myUserId).child("requestList")
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
        /* 피보호자의 요청 목록을 제거 */
        wardDB.child(myUserId).child("requestList").removeValue()

        for (checkId in checkList) {
            /* 피보호자는 선택한 보호자와 연결 */
            wardDB.child(myUserId).child("connectList").child(checkId).setValue(checkId)
            addConnectedGuardianList(connectedGuardianList, checkId)
            /* 선택된 보호자는 피보호자와 연결 */
            guardianDB.child(checkId).child("connectList").child(myUserId).setValue(myUserId)
            /* 보호자에게 FCM 보내기 */
            userDB.child(checkId).get().addOnSuccessListener { userSnapshot ->
                val guardian = userSnapshot.getValue(UserDTO::class.java) ?: throw IllegalArgumentException("user required")
                val notificationData = NotificationDTO.NotificationData("안심 집배원"
                    , userDTO.name, userDTO.name + "님이 요청을 수락했습니다.")
                val notificationDTO = NotificationDTO(guardian.token, "high", notificationData)

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

    private fun addRequestedGuardianList(
        requestedGuardianList : ArrayList<Pair<String, UserDTO>>,
        requestedUserId : String
    ) {
        userDB.child(requestedUserId).get().addOnSuccessListener { userSnapshot ->
            val userDTO = userSnapshot.getValue(UserDTO::class.java)
                ?: throw IllegalArgumentException("user required")
            val requestedGuardian = Pair(requestedUserId, userDTO)

            if (!requestedGuardianList.contains(requestedGuardian)) {
                requestedGuardianList.add(requestedGuardian)
                /* 실시간으로 요청온 유저들을 추가 반영 */
                responseAdapter.notifyDataSetChanged()
            }
        }
    }

    private fun addConnectedGuardianList(
        connectedGuardianList : ArrayList<Pair<String, UserDTO>>,
        connectedUserId : String
    ) {
        userDB.child(connectedUserId).get().addOnSuccessListener { userSnapshot ->
            val userDTO = userSnapshot.getValue(UserDTO::class.java)
                ?: throw IllegalArgumentException("user required")
            val connectedGuardian = Pair(connectedUserId, userDTO)

            if (!connectedGuardianList.contains(connectedGuardian)) {
                connectedGuardianList.add(connectedGuardian)
                /* 실시간으로 선택한 유저들을 추가 반영 */
                wardAdapter.notifyDataSetChanged()
            }
        }
    }
}