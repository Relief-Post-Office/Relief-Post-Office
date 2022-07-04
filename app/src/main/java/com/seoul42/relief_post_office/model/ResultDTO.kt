package com.seoul42.relief_post_office.model

data class ResultDTO(
    var date: String,
    var regard_id: String,
    var responseTime: Int,
    var answerList: MutableMap<String, String> //<String, String> = <question_id, answer_id>
    ) {
    constructor() : this("", "", 0, mutableMapOf("" to ""))
}