package com.seoul42.relief_post_office.model

data class ResultDTO(
    val resultData: ResultData,
    val answerList: QuestionAndAnswer
) {
    data class ResultData(
        val date: String,
        val regardId: String,
        val responseTime: String
    ) {
        constructor() : this("", "", "")
    }
    data class QuestionAndAnswer(
        val questionId: String,
        val answerId: String
    ) {
        constructor() : this("", "")
    }
}