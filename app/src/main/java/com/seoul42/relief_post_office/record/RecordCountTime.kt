package com.seoul42.relief_post_office.record

import android.content.Context
import android.os.SystemClock
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatTextView

class RecordCountTime(
    context: Context,
    attributeSet: AttributeSet? = null
) : AppCompatTextView(context, attributeSet) {

    private var startTimeStamp: Long = 0L

    private val countUpAction: Runnable = object : Runnable {
        override fun run() {
            val currentTimeStamp = SystemClock.elapsedRealtime()

            val countTimeSeconds = ((currentTimeStamp - startTimeStamp)/1000L).toInt()
            updateCountTime(countTimeSeconds)
            handler?.postDelayed(this, 1000L)
        }
    }
    /** 녹음파일 시간 카운트 */
    fun startCountUp() {
        startTimeStamp = SystemClock.elapsedRealtime()
        handler?.post(countUpAction)
    }
    /** 녹음파일 시간 카운트정지 */
    fun stopCountUp() {
        handler?.removeCallbacks(countUpAction)
        updateCountTime(0)
    }
    /** 녹음파일 시간 초기화 */
    fun clearCountTime() {
        updateCountTime(0)
    }
    /** 녹음파일 시간을 분/초로 표시 */
    private fun updateCountTime(countTimeSeconds : Int) {
        val minutes = countTimeSeconds / 60
        val seconds = countTimeSeconds % 60
        text = "%02d:%02d".format(minutes, seconds)
    }
}