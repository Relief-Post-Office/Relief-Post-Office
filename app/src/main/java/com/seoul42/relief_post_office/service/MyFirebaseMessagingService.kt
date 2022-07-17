package com.seoul42.relief_post_office.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.app.Person
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

class MyFirebaseMessagingService : FirebaseMessagingService() {
    // 메세지가 수신되면 호출
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        // 서버에서 직접 보냈을 때(사용 x)
        if(remoteMessage.notification != null){
            sendNotification(remoteMessage.notification?.title,
                remoteMessage.notification?.body!!)
        }
        // 다른 기기에서 서버로 보냈을 때(이 경우에 해당)
        else if(remoteMessage.data.isNotEmpty()){
            val title = remoteMessage.data["title"]!!
            val userId = remoteMessage.data["name"]!!
            val message = remoteMessage.data["message"]!!

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                sendMessageNotification(title, userId, message)
            }
            else{
                sendNotification(remoteMessage.notification?.title,
                    remoteMessage.notification?.body!!)
            }
        }
    }

    // Firebase Cloud Messaging Server 가 대기중인 메세지를 삭제 시 호출
    override fun onDeletedMessages() {
        super.onDeletedMessages()
    }

    // 메세지가 서버로 전송 성공 했을때 호출
    override fun onMessageSent(p0: String) {
        super.onMessageSent(p0)
    }

    // 메세지가 서버로 전송 실패 했을때 호출
    override fun onSendError(p0: String, p1: Exception) {
        super.onSendError(p0, p1)
    }

    // 새로운 토큰이 생성 될 때 호출
    override fun onNewToken(token: String) {
        super.onNewToken(token)
    }

    // 서버에서 직접 보냈을 때
    private fun sendNotification(title: String?, body: String){
        val intent = Intent(this, CheckLoginService::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP) // 액티비티 중복 생성 방지
        val pendingIntent = PendingIntent.getActivity(this, 0 , intent,
            PendingIntent.FLAG_ONE_SHOT) // 일회성

        val channelId = "channel" // 채널 아이디
        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION) // 소리
        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setContentTitle(title) // 제목
            .setContentText(body) // 내용
            .setSmallIcon(R.drawable.ic_launcher_background) // 아이콘
            .setAutoCancel(true)
            .setSound(defaultSoundUri)
            .setContentIntent(pendingIntent)

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // 오레오 버전 예외처리
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId,
                "Channel human readable title",
                NotificationManager.IMPORTANCE_HIGH)
            notificationManager.createNotificationChannel(channel)
        }

        notificationManager.notify(0 , notificationBuilder.build()) // 알림 생성
    }

    // 다른 기기에서 서버로 보냈을 때
    @RequiresApi(Build.VERSION_CODES.P)
    private fun sendMessageNotification(title: String, name: String, body: String) {
        /* 피보호자가 안부에 대한 푸시 알람을 받게 될 경우 알람 설정 */
        if (title == "SafetyWard") {
            if (Firebase.auth.currentUser != null) {
                val start = Intent(WardReceiver.REPEAT_START)

                start.setClass(this, WardReceiver::class.java)
                sendBroadcast(start, WardReceiver.PERMISSION_REPEAT)
            }
        }
        /* 보호자가 피보호자의 안부에 대한 변경 작업이 있을 때 알람 설정 */
        else if (title == "SafetyGuardian") {
            if (Firebase.auth.currentUser != null) {
                val start = Intent(GuardianReceiver.REPEAT_START)

                start.setClass(this, GuardianReceiver::class.java)
                sendBroadcast(start, GuardianReceiver.PERMISSION_REPEAT)
            }
        }

        if (title != "SafetyWard") {
            /* 받은 푸시 알람을 띄우도록 설정 */
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
}