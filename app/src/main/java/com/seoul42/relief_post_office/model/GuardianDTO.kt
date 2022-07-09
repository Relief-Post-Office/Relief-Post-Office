package com.seoul42.relief_post_office.model

data class GuardianDTO(
    val questionList : ArrayList<String>,
    val safetyList : ArrayList<String>,
    val connectList : ArrayList<String>
) {
    constructor() : this(ArrayList<String>(), ArrayList<String>(), ArrayList<String>())
}