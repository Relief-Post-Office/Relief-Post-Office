package com.seoul42.relief_post_office.model

data class ResultDTO(
    var date: String,
    var regardId: String,
    var responseTime: Int,
    var answerList: MutableMap<String, String>
    ) {
    constructor() : this("", "", 0, mutableMapOf("" to ""))
}