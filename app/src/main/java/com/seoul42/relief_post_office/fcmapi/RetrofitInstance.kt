package com.seoul42.relief_post_office.fcmapi

import android.util.Log
import com.seoul42.relief_post_office.util.Constants.Companion.FCM_KEY
import com.seoul42.relief_post_office.util.Constants.Companion.FCM_URL
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.IOException

/**
 * 현재 프로젝트의 클라우드 메시징 서버 키를 헤더에 삽입
 * 정상적으로 서버 접근이 가능하도록 헤더를 설정
 */
object RetrofitInstance {

    private val retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(FCM_URL)
            .client(provideOkHttpClient(AppInterceptor()))
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    val api : FcmInterface by lazy {
        retrofit.create(FcmInterface::class.java)
    }

    private fun provideOkHttpClient(
        interceptor: AppInterceptor
    ): OkHttpClient = OkHttpClient.Builder()
        .run {
            addInterceptor(interceptor)
            build()
        }

    /**
     *  FCM_KEY : relief_post_office 의 클라우드 메시징 서버 키
     */
    class AppInterceptor : Interceptor {
        @Throws(IOException::class)
        override fun intercept(chain: Interceptor.Chain)
                : Response = with(chain) {
            val newRequest = request().newBuilder()
                .addHeader("Authorization", "key=$FCM_KEY")
                .addHeader("Content-Type", "application/json")
                .build()
            proceed(newRequest)
        }
    }
}