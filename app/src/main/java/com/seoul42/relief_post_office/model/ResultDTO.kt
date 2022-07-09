package com.seoul42.relief_post_office.model

import java.io.Serializable

data class ResultDTO(
    val resultData: ResultData,
    val questionAndAnswerIdList: QuestionAndAnswerIdList
) : Serializable {
    data class ResultData(
        val date: String,
        val safetyId: String,
        val responseTime: String
    ) : Serializable {
        constructor() : this("", "", "")
    }
    data class QuestionAndAnswerIdList(
        val questionAndAnswerIdList: ArrayList<QuestionAndAnswerId>
    ) : Serializable {
        constructor() : this(ArrayList<QuestionAndAnswerId>())
    }
}