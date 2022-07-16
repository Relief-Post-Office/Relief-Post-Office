package com.seoul42.relief_post_office

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction
import com.seoul42.relief_post_office.databinding.GuardianBackgroundBinding
import com.seoul42.relief_post_office.guardian.MainFragment
import com.seoul42.relief_post_office.guardian.QuestionFragment
import com.seoul42.relief_post_office.guardian.SafetyFragment
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
        safetyFragment = SafetyFragment(userDTO)
        questionFragment = QuestionFragment()

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

    /* Fragment State 유지 함수 */
    private fun setFragment(tag: String, fragment: Fragment){
        val manager: FragmentManager = supportFragmentManager
        val ft: FragmentTransaction = manager.beginTransaction()

        /* 트랜잭션에 태그로 전달된 프래그먼트가 없을 경우 추가 */
        if(manager.findFragmentByTag(tag) == null){
            ft.add(R.id.fragmentContainer, fragment, tag)
        }

        /* 작업이 수월하도록 관리자에 추가되어 있는 프래그먼트를 변수로 할당 */
        val main = manager.findFragmentByTag(TAG_MAIN)
        val safety = manager.findFragmentByTag(TAG_SAFETY)
        val question = manager.findFragmentByTag(TAG_QUESTION)

        /* 모든 프래그먼트를 숨김 */
        if(main != null){
            ft.hide(main)
        }
        if(safety != null){
            ft.hide(safety)
        }
        if(question != null){
            ft.hide(question)
        }

        /* 선택한 항목에 따라 그에 맞는 프래그먼트만 보여줌 */
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