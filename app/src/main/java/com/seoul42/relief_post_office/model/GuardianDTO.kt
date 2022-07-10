package com.seoul42.relief_post_office.model

data class GuardianDTO(
    val questionList : MutableMap<String, String>,
    val safetyList : MutableMap<String, String>,
    val connectList : MutableMap<String, String>
) {
    constructor() : this(mutableMapOf(), mutableMapOf(), mutableMapOf())
}