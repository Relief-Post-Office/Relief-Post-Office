package com.seoul42.relief_post_office.model

import java.io.Serializable

data class WardDTO(
    val safetyIdList: MutableMap<String, String>,
    val resultIdList: MutableMap<String, String>,
    val requestList: MutableMap<String, String>,
    val connectList: MutableMap<String, String>,
) :Serializable {
    constructor() : this(mutableMapOf(), mutableMapOf(), mutableMapOf(), mutableMapOf())
}