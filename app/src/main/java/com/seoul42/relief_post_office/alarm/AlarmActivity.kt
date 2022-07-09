package com.seoul42.relief_post_office.alarm

import android.app.NotificationManager
import android.content.Context
import android.media.MediaPlayer
import android.os.Bundle
import android.os.Handler
import android.system.Os.close
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.seoul42.relief_post_office.R
import kotlinx.android.synthetic.main.activity_alarm.view.*

class AlarmActivity : AppCompatActivity() {

    private  val btnClose : Button by lazy {
        findViewById<Button>(R.id.alarm_btnClose)
    }
    private var mediaPlayer: MediaPlayer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_alarm)

        setAlarm()
        setButton()
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
        mediaPlayer = MediaPlayer.create(this, R.raw.alarm)
        mediaPlayer!!.start()
        mediaPlayer!!.isLooping = true
    }

    /* 상단 notification 을 없애기 */
    private fun removeNotification() {
        val notificationManager = this.getSystemService(
            Context.NOTIFICATION_SERVICE) as NotificationManager

        notificationManager.cancelAll()
    }

    /* 버튼 text = 피보호자가 진행해야 할 "안부" 이름 */
    private fun setButton() {
        btnClose.text = intent.getStringExtra("safetyName")
        btnClose.setOnClickListener{
            removeNotification()
            close()
        }
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

