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
import com.seoul42.relief_post_office.util.Network
import com.seoul42.relief_post_office.ward.AlarmActivity
import java.text.SimpleDateFormat
import java.util.*

class WardReceiver() : BroadcastReceiver() {

    /* 추천할 수 있는 모든 객체들을 담음 */
    private val recommendList = ArrayList<WardRecommendDTO>()

    /* 안부 id 에 대응하는 결과 id 를 가져오기 위한 용도 */
    private val resultMap : MutableMap<String, String> = mutableMapOf()

    /* Access to Database */
    private val wardDB = Firebase.database.reference.child("ward")
    private val resultDB = Firebase.database.reference.child("result")
    private val safetyDB = Firebase.database.reference.child("safety")
    private val answerDB = Firebase.database.reference.child("answer")
    private val questionDB = Firebase.database.reference.child("question")

    /*
     *  - REPEAT_START : "강제 알람 요청", "요청 없음" 둘 중 하나를 결정하기 위한 플래그
     *  - REPEAT_STOP : 특정 안부에 대한 강제 알람 요청을 수행하기 위한 플래그
     */
    companion object {
        const val PERMISSION_REPEAT = "com.rightline.backgroundrepeatapp.permission.ACTION_REPEAT"
        const val REPEAT_START = "com.rightline.backgroundrepeatapp.REPEAT_START"
        const val REPEAT_STOP = "com.rightline.backgroundrepeatapp.REPEAT_STOP"
        const val PRIMARY_CHANNEL_ID = "primary_notification_channel"
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
            if (Firebase.auth.currentUser != null && Alarm.isIgnoringBatteryOptimizations(context)) {
                Log.d("확인", "피보호자측 네트워크 연결 성공")
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
     *  5분 단위로 네트워크 알람 요청을 수행
     */
    private fun setNetworkAlarm(context : Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val schedule = Intent(NetworkReceiver.REPEAT_START)

        schedule.setClass(context, NetworkReceiver::class.java)

        val sender = PendingIntent.getBroadcast(context, 0, schedule,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val interval = Calendar.getInstance()

        interval.timeInMillis = System.currentTimeMillis()
        interval.add(Calendar.MINUTE, 5) /* Here! */
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
        val uid = Firebase.auth.uid.toString()

        wardDB.child(uid).get().addOnSuccessListener { snapshot ->
            if (snapshot.getValue(WardDTO::class.java) != null) {
                var safetyId : String
                var resultId : String
                val wardDTO = snapshot.getValue(WardDTO::class.java) as WardDTO

                for (safety in wardDTO.safetyIdList) {
                    safetyId = safety.key
                    setSafety(dateDTO, safetyId)
                }
                for (result in wardDTO.resultIdList) {
                    resultId = result.key
                    setResult(dateDTO, resultId)
                }

                /* 비동기식 데이터 통신으로 인해 5초 후 추천 시작 */
                Handler().postDelayed({
                    if (recommendList.isNotEmpty()) {
                        val recommendDTO = recommendList.minBy { it.timeGap }
                        Log.d("Recommend", recommendDTO.toString())
                        setForceAlarm(context, recommendDTO)
                    }
                }, 5000)
            }
        }
    }

    /* 피보호자가 보유한 각각의 안부에 대해 추천 리스트에 추가하도록 세팅하는 메서드 */
    private fun setSafety(dateDTO : DateDTO, safetyId : String) {
        safetyDB.child(safetyId).get().addOnSuccessListener { snapshot ->
            if (snapshot.getValue(SafetyDTO::class.java) != null) {
                val safetyDTO = snapshot.getValue(SafetyDTO::class.java) as SafetyDTO
                addSafetyToRecommendList(dateDTO, safetyId, safetyDTO)
            }
        }
    }

    /* 안부에 설정된 day(ex: "월", "수", "금") 마다 timeGap 을 계산하여 추천 리스트에 추가하는 메서드 */
    private fun addSafetyToRecommendList(dateDTO : DateDTO, safetyId : String, safetyDTO : SafetyDTO) {
        val curTime = dateDTO.curTime
        val curDay = dateDTO.curDay
        var safetyDay : Int
        var timeGap : Int

        for (day in safetyDTO.dayOfWeek) {
            if (!day.value) {
                continue
            }
            safetyDay = getDay(day.key)
            timeGap = if (curDay == safetyDay) {
                getTimeGap(curTime, safetyDTO.time!!, 0)
            } else if (safetyDay - curDay < 0) {
                getTimeGap(curTime, safetyDTO.time!!, (safetyDay + 7) - curDay)
            } else {
                getTimeGap(curTime, safetyDTO.time!!, safetyDay - curDay)
            }
            recommendList.add(WardRecommendDTO(timeGap, safetyId,null, safetyDTO))
        }
    }

    /*
     *  피보호자가 보유한 각각의 결과에 대해 결과가 존재할 경우, 조건에 따라 결과에 대한 수정 작업을 결정하는 메서드
     *  - (결과 날짜 < 현재 날짜) : 이전 결과이므로 수정 작업을 수행 x
     *  - (결과 날짜 = 현재 날짜) : 현재 진행될 수 있는 결과이므로 수정 작업을 수행 x
     *  - (결과 날짜 > 현재 날짜) : 미리 만들어둔 결과이므로 수정 작업을 수행 o
     */
    private fun setResult(dateDTO : DateDTO, resultId : String) {
        val curDate = dateDTO.curDate
        val curTime = dateDTO.curTime

        resultDB.child(resultId).get().addOnSuccessListener { resultSnapshot ->
            if (resultSnapshot.getValue(ResultDTO::class.java) != null) {
                val resultDTO = resultSnapshot.getValue(ResultDTO::class.java) as ResultDTO
                if (getDateToLong(resultDTO.date, resultDTO.safetyTime) > getDateToLong(curDate, curTime)) {
                    updateAndRemoveResult(resultId, resultDTO)
                }
            }
        }
    }

    /*
     *  결과의 업데이트를 수행하는 메서드
     *  - 결과에 대응하는 안부가 존재하지 않을 경우 : 결과 삭제
     *  - 결과에 대응하는 안부와 시간이 동일하지 않을 경우 : 결과 삭제
     *  - 결과에 대응하는 안부와 시간이 동일할 경우 : 결과 수정
     */
    private fun updateAndRemoveResult(resultId : String, resultDTO : ResultDTO) {
        safetyDB.child(resultDTO.safetyId).get().addOnSuccessListener { safetySnapshot ->
            if (safetySnapshot.getValue(SafetyDTO::class.java) != null) {
                val safetyDTO = safetySnapshot.getValue(SafetyDTO::class.java) as SafetyDTO
                if (resultDTO.safetyTime == safetyDTO.time) {
                    updateResult(resultId, resultDTO, safetyDTO)
                } else {
                    removeResult(resultId, resultDTO)
                }
            } else {
                removeResult(resultId, resultDTO)
            }
        }
    }

    /*
     *  결과 수정 작업 3 가지
     *  1. 이름 변경 : 결과(result) 이름을 대응하는 안부의 이름으로 변경
     *  2. 응답 목록 변경 : 응답(answer) 목록에 대응하는 안부의 질문 목록에 맞게 변경
     *  3. 결과 반영 : resultId 에 변경된 값을 세팅
     */
    private fun updateResult(resultId : String, resultDTO : ResultDTO, safetyDTO : SafetyDTO) {
        val questionList = safetyDTO.questionList

        resultMap[resultDTO.safetyId] = resultId
        /* 1. 이름 변경 */
        resultDTO.safetyName = safetyDTO.name!!

        /* 2. 응답 목록 변경 */
        for (answer in resultDTO.answerIdList) {
            val answerId = answer.value
            answerDB.child(answerId).removeValue()
        }
        resultDTO.answerIdList.clear()

        for (question in questionList) {
            val questionId = question.key

            questionDB.child(questionId).get().addOnSuccessListener { questionSnapshot ->
                if (questionSnapshot.getValue(QuestionDTO::class.java) != null) {
                    val questionDTO = questionSnapshot.getValue(QuestionDTO::class.java) as QuestionDTO
                    val answerKey = answerDB.push()
                    val answerId = answerKey.key.toString()
                    val answerDTO = AnswerDTO(null, questionDTO.secret, questionDTO.record,
                        questionDTO.owner!!, questionDTO.src!!, questionDTO.text!!, "")

                    answerKey.setValue(answerDTO)
                    resultDTO.answerIdList[questionId] = answerId /* answerIdList 채우기 [questionId, answerId] */
                }
            }
        }

        /* 비동기식 데이터 통신으로 인해 1초 후 결과 반영 */
        Handler().postDelayed({
            resultDB.child(resultId).setValue(resultDTO)
        }, 1000)
    }

    /*
     *  결과 삭제 작업 3 가지
     *  1. answers 삭제 : answer 에 존재하는 answerIdList 의 모든 answerId 의 값 삭제
     *  2. result 삭제 : result 에 존재하는 resultId 의 값 삭제
     *  3. resultId 삭제 : ward -> uid -> resultIdList 에 존재하는 resultId 를 삭제
     */
    private fun removeResult(resultId : String, resultDTO : ResultDTO) {
        val uid = Firebase.auth.uid.toString()

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
     *  강제 알람을 세팅하는 작업 수행
     *  - 안부 id 에 대응하는 결과 id 가 없는 경우 : 결과 생성 후 강제 알람 요청을 수행
     *  - 안부 id 에 대응하는 결과 id 가 있는 경우 : resultId 를 추가하고 강제 알람 요청을 바로 수행
     */
    private fun setForceAlarm(context: Context, recommendDTO : WardRecommendDTO) {
        if (resultMap[recommendDTO.safetyId] == null) {
            makeResultAndSetAlarm(context, recommendDTO)
        } else {
            recommendDTO.resultId = resultMap[recommendDTO.safetyId]
            setAlarm(context, REPEAT_STOP, recommendDTO)
        }
    }

    /*
     *  결과를 생성하는 메서드
     *  1. result 의 answerList 생성 : 안부의 질문에 대응하는 answer 를 생성 및 추가
     *  2. result 생성
     *   - date = 현재 날짜 + ( 현재로부터 안부까지 timeGap )
     *   - responseTime = "미응답"
     */
    private fun makeResultAndSetAlarm(context: Context, recommendDTO : WardRecommendDTO) {
        val uid = Firebase.auth.uid.toString()
        val questionList = recommendDTO.safetyDTO.questionList
        val answerList : MutableMap<String, String> = mutableMapOf()
        val cal = Calendar.getInstance()
        val date = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")

        cal.time = Date()
        cal.add(Calendar.SECOND, recommendDTO.timeGap)

        /* 1. result 의 answerList 생성 */
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
            }
        }

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
        }, 1000)
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
            interval.add(Calendar.SECOND, recommendDTO!!.timeGap)
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
        safetyDB.child(recommendDTO.safetyId).get().addOnSuccessListener {
            if (it.getValue(SafetyDTO::class.java) != null) {
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
                    0,
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

                notificationManager.notify(0, builder.build())
            }
        }

        setAlarm(context, REPEAT_START, null)
    }

    /* Start alarm util */
    private fun getDay(curDay : String) : Int {
        return when(curDay) {
            "월" -> 1
            "화" -> 2
            "수" -> 3
            "목" -> 4
            "금" -> 5
            "토" -> 6
            else -> 7
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

    private fun getDateToLong(date : String, time : String) : Long {
        val dateArray = date.split("-")
        val timeArray = time.split(":")
        val dateTime = dateArray[0] + dateArray[1] + dateArray[2] +
                timeArray[0] + timeArray[1]

        return dateTime.toLong() /* date = "2022-07-12", time = "13:25" => 202207121325 (type : long) */
    }
    /* End alarm util */
}