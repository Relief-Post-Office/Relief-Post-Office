package com.seoul42.relief_post_office.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.seoul42.relief_post_office.model.UserDTO
import com.seoul42.relief_post_office.model.WardDTO
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.abs

class BootCompleteReceiver : BroadcastReceiver() {

    data class RecommendDTO(
        val timeGap: Int?,
        val safetyId: String?
    ) {
        constructor() : this(0,"")
    }

    private val recommendList = ArrayList<RecommendDTO>()
    private var safetyCount : Int = 0

    companion object {
        const val REPEAT_STOP = "com.rightline.backgroundrepeatapp.REPEAT_STOP"
    }

    override fun onReceive(context : Context?, intent : Intent?) {
        if (intent!!.action.equals(Intent.ACTION_BOOT_COMPLETED) && Firebase.auth.currentUser != null) {
            Log.d("Boot Complete", "check...")
            val userId = Firebase.auth.uid.toString()
            val userDB = Firebase.database.reference.child("user").child(userId)

            userDB.get().addOnSuccessListener {
                val userDTO = it.getValue(UserDTO::class.java) as UserDTO
                if (userDTO.guardian == false) {
                    findSafety(context!!) /* 보유한 안부중에 가장 근접한 안부가 있을 경우 강제 알람을 요청 */
                }
            }
        }
    }

    /*
    *  alarmFlag = REPEAT_STOP
    *   - REPEAT_STOP : 강제 알람을 수행하기 위한 flag
    *
    *  alarmSecond = (안부 시간 - 현재 시간)
    *   - 안부 시간 - 현재 시간 : 현재 시간으로부터 가장 가까운 안부 시간(초 단위)
    */
    private fun setWardAlarm(context: Context, alarmFlag : String, alarmSecond : Int, safetyId : String) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val schedule = Intent(alarmFlag) /* 주기적으로 수행할지, 강제 알람을 수행할지 결정 */
        schedule.putExtra("safetyId", safetyId) /* schedule intent 에 safetyId 를 넣음 */
        schedule.setClass(context, WardReceiver::class.java)

        val sender = PendingIntent.getBroadcast(context, 0, schedule,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val interval = Calendar.getInstance()

        interval.timeInMillis = System.currentTimeMillis()
        interval.add(Calendar.SECOND, alarmSecond) /* 알람 시간 설정 */
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

        /*userDB.get().addOnSuccessListener { snapshot ->
            val connectedSafetyIdList = snapshot.getValue(WardDTO.ConnectedSafetyIdList::class.java) as WardDTO.ConnectedSafetyIdList
            safetyCount = connectedSafetyIdList.connectedSafetyIdList.size
            for (connectedSafetyId in connectedSafetyIdList.connectedSafetyIdList) {
                setSafetyList(context, connectedSafetyId)
            }
        }*/
    }

    /* 안부 data 를 가져와서 safetyList 를 세팅하는 작업을 하는 메서드 */
    private fun setSafetyList(context: Context, safetyId : String) {
        val date = SimpleDateFormat("HH:mm:ss:E")
            .format(Date(System.currentTimeMillis()))
        val curTime = date.substring(0, 8)
        val curDay = getDay(date.split(":")[2])
        val userDB = Firebase.database.reference.child("safety").child(safetyId)

        /*userDB.get().addOnSuccessListener { snapshot ->
            val safetyData = snapshot.getValue(SafetyBody::class.java) as SafetyBody
            addSafetyList(curDay, curTime, safetyId, safetyData.safetyData)
            safetyCount--
            if (safetyCount == 0) {
                if (recommendList.isNotEmpty()) {
                    recommendList.sortBy { it.timeGap }
                    Log.d("Check [recommendList]", recommendList.toString())
                    val timeDTO = recommendList.minBy { it.timeGap!! }
                    Log.d("Check [findSafety]", "$timeDTO is recommended!")
                    setWardAlarm(context, REPEAT_STOP, timeDTO.timeGap!!, timeDTO.safetyId!!)
                } else {
                    Log.d("Check [findSafety]", "no Safety ...")
                }
            }
        }*/
    }

    /* 보유한 안부중에 동일한 요일이 있을 경우 safetyList 에 추가하는 메서드 */
    /*private fun addSafetyList(curDay : Int, curTime : String, safetyId : String, safetyData : SafetyBody.SafetyData) {
        for (safetyDay in safetyData.dayList) {
            if (curDay == getDay(safetyDay)) {
                val timeGap = getTimeGap(curTime, safetyData.time, 0)
                recommendList.add(RecommendDTO(timeGap, safetyId))
            } else {
                if (getDay(safetyDay) - curDay < 0) {
                    val timeGap = getTimeGap(curTime, safetyData.time, (getDay(safetyDay) + 7) - curDay)
                    recommendList.add(RecommendDTO(timeGap, safetyId))
                } else {
                    val timeGap = getTimeGap(curTime, safetyData.time, getDay(safetyDay) - curDay)
                    recommendList.add(RecommendDTO(timeGap, safetyId))
                }
            }
        }
    }*/

    /* 피보호자 측에게 강제 알람을 띄우도록 하는 메서드 */
    private fun setForcedAlarm(safetyId : String) {
        Log.d("Check [setForcedAlarm]", "safety : $safetyId")
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