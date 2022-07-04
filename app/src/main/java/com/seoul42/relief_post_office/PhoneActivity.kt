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
import com.seoul42.relief_post_office.model.UserDTO
import com.seoul42.relief_post_office.service.CheckLoginService
import java.util.concurrent.TimeUnit

class PhoneActivity : AppCompatActivity() {

    private lateinit var phoneAuthCredential: PhoneAuthCredential
    private lateinit var verificationId : String
    private lateinit var phoneNumber: String
    private lateinit var callbacks : PhoneAuthProvider.OnVerificationStateChangedCallbacks
    private var requestFlag: Boolean = false

    private val auth : FirebaseAuth by lazy {
        Firebase.auth
    }
    private val requestButton by lazy {
        findViewById<LoadingButton>(R.id.phone_request_button)
    }
    private val timerText by lazy {
        findViewById<TextView>(R.id.phone_timer_text)
    }
    private val phoneEditText by lazy {
        findViewById<EditText>(R.id.phone_edit_number)
    }
    private val verificationEditText by lazy {
        findViewById<EditText>(R.id.phone_edit_verification)
    }
    private val progressBar by lazy {
        findViewById<ProgressBar>(R.id.phone_progressBar)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.phone)

        initCallback()
        requestVerification()
        checkVerification()
    }

    private fun initCallback() {
        callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            override fun onVerificationCompleted(p0: PhoneAuthCredential) {  }
            override fun onVerificationFailed(p0: FirebaseException) {
                Toast.makeText(this@PhoneActivity, "잘못된 전화번호입니다. 다시 입력해주세요.", Toast.LENGTH_SHORT).show()
                requestButton.loadingFailed()
                setPhoneEnable()
            }
            override fun onCodeSent(verificationId: String, token: PhoneAuthProvider.ForceResendingToken) {
                Toast.makeText(this@PhoneActivity, "인증 요청이 완료되었습니다.", Toast.LENGTH_SHORT).show()
                this@PhoneActivity.verificationId = verificationId
                requestButton.loadingSuccessful()
                setVerificationEnable()
                requestFlag = true
                setTimer()
            }
        }
    }

    private fun requestVerification() {
        requestButton.setOnClickListener {
            val phoneNum = phoneEditText.text.toString()

            requestButton.startLoading()
            if (phoneNum.length == 11) {
                if (!requestFlag) {
                    requestPhone()
                } else {
                    requestButton.loadingFailed()
                    Toast.makeText(this, "이미 인증요청을 하셨습니다.", Toast.LENGTH_SHORT).show()
                }
            } else {
                requestButton.loadingFailed()
                Toast.makeText(this, "휴대전화번호를 정확히 입력해주세요.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun checkVerification() {
        var myVerification : String

        verificationEditText.addTextChangedListener {
            myVerification = verificationEditText.text.toString()
            if (myVerification.length == 6) {
                setVerificationDisable()
                progressBar.visibility = View.VISIBLE
                phoneAuthCredential = PhoneAuthProvider.getCredential(verificationId, myVerification)
                Thread {
                    Thread.sleep(2000)
                    Handler(Looper.getMainLooper()).post {
                        auth.signInWithCredential(phoneAuthCredential)
                            .addOnCompleteListener(this) { task ->
                                if (task.isSuccessful) {
                                    successVerification()
                                } else {
                                    failVerification()
                                    progressBar.visibility = View.INVISIBLE
                                }
                            }
                    }
                }.start()
            }
        }
    }

    /* Start verification assistant */
    private fun successVerification() {
        val userId = auth.uid.toString()
        val userDB = Firebase.database.reference.child("user").child(userId)
        userDB.get().addOnSuccessListener {
            if (it.getValue(UserDTO::class.java) != null) {
                ActivityCompat.finishAffinity(this)
                startActivity(Intent(this, MainActivity::class.java))
            }
            else {
                val intent = Intent(this, JoinActivity::class.java)
                intent.putExtra("tel", phoneNumber)
                startActivity(intent)
            }
        }
    }

    private fun failVerification() {
        verificationEditText.setText("")
        Toast.makeText(this, "인증 실패! 인증번호를 다시 확인하세요", Toast.LENGTH_SHORT).show()
        setVerificationEnable()
    }
    /* End verification assistant */

    /* Start request assistant */
    private fun requestPhone() {
        var phoneNum = phoneEditText.text.toString()

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
        phoneEditText.isEnabled = true
        requestButton.isEnabled = true
    }

    private fun setPhoneDisable() {
        phoneEditText.isEnabled = false
        requestButton.isEnabled = false
    }

    private fun setVerificationEnable() {
        verificationEditText.isEnabled = true
    }

    private fun setVerificationDisable() {
        verificationEditText.isEnabled = false
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
                    timerText.text = makeTime(minute, second)
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