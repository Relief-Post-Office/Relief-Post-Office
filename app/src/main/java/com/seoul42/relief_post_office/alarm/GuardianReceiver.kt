package com.seoul42.relief_post_office.alarm

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.PowerManager
import android.util.Log
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.app.Person
import androidx.core.graphics.drawable.IconCompat
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.seoul42.relief_post_office.R
import com.seoul42.relief_post_office.model.*
import com.seoul42.relief_post_office.util.Alarm
import com.seoul42.relief_post_office.util.Alarm.getDay
import com.seoul42.relief_post_office.util.Alarm.getTimeGap
import com.seoul42.relief_post_office.util.Network
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.max

/**
 * 보호자 유저를 위한 알람 설정을 위한 클래스
 * 연결된 피보호자의 안부 시작 시간으로부터 30분 뒤 미응답시 notification 을 알림
 *
 * 총 3 가지 케이스로 알람 설정
 *  1. 추천 알람 : 연결된 피보호자의 (안부 시간 + 30분) 과 가장 근접한 안부(여러 개)를 추천 후 통지 알람을 요청
 *  2. 통지 알람 : 추천 받은 안부들 중 미응답인 안부인 경우 notification 을 알리고 추천 알람을 요청
 *  3. 네트워크 알람 : 네트워크 연결이 안된 경우 네트워크 알람을 요청
 */
class GuardianReceiver () : BroadcastReceiver() {

    companion object {
        const val PERMISSION_REPEAT = "com.rightline.backgroundrepeatapp.permission.ACTION_REPEAT"

        // REPEAT_START : 추천 알람을 수행하기 위한 플래그
        const val REPEAT_START = "com.rightline.backgroundrepeatapp.REPEAT_START"
        // REPEAT_STOP : 통지 알람을 수행하기 위한 플래그
        const val REPEAT_STOP = "com.rightline.backgroundrepeatapp.REPEAT_STOP"
    }

    // 데이터베이스 참조 변수
    private val userDB = Firebase.database.reference.child("user")
    private val wardDB = Firebase.database.reference.child("ward")
    private val resultDB = Firebase.database.reference.child("result")
    private val safetyDB = Firebase.database.reference.child("safety")
    private val guardianDB = Firebase.database.reference.child("guardian")

    // 추천 가능한 모든 객체들을 담음
    private val candidateList = ArrayList<GuardianRecommendDTO>()

    // 추천 객체들을 담음
    private val recommendList = ArrayList<GuardianRecommendDTO>()

    // 상단 notification 의 겹침 현상 방지를 위함
    private var notificationId : Int = 100

    private var isFail : Boolean = false
    private var screenWakeLock : PowerManager.WakeLock? = null

    private lateinit var uid : String

    /**
     * 알람 요청을 받고 플래그에 따라 특정 작업을 수행하는 메서드
     *
     * 알람 요청을 받는 5 가지 케이스
     *  1. 보호자가 메인 화면으로 이동
     *  2. 보호자가 재부팅한 경우
     *  3. 연결된 피보호자의 안부 or 질문이 추가된 경우
     *  4. 연결된 피보호자의 안부 or 질문이 수정된 경우
     *  5. 연결된 피보호자의 안부 or 질문이 삭제된 경우
     */
    override fun onReceive(context: Context, intent: Intent) {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager

        // doze 모드 상태에서 즉각적으로 알람이 수행 가능하도록 깨우도록 함
        screenWakeLock = powerManager.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK
                or PowerManager.ACQUIRE_CAUSES_WAKEUP, "WakeLock")
        screenWakeLock?.acquire()

