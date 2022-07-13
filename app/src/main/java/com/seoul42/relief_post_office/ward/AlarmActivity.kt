package com.seoul42.relief_post_office.ward

import android.app.NotificationManager
import android.content.Context
import android.media.MediaPlayer
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import com.seoul42.relief_post_office.R
import com.seoul42.relief_post_office.databinding.ActivityAlarmBinding
import com.seoul42.relief_post_office.model.WardRecommendDTO
import java.text.SimpleDateFormat
import java.util.*

class AlarmActivity : AppCompatActivity() {

    private val binding: ActivityAlarmBinding by lazy {
        ActivityAlarmBinding.inflate(layoutInflater)
    }
    private var mediaPlayer: MediaPlayer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        setAlarm()
        setButton()
        setStatusBarTransparent()
        removeNotification()
    }

    /* MediaPlayer release */
    override fun onDestroy() {
        super.onDestroy()

        if (mediaPlayer != null) {
            mediaPlayer!!.release()
            mediaPlayer = null
        }
    }

    /* 알람 설정 */
    private fun setAlarm() {
        val date = SimpleDateFormat("MM월 dd일 E요일 HH:mm")
            .format(Date(System.currentTimeMillis()))
        val curDay = date.substring(0, 11)
        val curTime = date.substring(12, 17)
        val finishTime : Long = 300000
        val recommendDTO = intent.getSerializableExtra("recommendDTO") as WardRecommendDTO

        mediaPlayer = MediaPlayer.create(this, R.raw.alarm)
        mediaPlayer!!.start()
        mediaPlayer!!.isLooping = true

        binding.alarmDay.text = curDay
        binding.alarmTime.text = curTime
        binding.alarmText.text = recommendDTO.safetyDTO.name

        /* 5분 뒤에 알람 종료 */
        Handler().postDelayed({
            if (mediaPlayer != null) {
                close()
            }
        }, finishTime)
    }

    /* 버튼 text = 피보호자가 진행해야 할 "안부" 이름 */
    private fun setButton() {
        Log.d("확인용", "ㅇㅇㅇ")
        binding.alarmButton.setOnClickListener{
            Log.d("확인용", "될까?")
            close()
        }
    }

    private fun setStatusBarTransparent() {
        window.setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)
    }

    /* 상단 notification 을 없애기 */
    private fun removeNotification() {
        val notificationManager = this.getSystemService(
            Context.NOTIFICATION_SERVICE) as NotificationManager

        notificationManager.cancelAll()
    }

    /* 알람 종료 */
    private fun close() {
        if (mediaPlayer!!.isPlaying) {
            mediaPlayer!!.stop()
            mediaPlayer!!.release()
            mediaPlayer = null
        }
        finish()
    }
}

