package com.seoul42.relief_post_office.model

data class NotificationDTO(
    val to: String,
    val data: NotificationData
) {
    data class NotificationData(
        val title: String,
        val name : String,
        val message: String
    )
}