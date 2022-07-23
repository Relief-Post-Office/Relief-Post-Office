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

class WardReceiver() : BroadcastReceiver() {

    /* 추천할 수 있는 모든 객체들을 담음 */
    private val recommendList = ArrayList<WardRecommendDTO>()

    /* Access to Database */
    private val wardDB = Firebase.database.reference.child("ward")
    private val resultDB = Firebase.database.reference.child("result")
    private val safetyDB = Firebase.database.reference.child("safety")
    private val answerDB = Firebase.database.reference.child("answer")
    private val questionDB = Firebase.database.reference.child("question")

    /* user Id */
    private lateinit var uid : String

    /* Fail flag */
    private var isFail : Boolean = false

    /* WakeLock */
    private var screenWakeLock : PowerManager.WakeLock? = null

    /*
     *  - REPEAT_START : "강제 알람 요청", "요청 없음" 둘 중 하나를 결정하기 위한 플래그
     *  - REPEAT_NOTIFY : 10분 전에 공지 알람 요청을 수행하기 위한 플래그
     *  - REPEAT_STOP : 특정 안부에 대한 강제 알람 요청을 수행하기 위한 플래그
     */
    companion object {
        const val PERMISSION_REPEAT = "com.rightline.backgroundrepeatapp.permission.ACTION_REPEAT"
        const val REPEAT_START = "com.rightline.backgroundrepeatapp.REPEAT_START"
        const val REPEAT_NOTIFY = "com.rightline.backgroundrepeatapp.REPEAT_NOTIFY"
        const val REPEAT_STOP = "com.rightline.backgroundrepeatapp.REPEAT_STOP"
        const val PRIMARY_CHANNEL_ID = "primary_notification_channel"
        const val NOTIFICATION_ID = 100
    }

    /*
     *  알람 요청을 받고 플래그에 따라 특정 작업을 수행하는 메서드
     *  doze 모드에서 알람이 수행하기 위한 방법
     *  - doze 모드에서 빠져나올 수 있도록 wakeLock 을 깨움
     *
     *  알람 요청을 받는 5 가지 케이스
     *  - 1. 피보호자가 메인 화면으로 이동
     *  - 2. 피보호자가 재부팅한 경우
     *  - 3. 보호자가 피호보자의 안부 or 질문을 추가한 경우
     *  - 4. 보호자가 피보호자의 안부 or 질문을 수정한 경우
     *  - 5. 보호자가 피보호자의 안부 or 질문을 삭제한 경우
     */
    override fun onReceive(context: Context, intent: Intent) {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager

        screenWakeLock = powerManager.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK
                or PowerManager.ACQUIRE_CAUSES_WAKEUP, "WakeLock")
        screenWakeLock?.acquire()

        Log.d("확인", "Ward")
        if (!Network.isNetworkAvailable(context)) {
            setNetworkAlarm(context)
        } else {
            if (Firebase.auth.currentUser != null) {
                Log.d("확인", "피보호자측 네트워크 연결 성공")
                uid = Firebase.auth.uid.toString()
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
        }
    }

