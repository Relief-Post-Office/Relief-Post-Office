package com.seoul42.relief_post_office

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.widget.addTextChangedListener
import com.dx.dxloadingbutton.lib.LoadingButton
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.google.firebase.messaging.FirebaseMessaging
import com.seoul42.relief_post_office.model.UserDTO
import com.seoul42.relief_post_office.util.Guardian
import com.seoul42.relief_post_office.util.Ward
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

    private lateinit var phoneAuthCredential: PhoneAuthCredential
    private lateinit var verificationId : String
    private lateinit var phoneNumber: String
    private lateinit var callbacks : PhoneAuthProvider.OnVerificationStateChangedCallbacks
    private lateinit var userDTO: UserDTO

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
            override fun onCodeSent(verificationId: String, token: PhoneAuthProvider.ForceResendingToken) {
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
                if (!requestFlag) {
                    requestPhone()
                } else {
                    binding.phoneRequestButton.loadingFailed()
                    Toast.makeText(this, "이미 인증요청을 하셨습니다.", Toast.LENGTH_SHORT).show()
                }
            } else {
                binding.phoneRequestButton.loadingFailed()
                Toast.makeText(this, "휴대전화번호를 정확히 입력해주세요.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun checkVerification() {
        var myVerification : String

        binding.phoneEditVerification.addTextChangedListener {
            myVerification = binding.phoneEditVerification.text.toString()
            if (myVerification.length == 6) {
                setVerificationDisable()
                binding.phoneProgressBar.visibility = View.VISIBLE
                phoneAuthCredential = PhoneAuthProvider.getCredential(verificationId, myVerification)
                auth.signInWithCredential(phoneAuthCredential)
                    .addOnCompleteListener(this) { task ->
                        if (task.isSuccessful) {
                            successVerification()
                        } else {
                            failVerification()
                            binding.phoneProgressBar.visibility = View.INVISIBLE
                        }
                    }
            }
        }
    }

    /* Start verification assistant */
    private fun successVerification() {
        val userId = auth.uid.toString()
        val userDB = Firebase.database.reference.child("user").child(userId)
        userDB.get().addOnSuccessListener {
            if (it.getValue(UserDTO::class.java) != null) {
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
        val myUserId = auth.uid.toString()
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
            ActivityCompat.finishAffinity(this)
            if (userDTO.guardian == true)
                startActivity(Intent(this, GuardianBackgroundActivity::class.java))
            else
                startActivity(Intent(this, WardActivity::class.java))
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
            callbacks )
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

    private fun setTimer() {
        var minute = 2
        var second = 0

        Thread {
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
        }.start()
    }
    /* End miscellaneous assistant */
}