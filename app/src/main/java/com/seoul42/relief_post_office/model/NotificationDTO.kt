package com.seoul42.relief_post_office.model

data class NotificationDTO(
    val to: String,
    val priority: String,
    val data: NotificationData
) {
    data class NotificationData(
        val title: String,
        val text : String,
        val message: String
    )
}