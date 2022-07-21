package com.seoul42.relief_post_office.ward

import android.media.MediaPlayer
import android.os.Bundle
import android.os.Handler
import android.view.WindowManager
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.seoul42.relief_post_office.R
import com.seoul42.relief_post_office.databinding.WardEndingBinding
import com.seoul42.relief_post_office.model.*
import com.seoul42.relief_post_office.viewmodel.FirebaseViewModel

class EndingActivity : AppCompatActivity() {

    private val binding: WardEndingBinding by lazy {
        WardEndingBinding.inflate(layoutInflater)
    }

    private lateinit var userDTO : UserDTO
    private lateinit var wardDTO : WardDTO
    private var resultDTO : ResultDTO? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        // 보이스 재생 후 종료
        val endingGuideVoice = MediaPlayer.create(this, R.raw.safetyending)
        endingGuideVoice.setOnCompletionListener {
            Handler().postDelayed({
                endingGuideVoice.release()
                finish()
            }, 500)
        }
        endingGuideVoice.start()
        setFCM()
        setStatusBarTransparent()
    }

    private fun setFCM() {
        val uid = Firebase.auth.uid.toString()
        val resultId = intent.getStringExtra("resultId").toString()

        setUser(uid)
        setWard(uid)
        setResult(resultId)

        Handler().postDelayed({
            sendFCM(userDTO.name, wardDTO.connectList, resultDTO!!.safetyName)
        }, 1000)
    }

    private fun setUser(uid : String) {
        val userDB = Firebase.database.getReference("user")

        userDB.child(uid).get().addOnSuccessListener { user ->
            if (user.getValue(UserDTO::class.java) != null) {
                userDTO = user.getValue(UserDTO::class.java) as UserDTO
            }
        }
    }

    private fun setWard(uid : String) {
        val wardDB = Firebase.database.getReference("ward")

        wardDB.child(uid).get().addOnSuccessListener { ward ->
            if (ward.getValue(WardDTO::class.java) != null) {
                wardDTO = ward.getValue(WardDTO::class.java) as WardDTO
            }
        }
    }

    private fun setResult(resultId : String) {
        val resultDB = Firebase.database.getReference("result")

        resultDB.child(resultId).get().addOnSuccessListener { result ->
            if (result.getValue(ResultDTO::class.java) != null) {
                resultDTO = result.getValue(ResultDTO::class.java)
            }
        }
    }

    private fun sendFCM(myName : String, connectList : MutableMap<String, String>, safety : String) {
        val userDB = Firebase.database.getReference("user")
        val firebaseViewModel : FirebaseViewModel by viewModels()

        for (connect in connectList) {
            val uid = connect.key
            userDB.child(uid).get().addOnSuccessListener {
                if (it.getValue(UserDTO::class.java) != null) {
                    val userDTO = it.getValue(UserDTO::class.java) as UserDTO
                    val notificationData = NotificationDTO.NotificationData("안심우체국",
                        myName, "$myName 님이 $safety 안부를 완료했습니다.")
                    val notificationDTO = NotificationDTO(userDTO.token, notificationData)
                    firebaseViewModel.sendNotification(notificationDTO) /* FCM 전송하기 */
                }
            }
        }
    }

    private fun setStatusBarTransparent() {
        window.setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)
    }
}