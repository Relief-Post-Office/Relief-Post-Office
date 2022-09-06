package com.seoul42.relief_post_office.model

import java.io.Serializable

data class QuestionDTO(
    var secret: Boolean,    // 비밀 옵션
    var record: Boolean,     // 녹음 옵션
    var ttsFlag: Boolean,    // tts 옵션
    var owner: String?,      // 소유주
    var date: String?,        //  마지막 수정 날짜
    var text: String?,       // 텍스트
    var src: String?,         // 녹음 파일 주소
    var connectedSafetyList : MutableMap<String, String?>    // 연결된 피보호자 안부 목록
) : Serializable {
    constructor() : this(false, false, false,"", "", "", "", mutableMapOf())
}