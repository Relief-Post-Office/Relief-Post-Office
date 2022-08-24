package com.seoul42.relief_post_office.guardian

import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Handler
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.widget.ProgressBar
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.request.RequestListener
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.seoul42.relief_post_office.databinding.DialogRequestBinding
import com.seoul42.relief_post_office.model.NotificationDTO
import com.seoul42.relief_post_office.model.UserDTO
import com.seoul42.relief_post_office.viewmodel.FirebaseViewModel
import com.seoul42.relief_post_office.util.Constants.Companion.INVALID_PHONE_NUMBER
import com.seoul42.relief_post_office.util.Constants.Companion.CONNECTED_GUARDIAN
import com.seoul42.relief_post_office.util.Constants.Companion.NON_EXIST_GUARDIAN
import com.seoul42.relief_post_office.util.Constants.Companion.REGISTER_SUCCESS

class RequestDialog(
    context : AppCompatActivity,
    firebaseViewModel: FirebaseViewModel,
    private val connectList : MutableMap<String, String>)
{
    private val myUserId: String by lazy {
        Firebase.auth.uid.toString()
    }
    private val binding by lazy {
        DialogRequestBinding.inflate(context.layoutInflater)
    }
    private val requestDialog by lazy {
        Dialog(context)
    }
    private val firebaseViewModel by lazy {
        firebaseViewModel
    }

    private val userDB = Firebase.database.reference.child("user")
    private val wardDB = Firebase.database.reference.child("ward")

    private lateinit var requestListener: RequestListener

    fun show(window : Window) {
        binding.requestButton.setOnClickListener {
            val tel = binding.requestEdit.text.toString()

            if (tel.length != 11) {
                requestListener.request(INVALID_PHONE_NUMBER)
            } else {
                binding.requestProgressBar.visibility = View.VISIBLE
                window.addFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
                connectUser(tel, window, binding.requestProgressBar) /* 휴대전화번호에 해당하는 피보호자와 연결 시도 */
            }
        }

        requestDialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        requestDialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        requestDialog.setContentView(binding.root)
        requestDialog.create()
        requestDialog.show()
    }

    /* 연결된 피보호자인지 확인하는 메서드 */
    private fun connectUser(
        tel : String,
        window : Window,
        progressBar : ProgressBar)
    {
        var connectFlag = false

        for (ward in connectList) {
            userDB.child(ward.key).get().addOnSuccessListener { userSnapshot ->
                val userDTO = userSnapshot.getValue(UserDTO::class.java) as UserDTO
                connectFlag = connectFlag || (userDTO.tel == tel)
            }
        }

        Handler().postDelayed({
            if (connectFlag) {
                requestListener.request(CONNECTED_GUARDIAN)
                window.clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
                progressBar.visibility = View.INVISIBLE
            } else {
                requestUser(tel, window, progressBar)
            }
        }, 1000)
    }

    /* 요청 작업을 수행할지 확인하는 메서드 */
    private fun requestUser(
        tel : String,
        window : Window,
        progressBar : ProgressBar)
    {
        var isExist : Boolean = false

        userDB.get().addOnSuccessListener { userSnapshot ->
            for (user in userSnapshot.children) {
                val userId = user.key!!
                val userValue = user.getValue(UserDTO::class.java) as UserDTO

                /* 피보호자의 전화번호와 일치한지 확인 */
                isExist = isExist || checkWard(tel, userId, userValue)
            }
        }

        Handler().postDelayed({
            if (isExist) {
                requestListener.request(REGISTER_SUCCESS)
                requestDialog.dismiss()
                requestDialog.cancel()
            } else {
                requestListener.request(NON_EXIST_GUARDIAN)
            }
            window.clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
            progressBar.visibility = View.INVISIBLE
        }, 1500)
    }

    private fun checkWard(
        tel :String,
        userId : String,
        userValue : UserDTO) : Boolean
    {
        if (tel == userValue.tel && !userValue.guardian) {
            processRequest(userId) /* 요청 작업을 수행 */
            return true
        }
        return false
    }

    /* 요청 작업을 수행하는 메서드 */
    private fun processRequest(userId : String) {
        userDB.get().addOnSuccessListener { userSnapshot ->
            for (user in userSnapshot.children) {
                if (user.key == userId) executeRequest(user)
            }
        }
    }

    private fun executeRequest(user : DataSnapshot) {
        val userDTO = user.getValue(UserDTO::class.java) as UserDTO
        val token = userDTO.token
        val notificationData = NotificationDTO.NotificationData("안심 집배원"
            , userDTO.name, userDTO.name + "님이 요청을 보냈습니다.")
        val notificationDTO = NotificationDTO(token, "high", notificationData)

        /* 피보호자 요청 목록에 현재 보호자의 uid 추가 */
        wardDB.child(user.key!!).child("requestList").child(myUserId).setValue(myUserId)
        firebaseViewModel.sendNotification(notificationDTO) /* FCM 전송하기 */
    }

    fun setOnRequestListener(listener: (String) -> Unit) {
        this.requestListener = object: RequestListener {
            override fun request(requestCase : String) {
                listener(requestCase)
            }
        }
    }

    interface RequestListener {
        fun request(requestCase : String)
    }
}