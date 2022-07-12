package com.seoul42.relief_post_office.alarm

import android.app.*
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Handler
import android.system.Os.close
import android.util.Log
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat
import androidx.fragment.app.viewModels
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.seoul42.relief_post_office.R
import com.seoul42.relief_post_office.alarm.BootCompleteReceiver.Companion.REPEAT_FORCE
import com.seoul42.relief_post_office.alarm.BootCompleteReceiver.Companion.REPEAT_PUSH
import com.seoul42.relief_post_office.model.*
import com.seoul42.relief_post_office.util.Guardian
import com.seoul42.relief_post_office.viewmodel.FirebaseViewModel
import java.io.Serializable
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.util.*
import kotlin.properties.ReadOnlyProperty

class WardReceiver() : BroadcastReceiver() {

    /* Class for recommend */
    data class RecommendDTO(
        val force: Boolean,
        val timeGap: Int,
        val safetyId: String,
        val safetyDTO: SafetyDTO,
        var resultId: String?,
        var resultDTO: ResultDTO?
    ) : Serializable

    /* Collection for recommend */
    private val recommendList = ArrayList<RecommendDTO>() /* 추천할 수 있는 모든 추천 객체들을 담음 */
    private val resultMap : MutableMap<String, String> = mutableMapOf() /* key = safetyId, value = resultId */

    /* Access to database */
    private val userDB = Firebase.database.reference.child("user")
    private val wardDB = Firebase.database.reference.child("ward")
    private val resultDB = Firebase.database.reference.child("result")
    private val safetyDB = Firebase.database.reference.child("safety")
    private val answerDB = Firebase.database.reference.child("answer")
    private val questionDB = Firebase.database.reference.child("question")

    /*
     * REPEAT_START : "강제 알람 요청", "푸시 알람 요청", "요청 없음" 셋 중 하나를 결정하기 위한 flag
     * REPEAT_FORCE : 특정 안부에 대한 강제 알람 요청을 수행하기 위한 flag
     * REPEAT_PUSH : 피보호자와 연결된 모든 보호자들에게 푸시 알람을 수행하기 위한 flag
     */
    companion object {
        const val PERMISSION_REPEAT = "com.rightline.backgroundrepeatapp.permission.ACTION_REPEAT"
        const val REPEAT_START = "com.rightline.backgroundrepeatapp.REPEAT_START"
        const val REPEAT_FORCE = "com.rightline.backgroundrepeatapp.REPEAT_FORCE"
        const val REPEAT_PUSH = "com.rightline.backgroundrepeatapp.REPEAT_PUSH"
        const val PRIMARY_CHANNEL_ID = "primary_notification_channel"
        const val NOTIFICATION_ID = 0
    }

    /* 알람 요청을 받고 flag 에 따라 특정 작업을 수행하는 메서드 */
    override fun onReceive(context: Context, intent: Intent) {
        /* 무조건 로그인된 피보호자여야 함 */
        if (Firebase.auth.currentUser != null) {
            when (intent.action) {
                /* 최초 수행 및 "강제 알람 요청", "푸시 알람 요청", "요청 없음" 셋 중 하나 결정 */
                REPEAT_START -> {
                    recommend(context)
                }
                /* 강제 알람을 수행 */
                REPEAT_FORCE -> {
                    val recommendDTO = intent.getSerializableExtra("recommendDTO") as RecommendDTO
                    forceAlarm(context, recommendDTO)
                }
                /* 푸시 알람을 수행 */
                REPEAT_PUSH -> {
                    val recommendDTO = intent.getSerializableExtra("recommendDTO") as RecommendDTO
                    pushAlarm(context, recommendDTO)
                }
            }
        }
    }

    /*
    *  alarmFlag = REPEAT_FORCE or REPEAT_PUSH
    *   - REPEAT_FORCE : 강제 알람을 요청하기 위한 flag
    *   - REPEAT_PUSH : 푸시 알람을 요청하기 위한 flag
    *
    *  recommendDTO = 나중에
    *
    */
    private fun setAlarm(context: Context, alarmFlag : String, recommendDTO : RecommendDTO) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val schedule = Intent(alarmFlag)

        schedule.putExtra("recommendDTO", recommendDTO)
        schedule.setClass(context, WardReceiver::class.java)

