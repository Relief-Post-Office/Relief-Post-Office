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

class PhoneActivity : AppCompatActivity() {

    private val auth : FirebaseAuth by lazy {
        Firebase.auth
    }
    private val binding by lazy {
        PhoneBinding.inflate(layoutInflater)
    }

    private val userDB = Firebase.database.reference.child("user")

    private lateinit var phoneAuthCredential: PhoneAuthCredential
    private lateinit var verificationId : String
    private lateinit var phoneNumber: String
    private lateinit var callbacks : PhoneAuthProvider.OnVerificationStateChangedCallbacks

    private var minute = 2
    private var second = 0
    private var requestFlag: Boolean = false

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

    private fun checkRequest() {
        if (!requestFlag) {
            requestPhone()
        } else {
            binding.phoneRequestButton.loadingFailed()
            Toast.makeText(this, "이미 인증요청을 하셨습니다.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun checkVerification() {
        var myVerification : String

        binding.phoneEditVerification.addTextChangedListener {
            myVerification = binding.phoneEditVerification.text.toString()
            if (myVerification.length == 6) {
                processVerification(myVerification)
            }
        }
    }

    private fun processVerification(myVerification : String) {
        setVerificationDisable()
        binding.phoneProgressBar.visibility = View.VISIBLE
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

    /* Start verification assistant */
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

    private fun setInfo() {
        /* 토큰 획득 및 업데이트 */
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                updateTokenAndMoveActivity(task)
            }
        }
    }

    private fun updateTokenAndMoveActivity(task : Task<String>) {
        val uid = auth.uid.toString()
        val userToken = task.result.toString()

        userDB.child(uid).get().addOnSuccessListener { userSnapshot ->
            val userDTO = userSnapshot.getValue(UserDTO::class.java)
                ?: throw IllegalArgumentException("user required")

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
                startActivity(guardianIntent)
            } else {
                wardIntent.putExtra("userDTO", userDTO)
                startActivity(wardIntent)
            }
        }, 2000)
    }
    /* End verification assistant */

    /* Start request assistant */
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
    /* End request assistant */

    /* Start miscellaneous assistant */
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
    /* End miscellaneous assistant */
}