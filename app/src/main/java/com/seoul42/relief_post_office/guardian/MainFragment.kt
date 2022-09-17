package com.seoul42.relief_post_office.guardian

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
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
import com.seoul42.relief_post_office.adapter.GuardianAdapter
import com.seoul42.relief_post_office.alarm.GuardianReceiver
import com.seoul42.relief_post_office.databinding.FragmentGuardianBinding
import com.seoul42.relief_post_office.model.GuardianDTO
import com.seoul42.relief_post_office.model.ListenerDTO
import com.seoul42.relief_post_office.model.UserDTO
import com.seoul42.relief_post_office.util.Constants.Companion.INVALID_PHONE_NUMBER
import com.seoul42.relief_post_office.util.Constants.Companion.CONNECTED_GUARDIAN
import com.seoul42.relief_post_office.util.Constants.Companion.NON_EXIST_GUARDIAN
import com.seoul42.relief_post_office.util.Constants.Companion.REGISTER_SUCCESS
import com.seoul42.relief_post_office.viewmodel.FirebaseViewModel

/**
 *  보호자의 메인 화면을 띄우도록 돕는 클래스
 *   - userDTO : 현재 보호자의 정보가 GuardianBackgroundActivity 에서 객체로 넘어옴
 */
class MainFragment(private var userDTO : UserDTO) : Fragment(R.layout.fragment_guardian) {

    private val myUserId: String by lazy {
        Firebase.auth.uid.toString()
    }

    // 데이터베이스 참조 변수
    private val userDB = Firebase.database.reference.child("user")
    private val guardianDB = Firebase.database.reference.child("guardian")

    // FCM 푸시 알람을 처리하도록 돕는 변수
    private val firebaseViewModel : FirebaseViewModel by viewModels()

    // 실시간으로 연결된 피보호자들을 담도록 하는 리스트
    private val connectedWardList = ArrayList<Pair<String, UserDTO>>()

    // 등록한 데이터베이스 리스너들을 담는 리스트
    private val listenerList = ArrayList<ListenerDTO>()

    // 데이터베이스에 존재했던 보호자와 연결된 피보호자들을 담은 리스트
    private lateinit var connectList : MutableMap<String, String>

    // 프로필 변경 화면에서 정상적으로 수정 작업이 이뤄진 경우
    // 변경된 객체를 받아서 현재 화면에 적용될 수 있도록 돕는 변수
    private lateinit var activityResultLauncher: ActivityResultLauncher<Intent>

    // 보호자와 연결된 피보호자들을 처리할 수 있는 어댑터
    private lateinit var guardianAdapter : GuardianAdapter

    private lateinit var binding : FragmentGuardianBinding