        val sender = PendingIntent.getBroadcast(context, 0, schedule,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val interval = Calendar.getInstance()

        /* 알람 시간 설정(recommendDTO.timeGap) */
        interval.timeInMillis = System.currentTimeMillis()
        interval.add(Calendar.SECOND, recommendDTO.timeGap)
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

    private fun setForceAlarm(context: Context, recommendDTO : RecommendDTO) {
        var resultDTO : ResultDTO

        if (resultMap[recommendDTO.safetyId] == null) {
            makeResultAndSetAlarm(context, recommendDTO)
        } else {
            recommendDTO.resultId = resultMap[recommendDTO.safetyId]
            resultDB.child(recommendDTO.resultId!!).get().addOnSuccessListener {
                if (it.getValue(ResultDTO::class.java) != null) {
                    resultDTO = it.getValue(ResultDTO::class.java) as ResultDTO
                    recommendDTO.resultDTO = resultDTO
                    setAlarm(context, REPEAT_FORCE, recommendDTO)
                }
            }
        }
    }

    /* 결과를 생성하는 메서드 */
    private fun makeResultAndSetAlarm(context: Context, recommendDTO : RecommendDTO) {
        val uid = Firebase.auth.uid.toString()
        val questionList = recommendDTO.safetyDTO.questionList
        val answerList : MutableMap<String, String> = mutableMapOf()
        val cal = Calendar.getInstance()
        val date = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")

        cal.time = Date()
        cal.add(Calendar.SECOND, recommendDTO.timeGap)
        Log.d("확인용[makeResultAndSetAlarm]", date.format(cal.time))

        for (question in questionList) {
            val questionId = question.key

            questionDB.child(questionId).get().addOnSuccessListener { questionSnapshot ->
                if (questionSnapshot.getValue(QuestionDTO::class.java) != null) {
                    val questionDTO = questionSnapshot.getValue(QuestionDTO::class.java) as QuestionDTO
                    val answerKey = answerDB.push()
                    val answerId = answerKey.key.toString()
                    val answerDTO = AnswerDTO(null, questionDTO.secret, questionDTO.record,
                        questionDTO.owner!!, questionDTO.src!!, questionDTO.text!!, "")

                    Log.d("확인용[makeResultAndSetAlarm]", "answerKey = $answerKey, answerId = $answerId")
                    answerKey.setValue(answerDTO) /* answer 새로 생성 */
                    answerList[questionId] = answerId /* answerIdList 채우기 [questionId, answerId] */
                }
            }
        }

        Handler().postDelayed({
            val resultKey = resultDB.push()
            val resultId = resultKey.key.toString()
            val resultDTO = ResultDTO(date.format(cal.time).substring(0, 10),
                recommendDTO.safetyId, recommendDTO.safetyDTO.name!!,
                recommendDTO.safetyDTO.time!!, "미응답", answerList)

            resultKey.setValue(resultDTO)
            wardDB.child(uid).child("resultIdList").child(resultId).setValue(resultId)

            Log.d("확인용[makeResultAndSetAlarm]", "resultKey = $resultKey, resultId = $resultId")
            recommendDTO.resultId = resultId
            recommendDTO.resultDTO = resultDTO
            setAlarm(context, REPEAT_FORCE, recommendDTO)
        }, 1000)
    }

    /*
     * 현재 로그인한 피보호자의 안부 및 결과 탐색
     *  - 안부 탐색 : 강제 알람 요청을 위한 용도
     *  - 결과 탐색 : 푸시 알람 요청을 위한 용도
     */
    private fun recommend(context: Context) {
        val date = SimpleDateFormat("yyyy-MM-dd HH:mm:ss E")
            .format(Date(System.currentTimeMillis()))
        val curDate = date.substring(0, 10)
        val curTime = date.substring(11, 19)
        val curDay = date.split(" ")[2]
        val uid = Firebase.auth.uid.toString()

        wardDB.child(uid).get().addOnSuccessListener { snapshot ->
            if (snapshot.getValue(WardDTO::class.java) != null) {
                var safetyId : String
                var resultId : String
                val wardDTO = snapshot.getValue(WardDTO::class.java) as WardDTO

                Log.d("확인용[recommend]", "safetyIdList => " + wardDTO.safetyIdList.toString())
                Log.d("확인용[recommend]", "resultIdList => " + wardDTO.resultIdList.toString())
                for (safety in wardDTO.safetyIdList) {
                    safetyId = safety.key
                    setSafety(curTime, getDay(curDay), safetyId)
                }
                for (result in wardDTO.resultIdList) {
                    resultId = result.key
                    setResult(curDate, curTime, curDay, resultId)
                }

                /* 비동기식 데이터 통신으로 인해 5초 후 추천 시작 */
                Handler().postDelayed({
                    if (recommendList.isNotEmpty()) {
                        recommendList.sortBy { it.timeGap } /* 시각적 편의를 위한 정렬 */
                        Log.d("확인용[recommend]", recommendList.toString())
                        val recommendDTO = recommendList.minBy { it.timeGap }
                        /* 강제 알람 요청인 경우 */
                        if (recommendDTO.force) {
                            setForceAlarm(context, recommendDTO)
                        }
                        /* 푸시 알람 요청인 경우 */
                        else {
                            setAlarm(context, REPEAT_PUSH, recommendDTO)
                        }
                    } else {
                        Log.d("확인용[recommend]", "recommendList is empty")
                    }
                }, 5000)
            }
        }
    }

    /* 피보호자가 보유한 각각의 안부에 대해 안부가 존재할 경우, 추천 리스트에 추가하도록 결정하는 메서드 */
    private fun setSafety(curTime : String, curDay : Int, safetyId : String) {
        safetyDB.child(safetyId).get().addOnSuccessListener { snapshot ->
            if (snapshot.getValue(SafetyDTO::class.java) != null) {
                val safetyDTO = snapshot.getValue(SafetyDTO::class.java) as SafetyDTO

                Log.d("확인용[setSafety]", safetyDTO.toString())
                addSafetyToRecommendList(curTime, curDay, safetyId, safetyDTO)
            }
        }
    }

    /* 안부에 설정된 day(ex: "월", "수", "금") 마다 timeGap 을 계산하여 추천 리스트에 추가하는 메서드 */
    private fun addSafetyToRecommendList(curTime : String, curDay : Int, safetyId : String, safetyDTO : SafetyDTO) {
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
            recommendList.add(RecommendDTO(true, timeGap, safetyId, safetyDTO, null, null))
        }
    }

