package com.seoul42.relief_post_office.ward

import android.R
import android.app.Dialog
import android.content.res.Resources
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.Gravity
import android.view.ViewGroup
import android.view.Window
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.seoul42.relief_post_office.adapter.ResponseAdapter
import com.seoul42.relief_post_office.databinding.DialogResponseBinding

/**
 * 피보호자가 요청온 보호자들에 대한 응답 다이얼로그 클래스
 */
class ResponseDialog(context : AppCompatActivity) {

    private val binding by lazy {
        DialogResponseBinding.inflate(context.layoutInflater)
    }
    private val responseDialog by lazy {
        Dialog(context)
    }

    // 보호자들을 선택하고 응답 버튼을 눌렀을 때를 처리해주는 리스너 변수
    private lateinit var responseListener: ResponseAddClickedListener

    fun show(
        responseAdapter : ResponseAdapter,
        responseLayout: LinearLayoutManager,
        resources : Resources
    ) {
        binding.responseRecyclerView.adapter = responseAdapter
        binding.responseRecyclerView.layoutManager = responseLayout
        binding.responseRecyclerView.setHasFixedSize(true)

        binding.responseButton.buttonColor = resources.getColor(com.seoul42.relief_post_office.R.color.gray)
        binding.responseButton.cornerRadius = 30

        // 보호자들을 선택하고 응답 버튼을 눌렀을 때
        // WardActivity 에서 선택된 보호자들에 대한 응답 처리를 하도록 돕는 리스너
        binding.responseButton.setOnClickListener {
            responseListener.onSaveClicked()
            responseDialog.dismiss()
            responseDialog.cancel()
        }

        responseDialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        responseDialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        responseDialog.setContentView(binding.root)
        responseDialog.create()
        responseDialog.show()
    }

    fun setOnAddClickedListener(listener : () -> Unit) {
        this.responseListener = object: ResponseAddClickedListener {
            override fun onSaveClicked() {
                listener()
            }
        }
    }

    interface ResponseAddClickedListener {
        fun onSaveClicked()
    }
}