package com.seoul42.relief_post_office.join

import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.Window
import android.widget.NumberPicker
import androidx.appcompat.app.AppCompatActivity
import com.seoul42.relief_post_office.databinding.DialogBirthBinding

class BirthDialog(context : AppCompatActivity) {

    private val binding by lazy {
        DialogBirthBinding.inflate(context.layoutInflater)
    }
    private val birthDialog by lazy {
        Dialog(context)
    }
    private lateinit var birthListener: BirthSaveClickedListener

    fun show(birth : String) {
        var myYear : Int = 1970
        var myMonth : Int = 1
        var myDay : Int = 1
        val listener = NumberPicker.OnValueChangeListener { numberPicker, _, new ->
            when (numberPicker) {
                binding.birthYear -> myYear = new
                binding.birthMonth -> myMonth = new
                binding.birthDay -> myDay = new
            }
        }

        binding.birthYear.wrapSelectorWheel = false
        binding.birthMonth.wrapSelectorWheel = false
        binding.birthDay.wrapSelectorWheel = false
        binding.birthYear.descendantFocusability = NumberPicker.FOCUS_BLOCK_DESCENDANTS
        binding.birthMonth.descendantFocusability = NumberPicker.FOCUS_BLOCK_DESCENDANTS
        binding.birthDay.descendantFocusability = NumberPicker.FOCUS_BLOCK_DESCENDANTS
        binding.birthYear.minValue = 1900
        binding.birthMonth.minValue = 1
        binding.birthDay.minValue = 1
        binding.birthYear.maxValue = 2100
        binding.birthMonth.maxValue = 12
        binding.birthDay.maxValue = 31

        if (birth.isEmpty()) {
            binding.birthYear.value = 1970
            binding.birthMonth.value = 1
            binding.birthDay.value = 1
        } else {
            binding.birthYear.value = birth.split("/")[0].toInt()
            binding.birthMonth.value = birth.split("/")[1].toInt()
            binding.birthDay.value = birth.split("/")[2].toInt()
            myYear = binding.birthYear.value
            myMonth = binding.birthMonth.value
            myDay = binding.birthDay.value
        }

        binding.birthSave.setOnClickListener {
            birthListener.onSaveClicked("$myYear/$myMonth/$myDay")
            birthDialog.dismiss()
            birthDialog.cancel()
        }

        binding.birthYear.setOnValueChangedListener(listener)
        binding.birthMonth.setOnValueChangedListener(listener)
        binding.birthDay.setOnValueChangedListener(listener)

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