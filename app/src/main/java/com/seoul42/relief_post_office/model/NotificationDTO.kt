package com.seoul42.relief_post_office.model

data class NotificationDTO(
    val to: String,
    val title: String,
    val name : String,
    val message: String
)