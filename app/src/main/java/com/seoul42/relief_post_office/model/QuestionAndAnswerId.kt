package com.seoul42.relief_post_office.model

import java.io.Serializable

data class QuestionAndAnswerId(
    val questionId: String,
    val answerId: String
) : Serializable {
    constructor() : this("", "")
}
