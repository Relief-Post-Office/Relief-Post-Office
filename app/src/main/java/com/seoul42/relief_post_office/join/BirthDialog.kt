package com.seoul42.relief_post_office.join

import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.Window
import android.widget.NumberPicker
import androidx.appcompat.app.AppCompatActivity
import com.seoul42.relief_post_office.databinding.DialogBirthBinding

/**
 * 회원가입한 유저가 생년월일을 선택하도록 돕는 클래스
 */
class BirthDialog(context : AppCompatActivity) {

    private val binding by lazy {
        DialogBirthBinding.inflate(context.layoutInflater)
    }
    private val birthDialog by lazy {
        Dialog(context)
    }

    private var myYear : Int = 1970
    private var myMonth : Int = 1
    private var myDay : Int = 1

    private lateinit var birthListener: BirthSaveClickedListener

    fun show(birth : String) {
        // 연도, 월, 일에 대한 NumberPicker 가 핸들링 될 때 변화된 값이 적용되도록 돕는 리스너 변수
        val listener = NumberPicker.OnValueChangeListener{ numberPicker, _, new ->
            when (numberPicker) {
                binding.birthYear -> myYear = new
                binding.birthMonth -> myMonth = new
                binding.birthDay -> myDay = new
            }
        }

        setWrapSelectorWheel()
        setDescendantFocusAbility()
        setIntervalBirthDate()

        // 최초로 선택된 경우 1970년 1월 1일 적용
        if (birth.isEmpty()) {
            setDefaultBirthDate()
        }
        // 이전에 선택된 경우 선택된 연도, 월, 일 적용
        else {
            setExistBirthDate(birth)
        }

        setBirthSave()
        setBirthListener(listener)
        setBirthDialog()
    }

    /**
     * 순환이 생기지 않도록 막아두는 메서드
     */
    private fun setWrapSelectorWheel() {
        binding.birthYear.wrapSelectorWheel = false
        binding.birthMonth.wrapSelectorWheel = false
        binding.birthDay.wrapSelectorWheel = false
    }

    private fun setDescendantFocusAbility() {
        binding.birthYear.descendantFocusability = NumberPicker.FOCUS_BLOCK_DESCENDANTS
        binding.birthMonth.descendantFocusability = NumberPicker.FOCUS_BLOCK_DESCENDANTS
        binding.birthDay.descendantFocusability = NumberPicker.FOCUS_BLOCK_DESCENDANTS
    }

    private fun setIntervalBirthDate() {
        binding.birthYear.minValue = 1900
        binding.birthMonth.minValue = 1
        binding.birthDay.minValue = 1
        binding.birthYear.maxValue = 2100
        binding.birthMonth.maxValue = 12
        binding.birthDay.maxValue = 31
    }

    private fun setDefaultBirthDate() {
        binding.birthYear.value = 1970
        binding.birthMonth.value = 1
        binding.birthDay.value = 1
    }

    private fun setExistBirthDate(birth : String) {
        binding.birthYear.value = birth.split("/")[0].toInt()
        binding.birthMonth.value = birth.split("/")[1].toInt()
        binding.birthDay.value = birth.split("/")[2].toInt()
        myYear = binding.birthYear.value
        myMonth = binding.birthMonth.value
        myDay = binding.birthDay.value
    }

    /**
     * 저장 버튼을 누를 시 현재 적용된 생년월일을 JoinActivity 에서 받도록 돕는 메서드
     */
    private fun setBirthSave() {
        binding.birthSave.setOnClickListener {
            birthListener.onSaveClicked("$myYear/$myMonth/$myDay")
            birthDialog.dismiss()
            birthDialog.cancel()
        }
    }

    private fun setBirthListener(
        listener : NumberPicker.OnValueChangeListener
    ) {
        binding.birthYear.setOnValueChangedListener(listener)
        binding.birthMonth.setOnValueChangedListener(listener)
        binding.birthDay.setOnValueChangedListener(listener)
    }

    private fun setBirthDialog() {
        birthDialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        birthDialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        birthDialog.setContentView(binding.root)
        birthDialog.create()
        birthDialog.show()
    }

    fun setOnSaveClickedListener(listener: (String) -> Unit) {
        this.birthListener = object: BirthSaveClickedListener {
            override fun onSaveClicked(content: String) {
                listener(content)
            }
        }
    }

    interface BirthSaveClickedListener {
        fun onSaveClicked(content : String)
    }
}