package com.seoul42.relief_post_office.model

import java.io.Serializable

data class SafetyDTO(
	val uid: String?,
	val name: String?,
	val date: String?,
	val time: String?,  // (ex) 18:00
	val questionList: MutableMap<String, String>,
	val dayOfWeek: MutableMap<String, String>
) : Serializable{
	constructor() : this("", "", "","", mutableMapOf(), mutableMapOf())
}