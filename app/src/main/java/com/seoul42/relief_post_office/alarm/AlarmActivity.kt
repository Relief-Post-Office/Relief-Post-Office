package com.seoul42.relief_post_office.alarm

import android.media.MediaPlayer
import android.os.Bundle
import android.view.View
import android.widget.Button
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

        // 알람음 재생
        mediaPlayer = MediaPlayer.create(this, R.raw.alarm)
        mediaPlayer!!.start()
        mediaPlayer!!.isLooping = true

        btnClose.text = intent.getStringExtra("safetyName")
        btnClose.setOnClickListener(mClickListener)
    }

    override fun onDestroy() {
        super.onDestroy()

        // MediaPlayer release
        if (mediaPlayer != null) {
            mediaPlayer!!.release()
            mediaPlayer = null
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

    private var mClickListener: View.OnClickListener = View.OnClickListener { v ->
        when(v!!.id) {
            R.id.alarm_btnClose -> close()
        }
    }
}