    /*
     *  네트워크 연결이 안될 경우 실행하는 메서드
     *  15분 단위로 네트워크 알람 요청을 수행
     */
    private fun setNetworkAlarm(context : Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val schedule = Intent(REPEAT_START)

        Log.d("확인", "보호자측 네트워크 연결 실패")
        isFail = true
        schedule.setClass(context, NetworkReceiver::class.java)

        val sender = PendingIntent.getBroadcast(context, 0, schedule,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val interval = Calendar.getInstance()

        interval.timeInMillis = System.currentTimeMillis()
        interval.add(Calendar.MINUTE, 15) /* Here! */
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

        screenWakeLock?.release()
    }

    /*
     *  피보호자의 안부 및 결과에 대한 추천 및 세팅 작업을 수행
     *  1. setSafety 메서드 : 피보호자가 보유한 안부에 대해 추천 객체를 모으도록 수행
     *  2. setResult 메서드 : 피보호자가 보유한 결과에 대해 삭제, 수정 작업을 수행
     *
     *  추천 객체를 전부 모은 경우, timeGap 이 가장 작은 안부를 강제 알람 요청
     *   - ( 피보호자의 알람 시간이 겹치는 케이스를 고려하지 않음 )
     */
    private fun recommendAlarm(context: Context, intent : Intent) {
        val date = SimpleDateFormat("yyyy-MM-dd HH:mm:ss E")
            .format(Date(System.currentTimeMillis()))
        val curDate = date.substring(0, 10)
        val curTime = date.substring(11, 19)
        val curDay = date.split(" ")[2]
        val dateDTO = DateDTO(curDate, curTime, getDay(curDay))

        wardDB.child(uid).get().addOnSuccessListener { snapshot ->
            if (snapshot.getValue(WardDTO::class.java) != null) {
                val wardDTO = snapshot.getValue(WardDTO::class.java) as WardDTO

                for (safety in wardDTO.safetyIdList) {
                    val safetyId = safety.key
                    setSafety(context, dateDTO, safetyId)
                }
                for (result in wardDTO.resultIdList) {
                    val resultKey = result.key
                    val resultId = result.value
                    setResult(context, dateDTO, resultKey, resultId)
                }

                /* 비동기식 데이터 통신으로 인해 5초 후 추천 시작 */
                Handler().postDelayed({
                    if (recommendList.isNotEmpty()) {
                        val recommendDTO = recommendList.minBy { it.timeGap }
                        Log.d("확인[Recommend]", recommendDTO.toString())
                        setForceAlarm(context, recommendDTO, intent)
                    }
                }, 5000)
            }
        }.addOnFailureListener {
            if (!isFail) setNetworkAlarm(context)
        }
    }

    /* 피보호자가 보유한 각각의 안부에 대해 추천 리스트에 추가하도록 세팅하는 메서드 */
    private fun setSafety(context : Context, dateDTO : DateDTO, safetyId : String) {
        safetyDB.child(safetyId).get().addOnSuccessListener { snapshot ->
            if (snapshot.getValue(SafetyDTO::class.java) != null) {
                val safetyDTO = snapshot.getValue(SafetyDTO::class.java) as SafetyDTO
                addSafetyToRecommendList(dateDTO, safetyId, safetyDTO)
            }
        }.addOnFailureListener {
            if (!isFail) setNetworkAlarm(context)
        }
    }

    /* 안부에 설정된 day(ex: "월", "수", "금") 마다 timeGap 을 계산하여 추천 리스트에 추가하는 메서드 */
    private fun addSafetyToRecommendList(dateDTO : DateDTO, safetyId : String, safetyDTO : SafetyDTO) {
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
            recommendList.add(WardRecommendDTO(timeGap, safetyId,null, safetyDTO, null))
        }
    }

    /*
     *  피보호자가 보유한 각각의 결과에 대해 결과가 존재할 경우, 조건에 따라 결과에 대한 삭제 작업을 결정하는 메서드
     *  - (결과 날짜 < 현재 날짜) : 이전 결과이므로 수정 작업을 수행 x
     *  - (결과 날짜 = 현재 날짜) : 현재 진행될 수 있는 결과이므로 수정 작업을 수행 x
     *  - (결과 날짜 > 현재 날짜) : 미리 만들어둔 결과이므로 삭제 작업을 수행
     */
    private fun setResult(context : Context, dateDTO : DateDTO, resultKey : String, resultId : String) {
        val curDate = dateDTO.curDate
        val curTime = dateDTO.curTime

        resultDB.child(resultId).get().addOnSuccessListener { resultSnapshot ->
            if (resultSnapshot.getValue(ResultDTO::class.java) != null) {
                val resultDTO = resultSnapshot.getValue(ResultDTO::class.java) as ResultDTO
                /*
                 *  answerIdList 가 비어있는 경우 : resultDTO 까지 세팅되고 answer 들이 만들어지지 않은 경우
                 *  answerIdList 가 있는 경우 : 완벽하게 만들어진 경우
                 */
                if (resultDTO.answerIdList.isEmpty()) {
                    removeResult(resultKey, resultId, null)
                } else if (getDateToLong(resultDTO.date, resultDTO.safetyTime) > getDateToLong(curDate, curTime)) {
                    removeResult(resultKey, resultId, resultDTO)
                }
            }
            /*  resultIdList 에 resultId 는 있지만 값이 존재하지 않는 경우
             *  resultId 삭제 : ward -> uid -> resultIdList 에 존재하는 resultId 삭제
             */
            else {
                wardDB.child(uid).child("resultIdList").child(resultKey).removeValue()
            }
        }.addOnFailureListener {
            if (!isFail) setNetworkAlarm(context)
        }
    }

