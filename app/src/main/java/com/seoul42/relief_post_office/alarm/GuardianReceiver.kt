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

class GuardianReceiver () : BroadcastReceiver() {

    /* 추천 가능한 모든 객체들을 담음 */
    private val candidateList = ArrayList<GuardianRecommendDTO>()
    /* 추천 객체들을 담음 */
    private val recommendList = ArrayList<GuardianRecommendDTO>()

    /* Access to database */
    private val userDB = Firebase.database.reference.child("user")
    private val wardDB = Firebase.database.reference.child("ward")
    private val resultDB = Firebase.database.reference.child("result")
    private val safetyDB = Firebase.database.reference.child("safety")
    private val guardianDB = Firebase.database.reference.child("guardian")

    /* Several notification Id : 상단에 공지가 누적되는 것을 방지 */
    private var notificationId : Int = 100

    /* user Id */
    private lateinit var uid : String

    /* Fail flag */
    private var isFail : Boolean = false

    /*
     * REPEAT_START : "강제 알람 요청", "푸시 알람 요청", "요청 없음" 셋 중 하나를 결정하기 위한 플래그
     * REPEAT_STOP : 특정 안부에 대한 통지 알람 요청을 수행하기 위한 플래그
     */
    companion object {
        const val PERMISSION_REPEAT = "com.rightline.backgroundrepeatapp.permission.ACTION_REPEAT"
        const val REPEAT_START = "com.rightline.backgroundrepeatapp.REPEAT_START"
        const val REPEAT_STOP = "com.rightline.backgroundrepeatapp.REPEAT_STOP"
    }

