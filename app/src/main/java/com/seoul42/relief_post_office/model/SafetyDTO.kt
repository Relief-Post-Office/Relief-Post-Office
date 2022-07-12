package com.seoul42.relief_post_office.model

import java.io.Serializable

data class SafetyDTO(
	val uid: String?,
	val name: String?,
	val date: String?,
	val time: String?, // (ex) 18:00
	val questionList: MutableMap<String, String>,
	val dayOfWeek: MutableMap<String, Boolean>
) : Serializable{
	constructor() : this("", "", "","", mutableMapOf(),
		mutableMapOf(
			Pair("월", false),
			Pair("화", false),
			Pair("수", false),
			Pair("목", false),
			Pair("금", false),
			Pair("토", false),
			Pair("일", false)
		))
}