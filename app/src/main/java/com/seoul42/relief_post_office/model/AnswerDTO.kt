package com.seoul42.relief_post_office.model

import java.io.Serializable

data class AnswerDTO(
    var reply : Boolean?,
    var questionSecret : Boolean,
    var questionRecord : Boolean,
    var questionOwner : String,
    var questionSrc : String,
    var questionText : String
) : Serializable {
    constructor() : this(null, false, false, "", "", "")
}