    /*
     *  - resultDTO 에 answer 존재 시 삭제 과정
     *  1. answers 삭제 : answer 에 존재하는 answerIdList 의 모든 answerId 값 삭제
     *  2. result 삭제 : result 에 존재하는 resultId 값 삭제
     *  3. resultId 삭제 : ward -> uid -> resultIdList 에 존재하는 resultId 삭제
     *
     *  - resultDTO 에 answer 없을 시 삭제 과정
     *  1. result 삭제 : result 에 존재하는 resultId 값 삭제
     *  2. resultId 삭제 : ward -> uid -> resultIdList 에 존재하는 resultId 삭제
     */
    private fun removeResult(resultKey : String, resultId : String, resultDTO : ResultDTO?) {
        if (resultDTO != null) {
            /* answers 삭제 */
            for (answer in resultDTO.answerIdList) {
                val answerId = answer.value
                answerDB.child(answerId).removeValue()
            }
        }
        /* result 삭제 */
        resultDB.child(resultId).removeValue()
        /* resultId 삭제 */
        wardDB.child(uid).child("resultIdList").child(resultKey).removeValue()
    }

    /*
     *  강제 알람을 세팅하는 메서드
     *  다음과 같은 순서로 result 및 answer 를 세팅 후 강제 알람을 요청
     *  (단, 동일한 시간대의 안부는 단 하나만 적용하도록 함)
     *
     *  1. Create : resultIdList -> resultId
     *  2. Create : resultId -> resultDTO
     *  3. Create : resultDTO.answerList -> [question, answerId]
     *  4. Create : answerId -> answerDTO
     */
    private fun setForceAlarm(context: Context, recommendDTO: WardRecommendDTO, intent: Intent) {
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

        /* 1. Create : resultIdList -> resultId */
        wardDB.child(uid).child("resultIdList").child(safetyDate)
            .setValue(resultId)
            .addOnFailureListener {
                if (!isFail) setNetworkAlarm(context)
            }
        /* 2. Create : resultId -> resultDTO */
        resultDB.child(resultId).setValue(resultDTO).addOnFailureListener {
            if (!isFail) setNetworkAlarm(context)
        }
        /* 3, 4 순서를 수행 */
        makeAnswerList(context, recommendDTO, safetyDate, intent)
    }

    /*
     *  answerList 를 생성하는 메서드
     *  - 생성한 질문 id 에 대해 answerList[질문 id] = 대답 id
     *  - 질문에 대응하는 대답을 생성
     */
    private fun makeAnswerList(context : Context, recommendDTO : WardRecommendDTO, safetyDate : String, intent : Intent) {
        val questionList : MutableMap<String, String> = recommendDTO.safetyDTO.questionList

        for (question in questionList) {
            val questionId = question.key

            questionDB.child(questionId).get().addOnSuccessListener { questionSnapshot ->
                if (questionSnapshot.getValue(QuestionDTO::class.java) != null) {
                    val questionDTO = questionSnapshot.getValue(QuestionDTO::class.java) as QuestionDTO
                    val answerId = "$uid/$safetyDate/$questionId"
                    val answerDTO = AnswerDTO(null, questionDTO.secret, questionDTO.record,
                        questionDTO.owner!!, questionDTO.src!!, questionDTO.text!!, "")

                    /* 3. Create : answerList -> [question, answerId] */
                    resultDB.child(recommendDTO.resultId!!).child("answerIdList").child(questionId).setValue(answerId)
                        .addOnFailureListener {
                            if (!isFail) setNetworkAlarm(context)
                        }
                    /* 4. Create : answerId -> answerDTO */
                    answerDB.child(answerId).setValue(answerDTO)
                        .addOnFailureListener {
                            if (!isFail) setNetworkAlarm(context)
                        }
                }
            }.addOnFailureListener {
                if (!isFail) setNetworkAlarm(context)
            }
        }

        /* 비동기식 데이터 통신으로 인해 5초 후 시작 */
        Handler().postDelayed({
            setAlarm(context, REPEAT_NOTIFY, recommendDTO, intent)
        }, 5000)
    }

