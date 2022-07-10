package com.seoul42.relief_post_office.alarm

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.system.Os.close
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import com.seoul42.relief_post_office.GuardianBackgroundActivity
import com.seoul42.relief_post_office.R
import com.seoul42.relief_post_office.ward.WardActivity
import kotlinx.android.synthetic.main.activity_alarm.view.*
import java.text.SimpleDateFormat
import java.util.*

class AlarmActivity : AppCompatActivity() {

    private val timeText : TextView by lazy {
        findViewById<TextView>(R.id.alarm_time)
    }
    private val dayText : TextView by lazy {
        findViewById<TextView>(R.id.alarm_day)
    }
    private val alarmText : TextView by lazy {
        findViewById<TextView>(R.id.alarm_text)
    }
    private val alarmButton : ImageButton by lazy {
        findViewById<ImageButton>(R.id.alarm_button)
    }
    private var mediaPlayer: MediaPlayer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_alarm)

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

        mediaPlayer = MediaPlayer.create(this, R.raw.alarm)
        mediaPlayer!!.start()
        mediaPlayer!!.isLooping = true

        dayText.text = curDay
        timeText.text = curTime
        alarmText.text = intent.getStringExtra("safetyName")

        /* 5분 뒤에 알람 종료 */
        Handler().postDelayed({
            close()
        }, finishTime)
    }

    /* 버튼 text = 피보호자가 진행해야 할 "안부" 이름 */
    private fun setButton() {
        alarmButton.setOnClickListener{
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

