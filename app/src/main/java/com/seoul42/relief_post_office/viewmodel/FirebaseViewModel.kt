package com.seoul42.relief_post_office.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.seoul42.relief_post_office.model.NotificationDTO
import com.seoul42.relief_post_office.repository.FirebaseRepository
import kotlinx.coroutines.launch

class FirebaseViewModel(application: Application) : AndroidViewModel(application) {

    private val repository : FirebaseRepository = FirebaseRepository()

    // 푸시 메세지 전송
    fun sendNotification(notification: NotificationDTO) {
        viewModelScope.launch {
            repository.sendNotification(notification)
        }
    }
}