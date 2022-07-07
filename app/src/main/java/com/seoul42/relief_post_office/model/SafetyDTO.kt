package com.seoul42.relief_post_office.model

import java.io.Serializable

data class SafetyDTO(
	val key: String,
	val data: SafetyData?
) : Serializable {
	data class SafetyData(
		val uid: String,
		val content:String,
		val date: String
	) : Serializable {
		constructor() : this("", "", "")
	}
	constructor() : this("", SafetyData())
}

