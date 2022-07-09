package com.seoul42.relief_post_office.model

import java.io.Serializable

data class SafetyDTO(
	val uid: String?,
	val name: String?,
	val time: String?,
	val questionList: ArrayList<String>,
	val dayOfWeek: ArrayList<String>
) : Serializable{
	constructor() : this("", "", "", ArrayList<String>(), ArrayList<String>())
}