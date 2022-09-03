package com.seoul42.relief_post_office.record

import android.content.Context
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatButton

class RecordButton (
    context : Context,
    attrs : AttributeSet
) : AppCompatButton(context, attrs) {

    init {
        text = "녹음"
    }

    /**
     * 안부질문 녹음 시 나타나는 버튼
     * - 상태에 따른 버튼 Text 설정
     */
    fun updateIconWithState(state: RecordState) {
        text = when (state) {
            RecordState.BEFORE_RECORDING -> {
                "녹음"
            }
            RecordState.ON_RECORDING -> {
                "녹음 중"
            }
            RecordState.AFTER_RECORDING -> {
                "재생"
            }
            RecordState.ON_PLAYING -> {
                "재생 중"
            }
        }
    }
}