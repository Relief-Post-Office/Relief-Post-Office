package com.seoul42.relief_post_office.service

import android.content.Intent
import android.graphics.drawable.AnimatedImageDrawable
import android.graphics.drawable.AnimationDrawable
import android.os.Bundle
import android.os.Handler
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat.startActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.google.firebase.messaging.FirebaseMessaging
import com.seoul42.relief_post_office.guardian.GuardianBackgroundActivity
import com.seoul42.relief_post_office.login.MainActivity
import com.seoul42.relief_post_office.databinding.SplashBinding
import com.seoul42.relief_post_office.model.UserDTO
import com.seoul42.relief_post_office.ward.WardActivity

class CheckLoginService : AppCompatActivity() {

    private val myUserId: String by lazy {
        Firebase.auth.uid.toString()
    }
    private val auth: FirebaseAuth by lazy {
        Firebase.auth
    }
    private val binding: SplashBinding by lazy {
        SplashBinding.inflate(layoutInflater)
    }

    private val userDB = Firebase.database.reference.child("user")

    private lateinit var userDTO: UserDTO
    private lateinit var imageView : ImageView
    private lateinit var animationDrawable: AnimationDrawable

    @Override
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)

        imageView = binding.splashImage
        animationDrawable = imageView.background as AnimationDrawable
        animationDrawable.start()

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
        userDB.get().addOnSuccessListener { allUserSnapshot ->
            if (!isValidUser(allUserSnapshot)) {
                auth.signOut()
                startActivity(Intent(this, MainActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION))
                finish()
            } else {
                setInfo()
            }
        }
    }

    private fun isValidUser(allUserSnapshot : DataSnapshot) : Boolean {
        return allUserSnapshot.children.any { user ->
            user.key == auth.uid
        }
    }

    private fun setInfo() {
        /* 토큰 획득 및 업데이트 */
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val userToken = task.result.toString()
                checkUserAndMoveActivity(userToken)
            }
        }
    }

    private fun checkUserAndMoveActivity(userToken : String) {
        /* 로그인한 유저가 보호자인지 피보호자인지 확인 */
        userDB.child(myUserId).get().addOnSuccessListener { user ->
            userDTO = user.getValue(UserDTO::class.java) as UserDTO
            userDTO.token = userToken
            userDB.setValue(userDTO)
            moveActivity(userDTO)
        }
    }

    private fun moveActivity(userDTO : UserDTO) {
        val guardianIntent = Intent(this, GuardianBackgroundActivity::class.java)
        val wardIntent = Intent(this, WardActivity::class.java)

        Handler().postDelayed({
            ActivityCompat.finishAffinity(this)
            if (userDTO.guardian) {
                guardianIntent.putExtra("userDTO", userDTO)
                startActivity(guardianIntent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION))
            } else {
                wardIntent.putExtra("userDTO", userDTO)
                startActivity(wardIntent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION))
            }
        }, 2000)
    }
}