    /*
     *  alarmFlag 에 따라 알람요청을 세팅
     *  - REPEAT_STOP : forceAlarm 세팅
     *  - REPEAT_NOTIFY : notifyAlarm 세팅
     *  - REPEAT_START : recommendAlarm 세팅
     *
     *  recommendDTO 를 intent 에 추가하여 넘겨줌
     */
    private fun setAlarm(context: Context, alarmFlag : String, recommendDTO : WardRecommendDTO?, intent : Intent) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val schedule = Intent(alarmFlag)

        if (alarmFlag != REPEAT_START) {
            schedule.putExtra("recommendDTO", recommendDTO)
        }
        schedule.setClass(context, WardReceiver::class.java)

        val sender = PendingIntent.getBroadcast(context, 0, schedule,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val interval = Calendar.getInstance()

        /* 알람 시간 설정(recommendDTO.timeGap) */
        interval.timeInMillis = System.currentTimeMillis()

        if (alarmFlag == REPEAT_NOTIFY) {
            /* 현재 시간이 [안부 시간 - 10(초), 안부 시간] 인 경우 => 무시 */
            if (recommendDTO!!.timeGap - 10 <= 0) {
                setAlarm(context, REPEAT_START, null, intent)
                return
            }
            /* 현재 시간이 [안부시간 - 10(분), 안부시간 - 10(초)) 인 경우 => 통지 알람 */
            else if (recommendDTO.timeGap - 600 <= 0) {
                notifyAlarm(context, intent, recommendDTO)
                return
            }
            /* 현재 시간이 (안부시간 - 10(분)) 전인 경우 => 강제 알람 요청 */
            else {
                interval.add(Calendar.SECOND, recommendDTO.timeGap - 600)
            }
        } else if (alarmFlag == REPEAT_STOP) {
            interval.add(Calendar.SECOND, recommendDTO!!.timeGap)
        } else {
            interval.add(Calendar.SECOND, 5)
        }

        if (Build.VERSION.SDK_INT >= 23) {
            alarmManager.setAlarmClock(AlarmManager.AlarmClockInfo(interval.timeInMillis, sender), sender)
        } else if (Build.VERSION.SDK_INT >= 19) {
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, interval.timeInMillis, sender)
        } else {
            alarmManager[AlarmManager.RTC_WAKEUP, interval.timeInMillis] = sender
        }

        screenWakeLock?.release()
    }

    /*  피보호자에게 미리 안부에 대한 통지 알람을 보내는 메서드  */
    private fun notifyAlarm(context : Context, intent : Intent, recommendDTO : WardRecommendDTO) {
        val curTime = Calendar.getInstance()
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm")
        val safetyTime = dateFormat.parse(recommendDTO.resultDTO!!.date + " " + recommendDTO.resultDTO!!.safetyTime)
        val forceTime = safetyTime!!.time - curTime.time.time

        recommendDTO.timeGap = forceTime.toInt() / 1000
        if (recommendDTO.timeGap < 0)
            recommendDTO.timeGap = 0
        executeNotifyAlarm(context, recommendDTO.safetyDTO)
        setAlarm(context, REPEAT_STOP, recommendDTO, intent)
    }

    private fun executeNotifyAlarm(context : Context, safetyDTO : SafetyDTO) {
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

    /*
     *  피보호자 측에게 강제 알람을 띄우도록 하는 메서드
     *  recommendDTO 를 AlarmActivity 에서 받을 수 있도록 설정
     *  그리고 다시 알람 작업을 수행할 수 있도록 설정
     */
    private fun forceAlarm(context : Context, intent : Intent, recommendDTO : WardRecommendDTO) {
        executeForceAlarm(context, recommendDTO)
        setAlarm(context, REPEAT_START, null, intent)
    }

    private fun executeForceAlarm(context : Context, recommendDTO : WardRecommendDTO) {
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

        contentIntent.putExtra("recommendDTO", recommendDTO)

        val safetyName = recommendDTO.safetyDTO.name
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
}