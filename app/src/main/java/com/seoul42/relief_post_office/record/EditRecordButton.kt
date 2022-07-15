package com.seoul42.relief_post_office.record

import android.content.Context
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatButton

class EditRecordButton (
    context : Context,
    attrs : AttributeSet
) : AppCompatButton(context, attrs) {

    init {
        text = "재생"
    }

    fun updateIconWithState(state: RecordState) {
        text = when (state) {
            RecordState.BEFORE_RECORDING -> {
                "녹음"
            }
            RecordState.ON_RECORDING -> {
                "녹음중단"
            }
            RecordState.AFTER_RECORDING -> {
                "재생"
            }
            RecordState.ON_PLAYING -> {
                "재생중단"
            }
        }
    }
}