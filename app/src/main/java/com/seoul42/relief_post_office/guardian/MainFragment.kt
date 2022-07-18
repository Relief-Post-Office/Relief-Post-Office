package com.seoul42.relief_post_office.guardian

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.*
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
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
import com.seoul42.relief_post_office.model.WardDTO
import com.seoul42.relief_post_office.service.CheckLoginService
import com.seoul42.relief_post_office.viewmodel.FirebaseViewModel
import com.seoul42.relief_post_office.ward.WardProfileActivity

class MainFragment(private var userDTO : UserDTO) : Fragment(R.layout.fragment_guardian) {

    private val auth: FirebaseAuth by lazy {
        Firebase.auth
    }
    private val myUserId: String by lazy {
        Firebase.auth.uid.toString()
    }

    private val firebaseViewModel : FirebaseViewModel by viewModels()
    private val connectedWardList = ArrayList<Pair<String, UserDTO>>()
    private val listenerList = ArrayList<ListenerDTO>()

    private lateinit var connectList : MutableMap<String, String>
    private lateinit var guardianAdapter : GuardianAdapter
    private lateinit var binding : FragmentGuardianBinding
    private lateinit var activityResultLauncher: ActivityResultLauncher<Intent>

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?) : View {
        binding = FragmentGuardianBinding.inflate(inflater, container, false)
        activityResultLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult())
            {
                if (it.resultCode == AppCompatActivity.RESULT_OK) {
                    userDTO = it.data?.getSerializableExtra("userDTO") as UserDTO
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

        getGuardian()
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

    private fun getGuardian() {
        val guardianDB = Firebase.database.reference.child("guardian").child(myUserId)

        guardianDB.get().addOnSuccessListener {
            if (it.getValue(GuardianDTO::class.java) != null) {
                val guardianDTO = it.getValue(GuardianDTO::class.java) as GuardianDTO

                connectList = guardianDTO.connectList
                setAlarm()
                setLogout()
                setGuardianPhoto()
                setRecyclerView()
                setRequestButton()
            } else {
                connectList = mutableMapOf()
                setAlarm()
                setLogout()
                setGuardianPhoto()
                setRecyclerView()
                setRequestButton()
            }
        }
    }
    /*
     * 알람 요청 작업을 수행할 수 있도록 설정
     */
    private fun setAlarm() {
        val start = Intent(GuardianReceiver.REPEAT_START)

        start.setClass(context!!, GuardianReceiver::class.java)
        context!!.sendBroadcast(start, GuardianReceiver.PERMISSION_REPEAT)
    }

    private fun setLogout() {
        binding.guardianLogout.buttonColor = resources.getColor(R.color.pink)
        binding.guardianLogout.cornerRadius = 30
        binding.guardianLogout.setOnClickListener {
            auth.signOut()
            ActivityCompat.finishAffinity(requireActivity())
            startActivity(Intent(context, CheckLoginService::class.java))
        }
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

    private fun setRecyclerView() {
        val wardLayout = LinearLayoutManager(context)
        val connectDB = Firebase.database.reference.child("guardian").child(myUserId).child("connectList")

        guardianAdapter = GuardianAdapter(requireContext(), connectedWardList)

        binding.guardianRecyclerView.adapter = guardianAdapter
        binding.guardianRecyclerView.layoutManager = wardLayout
        binding.guardianRecyclerView.setHasFixedSize(true)

        /* 연결된 피보호자가 실시간으로 recyclerView 에 적용하도록 리스너 설정 */
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
                connectedWardList.removeIf {
                    it.first == connectedUserId
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
            val requestDialog = RequestDialog(context as AppCompatActivity, firebaseViewModel, userDTO, connectList)

            requestDialog.show(activity!!.window)
            requestDialog.setOnRequestListener { requestCase ->
                when(requestCase) {
                    0 -> Toast.makeText(context, "휴대전화번호를 정확히 입력해주세요.", Toast.LENGTH_SHORT).show()
                    1 -> Toast.makeText(context, "이미 연결된 피보호자입니다.", Toast.LENGTH_SHORT).show()
                    2 -> Toast.makeText(context, "등록되지 않은 피보호자 번호입니다.\n다시 입력해주세요.", Toast.LENGTH_SHORT).show()
                    else -> Toast.makeText(context, "등록이 완료되었습니다.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun addConnectedWardList(connectedWardList : ArrayList<Pair<String, UserDTO>>, connectedUserId : String) {
        val userDB = Firebase.database.reference.child("user").child(connectedUserId)

        userDB.get().addOnSuccessListener {
            if (it.getValue(UserDTO::class.java) != null) {
                val ward = it.getValue(UserDTO::class.java) as UserDTO
                if (!connectedWardList.contains(Pair(connectedUserId, ward))) {
                    connectedWardList.add(Pair(connectedUserId, ward))
                    guardianAdapter.notifyDataSetChanged()
                }
            }
        }
    }
}