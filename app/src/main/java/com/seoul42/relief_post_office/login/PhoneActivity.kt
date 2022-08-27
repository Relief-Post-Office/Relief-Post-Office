package com.seoul42.relief_post_office.login

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.widget.addTextChangedListener
import com.google.android.gms.tasks.Task
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.google.firebase.messaging.FirebaseMessaging
import com.seoul42.relief_post_office.guardian.GuardianBackgroundActivity
import com.seoul42.relief_post_office.join.JoinActivity
import com.seoul42.relief_post_office.model.UserDTO
import com.seoul42.relief_post_office.ward.WardActivity
import java.util.concurrent.TimeUnit
import com.seoul42.relief_post_office.databinding.PhoneBinding

/**
 * 핸드폰 인증을 돕는 클래스
 * 핸드폰 인증을 마치면 회원가입 또는 보호자 및 피보호자 화면으로 이동
 */
class PhoneActivity : AppCompatActivity() {

    private val auth : FirebaseAuth by lazy {
        Firebase.auth
    }
    private val binding by lazy {
        PhoneBinding.inflate(layoutInflater)
    }

    // 데이터베이스 참조 변수
    private val userDB = Firebase.database.reference.child("user")

    // 인증 시간을 처리하도록 하는 변수들
    private var minute = 2
    private var second = 0
    private var requestFlag: Boolean = false

    // 유저가 입력한 인증번호와 일치한지를 판단해주는 변수
    private lateinit var phoneAuthCredential: PhoneAuthCredential

    // 핸드폰 인증 관련 콜백을 유저가 인증번호 요청 시에 수행되도록 돕는 변수
    private lateinit var callbacks : PhoneAuthProvider.OnVerificationStateChangedCallbacks