    /*
     * 피보호자가 보유한 각각의 결과에 대해 결과가 존재할 경우, 조건에 따라 결과에 대한 수정 작업을 결정하는 메서드
     *  - (결과 날짜 < 현재 날짜) : 이전 결과이므로 수정 작업을 수행 x
     *  - (결과 날짜 = 현재 날짜) : 현재 진행될 수 있는 결과이므로 수정 작업을 수행 x
     *  - (결과 날짜 > 현재 날짜) : 미리 만들어둔 결과이므로 수정 작업을 수행 o
     */
    private fun setResult(curDate : String, curTime : String, curDay : String, resultId : String) {
        resultDB.child(resultId).get().addOnSuccessListener { resultSnapshot ->
            if (resultSnapshot.getValue(ResultDTO::class.java) != null) {
                val resultDTO = resultSnapshot.getValue(ResultDTO::class.java) as ResultDTO

                Log.d("확인용[setResult]", resultDTO.toString())
                if (getDateToLong(resultDTO.date, resultDTO.safetyTime) > getDateToLong(curDate, curTime)) {
                    Log.d("확인용[setResult]", resultDTO.date + " " + resultDTO.safetyTime + " > " + curDate + " " + curTime)
                    updateResult(curTime, curDay, resultId, resultDTO)
                }
            }
        }
    }

