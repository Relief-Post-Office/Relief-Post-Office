package com.seoul42.relief_post_office.model

import java.io.Serializable

data class GuardianRecommendDTO(
    val timeGap: Int,
    val wardId: String,
    val safetyId: String,
) : Serializable