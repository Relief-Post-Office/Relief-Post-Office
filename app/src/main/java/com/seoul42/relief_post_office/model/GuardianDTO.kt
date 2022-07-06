package com.seoul42.relief_post_office.model

data class GuardianBody(
    val questionData: QuestionData?,
    val safetyData: SafetyData?,
    val connectData: ConnectData?
) {
    data class QuestionData(
        val questionList : ArrayList<String>
    ) {
        constructor() : this(ArrayList<String>())
    }
    data class SafetyData(
        val safetyList : ArrayList<String>
    ) {
        constructor() : this(ArrayList<String>())
    }
    data class ConnectData(
        val connectList : ArrayList<String>
    ) {
        constructor() : this(ArrayList<String>())
    }
    constructor() : this(null, null, null)
}
