package com.seoul42.relief_post_office.model

import java.io.Serializable

data class ResultDTO(
    var date: String,
    var safetyId : String,
    var safetyName : String,
    var safetyTime : String, // ex) 18:00
    var responseTime: String,
    var answerIdList: MutableMap<String, String>
) : Serializable {
    constructor() : this("", "", "", "", "미응답", mutableMapOf())
}