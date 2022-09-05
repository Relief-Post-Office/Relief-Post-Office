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
import com.google.firebase.database.ktx.getValue
import com.google.firebase.ktx.Firebase
import com.seoul42.relief_post_office.R
import com.seoul42.relief_post_office.adapter.ResponseAdapter
import com.seoul42.relief_post_office.adapter.WardAdapter
import com.seoul42.relief_post_office.alarm.WardReceiver
import com.seoul42.relief_post_office.databinding.WardBinding
import com.seoul42.relief_post_office.model.*
import com.seoul42.relief_post_office.service.CheckLoginService
import com.seoul42.relief_post_office.viewmodel.FirebaseViewModel

/**
 * 피보호자의 메인 화면을 띄우도록 돕는 클래스
 */
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

    // 데이터베이스 참조 변수
    private val userDB = Firebase.database.reference.child("user")
    private val wardDB = Firebase.database.reference.child("ward")
    private val guardianDB = Firebase.database.reference.child("guardian")

    // 실시간으로 연결된 보호자들을 담도록 하는 리스트
    private val connectedGuardianList = ArrayList<Pair<String, UserDTO>>()

    // 등록한 데이터베이스 리스너들을 담는 리스트
    private val listenerList = ArrayList<ListenerDTO>()

    // FCM 푸시 알람을 처리하도록 돕는 변수
    private val firebaseViewModel : FirebaseViewModel by viewModels()

    // 현재 피보호자의 정보
    private lateinit var userDTO : UserDTO

    // 데이터베이스에 존재했던 피보호자와 연결된 보호자들을 담은 리스트
    private lateinit var connectList : MutableMap<String, String>

    // 데이터베이스에 존재했던 피보호자에게 보호자 요청을 한 보호자들을 담은 리스트
    private lateinit var requestList : MutableMap<String, String>

    // 프로필 변경 화면에서 정상적으로 수정 작업이 이뤄진 경우
    // 변경된 객체를 받아서 현재 화면에 적용될 수 있도록 돕는 변수
    private lateinit var activityResultLauncher: ActivityResultLauncher<Intent>

    // 피보호자와 연결된 보호자들을 처리할 수 있는 어댑터 및 응답 어댑터
    private lateinit var wardAdapter : WardAdapter
    private lateinit var responseAdapter : ResponseAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        ignoreBatteryOptimization()
        getGuardianListAndSetWard()
        activityResultLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { profileActivity ->
            // 정상적으로 프로필 변경 완료시 변경 정보를 받고 변경된 사진을 적용
            if (profileActivity.resultCode == RESULT_OK) {
                userDTO = profileActivity.data?.getSerializableExtra("userDTO") as UserDTO
                Glide.with(this)
                    .load(userDTO.photoUri)
                    .circleCrop()
                    .into(binding.wardPhoto)
            }
        }
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

    /**
     *  화면이 완전히 종료시 데이터베이스 관련 이벤트 리스너들을 해제하도록 함
     */
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

    /**
     *  2 가지 케이스로 분류하여 보호자 정보를 띄우도록 돕는 메서드
     *   1. 이미 연결된 보호자가 있는 경우
     *   2. 보호자가 없는 경우
     */
    private fun getGuardianListAndSetWard() {
        userDTO = intent.getSerializableExtra("userDTO") as UserDTO
        wardDB.child(myUserId).get().addOnSuccessListener { wardSnapshot ->
            if (wardSnapshot.getValue(WardDTO::class.java) != null) {
                val wardDTO = wardSnapshot.getValue(WardDTO::class.java) as WardDTO

                connectList = wardDTO.connectList
                requestList = wardDTO.requestList
                setUp()
            } else {
                // nullPointerException 방지로 빈 객체 할당
                connectList = mutableMapOf()
                requestList = mutableMapOf()
                setUp()
            }
        }
    }

    /**
     * 총 4가지 세팅 작업이 이뤄짐
     *  1. 피보호자 알람 세팅
     *  2. 피보호자 프로필 사진 세팅
     *  3. 피보호자와 연결된 보호자들의 리사이클러뷰 세팅
     *  4. 요청온 보호자의 추가 버튼에 대한 리스터 세팅
     *  5. 음성 답변 기능(STT) 사용 여부 세팅
     */
    private fun setUp() {
        setAlarm()
        setWardPhoto()
        setRecyclerView()
        setAddButton()
        setSttButton()
    }

    /**
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

    /**
     *  연결된 보호자들을 확인할 수 있도록 하는 리사이클러뷰를 설정하도록 돕는 메서드
     */
    private fun setRecyclerView() {
        val wardLayout = LinearLayoutManager(this)

        wardAdapter = WardAdapter(this, connectedGuardianList)
        binding.wardRecyclerView.adapter = wardAdapter
        binding.wardRecyclerView.layoutManager = wardLayout
        binding.wardRecyclerView.setHasFixedSize(true)

        val connectDB = wardDB.child(myUserId).child("connectList")
        // 연결된 피보호자가 실시간으로 recyclerView 에 적용하도록 리스너 설정
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

        val requestDB = wardDB.child(myUserId).child("requestList")
        // 요청온 보호자의 수를 실시간으로 반영
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

    /**
     * 음성 답변 기능(Stt) 옵션에 대한 스위치를 세팅해주는 메서드
     *  - 데이터베이스의 ward 컬렉션 하위항목 "stt"의 값이
     *      - true : 기능 On
     *      - null or false : 기능 Off
     *  - 기능 On/Off에 따라 텍스트 색상 변경
     *      - 텍스트 색상이 붉은색 : 기능 On
     *      - 텍스트 색상이 회색 : 기능 Off
     */
    private fun setSttButton() {
        wardDB.child(myUserId).child("stt").get().addOnSuccessListener { snapshot ->
            var isActivated = snapshot.getValue() as Boolean? ?: false

            if (isActivated)
                binding.sttBtn.setTextColor(resources.getColor(R.color.red))

            binding.sttBtn.setOnClickListener {
                if (isActivated) {
                    wardDB.child(myUserId).child("stt").setValue(false).addOnSuccessListener {
                        binding.sttBtn.setTextColor(resources.getColor(R.color.gray))
                        isActivated = false
                        Toast.makeText(this, "음성 답변 기능 비활성화", Toast.LENGTH_LONG).show()
                    }
                }
                else {
                    wardDB.child(myUserId).child("stt").setValue(true).addOnSuccessListener {
                        binding.sttBtn.setTextColor(resources.getColor(R.color.pink))
                        isActivated = true
                        Toast.makeText(this, "음성 답변 기능 활성화", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

    /**
     * 피보호자에게 보호자 권한 요청을 한 보호자들을 추가할지에 대한 다이얼로그를 띄우는 메서드
     * 보호자를 선택하고 추가 버튼을 누를 시 선택된 목록이 비어있지 않을 때 추가시킴
     */
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

    /**
     * 선택된 보호자들을 추가하도록 돕는 메서드
     * 보호자들을 추가하도록 하기 위한 과정
     *  1. 피보호자의 요청 목록을 제거
     *  2. 피보호자는 선택한 보호자와 연결
     *  3. 선택된 보호자는 피보호자와 연결
     *  4. 보호자에게 FCM 푸시 알람을 송신
     */
    @SuppressLint("NotifyDataSetChanged")
    private fun userConnection(checkList : ArrayList<String>) {
        wardDB.child(myUserId).child("requestList").removeValue()

        for (checkId in checkList) {
            wardDB.child(myUserId).child("connectList").child(checkId).setValue(checkId)
            addConnectedGuardianList(connectedGuardianList, checkId)
            guardianDB.child(checkId).child("connectList").child(myUserId).setValue(myUserId)
            userDB.child(checkId).get().addOnSuccessListener { userSnapshot ->
                val guardian = userSnapshot.getValue(UserDTO::class.java) ?: throw IllegalArgumentException("user required")
                val notificationData = NotificationDTO.NotificationData("안심 집배원"
                    , userDTO.name, userDTO.name + "님이 요청을 수락했습니다.")
                val notificationDTO = NotificationDTO(guardian.token, "high", notificationData)

                firebaseViewModel.sendNotification(notificationDTO) // FCM 전송
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
                // 실시간으로 요청온 유저들을 추가 반영
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
                // 실시간으로 선택한 유저들을 추가 반영
                wardAdapter.notifyDataSetChanged()
            }
        }
    }
}