        if (!Network.isNetworkAvailable(context)) {
            setNetworkAlarm(context)
        } else if (Firebase.auth.currentUser != null) {
            uid = Firebase.auth.uid.toString()
            selectAlarm(context, intent)
        }
    }

    /**
     * REPEAT_START : 추천 알람 수행
     * REPEAT_STOP : 통지 알람 수행
     */
    private fun selectAlarm(context: Context, intent: Intent) {
        when (intent.action) {
            REPEAT_START -> {
                recommendAlarm(context)
            }
            REPEAT_STOP -> {
                notifyAlarm(context, intent.getSerializableExtra("recommendList")
                        as ArrayList<GuardianRecommendDTO>)
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

        isFail = true
        schedule.setClass(context, NetworkReceiver::class.java)

        val sender = PendingIntent.getBroadcast(context, 0, schedule,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val interval = Calendar.getInstance()

        interval.timeInMillis = System.currentTimeMillis()
        interval.add(Calendar.MINUTE, 15) // 15분 뒤에 네트워크 알람 설정

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

        screenWakeLock?.release() // 핸드폰이 다시 doze 모드에 빠질 수 있도록 잠금락을 해제
    }

    /**
     * 연결된 피보호자 중 가장 근접한 안부를 찾는 메서드
     *  1. 피보호자의 정보를 찾음 (그 후에 안부를 찾음)
     *  2. 가장 근접한 후보 객체를 선별후 timeGap 이 동일한 객체를 recommendList 에 추가
     *  3. 1, 2 작업이 끝날 경우 통지 알람을 설정 (단, candidateList 가 빌 경우 수행 x)
     */
    private fun recommendAlarm(context : Context) {
        val date = SimpleDateFormat("yyyy-MM-dd HH:mm:ss E")
            .format(Date(System.currentTimeMillis()))
        val curDate = date.substring(0, 10)
        val curTime = date.substring(11, 19)
        val curDay = date.split(" ")[2]
        val dateDTO = DateDTO(curDate, curTime, getDay(curDay))

        guardianDB.child(uid).get().addOnSuccessListener { guardianSnapshot ->
            val guardianDTO = guardianSnapshot.getValue(GuardianDTO::class.java) ?: GuardianDTO()

            for (ward in guardianDTO.connectList) {
                val wardId = ward.value
                findWard(context, dateDTO, wardId)
            }
            Handler().postDelayed({
                if (candidateList.isNotEmpty()) {
                    // 현재 시간으로부터 가장 근접한 안부의 초(second) 격차
                    val timeGap = candidateList.minBy{ candidate -> candidate.timeGap }.timeGap
                    setRecommendList(context, timeGap)
                }
            }, 5000) // 비동기식 데이터 통신으로 5초의 딜레이를 매꾸는 부분
        }.addOnFailureListener {
            if (!isFail) setNetworkAlarm(context)
        }
    }

    /**
     * candidateList 는 가장 근접한 후보 객체들이 선별된 목록
     * 현재 시간으로부터 가장 근접한 후보들을 recommendList 에 추가
     *  - minTimeGap = 현재 시간으로부터 가장 근접한 안부의 초(second) 격차
     */
    private fun setRecommendList(context : Context, minTimeGap : Int) {
        for (candidate in candidateList) {
            if (minTimeGap == candidate.timeGap) {
                recommendList.add(candidate)
            }
        }
        setAlarm(context, REPEAT_STOP, recommendList)
    }

    /**
     * 피보호자의 안부 리스트에 존재하는 안부를 추가하도록 돕는 메서드
     *  - dateDTO : 현재 날짜 및 현재 시간에 대한 정보
     */
    private fun findWard(
        context : Context,
        dateDTO : DateDTO,
        wardId : String
    ) {
        wardDB.child(wardId).get().addOnSuccessListener { wardSnapshot ->
            val wardDTO = wardSnapshot.getValue(WardDTO::class.java) ?: WardDTO()

            for (safety in wardDTO.safetyIdList) {
                val safetyId = safety.key

                setSafetyToCandidateList(context, dateDTO, wardId, safetyId)
            }
        }.addOnFailureListener {
            if (!isFail) setNetworkAlarm(context)
        }
    }

    /**
     * 안부를 후보 리스트에 추가하도록 돕는 메서드
     */
    private fun setSafetyToCandidateList(
        context : Context,
        dateDTO : DateDTO,
        wardId : String,
        safetyId : String
    ) {
        safetyDB.child(safetyId).get().addOnSuccessListener { safetySnapshot ->
            val safetyDTO = safetySnapshot.getValue(SafetyDTO::class.java) ?: SafetyDTO()

            searchDayOfWeekAndSetCandidateList(wardId, safetyId, dateDTO, safetyDTO)
        }.addOnFailureListener {
            if (!isFail) setNetworkAlarm(context)
        }
    }

    /**
     * 현재 시간으로부터 (안부 시간 + 30분) 까지의 초(second) 격차를 환산하여 후보 리스트에 추가하는 메서드
     */
    private fun searchDayOfWeekAndSetCandidateList(
        wardId : String,
        safetyId : String,
        dateDTO :DateDTO,
        safetyDTO : SafetyDTO
    ) {
        val curTime = dateDTO.curTime
        val curDay = dateDTO.curDay

        for (day in safetyDTO.dayOfWeek) {
            if (!day.value) {
                continue
            }
            val safetyDay = getDay(day.key)
            val timeGap = if (curDay == safetyDay) {
                getTimeGap(curTime, safetyDTO.time!!, 0, true)
            } else if (safetyDay - curDay < 0) {
                getTimeGap(curTime, safetyDTO.time!!, (safetyDay + 7) - curDay, true)
            } else {
                getTimeGap(curTime, safetyDTO.time!!, safetyDay - curDay, true)
            }
            candidateList.add(GuardianRecommendDTO(timeGap, wardId, safetyId))
        }
    }

    /**
     * alarmFlag 에 따라 알람 요청을 세팅
     *  - REPEAT_START : 추천 알람을 세팅
     *  - REPEAT_STOP : 통지 알람을 세팅
     */
    private fun setAlarm(
        context: Context,
        alarmFlag : String,
        recommendList : ArrayList<GuardianRecommendDTO>?
    ) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val schedule = Intent(alarmFlag)

        // 통지 알람을 위한 추천 리스트를 추가
        if (alarmFlag == REPEAT_STOP) {
            schedule.putExtra("recommendList", recommendList)
        }
        schedule.setClass(context, GuardianReceiver::class.java)

        val sender = PendingIntent.getBroadcast(context, 0, schedule,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val interval = Calendar.getInstance()

        interval.timeInMillis = System.currentTimeMillis()

        // 비동기식 데이터 통신으로 5초의 딜레이를 매꾸는 부분 (timeGap - 5 부분)
        // 최소 5초 뒤에 알람이 설정될 수 있도록 함 (5 미만의 정수는 허용 x)
        if (alarmFlag == REPEAT_STOP) {
            interval.add(Calendar.SECOND, max(recommendList!![0].timeGap - 5, 5))
        } else {
            interval.add(Calendar.SECOND, 5)
        }

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

        screenWakeLock?.release() // 핸드폰이 다시 doze 모드에 빠질 수 있도록 잠금락을 해제
    }

    /**
     * 통지 알람을 요청하도록 돕는 메서드
     */
    private fun notifyAlarm(
        context : Context,
        recommendList : ArrayList<GuardianRecommendDTO>
    ) {
        for (recommendDTO in recommendList) {
            checkWardAndNotifyAlarm(context, recommendDTO)
        }
        Handler().postDelayed({
            setAlarm(context, REPEAT_START, null)
        }, 5000)
    }

    /**
     * 1. 존재하는 유저인지 확인
     * 2. 피보호자인지 확인
     * 위 2 조건을 만족할 경우 피보호자의 userDTO, wardDTO 를 가지고 통지할지를 결정
     */
    private fun checkWardAndNotifyAlarm(
        context : Context,
        recommendDTO : GuardianRecommendDTO
    ) {
        val curTime = Calendar.getInstance()

        userDB.child(recommendDTO.wardId).get().addOnSuccessListener { userSnapshot ->
            val userDTO = userSnapshot.getValue(UserDTO::class.java)
                ?: throw IllegalArgumentException("user required")

            wardDB.child(recommendDTO.wardId).get().addOnSuccessListener { wardSnapshot ->
                val wardDTO = wardSnapshot.getValue(WardDTO::class.java) ?: WardDTO()

                notifyAlarmWithSafetyAndResult(context, userDTO, wardDTO, recommendDTO, curTime)
            }.addOnFailureListener {
                if (!isFail) setNetworkAlarm(context)
            }
        }.addOnFailureListener {
            if (!isFail) setNetworkAlarm(context)
        }
    }

    /**
     * 최종적으로 통지 알람을 보호자에게 보낼지를 결정
     * 아래의 조건이 전부 만족할 경우 보호자에게 통지 알람을 보냄
     *
     * 1. (현재 시간 - 안부 시간) 이 [20 min, 40 min] 범위 내인지 => (대략 30분 근처 확인)
     * 2. 피보호자가 안부를 미응답했는지
     * 3. 결과에 대응하는 안부 id 가 선별된 안부 id 와 동일한지
     * 4. 결과에 대응하는 안부가 존재하는지 => (삭제 여부까지 확인)
     *
     * - userDTO : 피보호자의 정보를 확인하기 위한 용도
     * - wardDTO : 피보호자의 결과 목록을 확인하기 위한 용도
     * - recommendDTO : 추천 알람에서 받아온 추천 안부
     */
    private fun notifyAlarmWithSafetyAndResult(
        context : Context,
        userDTO : UserDTO,
        wardDTO : WardDTO,
        recommendDTO : GuardianRecommendDTO,
        curTime : Calendar
    ) {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm")

        for (result in wardDTO.resultIdList) {
            val resultId = result.value

            resultDB.child(resultId).get().addOnSuccessListener { resultSnapshot ->
                val resultDTO = resultSnapshot.getValue(ResultDTO::class.java)
                    ?: throw IllegalArgumentException("result required")
                val safetyTime = dateFormat.parse(resultDTO.date + " " + resultDTO.safetyTime)
                val gapTime = curTime.time.time - safetyTime!!.time

                if (isEligibleNonResponseSafety(gapTime, resultDTO, recommendDTO)) {
                    setNotification(context, resultDTO, userDTO)
                }
            }.addOnFailureListener {
                if (!isFail) setNetworkAlarm(context)
            }
        }
    }

    /**
     * 1. (현재 시간 - 안부 시간) 이 [20 min, 40 min] 범위 내인지 => (대략 30분 근처 확인)
     * 2. 피보호자가 안부를 미응답했는지
     * 3. 결과에 대응하는 안부 id 가 선별된 안부 id 와 동일한지
     * 4. 결과에 대응하는 안부가 존재하는지 => (삭제 여부까지 확인)
     *
     * 위 4 가지 조건을 만족하면 true, 그 외에 false
     */
    private fun isEligibleNonResponseSafety(
        gapTime : Long,
        resultDTO : ResultDTO,
        recommendDTO : GuardianRecommendDTO
    ) : Boolean {
        return (gapTime >= 1200000) && (gapTime <= 2400000)
                && (resultDTO.responseTime == "미응답")
                && (resultDTO.safetyId == recommendDTO.safetyId)
    }

    /**
     * 통지를 세팅하는 작업을 돕는 메서드
     */
    private fun setNotification(
        context : Context,
        resultDTO : ResultDTO,
        userDTO : UserDTO
    ) {
        safetyDB.child(resultDTO.safetyId).get().addOnSuccessListener { safetySnapshot ->
            val safetyDTO = safetySnapshot.getValue(SafetyDTO::class.java)
                ?: throw IllegalArgumentException("safety required")

            executeNotification(context, userDTO, safetyDTO)
        }.addOnFailureListener {
            if (!isFail) setNetworkAlarm(context)
        }
    }

    /**
     * 최종적으로 보호자에게 통지를 보내는 메서드
     * 피보호자가 "미응답"한 안부를 보호자 핸드폰의 상단에 메시지가 띄워지도록 함
     */
    private fun executeNotification(
        context : Context,
        userDTO : UserDTO,
        safetyDTO : SafetyDTO
    ) {
        val user: Person = Person.Builder()
            .setName(userDTO.name)
            .setIcon(IconCompat.createWithResource(context, R.drawable.relief_post_office))
            .build()
        val body = userDTO.name + "님이 " + safetyDTO.name + " 안부를 미응답하셨습니다."
        val message = NotificationCompat.MessagingStyle.Message(
            body,
            System.currentTimeMillis(),
            user
        )
        val messageStyle = NotificationCompat.MessagingStyle(user)
            .addMessage(message)
        val notificationManager = context.getSystemService(
            Context.NOTIFICATION_SERVICE) as NotificationManager
        val builder = NotificationCompat.Builder(context, "default")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelName = "미응답 채널"
            val description = "피보호자가 안부를 미응답시 알람합니다."
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel("default", channelName, importance)

            channel.description = description
            notificationManager.createNotificationChannel(channel)
        }

        builder.setContentTitle("안심 우체국")
            .setContentText(body)
            .setStyle(messageStyle)
            .setSmallIcon(R.drawable.relief_post_office)
            .setAutoCancel(true)

        // notification 아이디를 분할하여 여러 통지가 한번에 올 경우 통지 중복을 방지
        notificationManager.notify(notificationId++, builder.build())
    }
}