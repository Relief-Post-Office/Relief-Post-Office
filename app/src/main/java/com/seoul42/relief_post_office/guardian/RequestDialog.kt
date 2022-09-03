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

/**
 * 보호자가 피보호자 전화번호를 입력하여 요청하도록 돕는 클래스
 * 총 4 가지 케이스로 분류
 *  1. 전화번호를 정확히 입력하지 못한 경우 (11 글자 이내)
 *  2. 이미 연결된 피보호자인 경우
 *  3. 존재하지 않는 피보호자인 경우
 *  4. 정상적으로 피보호자 요청을 성공한 경우
 */
class RequestDialog(
    context : AppCompatActivity,
    firebaseViewModel: FirebaseViewModel,
    private val connectList : MutableMap<String, String>
    ) {

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

    // 데이터베이스 참조 변수
    private val userDB = Firebase.database.reference.child("user")
    private val wardDB = Firebase.database.reference.child("ward")

    // 전화번호 입력 후 요청 버튼을 누를 시 발동되는 리스너
    // MainFragment 에서 4 가지 케이스중 한 가지 케이스를 전달하도록 돕는 리스너
    //  1. 핸드폰이 유효하지 않음
    //  2. 이미 연결된 보호자
    //  3. 보호자가 존재하지 않음
    //  4. 보호자 등록을 성공
    private lateinit var requestListener: RequestListener

    fun show(window : Window) {
        binding.requestButton.setOnClickListener {
            val tel = binding.requestEdit.text.toString()

            if (tel.length != 11) {
                requestListener.request(INVALID_PHONE_NUMBER)
            } else {
                binding.requestProgressBar.visibility = View.VISIBLE
                // 요청 작업이 진행되는 동안 다른 화면 선택을 할 수 없도록 설정
                window.addFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
                // 휴대전화번호에 해당하는 피보호자와 연결 시도
                connectUser(tel, window, binding.requestProgressBar)
            }
        }

        requestDialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        requestDialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        requestDialog.setContentView(binding.root)
        requestDialog.create()
        requestDialog.show()
    }

    /**
     * 연결된 피보호자인지 확인하는 메서드
     */
    private fun connectUser(
        tel : String,
        window : Window,
        progressBar : ProgressBar
    ) {
        var connectFlag = false

        // 연결된 피보호자를 확인 후 동일한 전화번호가 있는지를 확인
        for (ward in connectList) {
            userDB.child(ward.key).get().addOnSuccessListener { userSnapshot ->
                val userDTO = userSnapshot.getValue(UserDTO::class.java) as UserDTO
                // 한번이라도 동일하게 될 경우 true 값
                connectFlag = connectFlag || (userDTO.tel == tel)
            }
        }

        Handler().postDelayed({
            if (connectFlag) {
                requestListener.request(CONNECTED_GUARDIAN)
                // 화면 터치가 다시 가능하도록 설정
                window.clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
                progressBar.visibility = View.INVISIBLE
            } else {
                requestUser(tel, window, progressBar)
            }
        }, 1000) // 비동기 데이터통신을 고려하여 딜레이 1000ms 설정
    }

    /**
     * 요청 작업을 수행할지 확인하는 메서드
     */
    private fun requestUser(
        tel : String,
        window : Window,
        progressBar : ProgressBar
    ) {
        var isExist : Boolean = false

        // 모든 유저의 정보를 탐색하여 피보호자의 전화번호가 일치한지를 확인
        userDB.get().addOnSuccessListener { userSnapshot ->
            for (user in userSnapshot.children) {
                val userId = user.key!!
                val userValue = user.getValue(UserDTO::class.java) as UserDTO

                // 피보호자의 전화번호가 한번이라도 일치한 경우 존재
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
            // 화면 터치가 다시 가능하도록 설정
            window.clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
            progressBar.visibility = View.INVISIBLE
        }, 1500) // 비동기 데이터통신을 고려하여 딜레이 1500ms 을 설정
                          // 모든 유저의 정보이므로 더 많은 딜레이를 설정하였음
    }

    /**
     * 피보호자의 번호일 경우 요청 작업을 수행
     */
    private fun checkWard(
        tel :String,
        userId : String,
        userValue : UserDTO
    ) : Boolean {
        if (tel == userValue.tel && !userValue.guardian) {
            processRequest(userId)
            return true
        }
        return false
    }

    private fun processRequest(userId : String) {
        userDB.get().addOnSuccessListener { userSnapshot ->
            for (user in userSnapshot.children) {
                if (user.key == userId) executeRequest(user)
            }
        }
    }

    /**
     * 요청 작업을 실행하는 메서드
     */
    private fun executeRequest(user : DataSnapshot) {
        val userDTO = user.getValue(UserDTO::class.java) as UserDTO
        val token = userDTO.token
        // FCM 을 보낼 수 있도록 데이터를 설정한 변수
        val notificationData = NotificationDTO.NotificationData("안심 집배원"
            , userDTO.name, userDTO.name + "님이 요청을 보냈습니다.")
        // 우선순위를 "high" 로 설정하여 도즈모드에서도 즉각 반응이 가능하도록 함
        val notificationDTO = NotificationDTO(token, "high", notificationData)

        // 피보호자 요청 목록에 현재 보호자의 uid 추가
        wardDB.child(user.key!!).child("requestList").child(myUserId).setValue(myUserId)
        // FCM 을 전송
        firebaseViewModel.sendNotification(notificationDTO)
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