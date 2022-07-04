package com.seoul42.relief_post_office

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.seoul42.relief_post_office.util.UserInfo
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.google.firebase.messaging.FirebaseMessaging
import com.seoul42.relief_post_office.model.UserDTO
import com.seoul42.relief_post_office.util.Guardian
import com.seoul42.relief_post_office.util.Ward

class MainActivity : AppCompatActivity() {

    private val auth: FirebaseAuth by lazy {
        Firebase.auth
    }
    private val userId: String by lazy {
        Firebase.auth.uid.toString()
    }
    private val loadButton: Button by lazy {
        findViewById<Button>(R.id.main_button)
    }
    private val progressBar: ProgressBar by lazy {
        findViewById<ProgressBar>(R.id.main_progressBar)
    }
    private lateinit var userDTO: UserDTO

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main)

        findViewById<Button>(R.id.main_logout).setOnClickListener {
            auth.signOut()
            ActivityCompat.finishAffinity(this)
        }
        if (auth.currentUser != null) { /* 로그인 상태 */
            setInfo()
            processLogin()
        } else { /* 로그아웃 상태 */
            processLogout()
        }

        findViewById<Button>(R.id.btnTmp).setOnClickListener {
            val intent = Intent(this, ResultActivity::class.java)
            startActivity(intent)
        }
    }

    private fun setInfo() {
        val userDB = Firebase.database.reference.child("user").child(userId)
        var userToken : String

        /* 토큰 획득 및 업데이트 */
        FirebaseMessaging.getInstance().token
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    userToken = task.result.toString()
                    /* 로그인한 유저가 보호자인지 피보호자인지 확인 */
                    userDB.get().addOnSuccessListener {
                        userDTO = it.getValue(UserDTO::class.java) as UserDTO
                        userDTO.token = userToken
                        if (userDTO.guardian == true) Guardian(userDTO)
                        else Ward(userDTO)
                        userDB.setValue(userDTO)
                    }
                }
            }
        UserInfo() /* 모든 유저 정보 세팅 */
    }

    private fun processLogin() {
        loadButton.text = "환영합니다"
        loadButton.setOnClickListener {
            progressBar.visibility = View.VISIBLE
            loadButton.isEnabled = false

            Thread(Runnable {
                Thread.sleep(3000)
                Handler(Looper.getMainLooper()).post {
                    progressBar.visibility = View.INVISIBLE
                    loadButton.isEnabled = true

                    if (userDTO.guardian == true)
                        startActivity(Intent(this, GuardianActivity::class.java))
                    else
                        startActivity(Intent(this, WardActivity::class.java))
                }
            }).start()
        }
    }

    private fun processLogout() {
        loadButton.text = "핸드폰 인증"
        loadButton.setOnClickListener {
            startActivity(Intent(this, PhoneActivity::class.java))
        }
    }
}