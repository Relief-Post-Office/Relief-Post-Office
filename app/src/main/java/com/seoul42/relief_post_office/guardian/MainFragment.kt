package com.seoul42.relief_post_office.guardian

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.seoul42.relief_post_office.util.Guardian.Companion.USER
import com.seoul42.relief_post_office.util.Guardian.Companion.CONNECT_LIST
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.seoul42.relief_post_office.R
import com.seoul42.relief_post_office.adapter.GuardianAdapter
import com.seoul42.relief_post_office.databinding.FragmentGuardianBinding
import com.seoul42.relief_post_office.model.ListenerDTO
import com.seoul42.relief_post_office.model.NotificationDTO
import com.seoul42.relief_post_office.model.UserDTO
import com.seoul42.relief_post_office.service.CheckLoginService
import com.seoul42.relief_post_office.util.Guardian
import com.seoul42.relief_post_office.viewmodel.FirebaseViewModel

class MainFragment : Fragment(R.layout.fragment_guardian) {

    private val auth: FirebaseAuth by lazy {
        Firebase.auth
    }
    private val myUserId: String by lazy {
        Firebase.auth.uid.toString()
    }

    private val firebaseViewModel : FirebaseViewModel by viewModels()
    private val connectedWardList = ArrayList<Pair<String, UserDTO>>()
    private val listenerList = ArrayList<ListenerDTO>()

    private lateinit var guardianAdapter : GuardianAdapter
    private lateinit var binding : FragmentGuardianBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?) : View? {
        binding = FragmentGuardianBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setLogout()
        setGuardianPhoto()
        setRecyclerView()
        setRequestButton()
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

    private fun setLogout() {
        binding.guardianLogout.setOnClickListener {
            Guardian.setLogout()
            auth.signOut()
            ActivityCompat.finishAffinity(requireActivity())
            startActivity(Intent(context, CheckLoginService::class.java))
        }
    }

    private fun setGuardianPhoto() {
        val userDB = Firebase.database.reference.child("user").child(myUserId)

        Glide.with(this)
            .load(USER.photoUri) /* ★★★ USER is in class of Guardian ★★★ */
            .circleCrop()
            .into(binding.guardianPhoto)
        binding.guardianPhoto.setOnClickListener {
            startActivity(Intent(context, GuardianProfileActivity::class.java))
        }

        /* 프로필 편집이 완료될 경우 업데이트된 사진을 적용하도록 리스너 설정 */
        val userListener = userDB.addChildEventListener(object : ChildEventListener {
            @SuppressLint("NotifyDataSetChanged")
            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
                Glide.with(this@MainFragment)
                    .load(USER.photoUri)
                    .circleCrop()
                    .into(binding.guardianPhoto)
            }
            override fun onChildRemoved(snapshot: DataSnapshot) {}
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onCancelled(error: DatabaseError) {}
        })
        listenerList.add(ListenerDTO(userDB, userListener))
    }

    private fun setRecyclerView() {
        val wardLayout = LinearLayoutManager(context)
        val connectDB = Firebase.database.reference.child("guardian").child(myUserId).child("connectList")

        setConnectedWardList()
        guardianAdapter = GuardianAdapter(requireContext(), connectedWardList)

        binding.guardianRecyclerView.adapter = guardianAdapter
        binding.guardianRecyclerView.layoutManager = wardLayout
        binding.guardianRecyclerView.setHasFixedSize(true)

        /* 연결된 피보호자가 실시간으로 recyclerView 에 적용하도록 리스너 설정 */
        val connectListener = connectDB.addChildEventListener(object : ChildEventListener {
            @SuppressLint("NotifyDataSetChanged")
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                val connectedUserId = snapshot.value.toString()
                addConnectedWardList(connectedWardList, connectedUserId)
            }
            @SuppressLint("NotifyDataSetChanged")
            override fun onChildRemoved(snapshot: DataSnapshot) {
                val connectedUserId = snapshot.value.toString()
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
            val requestDialog = RequestDialog(context as AppCompatActivity, firebaseViewModel)

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

    private fun setConnectedWardList() {
        for (ward in CONNECT_LIST) {
            addConnectedWardList(connectedWardList, ward.value)
        }
    }

    private fun addConnectedWardList(connectedWardList : ArrayList<Pair<String, UserDTO>>, connectedUserId : String) {
        val userDB = Firebase.database.reference.child("user").child(connectedUserId)

        userDB.get().addOnSuccessListener {
            if (!connectedWardList.contains(Pair(connectedUserId, it.getValue(UserDTO::class.java) as UserDTO))) {
                connectedWardList.add(Pair(connectedUserId, it.getValue(UserDTO::class.java) as UserDTO))
                guardianAdapter.notifyDataSetChanged()
            }
        }
    }
}