    /**
     * 화면이 설정되기 전에 바인딩 설정
     */
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ) : View {
        binding = FragmentGuardianBinding.inflate(inflater, container, false)
        activityResultLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { profileActivity ->
            // 정상적으로 프로필 변경 완료시 변경 정보를 받고 변경된 사진을 적용
            if (profileActivity.resultCode == AppCompatActivity.RESULT_OK) {
                userDTO = profileActivity.data?.getSerializableExtra("userDTO") as UserDTO
                Glide.with(this)
                    .load(userDTO.photoUri)
                    .circleCrop()
                    .into(binding.guardianPhoto)
            }
        }
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        getWardListAndSetGuardian()
    }

    /**
     * 화면이 완전히 종료시 데이터베이스 관련 이벤트 리스너들을 해제하도록 함
     */
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
     * 2 가지 케이스로 분류하여 피보호자 정보를 띄우도록 돕는 메서드
     *  1. 이미 연결된 피보호자가 있는 경우
     *  2. 피보호자가 없는 경우
     */
    private fun getWardListAndSetGuardian() {
        guardianDB.child(myUserId).get().addOnSuccessListener { guardian->
            if (guardian.getValue(GuardianDTO::class.java) != null) {
                val guardianDTO = guardian.getValue(GuardianDTO::class.java) as GuardianDTO

                connectList = guardianDTO.connectList
                setUp()
            } else {
                // nullPointerException 방지로 빈 객체 할당
                connectList = mutableMapOf()
                setUp()
            }
        }
    }

    /**
     * 총 4가지 세팅 작업이 이뤄짐
     *  1. 보호자 알람 세팅
     *  2. 보호자 프로필 사진 세팅
     *  3. 보호자와 연결된 피보호자들의 리사이클러뷰 세팅
     *  4. 피보호자에게 보호자 권한 요청 버튼에 대한 리스터 세팅
     */
    private fun setUp() {
        setAlarm()
        setGuardianPhoto()
        setRecyclerView()
        setRequestButton()
    }

    /**
     * 보호자 알람 요청 작업을 수행할 수 있도록 설정
     */
    private fun setAlarm() {
        val start = Intent(GuardianReceiver.REPEAT_START)

        start.setClass(context!!, GuardianReceiver::class.java)
        context!!.sendBroadcast(start, GuardianReceiver.PERMISSION_REPEAT)
    }

    private fun setGuardianPhoto() {
        Glide.with(this)
            .load(userDTO.photoUri)
            .circleCrop()
            .into(binding.guardianPhoto)
        binding.guardianPhoto.setOnClickListener {
            val intent = Intent(context, GuardianProfileActivity::class.java)

            intent.putExtra("userDTO", userDTO)
            activityResultLauncher.launch(intent)
        }
    }

    /**
     * 연결된 피보호자들을 확인할 수 있도록 하는 리사이클러뷰를 설정하도록 돕는 메서드
     */
    private fun setRecyclerView() {
        val wardLayout = LinearLayoutManager(context)

        guardianAdapter = GuardianAdapter(requireContext(), connectedWardList)

        binding.guardianRecyclerView.adapter = guardianAdapter
        binding.guardianRecyclerView.layoutManager = wardLayout
        binding.guardianRecyclerView.setHasFixedSize(true)

        val connectDB = guardianDB.child(myUserId).child("connectList")
        // 연결된 피보호자가 실시간으로 recyclerView 에 적용하도록 리스너 설정
        val connectListener = connectDB.addChildEventListener(object : ChildEventListener {
            @SuppressLint("NotifyDataSetChanged")
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                val connectedUserId = snapshot.key.toString()

                connectList[connectedUserId] = connectedUserId
                addConnectedWardList(connectedWardList, connectedUserId)
            }
            @SuppressLint("NotifyDataSetChanged")
            override fun onChildRemoved(snapshot: DataSnapshot) {
                val connectedUserId = snapshot.value.toString()

                connectList.remove(connectedUserId)
                connectedWardList.removeIf { connectedWard ->
                    connectedWard.first == connectedUserId
                }
                guardianAdapter.notifyDataSetChanged()
            }
            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onCancelled(error: DatabaseError) {}
        })
        listenerList.add(ListenerDTO(connectDB, connectListener))
    }

    private fun setRequestButton() {
        binding.guardianAdd.setOnClickListener {
            val requestDialog = RequestDialog(context as AppCompatActivity, firebaseViewModel, connectList)

            requestDialog.show(activity!!.window)
            requestDialog.setOnRequestListener { requestCase ->
                requestToast(requestCase)
            }
        }
    }

    /**
     * 보호자가 피보호자의 전화번호를 입력 후 요청 시 발생되는 모든 토스트의 경우
     */
    private fun requestToast(requestCase : String) {
        when(requestCase) {
            INVALID_PHONE_NUMBER -> Toast.makeText(context, "휴대전화번호를 정확히 입력해주세요.", Toast.LENGTH_SHORT).show()
            CONNECTED_GUARDIAN -> Toast.makeText(context, "이미 연결된 피보호자입니다.", Toast.LENGTH_SHORT).show()
            NON_EXIST_GUARDIAN -> Toast.makeText(context, "등록되지 않은 피보호자 번호입니다.\n다시 입력해주세요.", Toast.LENGTH_SHORT).show()
            REGISTER_SUCCESS -> Toast.makeText(context, "등록이 완료되었습니다.", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * 보호자와 연결된 피보호자를 리사이클러뷰에 적용가능하도록 연결된 목록에 추가
     */
    private fun addConnectedWardList(
        connectedWardList : ArrayList<Pair<String, UserDTO>>,
        connectedUserId : String
    ) {
        userDB.child(connectedUserId).get().addOnSuccessListener { userSnapshot ->
            val ward = userSnapshot.getValue(UserDTO::class.java)
                ?: throw IllegalArgumentException("user required")
            if (!connectedWardList.contains(Pair(connectedUserId, ward))) {
                connectedWardList.add(Pair(connectedUserId, ward))
                // 리사이클러뷰가 즉각적으로 업데이트 가능하도록 설정
                guardianAdapter.notifyDataSetChanged()
            }
        }
    }
}