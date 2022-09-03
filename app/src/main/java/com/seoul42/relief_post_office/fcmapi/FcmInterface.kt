package com.seoul42.relief_post_office.fcmapi

import com.seoul42.relief_post_office.model.NotificationDTO
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

/**
 * Fcm 전송을 돕는 인터페이스
 */
interface FcmInterface {

    @POST("fcm/send")
    suspend fun sendNotification(
        @Body notification: NotificationDTO
    ) : Response<ResponseBody>
}