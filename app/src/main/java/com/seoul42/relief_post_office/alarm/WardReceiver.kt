package com.seoul42.relief_post_office.alarm

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Handler
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.Person
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.IconCompat
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.seoul42.relief_post_office.R
import com.seoul42.relief_post_office.model.*
import com.seoul42.relief_post_office.util.Alarm.getDateToLong
import com.seoul42.relief_post_office.util.Alarm.getDay
import com.seoul42.relief_post_office.util.Alarm.getTimeGap
import com.seoul42.relief_post_office.util.Network
import com.seoul42.relief_post_office.ward.AlarmActivity
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.max

/**
 * 피보호자 유저를 위한 알람 설정을 위한 클래스
 * 피보호자가 보유한 안부 중 가장 근접한 시간대를 선택하여 강제 알람을 띄우도록 알림
 *
 * 총 4 가지 케이스로 알람 설정
 *  1. 추천 알람 : 가장 근접한 시간대의 안부를 하나 추천 후 통지 알람을 요청
 *  2. 통지 알람 : 추천 받은 안부에 대한 notification 을 알리고 강제 알람을 요청
 *  3. 강제 알람 : 추천 받은 안부에 대한 강제 알람을 띄우고 추천 알람을 요청
 *  4. 네트워크 알람 : 네트워크 연결이 안된 경우 네트워크 알람을 요청
 */
class WardReceiver() : BroadcastReceiver() {

    companion object {
        const val PERMISSION_REPEAT = "com.rightline.backgroundrepeatapp.permission.ACTION_REPEAT"

        // REPEAT_START : 추천 알람을 수행하기 위한 플래그
        const val REPEAT_START = "com.rightline.backgroundrepeatapp.REPEAT_START"
        // REPEAT_NOTIFY : 통지 알람을 수행하기 위한 플래그
        const val REPEAT_NOTIFY = "com.rightline.backgroundrepeatapp.REPEAT_NOTIFY"
        // REPEAT_STOP : 강제 알람을 수행하기 위한 플래그
        const val REPEAT_STOP = "com.rightline.backgroundrepeatapp.REPEAT_STOP"

        const val PRIMARY_CHANNEL_ID = "primary_notification_channel"
        const val NOTIFICATION_ID = 100
        const val FORCE_ID = 200
    }

    // 데이터베이스 참조 변수
    private val wardDB = Firebase.database.reference.child("ward")
    private val resultDB = Firebase.database.reference.child("result")
    private val safetyDB = Firebase.database.reference.child("safety")
    private val answerDB = Firebase.database.reference.child("answer")
    private val questionDB = Firebase.database.reference.child("question")

    // 추천할 수 있는 모든 객체들을 후보 리스트에 담는 용도
    private val candidateList = ArrayList<WardRecommendDTO>()

    private var isFail : Boolean = false
    private var screenWakeLock : PowerManager.WakeLock? = null

    private lateinit var uid : String

    /**
     * 알람 요청을 받고 플래그에 따라 특정 작업을 수행하는 메서드
     *
     * 알람 요청을 받는 5 가지 케이스
     *  1. 피보호자가 메인 화면으로 이동
     *  2. 피보호자가 재부팅한 경우
     *  3. 보호자가 피호보자의 안부 or 질문을 추가한 경우
     *  4. 보호자가 피보호자의 안부 or 질문을 수정한 경우
     *  5. 보호자가 피보호자의 안부 or 질문을 삭제한 경우
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
     * REPEAT_NOTIFY : 통지 알람 수행
     * REPEAT_STOP : 강제 알람 수행
     */
    private fun selectAlarm(context: Context, intent: Intent) {
        when (intent.action) {
            REPEAT_START -> {
                recommendAlarm(context, intent)
            }
            REPEAT_NOTIFY -> {
                notifyAlarm(context, intent,
                    intent.getSerializableExtra("recommendDTO") as WardRecommendDTO)
            }
            REPEAT_STOP -> {
                forceAlarm(context, intent,
                    intent.getSerializableExtra("recommendDTO") as WardRecommendDTO)
            }
        }
    }