    /*
     * 결과의 업데이트를 수행하는 메서드
     *  - 결과에 대응하는 안부가 존재하지 않을 경우 : 결과 삭제
     *  - 결과에 대응하는 안부와 시간이 동일하지 않을 경우 : 결과 삭제
     *  - 결과에 대응하는 안부와 시간이 동일할 경우 : 결과 수정
     *
     * 결과 수정 작업 2가지
     *  1. 이름 변경 : 결과(result) 이름을 대응하는 안부의 이름으로 변경
     *  2. 응답 목록 변경 : 응답(answer) 목록에 대응하는 안부의 질문 목록에 맞게 변경
     */
    private fun updateResult(curTime : String, curDay : String, resultId : String, resultDTO : ResultDTO) {
        val uid = Firebase.auth.uid.toString()

        safetyDB.child(resultDTO.safetyId).get().addOnSuccessListener { safetySnapshot ->
            if (safetySnapshot.getValue(SafetyDTO::class.java) != null) {
                val safetyDTO = safetySnapshot.getValue(SafetyDTO::class.java) as SafetyDTO
                val questionList = safetyDTO.questionList

                Log.d("확인용[updateResult]", safetyDTO.toString())
                if (resultDTO.safetyTime == safetyDTO.time) {
                    resultMap[resultDTO.safetyId] = resultId

                    /* 1. 안부 이름 변경 */
                    resultDTO.safetyName = safetyDTO.name!!
                    /* 2-(i) 이미 만들어둔 answerId 를 제거 */
                    for (answer in resultDTO.answerIdList) {
                        val questionId = answer.key
                        answerDB.child(questionId).removeValue()
                    }
                    /* 2-(ii) answerId 제거 후 answerIdList 를 초기화 */
                    resultDTO.answerIdList.clear()
                    /* 2-(iii) 질문에 대응하는 answer 를 생성 및 answerIdList 채우기 */
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
                                resultDTO.answerIdList[questionId] = answerId /* answerIdList 채우기 [questionId, answerId] */
                            }
                        }
                    }
                    /* 비동기식 데이터 통신으로 인해 1초 후 시작 */
                    Handler().postDelayed({
                        Log.d("확인용[updateResult]", "update!!! => $resultDTO")
                        /* 3. 결과를 새로 세팅하고 추천 리스트에 결과 추가 */
                        resultDB.child(resultId).setValue(resultDTO)
                        addResultToRecommendList(curTime, getDay(curDay), resultId, resultDTO, safetyDTO)
                    }, 1000)
                } else {
                    Log.d("확인용[updateResult]", "remove[1]!!! => $resultDTO")
                    for (answer in resultDTO.answerIdList) {
                        val questionId = answer.key
                        answerDB.child(questionId).removeValue()
                    }
                    resultDB.child(resultId).removeValue()
                    wardDB.child(uid).child("resultIdList").child(resultId).removeValue()
                }
            } else {
                Log.d("확인용[updateResult]", "remove[2]!!! => $resultDTO")
                for (answer in resultDTO.answerIdList) {
                    val questionId = answer.key
                    answerDB.child(questionId).removeValue()
                }
                resultDB.child(resultId).removeValue()
                wardDB.child(uid).child("resultIdList").child(resultId).removeValue()
            }
        }
    }

    /* 결과에 설정된 date(ex: 2022/07/12) 를 day 로 변환후 timeGap 을 계산하여 추천 리스트에 추가하는 메서드 */
    private fun addResultToRecommendList(curTime : String, curDay : Int, resultId : String, resultDTO : ResultDTO, safetyDTO : SafetyDTO) {
        val resultDate = resultDTO.date
        val resultDateArray = resultDate.split("-")
        val safetyDay = LocalDate.of(resultDateArray[0].toInt(),
            resultDateArray[1].toInt(),
            resultDateArray[2].toInt()).dayOfWeek.value

        val timeGap = if (curDay == safetyDay) {
            getTimeGap(curTime, resultDTO.safetyTime, 0)
        } else if (safetyDay - curDay < 0) {
            getTimeGap(curTime, resultDTO.safetyTime, (safetyDay + 7) - curDay)
        } else {
            getTimeGap(curTime, resultDTO.safetyTime, safetyDay - curDay)
        }

        recommendList.add(RecommendDTO(false, timeGap + 1800, resultDTO.safetyId, safetyDTO, resultId, resultDTO))
    }

    /*
     * 피보호자 측에게 강제 알람을 띄우도록 하는 메서드
     * safetyId, safetyName : 피보호자가 보유한 안부 id 및 안부 이름
     */
    private fun forceAlarm(context : Context, recommendDTO : RecommendDTO) {
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

        contentIntent.putExtra("safetyDTO", recommendDTO.safetyDTO)
        contentIntent.putExtra("resultDTO", recommendDTO.resultDTO)

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

    /*
     * 피보호자와 연결된 보호자들에게 푸시 알람을 띄우도록 하는 메서드
     * safetyId, safetyName : 피보호자가 미응답한 안부 id 및 안부 이름
     */
    private fun pushAlarm(context : Context, recommendDTO : RecommendDTO) {
        val uid = Firebase.auth.uid.toString()
        val safetyName = recommendDTO.safetyDTO.name.toString()

        /* 결과에 대한 응답이 없는 경우 */
        if (recommendDTO.resultDTO!!.responseTime == "미응답") {
            userDB.child(uid).get().addOnSuccessListener { userSnapshot ->
                if (userSnapshot.getValue(UserDTO::class.java) != null) {
                    val myUserDTO = userSnapshot.getValue(UserDTO::class.java) as UserDTO

                    wardDB.child(uid).get().addOnSuccessListener { wardSnapshot ->
                        if (wardSnapshot.getValue(WardDTO::class.java) != null) {
                            val wardDTO = wardSnapshot.getValue(WardDTO::class.java) as WardDTO

                            for (connectUser in wardDTO.connectList) {
                                val connectUid = connectUser.value

                                notifyToGuardian(context, myUserDTO, connectUid, safetyName)
                            }
                        }
                    }
                }
            }
        } else {
            /* 다시 알람쪽으로... */
        }
    }

    private fun notifyToGuardian(context : Context, myUserDTO : UserDTO, connectUid : String, safetyName : String) {
        val firebaseViewModel = FirebaseViewModel(context as Application)

        userDB.child(connectUid).get().addOnSuccessListener { userSnapshot ->
            val connectUserDTO = userSnapshot.getValue(UserDTO::class.java) as UserDTO
            val token = connectUserDTO.token.toString()
            val notificationData = NotificationDTO.NotificationData("안심 집배원"
                , myUserDTO.name!!, myUserDTO.name + "님이 요청을 보냈습니다.")
            val notificationDTO = NotificationDTO(token, notificationData)

            firebaseViewModel.sendNotification(notificationDTO) /* FCM 전송하기 */
        }
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