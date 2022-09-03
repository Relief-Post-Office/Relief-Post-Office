package com.seoul42.relief_post_office.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.Intent.FLAG_INCLUDE_STOPPED_PACKAGES
import android.media.RingtoneManager
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.app.Person
import androidx.core.content.ContextCompat.getSystemService
import androidx.core.graphics.drawable.IconCompat
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.seoul42.relief_post_office.R
import com.seoul42.relief_post_office.alarm.GuardianReceiver
import com.seoul42.relief_post_office.alarm.WardReceiver
import com.seoul42.relief_post_office.util.Alarm

/**
 * FCM 푸시 알람을 처리하도록 돕는 클래스
 * 푸시 알람을 통해 핸드폰 상단에 알람을 수신받을 수 있음
 * 또한 궁극적으로 보호자 및 피보호자의 안부 동기화를 돕는 하나의 매개체가 됨
 */
class MyFirebaseMessagingService : FirebaseMessagingService() {

    /**
     * 메세지가 수신되면 호출
     */
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        // 다른 기기에서 서버로 보냈을 때
        if (remoteMessage.data.isNotEmpty()) {
            val title = remoteMessage.data["title"]!!
            val userId = remoteMessage.data["text"]!!
            val message = remoteMessage.data["message"]!!

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                sendMessageNotification(title, userId, message)
            }
        }
    }

    /**
     * Firebase Cloud Messaging Server 가 대기중인 메세지를 삭제 시 호출
     */
    override fun onDeletedMessages() {
        super.onDeletedMessages()
    }

    /**
     * 메세지가 서버로 전송 성공 했을때 호출
     */
    override fun onMessageSent(p0: String) {
        super.onMessageSent(p0)
    }

    /**
     * 메세지가 서버로 전송 실패 했을때 호출
     */
    override fun onSendError(p0: String, p1: Exception) {
        super.onSendError(p0, p1)
    }

    /**
     * 새로운 토큰이 생성 될 때 호출
     */
    override fun onNewToken(token: String) {
        super.onNewToken(token)
    }

    /**
     * 다른 기기에서 서버로 보냈을 때 호출
     * 보내진 메시지의 제목에 따라 처리가 달라짐 (안부 동기화 또는 상단에 푸시 알람 띄우기)
     *  - SafetyWard : 피보호자 알람 설정(안부 동기화)
     *  - SafetyGuardian : 보호자 알람 설정(안부 동기화) 및 상단에 푸시 알람 띄우기
     *  - 그 외 : 상단에 푸시 알람 띄우기
     */
    @RequiresApi(Build.VERSION_CODES.P)
    private fun sendMessageNotification(
        title: String,
        name: String,
        body: String
    ) {
        // 로그인 되지 않을 경우 예외처리
        Firebase.auth.currentUser ?: throw IllegalArgumentException("user required")

        when (title) {
            "SafetyWard" -> {
                setWardAlarm()
            }
            "SafetyGuardian" -> {
                setGuardianAlarm()
                setNotification(title, name, body)
            }
            else -> {
                setNotification(title, name, body)
            }
        }
    }

    private fun setWardAlarm() {
        val start = Intent(WardReceiver.REPEAT_START)

        start.setClass(this, WardReceiver::class.java)
        sendBroadcast(start, WardReceiver.PERMISSION_REPEAT)
    }

    private fun setGuardianAlarm() {
        val start = Intent(GuardianReceiver.REPEAT_START)

        start.setClass(this, GuardianReceiver::class.java)
        sendBroadcast(start, GuardianReceiver.PERMISSION_REPEAT)
    }

    /**
     * 받은 푸시 알람을 띄우도록 설정
     *  - title : 푸시 알람의 제목
     *  - name : 송신자 이름
     *  - body : 송신자가 보낼 내용
     */
    private fun setNotification(
        title: String,
        name: String,
        body: String
    ) {
        val intent = Intent(this, CheckLoginService::class.java)

        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP) // 액티비티 중복 생성 방지
        val pendingIntent = PendingIntent.getActivity(this, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)

        // messageStyle
        val user: Person = Person.Builder()
            .setName(name)
            .setIcon(IconCompat.createWithResource(this, R.drawable.relief_post_office))
            .build()
        val message = NotificationCompat.MessagingStyle.Message(
            body,
            System.currentTimeMillis(),
            user
        )
        val messageStyle = NotificationCompat.MessagingStyle(user)
            .addMessage(message)

        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val builder = NotificationCompat.Builder(this, "default")

        // 오레오 버전 예외처리
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelName = "매일 알람 채널"
            val description = "매일 정해진 시간에 알람합니다."
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel("default", channelName, importance)

            channel.description = description
            notificationManager.createNotificationChannel(channel)
        }

        builder.setContentTitle(title) // 제목
            .setContentText(body) // 내용
            .setStyle(messageStyle)
            .setSmallIcon(R.drawable.relief_post_office) // 아이콘
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        notificationManager.notify(0, builder.build()) // 알림 생성
    }
}