    /*
     *  알람 요청을 받고 플래그에 따라 특정 작업을 수행하는 메서드
     *
     *  알람 요청을 받는 5 가지 케이스
     *  - 1. 보호자가 메인 화면으로 이동
     *  - 2. 보호자가 재부팅한 경우
     *  - 3. 연결된 피보호자의 안부가 추가된 경우
     *  - 4. 연결된 피보호자의 안부가 수정된 경우
     *  - 5. 연결된 피보호자의 안부가 삭제된 경우
     */
    override fun onReceive(context: Context, intent: Intent) {
        Log.d("확인", "Guardian")
        if (!Network.isNetworkAvailable(context)) {
            Log.d("확인", "보호자측 네트워크 연결 실패")
            setNetworkAlarm(context)
        } else {
            if (Firebase.auth.currentUser != null) {
                uid = Firebase.auth.uid.toString()
                Log.d("확인", "보호자측 네트워크 연결 성공")
                when (intent.action) {
                    REPEAT_START -> {
                        recommend(context)
                    }
                    REPEAT_STOP -> {
                        val recommendList =
                            intent.getSerializableExtra("recommendList") as ArrayList<GuardianRecommendDTO>
                        notifyAlarm(context, recommendList)
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
     *  연결된 피보호자 중 가장 근접한 안부를 찾는 메서드
     *   1. findWard 메서드 : 피보호자의 정보를 찾음 (그 후에 안부를 찾음)
     *   2. candidateList : 가장 근접한 후보 객체를 선별후 timeGap 이 동일한 객체를 recommendList 에 추가
     *   3. 통지 알람 세팅 : 1, 2 작업이 끝날 경우 수행 (단, candidateList 가 빌 경우 수행 x)
     */
    private fun recommend(context : Context) {
        val date = SimpleDateFormat("yyyy-MM-dd HH:mm:ss E")
            .format(Date(System.currentTimeMillis()))
        val curDate = date.substring(0, 10)
        val curTime = date.substring(11, 19)
        val curDay = date.split(" ")[2]
        val dateDTO = DateDTO(curDate, curTime, getDay(curDay))

        guardianDB.child(uid).get().addOnSuccessListener { snapshot ->
            if (snapshot.getValue(GuardianDTO::class.java) != null) {
                val guardianDTO = snapshot.getValue(GuardianDTO::class.java) as GuardianDTO

                /* 1. 피보호자 정보 */
                for (ward in guardianDTO.connectList) {
                    val wardId = ward.value
                    findWard(context, dateDTO, wardId)
                }
                /* 2. candidateList */
                Handler().postDelayed({
                    if (candidateList.isNotEmpty()) {
                        val timeGap = candidateList.minBy{ it.timeGap }.timeGap

                        for (candidate in candidateList) {
                            if (timeGap == candidate.timeGap) {
                                recommendList.add(candidate)
                            }
                        }
                        Log.d("확인용...", recommendList.toString())
                        /* 3. 통지 알람 세팅 */
                        setAlarm(context, REPEAT_STOP, recommendList)
                    }
                }, 5000)
            }
        }.addOnFailureListener {
            if (!isFail) {
                Log.d("확인", "보호자측 네트워크 연결 실패")
                isFail = true
                setNetworkAlarm(context)
            }
        }
    }

    /* 피보호자의 안부 리스트에 존재하는 안부를 추가하도록 돕는 메서드 */
    private fun findWard(context : Context, dateDTO : DateDTO, wardId : String) {
        wardDB.child(wardId).get().addOnSuccessListener {
            if (it.getValue(WardDTO::class.java) != null) {
                val wardDTO = it.getValue(WardDTO::class.java) as WardDTO

                for (safety in wardDTO.safetyIdList) {
                    val safetyId = safety.key
                    addSafetyToRecommend(context, dateDTO, wardId, safetyId)
                }
            }
        }.addOnFailureListener {
            if (!isFail) {
                Log.d("확인", "보호자측 네트워크 연결 실패")
                isFail = true
                setNetworkAlarm(context)
            }
        }
    }

    /*
     *  안부를 추가하는 메서드
     */
    private fun addSafetyToRecommend(context : Context, dateDTO : DateDTO, wardId : String, safetyId : String) {
        val curTime = dateDTO.curTime
        val curDay = dateDTO.curDay

        safetyDB.child(safetyId).get().addOnSuccessListener {
            if (it.getValue(SafetyDTO::class.java) != null) {
                val safetyDTO = it.getValue(SafetyDTO::class.java) as SafetyDTO

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
        }.addOnFailureListener {
            if (!isFail) {
                Log.d("확인", "보호자측 네트워크 연결 실패")
                isFail = true
                setNetworkAlarm(context)
            }
        }
    }

    /*
     *  통지 알람을 요청하는 작업을 수행하는 메서드
     *  추천 리스트를 intent 에 담아서 보내도록 함
     */
    private fun setAlarm(context: Context, alarmFlag : String, recommendList : ArrayList<GuardianRecommendDTO>?) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val schedule = Intent(alarmFlag)

        if (alarmFlag == REPEAT_STOP) {
            schedule.putExtra("recommendList", recommendList)
        }
        schedule.setClass(context, GuardianReceiver::class.java)

        val sender = PendingIntent.getBroadcast(context, 0, schedule,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val interval = Calendar.getInstance()

        /* 알람 시간 설정(recommendDTO.timeGap) */
        interval.timeInMillis = System.currentTimeMillis()
        if (alarmFlag == REPEAT_STOP) {
            val timeGap = if (recommendList!![0].timeGap - 5 < 0) {
                0
            } else {
                recommendList[0].timeGap - 5
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
     *  통지 알람을 요청하도록 하는 메서드
     *  1. 존재하는 유저인지 확인
     *  2. 피보호자인지 확인
     *
     *  위 2 조건을 만족할 경우 피보호자의 userDTO, wardDTO 를 가지고 통지할지를 결정
     */
    private fun notifyAlarm(context : Context, recommendList : ArrayList<GuardianRecommendDTO>) {
        for (recommendDTO in recommendList) {
            userDB.child(recommendDTO.wardId).get().addOnSuccessListener { userSnapshot ->
                /* 1. 존재하는 유저인지 확인 */
                if (userSnapshot.getValue(UserDTO::class.java) != null) {
                    val userDTO = userSnapshot.getValue(UserDTO::class.java) as UserDTO
                    wardDB.child(recommendDTO.wardId).get().addOnSuccessListener { wardSnapshot ->
                        /* 2. 피보호자인지 확인 */
                        if (wardSnapshot.getValue(WardDTO::class.java) != null) {
                            val wardDTO = wardSnapshot.getValue(WardDTO::class.java) as WardDTO
                            compareSafetyAndResult(context, userDTO, wardDTO, recommendDTO)
                        }
                    }.addOnFailureListener {
                        if (!isFail) {
                            Log.d("확인", "보호자측 네트워크 연결 실패")
                            isFail = true
                            setNetworkAlarm(context)
                        }
                    }
                }
            }.addOnFailureListener {
                if (!isFail) {
                    Log.d("확인", "보호자측 네트워크 연결 실패")
                    isFail = true
                    setNetworkAlarm(context)
                }
            }
        }

        Handler().postDelayed({
            setAlarm(context, REPEAT_START, null)
        }, 5000)
    }

    /*
     *  최종적으로 통지 알람을 보호자에게 보낼지를 결정
     *  아래의 조건이 전부 만족할 경우 보호자에게 통지 알람을 보냄
     *
     *  1. 피보호자가 안부를 미응답했는지
     *  2. 결과가 현재 시간으로부터 30 분 전인지
     *  3. 결과에 대응하는 안부 id 가 선별된 안부 id 와 동일한지
     *  4. 결과에 대응하는 안부가 존재하는지
     */
    private fun compareSafetyAndResult(context : Context, userDTO : UserDTO, wardDTO : WardDTO, recommendDTO : GuardianRecommendDTO) {
        val curDate : String
        val curTime : String
        val cal = Calendar.getInstance()
        val date = SimpleDateFormat("yyyy-MM-dd HH:mm")

        cal.time = Date()
        cal.add(Calendar.MINUTE, -30)
        Log.d("확인용", date.format(cal.time))

        curDate = date.format(cal.time).substring(0, 10)
        curTime = date.format(cal.time).substring(11, 16)

        for (result in wardDTO.resultIdList) {
            val resultId = result.value

            resultDB.child(resultId).get().addOnSuccessListener { resultSnapshot ->
                if (resultSnapshot.getValue(ResultDTO::class.java) != null) {
                    val resultDTO = resultSnapshot.getValue(ResultDTO::class.java) as ResultDTO

                    if (resultDTO.responseTime == "미응답" && resultDTO.date == curDate
                        && resultDTO.safetyTime == curTime && resultDTO.safetyId == recommendDTO.safetyId) {
                        safetyDB.child(resultDTO.safetyId).get().addOnSuccessListener { safetySnapshot ->
                            if (safetySnapshot.getValue(SafetyDTO::class.java) != null) {
                                val safetyDTO = safetySnapshot.getValue(SafetyDTO::class.java) as SafetyDTO

                                notifySafety(context, userDTO, safetyDTO)
                            }
                        }.addOnFailureListener {
                            if (!isFail) {
                                Log.d("확인", "보호자측 네트워크 연결 실패")
                                isFail = true
                                setNetworkAlarm(context)
                            }
                        }
                    }
                }
            }.addOnFailureListener {
                if (!isFail) {
                    Log.d("확인", "보호자측 네트워크 연결 실패")
                    isFail = true
                    setNetworkAlarm(context)
                }
            }
        }
    }

    /*
     *  최종적으로 보호자에게 통지 알람을 보내는 메서드
     *  피보호자가 "미응답"한 안부를 보호자 핸드폰의 상단에 메시지가 띄워지도록 함
     */
    private fun notifySafety(context : Context, userDTO : UserDTO, safetyDTO : SafetyDTO) {
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
        builder.setContentTitle("안심 우체국") // 제목
            .setContentText(body) // 내용
            .setStyle(messageStyle)
            .setSmallIcon(R.drawable.relief_post_office) // 아이콘
            .setAutoCancel(true)

        notificationManager.notify(notificationId++ , builder.build()) // 알림 생성
    }
}