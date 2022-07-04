package com.seoul42.relief_post_office.ward

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.Window
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.seoul42.relief_post_office.util.UserInfo.Companion.ALL_USER
import com.seoul42.relief_post_office.util.Ward.Companion.CONNECT_GUARDIAN
import com.seoul42.relief_post_office.util.Ward.Companion.REQUEST_GUARDIAN
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
import com.seoul42.relief_post_office.model.RequestDTO
import com.seoul42.relief_post_office.model.UserDTO

class WardActivity : AppCompatActivity() {

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
    private val connectedGuardianList = ArrayList<UserDTO>()
    private lateinit var wardAdapter : WardAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.ward)

        setWardPhoto()
        setRecyclerView()
        setAddButton()
    }

    private fun setWardPhoto() {
        Glide.with(this)
            .load(USER.photoUri) /* ★★★ USER is in class of Ward ★★★ */
            .circleCrop()
            .into(wardPhoto)
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
        connectDB.addChildEventListener(object : ChildEventListener {
            @SuppressLint("NotifyDataSetChanged")
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                val connectedUserId = snapshot.value.toString()
                if (!connectedGuardianList.contains(ALL_USER[connectedUserId]))
                    connectedGuardianList.add(ALL_USER[connectedUserId]!!)
                wardAdapter.notifyDataSetChanged()
            }
            @SuppressLint("NotifyDataSetChanged")
            override fun onChildRemoved(snapshot: DataSnapshot) {
                val connectedUserId = snapshot.value.toString()
                connectedGuardianList.remove(ALL_USER[connectedUserId]!!)
                wardAdapter.notifyDataSetChanged()
            }
            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun setAddButton() {
        val requestDB = Firebase.database.reference.child("ward").child(myUserId).child("request")

        guardianAddButton.setOnClickListener {
            if (REQUEST_GUARDIAN.size == 0) {
                Toast.makeText(this, "추가하실 보호자 정보가 없습니다.", Toast.LENGTH_SHORT).show()
            } else {
                setAddDialog()
            }
        }

        /* 요청온 보호자의 수를 실시간으로 반영 */
        requestDB.addChildEventListener(object : ChildEventListener {
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                guardianAddButton.text = REQUEST_GUARDIAN.size.toString()
            }
            override fun onChildRemoved(snapshot: DataSnapshot) {
                guardianAddButton.text = REQUEST_GUARDIAN.size.toString()
            }
            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun setAddDialog() {
        val dialog = AlertDialog.Builder(this).create()
        val eDialog : LayoutInflater = LayoutInflater.from(this)
        val mView : View = eDialog.inflate(R.layout.dialog_response,null)
        val dialogRecyclerView : RecyclerView = mView.findViewById(R.id.response_recyclerView)
        val responseButton : Button = mView.findViewById(R.id.response_button)
        val responseAdapter = ResponseAdapter(this, getRequestedGuardianList())
        val responseLayout = LinearLayoutManager(this)

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
            wardDB = Firebase.database.reference.child("ward").child(myUserId).child("connection")
            wardDB.push().setValue(checkId)
            connectedGuardianList.add(ALL_USER[checkId]!!)
            /* 선택된 보호자는 피보호자와 연결 */
            guardianDB = Firebase.database.reference.child("guardian").child(checkId).child("connection")
            guardianDB.push().setValue(myUserId)
        }

        /* 실시간으로 선택한 유저들을 추가 반영 */
        wardAdapter.notifyDataSetChanged()
    }

    private fun setConnectedGuardianList() {
        for (connectId in CONNECT_GUARDIAN) {
            connectedGuardianList.add(ALL_USER[connectId]!!)
        }
    }

    private fun getRequestedGuardianList() : ArrayList<RequestDTO> {
        val requestedGuardianList = ArrayList<RequestDTO>()

        for (requestId in REQUEST_GUARDIAN) {
            val request = RequestDTO(ALL_USER[requestId]?.photoUri, ALL_USER[requestId]?.name,
                ALL_USER[requestId]?.birth, requestId)
            requestedGuardianList.add(request)
        }
        return requestedGuardianList
    }
}