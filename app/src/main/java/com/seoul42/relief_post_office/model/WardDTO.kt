package com.seoul42.relief_post_office.model

import java.io.Serializable

data class WardDTO(
    val connectedSafetyIdList: ArrayList<String>,
    val resultIdList: MutableMap<String, String>,
    val requestedUserIdList: ArrayList<String>,
    val connectedUserIdList: ArrayList<String>
) :Serializable {
    constructor() : this(arrayListOf(), mutableMapOf("" to ""), arrayListOf(), arrayListOf())
}