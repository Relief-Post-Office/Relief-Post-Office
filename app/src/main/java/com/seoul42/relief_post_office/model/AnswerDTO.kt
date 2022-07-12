package com.seoul42.relief_post_office.model

import java.io.Serializable

data class AnswerDTO(
    var reply : Boolean?,
    var questionSecret : Boolean,
    var questionRecord : Boolean,
    var questionOwner : String,
    var questionSrc : String,
    var questionText : String,
    var answerSrc : String  // 답변 녹음
) : Serializable {
    constructor() : this(null, false, false, "", "", "", "")
}