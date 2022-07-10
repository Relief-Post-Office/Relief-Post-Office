package com.seoul42.relief_post_office.alarm

import android.app.*
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.seoul42.relief_post_office.R
import com.seoul42.relief_post_office.model.SafetyDTO
import com.seoul42.relief_post_office.model.WardDTO
import java.text.SimpleDateFormat
import java.util.*


class WardReceiver() : BroadcastReceiver() {

    data class RecommendDTO(
        val timeGap: Int?,
        val safetyId: String?,
        val safetyName: String?
    ) {
        constructor() : this(0,"", "")
    }

    private val recommendList = ArrayList<RecommendDTO>()
    private var safetyCount : Int = 0

    companion object {
        const val PERMISSION_REPEAT = "com.rightline.backgroundrepeatapp.permission.ACTION_REPEAT"
        const val REPEAT_START = "com.rightline.backgroundrepeatapp.REPEAT_START"
        const val REPEAT_STOP = "com.rightline.backgroundrepeatapp.REPEAT_STOP"
        const val PRIMARY_CHANNEL_ID = "primary_notification_channel"
        const val CHANNEL_ID = "channel_id"
        const val NOTIFICATION_ID = 0
    }

    /* 주기적으로 특정 알람을 받아서 수행하는 메서드 */
    override fun onReceive(context: Context, intent: Intent) {
        /* 최초 알람을 수행 */
        if (intent.action == REPEAT_START) {
            findSafety(context) /* 보유한 안부중에 동일한 요일이 있을 경우 강제 알람을 요청 */
        }
        /* 강제 알람을 수행 */
        else if (intent.action == REPEAT_STOP && Firebase.auth.currentUser != null) {
            val safetyId = intent.getStringExtra("safetyId")
            val safetyName = intent.getStringExtra("safetyName")
            setForcedAlarm(context, safetyId!!, safetyName!!) /* 강제 알람 */
        }
    }

    /*
    *  alarmFlag = REPEAT_STOP
    *   - REPEAT_STOP : 강제 알람을 수행하기 위한 flag
    *
    *  alarmSecond = (안부 시간 - 현재 시간)
    *   - 안부 시간 - 현재 시간 : 현재 시간으로부터 가장 가까운 안부 시간(초 단위)
    */
    private fun setWardAlarm(context: Context, alarmFlag : String, recommendDTO : RecommendDTO) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val schedule = Intent(alarmFlag) /* 주기적으로 수행할지, 강제 알람을 수행할지 결정 */
        schedule.putExtra("safetyId", recommendDTO.safetyId) /* schedule intent 에 safetyId 를 넣음 */
        schedule.putExtra("safetyName", recommendDTO.safetyName) /* schedule intent 에 safetyName 를 넣음 */
        schedule.setClass(context, WardReceiver::class.java)

