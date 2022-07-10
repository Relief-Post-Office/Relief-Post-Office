package com.seoul42.relief_post_office.model

import java.io.Serializable

data class ResultDTO(
    var date: String,
    var safetyId: String,
    var responseTime: String,
    var answerIdList: MutableMap<String, String>
) : Serializable {
    constructor() : this("", "", "미응답", mutableMapOf("" to ""))
}