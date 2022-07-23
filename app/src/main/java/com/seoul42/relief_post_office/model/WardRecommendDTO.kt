package com.seoul42.relief_post_office.model

import java.io.Serializable

data class WardRecommendDTO(
    var timeGap: Int,
    val safetyId: String,
    var resultId: String?,
    val safetyDTO: SafetyDTO,
    var resultDTO: ResultDTO?
) : Serializable