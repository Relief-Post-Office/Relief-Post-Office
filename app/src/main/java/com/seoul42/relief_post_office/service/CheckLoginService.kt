package com.seoul42.relief_post_office.service

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.google.firebase.messaging.FirebaseMessaging
import com.seoul42.relief_post_office.GuardianBackgroundActivity
import com.seoul42.relief_post_office.MainActivity
import com.seoul42.relief_post_office.R
import com.seoul42.relief_post_office.model.UserDTO
import com.seoul42.relief_post_office.util.Guardian
import com.seoul42.relief_post_office.util.Ward
import com.seoul42.relief_post_office.ward.WardActivity

class CheckLoginService : AppCompatActivity() {

    private val myUserId: String by lazy {
        Firebase.auth.uid.toString()
    }
    private val auth: FirebaseAuth by lazy {
        Firebase.auth
    }
    private lateinit var userDTO: UserDTO

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.splash)

        if (auth.currentUser == null) {
            processLogout()
        } else {
            processLogin()
        }
    }

    private fun processLogout() {
        Handler().postDelayed({
            startActivity(Intent(this, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION))
            finish()
        }, 2500)
    }

    private fun processLogin() {
        val userDB = Firebase.database.reference.child("user")
        userDB.get().addOnSuccessListener {
            var flag = false
            for (user in it.children) {
                if (auth.uid == user.key) {
                    flag = true
                }
            }
            if (!flag) {
                auth.signOut()
                startActivity(Intent(this, MainActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION))
                finish()
            } else {
                setInfo()
            }
        }
    }

    private fun setInfo() {
        val userDB = Firebase.database.reference.child("user").child(myUserId)
        var userToken : String

        FirebaseMessaging.getInstance().token /* 토큰 획득 및 업데이트 */
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    userToken = task.result.toString()
                    /* 로그인한 유저가 보호자인지 피보호자인지 확인 */
                    userDB.get().addOnSuccessListener {
                        userDTO = it.getValue(UserDTO::class.java) as UserDTO
                        userDTO.token = userToken
                        userDB.setValue(userDTO)
                        if (userDTO.guardian == true) Guardian(userDTO)
                        else Ward(userDTO)
                        moveActivity()
                    }
                }
            }
    }

    private fun moveActivity() {
        Handler().postDelayed({
            if (userDTO.guardian == true)
                startActivity(Intent(this, GuardianBackgroundActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION))
            else
                startActivity(Intent(this, WardActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION))
            finish()
        }, 2000)
    }
}