    /**
     * 상단의 notification 을 제거
     */
    private fun removeNotification(context : Context) {
        val notificationManager = context.getSystemService(
            Context.NOTIFICATION_SERVICE) as NotificationManager

        notificationManager.cancel(NOTIFICATION_ID)
    }

    /**
     * 네트워크 연결이 안될 경우 실행하는 메서드
     * 15분 단위로 네트워크 알람 요청을 수행함
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
     * 피보호자의 안부 및 결과에 대한 추천 및 세팅 작업을 수행
     *  1. 피보호자가 보유한 안부에 대해 추천 객체를 모으도록 수행
     *  2. 피보호자가 보유한 미리 생성된 결과에 대해 삭제을 수행
     *
     * 추천 객체를 전부 모은 경우, timeGap 이 가장 작은 안부에 대해 강제 알람 요청
     *  ( 피보호자의 알람 시간이 겹칠 경우 임의의 안부 하나가 선택 )
     */
    private fun recommendAlarm(context: Context, intent : Intent) {
        val date = SimpleDateFormat("yyyy-MM-dd HH:mm:ss E")
            .format(Date(System.currentTimeMillis()))
        val curDate = date.substring(0, 10)
        val curTime = date.substring(11, 19)
        val curDay = date.split(" ")[2]
        val dateDTO = DateDTO(curDate, curTime, getDay(curDay))

        removeNotification(context) // 상단의 notification 을 제거

        wardDB.child(uid).get().addOnSuccessListener { wardSnapshot ->
            val wardDTO = wardSnapshot.getValue(WardDTO::class.java) ?: WardDTO()

            for (safety in wardDTO.safetyIdList) {
                val safetyId = safety.key
                setSafety(context, dateDTO, safetyId)
            }
            for (result in wardDTO.resultIdList) {
                val resultKey = result.key
                val resultId = result.value
                setResult(context, dateDTO, resultKey, resultId)
            }
            Handler().postDelayed({
                if (candidateList.isNotEmpty()) {
                    val recommendDTO = candidateList.minBy { candidate -> candidate.timeGap }
                    setForceAlarm(context, recommendDTO, intent)
                }
            }, 5000) // 비동기식 데이터 통신으로 5초의 딜레이를 매꾸는 부분
        }.addOnFailureListener {
            if (!isFail) setNetworkAlarm(context)
        }
    }

    /**
     * 피보호자가 보유한 각각의 안부에 대해 추천 리스트에 추가하도록 세팅하는 메서드
     *  - dateDTO : 현재 날짜 및 현재 시간에 대한 정보
     */
    private fun setSafety(
        context : Context,
        dateDTO : DateDTO,
        safetyId : String
    ) {
        safetyDB.child(safetyId).get().addOnSuccessListener { safetySnapshot ->
            val safetyDTO = safetySnapshot.getValue(SafetyDTO::class.java)
                ?: throw IllegalArgumentException("safety required")

            addSafetyToCandidateList(dateDTO, safetyId, safetyDTO)
        }.addOnFailureListener {
            if (!isFail) setNetworkAlarm(context)
        }
    }

