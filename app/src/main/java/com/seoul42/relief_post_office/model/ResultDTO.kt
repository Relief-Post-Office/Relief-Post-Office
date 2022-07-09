package com.seoul42.relief_post_office.model

import java.io.Serializable

data class ResultDTO(
    var date: String,
    var safetyId: String,
    var responseTime: String,
    var answerList: MutableMap<String, String>
) : Serializable {
    constructor() : this("", "", "", mutableMapOf("" to ""))
}