package com.seoul42.relief_post_office.guardian

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.Window
import android.widget.*
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.seoul42.relief_post_office.util.Guardian.Companion.USER
import com.seoul42.relief_post_office.util.Guardian.Companion.CONNECT_WARD
import com.seoul42.relief_post_office.util.UserInfo.Companion.ALL_USER
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.seoul42.relief_post_office.R
import com.seoul42.relief_post_office.adapter.GuardianAdapter
import com.seoul42.relief_post_office.model.NotificationBody
import com.seoul42.relief_post_office.model.UserDTO
import com.seoul42.relief_post_office.service.CheckLoginService
import com.seoul42.relief_post_office.viewmodel.FirebaseViewModel
import de.hdodenhof.circleimageview.CircleImageView

class MainFragment : Fragment(R.layout.fragment_guardian) {

    private val myUserId: String by lazy {
        Firebase.auth.uid.toString()
    }
    private val firebaseViewModel : FirebaseViewModel by viewModels()
    private val connectedWardList = ArrayList<UserDTO>()
    private lateinit var guardianAdapter : GuardianAdapter
    private lateinit var guardianPhoto : CircleImageView
    private lateinit var recyclerView : RecyclerView
    private lateinit var guardianButton : ImageButton

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setComponent(view)
        setGuardianPhoto()
        setRecyclerView()
        setRequestButton()
    }

    private fun setComponent(view : View) {
        guardianPhoto = view.findViewById<CircleImageView>(R.id.guardian_photo)
        recyclerView = view.findViewById<RecyclerView>(R.id.guardian_recyclerView)
        guardianButton = view.findViewById<ImageButton>(R.id.guardian_add)
    }

    private fun setGuardianPhoto() {
        val userDB = Firebase.database.reference.child("user").child(myUserId)

        Glide.with(this)
            .load(USER.photoUri) /* ★★★ USER is in class of Guardian ★★★ */
            .circleCrop()
            .into(guardianPhoto)
        guardianPhoto.setOnClickListener {
            startActivity(Intent(context, GuardianProfileActivity::class.java))
        }

        /* 프로필 편집이 완료될 경우 업데이트된 사진을 적용하도록 리스너 설정 */
        userDB.addChildEventListener(object : ChildEventListener {
            @SuppressLint("NotifyDataSetChanged")
            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
                Glide.with(this@MainFragment)
                    .load(USER.photoUri)
                    .circleCrop()
                    .into(guardianPhoto)
            }
            override fun onChildRemoved(snapshot: DataSnapshot) {}
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun setRecyclerView() {
        val wardLayout = LinearLayoutManager(context)
        val connectDB = Firebase.database.reference.child("guardian").child(myUserId).child("connection")

        setConnectedWardList()
        guardianAdapter = GuardianAdapter(requireContext(), connectedWardList)

        recyclerView.adapter = guardianAdapter
        recyclerView.layoutManager = wardLayout
        recyclerView.setHasFixedSize(true)

        /* 연결된 피보호자가 실시간으로 recyclerView 에 적용하도록 리스너 설정 */
        connectDB.addChildEventListener(object : ChildEventListener {
            @SuppressLint("NotifyDataSetChanged")
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                val connectedUserId = snapshot.value.toString()
                if (!connectedWardList.contains(ALL_USER[connectedUserId]))
                    connectedWardList.add(ALL_USER[connectedUserId]!!)
                guardianAdapter.notifyDataSetChanged()
            }
            @SuppressLint("NotifyDataSetChanged")
            override fun onChildRemoved(snapshot: DataSnapshot) {
                val connectedUserId = snapshot.value.toString()
                connectedWardList.remove(ALL_USER[connectedUserId]!!)
                guardianAdapter.notifyDataSetChanged()
            }
            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun setRequestButton() {
        guardianButton.setOnClickListener {
            val dialog = AlertDialog.Builder(context).create()
            val eDialog : LayoutInflater = LayoutInflater.from(context)
            val mView : View = eDialog.inflate(R.layout.dialog_request,null)
            val phoneEdit : EditText = mView.findViewById(R.id.request_edit)
            val requestBtn : Button = mView.findViewById(R.id.request_button)

            requestBtn.setOnClickListener {
                val tel = phoneEdit.text.toString()

                if (tel.length != 11) {
                    Toast.makeText(context, "휴대전화번호를 정확히 입력해주세요.", Toast.LENGTH_SHORT).show()
                } else {
                    if (connectUser(tel)) {
                        Toast.makeText(context, "이미 연결된 피보호자입니다.", Toast.LENGTH_SHORT).show()
                    } else {
                        if (requestUser(tel)) {
                            Toast.makeText(context, "등록이 완료되었습니다.", Toast.LENGTH_SHORT).show()
                            dialog.dismiss()
                            dialog.cancel()
                        } else {
                            Toast.makeText(context, "등록되지 않은 피보호자 번호입니다.\n다시 확인해주세요.", Toast.LENGTH_SHORT)
                                .show()
                        }
                    }
                }
            }

            dialog.setView(mView)
            dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
            dialog.create()
            dialog.show()
        }
    }

    /* 연결된 피보호자인지 확인하는 메서드 */
    private fun connectUser(tel : String) : Boolean {
        var isConnect : Boolean = false

        for (user in CONNECT_WARD) {
            /* 연결된 피보호자의 전화번호와 일치한지 확인 */
            if (ALL_USER[user]?.tel == tel)
                isConnect = true
        }
        return isConnect
    }

    /* 요청 작업을 수행할지 확인하는 메서드 */
    private fun requestUser(tel : String) : Boolean {
        var userId : String
        var userValue : UserDTO
        var isExist : Boolean = false

        for (user in ALL_USER) {
            userId = user.key
            userValue = user.value
            /* 피보호자의 전화번호와 일치한지 확인 */
            if (tel == userValue.tel && userValue.guardian == false) {
                processRequest(userId) /* 요청 작업을 수행 */
                isExist = true
                break
            }
        }

        return isExist
    }

    /* 요청 작업을 수행하는 메서드 */
    private fun processRequest(userId : String) {
        val wardDB = Firebase.database.reference.child("ward").child(userId).child("request")
        val data = NotificationBody.NotificationData("안심 집배원"
            , USER.name!!, USER.name + "님이 보호자 요청을 보냈습니다.")
        val token = ALL_USER[userId]?.token.toString()
        val body = NotificationBody(token, data)

        wardDB.push().setValue(myUserId) /* 피보호자 요청 목록에 현재 보호자의 uid 추가 */
        firebaseViewModel.sendNotification(body) /* FCM 전송하기 */
    }

    private fun setConnectedWardList() {
        for (userId in CONNECT_WARD) {
            connectedWardList.add(ALL_USER[userId]!!)
        }
    }
}