    /**
     * 현재 시간으로부터 안부 시간까지의 초(second) 격차를 환산하여 후보 리스트에 추가하는 메서드
     */
    private fun addSafetyToCandidateList(
        dateDTO : DateDTO,
        safetyId : String,
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
                getTimeGap(curTime, safetyDTO.time!!, 0, false)
            } else if (safetyDay - curDay < 0) {
                getTimeGap(curTime, safetyDTO.time!!, (safetyDay + 7) - curDay, false)
            } else {
                getTimeGap(curTime, safetyDTO.time!!, safetyDay - curDay, false)
            }
            candidateList.add(WardRecommendDTO(timeGap, safetyId,null, safetyDTO, null))
        }
    }

    /**
     * 피보호자가 보유한 결과에 대해 조건에 따라 결과에 대한 삭제 작업을 결정하는 메서드
     *  - (결과 날짜 < 현재 날짜) : 이전 결과이므로 삭제 작업을 수행 x
     *  - (결과 날짜 = 현재 날짜) : 현재 진행될 수 있는 결과이므로 삭제 작업을 수행 x
     *  - (결과 날짜 > 현재 날짜) : 미리 만들어둔 결과이므로 삭제 작업을 수행
     */
    private fun setResult(
        context : Context,
        dateDTO : DateDTO,
        resultKey : String,
        resultId : String
    ) {
        resultDB.child(resultId).get().addOnSuccessListener { resultSnapshot ->
            if (resultSnapshot.getValue(ResultDTO::class.java) != null) {
                val resultDTO = resultSnapshot.getValue(ResultDTO::class.java) as ResultDTO

                //  answerIdList 가 비어있는 경우 삭제 작업을 수행
                if (resultDTO.answerIdList.isEmpty()) {
                    removeResult(resultKey, resultId, resultDTO)
                }
                //  answerIdList 가 있는 경우 미리 만들어진 결과일 때 삭제
                else if (isPreMadeResult(resultDTO, dateDTO)) {
                    removeResult(resultKey, resultId, resultDTO)
                }
            }
            //  resultIdList 에 resultId 는 있지만 값이 존재하지 않는 경우
            //  resultId 삭제 : ward -> uid -> resultIdList 에 존재하는 resultId 삭제
            else {
                wardDB.child(uid).child("resultIdList").child(resultKey).removeValue()
            }
        }.addOnFailureListener {
            if (!isFail) setNetworkAlarm(context)
        }
    }

    private fun isPreMadeResult(
        resultDTO : ResultDTO,
        dateDTO : DateDTO
    ) : Boolean {
        val curDate = getDateToLong(dateDTO.curDate, dateDTO.curTime)
        val resultDate = getDateToLong(resultDTO.date, resultDTO.safetyTime)

        return resultDate > curDate
    }

    /**
     * resultDTO 에 answer 존재 시 삭제 과정
     *  1. answers 삭제 : answer 에 존재하는 answerIdList 의 모든 answerId 값 삭제
     *  2. result 삭제 : result 에 존재하는 resultId 값 삭제
     *  3. resultId 삭제 : ward -> uid -> resultIdList 에 존재하는 resultId 삭제
     *
     * resultDTO 에 answer 없을 시 삭제 과정
     *  1. result 삭제 : result 에 존재하는 resultId 값 삭제
     *  2. resultId 삭제 : ward -> uid -> resultIdList 에 존재하는 resultId 삭제
     */
    private fun removeResult(
        resultKey : String,
        resultId : String,
        resultDTO : ResultDTO?
    ) {
        for (answer in resultDTO!!.answerIdList) {
            val answerId = answer.value
            answerDB.child(answerId).removeValue()
        }
        resultDB.child(resultId).removeValue()
        wardDB.child(uid).child("resultIdList").child(resultKey).removeValue()
    }

    /**
     * 강제 알람을 세팅하는 메서드
     * 다음과 같은 순서로 result 및 answer 를 세팅 후 강제 알람을 요청
     *  (단, 동일한 시간대의 안부는 단 하나만 적용하도록 함)
     *
     *  1. Create : resultIdList -> resultId
     *  2. Create : resultId -> resultDTO
     *  3. Create : resultDTO.answerList -> [questionId, answerId]
     *  4. Create : answerId -> answerDTO
     *
     * 중간에 생성 실패 시 참조 불가능한 상태를 방지하기 위해 위 순서를 지키면서 생성
     */
    private fun setForceAlarm(
        context: Context,
        recommendDTO: WardRecommendDTO,
        intent: Intent
    ) {
        val cal = Calendar.getInstance()
        val date = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")

        cal.time = Date()
        cal.add(Calendar.SECOND, recommendDTO.timeGap)

        val safetyDate = date.format(cal.time).substring(0, 16)
        val resultDTO = ResultDTO(safetyDate.substring(0, 10),
            recommendDTO.safetyId, recommendDTO.safetyDTO.name!!,
            recommendDTO.safetyDTO.time!!, "미응답", mutableMapOf())
        val resultId = "$uid/$safetyDate"

        recommendDTO.resultId = resultId
        recommendDTO.resultDTO = resultDTO

        // 1. Create : resultIdList -> resultId
        wardDB.child(uid).child("resultIdList").child(safetyDate)
            .setValue(resultId)
            .addOnFailureListener {
                if (!isFail) setNetworkAlarm(context)
            }
        // 2. Create : resultId -> resultDTO
        resultDB.child(resultId).setValue(resultDTO).addOnFailureListener {
            if (!isFail) setNetworkAlarm(context)
        }
        // 3, 4 작업을 수행
        makeAnswerList(context, recommendDTO, safetyDate, intent)
    }

    /**
     * answerList 를 생성하는 메서드
     *  - 생성한 answerId 에 대해 answerList[ questionId ] = answerId
     *  - 질문에 대응하는 대답을 생성
     */
    private fun makeAnswerList(
        context : Context,
        recommendDTO : WardRecommendDTO,
        safetyDate : String,
        intent : Intent
    ) {
        val questionList : MutableMap<String, String> = recommendDTO.safetyDTO.questionList

        for (question in questionList) {
            val questionId = question.key

            questionDB.child(questionId).get().addOnSuccessListener { questionSnapshot ->
                val questionDTO = questionSnapshot.getValue(QuestionDTO::class.java)
                    ?: throw IllegalArgumentException("question required")
                val answerId = "$uid/$safetyDate/$questionId"
                val answerDTO = AnswerDTO(null, questionDTO.secret, questionDTO.record,
                    questionDTO.owner!!, questionDTO.src!!, questionDTO.text!!, "")

                // 3. Create : answerList -> [questionId, answerId]
                resultDB.child(recommendDTO.resultId!!).child("answerIdList").child(questionId).setValue(answerId)
                    .addOnFailureListener {
                        if (!isFail) setNetworkAlarm(context)
                    }
                // 4. Create : answerId -> answerDTO
                answerDB.child(answerId).setValue(answerDTO)
                    .addOnFailureListener {
                        if (!isFail) setNetworkAlarm(context)
                    }
            }.addOnFailureListener {
                if (!isFail) setNetworkAlarm(context)
            }
        }

        Handler().postDelayed({
            setAlarm(context, REPEAT_NOTIFY, recommendDTO, intent)
        }, 5000) // 비동기식 데이터 통신으로 5초의 딜레이를 매꾸는 부분
    }

    /**
     * alarmFlag 에 따라 알람요청을 세팅
     *  - REPEAT_START : recommendAlarm 세팅
     *  - REPEAT_STOP : forceAlarm 세팅
     *  - REPEAT_NOTIFY : notifyAlarm 세팅
     */
    private fun setAlarm(
        context: Context,
        alarmFlag : String,
        recommendDTO : WardRecommendDTO?,
        intent : Intent
    ) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val schedule = Intent(alarmFlag)

        // 통지 알람 및 강제 알람을 위한 추천 안부를 추가
        if (alarmFlag != REPEAT_START) {
            schedule.putExtra("recommendDTO", recommendDTO)
        }
        schedule.setClass(context, WardReceiver::class.java)

        val sender = PendingIntent.getBroadcast(context, 0, schedule,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val interval = Calendar.getInstance()

        interval.timeInMillis = System.currentTimeMillis()

        when (alarmFlag) {
            REPEAT_START -> {
                interval.add(Calendar.SECOND, 5)
            }
            REPEAT_NOTIFY -> {
                // 현재 시간이 가장 근접한 안부로부터 10분 이내의 차이인 경우
                // 알람을 설정하지 않고 바로 통지 알람이 수행되도록 설정
                if (recommendDTO!!.timeGap - 600 <= 0) {
                    notifyAlarm(context, intent, recommendDTO)
                    return
                }
                else {
                    interval.add(Calendar.SECOND, recommendDTO.timeGap - 600)
                }
            }
            REPEAT_STOP -> {
                interval.add(Calendar.SECOND, recommendDTO!!.timeGap)
            }
        }

        if (Build.VERSION.SDK_INT >= 23) {
            alarmManager.setAlarmClock(AlarmManager.AlarmClockInfo(interval.timeInMillis, sender), sender)
        } else if (Build.VERSION.SDK_INT >= 19) {
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, interval.timeInMillis, sender)
        } else {
            alarmManager[AlarmManager.RTC_WAKEUP, interval.timeInMillis] = sender
        }

        screenWakeLock?.release() // 핸드폰이 다시 doze 모드에 빠질 수 있도록 잠금락을 해제
    }

    /**
     * 피보호자에게 미리 안부에 대한 통지 알람을 보내는 메서드
     *  - recommendDTO : 추천 알람에서 선별된 추천 안부
     */
    private fun notifyAlarm(
        context : Context,
        intent : Intent,
        recommendDTO : WardRecommendDTO
    ) {
        val curTime = Calendar.getInstance()
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm")
        val safetyTime = dateFormat.parse(recommendDTO.resultDTO!!.date + " " + recommendDTO.resultDTO!!.safetyTime)
        val forceTime = safetyTime!!.time - curTime.time.time

        // 추천 객체에 강제 알람을 띄우기 위한 초(second) 격차를 재설정
        recommendDTO.timeGap = max(forceTime.toInt() / 1000, 5)
        executeNotifyAlarm(context, recommendDTO.safetyDTO)
        setAlarm(context, REPEAT_STOP, recommendDTO, intent)
    }

    /**
     * 곧 알람이 시작된다는 것을 상단에 notification 을 띄우도록 하는 메서드
     */
    private fun executeNotifyAlarm(
        context : Context,
        safetyDTO : SafetyDTO
    ) {
        val user: Person = Person.Builder()
            .setName("안심 우체국")
            .setIcon(IconCompat.createWithResource(context, R.drawable.relief_post_office))
            .build()
        val body = safetyDTO.name + " 안부가 곧 시작됩니다."
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
            val channelName = "안부 알람 채널"
            val description = "피보호자에게 안부를 미리 알람합니다."
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

        notificationManager.notify(NOTIFICATION_ID , builder.build())
    }

    /**
     * 피보호자 측에게 강제 알람을 띄우도록 하는 메서드
     * recommendDTO 가 피보호자 알람 화면에서 받을 수 있도록 설정
     * 그리고 다시 알람 작업을 수행할 수 있도록 설정
     */
    private fun forceAlarm(
        context : Context,
        intent : Intent,
        recommendDTO : WardRecommendDTO
    ) {
        removeNotification(context)
        executeForceAlarm(context, recommendDTO)
        setAlarm(context, REPEAT_START, null, intent)
    }

    /**
     * 최종적으로 강제 알람을 띄워서 피보호자가 안부를 받을 수 있도록 하는 메서드
     */
    private fun executeForceAlarm(
        context : Context,
        recommendDTO : WardRecommendDTO
    ) {
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

        contentIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        contentIntent.putExtra("recommendDTO", recommendDTO)

        val safetyName = recommendDTO.safetyDTO.name
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            FORCE_ID,
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

        notificationManager.notify(FORCE_ID, builder.build())
    }
}