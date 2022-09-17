package com.seoul42.relief_post_office.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import android.widget.Toast
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.seoul42.relief_post_office.model.ResultDTO
import com.seoul42.relief_post_office.model.SafetyDTO
import com.seoul42.relief_post_office.model.UserDTO
import com.seoul42.relief_post_office.model.WardDTO
import com.seoul42.relief_post_office.util.Alarm
import com.seoul42.relief_post_office.util.Network
import java.io.Serializable
import java.text.SimpleDateFormat
import java.util.*

/**
 * 핸드폰 부팅 시 알람 설정을 위한 클래스
 * 총 3 가지 케이스로 알람 설정
 *
 *  1. 보호자 추천 알람 : 보호자 유저인 경우 보호자 추천 알람을 요청
 *  2. 피보호자 추천 알람 : 피보호자 유저인 경우 피보호자 추천 알람을 요청
 *  3. 네트워크 알람 : 네트워크 연결이 안된 경우
 */
class BootCompleteReceiver : BroadcastReceiver() {

    companion object {
        // 최초로 알람을 수행시키기 위한 플래그
        const val REPEAT_START = "com.rightline.backgroundrepeatapp.REPEAT_START"
    }

    // 데이터베이스 참조 변수
    private val userDB = Firebase.database.reference.child("user")

    /**
     * 네트워크가 연결되었는지 확인
     *  - 연결이 안된 경우 : 15분 단위로 네트워크 알람을 재요청
     *  - 연결된 경우 : 부팅 셋업 작업을 수행
     *
     * 부팅 셋업 작업
     *  1. 로그인 한 유저인지 확인
     *  2. 배터리 최적화를 무시한 유저인지 확인
     *
     * 2 가지 조건을 만족하면 Alarm 을 수행하도록 처리
     */
    override fun onReceive(context : Context, intent : Intent) {
        if (!Network.isNetworkAvailable(context)) {
            setNetworkAlarm(context)
        } else if (intent.action.equals(Intent.ACTION_BOOT_COMPLETED) && Firebase.auth.currentUser != null) {
            val uid = Firebase.auth.uid.toString()

            userDB.child(uid).get().addOnSuccessListener { userSnapshot ->
                val userDTO = userSnapshot.getValue(UserDTO::class.java) ?: throw IllegalArgumentException("user required")

                setAlarm(context, userDTO.guardian)
            }.addOnFailureListener {
                setNetworkAlarm(context)
            }
        }
    }

    /**
     * 네트워크 연결이 안될 경우 실행하는 메서드
     * 15분 단위로 네트워크 알람 요청을 수행
     */
    private fun setNetworkAlarm(context : Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val schedule = Intent(REPEAT_START)

        // 클래스 인자를 NetworkReceiver 로 설정하여 네트워크 알람을 설정
        schedule.setClass(context, NetworkReceiver::class.java)

        val sender = PendingIntent.getBroadcast(context, 0, schedule,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val interval = Calendar.getInstance()

        interval.timeInMillis = System.currentTimeMillis()
        interval.add(Calendar.MINUTE, 15) // 15분 뒤 네트워크 알람을 받도록 설정

        if (Build.VERSION.SDK_INT >= 23) {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                interval.timeInMillis,
                sender
            )
        } else if (Build.VERSION.SDK_INT >= 19) {
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, interval.timeInMillis, sender)
        } else {
            alarmManager[AlarmManager.RTC_WAKEUP, interval.timeInMillis] = sender
        }
    }

    /**
     * 보호자 또는 피보호자의 Alarm 작업을 수행하도록 함
     *  - guardianFlag = true : 보호자 Alarm 을 수행
     *  - guardianFlag = false : 피보호자 Alarm 을 수행
     */
    private fun setAlarm(context: Context, guardianFlag : Boolean) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val schedule = Intent(REPEAT_START)

        // 보호자 플래그에 따라 클래스 인자를 다르게 설정하여 보호자 및 피보호자 알람을 설정
        if (guardianFlag) {
            schedule.setClass(context, GuardianReceiver::class.java)
        } else {
            schedule.setClass(context, WardReceiver::class.java)
        }

        val sender = PendingIntent.getBroadcast(context, 0, schedule,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val interval = Calendar.getInstance()

        interval.timeInMillis = System.currentTimeMillis()
        interval.add(Calendar.SECOND, 5) // 5초 뒤에 알람을 받도록 설정

        if (Build.VERSION.SDK_INT >= 23) {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                interval.timeInMillis,
                sender
            )
        } else if (Build.VERSION.SDK_INT >= 19) {
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, interval.timeInMillis, sender)
        } else {
            alarmManager[AlarmManager.RTC_WAKEUP, interval.timeInMillis] = sender
        }
    }
}