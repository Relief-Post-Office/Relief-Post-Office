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

/**
 * 앱의 스플래쉬 처리와 동시에 로그인 여부를 확인하는 클래스
 *  - 로그인 : 보호자 또는 피보호자 화면에 맞게 이동
 *  - 로그아웃 : 메인 화면(MainActivity)으로 이동
 */
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

    // 데이터베이스 참조 변수
    private val userDB = Firebase.database.reference.child("user")

    private lateinit var userDTO: UserDTO
    private lateinit var imageView : ImageView
    private lateinit var animationDrawable: AnimationDrawable

    @Override
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)

        // 스플래쉬 효과를 처리
        imageView = binding.splashImage
        animationDrawable = imageView.background as AnimationDrawable
        animationDrawable.start()

        // 유저의 로그인 또는 로그아웃 상태에 따라 처리
        if (auth.currentUser == null) {
            processLogout()
        } else {
            processLogin()
        }
    }

    /**
     * 로그아웃 시 메인 화면으로 이동
     */
    private fun processLogout() {
        Handler().postDelayed({
            startActivity(Intent(this, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION))
            finish()
        }, 2500)
    }

    /**
     * 현재 유저의 uid 가 유효한지를 체크
     *  - 유효하지 않은 경우 : 로그아웃 처리 및 메인 화면으로 이동
     *  - 유효한 경우 : 보호자 또는 피보호자 화면으로 이동
     */
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

    /**
     * FCM 토큰을 업데이트하는 메서드
     * 토큰은 앱을 삭제하고 다시 수행 시 변경될 수 있는 점을 고려
     */
    private fun setInfo() {
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val userToken = task.result.toString()
                checkUserAndMoveActivity(userToken)
            }
        }
    }

    /**
     * 기존에 있던 토큰을 새 토큰으로 업데이트
     * 그 후에 현재 유저의 정보를 담은 객체 userDTO 를 다음 화면으로 전송
     */
    private fun checkUserAndMoveActivity(userToken : String) {
        userDB.child(myUserId).get().addOnSuccessListener { user ->
            userDTO = user.getValue(UserDTO::class.java) as UserDTO
            userDTO.token = userToken
            userDB.child(myUserId).setValue(userDTO)
            moveActivity(userDTO)
        }
    }

    /**
     * 보호자 또는 피보호자 화면으로 현재 유저의 정보 userDTO 를 전송시킴
     */
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