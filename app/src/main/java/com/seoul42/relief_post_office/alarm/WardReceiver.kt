package com.seoul42.relief_post_office.alarm

import android.app.*
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Handler
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.seoul42.relief_post_office.R
import com.seoul42.relief_post_office.model.*
import com.seoul42.relief_post_office.util.Alarm
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

    /*
     *  - REPEAT_START : "강제 알람 요청", "요청 없음" 둘 중 하나를 결정하기 위한 플래그
     *  - REPEAT_STOP : 특정 안부에 대한 강제 알람 요청을 수행하기 위한 플래그
     */
    companion object {
        const val PERMISSION_REPEAT = "com.rightline.backgroundrepeatapp.permission.ACTION_REPEAT"
        const val REPEAT_START = "com.rightline.backgroundrepeatapp.REPEAT_START"
        const val REPEAT_STOP = "com.rightline.backgroundrepeatapp.REPEAT_STOP"
        const val PRIMARY_CHANNEL_ID = "primary_notification_channel"
        const val NOTIFICATION_ID = 100
    }

    /*
     *  알람 요청을 받고 플래그에 따라 특정 작업을 수행하는 메서드
     *
     *  알람 요청을 받는 5 가지 케이스
     *  - 1. 피보호자가 메인 화면으로 이동
     *  - 2. 피보호자가 재부팅한 경우
     *  - 3. 보호자가 피호보자의 안부를 추가한 경우
     *  - 4. 보호자가 피보호자의 안부를 수정한 경우
     *  - 5. 보호자가 피보호자의 안부를 삭제한 경우
     */
    override fun onReceive(context: Context, intent: Intent) {
        Log.d("확인", "Ward")
        if (!Network.isNetworkAvailable(context)) {
            Log.d("확인", "피보호자측 네트워크 연결 실패")
            setNetworkAlarm(context)
        } else {
            if (Firebase.auth.currentUser != null) {
                Log.d("확인", "피보호자측 네트워크 연결 성공")
                uid = Firebase.auth.uid.toString()
                when (intent.action) {
                    REPEAT_START -> {
                        recommend(context)
                    }
                    REPEAT_STOP -> {
                        val recommendDTO =
                            intent.getSerializableExtra("recommendDTO") as WardRecommendDTO
                        forceAlarm(context, recommendDTO)
                    }
                }
            }
        }
    }

    /*
     *  네트워크 연결이 안될 경우 실행하는 메서드
     *  30초 단위로 네트워크 알람 요청을 수행
     */
    private fun setNetworkAlarm(context : Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val schedule = Intent(REPEAT_START)

        schedule.setClass(context, NetworkReceiver::class.java)

        val sender = PendingIntent.getBroadcast(context, 0, schedule,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val interval = Calendar.getInstance()

        interval.timeInMillis = System.currentTimeMillis()
        interval.add(Calendar.SECOND, 30) /* Here! */
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

    /*
     *  피보호자의 안부 및 결과에 대한 추천 및 세팅 작업을 수행
     *  1. setSafety 메서드 : 피보호자가 보유한 안부에 대해 추천 객체를 모으도록 수행
     *  2. setResult 메서드 : 피보호자가 보유한 결과에 대해 삭제, 수정 작업을 수행
     *
     *  추천 객체를 전부 모은 경우, timeGap 이 가장 작은 안부를 강제 알람 요청
     *   - ( 피보호자의 알람 시간이 겹치는 케이스를 고려하지 않음 )
     */
    private fun recommend(context: Context) {
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
                    val resultId = result.key
                    setResult(context, dateDTO, resultId)
                }

                /* 비동기식 데이터 통신으로 인해 5초 후 추천 시작 */
                Handler().postDelayed({
                    if (recommendList.isNotEmpty()) {
                        val recommendDTO = recommendList.minBy { it.timeGap }
                        Log.d("확인[Recommend]", recommendDTO.toString())
                        setForceAlarm(context, recommendDTO)
                    }
                }, 5000)
            }
        }.addOnFailureListener {
            if (!isFail) {
                Log.d("확인", "피보호자측 네트워크 연결 실패")
                isFail = true
                setNetworkAlarm(context)
            }
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
            if (!isFail) {
                Log.d("확인", "피보호자측 네트워크 연결 실패")
                isFail = true
                setNetworkAlarm(context)
            }
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
            recommendList.add(WardRecommendDTO(timeGap, safetyId,null, safetyDTO))
        }
    }

    /*
     *  피보호자가 보유한 각각의 결과에 대해 결과가 존재할 경우, 조건에 따라 결과에 대한 삭제 작업을 결정하는 메서드
     *  - (결과 날짜 < 현재 날짜) : 이전 결과이므로 수정 작업을 수행 x
     *  - (결과 날짜 = 현재 날짜) : 현재 진행될 수 있는 결과이므로 수정 작업을 수행 x
     *  - (결과 날짜 > 현재 날짜) : 미리 만들어둔 결과이므로 삭제 작업을 수행
     */
    private fun setResult(context : Context, dateDTO : DateDTO, resultId : String) {
        val curDate = dateDTO.curDate
        val curTime = dateDTO.curTime

        resultDB.child(resultId).get().addOnSuccessListener { resultSnapshot ->
            if (resultSnapshot.getValue(ResultDTO::class.java) != null) {
                val resultDTO = resultSnapshot.getValue(ResultDTO::class.java) as ResultDTO
                if (getDateToLong(resultDTO.date, resultDTO.safetyTime) > getDateToLong(curDate, curTime)) {
                    removeResult(resultId, resultDTO)
                }
            }
        }.addOnFailureListener {
            if (!isFail) {
                Log.d("확인", "피보호자측 네트워크 연결 실패")
                isFail = true
                setNetworkAlarm(context)
            }
        }
    }

    /*
     *  결과 삭제 작업 3 가지
     *  1. answers 삭제 : answer 에 존재하는 answerIdList 의 모든 answerId 의 값 삭제
     *  2. result 삭제 : result 에 존재하는 resultId 의 값 삭제
     *  3. resultId 삭제 : ward -> uid -> resultIdList 에 존재하는 resultId 를 삭제
     */
    private fun removeResult(resultId : String, resultDTO : ResultDTO) {
        /* 1. answers 삭제 */
        for (answer in resultDTO.answerIdList) {
            val answerId = answer.value
            answerDB.child(answerId).removeValue()
        }
        /* 2. result 삭제 */
        resultDB.child(resultId).removeValue()
        /* 3. resultId 삭제 */
        wardDB.child(uid).child("resultIdList").child(resultId).removeValue()
    }

    /*
     *  강제 알람을 세팅하는 메서드
     *  결과를 생성한 다음에 강제 알람을 요청
     *  1. result 의 answerList 생성 : 안부의 질문에 대응하는 answer 를 생성 및 추가
     *  2. result 생성
     *   - date = 현재 날짜 + ( 현재로부터 안부까지 timeGap )
     *   - responseTime = "미응답"
     */
    private fun setForceAlarm(context: Context, recommendDTO : WardRecommendDTO) {
        val answerList : MutableMap<String, String> = mutableMapOf()
        val cal = Calendar.getInstance()
        val date = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")

        cal.time = Date()
        cal.add(Calendar.SECOND, recommendDTO.timeGap)
        Log.d("확인용", date.format(cal.time))

        /* 1. result 의 answerList 생성 */
        makeAnswerList(context, recommendDTO.safetyDTO.questionList, answerList)

        /* 2. result 생성 */
        Handler().postDelayed({
            val resultKey = resultDB.push()
            val resultId = resultKey.key.toString()
            val resultDTO = ResultDTO(date.format(cal.time).substring(0, 10),
                recommendDTO.safetyId, recommendDTO.safetyDTO.name!!,
                recommendDTO.safetyDTO.time!!, "미응답", answerList)

            resultKey.setValue(resultDTO)
            wardDB.child(uid).child("resultIdList").child(resultId).setValue(resultId)
            recommendDTO.resultId = resultId
            setAlarm(context, REPEAT_STOP, recommendDTO)
        }, 5000)
    }

    /*
     *  answerList 를 생성하는 메서드
     *  - 질문에 대응하는 대답을 생성
     *  - 생성한 질문 id 에 대해 answerList[질문 id] = 대답 id
     */
    private fun makeAnswerList(context : Context, questionList : MutableMap<String, String>, answerList : MutableMap<String, String>) {
        for (question in questionList) {
            val questionId = question.key

            questionDB.child(questionId).get().addOnSuccessListener { questionSnapshot ->
                if (questionSnapshot.getValue(QuestionDTO::class.java) != null) {
                    val questionDTO = questionSnapshot.getValue(QuestionDTO::class.java) as QuestionDTO
                    val answerKey = answerDB.push()
                    val answerId = answerKey.key.toString()
                    val answerDTO = AnswerDTO(null, questionDTO.secret, questionDTO.record,
                        questionDTO.owner!!, questionDTO.src!!, questionDTO.text!!, "")

                    answerKey.setValue(answerDTO) /* answer 새로 생성 */
                    answerList[questionId] = answerId /* answerIdList 채우기 [questionId, answerId] */
                }
            }.addOnFailureListener {
                if (!isFail) {
                    Log.d("확인", "피보호자측 네트워크 연결 실패")
                    isFail = true
                    setNetworkAlarm(context)
                }
            }
        }
    }

    /*
     *  강제 알람을 요청하는 작업을 수행
     *  recommendDTO 를 intent 에 추가하여 넘겨줌
     */
    private fun setAlarm(context: Context, alarmFlag : String, recommendDTO : WardRecommendDTO?) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val schedule = Intent(alarmFlag)

        if (alarmFlag == REPEAT_STOP) {
            schedule.putExtra("recommendDTO", recommendDTO)
        }
        schedule.setClass(context, WardReceiver::class.java)

        val sender = PendingIntent.getBroadcast(context, 0, schedule,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val interval = Calendar.getInstance()

        /* 알람 시간 설정(recommendDTO.timeGap) */
        interval.timeInMillis = System.currentTimeMillis()
        if (alarmFlag == REPEAT_STOP) {
            val timeGap = if (recommendDTO!!.timeGap - 10 < 0) {
                0
            }
            else {
                recommendDTO.timeGap - 10
            }
            interval.add(Calendar.SECOND, timeGap)
        } else {
            interval.add(Calendar.SECOND, 1)
        }

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

    /*
     *  피보호자 측에게 강제 알람을 띄우도록 하는 메서드
     *  recommendDTO 를 AlarmActivity 에서 받을 수 있도록 설정
     *  그리고 다시 알람 작업을 수행할 수 있도록 설정
     *  안부 id 가 유효한지를 확인해서 강제 알람을 띄움
     */
    private fun forceAlarm(context : Context, recommendDTO : WardRecommendDTO) {
        val date = SimpleDateFormat("HH:mm E")
            .format(Date(System.currentTimeMillis()))
        val curTime = date.substring(0, 5)
        val curDay = date[6].toString()

        safetyDB.child(recommendDTO.safetyId).get().addOnSuccessListener {
            if (it.getValue(SafetyDTO::class.java) != null) {
                val safetyDTO = it.getValue(SafetyDTO::class.java) as SafetyDTO

                if (safetyDTO.dayOfWeek[curDay] == true && safetyDTO.time == curTime) {
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
        }.addOnFailureListener {
            if (!isFail) {
                Log.d("확인", "피보호자측 네트워크 연결 실패")
                isFail = true
                setNetworkAlarm(context)
            }
        }

        setAlarm(context, REPEAT_START, null)
    }
}