    // 유저의 인증번호 및 핸드폰 번호
    private lateinit var verificationId : String
    private lateinit var phoneNumber: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        initCallback()
        requestVerification()
        checkVerification()
    }

    private fun initCallback() {
        callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            override fun onVerificationCompleted(p0: PhoneAuthCredential) {  }
            override fun onVerificationFailed(p0: FirebaseException) {
                Toast.makeText(this@PhoneActivity, "잘못된 전화번호입니다. 다시 입력해주세요.", Toast.LENGTH_SHORT).show()
                binding.phoneRequestButton.loadingFailed()
                setPhoneEnable()
            }

            /**
             * 핸드폰 번호를 입력하고 인증 번호 요청시 정상적으로 수행되는 메서드
             *  - verificationId : 유저가 입력해야 할 인증번호
             */
            override fun onCodeSent(
                verificationId: String,
                token: PhoneAuthProvider.ForceResendingToken
            ) {
                Toast.makeText(this@PhoneActivity, "인증 요청이 완료되었습니다.", Toast.LENGTH_SHORT).show()
                this@PhoneActivity.verificationId = verificationId
                binding.phoneRequestButton.loadingSuccessful()
                setVerificationEnable()
                requestFlag = true
                setTimer()
            }
        }
    }

    /**
     * 인증 요청을 돕는 메서드
     * 핸드폰 번호가 정상적으로 기입되었는지 확인 후 인증 요청을 수행
     */
    private fun requestVerification() {
        binding.phoneRequestButton.setOnClickListener {
            val phoneNum = binding.phoneEditNumber.text.toString()

            binding.phoneRequestButton.startLoading()
            if (phoneNum.length == 11) {
                checkRequest()
            } else {
                binding.phoneRequestButton.loadingFailed()
                Toast.makeText(this, "휴대전화번호를 정확히 입력해주세요.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * 이미 인증요청을 한 경우를 제외하고 인증 요청이 수행되도록 처리하는 메서드
     */
    private fun checkRequest() {
        if (!requestFlag) {
            requestPhone()
        } else {
            binding.phoneRequestButton.loadingFailed()
            Toast.makeText(this, "이미 인증요청을 하셨습니다.", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * 유저가 기입한 인증번호가 6글자 정확히 입력한 경우를 확인하는 메서드
     */
    private fun checkVerification() {
        var myVerification : String

        binding.phoneEditVerification.addTextChangedListener {
            myVerification = binding.phoneEditVerification.text.toString()
            if (myVerification.length == 6) {
                processVerification(myVerification)
            }
        }
    }

    /**
     * 유저가 기입한 인증번호가 요청한 인증번호와 동일한지를 확인하는 메서드
     */
    private fun processVerification(myVerification : String) {
        setVerificationDisable()
        binding.phoneProgressBar.visibility = View.VISIBLE
        // verificationId : 유저가 요청한 인증번호
        // myVerificationId : 유저가 기입한 인증번호
        phoneAuthCredential = PhoneAuthProvider.getCredential(verificationId, myVerification)
        auth.signInWithCredential(phoneAuthCredential).addOnCompleteListener(this) { task ->
            if (task.isSuccessful) {
                successVerification()
            } else {
                failVerification()
                binding.phoneProgressBar.visibility = View.INVISIBLE
            }
        }
    }

    /**
     * 인증 요청이 성공적으로 이루어진 경우
     *  - 이미 회원가입 된 경우 : 보호자 또는 피보호자 메인 화면으로 이동
     *  - 첫 회원인 경우 : 회원가입 화면으로 이동
     */
    private fun successVerification() {
        val userId = auth.uid.toString()

        userDB.child(userId).get().addOnSuccessListener { userSnapshot ->
            if (userSnapshot.getValue(UserDTO::class.java) != null) {
                setInfo()
            }
            else {
                val intent = Intent(this, JoinActivity::class.java)

                intent.putExtra("tel", phoneNumber)
                startActivity(intent)
                binding.phoneProgressBar.visibility = View.INVISIBLE
            }
        }
    }

    private fun failVerification() {
        binding.phoneEditVerification.setText("")
        Toast.makeText(this, "인증 실패! 인증번호를 다시 확인하세요", Toast.LENGTH_SHORT).show()
        setVerificationEnable()
    }

    /**
     * FCM 토큰을 업데이트하는 메서드
     * 토큰은 앱을 삭제하고 다시 수행 시 변경될 수 있는 점을 고려
     */
    private fun setInfo() {
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                updateTokenAndMoveActivity(task)
            }
        }
    }

    /**
     * 기존에 있던 토큰을 새 토큰으로 업데이트
     * 그 후에 현재 유저의 정보를 담은 객체 userDTO 를 다음 화면으로 전송
     */
    private fun updateTokenAndMoveActivity(task : Task<String>) {
        val uid = auth.uid.toString()
        val userToken = task.result.toString()

        userDB.child(uid).get().addOnSuccessListener { userSnapshot ->
            val userDTO = userSnapshot.getValue(UserDTO::class.java)
                ?: throw IllegalArgumentException("user required")

            userDTO.token = userToken
            userDB.child(uid).setValue(userDTO)
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
                startActivity(guardianIntent)
            } else {
                wardIntent.putExtra("userDTO", userDTO)
                startActivity(wardIntent)
            }
        }, 2000)
    }

    /**
     * 인증 번호를 요청하도록 돕는 메서드
     * callbacks 변수를 인자로 설정하여 인증 요청을 확인해줌
     */
    private fun requestPhone() {
        var phoneNum = binding.phoneEditNumber.text.toString()

        phoneNumber = phoneNum
        phoneNum = "+82" + phoneNum.substring(1, phoneNum.length)
        setPhoneDisable()
        PhoneAuthProvider.getInstance().verifyPhoneNumber(
            phoneNum,
            120,
            TimeUnit.SECONDS,
            this,
            callbacks
        )
    }

    /**
     * ex) result = "01분 48초"
     */
    private fun makeTime(minute : Int, second : Int) : String {
        var result = ""

        result += "0" + minute.toString() + "분 "
        result += if (second < 10) {
            "0" + second.toString() + "초"
        } else {
            second.toString() + "초"
        }
        return result
    }

    /**
     * 00분 00초 가 될 때까지 1초씩 감소되어 인증 시간을 업데이트
     */
    private fun setThreadTimer() {
        while (minute != 0 || second != 0) {
            Handler(Looper.getMainLooper()).post {
                binding.phoneTimerText.text = makeTime(minute, second)
            }
            if (second == 0) {
                minute--
                second = 59
            } else {
                second--
            }
            Thread.sleep(1000)
        }
    }

    private fun setTimer() {
        minute = 2
        second = 0

        Thread {
            setThreadTimer()
        }.start()
    }

    private fun setPhoneEnable() {
        binding.phoneEditNumber.isEnabled = true
        binding.phoneRequestButton.isEnabled = true
    }

    private fun setPhoneDisable() {
        binding.phoneEditNumber.isEnabled = false
        binding.phoneRequestButton.isEnabled = false
    }

    private fun setVerificationEnable() {
        binding.phoneEditVerification.isEnabled = true
    }

    private fun setVerificationDisable() {
        binding.phoneEditVerification.isEnabled = false
    }
}