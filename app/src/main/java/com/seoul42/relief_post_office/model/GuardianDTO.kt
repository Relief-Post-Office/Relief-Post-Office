package com.seoul42.relief_post_office.model

import java.io.Serializable

data class GuardianDTO(
	val key: String,
	val data: GuardianData?
) : Serializable
{
	data class GuardianData (
		val safetyList: String
	) {
		constructor() : this("")
	}
	constructor() : this("", null)
}
