package com.seoul42.relief_post_office.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.seoul42.relief_post_office.model.NotificationDTO
import com.seoul42.relief_post_office.repository.FirebaseRepository
import kotlinx.coroutines.launch

/**
 * FCM 푸시 알람을 돕는 클래스
 */
class FirebaseViewModel(application: Application) : AndroidViewModel(application) {

    private val repository : FirebaseRepository = FirebaseRepository()

    /**
     * 푸시 메세지를 전송하는 메서드
     *  - notification : 전송하고자 할 객체(title, name, body)
     */
    fun sendNotification(notification: NotificationDTO) {
        viewModelScope.launch {
            repository.sendNotification(notification)
        }
    }
}