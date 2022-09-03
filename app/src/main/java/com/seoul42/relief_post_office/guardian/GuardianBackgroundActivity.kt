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

/**
 * 보호자는 총 3 가지 화면이 존재함
 *  1. 메인 화면 : 보호자와 연결된 피보호자 정보를 확인할 수 있는 화면
 *  2. 내 안부 화면 : 보호자가 보유한 내 안부를 확인할 수 있는 화면
 *  3. 내 질문 화면 : 보호자가 보유한 내 질문을 확인할 수 있는 화면
 */
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

        // 하단 내비게이션 선택시 해당 프래그먼트가 설정됨
        binding.bottomNavigationView.setOnItemSelectedListener { item ->
            when(item.itemId) {
                R.id.navigation_main -> setFragment(TAG_MAIN, mainFragment)
                R.id.navigation_safety -> setFragment(TAG_SAFETY, safetyFragment)
                R.id.navigation_question -> setFragment(TAG_QUESTION, questionFragment)
            }
            true
        }
    }

    /**
     * 배터리 최적화 무시를 설정
     *  => 알람 매니저 및 FCM 에 대비하기 위함
     */
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

    /**
     * Fragment State 유지 함수
     */
    private fun setFragment(tag: String, fragment: Fragment){
        val manager: FragmentManager = supportFragmentManager
        val ft: FragmentTransaction = manager.beginTransaction()

        // 트랜잭션에 태그로 전달된 프래그먼트가 없을 경우 추가
        if(manager.findFragmentByTag(tag) == null){
            ft.add(R.id.fragmentContainer, fragment, tag)
        }

        // 작업이 수월하도록 관리자에 추가되어 있는 프래그먼트를 변수로 할당
        val main = manager.findFragmentByTag(TAG_MAIN)
        val safety = manager.findFragmentByTag(TAG_SAFETY)
        val question = manager.findFragmentByTag(TAG_QUESTION)

        // 모든 프래그먼트를 숨김
        if(main != null){
            ft.hide(main)
        }
        if(safety != null){
            ft.hide(safety)
        }
        if(question != null){
            ft.hide(question)
        }

        // 선택한 항목에 따라 그에 맞는 프래그먼트를 보여줌
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