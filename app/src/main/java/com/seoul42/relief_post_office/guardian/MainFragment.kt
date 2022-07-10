package com.seoul42.relief_post_office.guardian

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.Dialog
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.widget.*
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
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
import com.seoul42.relief_post_office.GuardianBackgroundActivity
import com.seoul42.relief_post_office.R
import com.seoul42.relief_post_office.adapter.GuardianAdapter
import com.seoul42.relief_post_office.model.ListenerDTO
import com.seoul42.relief_post_office.model.NotificationDTO
import com.seoul42.relief_post_office.model.UserDTO
import com.seoul42.relief_post_office.service.CheckLoginService
import com.seoul42.relief_post_office.util.Guardian
import com.seoul42.relief_post_office.viewmodel.FirebaseViewModel
import com.seoul42.relief_post_office.ward.WardActivity
import de.hdodenhof.circleimageview.CircleImageView

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
    private lateinit var guardianPhoto : CircleImageView
    private lateinit var recyclerView : RecyclerView
    private lateinit var guardianButton : ImageButton
    private lateinit var guardianLogout : Button

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setComponent(view)
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

    private fun setComponent(view : View) {
        guardianPhoto = view.findViewById<CircleImageView>(R.id.guardian_photo)
        recyclerView = view.findViewById<RecyclerView>(R.id.guardian_recyclerView)
        guardianButton = view.findViewById<ImageButton>(R.id.guardian_add)
        guardianLogout = view.findViewById<Button>(R.id.guardian_logout)
    }

    private fun setLogout() {
        guardianLogout.setOnClickListener {
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
            .into(guardianPhoto)
        guardianPhoto.setOnClickListener {
            startActivity(Intent(context, GuardianProfileActivity::class.java))
        }

        /* 프로필 편집이 완료될 경우 업데이트된 사진을 적용하도록 리스너 설정 */
        val userListener = userDB.addChildEventListener(object : ChildEventListener {
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
        listenerList.add(ListenerDTO(userDB, userListener))
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
        guardianButton.setOnClickListener {
            val dialog = AlertDialog.Builder(context).create()
            val eDialog : LayoutInflater = LayoutInflater.from(context)
            val mView : View = eDialog.inflate(R.layout.dialog_request,null)
            val phoneEdit : EditText = mView.findViewById(R.id.request_edit)
            val requestBtn : Button = mView.findViewById(R.id.request_button)
            val progressBar : ProgressBar = mView.findViewById(R.id.request_progressBar)

            requestBtn.setOnClickListener {
                val tel = phoneEdit.text.toString()

                if (tel.length != 11) {
                    Toast.makeText(context, "휴대전화번호를 정확히 입력해주세요.", Toast.LENGTH_SHORT).show()
                } else {
                    progressBar.visibility = View.VISIBLE
                    activity!!.window.addFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
                    connectUser(tel, dialog, progressBar) /* 휴대전화번호에 해당하는 피보호자와 연결 시도 */
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
    private fun connectUser(tel : String, dialog : Dialog, progressBar : ProgressBar) {
        var connectFlag = false

        for (ward in CONNECT_LIST) {
            val wardDB = Firebase.database.reference.child("user").child(ward.value)

            wardDB.get().addOnSuccessListener {
                val userDTO = it.getValue(UserDTO::class.java) as UserDTO
                if (userDTO.tel == tel) {
                    connectFlag = true
                }
            }
        }

        Handler().postDelayed({
            if (connectFlag) {
                Toast.makeText(context, "이미 연결된 피보호자입니다.", Toast.LENGTH_SHORT).show()
                activity!!.window.clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
                progressBar.visibility = View.INVISIBLE
            } else {
                requestUser(tel, dialog, progressBar)
            }
        }, 1000)
    }

    /* 요청 작업을 수행할지 확인하는 메서드 */
    private fun requestUser(tel : String, dialog : Dialog, progressBar : ProgressBar) {
        val userDB = Firebase.database.reference.child("user")
        var userId : String
        var userValue : UserDTO
        var isExist : Boolean = false

        userDB.get().addOnSuccessListener {
            for (user in it.children) {
                userId = user.key!!
                userValue = user.getValue(UserDTO::class.java) as UserDTO
                /* 피보호자의 전화번호와 일치한지 확인 */
                if (tel == userValue.tel && userValue.guardian == false) {
                    processRequest(userId) /* 요청 작업을 수행 */
                    isExist = true
                }
            }
        }

        Handler().postDelayed({
            if (isExist) {
                Toast.makeText(context, "등록이 완료되었습니다.", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
                dialog.cancel()
            } else {
                if (tel == USER.tel) {
                    Toast.makeText(context, "본인의 휴대전화번호입니다.\n다시 입력해주세요.", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "등록되지 않은 피보호자 번호입니다.\n다시 입력해주세요.", Toast.LENGTH_SHORT).show()
                }
            }
            activity!!.window.clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
            progressBar.visibility = View.INVISIBLE
        }, 1000)
    }

    /* 요청 작업을 수행하는 메서드 */
    private fun processRequest(userId : String) {
        val userDB = Firebase.database.reference.child("user")
        val wardDB = Firebase.database.reference.child("ward").child(userId).child("request")

        userDB.get().addOnSuccessListener {
            for (child in it.children) {
                if (child.key == userId) {
                    val userDTO = child.getValue(UserDTO::class.java) as UserDTO
                    val token = userDTO.token.toString()
                    val notificationDTO = NotificationDTO(token, "안심 집배원"
                        , USER.name!!, USER.name + "님이 보호자 요청을 보냈습니다.")
                    wardDB.push().setValue(myUserId) /* 피보호자 요청 목록에 현재 보호자의 uid 추가 */
                    firebaseViewModel.sendNotification(notificationDTO) /* FCM 전송하기 */
                    break
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