        val sender = PendingIntent.getBroadcast(context, 0, schedule,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val interval = Calendar.getInstance()

        interval.timeInMillis = System.currentTimeMillis()
        interval.add(Calendar.SECOND, recommendDTO.timeGap!!) /* 알람 시간 설정 */
        alarmManager.cancel(sender)
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

    /* 현재 로그인한 피보호자의 안부 목록을 찾고 각 안부에 대해 setSafetyList() 메서드를 수행 */
    private fun findSafety(context: Context) {
        val myUserId = Firebase.auth.uid.toString()
        val userDB = Firebase.database.reference.child("ward").child(myUserId)

       userDB.get().addOnSuccessListener { snapshot ->
            if (snapshot.getValue(WardDTO::class.java) != null) {
                val connectedSafetyIdList = snapshot.getValue(WardDTO::class.java) as WardDTO
                safetyCount = connectedSafetyIdList.safetyIdList.size
                for (safety in connectedSafetyIdList.safetyIdList) {
                    setSafetyList(context, safety.value)
                }
            }
        }
    }

    /* 안부 data 를 가져와서 safetyList 를 세팅하는 작업을 하는 메서드 */
    private fun setSafetyList(context: Context, safetyId : String) {
        val date = SimpleDateFormat("HH:mm:ss:E")
            .format(Date(System.currentTimeMillis()))
        val curTime = date.substring(0, 8)
        val curDay = getDay(date.split(":")[3])
        val userDB = Firebase.database.reference.child("safety").child(safetyId)

        userDB.get().addOnSuccessListener { snapshot ->
            if (snapshot.getValue(SafetyDTO::class.java) != null) {
                val safetyDTO = snapshot.getValue(SafetyDTO::class.java) as SafetyDTO
                addSafetyList(curDay, curTime, safetyId, safetyDTO)
                safetyCount--
                if (safetyCount == 0) {
                    if (recommendList.isNotEmpty()) {
                        recommendList.sortBy { it.timeGap }
                        Log.d("Check [recommendList]", recommendList.toString())
                        val recommendDTO = recommendList.minBy { it.timeGap!! }
                        Log.d("Check [findSafety]", "$recommendDTO is recommended!")
                        setWardAlarm(context, REPEAT_STOP, recommendDTO)
                    } else {
                        Log.d("Check [findSafety]", "no Safety ...")
                    }
                }
            }
        }
    }

    /* 보유한 안부중에 동일한 요일이 있을 경우 safetyList 에 추가하는 메서드 */
   private fun addSafetyList(curDay : Int, curTime : String, safetyId : String, safetyDTO : SafetyDTO) {
        for (safetyDay in safetyDTO.dayOfWeek) {
            if (curDay == getDay(safetyDay.value)) {
                val timeGap = getTimeGap(curTime, safetyDTO.time!!, 0)
                recommendList.add(RecommendDTO(timeGap, safetyId, safetyDTO.name))
            } else {
                if (getDay(safetyDay.value) - curDay < 0) {
                    val timeGap = getTimeGap(curTime, safetyDTO.time!!, (getDay(safetyDay.value) + 7) - curDay)
                    recommendList.add(RecommendDTO(timeGap, safetyId, safetyDTO.name))
                } else {
                    val timeGap = getTimeGap(curTime, safetyDTO.time!!, getDay(safetyDay.value) - curDay)
                    recommendList.add(RecommendDTO(timeGap, safetyId, safetyDTO.name))
                }
            }
        }
    }

    /* 피보호자 측에게 강제 알람을 띄우도록 하는 메서드 */
    private fun setForcedAlarm(context : Context, safetyId : String, safetyName : String) {
        val notificationManager = context.getSystemService(
            Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationChannel = NotificationChannel(
                PRIMARY_CHANNEL_ID,
                "Stand up notification",
                NotificationManager.IMPORTANCE_HIGH
            )
            notificationChannel.enableLights(true)
            notificationChannel.lightColor = Color.RED
            notificationChannel.enableVibration(true)
            notificationChannel.description = "AlarmManager Tests"
            notificationManager.createNotificationChannel(
                notificationChannel)
        }

        val contentIntent = Intent(context, AlarmActivity::class.java)
        contentIntent.putExtra("safetyId", safetyId)
        contentIntent.putExtra("safetyName", safetyName)
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            NOTIFICATION_ID,
            contentIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val builder =
            NotificationCompat.Builder(context, PRIMARY_CHANNEL_ID)
                .setSmallIcon(R.drawable.relief_post_office)
                .setContentTitle(safetyName)
                .setContentText("$safetyName 안부에 대한 응답 시간입니다!")
                .setFullScreenIntent(contentPendingIntent, true)
                .setContentIntent(contentPendingIntent)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setDefaults(NotificationCompat.DEFAULT_ALL)

        notificationManager.notify(NOTIFICATION_ID, builder.build())
    }

    /* Start alarm's util */
    private fun getDay(curDay : String) : Int {
        return when(curDay) {
            "일" -> 0
            "월" -> 1
            "화" -> 2
            "수" -> 3
            "목" -> 4
            "금" -> 5
            else -> 6
        }
    }

    private fun getTimeGap(curTime : String, safetyTime : String, dayGap : Int) : Int {
        val curHour = curTime.substring(0, 2).toInt()
        val curMin = curTime.substring(3, 5).toInt()
        val curSecond = curTime.substring(6, 8).toInt()
        val safetyHour = safetyTime.substring(0, 2).toInt()
        val safetyMin = safetyTime.substring(3, 5).toInt()

        return if (dayGap == 0) {
            if ((safetyHour * 3600 + safetyMin * 60) - (curHour * 3600 + curMin * 60 + curSecond) < 0) {
                ((safetyHour + 24 * 7) * 3600 + safetyMin * 60) - (curHour * 3600 + curMin * 60 + curSecond)
            } else {
                (safetyHour * 3600 + safetyMin * 60) - (curHour * 3600 + curMin * 60 + curSecond)
            }
        } else {
            ((safetyHour + 24 * dayGap) * 3600 + safetyMin * 60) - (curHour * 3600 + curMin * 60 + curSecond)
        }
    }
    /* End alarm's util */
}