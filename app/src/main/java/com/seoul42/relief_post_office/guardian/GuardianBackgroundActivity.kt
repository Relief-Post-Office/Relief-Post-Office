package com.seoul42.relief_post_office.guardian

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction
import com.seoul42.relief_post_office.R
import com.seoul42.relief_post_office.databinding.GuardianBackgroundBinding
import com.seoul42.relief_post_office.model.UserDTO
import com.seoul42.relief_post_office.util.Constants.Companion.TAG_MAIN
import com.seoul42.relief_post_office.util.Constants.Companion.TAG_QUESTION
import com.seoul42.relief_post_office.util.Constants.Companion.TAG_SAFETY

class GuardianBackgroundActivity : AppCompatActivity() {

    private val binding: GuardianBackgroundBinding by lazy {
        GuardianBackgroundBinding.inflate(layoutInflater)
    }

    private lateinit var userDTO : UserDTO
    private lateinit var mainFragment : MainFragment
    private lateinit var safetyFragment : SafetyFragment
    private lateinit var questionFragment : QuestionFragment

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        userDTO = intent.getSerializableExtra("userDTO") as UserDTO
        mainFragment = MainFragment(userDTO)
        safetyFragment = SafetyFragment()
        questionFragment = QuestionFragment()

        ignoreBatteryOptimization()
        setContentView(binding.root)
        setFragment(TAG_MAIN, mainFragment)

        binding.bottomNavigationView.setOnItemSelectedListener { item ->
            when(item.itemId) {
                R.id.navigation_main -> setFragment(TAG_MAIN, mainFragment)
                R.id.navigation_safety -> setFragment(TAG_SAFETY, safetyFragment)
                R.id.navigation_question -> setFragment(TAG_QUESTION, questionFragment)
            }
            true
        }
    }

    private fun ignoreBatteryOptimization() {
        val intent = Intent()
        val packageName = packageName
        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager

        if (!pm.isIgnoringBatteryOptimizations(packageName)) {
            intent.action = Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
            intent.data = Uri.parse("package:$packageName")
            startActivity(intent)
        }
    }

    /* Fragment State ?????? ?????? */
    private fun setFragment(tag: String, fragment: Fragment){
        val manager: FragmentManager = supportFragmentManager
        val ft: FragmentTransaction = manager.beginTransaction()

        /* ??????????????? ????????? ????????? ?????????????????? ?????? ?????? ?????? */
        if(manager.findFragmentByTag(tag) == null){
            ft.add(R.id.fragmentContainer, fragment, tag)
        }

        /* ????????? ??????????????? ???????????? ???????????? ?????? ?????????????????? ????????? ?????? */
        val main = manager.findFragmentByTag(TAG_MAIN)
        val safety = manager.findFragmentByTag(TAG_SAFETY)
        val question = manager.findFragmentByTag(TAG_QUESTION)

        /* ?????? ?????????????????? ?????? */
        if(main != null){
            ft.hide(main)
        }
        if(safety != null){
            ft.hide(safety)
        }
        if(question != null){
            ft.hide(question)
        }

        /* ????????? ????????? ?????? ?????? ?????? ?????????????????? ????????? */
        if(tag == TAG_MAIN){
            if(main != null){
                ft.show(main)
            }
        }
        else if(tag == TAG_SAFETY){
            if(safety != null){
                ft.show(safety)
            }
        }
        else if(tag == TAG_QUESTION){
            if(question != null){
                ft.show(question)
            }
        }

        ft.commitAllowingStateLoss()
    }
}